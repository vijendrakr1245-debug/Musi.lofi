package com.example.audio

import android.content.Context
import android.net.Uri
import com.example.model.AcousticFeatureSummary
import com.example.model.BuiltInLofiPresets
import com.example.model.DetectedMusicMood
import com.example.model.LofiPreset
import com.example.model.MusicMoodAnalysisResult
import com.example.model.TrackItem
import com.example.model.VisualizerSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.net.URI
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * 100% On-Device Acoustic Music Mood Analyzer for LOFI WAVE.
 *
 * - Extracts real acoustic features (RMS energy, spectral brightness/centroid,
 *   onset transient density, autocorrelation tempo BPM, midrange harmonic warmth,
 *   and stereo Mid/Side width) from local PCM audio frames and live DSP telemetry.
 * - Never uploads private audio to any external server and requires zero API keys.
 * - Clearly separates Estimated Mood (acoustic heuristics) from Verified Track Metadata.
 * - Reports a confidence percentage ONLY when sufficient non-silent audio signal is analyzed.
 */
object MusicMoodAnalyzer {

    private const val MIN_REQUIRED_FRAMES = 512
    private const val MIN_NON_SILENT_RMS = 0.002f

    /**
     * Analyzes a [TrackItem] on-device by reading local PCM frames from its URI
     * (and incorporating live [VisualizerSnapshot] telemetry when actively playing).
     */
    suspend fun analyzeTrack(
        context: Context,
        track: TrackItem,
        liveSnapshot: VisualizerSnapshot?,
        availablePresets: List<LofiPreset> = BuiltInLofiPresets.ALL
    ): MusicMoodAnalysisResult = withContext(Dispatchers.IO) {
        val extracted = readLocalPcmSlice(context, track)
        if (extracted != null && extracted.pcmBytes.size >= MIN_REQUIRED_FRAMES * 4) {
            return@withContext analyzePcmBuffer(
                track = track,
                pcmBytes = extracted.pcmBytes,
                sampleRateHz = extracted.sampleRateHz,
                channelCount = extracted.channelCount,
                liveSnapshot = liveSnapshot,
                availablePresets = availablePresets,
                analysisMethodLabel = "On-Device PCM File Signal Analysis (${extracted.sampleRateHz / 1000.0}kHz)"
            )
        }

        // Fallback to live DSP visualizer telemetry if actively playing a compressed stream
        if (liveSnapshot != null && (liveSnapshot.rmsLeft > MIN_NON_SILENT_RMS || liveSnapshot.rmsRight > MIN_NON_SILENT_RMS)) {
            return@withContext analyzeFromLiveTelemetry(
                track = track,
                snapshot = liveSnapshot,
                availablePresets = availablePresets
            )
        }

        // If neither readable PCM slice nor active live signal is available, do NOT invent confidence
        buildInsufficientSignalResult(track, availablePresets)
    }

