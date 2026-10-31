package com.example.audio

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import com.example.model.EqualizerState
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
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Core Real-Time PCM Audio DSP Engine for LOFI WAVE.
 *
 * Engineered for low-latency, zero-allocation real-time processing of 16-bit PCM audio frames
 * inside ExoPlayer's audio rendering pipeline.
 *
 * Core DSP stages:
 * 1. Zipper-Free Slew-Smoothed Resonant Low-Pass Filter & High-Pass Filter (Cascaded Biquads)
 * 2. Low-Latency Vintage Sampler Bitcrusher (Mid-tread Bit-Depth Quantizer + Sample-Rate Decimator)
 * 3. Multi-Layer Stereo Vinyl Noise & Crackle Synthesizer (33.3 RPM Platter Swish + Dual Bandpass Needle Crackle + 48Hz Rumble)
 * 4. Dual-LFO Circular Fractional Delay Line for Tape Wow & Flutter
 * 5. Warm Tube Saturation, Schroeder Stereo Reverb, Mid/Side Stereo Width, Soft-Knee Compression & 10-Band EQ
 */
@OptIn(UnstableApi::class)
class LofiDspAudioProcessor : BaseAudioProcessor() {

    @Volatile
    private var lofiParams: LofiDspParameters = LofiDspParameters()

    @Volatile
    private var eqState: EqualizerState = EqualizerState()

    @Volatile
    private var coeffsDirty: Boolean = true

    private val _visualizerFlow = MutableStateFlow(VisualizerSnapshot())
    val visualizerFlow: StateFlow<VisualizerSnapshot> = _visualizerFlow.asStateFlow()

    // Smooth crossfade state for seamless Hi-Fi <-> Live Lo-Fi switching without audio clicks
    private var currentLofiMixEnvelope = 1.0f

    // Smooth low-pass cutoff slew state to eliminate zipper noise when sweeping cutoff live
    private var smoothedLowPassHz = 3200f

    // Fade-in envelope on track start/seek
    private var fadeInEnvelope = 0.0f

    // Biquad filters per channel (Left = 0, Right = 1)
    private val lowPassStage1Filters = Array(2) { BiquadFilter() }
    private val lowPassStage2Filters = Array(2) { BiquadFilter() }
    private val highPassFilters = Array(2) { BiquadFilter() }
    private val bassShelfFilters = Array(2) { BiquadFilter() }
    private val highShelfFilters = Array(2) { BiquadFilter() }

    // Dedicated biquad bandpass resonators for realistic stereo vinyl needle crackle
    private val crackleResonatorHighL = BiquadFilter()
    private val crackleResonatorHighR = BiquadFilter()
    private val crackleResonatorBodyL = BiquadFilter()
    private val crackleResonatorBodyR = BiquadFilter()

    // 10-band graphic EQ filters per channel
    private val eqBandFilters = Array(2) { Array(10) { BiquadFilter() } }

    // Wow & Flutter circular fractional delay line (4096 samples per channel)
    private val delayBufferSize = 4096
    private val delayBufferMask = delayBufferSize - 1
    private val wowDelayLeft = FloatArray(delayBufferSize)
    private val wowDelayRight = FloatArray(delayBufferSize)
    private var delayWriteIndex = 0
    private var wowPhase = 0.0
    private var flutterPhase = 0.0

    // Low-latency Bitcrusher & Sample-Rate Decimator state
    private var sampleHoldCounter = 0f
    private var heldSampleLeft = 0f
    private var heldSampleRight = 0f
    private var prevCrushedLeft = 0f
    private var prevCrushedRight = 0f

    // Soft compressor envelope follower state
    private var compEnvelope = 0f
    private var lastGainReductionDb = 0f

    // Multi-layer Vinyl Noise, 33.3 RPM platter swish, and Tape Hiss state
    private var pinkB0L = 0f
    private var pinkB1L = 0f
    private var pinkB2L = 0f
    private var pinkB0R = 0f
    private var pinkB1R = 0f
    private var pinkB2R = 0f
    private var rumblePhase = 0.0
    private var vinylPlatterPhase = 0.0 // 33.3 RPM = 0.555 Hz cyclic groove modulation
    private var tapePrevNoiseL = 0f
    private var tapePrevNoiseR = 0f
    private var prngState = 0x13579BDFu

    // Real-time stereo Schroeder/Freeverb reverb tanks
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

