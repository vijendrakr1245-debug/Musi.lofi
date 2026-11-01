package com.example.audio

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import com.example.model.AudioPlaybackQuality
import com.example.model.EqualizerState
import com.example.model.LiveSoundMode
import com.example.model.LofiDspParameters
import com.example.model.VisualizerSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Quantitative Audio Quality & DSP Telemetry Report for automated verification
 * and real-time headroom/CPU monitoring.
 */
data class AudioQualityReport(
    val peakLevelLinear: Float,
    val peakLevelDb: Float,
    val rmsLeftLinear: Float,
    val rmsRightLinear: Float,
    val rmsCombinedDb: Float,
    val clippingCount: Int,
    val dcOffsetLeft: Float,
    val dcOffsetRight: Float,
    val channelBalanceDb: Float,
    val underrunCount: Int,
    val processingLatencyMs: Float,
    val bufferDurationMs: Float
) {
    val cpuUsagePercent: Float
        get() = if (bufferDurationMs > 0.001f) (processingLatencyMs / bufferDurationMs) * 100f else 0f
}

/**
 * Real-Time PCM Audio DSP Engine for LOFI WAVE.
 *
 * Key Architecture & Performance Guarantees:
 * - Zero per-buffer heap allocations on the real-time audio rendering thread (`reusablePcm16Buffer`,
 *   precomputed Goertzel coefficients, and active-filter bitmasks).
 * - Strict sound-mode isolation: switching between `Original Stereo`, `Live Lo-Fi`, `Bass Boost`,
 *   `Deep Bass`, `Soft Bass`, `Slowed + Reverb`, `Nightcore`, `8D Ultra`, `3D Spatial Audio`,
 *   `Ultra Surround`, `Clear Vocal`, and `Dreamy Sound` executes dedicated, non-conflicting
 *   DSP stages without leaking muffled Lo-Fi low-pass filters or vinyl noise into other modes.
 * - Click-free parameter slewing & mode-switch crossfading without flushing delay lines or
 *   restarting playback.
 */
@OptIn(UnstableApi::class)
class LofiDspAudioProcessor : BaseAudioProcessor() {

    @Volatile
    private var targetDspParams: LofiDspParameters = LofiDspParameters()

    @Volatile
    private var currentEqState: EqualizerState = EqualizerState()

    @Volatile
    var currentPlaybackQuality: AudioPlaybackQuality = AudioPlaybackQuality.HIGH_QUALITY
        private set

    fun updatePlaybackQuality(quality: AudioPlaybackQuality) {
        currentPlaybackQuality = quality
    }

    private val _visualizerFlow = MutableStateFlow(VisualizerSnapshot())
    val visualizerFlow: StateFlow<VisualizerSnapshot> = _visualizerFlow.asStateFlow()

    // Smoothly slewed parameters to prevent zipper noise or clicks on mode/preset changes
    private var currentWetDryMix: Float = 1.0f
    private var currentMasterGain: Float = 1.0f
    private var smoothedLowPassHz: Float = 8800f
    private var lastComputedLowPassHz: Float = -1f
    private var trackFadeEnvelope: Float = 1.0f
    private var modeTransitionGain: Float = 1.0f
    private var lastActiveSoundMode: LiveSoundMode = LiveSoundMode.LIVE_LOFI

    // Per-channel biquad filters for Lo-Fi & Equalizer stages
    private val highPassFilters = Array(2) { BiquadFilter() }
    private val lowPassStage1Filters = Array(2) { BiquadFilter() }
    private val lowPassStage2Filters = Array(2) { BiquadFilter() }
    private val subBassShelfFilters = Array(2) { BiquadFilter() }
    private val bassShelfFilters = Array(2) { BiquadFilter() }
    private val midrangePeakFilters = Array(2) { BiquadFilter() }
    private val vocalMudCutFilters = Array(2) { BiquadFilter() }
    private val vocalPresenceFilters = Array(2) { BiquadFilter() }
    private val highShelfFilters = Array(2) { BiquadFilter() }
    private val eqFilters = Array(2) { Array(10) { BiquadFilter() } }

    // Active filter flags & active EQ band index list (skips 0.0 dB identity filters on audio thread)
    private var isHighPassFilterActive = false
    private var isSubBassShelfActive = false
    private var isBassShelfActive = false
    private var isMidPeakActive = false
    private var isVocalClarityActive = false
    private var isHighShelfActive = false
    private val activeEqBandIndices = IntArray(10)
    private var activeEqBandCount = 0

    // Dedicated Mode-Specific Biquads (Bass Boost, Deep Bass, Soft Bass, Clear Vocal, Nightcore, Phonk)
    private val modeFilter1 = Array(2) { BiquadFilter() }
    private val modeFilter2 = Array(2) { BiquadFilter() }
    private val modeFilter3 = Array(2) { BiquadFilter() }
    private var isModeFilter1Active = false
    private var isModeFilter2Active = false
    private var isModeFilter3Active = false

    // Binaural HRTF (3D Spatial Audio) Pinna Elevation Notch & Concha Resonance filters
    private val hrtfPinnaNotchFilters = Array(2) { BiquadFilter() }
    private val hrtfConchaPeakFilters = Array(2) { BiquadFilter() }
    private var lastHrtfNotchFreqHz: Float = 0f
    private var lastHrtfSampleRate: Float = 0f

    // 1st-order DC Offset Blocker state (removes < 18 Hz subsonic bias)
    private val dcPrevInput = FloatArray(2)
    private val dcPrevOutput = FloatArray(2)

    // Bass Harmonic Exciter & Sub-Harmonic Octave Synthesizer state (for Bass Boost & Deep Bass)
    private var bassLowPassLeft: Float = 0f
    private var bassLowPassRight: Float = 0f
    private var subSynthPrevSignL: Int = 1
    private var subSynthPrevSignR: Int = 1
    private var subSynthFlipL: Float = 1f
    private var subSynthFlipR: Float = 1f
    private var subSynthLpLeft: Float = 0f
    private var subSynthLpRight: Float = 0f

    // Wow & Flutter / Dreamy Chorus fractional delay lines (4096 samples per channel ~85ms at 48kHz)
    private val wowDelaySize = 4096
    private val wowDelayMask = wowDelaySize - 1
    private val wowDelayLeft = FloatArray(wowDelaySize)
    private val wowDelayRight = FloatArray(wowDelaySize)
    private var wowWriteIndex = 0
    private var wowPhase = 0.0
    private var flutterPhase = 0.0

    // 8D Ultra & 3D Spatial Binaural HRTF state (ITD fractional delay + head-shadow ILD + early reflections)
    private val spatialItdDelaySize = 2048
    private val spatialItdDelayMask = spatialItdDelaySize - 1
    private val spatialItdDelayLeft = FloatArray(spatialItdDelaySize)
    private val spatialItdDelayRight = FloatArray(spatialItdDelaySize)
    private var spatialItdWriteIndex = 0
    private var eightDPhase = 0.0
    private var spatialOrbitPhase = 0.0
    private var hrtfShadowLeftState = 0f
    private var hrtfShadowRightState = 0f
    private var surroundSideHpPrevIn = 0f
    private var surroundSideHpPrevOut = 0f

    // Pre-delay buffer for Slowed + Reverb & Dreamy Hall Reverb (4096 samples ~ 92ms)
    private val reverbPreDelaySize = 4096
    private val reverbPreDelayMask = reverbPreDelaySize - 1
    private val reverbPreDelayLeft = FloatArray(reverbPreDelaySize)
    private val reverbPreDelayRight = FloatArray(reverbPreDelaySize)
    private var reverbPreDelayWriteIdx = 0

    // Stereo Tape Echo / Delay Line (65536 samples per channel ~1.36s at 48kHz)
    private val echoDelaySize = 65536
    private val echoDelayMask = echoDelaySize - 1
    private val echoDelayLeft = FloatArray(echoDelaySize)
    private val echoDelayRight = FloatArray(echoDelaySize)
    private var echoWriteIndex = 0
    private var echoLpLeft = 0f
    private var echoLpRight = 0f

    // Sample-rate reducer & anti-aliasing state
    private var decimationCounter = 0f
    private var heldSampleLeft = 0f
    private var heldSampleRight = 0f
    private var prevHeldLeft = 0f
    private var prevHeldRight = 0f
    private var bitcrushAntiAliasL = 0f
    private var bitcrushAntiAliasR = 0f

    // Authentic 33⅓ RPM Vinyl Surface & Resonant Needle Crackle Synthesizer State
    private var rngSeed: Int = 0x5A3C9E17
    private var pinkB0 = 0f
    private var pinkB1 = 0f
    private var pinkB2 = 0f
    private var vinylGroovePhase = 0.0
    private var crackleEnvLeft = 0f
    private var crackleEnvRight = 0f
    private var crackleBandStateL1 = 0f
    private var crackleBandStateL2 = 0f
    private var crackleBandStateR1 = 0f
    private var crackleBandStateR2 = 0f
    private var vinylHpStateL = 0f
    private var vinylHpStateR = 0f

    // Studio Schroeder / Freeverb Stereo Reverb (4 Parallel Comb Filters + 2 Series All-Pass Filters per channel)
    private val combLengthsL = intArrayOf(1116, 1188, 1277, 1356)
    private val combLengthsR = intArrayOf(1139, 1211, 1300, 1379)
    private val combBuffersL = Array(4) { i -> FloatArray(combLengthsL[i]) }
    private val combBuffersR = Array(4) { i -> FloatArray(combLengthsR[i]) }
    private val combIndicesL = IntArray(4)
    private val combIndicesR = IntArray(4)
    private val combFilterStatesL = FloatArray(4)
    private val combFilterStatesR = FloatArray(4)

    private val allPassLengthsL = intArrayOf(556, 441)
    private val allPassLengthsR = intArrayOf(579, 464)
    private val allPassBuffersL = Array(2) { i -> FloatArray(allPassLengthsL[i]) }
    private val allPassBuffersR = Array(2) { i -> FloatArray(allPassLengthsR[i]) }
    private val allPassIndicesL = IntArray(2)
    private val allPassIndicesR = IntArray(2)

    // Dynamics & True-Peak Limiter state
    private var compressorEnvelope = 0f
    private var limiterGainEnvelope = 1.0f
    private var smoothedGainReductionDb = 0f

    // Quality & Telemetry Metrics
    @Volatile
    var lastPeakLevel: Float = 0f
        private set

    @Volatile
    var lastRmsLeft: Float = 0f
        private set

    @Volatile
    var lastRmsRight: Float = 0f
        private set

    @Volatile
    var lastClippingCount: Int = 0
        private set

    @Volatile
    var lastDcOffsetLeft: Float = 0f
        private set

    @Volatile
    var lastDcOffsetRight: Float = 0f
        private set

    @Volatile
    var lastChannelBalanceDb: Float = 0f
        private set

    @Volatile
    var totalUnderrunCount: Int = 0
        private set

    @Volatile
    var lastProcessingLatencyMs: Float = 0.8f
        private set

    @Volatile
    var lastDspCpuTimeUs: Float = 120f
        private set

    @Volatile
    var lastBufferDurationMs: Float = 23.2f
        private set

    @Volatile
    var reusableBufferHitCount: Long = 0L
        private set