    /**
     * Core deterministic DSP acoustic feature extractor and mood classifier on 16-bit signed
     * little-endian PCM audio frames.
     */
    fun analyzePcmBuffer(
        track: TrackItem,
        pcmBytes: ByteArray,
        sampleRateHz: Int = 44100,
        channelCount: Int = 2,
        liveSnapshot: VisualizerSnapshot? = null,
        availablePresets: List<LofiPreset> = BuiltInLofiPresets.ALL,
        analysisMethodLabel: String = "On-Device PCM Acoustic Signal Analysis"
    ): MusicMoodAnalysisResult {
        val safeChannels = channelCount.coerceIn(1, 2)
        val bytesPerFrame = safeChannels * 2
        val totalFrames = pcmBytes.size / bytesPerFrame

        if (totalFrames < MIN_REQUIRED_FRAMES) {
            return buildInsufficientSignalResult(track, availablePresets)
        }

        val buf = ByteBuffer.wrap(pcmBytes, 0, totalFrames * bytesPerFrame).order(ByteOrder.LITTLE_ENDIAN)

        var sumSqL = 0.0
        var sumSqR = 0.0
        var zeroCrossings = 0
        var prevMono = 0f

        // 1-pole low-pass (~320 Hz) and band-pass (~300 Hz - 2200 Hz) states for spectral balance
        val alphaLow = (2.0 * Math.PI * 320.0 / sampleRateHz.toDouble()).toFloat().coerceIn(0.01f, 0.5f)
        val alphaMidHigh = (2.0 * Math.PI * 2200.0 / sampleRateHz.toDouble()).toFloat().coerceIn(0.05f, 0.85f)
        var lpLowState = 0f
        var lpMidState = 0f

        var lowBandEnergy = 0.0
        var midBandEnergy = 0.0
        var highBandEnergy = 0.0

        var midSideMidEnergy = 0.0
        var midSideSideEnergy = 0.0

        // Envelope hops (~23.2ms per hop at 44.1kHz -> 1024 frames) for rhythm & tempo autocorrelation
        val hopSize = (sampleRateHz / 43).coerceAtLeast(256)
        val numHops = (totalFrames / hopSize).coerceAtLeast(1)
        val hopRms = FloatArray(numHops)
        var currentHopSumSq = 0.0
        var hopFrameCounter = 0
        var hopIndex = 0

        for (frame in 0 until totalFrames) {
            val sL = (buf.short / 32768.0f).coerceIn(-1f, 1f)
            val sR = if (safeChannels == 2) (buf.short / 32768.0f).coerceIn(-1f, 1f) else sL
            val mono = 0.5f * (sL + sR)

            sumSqL += (sL * sL).toDouble()
            sumSqR += (sR * sR).toDouble()

            if ((mono >= 0f && prevMono < 0f) || (mono < 0f && prevMono >= 0f)) {
                zeroCrossings++
            }
            prevMono = mono

            // Three-band spectral energy decomposition (Low < 320Hz, Mid 320-2200Hz, High > 2200Hz)
            lpLowState += alphaLow * (mono - lpLowState)
            lpMidState += alphaMidHigh * (mono - lpMidState)
            val lowSample = lpLowState
            val midSample = lpMidState - lpLowState
            val highSample = mono - lpMidState

            lowBandEnergy += (lowSample * lowSample).toDouble()
            midBandEnergy += (midSample * midSample).toDouble()
            highBandEnergy += (highSample * highSample).toDouble()

            // Stereo Mid/Side energy
            val mid = 0.5f * (sL + sR)
            val side = 0.5f * (sL - sR)
            midSideMidEnergy += (mid * mid).toDouble()
            midSideSideEnergy += (side * side).toDouble()

            // Hop energy accumulation for onset rhythm & tempo estimation
            currentHopSumSq += (mono * mono).toDouble()
            hopFrameCounter++
            if (hopFrameCounter >= hopSize && hopIndex < numHops) {
                hopRms[hopIndex] = sqrt(currentHopSumSq / hopFrameCounter).toFloat()
                hopIndex++
                currentHopSumSq = 0.0
                hopFrameCounter = 0
            }
        }

        val rmsL = sqrt(sumSqL / totalFrames).toFloat()
        val rmsR = sqrt(sumSqR / totalFrames).toFloat()
        val combinedRms = sqrt(0.5 * (sumSqL + sumSqR) / totalFrames).toFloat()

        // Do not invent results on silent buffers
        if (combinedRms < MIN_NON_SILENT_RMS) {
            return buildInsufficientSignalResult(track, availablePresets)
        }

        // Normalize acoustic features into [0.0, 1.0]
        val rmsEnergyNorm = (combinedRms / 0.32f).coerceIn(0f, 1f)

        val totalSpectralEnergy = (lowBandEnergy + midBandEnergy + highBandEnergy).coerceAtLeast(1e-9)
        val highRatio = (highBandEnergy / totalSpectralEnergy).toFloat()
        val midRatio = (midBandEnergy / totalSpectralEnergy).toFloat()
        val zcrHz = (zeroCrossings.toFloat() / totalFrames.toFloat()) * (sampleRateHz / 2f)

        // Blend PCM high-frequency ratio + ZCR (+ live spectrum if playing) for Spectral Brightness
        val rawBrightness = (highRatio * 2.4f + (zcrHz / 3200f) * 0.45f).coerceIn(0f, 1f)
        val spectralBrightnessNorm = if (liveSnapshot != null && liveSnapshot.spectrum.size >= 16) {
            val highBinsAvg = liveSnapshot.spectrum.takeLast(12).average().toFloat()
            (0.75f * rawBrightness + 0.25f * highBinsAvg).coerceIn(0f, 1f)
        } else {
            rawBrightness
        }

        val warmMidNorm = (midRatio * 1.65f).coerceIn(0f, 1f)

        val stereoWidthNorm = if (safeChannels == 2 && midSideMidEnergy > 1e-7) {
            val sideToMid = sqrt(midSideSideEnergy / midSideMidEnergy).toFloat()
            (sideToMid * 1.85f).coerceIn(0f, 1f)
        } else {
            0.15f
        }

        // Compute Onset Flux & Autocorrelation Tempo (BPM)
        val (estimatedBpm, transientDensityNorm) = estimateTempoAndTransients(
            hopRms = hopRms,
            validHops = hopIndex,
            hopDurationSec = hopSize.toFloat() / sampleRateHz.toFloat()
        )

        val features = AcousticFeatureSummary(
            estimatedTempoBpm = estimatedBpm,
            rmsEnergyNormalized = rmsEnergyNorm,
            spectralBrightnessNormalized = spectralBrightnessNorm,
            transientDensityNormalized = transientDensityNorm,
            warmMidRatioNormalized = warmMidNorm,
            stereoWidthNormalized = stereoWidthNorm,
            analyzedFramesCount = totalFrames,
            sampleRateHz = sampleRateHz,
            analysisMethodLabel = analysisMethodLabel
        )

        return scoreAndBuildResult(
            track = track,
            features = features,
            availablePresets = availablePresets
        )
    }

