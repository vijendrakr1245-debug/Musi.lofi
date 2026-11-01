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
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Quantitative audio quality metrics measured on a PCM buffer (Phase 6 Validation).
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
)

/**
 * Rebuilt Professional Real-Time PCM Audio DSP Engine for LOFI WAVE.
 *
 * Supports:
 * - All 10 Live Sound Modes (Original Stereo, Live Lo-Fi, 8D Ultra, 3D Spatial HRTF,
 *   Ultra Surround, Bass Boost, Deep Bass, Soft Bass, Clear Vocal, Dreamy Sound)
 * - 10-Stage Analog Lo-Fi Pipeline (DC Blocker, Tone EQ, Symmetric Tape Saturation,
 *   Butterworth LPF, Wow/Flutter, Anti-Aliased Bitcrusher, Schroeder Reverb,
 *   Vinyl/Tape Texture, M/S Width, Soft-Knee Compressor, Stereo Echo Delay, True-Peak Limiter)
 */
@OptIn(UnstableApi::class)
class LofiDspAudioProcessor : BaseAudioProcessor() {

    @Volatile
    private var lofiParams: LofiDspParameters = LofiDspParameters()

    @Volatile
    private var eqState: EqualizerState = EqualizerState()

    @Volatile
    private var playbackQuality: AudioPlaybackQuality = AudioPlaybackQuality.HIGH_QUALITY

    @Volatile
    private var coeffsDirty: Boolean = true

    private val _visualizerFlow = MutableStateFlow(VisualizerSnapshot())
    val visualizerFlow: StateFlow<VisualizerSnapshot> = _visualizerFlow.asStateFlow()

    // Click-free parameter slewing states
    private var currentLofiMixEnvelope = 0.72f
    private var smoothedLowPassHz = 8800f
    private var smoothedWowDepth = 0.08f
    private var currentSlewedMasterGain = 1.0f
    private var fadeInEnvelope = 1.0f

    // Stage 1: DC Offset Blockers (Left = 0, Right = 1)
    private val dcPrevInput = FloatArray(2)
    private val dcPrevOutput = FloatArray(2)

    // Post-saturation DC Offset Blockers
    private val postSatDcPrevIn = FloatArray(2)
    private val postSatDcPrevOut = FloatArray(2)

    // Stage 2 & 4: Biquad Filters per channel (Left = 0, Right = 1)
    private val highPassFilters = Array(2) { BiquadFilter() }
    private val subBassShelfFilters = Array(2) { BiquadFilter() }
    private val bassShelfFilters = Array(2) { BiquadFilter() }
    private val midrangePeakFilters = Array(2) { BiquadFilter() }
    private val vocalMudCutFilters = Array(2) { BiquadFilter() }
    private val vocalPresenceFilters = Array(2) { BiquadFilter() }
    private val highShelfFilters = Array(2) { BiquadFilter() }
    private val lowPassFilters = Array(2) { BiquadFilter() }

    // Binaural HRTF 3D Spatial & Ultra Surround filters
    private val hrtfPinnaNotchL = BiquadFilter()
    private val hrtfPinnaNotchR = BiquadFilter()
    private val hrtfConchaPeakL = BiquadFilter()
    private val hrtfConchaPeakR = BiquadFilter()
    private val surroundSideHighPass = BiquadFilter()
    private var hrtfShadowLpL = 0f
    private var hrtfShadowLpR = 0f
    private var eightDRearLpL = 0f
    private var eightDRearLpR = 0f
    private var spatialOrbitPhase = 0.0

    // Binaural ITD / 8D / Surround fractional delay line (2048 samples per channel)
    private val spatialDelaySize = 2048
    private val spatialDelayMask = spatialDelaySize - 1
    private val spatialBufL = FloatArray(spatialDelaySize)
    private val spatialBufR = FloatArray(spatialDelaySize)
    private var spatialWriteIdx = 0

    // Stereo Ping-Pong / Tape Echo Delay Line (32768 samples per channel ~ 743ms at 44.1kHz)
    private val echoBufferSize = 32768
    private val echoBufferMask = echoBufferSize - 1
    private val echoBufL = FloatArray(echoBufferSize)
    private val echoBufR = FloatArray(echoBufferSize)
    private var echoWriteIdx = 0
    private var echoFeedbackLpL = 0f
    private var echoFeedbackLpR = 0f

    // Sub-harmonic octave divider state for Deep Bass / Sub Bass
    private var subFilterStateL = 0f
    private var subFilterStateR = 0f
    private var subFlipSignL = 1f
    private var subFlipSignR = 1f
    private var subPrevSignL = false
    private var subPrevSignR = false

    // Anti-aliasing pre/post filters for optional Bitcrusher
    private val bitcrushAntiAliasPre = Array(2) { BiquadFilter() }
    private val bitcrushAntiAliasPost = Array(2) { BiquadFilter() }
    private var lastAntiAliasCutoffHz = 0f

    // Dedicated biquad bandpass resonators for subtle stereo vinyl needle crackle
    private val crackleResonatorHighL = BiquadFilter()
    private val crackleResonatorHighR = BiquadFilter()
    private val crackleResonatorBodyL = BiquadFilter()
    private val crackleResonatorBodyR = BiquadFilter()

    // 10-band graphic EQ filters per channel (active only when Equalizer is explicitly enabled)
    private val eqBandFilters = Array(2) { Array(10) { BiquadFilter() } }
    private var eqHeadroomGain = 1.0f

    // Wow & Flutter circular fractional delay line (4096 samples per channel, power-of-2 mask)
    private val delayBufferSize = 4096
    private val delayBufferMask = delayBufferSize - 1
    private val wowDelayLeft = FloatArray(delayBufferSize)
    private val wowDelayRight = FloatArray(delayBufferSize)
    private var delayWriteIndex = 0
    private var wowPhase = 0.0
    private var flutterPhase = 0.0

    // Anti-aliased Bitcrusher & Sample-Rate Decimator state
    private var sampleHoldCounter = 0f
    private var heldSampleLeft = 0f
    private var heldSampleRight = 0f
    private var prevHeldLeft = 0f
    private var prevHeldRight = 0f

    // Gentle glue compressor & Peak limiter envelope states
    private var compEnvelope = 0f
    private var limiterEnvelope = 1.0f
    private var lastGainReductionDb = 0f

    // Subtle Vinyl Noise, 33.3 RPM platter modulation, and Tape Hiss state
    private var pinkB0L = 0f
    private var pinkB1L = 0f
    private var pinkB2L = 0f
    private var pinkB0R = 0f
    private var pinkB1R = 0f
    private var pinkB2R = 0f
    private var vinylPlatterPhase = 0.0
    private var tapePrevNoiseL = 0f
    private var tapePrevNoiseR = 0f
    private var tapeLpStateL = 0f
    private var tapeLpStateR = 0f
    private var prngState = 0x13579BDFu

    // Decorrelated Stereo Room Reverb (Schroeder/Freeverb tank)
    private val combDelaysL = intArrayOf(1116, 1188, 1277, 1356)
    private val combDelaysR = intArrayOf(1116 + 23, 1188 + 23, 1277 + 23, 1356 + 23)
    private val combBuffersL = Array(4) { FloatArray(combDelaysL[it]) }
    private val combBuffersR = Array(4) { FloatArray(combDelaysR[it]) }
    private val combIndicesL = IntArray(4)
    private val combIndicesR = IntArray(4)
    private val combFilterStoreL = FloatArray(4)
    private val combFilterStoreR = FloatArray(4)

    private val allPassDelaysL = intArrayOf(556, 441)
    private val allPassDelaysR = intArrayOf(556 + 23, 441 + 23)
    private val allPassBuffersL = Array(2) { FloatArray(allPassDelaysL[it]) }
    private val allPassBuffersR = Array(2) { FloatArray(allPassDelaysR[it]) }
    private val allPassIndicesL = IntArray(2)
    private val allPassIndicesR = IntArray(2)

    // Real-time Visualizer & Phase 6 Telemetry accumulators
    private val visWindowSize = 256
    private val visPcmRing = FloatArray(visWindowSize)
    private var visRingWritePos = 0
    private var visFrameCounter = 0
    private var accumSumSqL = 0f
    private var accumSumSqR = 0f
    private var accumSumL = 0f
    private var accumSumR = 0f
    private var accumPeak = 0f
    private var accumClipCount = 0
    private var accumSampleCount = 0
    private var lastBufferLatencyMs = 3.8f
    private var lastDspCpuTimeUs = 0f

    fun updateDspParameters(newParams: LofiDspParameters) {
        lofiParams = newParams
        coeffsDirty = true
    }

    fun updateEqualizerState(newEqState: EqualizerState) {
        eqState = newEqState
        coeffsDirty = true
    }

    fun updatePlaybackQuality(quality: AudioPlaybackQuality) {
        playbackQuality = quality
        coeffsDirty = true
    }

    fun getCurrentParameters(): LofiDspParameters = lofiParams
    fun getCurrentEqualizerState(): EqualizerState = eqState
    fun getPlaybackQuality(): AudioPlaybackQuality = playbackQuality