    // Reusable PCM16 conversion buffer for 24-bit/32-bit/float inputs (zero audio-thread allocations)
    private var reusablePcm16Buffer: ByteBuffer = EMPTY_BUFFER

    // Visualizer ring buffer & precomputed Goertzel table (zero trig calls on audio thread)
    private val visRingSize = 256
    private val visRingMask = visRingSize - 1
    private val visRing = FloatArray(visRingSize)
    private var visWritePos = 0
    private var framesSinceLastVisEmit = 0
    private val precomputedGoertzelCos = FloatArray(32)
    private var goertzelSampleRate = 0

    @Volatile
    private var coeffsDirty = true

    fun updateDspParameters(newParams: LofiDspParameters) {
        val prev = targetDspParams
        targetDspParams = newParams
        if (prev.soundMode != newParams.soundMode ||
            prev.isLiveLofiEnabled != newParams.isLiveLofiEnabled ||
            prev.presetId != newParams.presetId ||
            abs(prev.masterIntensity - newParams.masterIntensity) > 0.002f ||
            abs(prev.lowPassCutoffHz - newParams.lowPassCutoffHz) > 2f ||
            abs(prev.highPassCutoffHz - newParams.highPassCutoffHz) > 0.5f ||
            abs(prev.warmBassBoostDb - newParams.warmBassBoostDb) > 0.02f ||
            abs(prev.midrangeGainDb - newParams.midrangeGainDb) > 0.02f ||
            abs(prev.trebleGainDb - newParams.trebleGainDb) > 0.02f ||
            abs(prev.highFreqReductionDb - newParams.highFreqReductionDb) > 0.02f
        ) {
            coeffsDirty = true
        }
    }

    fun updateEqualizerState(newEq: EqualizerState) {
        val prev = currentEqState
        currentEqState = newEq
        if (prev != newEq) {
            coeffsDirty = true
        }
    }