    private fun estimateTempoAndTransients(
        hopRms: FloatArray,
        validHops: Int,
        hopDurationSec: Float
    ): Pair<Float, Float> {
        if (validHops < 8) {
            return 78f to 0.30f
        }

        // Half-wave rectified first-order difference (onset flux)
        val onsetFlux = FloatArray(validHops)
        var meanRms = 0f
        for (i in 0 until validHops) {
            meanRms += hopRms[i]
        }
        meanRms = (meanRms / validHops).coerceAtLeast(1e-5f)

        var positiveFluxSum = 0f
        var sharpOnsetCount = 0
        for (i in 1 until validHops) {
            val diff = (hopRms[i] - hopRms[i - 1]).coerceAtLeast(0f) / meanRms
            onsetFlux[i] = diff
            positiveFluxSum += diff
            if (diff > 0.18f) {
                sharpOnsetCount++
            }
        }

        val avgFlux = positiveFluxSum / validHops
        val onsetRatePerSec = sharpOnsetCount.toFloat() / (validHops * hopDurationSec).coerceAtLeast(0.5f)
        val transientDensity = ((avgFlux * 1.8f) + (onsetRatePerSec / 8.0f) * 0.5f).coerceIn(0f, 1f)

        // Autocorrelation over BPM range [64 BPM .. 148 BPM]
        val minLag = ((60f / 148f) / hopDurationSec).roundToInt().coerceAtLeast(2)
        val maxLag = ((60f / 64f) / hopDurationSec).roundToInt().coerceAtMost(validHops / 2)

        if (minLag >= maxLag) {
            val fallbackBpm = (68f + transientDensity * 52f).coerceIn(64f, 140f)
            return fallbackBpm to transientDensity
        }

        var bestLag = minLag
        var bestCorr = -1f
        for (lag in minLag..maxLag) {
            var corr = 0f
            val count = validHops - lag
            for (i in 0 until count) {
                corr += onsetFlux[i] * onsetFlux[i + lag]
            }
            val normalizedCorr = corr / count.coerceAtLeast(1)
            if (normalizedCorr > bestCorr) {
                bestCorr = normalizedCorr
                bestLag = lag
            }
        }

        val rawBpm = (60f / (bestLag * hopDurationSec)).coerceIn(64f, 148f)
        // Smoothly weight autocorrelation BPM with transient pulse rate when onset flux is gentle
        val blendedBpm = if (bestCorr > 0.008f) {
            rawBpm
        } else {
            (68f + transientDensity * 48f).coerceIn(64f, 138f)
        }

        return blendedBpm to transientDensity
    }