    private var prevSatInputL = 0f
    private var prevSatInputR = 0f

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        val enc = inputAudioFormat.encoding
        val supportedEncoding = enc == C.ENCODING_PCM_16BIT ||
            enc == C.ENCODING_PCM_24BIT ||
            enc == C.ENCODING_PCM_32BIT ||
            enc == C.ENCODING_PCM_FLOAT
        if (!supportedEncoding) {
            return AudioProcessor.AudioFormat.NOT_SET
        }
        if (inputAudioFormat.channelCount != 1 && inputAudioFormat.channelCount != 2) {
            return AudioProcessor.AudioFormat.NOT_SET
        }
        smoothedLowPassHz = lofiParams.lowPassCutoffHz.coerceIn(800f, 18000f)
        smoothedWowDepth = if (lofiParams.isLiveLofiEnabled) lofiParams.wowFlutterDepth.coerceIn(0f, 1f) else 0f
        currentLofiMixEnvelope = if (lofiParams.isLiveLofiEnabled && lofiParams.presetId != "original") {
            lofiParams.masterIntensity.coerceIn(0f, 1f)
        } else {
            0f
        }
        currentSlewedMasterGain = dbToLinear(lofiParams.masterGainDb.coerceIn(-24f, 12f))
        coeffsDirty = true
        return AudioProcessor.AudioFormat(
            inputAudioFormat.sampleRate,
            inputAudioFormat.channelCount,
            C.ENCODING_PCM_16BIT
        )
    }

    private fun dbToLinear(db: Float): Float {
        if (abs(db) < 0.01f) return 1.0f
        return 10.0.pow(db / 20.0).toFloat()
    }

    private fun fastRandomFloat(): Float {
        var x = prngState
        x = x xor (x shl 13)
        x = x xor (x shr 17)
        x = x xor (x shl 5)
        prngState = x
        return ((x and 0x7FFFFFFFu).toFloat() / 1073741824.0f) - 1.0f
    }

    private fun updateLowPassCoefficients(sampleRate: Float, cutoffHz: Float) {
        val safeSr = sampleRate.coerceAtLeast(8000f)
        val lpCutoff = cutoffHz.coerceIn(800f, min(18500f, safeSr * 0.46f))
        // Butterworth Q = 0.7071f ensures flat passband with zero artificial mid/bass bump
        for (ch in 0..1) {
            lowPassFilters[ch].setLowPass(safeSr, lpCutoff, 0.7071f)
        }
    }

    private fun updateAntiAliasFiltersIfNeeded(sampleRate: Float, srDivisor: Float) {
        val safeSr = sampleRate.coerceAtLeast(8000f)
        val targetNyquist = (safeSr / srDivisor.coerceIn(1f, 8f)) * 0.42f
        val clampedCutoff = targetNyquist.coerceIn(1200f, safeSr * 0.45f)
        if (abs(clampedCutoff - lastAntiAliasCutoffHz) > 25f) {
            lastAntiAliasCutoffHz = clampedCutoff
            for (ch in 0..1) {
                bitcrushAntiAliasPre[ch].setLowPass(safeSr, clampedCutoff, 0.7071f)
                bitcrushAntiAliasPost[ch].setLowPass(safeSr, clampedCutoff, 0.7071f)
            }
        }
    }

    private fun recalculateStaticFilters(sampleRate: Float) {
        val p = lofiParams
        val eq = eqState
        val safeSr = sampleRate.coerceAtLeast(8000f)

        val hpCutoff = p.highPassCutoffHz.coerceIn(20f, 350f)
        // Controlled bass boost (-6 dB to +6 dB effective shelf) so bass never muddies vocals
        val bassGain = p.warmBassBoostDb.coerceIn(-6f, 6f)
        // Gentle high-shelf at 4.5 kHz preserves 1 kHz - 4 kHz vocal intelligibility
        val netHighGain = (p.trebleGainDb - p.highFreqReductionDb * 0.45f).coerceIn(-8f, 6f)

        updateLowPassCoefficients(safeSr, smoothedLowPassHz)

        // Subtle warm vinyl crackle bandpass resonators (2.2 kHz soft needle + 680 Hz warm vinyl body)
        crackleResonatorHighL.setBandPass(safeSr, 2200f, 2.2f)
        crackleResonatorHighR.setBandPass(safeSr, 2120f, 2.2f)
        crackleResonatorBodyL.setBandPass(safeSr, 680f, 1.5f)
        crackleResonatorBodyR.setBandPass(safeSr, 650f, 1.5f)

        var maxPositiveEqBoost = 0f
        val mode = p.soundMode
        val modeSubGainDb = when (mode) {
            LiveSoundMode.DEEP_BASS -> 4.6f
            LiveSoundMode.BASS_BOOST -> 2.8f
            LiveSoundMode.SOFT_BASS -> 2.0f
            else -> 0.0f
        }
        val modeBassShelfDb = when (mode) {
            LiveSoundMode.BASS_BOOST -> (bassGain + 3.2f).coerceIn(-6f, 8f)
            LiveSoundMode.DEEP_BASS -> (bassGain + 2.4f).coerceIn(-6f, 8f)
            LiveSoundMode.SOFT_BASS -> (bassGain + 2.2f).coerceIn(-6f, 6f)
            LiveSoundMode.CLEAR_VOCAL -> -1.5f
            else -> bassGain
        }
        val midGain = p.midrangeGainDb.coerceIn(-12f, 12f)
        val vocalMudDb = when (mode) {
            LiveSoundMode.CLEAR_VOCAL -> -2.4f
            LiveSoundMode.DEEP_BASS, LiveSoundMode.BASS_BOOST -> -1.2f // Keep low-mids clean when bass is boosted
            else -> 0f
        }
        // Preserve clear vocals during Live Lo-Fi low-pass filtering as well as Clear Vocal mode
        val vocalPresDb = when {
            mode == LiveSoundMode.CLEAR_VOCAL -> 3.6f
            mode == LiveSoundMode.LIVE_LOFI && p.lowPassCutoffHz < 7800f -> 1.25f
            else -> 0f
        }

        // Configure HRTF Pinna & Concha biquads based on elevation (-45° .. +60°)
        val elevNorm = ((p.spatialElevationDeg.coerceIn(-45f, 60f) + 45f) / 105f).coerceIn(0f, 1f)
        val pinnaNotchHz = (6200f + elevNorm * 2800f).coerceIn(5000f, safeSr * 0.43f)
        hrtfPinnaNotchL.setPeakingEq(safeSr, pinnaNotchHz, 3.2f, -5.5f)
        hrtfPinnaNotchR.setPeakingEq(safeSr, pinnaNotchHz * 1.02f, 3.2f, -5.5f)
        hrtfConchaPeakL.setPeakingEq(safeSr, 3800f, 1.4f, 2.8f)
        hrtfConchaPeakR.setPeakingEq(safeSr, 3850f, 1.4f, 2.8f)
        surroundSideHighPass.setHighPass(safeSr, 180f, 0.7071f)

        for (ch in 0..1) {
            highPassFilters[ch].setHighPass(safeSr, hpCutoff, 0.7071f)
            subBassShelfFilters[ch].setLowShelf(safeSr, 58f, modeSubGainDb)
            bassShelfFilters[ch].setLowShelf(safeSr, 105f, modeBassShelfDb)
            midrangePeakFilters[ch].setPeakingEq(safeSr, 1150f, 0.95f, midGain)
            vocalMudCutFilters[ch].setPeakingEq(safeSr, 220f, 1.2f, vocalMudDb)
            vocalPresenceFilters[ch].setPeakingEq(safeSr, 2450f, 1.1f, vocalPresDb)
            highShelfFilters[ch].setHighShelf(safeSr, 4500f, netHighGain)

            val bands = eq.bandGainsDb
            for (b in 0 until 10) {
                val freq = EqualizerState.BAND_FREQUENCIES[b].toFloat()
                if (eq.isEnabled && freq < safeSr * 0.46f) {
                    var bandGain = if (b < bands.size) bands[b] else 0f
                    // Bass control reinforcement (31Hz, 62Hz, 125Hz)
                    when (b) {
                        0 -> bandGain += eq.bassBoostDb * 0.55f
                        1 -> bandGain += eq.bassBoostDb * 0.75f
                        2 -> bandGain += eq.bassBoostDb * 0.45f
                    }
                    // Midrange control reinforcement (500Hz, 1kHz, 2kHz)
                    when (b) {
                        4 -> bandGain += eq.midrangeBoostDb * 0.55f
                        5 -> bandGain += eq.midrangeBoostDb * 0.80f
                        6 -> bandGain += eq.midrangeBoostDb * 0.55f
                    }
                    // Treble control reinforcement (4kHz, 8kHz, 16kHz)
                    when (b) {
                        7 -> bandGain += eq.trebleBoostDb * 0.45f
                        8 -> bandGain += eq.trebleBoostDb * 0.75f
                        9 -> bandGain += eq.trebleBoostDb * 0.60f
                    }
                    val clampedBandGain = bandGain.coerceIn(-12f, 12f)
                    if (ch == 0 && clampedBandGain > maxPositiveEqBoost) {
                        maxPositiveEqBoost = clampedBandGain
                    }
                    eqBandFilters[ch][b].setPeakingEq(safeSr, freq, 1.15f, clampedBandGain)
                } else {
                    eqBandFilters[ch][b].setIdentity()
                }
            }
        }
        // Automatic EQ headroom compensation when user boosts graphic EQ bands
        eqHeadroomGain = if (eq.isEnabled && maxPositiveEqBoost > 1.5f) {
            dbToLinear(-maxPositiveEqBoost * 0.45f)
        } else {
            1.0f
        }
        coeffsDirty = false
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        val enc = inputAudioFormat.encoding
        val sr = inputAudioFormat.sampleRate.takeIf { it > 0 } ?: 44100
        val ch = inputAudioFormat.channelCount.coerceIn(1, 2)

        if (enc == C.ENCODING_PCM_16BIT) {
            val outputBuffer = replaceOutputBuffer(remaining)
            processPcmBufferInternal(
                inputBuffer = inputBuffer,
                outputBuffer = outputBuffer,
                sampleRate = sr,
                channels = ch
            )
            outputBuffer.flip()
        } else {
            // Convert 24-bit, 32-bit, or 32-bit float PCM to 16-bit PCM buffer for unified 64-bit biquad DSP
            val bytesPerSample = when (enc) {
                C.ENCODING_PCM_24BIT -> 3
                C.ENCODING_PCM_32BIT, C.ENCODING_PCM_FLOAT -> 4
                else -> 2
            }
            val sampleCount = remaining / bytesPerSample
            val pcm16Bytes = sampleCount * 2
            val tempPcm16 = ByteBuffer.allocate(pcm16Bytes).order(ByteOrder.LITTLE_ENDIAN)
            inputBuffer.order(ByteOrder.LITTLE_ENDIAN)
            for (s in 0 until sampleCount) {
                val normalized = when (enc) {
                    C.ENCODING_PCM_24BIT -> {
                        val b0 = inputBuffer.get().toInt() and 0xFF
                        val b1 = inputBuffer.get().toInt() and 0xFF
                        val b2 = inputBuffer.get().toInt() // signed MSB
                        val int24 = (b2 shl 16) or (b1 shl 8) or b0
                        (int24 / 8388608.0f).coerceIn(-1f, 1f)
                    }
                    C.ENCODING_PCM_32BIT -> {
                        val int32 = inputBuffer.int
                        (int32 / 2147483648.0f).coerceIn(-1f, 1f)
                    }
                    C.ENCODING_PCM_FLOAT -> {
                        val flt = inputBuffer.float
                        if (flt.isNaN() || flt.isInfinite()) 0f else flt.coerceIn(-1f, 1f)
                    }
                    else -> 0f
                }
                tempPcm16.putShort((normalized * 32767.0f).roundToInt().coerceIn(-32768, 32767).toShort())
            }
            tempPcm16.flip()
            val outputBuffer = replaceOutputBuffer(pcm16Bytes)
            processPcmBufferInternal(
                inputBuffer = tempPcm16,
                outputBuffer = outputBuffer,
                sampleRate = sr,
                channels = ch
            )
            outputBuffer.flip()
        }
    }

    /**
     * Processes a PCM16 little-endian [inputBuffer] into [outputBuffer] and returns a full
     * [AudioQualityReport] for Phase 6 testing and validation.
     */
    fun processAndMeasureBuffer(
        inputBuffer: ByteBuffer,
        outputBuffer: ByteBuffer,
        sampleRate: Int = 44100,
        channels: Int = 2
    ): AudioQualityReport {
        val startNs = System.nanoTime()
        inputBuffer.order(ByteOrder.LITTLE_ENDIAN)
        outputBuffer.order(ByteOrder.LITTLE_ENDIAN)

        val report = processPcmBufferInternal(
            inputBuffer = inputBuffer,
            outputBuffer = outputBuffer,
            sampleRate = sampleRate,
            channels = channels
        )
        val elapsedMs = (System.nanoTime() - startNs) / 1_000_000f
        return report.copy(processingLatencyMs = elapsedMs)
    }

    private fun processPcmBufferInternal(
        inputBuffer: ByteBuffer,
        outputBuffer: ByteBuffer,
        sampleRate: Int,
        channels: Int
    ): AudioQualityReport {
        val startNs = System.nanoTime()
        val remaining = inputBuffer.remaining()
        val frameSize = 2 * channels
        val frameCount = remaining / frameSize
        if (frameCount <= 0) {
            return AudioQualityReport(0f, -96f, 0f, 0f, -96f, 0, 0f, 0f, 0f, 0, 0f, 0f)
        }

        val srFloat = sampleRate.toFloat().coerceAtLeast(8000f)
        lastBufferLatencyMs = (frameCount.toFloat() / srFloat) * 1000f

        val p = lofiParams
        val eq = eqState
        val qualityMode = playbackQuality

        // Smoothly slew Low-Pass cutoff across buffers
        val targetLpCutoff = p.lowPassCutoffHz.coerceIn(800f, 18000f)
        if (abs(smoothedLowPassHz - targetLpCutoff) > 1.0f) {
            smoothedLowPassHz += 0.25f * (targetLpCutoff - smoothedLowPassHz)
            updateLowPassCoefficients(srFloat, smoothedLowPassHz)
        } else if (smoothedLowPassHz != targetLpCutoff) {
            smoothedLowPassHz = targetLpCutoff
            updateLowPassCoefficients(srFloat, smoothedLowPassHz)
        }

        if (coeffsDirty) {
            recalculateStaticFilters(srFloat)
        }

        val mode = p.soundMode
        val isSpecializedSoundMode = mode != LiveSoundMode.ORIGINAL_STEREO && mode != LiveSoundMode.LIVE_LOFI
        val isLofiEffectivelyOn = when (mode) {
            LiveSoundMode.ORIGINAL_STEREO -> false
            LiveSoundMode.LIVE_LOFI -> p.isLiveLofiEnabled && p.presetId != "original"
            else -> p.isLiveLofiEnabled || isSpecializedSoundMode
        }
        val targetLofiMix = if (isLofiEffectivelyOn) {
            if (isSpecializedSoundMode && p.masterIntensity < 0.1f) 0.80f else p.masterIntensity.coerceIn(0f, 1f)
        } else {
            0f
        }
        val mixStep = 1.0f / (srFloat * 0.025f) // 25ms click-free crossfade
        val targetMasterGain = dbToLinear(p.masterGainDb.coerceIn(-24f, 12f))
        val gainSlewStep = 1.0f / (srFloat * 0.020f) // 20ms click-free volume ramp
        val fadeStep = if (eq.smoothTransitions && eq.fadeInOutMs > 0) {
            1.0f / (srFloat * (eq.fadeInOutMs / 1000f))
        } else {
            1.0f
        }

        outputBuffer.order(ByteOrder.LITTLE_ENDIAN)
        inputBuffer.order(ByteOrder.LITTLE_ENDIAN)

        // DC blocker pole coefficient (~18 Hz cutoff)
        val dcPoleR = (1.0f - (2.0 * PI * 18.0 / srFloat)).toFloat().coerceIn(0.990f, 0.9995f)

        // Controlled bass headroom compensation so warm bass boost never causes mud or clipping
        val effectiveBassBoost = p.warmBassBoostDb + when (mode) {
            LiveSoundMode.DEEP_BASS -> 3.5f
            LiveSoundMode.BASS_BOOST -> 2.8f
            LiveSoundMode.SOFT_BASS -> 1.8f
            else -> 0f
        }
        val bassTrim = if (effectiveBassBoost > 1.5f) {
            dbToLinear(-(effectiveBassBoost - 1.5f) * 0.24f)
        } else {
            1.0f
        }

        // Echo delay parameters
        val echoWet = p.echoMix.coerceIn(0f, 1f) * 0.55f
        val echoDelaySamples = ((p.echoDelayMs.coerceIn(40f, 680f) / 1000f) * srFloat)
            .toInt()
            .coerceIn(64, echoBufferSize - 8)
        val echoFb = p.echoFeedback.coerceIn(0f, 0.82f)

        // Spatial / 8D / Binaural HRTF parameters
        val spatialAmt = p.spatialIntensity.coerceIn(0f, 1f)
        val orbitRateHz = p.eightDRotationSpeedHz.coerceIn(0.03f, 0.75f)
        val orbitPhaseInc = (2.0 * PI * orbitRateHz) / srFloat
        val maxItdSamples = (0.00066f * srFloat).coerceIn(4f, 42f) // ~0.66ms Woodworth spherical head max ITD

        // Bitcrusher & anti-aliasing setup (strictly bypassed when bitDepth >= 15.9 and srDivisor <= 1.02)
        val clampedBitDepth = p.bitDepth.coerceIn(6f, 16f)
        val srDivisor = p.sampleRateDivisor.coerceIn(1f, 8f)
        val bitcrusherActive = clampedBitDepth < 15.9f || srDivisor > 1.02f
        if (bitcrusherActive) {
            updateAntiAliasFiltersIfNeeded(srFloat, srDivisor)
        }
        val halfScaleLevels = (2.0.pow((clampedBitDepth - 1f).toDouble())).toFloat()
        val invHalfScaleLevels = 1.0f / halfScaleLevels
        val ditherScale = if (clampedBitDepth < 15.5f) invHalfScaleLevels * 0.15f else 0f

        val satAmount = (p.analogSaturation + p.softDistortion * 0.6f).coerceIn(0f, 1f)
        val crackleAmt = p.vinylCrackle.coerceIn(0f, 1f)
        val vinylNoiseAmt = p.vinylNoise.coerceIn(0f, 1f)
        val tapeHissAmt = p.tapeHiss.coerceIn(0f, 1f)

        val targetWowDepth = if (isLofiEffectivelyOn) p.wowFlutterDepth.coerceIn(0f, 1f) else 0f
        val wowDepthSlew = 1.0f / (srFloat * 0.040f)
        val wowRate = p.wowFlutterRateHz.coerceIn(0.2f, 4.0f)
        val wowPhaseInc = (2.0 * PI * wowRate) / srFloat
        val flutterPhaseInc = (2.0 * PI * (wowRate * 3.8f)) / srFloat
        val platterPhaseInc = (2.0 * PI * 0.555) / srFloat // 33.3 RPM vinyl rotation

        // Natural Room Reverb parameters (scaled for clarity so vocals stay upfront)
        val reverbWet = p.reverbMix.coerceIn(0f, 1f) * 0.26f
        val reverbRoom = (0.62f + p.reverbRoomSize.coerceIn(0f, 1f) * 0.24f)
        val reverbDamp = 0.42f

        // Constant-power Stereo Width
        val stereoWidth = p.stereoWidth.coerceIn(0f, 2f)
        val widthNorm = if (stereoWidth > 1.0f) {
            (1.0f / sqrt((1.0f + stereoWidth * stereoWidth) * 0.5f)).coerceIn(0.65f, 1.0f)
        } else {
            1.0f
        }

        val compAmount = p.softCompression.coerceIn(0f, 1f)
        val balance = eq.stereoBalance.coerceIn(-1f, 1f)
        val leftPanGain = if (balance > 0f) 1f - balance else 1f
        val rightPanGain = if (balance < 0f) 1f + balance else 1f

        var bufSumSqL = 0f
        var bufSumSqR = 0f
        var bufSumL = 0f
        var bufSumR = 0f
        var bufPeak = 0f
        var bufClipCount = 0

        for (i in 0 until frameCount) {
            // 1. Decode signed 16-bit PCM to normalized float [-1.0, 1.0]
            val rawShortL = inputBuffer.short
            val rawShortR = if (channels == 2) inputBuffer.short else rawShortL
            val dryL = rawShortL / 32768.0f
            val dryR = rawShortR / 32768.0f

            // Slew crossfade & master volume envelopes per sample
            if (currentLofiMixEnvelope < targetLofiMix) {
                currentLofiMixEnvelope = min(targetLofiMix, currentLofiMixEnvelope + mixStep)
            } else if (currentLofiMixEnvelope > targetLofiMix) {
                currentLofiMixEnvelope = max(targetLofiMix, currentLofiMixEnvelope - mixStep)
            }

            if (currentSlewedMasterGain < targetMasterGain) {
                currentSlewedMasterGain = min(targetMasterGain, currentSlewedMasterGain + gainSlewStep)
            } else if (currentSlewedMasterGain > targetMasterGain) {
                currentSlewedMasterGain = max(targetMasterGain, currentSlewedMasterGain - gainSlewStep)
            }

            if (smoothedWowDepth < targetWowDepth) {
                smoothedWowDepth = min(targetWowDepth, smoothedWowDepth + wowDepthSlew)
            } else if (smoothedWowDepth > targetWowDepth) {
                smoothedWowDepth = max(targetWowDepth, smoothedWowDepth - wowDepthSlew)
            }

            if (fadeInEnvelope < 1.0f) {
                fadeInEnvelope = min(1.0f, fadeInEnvelope + fadeStep)
            }

            var outL = dryL
            var outR = dryR

            // Always keep delay line populated with clean signal so toggling Lo-Fi never clicks
            if (currentLofiMixEnvelope <= 0.0002f) {
                wowDelayLeft[delayWriteIndex] = dryL
                wowDelayRight[delayWriteIndex] = dryR
                delayWriteIndex = (delayWriteIndex + 1) and delayBufferMask
            }

            // Execute Lo-Fi DSP Pipeline when active or during smooth crossfade
            if (currentLofiMixEnvelope > 0.0002f) {
                // STAGE A: DC Offset Removal (18 Hz 1st-Order High-Pass Blocker)
                var lofiL = dryL - dcPrevInput[0] + dcPoleR * dcPrevOutput[0]
                dcPrevInput[0] = dryL
                dcPrevOutput[0] = lofiL

                var lofiR = dryR - dcPrevInput[1] + dcPoleR * dcPrevOutput[1]
                dcPrevInput[1] = dryR
                dcPrevOutput[1] = lofiR

                // STAGE B: Gentle Tone EQ (Sub-sonic High-Pass + Sub/Warm Bass Shelf + Midrange/Vocal + 4.5kHz High Shelf)
                lofiL = highPassFilters[0].process(lofiL)
                lofiR = highPassFilters[1].process(lofiR)

                if (mode == LiveSoundMode.DEEP_BASS || mode == LiveSoundMode.BASS_BOOST || mode == LiveSoundMode.SOFT_BASS) {
                    lofiL = subBassShelfFilters[0].process(lofiL)
                    lofiR = subBassShelfFilters[1].process(lofiR)

                    // Subtle sub-harmonic octave synthesizer for DEEP_BASS mode
                    if (mode == LiveSoundMode.DEEP_BASS) {
                        subFilterStateL += 0.014f * (lofiL - subFilterStateL)
                        subFilterStateR += 0.014f * (lofiR - subFilterStateR)
                        val signL = subFilterStateL >= 0f
                        if (signL && !subPrevSignL) subFlipSignL = -subFlipSignL
                        subPrevSignL = signL
                        val signR = subFilterStateR >= 0f
                        if (signR && !subPrevSignR) subFlipSignR = -subFlipSignR
                        subPrevSignR = signR
                        lofiL += subFilterStateL * subFlipSignL * 0.14f
                        lofiR += subFilterStateR * subFlipSignR * 0.14f
                    }
                }

                lofiL = bassShelfFilters[0].process(lofiL) * bassTrim
                lofiR = bassShelfFilters[1].process(lofiR) * bassTrim

                if (abs(p.midrangeGainDb) > 0.05f) {
                    lofiL = midrangePeakFilters[0].process(lofiL)
                    lofiR = midrangePeakFilters[1].process(lofiR)
                }

                if (mode == LiveSoundMode.CLEAR_VOCAL || mode == LiveSoundMode.DEEP_BASS || mode == LiveSoundMode.BASS_BOOST) {
                    lofiL = vocalMudCutFilters[0].process(lofiL)
                    lofiR = vocalMudCutFilters[1].process(lofiR)
                }

                if (mode == LiveSoundMode.CLEAR_VOCAL || (mode == LiveSoundMode.LIVE_LOFI && p.lowPassCutoffHz < 7800f)) {
                    lofiL = vocalPresenceFilters[0].process(lofiL)
                    lofiR = vocalPresenceFilters[1].process(lofiR)
                }

                lofiL = highShelfFilters[0].process(lofiL)
                lofiR = highShelfFilters[1].process(lofiR)

                // STAGE C: Symmetric Warm Analog Tape Saturation (Zero DC Bias, Unity Gain Compensated)
                if (satAmount > 0.005f) {
                    if (qualityMode == AudioPlaybackQuality.MAXIMUM_AVAILABLE_QUALITY) {
                        // 2x linear-oversampled analog tape saturation reduces harmonic aliasing
                        val midSampleL = 0.5f * (prevSatInputL + lofiL)
                        val midSampleR = 0.5f * (prevSatInputR + lofiR)
                        prevSatInputL = lofiL
                        prevSatInputR = lofiR
                        lofiL = 0.5f * (warmTapeSaturate(midSampleL, satAmount) + warmTapeSaturate(lofiL, satAmount))
                        lofiR = 0.5f * (warmTapeSaturate(midSampleR, satAmount) + warmTapeSaturate(lofiR, satAmount))
                    } else {
                        prevSatInputL = lofiL
                        prevSatInputR = lofiR
                        lofiL = warmTapeSaturate(lofiL, satAmount)
                        lofiR = warmTapeSaturate(lofiR, satAmount)
                    }

                    // Post-saturation DC blocker ensures strictly zero DC drift
                    val cleanL = lofiL - postSatDcPrevIn[0] + dcPoleR * postSatDcPrevOut[0]
                    postSatDcPrevIn[0] = lofiL
                    postSatDcPrevOut[0] = cleanL
                    lofiL = cleanL

                    val cleanR = lofiR - postSatDcPrevIn[1] + dcPoleR * postSatDcPrevOut[1]
                    postSatDcPrevIn[1] = lofiR
                    postSatDcPrevOut[1] = cleanR
                    lofiR = cleanR
                }

                // STAGE D: Gentle 2nd-Order Butterworth Low-Pass Filter (Q = 0.7071, Clear Vocals)
                lofiL = lowPassFilters[0].process(lofiL)
                lofiR = lowPassFilters[1].process(lofiR)

                // STAGE E: Natural Tape Wow & Flutter (True Positive-Modulo Fractional Delay Interpolation)
                wowDelayLeft[delayWriteIndex] = lofiL
                wowDelayRight[delayWriteIndex] = lofiR
                if (smoothedWowDepth > 0.002f) {
                    wowPhase += wowPhaseInc
                    if (wowPhase > 2.0 * PI) wowPhase -= 2.0 * PI
                    flutterPhase += flutterPhaseInc
                    if (flutterPhase > 2.0 * PI) flutterPhase -= 2.0 * PI

                    val modLfo = (sin(wowPhase) * 0.80 + sin(flutterPhase) * 0.20).toFloat()
                    // Subtle musical delay excursion (max ~0.8ms center ± 0.55ms at 100% depth)
                    val centerDelaySamples = smoothedWowDepth * (srFloat * 0.0008f) + 2.0f
                    val currentDelay = (centerDelaySamples + modLfo * smoothedWowDepth * (srFloat * 0.00055f))
                        .coerceIn(1.0f, (delayBufferSize - 4).toFloat())

                    var readPos = delayWriteIndex.toFloat() - currentDelay
                    while (readPos < 0f) {
                        readPos += delayBufferSize.toFloat()
                    }
                    val readInt = readPos.toInt()
                    val frac = readPos - readInt.toFloat()
                    val idx0 = readInt and delayBufferMask
                    val idx1 = (idx0 + 1) and delayBufferMask

                    lofiL = wowDelayLeft[idx0] * (1.0f - frac) + wowDelayLeft[idx1] * frac
                    lofiR = wowDelayRight[idx0] * (1.0f - frac) + wowDelayRight[idx1] * frac
                }
                delayWriteIndex = (delayWriteIndex + 1) and delayBufferMask

                // STAGE F: Optional Anti-Aliased Bitcrusher (Pre-Filter -> Decimate & Quantize -> Post-Filter)
                if (bitcrusherActive) {
                    lofiL = bitcrushAntiAliasPre[0].process(lofiL)
                    lofiR = bitcrushAntiAliasPre[1].process(lofiR)

                    if (srDivisor > 1.02f) {
                        sampleHoldCounter += 1.0f
                        if (sampleHoldCounter >= srDivisor) {
                            sampleHoldCounter -= srDivisor
                            prevHeldLeft = heldSampleLeft
                            prevHeldRight = heldSampleRight
                            heldSampleLeft = lofiL
                            heldSampleRight = lofiR
                        }
                        val interp = (sampleHoldCounter / srDivisor).coerceIn(0f, 1f)
                        lofiL = prevHeldLeft + (heldSampleLeft - prevHeldLeft) * (0.65f + 0.35f * interp)
                        lofiR = prevHeldRight + (heldSampleRight - prevHeldRight) * (0.65f + 0.35f * interp)
                    }

                    if (clampedBitDepth < 15.9f) {
                        val dither = (fastRandomFloat() - fastRandomFloat()) * ditherScale
                        lofiL = ((lofiL + dither) * halfScaleLevels).roundToInt() * invHalfScaleLevels
                        lofiR = ((lofiR - dither) * halfScaleLevels).roundToInt() * invHalfScaleLevels
                    }

                    lofiL = bitcrushAntiAliasPost[0].process(lofiL.coerceIn(-1f, 1f))
                    lofiR = bitcrushAntiAliasPost[1].process(lofiR.coerceIn(-1f, 1f))
                }

                // STAGE G: Natural Decorrelated Stereo Room Reverb
                if (reverbWet > 0.002f) {
                    // Preserve stereo separation in reverb send
                    val revInL = lofiL * 0.20f + lofiR * 0.05f
                    val revInR = lofiR * 0.20f + lofiL * 0.05f
                    var revOutL = 0f
                    var revOutR = 0f

                    for (c in 0 until 4) {
                        val bufL = combBuffersL[c]
                        val idxL = combIndicesL[c]
                        val delayedL = bufL[idxL]
                        combFilterStoreL[c] = (delayedL * (1f - reverbDamp)) + (combFilterStoreL[c] * reverbDamp)
                        bufL[idxL] = revInL + combFilterStoreL[c] * reverbRoom
                        combIndicesL[c] = if (idxL + 1 >= bufL.size) 0 else idxL + 1
                        revOutL += delayedL

                        val bufR = combBuffersR[c]
                        val idxR = combIndicesR[c]
                        val delayedR = bufR[idxR]
                        combFilterStoreR[c] = (delayedR * (1f - reverbDamp)) + (combFilterStoreR[c] * reverbDamp)
                        bufR[idxR] = revInR + combFilterStoreR[c] * reverbRoom
                        combIndicesR[c] = if (idxR + 1 >= bufR.size) 0 else idxR + 1
                        revOutR += delayedR
                    }

                    // Normalize parallel comb sum so reverb tank never builds excessive gain
                    revOutL *= 0.25f
                    revOutR *= 0.25f

                    // Canonical Schroeder All-Pass Diffusers
                    for (a in 0 until 2) {
                        val apBufL = allPassBuffersL[a]
                        val apIdxL = allPassIndicesL[a]
                        val delayedL = apBufL[apIdxL]
                        val inL = revOutL
                        val outApL = -0.5f * inL + delayedL
                        apBufL[apIdxL] = inL + 0.5f * outApL
                        revOutL = outApL
                        allPassIndicesL[a] = if (apIdxL + 1 >= apBufL.size) 0 else apIdxL + 1

                        val apBufR = allPassBuffersR[a]
                        val apIdxR = allPassIndicesR[a]
                        val delayedR = apBufR[apIdxR]
                        val inR = revOutR
                        val outApR = -0.5f * inR + delayedR
                        apBufR[apIdxR] = inR + 0.5f * outApR
                        revOutR = outApR
                        allPassIndicesR[a] = if (apIdxR + 1 >= apBufR.size) 0 else apIdxR + 1
                    }

                    lofiL = lofiL * (1f - reverbWet * 0.25f) + revOutL * reverbWet
                    lofiR = lofiR * (1f - reverbWet * 0.25f) + revOutR * reverbWet
                }

                // STAGE H: Subtle Stereo Vinyl Noise, Warm Dust Crackle & Band-Limited Tape Hiss
                if (crackleAmt > 0.002f || vinylNoiseAmt > 0.002f || tapeHissAmt > 0.002f) {
                    val whiteL = fastRandomFloat()
                    val whiteR = fastRandomFloat()

                    // 1. Subtle Band-Limited Vinyl Crackle (soft-excited resonator, never harsh clicks)
                    var crackleOutL = 0f
                    var crackleOutR = 0f
                    if (crackleAmt > 0.002f) {
                        val popThreshold = 1.0f - (crackleAmt * 0.0022f)
                        val popL = if (abs(whiteL) > popThreshold) whiteL * crackleAmt * 0.08f else 0f
                        val popR = if (abs(whiteR) > popThreshold) whiteR * crackleAmt * 0.08f else 0f
                        val needleInL = popL + popR * 0.25f
                        val needleInR = popR + popL * 0.25f
                        crackleOutL = crackleResonatorHighL.process(needleInL) * 0.55f +
                            crackleResonatorBodyL.process(needleInL) * 0.45f
                        crackleOutR = crackleResonatorHighR.process(needleInR) * 0.55f +
                            crackleResonatorBodyR.process(needleInR) * 0.45f
                    }

                    // 2. Subtle 33.3 RPM Pink Vinyl Groove Floor (No muddy 48Hz sine tone)
                    var vinylFloorL = 0f
                    var vinylFloorR = 0f
                    if (vinylNoiseAmt > 0.002f) {
                        pinkB0L = 0.99765f * pinkB0L + whiteL * 0.0990460f
                        pinkB1L = 0.96300f * pinkB1L + whiteL * 0.2965164f
                        pinkB2L = 0.57000f * pinkB2L + whiteL * 1.0526913f
                        val pinkL = (pinkB0L + pinkB1L + pinkB2L + whiteL * 0.1848f) * 0.035f

                        pinkB0R = 0.99765f * pinkB0R + whiteR * 0.0990460f
                        pinkB1R = 0.96300f * pinkB1R + whiteR * 0.2965164f
                        pinkB2R = 0.57000f * pinkB2R + whiteR * 1.0526913f
                        val pinkR = (pinkB0R + pinkB1R + pinkB2R + whiteR * 0.1848f) * 0.035f

                        vinylPlatterPhase += platterPhaseInc
                        if (vinylPlatterPhase > 2.0 * PI) vinylPlatterPhase -= 2.0 * PI
                        val platterSwish = 1.0f + 0.18f * sin(vinylPlatterPhase).toFloat()

                        vinylFloorL = pinkL * platterSwish * vinylNoiseAmt * 0.045f
                        vinylFloorR = pinkR * platterSwish * vinylNoiseAmt * 0.045f
                    }

                    // 3. Smooth Band-Limited Magnetic Tape Hiss
                    var hissL = 0f
                    var hissR = 0f
                    if (tapeHissAmt > 0.002f) {
                        val hpNoiseL = (whiteL - tapePrevNoiseL) * 0.5f
                        val hpNoiseR = (whiteR - tapePrevNoiseR) * 0.5f
                        tapePrevNoiseL = whiteL
                        tapePrevNoiseR = whiteR
                        tapeLpStateL += 0.35f * (hpNoiseL - tapeLpStateL)
                        tapeLpStateR += 0.35f * (hpNoiseR - tapeLpStateR)
                        hissL = tapeLpStateL * tapeHissAmt * 0.008f
                        hissR = tapeLpStateR * tapeHissAmt * 0.008f
                    }

                    lofiL += crackleOutL + vinylFloorL + hissL
                    lofiR += crackleOutR + vinylFloorR + hissR
                }

                // STAGE I: Constant-Power Stereo Width (Mid/Side Matrix) & Specialized Spatial Modes
                if (abs(stereoWidth - 1.0f) > 0.01f) {
                    val mid = (lofiL + lofiR) * 0.5f * widthNorm
                    val side = (lofiL - lofiR) * 0.5f * stereoWidth * widthNorm
                    lofiL = mid + side
                    lofiR = mid - side
                }

                // Write current signal into spatial delay line for 8D / Binaural HRTF / Ultra Surround
                spatialBufL[spatialWriteIdx] = lofiL
                spatialBufR[spatialWriteIdx] = lofiR

                when (mode) {
                    LiveSoundMode.EIGHT_D_ULTRA -> {
                        // Continuous 360° 8D Orbit with smooth L/R panning, interaural delay, rear shadow & cross-feed
                        spatialOrbitPhase += orbitPhaseInc
                        if (spatialOrbitPhase > 2.0 * PI) spatialOrbitPhase -= 2.0 * PI
                        val sinAz = sin(spatialOrbitPhase).toFloat()
                        val cosAz = cos(spatialOrbitPhase).toFloat()

                        val delayL = (1.0f + max(0f, sinAz) * maxItdSamples * spatialAmt)
                        val delayR = (1.0f + max(0f, -sinAz) * maxItdSamples * spatialAmt)
                        val tapL = readSpatialDelay(spatialBufL, spatialWriteIdx, delayL)
                        val tapR = readSpatialDelay(spatialBufR, spatialWriteIdx, delayR)

                        // Rear-hemisphere subtle head shadow when source orbits behind the listener (cosAz < 0)
                        val rearFactor = if (cosAz < 0f) (-cosAz * 0.45f * spatialAmt) else 0f
                        eightDRearLpL += 0.22f * (tapL - eightDRearLpL)
                        eightDRearLpR += 0.22f * (tapR - eightDRearLpR)
                        val shadedL = tapL * (1f - rearFactor) + eightDRearLpL * rearFactor
                        val shadedR = tapR * (1f - rearFactor) + eightDRearLpR * rearFactor

                        // Constant-power panning with headphone cross-feed (never leaves one ear completely empty)
                        val panAngle = ((sinAz * 0.82f * spatialAmt + 1.0f) * 0.25f * PI).toFloat()
                        val gainL = cos(panAngle)
                        val gainR = sin(panAngle)
                        val monoSum = (shadedL + shadedR) * 0.5f
                        lofiL = (shadedL * 0.65f + monoSum * 0.35f) * gainL * 1.22f + shadedR * 0.14f
                        lofiR = (shadedR * 0.65f + monoSum * 0.35f) * gainR * 1.22f + shadedL * 0.14f
                    }

                    LiveSoundMode.THREE_D_SPATIAL -> {
                        // Parametric Binaural HRTF Pipeline:
                        // 1. Woodworth Spherical-Head Interaural Time Difference (ITD)
                        // 2. Frequency-Dependent Head-Shadow Diffraction (ILD)
                        // 3. Pinna Elevation Notch (6.2kHz-9kHz) + Concha Resonance Peak (3.8kHz)
                        // 4. Distance law + Early binaural room reflection taps
                        if (p.spatialAutoOrbit) {
                            spatialOrbitPhase += orbitPhaseInc * 0.45
                            if (spatialOrbitPhase > 2.0 * PI) spatialOrbitPhase -= 2.0 * PI
                        }
                        val azRad = if (p.spatialAutoOrbit) {
                            (sin(spatialOrbitPhase) * (PI * 0.42)).toFloat()
                        } else {
                            (p.spatialAzimuthDeg.coerceIn(-180f, 180f) * (PI / 180.0)).toFloat()
                        }
                        val sinAz = sin(azRad)
                        val absTheta = abs(azRad).coerceAtMost((PI * 0.5).toFloat())
                        // Woodworth formula: normalized ITD in [0..1]
                        val woodworthNorm = ((absTheta + sin(absTheta)) / (PI * 0.5 + 1.0)).toFloat().coerceIn(0f, 1f)
                        val itdSamples = woodworthNorm * maxItdSamples * spatialAmt

                        val delayEarL = if (sinAz > 0f) 2.0f + itdSamples else 2.0f
                        val delayEarR = if (sinAz < 0f) 2.0f + itdSamples else 2.0f
                        var earL = readSpatialDelay(spatialBufL, spatialWriteIdx, delayEarL)
                        var earR = readSpatialDelay(spatialBufR, spatialWriteIdx, delayEarR)

                        // Early lateral wall reflections (~11ms & ~17ms) scaled by virtual distance
                        val dist = p.spatialDistance.coerceIn(0.05f, 1.0f)
                        val earlyTapL = readSpatialDelay(spatialBufR, spatialWriteIdx, min((spatialDelaySize - 4).toFloat(), srFloat * 0.011f))
                        val earlyTapR = readSpatialDelay(spatialBufL, spatialWriteIdx, min((spatialDelaySize - 4).toFloat(), srFloat * 0.014f))

                        // Head-shadow ILD low-pass diffraction + spherical-head broadband ILD
                        val shadowL = max(0f, sinAz) * spatialAmt * 0.68f
                        val shadowR = max(0f, -sinAz) * spatialAmt * 0.68f
                        hrtfShadowLpL += 0.14f * (earL - hrtfShadowLpL)
                        hrtfShadowLpR += 0.14f * (earR - hrtfShadowLpR)
                        val ildGainL = (1.0f - max(0f, sinAz) * spatialAmt * 0.36f + max(0f, -sinAz) * spatialAmt * 0.16f).coerceIn(0.45f, 1.25f)
                        val ildGainR = (1.0f - max(0f, -sinAz) * spatialAmt * 0.36f + max(0f, sinAz) * spatialAmt * 0.16f).coerceIn(0.45f, 1.25f)
                        earL = (earL * (1f - shadowL) + hrtfShadowLpL * shadowL) * ildGainL
                        earR = (earR * (1f - shadowR) + hrtfShadowLpR * shadowR) * ildGainR

                        // Pinna Elevation Notch & Concha Resonance Cues
                        val pinnaL = hrtfConchaPeakL.process(hrtfPinnaNotchL.process(earL))
                        val pinnaR = hrtfConchaPeakR.process(hrtfPinnaNotchR.process(earR))
                        val directAtten = (1.0f - dist * 0.22f).coerceIn(0.72f, 1.0f)

                        lofiL = (earL * (1f - spatialAmt * 0.5f) + pinnaL * (spatialAmt * 0.5f)) * directAtten +
                            earlyTapL * (0.12f * dist * spatialAmt)
                        lofiR = (earR * (1f - spatialAmt * 0.5f) + pinnaR * (spatialAmt * 0.5f)) * directAtten +
                            earlyTapR * (0.12f * dist * spatialAmt)
                    }

                    LiveSoundMode.ULTRA_SURROUND -> {
                        // Controlled Mid/Side stereo widening with High-Passed Side (>180Hz)
                        // Preserves a rock-solid center image and 100% mono compatibility
                        val mid = (lofiL + lofiR) * 0.5f
                        val rawSide = (lofiL - lofiR) * 0.5f
                        val cleanSide = surroundSideHighPass.process(rawSide)
                        val delayedMid = readSpatialDelay(spatialBufL, spatialWriteIdx, (srFloat * 0.0065f).coerceAtMost(320f)) -
                            readSpatialDelay(spatialBufR, spatialWriteIdx, (srFloat * 0.0065f).coerceAtMost(320f))
                        val surroundSide = cleanSide * (1.0f + 0.65f * spatialAmt) + delayedMid * (0.18f * spatialAmt)
                        val norm = (1.0f / sqrt(1.0f + 0.35f * spatialAmt)).coerceIn(0.78f, 1.0f)
                        lofiL = (mid + surroundSide) * norm
                        lofiR = (mid - surroundSide) * norm
                    }

                    LiveSoundMode.DREAMY_SOUND -> {
                        // Lush dual-tap stereo chorus modulation
                        spatialOrbitPhase += (2.0 * PI * 0.42) / srFloat
                        if (spatialOrbitPhase > 2.0 * PI) spatialOrbitPhase -= 2.0 * PI
                        val chorusDelayL = 12.0f + (1.0f + sin(spatialOrbitPhase).toFloat()) * (srFloat * 0.0032f)
                        val chorusDelayR = 12.0f + (1.0f + cos(spatialOrbitPhase).toFloat()) * (srFloat * 0.0032f)
                        val chL = readSpatialDelay(spatialBufL, spatialWriteIdx, chorusDelayL)
                        val chR = readSpatialDelay(spatialBufR, spatialWriteIdx, chorusDelayR)
                        lofiL = lofiL * 0.72f + chL * 0.28f
                        lofiR = lofiR * 0.72f + chR * 0.28f
                    }

                    else -> Unit
                }
                spatialWriteIdx = (spatialWriteIdx + 1) and spatialDelayMask

                // STAGE I2: Stereo Tape / Ping-Pong Echo Delay Line (when Echo > 0%)
                if (echoWet > 0.004f) {
                    val readEchoIdxL = (echoWriteIdx - echoDelaySamples) and echoBufferMask
                    val readEchoIdxR = (echoWriteIdx - ((echoDelaySamples * 3) / 4).coerceAtLeast(32)) and echoBufferMask
                    val delayedEchoL = echoBufL[readEchoIdxL]
                    val delayedEchoR = echoBufR[readEchoIdxR]

                    // Warm low-pass damping in feedback loop
                    echoFeedbackLpL += 0.35f * (delayedEchoL - echoFeedbackLpL)
                    echoFeedbackLpR += 0.35f * (delayedEchoR - echoFeedbackLpR)

                    echoBufL[echoWriteIdx] = (lofiL + echoFeedbackLpR * echoFb).coerceIn(-1.2f, 1.2f)
                    echoBufR[echoWriteIdx] = (lofiR + echoFeedbackLpL * echoFb).coerceIn(-1.2f, 1.2f)
                    echoWriteIdx = (echoWriteIdx + 1) and echoBufferMask

                    lofiL = lofiL * (1f - echoWet * 0.25f) + delayedEchoL * echoWet
                    lofiR = lofiR * (1f - echoWet * 0.25f) + delayedEchoR * echoWet
                } else {
                    echoBufL[echoWriteIdx] = lofiL
                    echoBufR[echoWriteIdx] = lofiR
                    echoWriteIdx = (echoWriteIdx + 1) and echoBufferMask
                }

                // STAGE J: Gentle Glue Compression (High threshold, slow attack — never pumps or crushes volume)
                if (compAmount > 0.01f) {
                    val absPeak = max(abs(lofiL), abs(lofiR))
                    val attackCoeff = 0.0015f // ~15ms smooth attack preserves vocal & drum transients
                    val releaseCoeff = 0.0002f // ~110ms natural release
                    compEnvelope += if (absPeak > compEnvelope) {
                        attackCoeff * (absPeak - compEnvelope)
                    } else {
                        releaseCoeff * (absPeak - compEnvelope)
                    }
                    val threshold = 0.65f
                    if (compEnvelope > threshold) {
                        val excess = compEnvelope - threshold
                        val compressed = threshold + excess / (1.0f + compAmount * 0.85f)
                        val makeup = 1.0f + compAmount * 0.06f
                        val gainFactor = (compressed / compEnvelope.coerceAtLeast(0.0001f)) * makeup
                        lofiL *= gainFactor
                        lofiR *= gainFactor
                        lastGainReductionDb = 20f * log10(gainFactor.coerceAtLeast(0.25f))
                    } else {
                        lastGainReductionDb *= 0.995f
                    }
                }

                // Crossfade Dry (Original Hi-Fi) and Wet (Lo-Fi DSP)
                outL = dryL * (1.0f - currentLofiMixEnvelope) + lofiL * currentLofiMixEnvelope
                outR = dryR * (1.0f - currentLofiMixEnvelope) + lofiR * currentLofiMixEnvelope
            }

            // STAGE K: Optional 10-Band Equalizer (only when explicitly enabled by user)
            if (eq.isEnabled) {
                for (b in 0 until 10) {
                    outL = eqBandFilters[0][b].process(outL)
                    outR = eqBandFilters[1][b].process(outR)
                }
                outL *= eqHeadroomGain
                outR *= eqHeadroomGain
            }

            // STAGE L: Proper Slewed Master Gain & Stereo Balance
            val totalGainL = currentSlewedMasterGain * leftPanGain * fadeInEnvelope
            val totalGainR = currentSlewedMasterGain * rightPanGain * fadeInEnvelope
            outL *= totalGainL
            outR *= totalGainR

            // STAGE M: Transparent True-Peak Limiter (-0.3 dBFS / 0.965 ceiling, guarantees 0 clipping)
            val preLimitPeak = max(abs(outL), abs(outR))
            val ceiling = 0.965f
            val targetLimGain = if (preLimitPeak > ceiling) {
                ceiling / preLimitPeak
            } else {
                1.0f
            }
            if (targetLimGain < limiterEnvelope) {
                limiterEnvelope = targetLimGain // Instant peak catch prevents any sample from clipping
            } else {
                limiterEnvelope += 0.001f * (1.0f - limiterEnvelope) // Smooth 22ms release
            }
            outL *= limiterEnvelope
            outR *= limiterEnvelope

            // Soft-knee ceiling polish between 0.90 and 0.965
            outL = transparentPeakCeiling(outL)
            outR = transparentPeakCeiling(outR)

            // Track Phase 6 validation metrics on final output
            val finalPeak = max(abs(outL), abs(outR))
            if (finalPeak > bufPeak) bufPeak = finalPeak
            if (finalPeak >= 0.999f) {
                bufClipCount++
                accumClipCount++
            }
            bufSumSqL += outL * outL
            bufSumSqR += outR * outR
            bufSumL += outL
            bufSumR += outR

            // Encode rounded 16-bit PCM back to output buffer
            val finalShortL = (outL * 32767.0f).roundToInt().coerceIn(-32768, 32767).toShort()
            outputBuffer.putShort(finalShortL)
            if (channels == 2) {
                val finalShortR = (outR * 32767.0f).roundToInt().coerceIn(-32768, 32767).toShort()
                outputBuffer.putShort(finalShortR)
            }

            // Accumulate live PCM samples for real-time Visualizer
            accumSumSqL += outL * outL
            accumSumSqR += outR * outR
            accumSumL += outL
            accumSumR += outR
            if (finalPeak > accumPeak) accumPeak = finalPeak
            accumSampleCount++
            if ((accumSampleCount and 3) == 0) {
                visPcmRing[visRingWritePos] = (outL + outR) * 0.5f
                visRingWritePos = (visRingWritePos + 1) and (visWindowSize - 1)
            }
            visFrameCounter++
            if (visFrameCounter >= 1470) {
                emitVisualizerSnapshot(srFloat)
                visFrameCounter = 0
            }
        }

        val elapsedUs = (System.nanoTime() - startNs) / 1000f
        lastDspCpuTimeUs = elapsedUs

        val rmsL = sqrt(bufSumSqL / frameCount).coerceIn(0f, 1f)
        val rmsR = sqrt(bufSumSqR / frameCount).coerceIn(0f, 1f)
        val rmsComb = sqrt((bufSumSqL + bufSumSqR) / (frameCount * 2f)).coerceIn(0f, 1f)
        val rmsDb = if (rmsComb > 0.00001f) 20f * log10(rmsComb) else -96f
        val peakDb = if (bufPeak > 0.00001f) 20f * log10(bufPeak) else -96f
        val dcL = bufSumL / frameCount
        val dcR = bufSumR / frameCount
        val balDb = if (rmsL > 0.0001f && rmsR > 0.0001f) {
            20f * log10(rmsL / rmsR)
        } else {
            0f
        }

        return AudioQualityReport(
            peakLevelLinear = bufPeak,
            peakLevelDb = peakDb,
            rmsLeftLinear = rmsL,
            rmsRightLinear = rmsR,
            rmsCombinedDb = rmsDb,
            clippingCount = bufClipCount,
            dcOffsetLeft = dcL,
            dcOffsetRight = dcR,
            channelBalanceDb = balDb,
            underrunCount = AudioEngineBridge.pcmAudioEngine.getUnderrunCount(),
            processingLatencyMs = elapsedUs / 1000f,
            bufferDurationMs = lastBufferLatencyMs
        )
    }

    /**
     * Symmetric Warm Analog Tape Saturator:
     * Uses a smooth Padé rational tanh approximation with exact small-signal unity gain compensation
     * and zero even-order DC offset.
     */
    private fun warmTapeSaturate(x: Float, amount: Float): Float {
        val drive = 1.0f + amount * 1.35f
        val driven = (x * drive).coerceIn(-2.5f, 2.5f)
        val x2 = driven * driven
        val saturated = driven * (27.0f + x2) / (27.0f + 9.0f * x2)
        // Normalize by small-signal gain so adding warmth does not artificially bloat volume or clip
        val normDrive = drive * (27.0f + 1.0f) / (27.0f + 9.0f)
        val gainComp = 1.0f / (1.0f + (normDrive - 1.0f) * 0.65f)
        return saturated * gainComp
    }

    /**
     * Transparent soft-knee ceiling limiter between 0.90 and 0.965 (-0.31 dBFS).
     * Guarantees samples never reach ±1.000 while keeping waveforms below 0.90 100% linear.
     */
    private fun transparentPeakCeiling(x: Float): Float {
        val absX = abs(x)
        if (absX <= 0.90f) return x
        val sign = if (x >= 0f) 1f else -1f
        val excess = absX - 0.90f
        return sign * (0.90f + 0.065f * (excess / (0.065f + excess)))
    }

    private fun emitVisualizerSnapshot(sampleRate: Float) {
        val count = accumSampleCount.coerceAtLeast(1)
        val rmsL = sqrt(accumSumSqL / count).coerceIn(0f, 1f)
        val rmsR = sqrt(accumSumSqR / count).coerceIn(0f, 1f)
        val dcL = accumSumL / count
        val dcR = accumSumR / count
        val peakLin = accumPeak.coerceIn(0f, 1f)
        val channelBalDb = if (rmsL > 0.0001f && rmsR > 0.0001f) {
            20f * log10(rmsL / rmsR)
        } else {
            0f
        }

        accumSumSqL = 0f
        accumSumSqR = 0f
        accumSumL = 0f
        accumSumR = 0f
        accumPeak = 0f
        accumSampleCount = 0

        val wave = FloatArray(64)
        val pos = visRingWritePos
        for (i in 0 until 64) {
            val idx = (pos + i * 4) and (visWindowSize - 1)
            wave[i] = visPcmRing[idx].coerceIn(-1f, 1f)
        }

        val spec = FloatArray(32)
        for (b in 0 until 32) {
            val normFreq = 0.012f + (b / 31.0f).pow(1.65f) * 0.42f
            val omega = (2.0 * PI * normFreq).toFloat()
            val coeff = 2.0f * cos(omega)
            var q1 = 0f
            var q2 = 0f
            for (n in 0 until 96) {
                val sampleIdx = (pos + n * 2) and (visWindowSize - 1)
                val q0 = coeff * q1 - q2 + visPcmRing[sampleIdx]
                q2 = q1
                q1 = q0
            }
            val magSq = q1 * q1 + q2 * q2 - q1 * q2 * coeff
            val mag = (sqrt(max(0f, magSq)) / 18.0f).coerceIn(0f, 1f)
            spec[b] = mag
        }

        val peakDb = if (peakLin > 0.00001f) 20f * log10(peakLin) else -60f
        val p = lofiParams
        val effSr = if (p.isLiveLofiEnabled && p.sampleRateDivisor > 1.02f) {
            (sampleRate / p.sampleRateDivisor.coerceIn(1f, 8f)).toInt()
        } else {
            sampleRate.toInt()
        }

        _visualizerFlow.value = VisualizerSnapshot(
            waveform = wave,
            spectrum = spec,
            rmsLeft = rmsL,
            rmsRight = rmsR,
            peakDb = peakDb.coerceIn(-60f, 0f),
            peakLevelLinear = peakLin,
            gainReductionDb = lastGainReductionDb,
            isLofiActive = currentLofiMixEnvelope > 0.05f,
            effectiveSampleRateHz = effSr,
            activeLowPassHz = smoothedLowPassHz.toInt(),
            activeBitDepth = if (p.isLiveLofiEnabled) p.bitDepth else 16.0f,
            bufferLatencyMs = lastBufferLatencyMs,
            clippingCount = accumClipCount,
            dcOffsetLeft = dcL,
            dcOffsetRight = dcR,
            channelBalanceDb = channelBalDb,
            underrunCount = AudioEngineBridge.pcmAudioEngine.getUnderrunCount(),
            dspCpuTimeUs = lastDspCpuTimeUs
        )
    }

    /**
     * Reads a linearly-interpolated fractional delay sample from a power-of-2 circular buffer.
     */
    private fun readSpatialDelay(buffer: FloatArray, writeIndex: Int, delaySamples: Float): Float {
        val clampedDelay = delaySamples.coerceIn(1.0f, (spatialDelaySize - 4).toFloat())
        var readPos = writeIndex.toFloat() - clampedDelay
        while (readPos < 0f) {
            readPos += spatialDelaySize.toFloat()
        }
        val readInt = readPos.toInt()
        val frac = readPos - readInt.toFloat()
        val idx0 = readInt and spatialDelayMask
        val idx1 = (idx0 + 1) and spatialDelayMask
        return buffer[idx0] * (1.0f - frac) + buffer[idx1] * frac
    }

    override fun onFlush() {
        fadeInEnvelope = 0.25f
        limiterEnvelope = 1.0f
        dcPrevInput.fill(0f)
        dcPrevOutput.fill(0f)
        postSatDcPrevIn.fill(0f)
        postSatDcPrevOut.fill(0f)
        wowDelayLeft.fill(0f)
        wowDelayRight.fill(0f)
        spatialBufL.fill(0f)
        spatialBufR.fill(0f)
        echoBufL.fill(0f)
        echoBufR.fill(0f)
        echoFeedbackLpL = 0f
        echoFeedbackLpR = 0f
        hrtfShadowLpL = 0f
        hrtfShadowLpR = 0f
        eightDRearLpL = 0f
        eightDRearLpR = 0f
        for (c in 0 until 4) {
            combBuffersL[c].fill(0f)
            combBuffersR[c].fill(0f)
            combFilterStoreL[c] = 0f
            combFilterStoreR[c] = 0f
        }
        for (a in 0 until 2) {
            allPassBuffersL[a].fill(0f)
            allPassBuffersR[a].fill(0f)
        }
    }

    override fun onReset() {
        onFlush()
        accumClipCount = 0
    }

    /**
     * Direct Form II Transposed Biquad Filter for zero-click real-time audio filtering.
     */
    private class BiquadFilter {
        private var b0 = 1f
        private var b1 = 0f
        private var b2 = 0f
        private var a1 = 0f
        private var a2 = 0f
        private var z1 = 0f
        private var z2 = 0f

        fun setIdentity() {
            b0 = 1f; b1 = 0f; b2 = 0f; a1 = 0f; a2 = 0f
        }

        fun setLowPass(sampleRate: Float, cutoffHz: Float, q: Float) {
            val w0 = (2.0 * PI * cutoffHz / sampleRate).toFloat()
            val cosW0 = cos(w0)
            val sinW0 = sin(w0)
            val alpha = sinW0 / (2.0f * q)
            val a0 = 1.0f + alpha
            b0 = ((1.0f - cosW0) * 0.5f) / a0
            b1 = (1.0f - cosW0) / a0
            b2 = ((1.0f - cosW0) * 0.5f) / a0
            a1 = (-2.0f * cosW0) / a0
            a2 = (1.0f - alpha) / a0
        }

        fun setHighPass(sampleRate: Float, cutoffHz: Float, q: Float) {
            val w0 = (2.0 * PI * cutoffHz / sampleRate).toFloat()
            val cosW0 = cos(w0)
            val sinW0 = sin(w0)
            val alpha = sinW0 / (2.0f * q)
            val a0 = 1.0f + alpha
            b0 = ((1.0f + cosW0) * 0.5f) / a0
            b1 = -(1.0f + cosW0) / a0
            b2 = ((1.0f + cosW0) * 0.5f) / a0
            a1 = (-2.0f * cosW0) / a0
            a2 = (1.0f - alpha) / a0
        }

        fun setBandPass(sampleRate: Float, centerHz: Float, q: Float) {
            val safeCenter = centerHz.coerceIn(60f, sampleRate * 0.45f)
            val w0 = (2.0 * PI * safeCenter / sampleRate).toFloat()
            val cosW0 = cos(w0)
            val sinW0 = sin(w0)
            val alpha = sinW0 / (2.0f * q)
            val a0 = 1.0f + alpha
            b0 = alpha / a0
            b1 = 0f
            b2 = -alpha / a0
            a1 = (-2.0f * cosW0) / a0
            a2 = (1.0f - alpha) / a0
        }

        fun setLowShelf(sampleRate: Float, freqHz: Float, gainDb: Float) {
            if (abs(gainDb) < 0.05f) {
                setIdentity()
                return
            }
            val a = 10.0.pow(gainDb / 40.0).toFloat()
            val w0 = (2.0 * PI * freqHz / sampleRate).toFloat()
            val cosW0 = cos(w0)
            val sinW0 = sin(w0)
            val alpha = sinW0 * 0.5f * sqrt(2.0f)
            val twoSqrtAAlpha = 2.0f * sqrt(a) * alpha

            val a0 = (a + 1f) + (a - 1f) * cosW0 + twoSqrtAAlpha
            b0 = (a * ((a + 1f) - (a - 1f) * cosW0 + twoSqrtAAlpha)) / a0
            b1 = (2f * a * ((a - 1f) - (a + 1f) * cosW0)) / a0
            b2 = (a * ((a + 1f) - (a - 1f) * cosW0 - twoSqrtAAlpha)) / a0
            a1 = (-2f * ((a - 1f) + (a + 1f) * cosW0)) / a0
            a2 = ((a + 1f) + (a - 1f) * cosW0 - twoSqrtAAlpha) / a0
        }

        fun setHighShelf(sampleRate: Float, freqHz: Float, gainDb: Float) {
            if (abs(gainDb) < 0.05f) {
                setIdentity()
                return
            }
            val a = 10.0.pow(gainDb / 40.0).toFloat()
            val w0 = (2.0 * PI * freqHz / sampleRate).toFloat()
            val cosW0 = cos(w0)
            val sinW0 = sin(w0)
            val alpha = sinW0 * 0.5f * sqrt(2.0f)
            val twoSqrtAAlpha = 2.0f * sqrt(a) * alpha

            val a0 = (a + 1f) - (a - 1f) * cosW0 + twoSqrtAAlpha
            b0 = (a * ((a + 1f) + (a - 1f) * cosW0 + twoSqrtAAlpha)) / a0
            b1 = (-2f * a * ((a - 1f) + (a + 1f) * cosW0)) / a0
            b2 = (a * ((a + 1f) - (a - 1f) * cosW0 - twoSqrtAAlpha)) / a0
            a1 = (2f * ((a - 1f) - (a + 1f) * cosW0)) / a0
            a2 = ((a + 1f) - (a - 1f) * cosW0 - twoSqrtAAlpha) / a0
        }

        fun setPeakingEq(sampleRate: Float, freqHz: Float, q: Float, gainDb: Float) {
            if (abs(gainDb) < 0.05f) {
                setIdentity()
                return
            }
            val a = 10.0.pow(gainDb / 40.0).toFloat()
            val w0 = (2.0 * PI * freqHz / sampleRate).toFloat()
            val cosW0 = cos(w0)
            val sinW0 = sin(w0)
            val alpha = sinW0 / (2.0f * q)

            val a0 = 1.0f + alpha / a
            b0 = (1.0f + alpha * a) / a0
            b1 = (-2.0f * cosW0) / a0
            b2 = (1.0f - alpha * a) / a0
            a1 = (-2.0f * cosW0) / a0
            a2 = (1.0f - alpha / a) / a0
        }

        fun process(x: Float): Float {
            val y = b0 * x + z1
            z1 = b1 * x - a1 * y + z2
            z2 = b2 * x - a2 * y
            return y
        }
    }
}