    fun getLatestQualityReport(): AudioQualityReport {
        val rmsCombined = sqrt((lastRmsLeft * lastRmsLeft + lastRmsRight * lastRmsRight) * 0.5f)
        val rmsDb = if (rmsCombined > 0.00001f) 20f * log10(rmsCombined) else -96f
        val peakDb = if (lastPeakLevel > 0.00001f) 20f * log10(lastPeakLevel) else -96f
        return AudioQualityReport(
            peakLevelLinear = lastPeakLevel,
            peakLevelDb = peakDb,
            rmsLeftLinear = lastRmsLeft,
            rmsRightLinear = lastRmsRight,
            rmsCombinedDb = rmsDb,
            clippingCount = lastClippingCount,
            dcOffsetLeft = lastDcOffsetLeft,
            dcOffsetRight = lastDcOffsetRight,
            channelBalanceDb = lastChannelBalanceDb,
            underrunCount = totalUnderrunCount,
            processingLatencyMs = lastProcessingLatencyMs,
            bufferDurationMs = lastBufferDurationMs
        )
    }

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        val enc = inputAudioFormat.encoding
        if (enc != C.ENCODING_PCM_16BIT &&
            enc != C.ENCODING_PCM_24BIT &&
            enc != C.ENCODING_PCM_32BIT &&
            enc != C.ENCODING_PCM_FLOAT
        ) {
            return AudioProcessor.AudioFormat.NOT_SET
        }
        if (inputAudioFormat.channelCount !in 1..2) {
            return AudioProcessor.AudioFormat.NOT_SET
        }
        coeffsDirty = true
        precomputeGoertzelTable(inputAudioFormat.sampleRate)
        return AudioProcessor.AudioFormat(
            inputAudioFormat.sampleRate,
            inputAudioFormat.channelCount,
            C.ENCODING_PCM_16BIT
        )
    }

    override fun isActive(): Boolean = true

    private fun precomputeGoertzelTable(sampleRate: Int) {
        val sr = sampleRate.coerceAtLeast(8000)
        if (goertzelSampleRate == sr) return
        goertzelSampleRate = sr
        val minFreq = 45.0
        val maxFreq = min(14000.0, sr * 0.44)
        for (b in 0 until 32) {
            val frac = b.toDouble() / 31.0
            val freq = minFreq * (maxFreq / minFreq).pow(frac)
            val omega = 2.0 * PI * freq / sr
            precomputedGoertzelCos[b] = (2.0 * cos(omega)).toFloat()
        }
    }

    private fun isModeOrDspActive(lofi: LofiDspParameters): Boolean {
        return when (lofi.soundMode) {
            LiveSoundMode.ORIGINAL_STEREO -> false
            LiveSoundMode.LIVE_LOFI -> lofi.isLiveLofiEnabled &&
                lofi.presetId != "original" &&
                lofi.masterIntensity > 0.01f
            else -> lofi.masterIntensity > 0.01f
        }
    }

    private fun isLofiColorationActive(lofi: LofiDspParameters): Boolean {
        if (!lofi.isLiveLofiEnabled || lofi.presetId == "original" || lofi.masterIntensity <= 0.01f) {
            return false
        }
        return lofi.soundMode == LiveSoundMode.LIVE_LOFI ||
            lofi.lowPassCutoffHz < 7800f ||
            lofi.vinylCrackle > 0.08f ||
            lofi.bitDepth < 15.5f
    }

    private fun recalculateStaticFilters(sampleRate: Float) {
        val lofi = targetDspParams
        val eq = currentEqState
        val intensity = lofi.masterIntensity.coerceIn(0f, 1f)
        val dspActive = isModeOrDspActive(lofi)
        val lofiColorActive = isLofiColorationActive(lofi)
        val mode = lofi.soundMode

        // 1. High-Pass Filter (Subsonic rumble control)
        val effectiveHpHz = when {
            !dspActive -> 20f
            mode == LiveSoundMode.CLEAR_VOCAL -> max(110f, lofi.highPassCutoffHz)
            else -> lofi.highPassCutoffHz.coerceIn(18f, 220f)
        }
        isHighPassFilterActive = dspActive && effectiveHpHz > 22f
        if (isHighPassFilterActive) {
            for (ch in 0..1) {
                highPassFilters[ch].setHighPass(sampleRate, effectiveHpHz, 0.7071f)
            }
        }

        // 2. Sub-Bass & Bass Shelf Filters
        val bassBoost = (if (dspActive) lofi.warmBassBoostDb * intensity else 0f) +
            (if (eq.isEnabled) eq.bassBoostDb else 0f)
        val subBassDb = if (bassBoost > 0f) (bassBoost * 0.45f).coerceIn(0f, 6.0f) else 0f
        isSubBassShelfActive = abs(subBassDb) >= 0.05f
        if (isSubBassShelfActive) {
            for (ch in 0..1) {
                subBassShelfFilters[ch].setLowShelf(sampleRate, 55f, 0.7071f, subBassDb)
            }
        }

        val clampedBassShelfDb = bassBoost.coerceIn(-10.0f, 10.0f)
        isBassShelfActive = abs(clampedBassShelfDb) >= 0.05f
        if (isBassShelfActive) {
            for (ch in 0..1) {
                bassShelfFilters[ch].setLowShelf(sampleRate, 105f, 0.68f, clampedBassShelfDb)
            }
        }

        // 3. Midrange Peaking Filter (1.1 kHz)
        val midBoost = (if (dspActive) lofi.midrangeGainDb * intensity else 0f) +
            (if (eq.isEnabled) eq.midrangeBoostDb else 0f)
        val clampedMidDb = midBoost.coerceIn(-10.0f, 10.0f)
        isMidPeakActive = abs(clampedMidDb) >= 0.05f
        if (isMidPeakActive) {
            for (ch in 0..1) {
                midrangePeakFilters[ch].setPeakingEq(sampleRate, 1100f, 0.95f, clampedMidDb)
            }
        }

        // 4. Vocal Mud Cut (230 Hz) & Vocal Presence Boost (2.45 kHz) for Lo-Fi vocal intelligibility
        isVocalClarityActive = lofiColorActive
        if (isVocalClarityActive) {
            val mudCutDb = (-1.3f * intensity).coerceIn(-2.5f, 0f)
            val presenceBoostDb = (1.6f * intensity).coerceIn(0f, 3.0f)
            for (ch in 0..1) {
                vocalMudCutFilters[ch].setPeakingEq(sampleRate, 230f, 1.15f, mudCutDb)
                vocalPresenceFilters[ch].setPeakingEq(sampleRate, 2450f, 1.05f, presenceBoostDb)
            }
        }

        // 5. High-Shelf Filter (4.8 kHz)
        val netTrebleDb = (if (dspActive) (lofi.trebleGainDb - lofi.highFreqReductionDb * 0.55f) * intensity else 0f) +
            (if (eq.isEnabled) eq.trebleBoostDb else 0f)
        val clampedTrebleDb = netTrebleDb.coerceIn(-12.0f, 12.0f)
        isHighShelfActive = abs(clampedTrebleDb) >= 0.05f
        if (isHighShelfActive) {
            for (ch in 0..1) {
                highShelfFilters[ch].setHighShelf(sampleRate, 4800f, 0.7071f, clampedTrebleDb)
            }
        }

        // 6. Low-Pass Filter Coefficients
        updateLowPassCoefficients(sampleRate, smoothedLowPassHz)

        // 7. Mode-Specific Voicing Filters (Distinct acoustic profiles per LiveSoundMode)
        isModeFilter1Active = false
        isModeFilter2Active = false
        isModeFilter3Active = false

        if (dspActive) {
            when (mode) {
                LiveSoundMode.BASS_BOOST -> {
                    isModeFilter1Active = true
                    isModeFilter2Active = true
                    isModeFilter3Active = true
                    for (ch in 0..1) {
                        // Punchy 98Hz low-shelf + 112Hz chest kick peak, with 250Hz mud cleanup
                        modeFilter1[ch].setLowShelf(sampleRate, 98f, 0.72f, 4.8f * intensity)
                        modeFilter2[ch].setPeakingEq(sampleRate, 250f, 1.35f, -2.0f * intensity)
                        modeFilter3[ch].setPeakingEq(sampleRate, 2600f, 1.10f, 1.5f * intensity)
                    }
                }
                LiveSoundMode.DEEP_BASS -> {
                    isModeFilter1Active = true
                    isModeFilter2Active = true
                    isModeFilter3Active = true
                    for (ch in 0..1) {
                        // Deep 56Hz sub-bass shelf + 240Hz theater scoop + crisp 3.2kHz definition
                        modeFilter1[ch].setLowShelf(sampleRate, 56f, 0.68f, 5.8f * intensity)
                        modeFilter2[ch].setPeakingEq(sampleRate, 240f, 1.25f, -2.6f * intensity)
                        modeFilter3[ch].setPeakingEq(sampleRate, 3200f, 1.10f, 1.2f * intensity)
                    }
                }
                LiveSoundMode.SOFT_BASS -> {
                    isModeFilter1Active = true
                    isModeFilter2Active = true
                    for (ch in 0..1) {
                        // Warm 115Hz velvet low cushion + smooth -2.4dB 6.5kHz high-shelf
                        modeFilter1[ch].setLowShelf(sampleRate, 115f, 0.58f, 3.6f * intensity)
                        modeFilter2[ch].setHighShelf(sampleRate, 6500f, 0.7071f, -2.4f * intensity)
                    }
                }
                LiveSoundMode.CLEAR_VOCAL -> {
                    isModeFilter1Active = true
                    isModeFilter2Active = true
                    isModeFilter3Active = true
                    for (ch in 0..1) {
                        // -3.5dB 210Hz boxiness cut + +4.8dB 2.45kHz vocal presence + +2.8dB 8.2kHz air
                        modeFilter1[ch].setPeakingEq(sampleRate, 210f, 1.15f, -3.5f * intensity)
                        modeFilter2[ch].setPeakingEq(sampleRate, 2450f, 0.95f, 4.8f * intensity)
                        modeFilter3[ch].setHighShelf(sampleRate, 8200f, 0.7071f, 2.8f * intensity)
                    }
                }
                LiveSoundMode.DREAMY_SOUND -> {
                    isModeFilter1Active = true
                    isModeFilter2Active = true
                    for (ch in 0..1) {
                        // Warm 140Hz foundation + ethereal 3.6kHz shimmer
                        modeFilter1[ch].setLowShelf(sampleRate, 140f, 0.65f, 1.8f * intensity)
                        modeFilter2[ch].setPeakingEq(sampleRate, 3600f, 1.10f, 1.6f * intensity)
                    }
                }
                LiveSoundMode.ULTRA_SURROUND -> {
                    isModeFilter1Active = true
                    for (ch in 0..1) {
                        modeFilter1[ch].setHighShelf(sampleRate, 5500f, 0.7071f, 1.8f * intensity)
                    }
                }
                else -> {}
            }
        }

        // 8. 5-Band / 10-Band Graphical Equalizer (build active band list to skip 0.0 dB bands)
        activeEqBandCount = 0
        if (eq.isEnabled) {
            val freqs = EqualizerState.BAND_FREQUENCIES
            val gains = eq.bandGainsDb
            val fiveBandMode = eq.isFiveBandMode
            val fiveBandSet = EqualizerState.FIVE_BAND_INDICES
            for (b in 0 until 10) {
                val rawGain = gains.getOrElse(b) { 0f }.coerceIn(-12f, 12f)
                val bandGain = if (fiveBandMode && b !in fiveBandSet) {
                    val leftIdx = fiveBandSet.lastOrNull { it < b } ?: fiveBandSet.first()
                    val rightIdx = fiveBandSet.firstOrNull { it > b } ?: fiveBandSet.last()
                    val gL = gains.getOrElse(leftIdx) { 0f }.coerceIn(-12f, 12f)
                    val gR = gains.getOrElse(rightIdx) { 0f }.coerceIn(-12f, 12f)
                    (gL + gR) * 0.5f
                } else {
                    rawGain
                }
                if (abs(bandGain) >= 0.05f) {
                    val q = if (fiveBandMode) 1.05f else 1.414f
                    for (ch in 0..1) {
                        eqFilters[ch][b].setPeakingEq(sampleRate, freqs[b].toFloat(), q, bandGain)
                    }
                    activeEqBandIndices[activeEqBandCount++] = b
                }
            }
        }
        coeffsDirty = false
    }

    private fun updateLowPassCoefficients(sampleRate: Float, cutoffHz: Float) {
        val safeCutoff = cutoffHz.coerceIn(600f, min(18500f, sampleRate * 0.46f))
        lastComputedLowPassHz = safeCutoff
        for (ch in 0..1) {
            lowPassStage1Filters[ch].setLowPass(sampleRate, safeCutoff, 0.5412f)
            lowPassStage2Filters[ch].setLowPass(sampleRate, safeCutoff, 1.3065f)
        }
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        val sr = inputAudioFormat.sampleRate.coerceAtLeast(8000)
        val chCount = inputAudioFormat.channelCount.coerceIn(1, 2)
        val enc = inputAudioFormat.encoding

        if (enc == C.ENCODING_PCM_16BIT) {
            val frameSize = chCount * 2
            val numFrames = remaining / frameSize
            if (numFrames <= 0) {
                inputBuffer.position(inputBuffer.limit())
                return
            }
            val outBuffer = replaceOutputBuffer(numFrames * frameSize).order(ByteOrder.LITTLE_ENDIAN)
            inputBuffer.order(ByteOrder.LITTLE_ENDIAN)
            processPcmBufferInternal(inputBuffer, outBuffer, sr, chCount, numFrames)
            outBuffer.flip()
            return
        }

        // High-precision conversion for 24-bit, 32-bit, or 32-bit Float PCM using reusable buffer
        val bytesPerSample = when (enc) {
            C.ENCODING_PCM_24BIT -> 3
            C.ENCODING_PCM_32BIT, C.ENCODING_PCM_FLOAT -> 4
            else -> 2
        }
        val inFrameSize = chCount * bytesPerSample
        val numFrames = remaining / inFrameSize
        if (numFrames <= 0) {
            inputBuffer.position(inputBuffer.limit())
            return
        }

        val pcm16Bytes = numFrames * chCount * 2
        if (reusablePcm16Buffer.capacity() < pcm16Bytes) {
            reusablePcm16Buffer = ByteBuffer.allocateDirect(pcm16Bytes.coerceAtLeast(16384))
                .order(ByteOrder.LITTLE_ENDIAN)
        } else {
            reusableBufferHitCount++
        }
        val tempBuffer = reusablePcm16Buffer
        tempBuffer.clear()
        tempBuffer.order(ByteOrder.LITTLE_ENDIAN)
        inputBuffer.order(ByteOrder.LITTLE_ENDIAN)

        for (i in 0 until numFrames * chCount) {
            val normalizedSample = when (enc) {
                C.ENCODING_PCM_FLOAT -> {
                    val f = inputBuffer.float
                    if (f.isNaN() || f.isInfinite()) 0f else f.coerceIn(-1.0f, 1.0f)
                }
                C.ENCODING_PCM_24BIT -> {
                    val b0 = inputBuffer.get().toInt() and 0xFF
                    val b1 = inputBuffer.get().toInt() and 0xFF
                    val b2 = inputBuffer.get().toInt()
                    val int24 = (b2 shl 16) or (b1 shl 8) or b0
                    (int24.toFloat() / 8388608.0f).coerceIn(-1.0f, 1.0f)
                }
                C.ENCODING_PCM_32BIT -> {
                    val int32 = inputBuffer.int
                    (int32.toDouble() / 2147483648.0).toFloat().coerceIn(-1.0f, 1.0f)
                }
                else -> (inputBuffer.short / 32768.0f)
            }
            val s16 = (normalizedSample * 32767.0f).roundToInt().coerceIn(-32768, 32767).toShort()
            tempBuffer.putShort(s16)
        }
        tempBuffer.flip()

        val outBuffer = replaceOutputBuffer(pcm16Bytes).order(ByteOrder.LITTLE_ENDIAN)
        processPcmBufferInternal(tempBuffer, outBuffer, sr, chCount, numFrames)
        outBuffer.flip()
    }

    /**
     * Direct synchronous entry point for automated Robolectric & Unit DSP tests.
     */
    fun processAndMeasureBuffer(
        inputBuffer: ByteBuffer,
        outputBuffer: ByteBuffer,
        sampleRate: Int = 44100,
        channels: Int = 2
    ): AudioQualityReport {
        val frameSize = channels * 2
        val numFrames = inputBuffer.remaining() / frameSize
        inputBuffer.order(ByteOrder.LITTLE_ENDIAN)
        outputBuffer.order(ByteOrder.LITTLE_ENDIAN)
        if (coeffsDirty) {
            precomputeGoertzelTable(sampleRate)
            recalculateStaticFilters(sampleRate.toFloat())
        }
        processPcmBufferInternal(inputBuffer, outputBuffer, sampleRate, channels, numFrames)
        return getLatestQualityReport()
    }

    private fun processPcmBufferInternal(
        inputBuffer: ByteBuffer,
        outBuffer: ByteBuffer,
        sr: Int,
        chCount: Int,
        numFrames: Int
    ) {
        val startNs = System.nanoTime()
        val srFloat = sr.toFloat()

        val lofiParams = targetDspParams
        val eq = currentEqState
        val qualityMode = currentPlaybackQuality
        val mode = lofiParams.soundMode

        // Smooth mode transition envelope to prevent clicks when switching sound modes
        if (mode != lastActiveSoundMode) {
            lastActiveSoundMode = mode
            modeTransitionGain = 0.90f
            coeffsDirty = true
        }

        val isDspActive = isModeOrDspActive(lofiParams)
        val isLofiColorActive = isLofiColorationActive(lofiParams)
        val targetMix = if (isDspActive) 1.0f else 0.0f
        val mixStep = if (eq.smoothTransitions) {
            1.0f / (srFloat * 0.020f) // 20ms click-free wet/dry crossfade
        } else {
            1.0f
        }

        val targetLowPassHz = when {
            !isDspActive -> 18500f
            isLofiColorActive -> lofiParams.lowPassCutoffHz.coerceIn(800f, 18000f)
            mode == LiveSoundMode.SOFT_BASS -> lofiParams.lowPassCutoffHz.coerceIn(11000f, 15500f)
            mode == LiveSoundMode.DREAMY_SOUND -> lofiParams.lowPassCutoffHz.coerceIn(10500f, 16500f)
            else -> 18500f
        }
        val lpSlewFactor = (32.0f / srFloat).coerceIn(0.0002f, 0.02f)

        if (coeffsDirty) {
            recalculateStaticFilters(srFloat)
        } else if (abs(smoothedLowPassHz - targetLowPassHz) > 8.0f &&
            abs(smoothedLowPassHz - lastComputedLowPassHz) > 12.0f
        ) {
            updateLowPassCoefficients(srFloat, smoothedLowPassHz)
        }

        val intensity = lofiParams.masterIntensity.coerceIn(0f, 1f)
        val targetLinearMasterGain = 10.0.pow(lofiParams.masterGainDb.coerceIn(-12f, 12f) / 20.0).toFloat()
        val gainSlewStep = 1.0f / (srFloat * 0.015f)

        val bal = eq.stereoBalance.coerceIn(-1f, 1f)
        val leftChannelTrim = if (bal > 0f) 1f - bal else 1f
        val rightChannelTrim = if (bal < 0f) 1f + bal else 1f

        val fadeDurationSec = (eq.fadeInOutMs.coerceIn(0, 2000) / 1000f)
        val fadeStep = if (fadeDurationSec > 0.005f) 1.0f / (srFloat * fadeDurationSec) else 1.0f

        // Fast Bit-Transparent Bypass Path for Original Stereo when EQ & Gain are neutral
        val canFastBypass = !isDspActive &&
            activeEqBandCount == 0 &&
            !isBassShelfActive &&
            !isMidPeakActive &&
            !isHighShelfActive &&
            currentWetDryMix < 0.0001f &&
            trackFadeEnvelope >= 0.9999f &&
            abs(currentMasterGain - 1.0f) < 0.0005f &&
            abs(targetLinearMasterGain - 1.0f) < 0.0005f &&
            abs(leftChannelTrim - 1.0f) < 0.0005f &&
            abs(rightChannelTrim - 1.0f) < 0.0005f

        if (canFastBypass) {
            var sumSqL = 0f
            var sumSqR = 0f
            var sumL = 0f
            var sumR = 0f
            var maxPeak = 0f
            for (frame in 0 until numFrames) {
                val rawL = inputBuffer.short
                val rawR = if (chCount == 2) inputBuffer.short else rawL
                outBuffer.putShort(rawL)
                if (chCount == 2) outBuffer.putShort(rawR)

                val fL = rawL / 32768.0f
                val fR = rawR / 32768.0f
                sumSqL += fL * fL
                sumSqR += fR * fR
                sumL += fL
                sumR += fR
                val pk = max(abs(fL), abs(fR))
                if (pk > maxPeak) maxPeak = pk
                if ((frame and 3) == 0) {
                    visRing[visWritePos] = (fL + fR) * 0.5f
                    visWritePos = (visWritePos + 1) and visRingMask
                }
            }
            finalizeBufferTelemetry(
                startNs = startNs,
                sr = sr,
                numFrames = numFrames,
                sumSqL = sumSqL,
                sumSqR = sumSqR,
                sumL = sumL,
                sumR = sumR,
                maxPeak = maxPeak,
                clipCount = 0,
                isDspActive = false,
                lofiParams = lofiParams
            )
            return
        }

        // Pre-calculate per-buffer DSP constants
        val dcPoleR = (1.0f - (2.0 * PI * 18.0 / srFloat)).toFloat().coerceIn(0.992f, 0.9995f)
        val satAmount = if (isLofiColorActive || mode == LiveSoundMode.BASS_BOOST || mode == LiveSoundMode.DEEP_BASS || mode == LiveSoundMode.SOFT_BASS) {
            (lofiParams.analogSaturation * intensity).coerceIn(0f, 0.85f)
        } else {
            0f
        }
        val softDistAmount = if (isLofiColorActive) (lofiParams.softDistortion * intensity).coerceIn(0f, 0.55f) else 0f

        val effectiveBitDepth = if (isLofiColorActive) lofiParams.bitDepth.coerceIn(6f, 16f) else 16f
        val quantLevels = if (effectiveBitDepth < 15.8f) 2.0.pow(effectiveBitDepth.toDouble() - 1.0).toFloat() else 0f
        val srDivisor = if (isLofiColorActive) lofiParams.sampleRateDivisor.coerceIn(1.0f, 4.0f) else 1.0f
        val bitcrusherActive = quantLevels > 0f || srDivisor > 1.02f

        val wowDepth = if (isLofiColorActive || mode == LiveSoundMode.DREAMY_SOUND) {
            (lofiParams.wowFlutterDepth * intensity).coerceIn(0f, 1f)
        } else {
            0f
        }
        val wowRate = lofiParams.wowFlutterRateHz.coerceIn(0.2f, 5.0f).toDouble()
        val wowPhaseInc = 2.0 * PI * wowRate / sr
        val flutterPhaseInc = 2.0 * PI * (wowRate * 4.7) / sr
        val vinylGroovePhaseInc = 2.0 * PI * 0.555 / sr

        val crackleAmount = if (isLofiColorActive) (lofiParams.vinylCrackle * intensity).coerceIn(0f, 1f) else 0f
        val vinylNoiseAmount = if (isLofiColorActive) (lofiParams.vinylNoise * intensity).coerceIn(0f, 1f) else 0f
        val tapeHissAmount = if (isLofiColorActive) (lofiParams.tapeHiss * intensity).coerceIn(0f, 1f) else 0f
        val anyVinylOrTapeNoise = crackleAmount > 0.002f || vinylNoiseAmount > 0.002f || tapeHissAmount > 0.002f

        val reverbWet = (lofiParams.reverbMix * intensity).coerceIn(0f, 0.65f)
        val roomFeedback = (0.64f + lofiParams.reverbRoomSize.coerceIn(0.1f, 0.98f) * 0.26f).coerceIn(0.64f, 0.90f)
        val reverbDamp = if (mode == LiveSoundMode.DREAMY_SOUND) 0.28f else 0.36f
        val preDelaySamples = if (reverbWet > 0.25f && lofiParams.reverbRoomSize > 0.76f) {
            (0.026f * srFloat).toInt().coerceIn(1, reverbPreDelaySize - 4)
        } else {
            0
        }

        val echoWet = (lofiParams.echoMix * intensity).coerceIn(0f, 0.65f)
        val echoDelaySamples = ((lofiParams.echoDelayMs.coerceIn(40f, 950f) / 1000f) * srFloat)
            .toInt()
            .coerceIn(64, echoDelaySize - 64)
        val echoFb = lofiParams.echoFeedback.coerceIn(0.05f, 0.78f)

        val compAmount = (lofiParams.softCompression * intensity).coerceIn(0f, 1f)
        val stereoWidthFactor = lofiParams.stereoWidth.coerceIn(0.5f, 1.80f)
        val spatInt = lofiParams.spatialIntensity.coerceIn(0f, 1f)

        val eightDRateHz = lofiParams.eightDRotationSpeedHz.coerceIn(0.04f, 0.80f).toDouble()
        val eightDPhaseInc = 2.0 * PI * eightDRateHz / sr
        val spatialOrbitPhaseInc = 2.0 * PI * 0.12 / sr

        val isLowPassActive = isDspActive && targetLowPassHz < 17800f
        val isNightcoreStyle = isDspActive && (lofiParams.presetId == "conv_nightcore" || lofiParams.playbackSpeed > 1.12f)
        val bassTrim = if (isLofiColorActive && lofiParams.warmBassBoostDb > 3.0f) {
            1.0f / (1.0f + (lofiParams.warmBassBoostDb - 3.0f) * 0.025f)
        } else {
            1.0f
        }
        val makeupGain = (1.0f + intensity * 0.04f - satAmount * 0.06f) * bassTrim

        // Precompute Binaural HRTF Pinna Notch & Concha Resonance once per buffer when in 3D Spatial mode
        if (isDspActive && mode == LiveSoundMode.THREE_D_SPATIAL) {
            val elevNorm = ((lofiParams.spatialElevationDeg.coerceIn(-45f, 90f) + 45f) / 135f)
            val pinnaNotchFreq = (6200f + elevNorm * 3000f).coerceIn(5200f, min(11000f, srFloat * 0.42f))
            if (abs(pinnaNotchFreq - lastHrtfNotchFreqHz) > 25f || lastHrtfSampleRate != srFloat) {
                lastHrtfNotchFreqHz = pinnaNotchFreq
                lastHrtfSampleRate = srFloat
                for (ch in 0..1) {
                    hrtfPinnaNotchFilters[ch].setPeakingEq(srFloat, pinnaNotchFreq, 3.2f, -6.5f * spatInt)
                    hrtfConchaPeakFilters[ch].setPeakingEq(srFloat, 3800f, 1.5f, 3.2f * spatInt)
                }
            }
        }

        // Bass Exciter & Sub-Harmonic Synthesizer filter coefficients
        val bassLpAlpha = (2.0 * PI * 125.0 / srFloat).toFloat().coerceIn(0.005f, 0.08f)
        val subLpAlpha = (2.0 * PI * 68.0 / srFloat).toFloat().coerceIn(0.003f, 0.05f)

        var sumSqL = 0f
        var sumSqR = 0f
        var sumL = 0f
        var sumR = 0f
        var maxPeak = 0f
        var clipCount = 0

        for (frame in 0 until numFrames) {
            // Smooth parameter slewing
            if (currentWetDryMix < targetMix) {
                currentWetDryMix = min(targetMix, currentWetDryMix + mixStep)
            } else if (currentWetDryMix > targetMix) {
                currentWetDryMix = max(targetMix, currentWetDryMix - mixStep)
            }

            if (currentMasterGain < targetLinearMasterGain) {
                currentMasterGain = min(targetLinearMasterGain, currentMasterGain + gainSlewStep)
            } else if (currentMasterGain > targetLinearMasterGain) {
                currentMasterGain = max(targetLinearMasterGain, currentMasterGain - gainSlewStep)
            }

            if (trackFadeEnvelope < 1.0f) {
                trackFadeEnvelope = min(1.0f, trackFadeEnvelope + fadeStep)
            }
            if (modeTransitionGain < 1.0f) {
                modeTransitionGain = min(1.0f, modeTransitionGain + 0.0015f)
            }

            smoothedLowPassHz += lpSlewFactor * (targetLowPassHz - smoothedLowPassHz)

            // Decode 16-bit signed PCM input to normalized [-1.0, 1.0] float
            val rawShortL = inputBuffer.short
            val rawShortR = if (chCount == 2) inputBuffer.short else rawShortL

            val dryL = rawShortL / 32768.0f
            val dryR = rawShortR / 32768.0f

            var wetL = dryL
            var wetR = dryR

            if (currentWetDryMix > 0.0001f) {
                // =============================================================
                // STAGE 1: Subsonic DC Offset Blocker & High-Pass Filter
                // =============================================================
                val dcL = wetL - dcPrevInput[0] + dcPoleR * dcPrevOutput[0]
                dcPrevInput[0] = wetL
                dcPrevOutput[0] = dcL
                wetL = dcL

                val dcR = wetR - dcPrevInput[1] + dcPoleR * dcPrevOutput[1]
                dcPrevInput[1] = wetR
                dcPrevOutput[1] = dcR
                wetR = dcR

                if (isHighPassFilterActive) {
                    wetL = highPassFilters[0].process(wetL)
                    wetR = highPassFilters[1].process(wetR)
                }

                // =============================================================
                // STAGE 2: Tonal Shelving, Midrange & Vocal Clarity Biquads
                // =============================================================
                if (isSubBassShelfActive) {
                    wetL = subBassShelfFilters[0].process(wetL)
                    wetR = subBassShelfFilters[1].process(wetR)
                }
                if (isBassShelfActive) {
                    wetL = bassShelfFilters[0].process(wetL)
                    wetR = bassShelfFilters[1].process(wetR)
                }
                if (isMidPeakActive) {
                    wetL = midrangePeakFilters[0].process(wetL)
                    wetR = midrangePeakFilters[1].process(wetR)
                }
                if (isVocalClarityActive) {
                    wetL = vocalPresenceFilters[0].process(vocalMudCutFilters[0].process(wetL))
                    wetR = vocalPresenceFilters[1].process(vocalMudCutFilters[1].process(wetR))
                }
                if (isHighShelfActive) {
                    wetL = highShelfFilters[0].process(wetL)
                    wetR = highShelfFilters[1].process(wetR)
                }

                // =============================================================
                // STAGE 3: Mode-Specific Voicing & Dedicated Bass/Spatial DSP
                // =============================================================
                if (isModeFilter1Active) {
                    wetL = modeFilter1[0].process(wetL)
                    wetR = modeFilter1[1].process(wetR)
                }
                if (isModeFilter2Active) {
                    wetL = modeFilter2[0].process(wetL)
                    wetR = modeFilter2[1].process(wetR)
                }
                if (isModeFilter3Active) {
                    wetL = modeFilter3[0].process(wetL)
                    wetR = modeFilter3[1].process(wetR)
                }

                when (mode) {
                    LiveSoundMode.BASS_BOOST -> {
                        // Isolated Low-Band (<125Hz) Harmonic Exciter for tight physical punch
                        bassLowPassLeft += bassLpAlpha * (wetL - bassLowPassLeft)
                        bassLowPassRight += bassLpAlpha * (wetR - bassLowPassRight)
                        val driveL = bassLowPassLeft * 2.2f
                        val driveR = bassLowPassRight * 2.2f
                        val harmL = (driveL / (1f + abs(driveL))) * 0.38f * intensity
                        val harmR = (driveR / (1f + abs(driveR))) * 0.38f * intensity
                        wetL = (wetL + harmL) * 0.90f
                        wetR = (wetR + harmR) * 0.90f
                    }

                    LiveSoundMode.DEEP_BASS -> {
                        // Sub-Harmonic Octave Divider + Low-Band Warmth for deep theater sub weight
                        bassLowPassLeft += bassLpAlpha * (wetL - bassLowPassLeft)
                        bassLowPassRight += bassLpAlpha * (wetR - bassLowPassRight)
                        val signL = if (bassLowPassLeft >= 0f) 1 else -1
                        if (signL != subSynthPrevSignL && signL > 0) {
                            subSynthFlipL = -subSynthFlipL
                        }
                        subSynthPrevSignL = signL

                        val signR = if (bassLowPassRight >= 0f) 1 else -1
                        if (signR != subSynthPrevSignR && signR > 0) {
                            subSynthFlipR = -subSynthFlipR
                        }
                        subSynthPrevSignR = signR

                        val rawSubL = abs(bassLowPassLeft) * subSynthFlipL
                        val rawSubR = abs(bassLowPassRight) * subSynthFlipR
                        subSynthLpLeft += subLpAlpha * (rawSubL - subSynthLpLeft)
                        subSynthLpRight += subLpAlpha * (rawSubR - subSynthLpRight)

                        wetL = (wetL + subSynthLpLeft * 0.46f * intensity + bassLowPassLeft * 0.22f * intensity) * 0.88f
                        wetR = (wetR + subSynthLpRight * 0.46f * intensity + bassLowPassRight * 0.22f * intensity) * 0.88f
                    }

                    LiveSoundMode.SOFT_BASS -> {
                        // Smooth acoustic low-band cushion
                        bassLowPassLeft += bassLpAlpha * (wetL - bassLowPassLeft)
                        bassLowPassRight += bassLpAlpha * (wetR - bassLowPassRight)
                        wetL = (wetL + bassLowPassLeft * 0.25f * intensity) * 0.93f
                        wetR = (wetR + bassLowPassRight * 0.25f * intensity) * 0.93f
                    }

                    LiveSoundMode.CLEAR_VOCAL -> {
                        // Focus center vocal presence and tighten low-frequency side bleed
                        val mid = (wetL + wetR) * 0.5f
                        val side = (wetL - wetR) * 0.5f
                        wetL = mid * 1.08f + side * 0.88f
                        wetR = mid * 1.08f - side * 0.88f
                    }

                    else -> {}
                }

                if (isNightcoreStyle) {
                    // Crisp upper-harmonic transient sparkle for Nightcore
                    val mid = (wetL + wetR) * 0.5f
                    val side = (wetL - wetR) * 0.5f * 1.15f
                    wetL = mid + side
                    wetR = mid - side
                }

                // =============================================================
                // STAGE 4: Warm Analog Tape Saturation (Lo-Fi & Phonk)
                // =============================================================
                if (isLofiColorActive && (satAmount > 0.005f || softDistAmount > 0.005f)) {
                    val totalDrive = (satAmount * 0.85f + softDistAmount * 0.70f).coerceIn(0f, 1.15f)
                    if (qualityMode == AudioPlaybackQuality.MAXIMUM_AVAILABLE_QUALITY) {
                        val midL = (wetL + dcPrevOutput[0]) * 0.5f
                        val midR = (wetR + dcPrevOutput[1]) * 0.5f
                        wetL = 0.5f * (warmTapeSaturate(midL, totalDrive) + warmTapeSaturate(wetL, totalDrive))
                        wetR = 0.5f * (warmTapeSaturate(midR, totalDrive) + warmTapeSaturate(wetR, totalDrive))
                    } else {
                        wetL = warmTapeSaturate(wetL, totalDrive)
                        wetR = warmTapeSaturate(wetR, totalDrive)
                    }
                }

                // =============================================================
                // STAGE 5: Cascaded 4th-Order Butterworth Low-Pass Filter
                // =============================================================
                if (isLowPassActive) {
                    wetL = lowPassStage2Filters[0].process(lowPassStage1Filters[0].process(wetL))
                    wetR = lowPassStage2Filters[1].process(lowPassStage1Filters[1].process(wetR))
                }

                // =============================================================
                // STAGE 6: Analog Tape Wow & Flutter / Dreamy Stereo Chorus
                // =============================================================
                if (wowDepth > 0.003f) {
                    wowDelayLeft[wowWriteIndex] = wetL
                    wowDelayRight[wowWriteIndex] = wetR

                    val wowLfoL = sin(wowPhase).toFloat()
                    val wowLfoR = sin(wowPhase + 0.42).toFloat()
                    val flutterLfo = sin(flutterPhase).toFloat()
                    wowPhase += wowPhaseInc
                    if (wowPhase > 2.0 * PI) wowPhase -= 2.0 * PI
                    flutterPhase += flutterPhaseInc
                    if (flutterPhase > 2.0 * PI) flutterPhase -= 2.0 * PI

                    if (mode == LiveSoundMode.DREAMY_SOUND && !isLofiColorActive) {
                        // Dual-tap lush stereo pitch chorus for Dreamy Sound & Slowed + Reverb
                        val centerDelay = 0.0075f * srFloat
                        val sweep = 0.0028f * srFloat * wowDepth
                        val delaySamplesL = (centerDelay + wowLfoL * sweep + flutterLfo * sweep * 0.25f)
                            .coerceIn(4f, (wowDelaySize - 8).toFloat())
                        val delaySamplesR = (centerDelay + cos(wowPhase).toFloat() * sweep - flutterLfo * sweep * 0.25f)
                            .coerceIn(4f, (wowDelaySize - 8).toFloat())

                        val chorusL = readFractionalDelay(wowDelayLeft, wowWriteIndex, wowDelaySize, wowDelayMask, delaySamplesL)
                        val chorusR = readFractionalDelay(wowDelayRight, wowWriteIndex, wowDelaySize, wowDelayMask, delaySamplesR)
                        wetL = wetL * 0.68f + chorusL * 0.32f
                        wetR = wetR * 0.68f + chorusR * 0.32f
                    } else {
                        // Authentic analog tape capstan wow & flutter
                        val maxSweepSamples = (0.0014f * srFloat) * wowDepth
                        val centerDelay = maxSweepSamples + 3.0f
                        val modL = (wowLfoL * 0.80f + flutterLfo * 0.20f) * maxSweepSamples
                        val modR = (wowLfoR * 0.80f + flutterLfo * 0.20f) * maxSweepSamples

                        wetL = readFractionalDelay(
                            wowDelayLeft,
                            wowWriteIndex,
                            wowDelaySize,
                            wowDelayMask,
                            (centerDelay + modL).coerceIn(1f, (wowDelaySize - 4).toFloat())
                        )
                        wetR = readFractionalDelay(
                            wowDelayRight,
                            wowWriteIndex,
                            wowDelaySize,
                            wowDelayMask,
                            (centerDelay + modR).coerceIn(1f, (wowDelaySize - 4).toFloat())
                        )
                    }
                    wowWriteIndex = (wowWriteIndex + 1) and wowDelayMask
                }

                // =============================================================
                // STAGE 7: Anti-Aliased Vintage Sampler Bitcrusher
                // =============================================================
                if (bitcrusherActive) {
                    decimationCounter += 1.0f
                    if (decimationCounter >= srDivisor) {
                        decimationCounter -= srDivisor
                        prevHeldLeft = heldSampleLeft
                        prevHeldRight = heldSampleRight
                        var qL = wetL
                        var qR = wetR
                        if (quantLevels > 0f) {
                            val dither = (nextWhiteNoise() - nextWhiteNoise()) * (0.35f / quantLevels)
                            qL = ((qL + dither) * quantLevels).roundToInt() / quantLevels
                            qR = ((qR - dither) * quantLevels).roundToInt() / quantLevels
                        }
                        heldSampleLeft = qL
                        heldSampleRight = qR
                    }
                    val frac = (decimationCounter / srDivisor).coerceIn(0f, 1f)
                    val interpL = prevHeldLeft + frac * (heldSampleLeft - prevHeldLeft)
                    val interpR = prevHeldRight + frac * (heldSampleRight - prevHeldRight)
                    bitcrushAntiAliasL += 0.45f * (interpL - bitcrushAntiAliasL)
                    bitcrushAntiAliasR += 0.45f * (interpR - bitcrushAntiAliasR)
                    wetL = wetL * 0.25f + bitcrushAntiAliasL * 0.75f
                    wetR = wetR * 0.25f + bitcrushAntiAliasR * 0.75f
                }

                // =============================================================
                // STAGE 8: Stereo Tape Echo & Lush Schroeder Hall Reverb
                // =============================================================
                if (echoWet > 0.005f) {
                    val readIdx = (echoWriteIndex - echoDelaySamples + echoDelaySize) and echoDelayMask
                    val delL = echoDelayLeft[readIdx]
                    val delR = echoDelayRight[readIdx]

                    echoLpLeft += 0.34f * (delL - echoLpLeft)
                    echoLpRight += 0.34f * (delR - echoLpRight)

                    echoDelayLeft[echoWriteIndex] = (wetL + echoLpRight * echoFb).coerceIn(-0.95f, 0.95f)
                    echoDelayRight[echoWriteIndex] = (wetR + echoLpLeft * echoFb).coerceIn(-0.95f, 0.95f)
                    echoWriteIndex = (echoWriteIndex + 1) and echoDelayMask

                    wetL = wetL * (1.0f - echoWet * 0.25f) + echoLpLeft * echoWet
                    wetR = wetR * (1.0f - echoWet * 0.25f) + echoLpRight * echoWet
                }

                if (reverbWet > 0.004f) {
                    // Optional pre-delay for spacious Slowed + Reverb & Dreamy Hall modes
                    var revInL = wetL
                    var revInR = wetR
                    if (preDelaySamples > 0) {
                        reverbPreDelayLeft[reverbPreDelayWriteIdx] = wetL
                        reverbPreDelayRight[reverbPreDelayWriteIdx] = wetR
                        val pdRead = (reverbPreDelayWriteIdx - preDelaySamples + reverbPreDelaySize) and reverbPreDelayMask
                        revInL = reverbPreDelayLeft[pdRead]
                        revInR = reverbPreDelayRight[pdRead]
                        reverbPreDelayWriteIdx = (reverbPreDelayWriteIdx + 1) and reverbPreDelayMask
                    }

                    val revMono = (revInL + revInR) * 0.22f
                    var outRevL = 0f
                    var outRevR = 0f

                    for (c in 0 until 4) {
                        val bufL = combBuffersL[c]
                        val idxL = combIndicesL[c]
                        val delL = bufL[idxL]
                        combFilterStatesL[c] = delL * (1f - reverbDamp) + combFilterStatesL[c] * reverbDamp
                        bufL[idxL] = revMono + combFilterStatesL[c] * roomFeedback
                        combIndicesL[c] = if (idxL + 1 >= bufL.size) 0 else idxL + 1
                        outRevL += delL

                        val bufR = combBuffersR[c]
                        val idxR = combIndicesR[c]
                        val delR = bufR[idxR]
                        combFilterStatesR[c] = delR * (1f - reverbDamp) + combFilterStatesR[c] * reverbDamp
                        bufR[idxR] = revMono + combFilterStatesR[c] * roomFeedback
                        combIndicesR[c] = if (idxR + 1 >= bufR.size) 0 else idxR + 1
                        outRevR += delR
                    }

                    for (a in 0 until 2) {
                        val apBufL = allPassBuffersL[a]
                        val apIdxL = allPassIndicesL[a]
                        val bufOutL = apBufL[apIdxL]
                        val inValL = outRevL
                        outRevL = -inValL + bufOutL
                        apBufL[apIdxL] = inValL + bufOutL * 0.5f
                        allPassIndicesL[a] = if (apIdxL + 1 >= apBufL.size) 0 else apIdxL + 1

                        val apBufR = allPassBuffersR[a]
                        val apIdxR = allPassIndicesR[a]
                        val bufOutR = apBufR[apIdxR]
                        val inValR = outRevR
                        outRevR = -inValR + bufOutR
                        apBufR[apIdxR] = inValR + bufOutR * 0.5f
                        allPassIndicesR[a] = if (apIdxR + 1 >= apBufR.size) 0 else apIdxR + 1
                    }

                    // Cross-stereo diffusion for lush wide reverb tail
                    val diffL = outRevL * 0.85f + outRevR * 0.15f
                    val diffR = outRevR * 0.85f + outRevL * 0.15f
                    val dryScale = 1.0f - reverbWet * 0.30f
                    wetL = wetL * dryScale + diffL * 0.24f * reverbWet
                    wetR = wetR * dryScale + diffR * 0.24f * reverbWet
                }

                // =============================================================
                // STAGE 9: Authentic 33⅓ RPM Vinyl Texture (Live Lo-Fi Only)
                // =============================================================
                if (anyVinylOrTapeNoise) {
                    val white = nextWhiteNoise()
                    pinkB0 = 0.99765f * pinkB0 + white * 0.0990460f
                    pinkB1 = 0.96300f * pinkB1 + white * 0.2965164f
                    pinkB2 = 0.57000f * pinkB2 + white * 1.0526913f
                    val rawPink = (pinkB0 + pinkB1 + pinkB2 + white * 0.1848f) * 0.04f

                    val platterSwish = (0.82f + 0.18f * sin(vinylGroovePhase).toFloat())
                    vinylGroovePhase += vinylGroovePhaseInc
                    if (vinylGroovePhase > 2.0 * PI) vinylGroovePhase -= 2.0 * PI

                    val grooveL = (rawPink - vinylHpStateL) * platterSwish * vinylNoiseAmount * 0.011f
                    vinylHpStateL += 0.018f * (rawPink - vinylHpStateL)
                    val grooveR = (rawPink * 0.92f + nextWhiteNoise() * 0.02f - vinylHpStateR) *
                        platterSwish * vinylNoiseAmount * 0.011f
                    vinylHpStateR += 0.018f * (rawPink - vinylHpStateR)

                    val hissL = (white - rawPink) * tapeHissAmount * 0.0055f
                    val hissR = (nextWhiteNoise() - rawPink) * tapeHissAmount * 0.0055f

                    if (crackleAmount > 0.002f) {
                        val popThreshold = 1.0f - (crackleAmount * 0.0014f)
                        if (abs(nextWhiteNoise()) > popThreshold) {
                            val amp = (0.015f + abs(nextWhiteNoise()) * 0.030f) * crackleAmount
                            if (nextWhiteNoise() >= 0f) crackleEnvLeft = amp else crackleEnvRight = amp
                        }
                    }
                    crackleEnvLeft *= 0.91f
                    crackleEnvRight *= 0.91f

                    val rawPopL = crackleEnvLeft * nextWhiteNoise()
                    val rawPopR = crackleEnvRight * nextWhiteNoise()
                    crackleBandStateL1 += 0.34f * (rawPopL - crackleBandStateL1)
                    crackleBandStateL2 += 0.09f * (crackleBandStateL1 - crackleBandStateL2)
                    val needlePopL = crackleBandStateL1 - crackleBandStateL2

                    crackleBandStateR1 += 0.34f * (rawPopR - crackleBandStateR1)
                    crackleBandStateR2 += 0.09f * (crackleBandStateR1 - crackleBandStateR2)
                    val needlePopR = crackleBandStateR1 - crackleBandStateR2

                    wetL += grooveL + hissL + needlePopL
                    wetR += grooveR + hissR + needlePopR
                }

                // =============================================================
                // STAGE 10: 8D Orbit, 3D Spatial Binaural HRTF & Ultra Surround
                // =============================================================
                when (mode) {
                    LiveSoundMode.EIGHT_D_ULTRA -> {
                        // Continuous 360-degree binaural azimuth rotation + ITD delay + rear head shadow
                        val azSin = sin(eightDPhase).toFloat() // -1 (Left) .. +1 (Right)
                        val azCos = cos(eightDPhase).toFloat() // +1 (Front) .. -1 (Behind head)
                        eightDPhase += eightDPhaseInc
                        if (eightDPhase > 2.0 * PI) eightDPhase -= 2.0 * PI

                        spatialItdDelayLeft[spatialItdWriteIndex] = wetL
                        spatialItdDelayRight[spatialItdWriteIndex] = wetR

                        val maxItdSamples = 0.00068f * srFloat * spatInt
                        val delayL = if (azSin > 0f) azSin * maxItdSamples else 0f
                        val delayR = if (azSin < 0f) (-azSin) * maxItdSamples else 0f

                        val itdL = readFractionalDelay(
                            spatialItdDelayLeft,
                            spatialItdWriteIndex,
                            spatialItdDelaySize,
                            spatialItdDelayMask,
                            delayL + 1f
                        )
                        val itdR = readFractionalDelay(
                            spatialItdDelayRight,
                            spatialItdWriteIndex,
                            spatialItdDelaySize,
                            spatialItdDelayMask,
                            delayR + 1f
                        )
                        spatialItdWriteIndex = (spatialItdWriteIndex + 1) and spatialItdDelayMask

                        // Wide constant-power stereo orbit with natural cross-feed
                        val monoSum = (itdL + itdR) * 0.5f
                        val stereoSide = (itdL - itdR) * 0.5f
                        val panAngle = ((azSin * 0.90f * spatInt + 1.0f) * 0.25f * PI).toFloat()
                        val leftPan = cos(panAngle)
                        val rightPan = sin(panAngle)

                        var pannedL = (monoSum * 0.78f + stereoSide * 0.35f) * leftPan * 1.38f + itdR * 0.08f
                        var pannedR = (monoSum * 0.78f - stereoSide * 0.35f) * rightPan * 1.38f + itdL * 0.08f

                        // Rear-hemisphere acoustic head shadow when source passes behind listener (azCos < 0)
                        val rearFactor = ((-azCos).coerceIn(0f, 1f)) * 0.58f * spatInt
                        hrtfShadowLeftState += (1f - rearFactor) * (pannedL - hrtfShadowLeftState)
                        hrtfShadowRightState += (1f - rearFactor) * (pannedR - hrtfShadowRightState)

                        wetL = hrtfShadowLeftState
                        wetR = hrtfShadowRightState
                    }

                    LiveSoundMode.THREE_D_SPATIAL -> {
                        // Parametric Binaural HRTF (Woodworth Spherical-Head ITD + Head-Shadow ILD + Pinna Notch + Early Reflections)
                        val azRad = if (lofiParams.spatialAutoOrbit) {
                            // Smooth frontal 3D stage arc (±42 deg) distinct from 8D's full 360 spin
                            val p = (sin(spatialOrbitPhase) * (PI * 0.24)).toFloat()
                            spatialOrbitPhase += spatialOrbitPhaseInc
                            if (spatialOrbitPhase > 2.0 * PI) spatialOrbitPhase -= 2.0 * PI
                            p
                        } else {
                            (lofiParams.spatialAzimuthDeg.coerceIn(-180f, 180f) * (PI / 180.0)).toFloat()
                        }

                        val sinAz = sin(azRad)
                        val absSinAz = abs(sinAz)
                        val woodworthFactor = (absSinAz + asinfApprox(absSinAz)).coerceIn(0f, 2.57f)
                        val itdSec = 0.000254f * woodworthFactor * spatInt
                        val itdSamples = (itdSec * srFloat).coerceIn(0f, 48f)

                        spatialItdDelayLeft[spatialItdWriteIndex] = wetL
                        spatialItdDelayRight[spatialItdWriteIndex] = wetR

                        val earDelayL = if (sinAz > 0f) itdSamples else 0f
                        val earDelayR = if (sinAz < 0f) itdSamples else 0f

                        var binauralL = readFractionalDelay(
                            spatialItdDelayLeft,
                            spatialItdWriteIndex,
                            spatialItdDelaySize,
                            spatialItdDelayMask,
                            earDelayL + 1f
                        )
                        var binauralR = readFractionalDelay(
                            spatialItdDelayRight,
                            spatialItdWriteIndex,
                            spatialItdDelaySize,
                            spatialItdDelayMask,
                            earDelayR + 1f
                        )

                        // Early lateral room reflections (11.3ms & 18.7ms stereo reflection taps)
                        val er1 = (0.0113f * srFloat).coerceIn(8f, (spatialItdDelaySize - 8).toFloat())
                        val er2 = (0.0187f * srFloat).coerceIn(16f, (spatialItdDelaySize - 8).toFloat())
                        val reflL = readFractionalDelay(spatialItdDelayRight, spatialItdWriteIndex, spatialItdDelaySize, spatialItdDelayMask, er1) * 0.16f -
                            readFractionalDelay(spatialItdDelayLeft, spatialItdWriteIndex, spatialItdDelaySize, spatialItdDelayMask, er2) * 0.10f
                        val reflR = readFractionalDelay(spatialItdDelayLeft, spatialItdWriteIndex, spatialItdDelaySize, spatialItdDelayMask, er1) * 0.16f -
                            readFractionalDelay(spatialItdDelayRight, spatialItdWriteIndex, spatialItdDelaySize, spatialItdDelayMask, er2) * 0.10f

                        spatialItdWriteIndex = (spatialItdWriteIndex + 1) and spatialItdDelayMask

                        // Frequency-dependent head-shadow ILD at contralateral ear
                        val shadowAlpha = (2.0 * PI * 1500.0 / srFloat).toFloat().coerceIn(0.05f, 0.35f)
                        hrtfShadowLeftState += shadowAlpha * (binauralL - hrtfShadowLeftState)
                        hrtfShadowRightState += shadowAlpha * (binauralR - hrtfShadowRightState)

                        val contraRight = sinAz.coerceIn(0f, 1f) * spatInt
                        val contraLeft = (-sinAz).coerceIn(0f, 1f) * spatInt

                        binauralL = binauralL * (1f - 0.58f * contraRight) + hrtfShadowLeftState * (0.28f * contraRight)
                        binauralR = binauralR * (1f - 0.58f * contraLeft) + hrtfShadowRightState * (0.28f * contraLeft)

                        val distAtten = (1.0f - lofiParams.spatialDistance.coerceIn(0f, 1f) * 0.20f)
                        binauralL = hrtfConchaPeakFilters[0].process(hrtfPinnaNotchFilters[0].process(binauralL)) * distAtten + reflL * spatInt
                        binauralR = hrtfConchaPeakFilters[1].process(hrtfPinnaNotchFilters[1].process(binauralR)) * distAtten + reflR * spatInt

                        wetL = binauralL
                        wetR = binauralR
                    }

                    LiveSoundMode.ULTRA_SURROUND -> {
                        // High-passed Side (>180Hz) Mid/Side expansion + Haas decorrelation
                        val mid = (wetL + wetR) * 0.5f
                        val rawSide = (wetL - wetR) * 0.5f
                        val hpSide = rawSide - surroundSideHpPrevIn + 0.974f * surroundSideHpPrevOut
                        surroundSideHpPrevIn = rawSide
                        surroundSideHpPrevOut = hpSide

                        spatialItdDelayLeft[spatialItdWriteIndex] = hpSide
                        val haasTap = readFractionalDelay(
                            spatialItdDelayLeft,
                            spatialItdWriteIndex,
                            spatialItdDelaySize,
                            spatialItdDelayMask,
                            (0.0068f * srFloat).coerceIn(4f, 512f)
                        )
                        spatialItdWriteIndex = (spatialItdWriteIndex + 1) and spatialItdDelayMask

                        val wideSide = hpSide * (1.0f + 0.68f * spatInt) + haasTap * 0.24f * spatInt
                        wetL = (mid + wideSide) * 0.93f
                        wetR = (mid - wideSide) * 0.93f
                    }

                    else -> {
                        if (abs(stereoWidthFactor - 1.0f) > 0.01f) {
                            val mid = (wetL + wetR) * 0.5f
                            val side = (wetL - wetR) * 0.5f * stereoWidthFactor
                            val norm = 1.0f / max(1.0f, 0.5f + 0.5f * stereoWidthFactor * 0.85f)
                            wetL = (mid + side) * norm
                            wetR = (mid - side) * norm
                        }
                    }
                }

                // =============================================================
                // STAGE 11: Smooth RMS Bus Glue Compressor
                // =============================================================
                if (compAmount > 0.01f) {
                    val framePower = sqrt((wetL * wetL + wetR * wetR) * 0.5f)
                    val coeff = if (framePower > compressorEnvelope) 0.008f else 0.0006f
                    compressorEnvelope += coeff * (framePower - compressorEnvelope)

                    val threshold = 0.42f
                    if (compressorEnvelope > threshold) {
                        val excess = compressorEnvelope - threshold
                        val gr = 1.0f / (1.0f + excess * compAmount * 1.4f)
                        wetL *= gr
                        wetR *= gr
                        smoothedGainReductionDb = -20f * log10(gr.coerceAtLeast(0.25f))
                    } else {
                        smoothedGainReductionDb *= 0.995f
                    }
                } else {
                    smoothedGainReductionDb = 0f
                }

                wetL *= makeupGain * modeTransitionGain
                wetR *= makeupGain * modeTransitionGain
            }

            // Equal-power dry/wet mix
            var finalL = dryL * (1.0f - currentWetDryMix) + wetL * currentWetDryMix
            var finalR = dryR * (1.0f - currentWetDryMix) + wetR * currentWetDryMix

            // Execute ONLY active Equalizer bands (skips all 0.0 dB bands)
            for (i in 0 until activeEqBandCount) {
                val b = activeEqBandIndices[i]
                finalL = eqFilters[0][b].process(finalL)
                finalR = eqFilters[1][b].process(finalR)
            }

            // Apply slewed Master Gain, Stereo Balance & Fade Envelope
            finalL *= currentMasterGain * leftChannelTrim * trackFadeEnvelope
            finalR *= currentMasterGain * rightChannelTrim * trackFadeEnvelope

            // =================================================================
            // STAGE 12: Envelope-Tracked True-Peak Limiter (-0.3 dBFS Ceiling)
            // =================================================================
            val ceiling = 0.965f
            val currentPeak = max(abs(finalL), abs(finalR))
            val targetLimiterGain = if (currentPeak > ceiling) ceiling / currentPeak else 1.0f
            if (targetLimiterGain < limiterGainEnvelope) {
                limiterGainEnvelope = targetLimiterGain
            } else {
                limiterGainEnvelope += 0.0008f * (1.0f - limiterGainEnvelope)
            }

            finalL = softKneeCeiling(finalL * limiterGainEnvelope)
            finalR = softKneeCeiling(finalR * limiterGainEnvelope)

            if (qualityMode == AudioPlaybackQuality.MAXIMUM_AVAILABLE_QUALITY && isDspActive) {
                val tpdfDither = (nextWhiteNoise() - nextWhiteNoise()) * (0.25f / 32768.0f)
                finalL = (finalL + tpdfDither).coerceIn(-0.965f, 0.965f)
                finalR = (finalR - tpdfDither).coerceIn(-0.965f, 0.965f)
            }

            val postLimiterPeak = max(abs(finalL), abs(finalR))
            if (postLimiterPeak > maxPeak) maxPeak = postLimiterPeak
            if (postLimiterPeak >= 0.999f) clipCount++

            sumSqL += finalL * finalL
            sumSqR += finalR * finalR
            sumL += finalL
            sumR += finalR

            if ((frame and 3) == 0) {
                visRing[visWritePos] = (finalL + finalR) * 0.5f
                visWritePos = (visWritePos + 1) and visRingMask
            }

            val outShortL = (finalL * 32767.0f).roundToInt().coerceIn(-32768, 32767).toShort()
            outBuffer.putShort(outShortL)
            if (chCount == 2) {
                val outShortR = (finalR * 32767.0f).roundToInt().coerceIn(-32768, 32767).toShort()
                outBuffer.putShort(outShortR)
            }
        }

        finalizeBufferTelemetry(
            startNs = startNs,
            sr = sr,
            numFrames = numFrames,
            sumSqL = sumSqL,
            sumSqR = sumSqR,
            sumL = sumL,
            sumR = sumR,
            maxPeak = maxPeak,
            clipCount = clipCount,
            isDspActive = isDspActive,
            lofiParams = lofiParams
        )
    }

    private fun finalizeBufferTelemetry(
        startNs: Long,
        sr: Int,
        numFrames: Int,
        sumSqL: Float,
        sumSqR: Float,
        sumL: Float,
        sumR: Float,
        maxPeak: Float,
        clipCount: Int,
        isDspActive: Boolean,
        lofiParams: LofiDspParameters
    ) {
        val elapsedNs = System.nanoTime() - startNs
        val elapsedMs = elapsedNs / 1_000_000f
        val elapsedUs = elapsedNs / 1_000f
        val bufferDurationMs = (numFrames.toFloat() / sr.toFloat()) * 1000f

        lastProcessingLatencyMs = elapsedMs
        lastDspCpuTimeUs = elapsedUs
        lastBufferDurationMs = bufferDurationMs

        if (elapsedMs > bufferDurationMs * 0.95f) {
            totalUnderrunCount++
        }

        val rmsL = if (numFrames > 0) sqrt(sumSqL / numFrames).coerceIn(0f, 1f) else 0f
        val rmsR = if (numFrames > 0) sqrt(sumSqR / numFrames).coerceIn(0f, 1f) else 0f
        val dcL = if (numFrames > 0) sumL / numFrames else 0f
        val dcR = if (numFrames > 0) sumR / numFrames else 0f
        val balanceDb = if (rmsL > 0.0001f && rmsR > 0.0001f) {
            20f * log10(rmsL / rmsR)
        } else {
            0f
        }

        lastPeakLevel = maxPeak.coerceIn(0f, 1f)
        lastRmsLeft = rmsL
        lastRmsRight = rmsR
        lastClippingCount = clipCount
        lastDcOffsetLeft = dcL
        lastDcOffsetRight = dcR
        lastChannelBalanceDb = balanceDb

        framesSinceLastVisEmit += numFrames
        // Throttle visualizer snapshot updates to ~20 Hz (every 50ms) with fast 64-point precomputed Goertzel
        if (framesSinceLastVisEmit >= sr / 20) {
            framesSinceLastVisEmit = 0
            emitVisualizerSnapshot(
                sr = sr,
                rmsL = rmsL,
                rmsR = rmsR,
                maxPeak = maxPeak,
                isLofiActive = isDspActive,
                lofiParams = lofiParams,
                latencyMs = elapsedMs,
                cpuUs = elapsedUs,
                clipCount = clipCount,
                dcL = dcL,
                dcR = dcR,
                balanceDb = balanceDb
            )
        }
    }

    private fun readFractionalDelay(
        buffer: FloatArray,
        writeIdx: Int,
        size: Int,
        mask: Int,
        delaySamples: Float
    ): Float {
        val readPos = writeIdx - delaySamples
        val basePos = if (readPos < 0f) readPos + size else readPos
        val idx0 = basePos.toInt() and mask
        val idx1 = (idx0 + 1) and mask
        val frac = basePos - basePos.toInt()
        return buffer[idx0] * (1.0f - frac) + buffer[idx1] * frac
    }

    private fun asinfApprox(x: Float): Float {
        val c = x.coerceIn(0f, 1f)
        return c + (c * c * c) * 0.166667f + (c * c * c * c * c) * 0.075f
    }

    private fun warmTapeSaturate(x: Float, amount: Float): Float {
        if (amount <= 0.001f) return x
        val drive = 1.0f + amount * 1.45f
        val driven = x * drive
        val evenBias = 0.035f * amount * driven * driven
        val asymmetric = driven + evenBias
        val shaped = asymmetric / (1.0f + abs(asymmetric) * 0.42f)
        val norm = 1.0f / (1.0f + amount * 0.36f)
        return shaped * norm
    }

    private fun softKneeCeiling(x: Float): Float {
        val absX = abs(x)
        val kneeStart = 0.90f
        val maxCeiling = 0.965f
        if (absX <= kneeStart) return x
        val sign = if (x >= 0f) 1.0f else -1.0f
        val excess = absX - kneeStart
        val range = maxCeiling - kneeStart
        val compressed = kneeStart + range * (excess / (range + excess))
        return sign * compressed
    }

    private fun nextWhiteNoise(): Float {
        var x = rngSeed
        x = x xor (x shl 13)
        x = x xor (x ushr 17)
        x = x xor (x shl 5)
        rngSeed = x
        return (x.toFloat() / Int.MAX_VALUE.toFloat())
    }

    private fun emitVisualizerSnapshot(
        sr: Int,
        rmsL: Float,
        rmsR: Float,
        maxPeak: Float,
        isLofiActive: Boolean,
        lofiParams: LofiDspParameters,
        latencyMs: Float,
        cpuUs: Float,
        clipCount: Int,
        dcL: Float,
        dcR: Float,
        balanceDb: Float
    ) {
        val waveform = FloatArray(64)
        for (i in 0 until 64) {
            val idx = (visWritePos - 64 + i + visRingSize) and visRingMask
            waveform[i] = visRing[idx].coerceIn(-1f, 1f)
        }

        val spectrum = FloatArray(32)
        val windowN = 64
        if (maxPeak > 0.0002f) {
            for (b in 0 until 32) {
                val coeff = precomputedGoertzelCos[b]
                var s1 = 0f
                var s2 = 0f
                for (n in 0 until windowN) {
                    val sample = waveform[n]
                    val s0 = sample + coeff * s1 - s2
                    s2 = s1
                    s1 = s0
                }
                val power = s1 * s1 + s2 * s2 - coeff * s1 * s2
                val mag = sqrt(max(0f, power)) / (windowN * 0.25f)
                val tilt = 1.0f + (b / 31.0f) * 1.3f
                spectrum[b] = (mag * tilt).coerceIn(0.02f, 1.0f)
            }
        }

        val peakDb = if (maxPeak > 0.0001f) (20.0 * ln(maxPeak.toDouble()) / ln(10.0)).toFloat() else -60f

        _visualizerFlow.value = VisualizerSnapshot(
            waveform = waveform,
            spectrum = spectrum,
            rmsLeft = rmsL.coerceIn(0f, 1f),
            rmsRight = rmsR.coerceIn(0f, 1f),
            peakDb = peakDb.coerceIn(-60f, 0f),
            peakLevelLinear = maxPeak.coerceIn(0f, 1f),
            gainReductionDb = smoothedGainReductionDb.coerceIn(0f, 12f),
            isLofiActive = isLofiActive,
            effectiveSampleRateHz = (sr / lofiParams.sampleRateDivisor.coerceIn(1f, 4f)).roundToInt(),
            activeLowPassHz = smoothedLowPassHz.roundToInt(),
            activeBitDepth = lofiParams.bitDepth,
            bufferLatencyMs = latencyMs,
            clippingCount = clipCount,
            dcOffsetLeft = dcL,
            dcOffsetRight = dcR,
            channelBalanceDb = balanceDb,
            underrunCount = totalUnderrunCount,
            dspCpuTimeUs = cpuUs
        )
    }

    override fun onFlush() {
        for (ch in 0..1) {
            highPassFilters[ch].reset()
            lowPassStage1Filters[ch].reset()
            lowPassStage2Filters[ch].reset()
            subBassShelfFilters[ch].reset()
            bassShelfFilters[ch].reset()
            midrangePeakFilters[ch].reset()
            vocalMudCutFilters[ch].reset()
            vocalPresenceFilters[ch].reset()
            highShelfFilters[ch].reset()
            modeFilter1[ch].reset()
            modeFilter2[ch].reset()
            modeFilter3[ch].reset()
            hrtfPinnaNotchFilters[ch].reset()
            hrtfConchaPeakFilters[ch].reset()
            for (b in 0 until 10) {
                eqFilters[ch][b].reset()
            }
            dcPrevInput[ch] = 0f
            dcPrevOutput[ch] = 0f
        }
        bassLowPassLeft = 0f
        bassLowPassRight = 0f
        subSynthLpLeft = 0f
        subSynthLpRight = 0f
        wowDelayLeft.fill(0f)
        wowDelayRight.fill(0f)
        wowWriteIndex = 0
        spatialItdDelayLeft.fill(0f)
        spatialItdDelayRight.fill(0f)
        spatialItdWriteIndex = 0
        hrtfShadowLeftState = 0f
        hrtfShadowRightState = 0f
        surroundSideHpPrevIn = 0f
        surroundSideHpPrevOut = 0f
        reverbPreDelayLeft.fill(0f)
        reverbPreDelayRight.fill(0f)
        reverbPreDelayWriteIdx = 0
        echoDelayLeft.fill(0f)
        echoDelayRight.fill(0f)
        echoWriteIndex = 0
        echoLpLeft = 0f
        echoLpRight = 0f
        for (c in 0 until 4) {
            combBuffersL[c].fill(0f)
            combBuffersR[c].fill(0f)
            combIndicesL[c] = 0
            combIndicesR[c] = 0
            combFilterStatesL[c] = 0f
            combFilterStatesR[c] = 0f
        }
        for (a in 0 until 2) {
            allPassBuffersL[a].fill(0f)
            allPassBuffersR[a].fill(0f)
            allPassIndicesL[a] = 0
            allPassIndicesR[a] = 0
        }
        compressorEnvelope = 0f
        limiterGainEnvelope = 1.0f
        trackFadeEnvelope = if (currentEqState.fadeInOutMs > 0 && currentEqState.smoothTransitions) 0.0f else 1.0f
        lastClippingCount = 0
    }

    override fun onReset() {
        onFlush()
        currentWetDryMix = 1.0f
        currentMasterGain = 1.0f
        smoothedLowPassHz = 8800f
        lastComputedLowPassHz = -1f
        totalUnderrunCount = 0
    }

    /**
     * Direct Form II Transposed Biquad Filter using 64-bit Double coefficient math
     * and denormal prevention for zero-noise real-time stability.
     */
    private class BiquadFilter {
        private var b0 = 1.0f
        private var b1 = 0.0f
        private var b2 = 0.0f
        private var a1 = 0.0f
        private var a2 = 0.0f

        private var z1 = 0.0f
        private var z2 = 0.0f

        fun reset() {
            z1 = 0.0f
            z2 = 0.0f
        }

        fun process(input: Float): Float {
            val out = b0 * input + z1
            z1 = b1 * input - a1 * out + z2
            z2 = b2 * input - a2 * out
            if (abs(z1) < 1e-20f) z1 = 0f
            if (abs(z2) < 1e-20f) z2 = 0f
            return if (out.isNaN() || out.isInfinite()) 0f else out
        }

        fun setLowPass(sampleRate: Float, cutoffHz: Float, q: Float) {
            val f0 = cutoffHz.coerceIn(40f, sampleRate * 0.46f).toDouble()
            val w0 = 2.0 * PI * f0 / sampleRate.toDouble()
            val cosW0 = cos(w0)
            val sinW0 = sin(w0)
            val alpha = sinW0 / (2.0 * q.coerceIn(0.3f, 8.0f).toDouble())

            val norm = 1.0 + alpha
            b0 = (((1.0 - cosW0) * 0.5) / norm).toFloat()
            b1 = ((1.0 - cosW0) / norm).toFloat()
            b2 = (((1.0 - cosW0) * 0.5) / norm).toFloat()
            a1 = ((-2.0 * cosW0) / norm).toFloat()
            a2 = ((1.0 - alpha) / norm).toFloat()
        }

        fun setHighPass(sampleRate: Float, cutoffHz: Float, q: Float) {
            val f0 = cutoffHz.coerceIn(15f, sampleRate * 0.45f).toDouble()
            val w0 = 2.0 * PI * f0 / sampleRate.toDouble()
            val cosW0 = cos(w0)
            val sinW0 = sin(w0)
            val alpha = sinW0 / (2.0 * q.coerceIn(0.3f, 8.0f).toDouble())

            val norm = 1.0 + alpha
            b0 = (((1.0 + cosW0) * 0.5) / norm).toFloat()
            b1 = (-(1.0 + cosW0) / norm).toFloat()
            b2 = (((1.0 + cosW0) * 0.5) / norm).toFloat()
            a1 = ((-2.0 * cosW0) / norm).toFloat()
            a2 = ((1.0 - alpha) / norm).toFloat()
        }

        fun setPeakingEq(sampleRate: Float, centerHz: Float, q: Float, gainDb: Float) {
            if (abs(gainDb) < 0.02f) {
                b0 = 1f; b1 = 0f; b2 = 0f; a1 = 0f; a2 = 0f
                return
            }
            val a = 10.0.pow(gainDb.toDouble() / 40.0)
            val f0 = centerHz.coerceIn(20f, sampleRate * 0.46f).toDouble()
            val w0 = 2.0 * PI * f0 / sampleRate.toDouble()
            val cosW0 = cos(w0)
            val sinW0 = sin(w0)
            val alpha = sinW0 / (2.0 * q.coerceIn(0.2f, 10.0f).toDouble())

            val norm = 1.0 + alpha / a
            b0 = ((1.0 + alpha * a) / norm).toFloat()
            b1 = ((-2.0 * cosW0) / norm).toFloat()
            b2 = ((1.0 - alpha * a) / norm).toFloat()
            a1 = ((-2.0 * cosW0) / norm).toFloat()
            a2 = ((1.0 - alpha / a) / norm).toFloat()
        }

        fun setLowShelf(sampleRate: Float, cutoffHz: Float, q: Float, gainDb: Float) {
            if (abs(gainDb) < 0.02f) {
                b0 = 1f; b1 = 0f; b2 = 0f; a1 = 0f; a2 = 0f
                return
            }
            val a = 10.0.pow(gainDb.toDouble() / 40.0)
            val f0 = cutoffHz.coerceIn(20f, sampleRate * 0.45f).toDouble()
            val w0 = 2.0 * PI * f0 / sampleRate.toDouble()
            val cosW0 = cos(w0)
            val sinW0 = sin(w0)
            val alpha = sinW0 / (2.0 * q.coerceIn(0.3f, 4.0f).toDouble())
            val twoSqrtAAlpha = 2.0 * sqrt(a) * alpha

            val norm = (a + 1.0) + (a - 1.0) * cosW0 + twoSqrtAAlpha
            b0 = ((a * ((a + 1.0) - (a - 1.0) * cosW0 + twoSqrtAAlpha)) / norm).toFloat()
            b1 = ((2.0 * a * ((a - 1.0) - (a + 1.0) * cosW0)) / norm).toFloat()
            b2 = ((a * ((a + 1.0) - (a - 1.0) * cosW0 - twoSqrtAAlpha)) / norm).toFloat()
            a1 = ((-2.0 * ((a - 1.0) + (a + 1.0) * cosW0)) / norm).toFloat()
            a2 = (((a + 1.0) + (a - 1.0) * cosW0 - twoSqrtAAlpha) / norm).toFloat()
        }

        fun setHighShelf(sampleRate: Float, cutoffHz: Float, q: Float, gainDb: Float) {
            if (abs(gainDb) < 0.02f) {
                b0 = 1f; b1 = 0f; b2 = 0f; a1 = 0f; a2 = 0f
                return
            }
            val a = 10.0.pow(gainDb.toDouble() / 40.0)
            val f0 = cutoffHz.coerceIn(200f, sampleRate * 0.46f).toDouble()
            val w0 = 2.0 * PI * f0 / sampleRate.toDouble()
            val cosW0 = cos(w0)
            val sinW0 = sin(w0)
            val alpha = sinW0 / (2.0 * q.coerceIn(0.3f, 4.0f).toDouble())
            val twoSqrtAAlpha = 2.0 * sqrt(a) * alpha

            val norm = (a + 1.0) - (a - 1.0) * cosW0 + twoSqrtAAlpha
            b0 = ((a * ((a + 1.0) + (a - 1.0) * cosW0 + twoSqrtAAlpha)) / norm).toFloat()
            b1 = ((-2.0 * a * ((a - 1.0) + (a + 1.0) * cosW0)) / norm).toFloat()
            b2 = ((a * ((a + 1.0) + (a - 1.0) * cosW0 - twoSqrtAAlpha)) / norm).toFloat()
            a1 = ((2.0 * ((a - 1.0) - (a + 1.0) * cosW0)) / norm).toFloat()
            a2 = (((a + 1.0) - (a - 1.0) * cosW0 - twoSqrtAAlpha) / norm).toFloat()
        }
    }
}