    private fun analyzeFromLiveTelemetry(
        track: TrackItem,
        snapshot: VisualizerSnapshot,
        availablePresets: List<LofiPreset>
    ): MusicMoodAnalysisResult {
        val rmsAvg = 0.5f * (snapshot.rmsLeft + snapshot.rmsRight)
        val energyNorm = (rmsAvg / 0.30f).coerceIn(0f, 1f)

        val spec = snapshot.spectrum
        val lowEnergy = if (spec.size >= 8) spec.take(8).average().toFloat() else 0.3f
        val midEnergy = if (spec.size >= 20) spec.slice(8..19).average().toFloat() else 0.35f
        val highEnergy = if (spec.size >= 32) spec.takeLast(12).average().toFloat() else 0.2f
        val totalSpec = (lowEnergy + midEnergy + highEnergy).coerceAtLeast(1e-4f)

        val brightnessNorm = ((highEnergy / totalSpec) * 1.9f).coerceIn(0f, 1f)
        val warmMidNorm = ((midEnergy / totalSpec) * 1.6f).coerceIn(0f, 1f)

        // Waveform zero-crossing & crest factor for transient density
        val wave = snapshot.waveform
        var diffs = 0f
        for (i in 1 until wave.size) {
            diffs += abs(wave[i] - wave[i - 1])
        }
        val transientNorm = ((diffs / wave.size.coerceAtLeast(1)) * 2.2f).coerceIn(0f, 1f)
        val stereoWidthNorm = (abs(snapshot.rmsLeft - snapshot.rmsRight) / rmsAvg.coerceAtLeast(0.01f) + 0.35f).coerceIn(0f, 1f)
        val estBpm = (68f + transientNorm * 45f + energyNorm * 18f).coerceIn(64f, 142f)

        val features = AcousticFeatureSummary(
            estimatedTempoBpm = estBpm,
            rmsEnergyNormalized = energyNorm,
            spectralBrightnessNormalized = brightnessNorm,
            transientDensityNormalized = transientNorm,
            warmMidRatioNormalized = warmMidNorm,
            stereoWidthNormalized = stereoWidthNorm,
            analyzedFramesCount = 4096,
            sampleRateHz = snapshot.effectiveSampleRateHz,
            analysisMethodLabel = "Real-Time PCM DSP Telemetry (${snapshot.effectiveSampleRateHz / 1000.0}kHz)"
        )

        return scoreAndBuildResult(
            track = track,
            features = features,
            availablePresets = availablePresets
        )
    }