    // Visualizer accumulation buffer (lock-free ring for UI visualization)
    private val visWindowSize = 256
    private val visPcmRing = FloatArray(visWindowSize)
    private var visRingWritePos = 0
    private var visFrameCounter = 0
    private var accumSumSqL = 0f
    private var accumSumSqR = 0f
    private var accumSampleCount = 0
    private var lastBufferLatencyMs = 3.8f

    fun updateDspParameters(newParams: LofiDspParameters) {
        lofiParams = newParams
        coeffsDirty = true
    }

    fun updateEqualizerState(newEqState: EqualizerState) {
        eqState = newEqState
        coeffsDirty = true
    }

    fun getCurrentParameters(): LofiDspParameters = lofiParams
    fun getCurrentEqualizerState(): EqualizerState = eqState

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            return AudioProcessor.AudioFormat.NOT_SET
        }
        if (inputAudioFormat.channelCount != 1 && inputAudioFormat.channelCount != 2) {
            return AudioProcessor.AudioFormat.NOT_SET
        }
        smoothedLowPassHz = lofiParams.lowPassCutoffHz.coerceIn(400f, 18000f)
        coeffsDirty = true
        return inputAudioFormat
    }

    private fun fastRandomFloat(): Float {
        // Lock-free Xorshift32 PRNG for deterministic, zero-allocation audio thread noise synthesis
        var x = prngState
        x = x xor (x shl 13)
        x = x xor (x shr 17)
        x = x xor (x shl 5)
        prngState = x
        return ((x and 0x7FFFFFFFu).toFloat() / 1073741824.0f) - 1.0f
    }

    private fun updateLowPassCoefficients(sampleRate: Float, cutoffHz: Float) {
        val safeSr = sampleRate.coerceAtLeast(8000f)
        val lpCutoff = cutoffHz.coerceIn(400f, min(18000f, safeSr * 0.45f))
        // Warm analog resonant stage 1 (Q = 0.84) + gentle roll-off stage 2
        for (ch in 0..1) {
            lowPassStage1Filters[ch].setLowPass(safeSr, lpCutoff, 0.84f)
            lowPassStage2Filters[ch].setLowPass(safeSr, (lpCutoff * 1.35f).coerceAtMost(safeSr * 0.46f), 0.65f)
        }
    }

    private fun recalculateStaticFilters(sampleRate: Float) {
        val p = lofiParams
        val eq = eqState
        val safeSr = sampleRate.coerceAtLeast(8000f)

        val hpCutoff = p.highPassCutoffHz.coerceIn(20f, 600f)
        val bassGain = p.warmBassBoostDb.coerceIn(-6f, 12f)
        val netHighGain = (p.trebleGainDb - p.highFreqReductionDb).coerceIn(-18f, 12f)

        updateLowPassCoefficients(safeSr, smoothedLowPassHz)

        // Configure vinyl crackle bandpass resonators (2.7kHz needle impulse + 750Hz vinyl body)
        crackleResonatorHighL.setBandPass(safeSr, 2750f, 3.1f)
        crackleResonatorHighR.setBandPass(safeSr, 2620f, 3.1f)
        crackleResonatorBodyL.setBandPass(safeSr, 740f, 1.8f)
        crackleResonatorBodyR.setBandPass(safeSr, 710f, 1.8f)

        for (ch in 0..1) {
            highPassFilters[ch].setHighPass(safeSr, hpCutoff, 0.707f)
            bassShelfFilters[ch].setLowShelf(safeSr, 95f, bassGain)
            highShelfFilters[ch].setHighShelf(safeSr, 4000f, netHighGain)

            val bands = eq.bandGainsDb
            for (b in 0 until 10) {
                val freq = EqualizerState.BAND_FREQUENCIES[b].toFloat()
                if (freq < safeSr * 0.46f) {
                    var bandGain = if (b < bands.size) bands[b] else 0f
                    if (b <= 1) bandGain += eq.bassBoostDb * 0.6f
                    if (b >= 7) bandGain += eq.trebleBoostDb * 0.6f
                    eqBandFilters[ch][b].setPeakingEq(safeSr, freq, 1.41f, bandGain.coerceIn(-15f, 15f))
                } else {
                    eqBandFilters[ch][b].setIdentity()
                }
            }
        }
        coeffsDirty = false
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        val sampleRate = inputAudioFormat.sampleRate.toFloat().coerceAtLeast(44100f)
        val channels = inputAudioFormat.channelCount
        val frameCount = remaining / (2 * channels)
        lastBufferLatencyMs = (frameCount.toFloat() / sampleRate) * 1000f

        val p = lofiParams
        val eq = eqState

        // Zipper-free low-pass cutoff slewing across buffers
        val targetLpCutoff = p.lowPassCutoffHz.coerceIn(400f, 18000f)
        if (abs(smoothedLowPassHz - targetLpCutoff) > 1.5f) {
            smoothedLowPassHz += 0.35f * (targetLpCutoff - smoothedLowPassHz)
            updateLowPassCoefficients(sampleRate, smoothedLowPassHz)
        } else if (smoothedLowPassHz != targetLpCutoff) {
            smoothedLowPassHz = targetLpCutoff
            updateLowPassCoefficients(sampleRate, smoothedLowPassHz)
        }

        if (coeffsDirty) {
            recalculateStaticFilters(sampleRate)
        }

        val targetLofiMix = if (p.isLiveLofiEnabled) p.masterIntensity.coerceIn(0f, 1f) else 0f
        val mixStep = 1.0f / (sampleRate * 0.030f) // 30ms click-free transition
        val fadeStep = if (eq.smoothTransitions && eq.fadeInOutMs > 0) {
            1.0f / (sampleRate * (eq.fadeInOutMs / 1000f))
        } else {
            1.0f
        }

        val outputBuffer = replaceOutputBuffer(remaining)
        outputBuffer.order(ByteOrder.LITTLE_ENDIAN)
        inputBuffer.order(ByteOrder.LITTLE_ENDIAN)

        // Precompute per-buffer Bitcrusher, Low-Pass & Vinyl Noise constants
        val clampedBitDepth = p.bitDepth.coerceIn(4f, 16f)
        // Half-scale levels for signed [-1.0, 1.0] mid-tread quantizer
        val halfScaleLevels = (2.0.pow((clampedBitDepth - 1f).toDouble())).toFloat()
        val invHalfScaleLevels = 1.0f / halfScaleLevels
        val ditherScale = if (clampedBitDepth < 15.5f) invHalfScaleLevels * 0.18f else 0f
        val srDivisor = p.sampleRateDivisor.coerceIn(1f, 10f)

        val satAmount = p.analogSaturation.coerceIn(0f, 1f)
        val distAmount = p.softDistortion.coerceIn(0f, 1f)
        val crackleAmt = p.vinylCrackle.coerceIn(0f, 1f)
        val vinylNoiseAmt = p.vinylNoise.coerceIn(0f, 1f)
        val tapeHissAmt = p.tapeHiss.coerceIn(0f, 1f)
        val wowDepth = p.wowFlutterDepth.coerceIn(0f, 1f)
        val wowRate = p.wowFlutterRateHz.coerceIn(0.2f, 5f)
        val wowPhaseInc = (2.0 * PI * wowRate) / sampleRate
        val flutterPhaseInc = (2.0 * PI * (wowRate * 4.4f)) / sampleRate
        val rumblePhaseInc = (2.0 * PI * 48.0) / sampleRate
        val platterPhaseInc = (2.0 * PI * 0.555) / sampleRate // 33.3 RPM vinyl rotation

        val reverbWet = p.reverbMix.coerceIn(0f, 1f) * 0.45f
        val reverbRoom = (0.70f + p.reverbRoomSize.coerceIn(0f, 1f) * 0.26f)
        val reverbDamp = 0.35f

        val stereoWidth = p.stereoWidth.coerceIn(0f, 2f)
        val compAmount = p.softCompression.coerceIn(0f, 1f)
        val balance = eq.stereoBalance.coerceIn(-1f, 1f)
        val leftPanGain = if (balance > 0f) 1f - balance else 1f
        val rightPanGain = if (balance < 0f) 1f + balance else 1f

        for (i in 0 until frameCount) {
            // Read 16-bit PCM normalized to [-1.0, 1.0]
            val rawShortL = inputBuffer.short
            val rawShortR = if (channels == 2) inputBuffer.short else rawShortL
            val dryL = (rawShortL / 32768.0f)
            val dryR = (rawShortR / 32768.0f)

            // Smooth crossfade envelope for Live Lo-Fi vs Hi-Fi
            if (currentLofiMixEnvelope < targetLofiMix) {
                currentLofiMixEnvelope = min(targetLofiMix, currentLofiMixEnvelope + mixStep)
            } else if (currentLofiMixEnvelope > targetLofiMix) {
                currentLofiMixEnvelope = max(targetLofiMix, currentLofiMixEnvelope - mixStep)
            }

            if (fadeInEnvelope < 1.0f) {
                fadeInEnvelope = min(1.0f, fadeInEnvelope + fadeStep)
            }

            var outL = dryL
            var outR = dryR

            // Run Live Lo-Fi DSP Chain if active or crossfading
            if (currentLofiMixEnvelope > 0.0005f) {
                var lofiL = dryL
                var lofiR = dryR

                // STAGE 1: High-Pass, Warm Bass Shelf & Dual-Stage Resonant Low-Pass Filter
                lofiL = highPassFilters[0].process(lofiL)
                lofiR = highPassFilters[1].process(lofiR)

                lofiL = bassShelfFilters[0].process(lofiL)
                lofiR = bassShelfFilters[1].process(lofiR)

                lofiL = lowPassStage1Filters[0].process(lofiL)
                lofiR = lowPassStage1Filters[1].process(lofiR)
                lofiL = lowPassStage2Filters[0].process(lofiL)
                lofiR = lowPassStage2Filters[1].process(lofiR)

                lofiL = highShelfFilters[0].process(lofiL)
                lofiR = highShelfFilters[1].process(lofiR)

                // STAGE 2: Wow & Flutter (Fractional Circular Delay Line Pitch Modulation)
                wowDelayLeft[delayWriteIndex] = lofiL
                wowDelayRight[delayWriteIndex] = lofiR
                if (wowDepth > 0.01f) {
                    wowPhase += wowPhaseInc
                    if (wowPhase > 2.0 * PI) wowPhase -= 2.0 * PI
                    flutterPhase += flutterPhaseInc
                    if (flutterPhase > 2.0 * PI) flutterPhase -= 2.0 * PI

                    val modLfo = (sin(wowPhase) * 0.78 + sin(flutterPhase) * 0.22).toFloat()
                    val centerDelaySamples = wowDepth * (sampleRate * 0.0055f) + 4f
                    val currentDelay = centerDelaySamples + (modLfo * wowDepth * (sampleRate * 0.0045f))

                    val readPos = delayWriteIndex - currentDelay
                    val readInt = readPos.toInt()
                    val frac = readPos - readInt
                    val idx0 = (readInt + delayBufferSize * 2) and delayBufferMask
                    val idx1 = (idx0 - 1 + delayBufferSize) and delayBufferMask

                    lofiL = wowDelayLeft[idx0] * (1f - frac) + wowDelayLeft[idx1] * frac
                    lofiR = wowDelayRight[idx0] * (1f - frac) + wowDelayRight[idx1] * frac
                }
                delayWriteIndex = (delayWriteIndex + 1) and delayBufferMask

                // STAGE 3: Low-Latency Bitcrusher (Sample-Rate Decimator + TPDF Dithered Bit-Depth Quantizer)
                if (srDivisor > 1.02f) {
                    sampleHoldCounter += 1.0f
                    if (sampleHoldCounter >= srDivisor) {
                        sampleHoldCounter -= srDivisor
                        heldSampleLeft = lofiL
                        heldSampleRight = lofiR
                    }
                    // Subtle vintage DAC smoothing (85% zero-order hold + 15% previous sample)
                    lofiL = heldSampleLeft * 0.85f + prevCrushedLeft * 0.15f
                    lofiR = heldSampleRight * 0.85f + prevCrushedRight * 0.15f
                }

                if (clampedBitDepth < 15.8f) {
                    // TPDF (Triangular Probability Density Function) micro-dither prevents harsh digital deadband
                    val dither = (fastRandomFloat() - fastRandomFloat()) * ditherScale
                    lofiL = (round((lofiL + dither) * halfScaleLevels) * invHalfScaleLevels).coerceIn(-1f, 1f)
                    lofiR = (round((lofiR - dither) * halfScaleLevels) * invHalfScaleLevels).coerceIn(-1f, 1f)
                }
                prevCrushedLeft = lofiL
                prevCrushedRight = lofiR

                // STAGE 4: Warm Analog Saturation & Soft Distortion
                if (satAmount > 0.01f || distAmount > 0.01f) {
                    val drive = 1.0f + satAmount * 2.2f + distAmount * 3.5f
                    val asym = distAmount * 0.08f
                    lofiL = softSaturate(lofiL * drive + asym) - asym * 0.5f
                    lofiR = softSaturate(lofiR * drive + asym) - asym * 0.5f
                    val gainComp = 1.0f / (1.0f + satAmount * 0.45f + distAmount * 0.75f)
                    lofiL *= gainComp
                    lofiR *= gainComp
                }

                // STAGE 5: Real-Time Stereo Reverb (Schroeder Parallel Comb + All-Pass Network)
                if (reverbWet > 0.005f) {
                    val revInputL = (lofiL + lofiR) * 0.25f
                    val revInputR = revInputL
                    var revOutL = 0f
                    var revOutR = 0f

                    for (c in 0 until 4) {
                        val bufL = combBuffersL[c]
                        val idxL = combIndicesL[c]
                        val delayedL = bufL[idxL]
                        combFilterStoreL[c] = (delayedL * (1f - reverbDamp)) + (combFilterStoreL[c] * reverbDamp)
                        bufL[idxL] = revInputL + combFilterStoreL[c] * reverbRoom
                        combIndicesL[c] = if (idxL + 1 >= bufL.size) 0 else idxL + 1
                        revOutL += delayedL

                        val bufR = combBuffersR[c]
                        val idxR = combIndicesR[c]
                        val delayedR = bufR[idxR]
                        combFilterStoreR[c] = (delayedR * (1f - reverbDamp)) + (combFilterStoreR[c] * reverbDamp)
                        bufR[idxR] = revInputR + combFilterStoreR[c] * reverbRoom
                        combIndicesR[c] = if (idxR + 1 >= bufR.size) 0 else idxR + 1
                        revOutR += delayedR
                    }

                    for (a in 0 until 2) {
                        val apBufL = allPassBuffersL[a]
                        val apIdxL = allPassIndicesL[a]
                        val bufOutL = apBufL[apIdxL]
                        val inL = revOutL
                        revOutL = -inL + bufOutL
                        apBufL[apIdxL] = inL + (bufOutL * 0.5f)
                        allPassIndicesL[a] = if (apIdxL + 1 >= apBufL.size) 0 else apIdxL + 1

                        val apBufR = allPassBuffersR[a]
                        val apIdxR = allPassIndicesR[a]
                        val bufOutR = apBufR[apIdxR]
                        val inR = revOutR
                        revOutR = -inR + bufOutR
                        apBufR[apIdxR] = inR + (bufOutR * 0.5f)
                        allPassIndicesR[a] = if (apIdxR + 1 >= apBufR.size) 0 else apIdxR + 1
                    }

                    lofiL = lofiL * (1f - reverbWet * 0.45f) + revOutL * reverbWet
                    lofiR = lofiR * (1f - reverbWet * 0.45f) + revOutR * reverbWet
                }

                // STAGE 6: Multi-Layer Stereo Vinyl Crackle, 33.3 RPM Vinyl Groove Noise & Tape Hiss
                if (crackleAmt > 0.005f || vinylNoiseAmt > 0.005f || tapeHissAmt > 0.005f) {
                    val whiteL = fastRandomFloat()
                    val whiteR = fastRandomFloat()

                    // A. Stereo Vinyl Dust Crackle Impulses shaped through Dual Biquad Bandpass Resonators
                    var popImpulseL = 0f
                    var popImpulseR = 0f
                    val popThreshold = 1.0f - (crackleAmt * 0.0052f)
                    if (abs(whiteL) > popThreshold) {
                        popImpulseL = whiteL * crackleAmt * 0.42f
                    }
                    if (abs(whiteR) > popThreshold) {
                        popImpulseR = whiteR * crackleAmt * 0.42f
                    }
                    // Cross-bleed 25% between L and R needles for realistic stereo phono cartridge pickup
                    val needleInL = popImpulseL + popImpulseR * 0.25f
                    val needleInR = popImpulseR + popImpulseL * 0.25f
                    val crackleOutL = crackleResonatorHighL.process(needleInL) + crackleResonatorBodyL.process(needleInL) * 0.45f
                    val crackleOutR = crackleResonatorHighR.process(needleInR) + crackleResonatorBodyR.process(needleInR) * 0.45f

                    // B. 33.3 RPM Platter-Modulated Stereo Pink Groove Noise + 48Hz Turntable Motor Rumble
                    pinkB0L = 0.99765f * pinkB0L + whiteL * 0.0990460f
                    pinkB1L = 0.96300f * pinkB1L + whiteL * 0.2965164f
                    pinkB2L = 0.57000f * pinkB2L + whiteL * 1.0526913f
                    val pinkL = (pinkB0L + pinkB1L + pinkB2L + whiteL * 0.1848f) * 0.042f

                    pinkB0R = 0.99765f * pinkB0R + whiteR * 0.0990460f
                    pinkB1R = 0.96300f * pinkB1R + whiteR * 0.2965164f
                    pinkB2R = 0.57000f * pinkB2R + whiteR * 1.0526913f
                    val pinkR = (pinkB0R + pinkB1R + pinkB2R + whiteR * 0.1848f) * 0.042f

                    vinylPlatterPhase += platterPhaseInc
                    if (vinylPlatterPhase > 2.0 * PI) vinylPlatterPhase -= 2.0 * PI
                    val platterSwish = (1.0f + 0.28f * sin(vinylPlatterPhase).toFloat())

                    rumblePhase += rumblePhaseInc
                    if (rumblePhase > 2.0 * PI) rumblePhase -= 2.0 * PI
                    val rumble = sin(rumblePhase).toFloat() * 0.0065f

                    val vinylFloorL = (pinkL * 0.48f * platterSwish + rumble) * vinylNoiseAmt * 0.15f
                    val vinylFloorR = (pinkR * 0.48f * platterSwish + rumble) * vinylNoiseAmt * 0.15f

                    // C. High-Frequency Magnetic Tape Hiss
                    val hissL = (whiteL - tapePrevNoiseL) * 0.5f * tapeHissAmt * 0.024f
                    val hissR = (whiteR - tapePrevNoiseR) * 0.5f * tapeHissAmt * 0.024f
                    tapePrevNoiseL = whiteL
                    tapePrevNoiseR = whiteR

                    lofiL += crackleOutL + vinylFloorL + hissL
                    lofiR += crackleOutR + vinylFloorR + hissR
                }

                // STAGE 7: Stereo Width (Mid/Side Matrix)
                if (abs(stereoWidth - 1.0f) > 0.02f) {
                    val mid = (lofiL + lofiR) * 0.5f
                    val side = (lofiL - lofiR) * 0.5f * stereoWidth
                    lofiL = mid + side
                    lofiR = mid - side
                }

                // STAGE 8: Soft Knee Dynamics Compressor
                if (compAmount > 0.01f) {
                    val absPeak = max(abs(lofiL), abs(lofiR))
                    val attackCoeff = 0.015f
                    val releaseCoeff = 0.0008f
                    compEnvelope += if (absPeak > compEnvelope) {
                        attackCoeff * (absPeak - compEnvelope)
                    } else {
                        releaseCoeff * (absPeak - compEnvelope)
                    }
                    val threshold = 0.35f
                    if (compEnvelope > threshold) {
                        val excess = compEnvelope - threshold
                        val compGain = threshold + excess / (1.0f + compAmount * 3.5f)
                        val gainFactor = (compGain / compEnvelope.coerceAtLeast(0.0001f)) * (1.0f + compAmount * 0.22f)
                        lofiL *= gainFactor
                        lofiR *= gainFactor
                        lastGainReductionDb = 20f * log10(gainFactor.coerceAtLeast(0.1f))
                    } else {
                        lastGainReductionDb *= 0.995f
                    }
                }

                // Blend dry Hi-Fi and wet Lo-Fi signal using currentLofiMixEnvelope
                outL = dryL * (1.0f - currentLofiMixEnvelope) + lofiL * currentLofiMixEnvelope
                outR = dryR * (1.0f - currentLofiMixEnvelope) + lofiR * currentLofiMixEnvelope
            }

            // STAGE 9: 10-Band Equalizer (active in both Hi-Fi and Lo-Fi modes if enabled)
            if (eq.isEnabled) {
                for (b in 0 until 10) {
                    outL = eqBandFilters[0][b].process(outL)
                    outR = eqBandFilters[1][b].process(outR)
                }
            }

            // STAGE 10: Master Gain, Stereo Balance, Fade-In & Soft Limiter (prevents digital clipping)
            val masterGainLin = if (abs(p.masterGainDb) > 0.05f) {
                10.0.pow((p.masterGainDb.coerceIn(-24f, 12f)) / 20.0).toFloat()
            } else {
                1.0f
            }
            outL *= masterGainLin * leftPanGain * fadeInEnvelope
            outR *= masterGainLin * rightPanGain * fadeInEnvelope

            if (eq.volumeNormalization) {
                outL = softLimiter(outL)
                outR = softLimiter(outR)
            } else {
                outL = outL.coerceIn(-1.0f, 1.0f)
                outR = outR.coerceIn(-1.0f, 1.0f)
            }

            // Write 16-bit PCM back to output buffer
            val finalShortL = (outL * 32767.0f).toInt().coerceIn(-32768, 32767).toShort()
            outputBuffer.putShort(finalShortL)
            if (channels == 2) {
                val finalShortR = (outR * 32767.0f).toInt().coerceIn(-32768, 32767).toShort()
                outputBuffer.putShort(finalShortR)
            }

            // STAGE 11: Accumulate live PCM samples for real-time Visualizer
            accumSumSqL += outL * outL
            accumSumSqR += outR * outR
            accumSampleCount++
            if ((accumSampleCount and 3) == 0) {
                visPcmRing[visRingWritePos] = (outL + outR) * 0.5f
                visRingWritePos = (visRingWritePos + 1) and (visWindowSize - 1)
            }
            visFrameCounter++
            if (visFrameCounter >= 1470) { // ~30 fps at 44.1kHz
                emitVisualizerSnapshot(sampleRate)
                visFrameCounter = 0
            }
        }

        outputBuffer.flip()
    }

    private fun emitVisualizerSnapshot(sampleRate: Float) {
        val count = accumSampleCount.coerceAtLeast(1)
        val rmsL = sqrt(accumSumSqL / count).coerceIn(0f, 1f)
        val rmsR = sqrt(accumSumSqR / count).coerceIn(0f, 1f)
        accumSumSqL = 0f
        accumSumSqR = 0f
        accumSampleCount = 0

        val wave = FloatArray(64)
        val pos = visRingWritePos
        for (i in 0 until 64) {
            val idx = (pos + i * 4) and (visWindowSize - 1)
            wave[i] = visPcmRing[idx].coerceIn(-1f, 1f)
        }

        // Compute 32-band real-time frequency spectrum using Goertzel / DFT bins on visPcmRing
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

        val avgRms = (rmsL + rmsR) * 0.5f
        val db = if (avgRms > 0.00001f) 20f * log10(avgRms) else -60f
        val p = lofiParams
        val effSr = if (p.isLiveLofiEnabled) {
            (sampleRate / p.sampleRateDivisor.coerceIn(1f, 10f)).toInt()
        } else {
            sampleRate.toInt()
        }

        _visualizerFlow.value = VisualizerSnapshot(
            waveform = wave,
            spectrum = spec,
            rmsLeft = rmsL,
            rmsRight = rmsR,
            peakDb = db.coerceIn(-60f, 0f),
            gainReductionDb = lastGainReductionDb,
            isLofiActive = currentLofiMixEnvelope > 0.05f,
            effectiveSampleRateHz = effSr,
            activeLowPassHz = smoothedLowPassHz.toInt(),
            activeBitDepth = if (p.isLiveLofiEnabled) p.bitDepth else 16.0f,
            bufferLatencyMs = lastBufferLatencyMs
        )
    }

    private fun softSaturate(x: Float): Float {
        val clamped = x.coerceIn(-3.0f, 3.0f)
        val x2 = clamped * clamped
        return clamped * (27.0f + x2) / (27.0f + 9.0f * x2)
    }

    private fun softLimiter(x: Float): Float {
        val absX = abs(x)
        if (absX <= 0.85f) return x
        val sign = if (x >= 0f) 1f else -1f
        val excess = absX - 0.85f
        return sign * (0.85f + 0.15f * (excess / (0.15f + excess)))
    }

    override fun onFlush() {
        fadeInEnvelope = 0.1f
        wowDelayLeft.fill(0f)
        wowDelayRight.fill(0f)
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
    }

    /**
     * Direct Form II Transposed Biquad Filter for zero-click audio filtering.
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