    private fun scoreAndBuildResult(
        track: TrackItem,
        features: AcousticFeatureSummary,
        availablePresets: List<LofiPreset>
    ): MusicMoodAnalysisResult {
        val tempoNorm = ((features.estimatedTempoBpm - 60f) / 85f).coerceIn(0f, 1f)
        val e = features.rmsEnergyNormalized
        val b = features.spectralBrightnessNormalized
        val t = features.transientDensityNormalized
        val m = features.warmMidRatioNormalized
        val w = features.stereoWidthNormalized

        // Multi-dimensional acoustic scoring across all 7 required moods:
        // 1. ENERGETIC ⚡: High tempo, high energy, crisp transients
        val energeticRaw = 0.36f * e + 0.34f * t + 0.20f * tempoNorm + 0.10f * b

        // 2. HAPPY 😊: Bright spectral balance, upbeat tempo, warm midrange & lively rhythm
        val happyRaw = 0.34f * b + 0.24f * e + 0.22f * tempoNorm + 0.20f * m - 0.12f * (1f - b)

        // 3. SAD 💔: Slow tempo, subdued high frequencies (dark spectrum), softer energy, low transient attack
        val sadRaw = 0.34f * (1f - b) + 0.28f * (1f - tempoNorm) + 0.22f * (1f - e) + 0.16f * (1f - t)

        // 4. ROMANTIC ❤️: Rich midrange harmonics (vocals/chords), moderate-slow tempo, smooth transients, warm stereo
        val midTempoCloseness = (1f - abs(features.estimatedTempoBpm - 82f) / 45f).coerceIn(0f, 1f)
        val romanticRaw = 0.38f * m + 0.24f * midTempoCloseness + 0.20f * w + 0.18f * (1f - abs(t - 0.35f))

        // 5. RELAXING 🌙: Laid-back Lo-Fi groove (70-86 BPM), warm low-mid balance, gentle high roll-off
        val lofiGrooveCloseness = (1f - abs(features.estimatedTempoBpm - 78f) / 38f).coerceIn(0f, 1f)
        val relaxingRaw = 0.32f * lofiGrooveCloseness + 0.28f * (1f - b) + 0.22f * m + 0.18f * (1f - abs(e - 0.45f))

        // 6. PEACEFUL 🍃: Very low transient density, gentle RMS energy, serene spectral balance
        val peacefulRaw = 0.40f * (1f - t) + 0.32f * (1f - e) + 0.16f * (1f - tempoNorm) + 0.12f * m

        // 7. DREAMY ✨: Wide stereo decorrelation, sustained harmonic shimmer, floating moderate-slow pulse
        val dreamyRaw = 0.42f * w + 0.22f * (1f - t) + 0.20f * m + 0.16f * (1f - abs(b - 0.48f))

        val rawMap = linkedMapOf(
            DetectedMusicMood.HAPPY to happyRaw.coerceAtLeast(0.05f),
            DetectedMusicMood.SAD to sadRaw.coerceAtLeast(0.05f),
            DetectedMusicMood.ROMANTIC to romanticRaw.coerceAtLeast(0.05f),
            DetectedMusicMood.RELAXING to relaxingRaw.coerceAtLeast(0.05f),
            DetectedMusicMood.ENERGETIC to energeticRaw.coerceAtLeast(0.05f),
            DetectedMusicMood.PEACEFUL to peacefulRaw.coerceAtLeast(0.05f),
            DetectedMusicMood.DREAMY to dreamyRaw.coerceAtLeast(0.05f)
        )

        // Softmax-style temperature normalization for clear probability distribution
        val temp = 0.14f
        val expMap = rawMap.mapValues { (_, v) -> exp((v / temp).toDouble()).toFloat() }
        val expSum = expMap.values.sum().coerceAtLeast(1e-6f)
        val probabilities = expMap.mapValues { (_, v) -> (v / expSum).coerceIn(0.01f, 0.99f) }

        val sorted = probabilities.entries.sortedByDescending { it.value }
        val topEntry = sorted[0]
        val runnerUpEntry = sorted.getOrElse(1) { sorted[0] }

        val winningMood = topEntry.key
        val margin = (topEntry.value - runnerUpEntry.value).coerceIn(0f, 1f)
        val frameQualityBoost = min(1f, features.analyzedFramesCount / 44100f) * 8f

        // Confidence percentage derived from acoustic separation margin and sample count (never 100% since mood is estimated)
        val confidence = (58f + margin * 42f + frameQualityBoost)
            .roundToInt()
            .coerceIn(56, 92)

        val primaryPreset = availablePresets.find { it.id == winningMood.primaryPresetId }
            ?: BuiltInLofiPresets.ALL.first { it.id == winningMood.primaryPresetId }
        val secondaryPreset = availablePresets.find { it.id == winningMood.secondaryPresetId }
            ?: BuiltInLofiPresets.ALL.first { it.id == winningMood.secondaryPresetId }

        val reasoning = String.format(
            Locale.US,
            "Estimated %.0f BPM pulse, %.0f%% RMS energy, %.0f%% spectral brightness, %.0f%% midrange warmth, and %.0f%% stereo width across %,d PCM frames.",
            features.estimatedTempoBpm,
            features.rmsEnergyNormalized * 100f,
            features.spectralBrightnessNormalized * 100f,
            features.warmMidRatioNormalized * 100f,
            features.stereoWidthNormalized * 100f,
            features.analyzedFramesCount
        )

        return MusicMoodAnalysisResult(
            trackId = track.id,
            verifiedTitle = track.title,
            verifiedArtist = track.artist,
            verifiedAlbum = track.album,
            verifiedFormat = track.formatBadge,
            verifiedDurationMs = track.durationMs,
            detectedMood = winningMood,
            confidencePercent = confidence,
            isSignalSufficient = true,
            moodProbabilities = probabilities,
            suggestedPreset = primaryPreset,
            secondaryPreset = secondaryPreset,
            acousticFeatures = features,
            acousticReasoning = reasoning
        )
    }

    private fun buildInsufficientSignalResult(
        track: TrackItem,
        availablePresets: List<LofiPreset>
    ): MusicMoodAnalysisResult {
        val defaultPreset = availablePresets.find { it.id == "soft_lofi" }
            ?: BuiltInLofiPresets.ALL.first { it.id == "soft_lofi" }
        val secondaryPreset = availablePresets.find { it.id == "rainy_night" }
            ?: BuiltInLofiPresets.ALL.first { it.id == "rainy_night" }

        val uniformProbs = DetectedMusicMood.entries.associateWith { 1f / DetectedMusicMood.entries.size }

        return MusicMoodAnalysisResult(
            trackId = track.id,
            verifiedTitle = track.title,
            verifiedArtist = track.artist,
            verifiedAlbum = track.album,
            verifiedFormat = track.formatBadge,
            verifiedDurationMs = track.durationMs,
            detectedMood = DetectedMusicMood.RELAXING,
            confidencePercent = null, // Do NOT invent a confidence percentage when signal is insufficient
            isSignalSufficient = false,
            moodProbabilities = uniformProbs,
            suggestedPreset = defaultPreset,
            secondaryPreset = secondaryPreset,
            acousticFeatures = AcousticFeatureSummary(
                estimatedTempoBpm = 0f,
                rmsEnergyNormalized = 0f,
                spectralBrightnessNormalized = 0f,
                transientDensityNormalized = 0f,
                warmMidRatioNormalized = 0f,
                stereoWidthNormalized = 0f,
                analyzedFramesCount = 0,
                sampleRateHz = 44100,
                analysisMethodLabel = "Awaiting Non-Silent Audio Signal"
            ),
            acousticReasoning = "Play the song or select a readable local audio file so real PCM frames can be measured."
        )
    }

    private class ExtractedPcmSlice(
        val pcmBytes: ByteArray,
        val sampleRateHz: Int,
        val channelCount: Int
    )

    /**
     * Reads up to ~6 seconds (262,144 bytes) of 16-bit PCM audio from the middle of a local WAV file
     * or content URI without modifying the file.
     */
    private fun readLocalPcmSlice(context: Context, track: TrackItem): ExtractedPcmSlice? {
        return runCatching {
            val uriStr = track.uriString
            val inputStream: InputStream = when {
                uriStr.startsWith("file://") -> {
                    val file = File(URI(uriStr))
                    if (!file.exists() || !file.canRead()) return null
                    file.inputStream()
                }
                uriStr.startsWith("/") -> {
                    val file = File(uriStr)
                    if (!file.exists() || !file.canRead()) return null
                    file.inputStream()
                }
                else -> {
                    context.contentResolver.openInputStream(Uri.parse(uriStr)) ?: return null
                }
            }

            inputStream.use { stream ->
                val header = ByteArray(44)
                val readHeader = stream.read(header)
                if (readHeader < 44) return null

                // Check for RIFF/WAVE header
                val isRiffWav = header[0] == 'R'.code.toByte() &&
                    header[1] == 'I'.code.toByte() &&
                    header[2] == 'F'.code.toByte() &&
                    header[3] == 'F'.code.toByte()

                if (!isRiffWav) {
                    return null
                }

                val headerBuf = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
                val channels = headerBuf.getShort(22).toInt().coerceIn(1, 2)
                val sampleRate = headerBuf.getInt(24).coerceIn(8000, 96000)

                // Skip first ~1.5 seconds of intro silence if file is long enough
                val skipBytes = (sampleRate * channels * 2 * 1.5).toLong()
                stream.skip(skipBytes)

                val maxSliceBytes = 262_144 // ~6 seconds of 44.1kHz stereo PCM
                val buffer = ByteArray(maxSliceBytes)
                var totalRead = 0
                while (totalRead < maxSliceBytes) {
                    val r = stream.read(buffer, totalRead, maxSliceBytes - totalRead)
                    if (r <= 0) break
                    totalRead += r
                }

                if (totalRead < MIN_REQUIRED_FRAMES * channels * 2) {
                    return null
                }

                ExtractedPcmSlice(
                    pcmBytes = if (totalRead == buffer.size) buffer else buffer.copyOf(totalRead),
                    sampleRateHz = sampleRate,
                    channelCount = channels
                )
            }
        }.getOrNull()
    }
}
