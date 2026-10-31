package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Modular DSP stage interface for plugging filter chains (Low-Pass, Bitcrusher, Vinyl Noise, EQ)
 * into [AudioEngine]'s real-time PCM frame processing loop.
 */
fun interface PcmFilterChainStage {
    /**
     * Processes a single normalized stereo frame in-place (`frame[0]` = Left, `frame[1]` = Right).
     */
    fun processFrame(frame: FloatArray, sampleRate: Int)
}

/**
 * Native Kotlin AudioEngine backed by [AudioTrack] that handles raw 16-bit PCM byte buffers,
 * DC offset removal, real-time frame-by-frame DSP processing, click-free slewed gain adjustments,
 * true-peak limiting, and underrun/quality telemetry.
 */
class AudioEngine(
    val sampleRate: Int = DEFAULT_SAMPLE_RATE,
    val channelCount: Int = DEFAULT_CHANNEL_COUNT
) {

    companion object {
        const val DEFAULT_SAMPLE_RATE = 44100
        const val DEFAULT_CHANNEL_COUNT = 2
        const val BYTES_PER_SAMPLE = 2 // 16-bit PCM
    }

    private var audioTrack: AudioTrack? = null
    private val isRunning = AtomicBoolean(false)

    // Target and slewed linear gain controls (thread-safe for real-time audio rendering)
    @Volatile
    private var targetMasterGain: Float = 1.0f

    @Volatile
    private var targetLeftGain: Float = 1.0f

    @Volatile
    private var targetRightGain: Float = 1.0f

    @Volatile
    private var isMuted: Boolean = false

    @Volatile
    private var softClipEnabled: Boolean = true

    @Volatile
    private var dcBlockerEnabled: Boolean = false

    private var currentSlewedGain: Float = 1.0f
    private var limiterEnvelope: Float = 1.0f

    // 1st-order DC offset blocker state per channel
    private val dcPrevIn = FloatArray(2)
    private val dcPrevOut = FloatArray(2)

    // Telemetry metrics updated by the real-time DSP loop
    @Volatile
    var lastRmsLeft: Float = 0f
        private set

    @Volatile
    var lastRmsRight: Float = 0f
        private set

    @Volatile
    var lastPeakLevel: Float = 0f
        private set

    @Volatile
    var lastDcOffsetLeft: Float = 0f
        private set

    @Volatile
    var lastDcOffsetRight: Float = 0f
        private set

    @Volatile
    var clippingCount: Int = 0
        private set

    @Volatile
    var lastProcessingLatencyMs: Float = 0f
        private set

    @Volatile
    var processedFramesCount: Long = 0L
        private set

    private val filterStages = CopyOnWriteArrayList<PcmFilterChainStage>()
    private val scratchStereoFrame = FloatArray(2)
    private var internalWorkBuffer = ByteArray(16384)

    /**
     * Initializes and starts the underlying [AudioTrack] in streaming PCM mode with a 4x safety
     * buffer to prevent audio dropouts and buffer underruns.
     */
    @Synchronized
    fun start(): Boolean {
        if (isRunning.get() && audioTrack != null) return true

        val channelMask = if (channelCount == 1) {
            AudioFormat.CHANNEL_OUT_MONO
        } else {
            AudioFormat.CHANNEL_OUT_STEREO
        }

        val minBufSize = AudioTrack.getMinBufferSize(
            sampleRate,
            channelMask,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(4096)

        // 4x minimum buffer size (at least 16 KB) prevents underruns while keeping latency low
        val targetBufferSizeBytes = (minBufSize * 4).coerceAtLeast(16384)

        return runCatching {
            val builder = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(channelMask)
                        .build()
                )
                .setBufferSizeInBytes(targetBufferSizeBytes)
                .setTransferMode(AudioTrack.MODE_STREAM)

            val track = builder.build()
            track.play()
            audioTrack = track
            isRunning.set(true)
            true
        }.getOrDefault(false)
    }

    /**
     * Sets the master output gain in decibels (-24.0 dB to +12.0 dB).
     */
    fun setMasterGainDb(gainDb: Float) {
        val clampedDb = gainDb.coerceIn(-24.0f, 12.0f)
        targetMasterGain = if (abs(clampedDb) < 0.01f) {
            1.0f
        } else {
            10.0.pow(clampedDb / 20.0).toFloat()
        }
    }

    /**
     * Sets the master output gain as a linear multiplier (0.0 to 4.0).
     */
    fun setMasterGainLinear(gainLinear: Float) {
        targetMasterGain = gainLinear.coerceIn(0.0f, 4.0f)
    }

    fun getMasterGainLinear(): Float = targetMasterGain

    /**
     * Sets per-channel stereo trim multipliers (0.0 to 2.0).
     */
    fun setChannelGains(leftGain: Float, rightGain: Float) {
        targetLeftGain = leftGain.coerceIn(0.0f, 2.0f)
        targetRightGain = rightGain.coerceIn(0.0f, 2.0f)
    }

    fun setMuted(muted: Boolean) {
        isMuted = muted
    }

    fun isMuted(): Boolean = isMuted

    fun setSoftClipEnabled(enabled: Boolean) {
        softClipEnabled = enabled
    }

    fun setDcBlockerEnabled(enabled: Boolean) {
        dcBlockerEnabled = enabled
    }

    fun getUnderrunCount(): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return runCatching { audioTrack?.underrunCount ?: 0 }.getOrDefault(0)
        }
        return 0
    }

    /**
     * Appends a modular [PcmFilterChainStage] to the real-time DSP processing loop.
     */
    fun addFilterStage(stage: PcmFilterChainStage) {
        filterStages.add(stage)
    }

    fun clearFilterStages() {
        filterStages.clear()
    }

    /**
     * Core Real-Time DSP Processing Loop for raw 16-bit little-endian PCM byte buffers.
     *
     * Decodes 16-bit PCM frames into normalized [-1.0, 1.0] floating-point samples,
     * removes DC offset if present, executes registered [PcmFilterChainStage]s,
     * applies click-free slewed gain adjustments, applies true-peak limiting,
     * and re-encodes back to rounded 16-bit little-endian PCM bytes.
     */
    fun processPcmBuffer(
        inputPcm: ByteArray,
        offset: Int = 0,
        length: Int = inputPcm.size - offset,
        outputPcm: ByteArray = inputPcm
    ): Int {
        val startNs = System.nanoTime()
        val frameSize = BYTES_PER_SAMPLE * channelCount
        val validByteCount = (length / frameSize) * frameSize
        if (validByteCount <= 0) return 0

        val frameCount = validByteCount / frameSize
        val effectiveTargetGain = if (isMuted) 0.0f else targetMasterGain
        val slewStep = 1.0f / (sampleRate * 0.020f) // 20ms zipper-free gain ramp
        val dcPoleR = (1.0f - (2.0 * PI * 18.0 / sampleRate.coerceAtLeast(8000))).toFloat().coerceIn(0.990f, 0.9995f)
        val lGain = targetLeftGain
        val rGain = targetRightGain
        val stagesCount = filterStages.size
        val bypassDsp = stagesCount == 0 &&
            !isMuted &&
            abs(currentSlewedGain - 1.0f) < 0.0001f &&
            abs(effectiveTargetGain - 1.0f) < 0.0001f &&
            abs(lGain - 1.0f) < 0.0001f &&
            abs(rGain - 1.0f) < 0.0001f

        var sumSqL = 0f
        var sumSqR = 0f
        var sumL = 0f
        var sumR = 0f
        var maxPeak = 0f
        var byteIdx = offset
        var outIdx = if (outputPcm === inputPcm) offset else 0

        for (frame in 0 until frameCount) {
            if (currentSlewedGain < effectiveTargetGain) {
                currentSlewedGain = min(effectiveTargetGain, currentSlewedGain + slewStep)
            } else if (currentSlewedGain > effectiveTargetGain) {
                currentSlewedGain = max(effectiveTargetGain, currentSlewedGain - slewStep)
            }

            // Decode 16-bit little-endian signed PCM samples to [-1.0, 1.0]
            val loL = inputPcm[byteIdx].toInt() and 0xFF
            val hiL = inputPcm[byteIdx + 1].toInt()
            val shortL = ((hiL shl 8) or loL).toShort()
            var sampleL = shortL / 32768.0f

            var sampleR = if (channelCount >= 2) {
                val loR = inputPcm[byteIdx + 2].toInt() and 0xFF
                val hiR = inputPcm[byteIdx + 3].toInt()
                val shortR = ((hiR shl 8) or loR).toShort()
                shortR / 32768.0f
            } else {
                sampleL
            }

            if (!bypassDsp) {
                // 1. DC Offset Removal (when DSP or gain stages are active)
                if (dcBlockerEnabled && stagesCount > 0) {
                    val cleanL = sampleL - dcPrevIn[0] + dcPoleR * dcPrevOut[0]
                    dcPrevIn[0] = sampleL
                    dcPrevOut[0] = cleanL
                    sampleL = cleanL

                    val cleanR = sampleR - dcPrevIn[1] + dcPoleR * dcPrevOut[1]
                    dcPrevIn[1] = sampleR
                    dcPrevOut[1] = cleanR
                    sampleR = cleanR
                }

                // 2. Execute registered DSP filter chain stages
                if (stagesCount > 0) {
                    scratchStereoFrame[0] = sampleL
                    scratchStereoFrame[1] = sampleR
                    for (s in 0 until stagesCount) {
                        filterStages[s].processFrame(scratchStereoFrame, sampleRate)
                    }
                    sampleL = scratchStereoFrame[0]
                    sampleR = scratchStereoFrame[1]
                }

                // 3. Apply slewed master & per-channel gain adjustments
                sampleL *= currentSlewedGain * lGain
                sampleR *= currentSlewedGain * rGain

                // 4. Envelope-tracked true-peak limiter + soft-knee ceiling
                if (softClipEnabled) {
                    val peak = max(abs(sampleL), abs(sampleR))
                    val ceiling = 0.965f
                    val targetLim = if (peak > ceiling) ceiling / peak else 1.0f
                    if (targetLim < limiterEnvelope) {
                        limiterEnvelope = targetLim
                    } else {
                        limiterEnvelope += 0.001f * (1.0f - limiterEnvelope)
                    }
                    sampleL = clampWithSoftKnee(sampleL * limiterEnvelope)
                    sampleR = clampWithSoftKnee(sampleR * limiterEnvelope)
                } else {
                    sampleL = sampleL.coerceIn(-1.0f, 1.0f)
                    sampleR = sampleR.coerceIn(-1.0f, 1.0f)
                }
            }

            val framePeak = max(abs(sampleL), abs(sampleR))
            if (framePeak > maxPeak) maxPeak = framePeak
            if (framePeak >= 0.999f) clippingCount++

            sumSqL += sampleL * sampleL
            sumSqR += sampleR * sampleR
            sumL += sampleL
            sumR += sampleR

            // Re-encode normalized float back to rounded 16-bit little-endian PCM bytes
            val outShortL = (sampleL * 32767.0f).roundToInt().coerceIn(-32768, 32767).toShort().toInt()
            outputPcm[outIdx] = (outShortL and 0xFF).toByte()
            outputPcm[outIdx + 1] = ((outShortL shr 8) and 0xFF).toByte()

            if (channelCount >= 2) {
                val outShortR = (sampleR * 32767.0f).roundToInt().coerceIn(-32768, 32767).toShort().toInt()
                outputPcm[outIdx + 2] = (outShortR and 0xFF).toByte()
                outputPcm[outIdx + 3] = ((outShortR shr 8) and 0xFF).toByte()
            }

            byteIdx += frameSize
            outIdx += frameSize
        }

        lastRmsLeft = sqrt(sumSqL / frameCount).coerceIn(0f, 1f)
        lastRmsRight = sqrt(sumSqR / frameCount).coerceIn(0f, 1f)
        lastPeakLevel = maxPeak.coerceIn(0f, 1f)
        lastDcOffsetLeft = sumL / frameCount
        lastDcOffsetRight = sumR / frameCount
        lastProcessingLatencyMs = (System.nanoTime() - startNs) / 1_000_000f
        processedFramesCount += frameCount

        return validByteCount
    }

    /**
     * Returns a complete [AudioQualityReport] from the most recently processed buffer.
     */
    fun getLatestQualityReport(lastBufferBytes: Int = 4096): AudioQualityReport {
        val frames = (lastBufferBytes / (BYTES_PER_SAMPLE * channelCount)).coerceAtLeast(1)
        val bufferDurationMs = (frames.toFloat() / sampleRate.coerceAtLeast(8000)) * 1000f
        val rmsComb = sqrt((lastRmsLeft * lastRmsLeft + lastRmsRight * lastRmsRight) * 0.5f)
        val rmsDb = if (rmsComb > 0.00001f) 20f * log10(rmsComb) else -96f
        val peakDb = if (lastPeakLevel > 0.00001f) 20f * log10(lastPeakLevel) else -96f
        val balDb = if (lastRmsLeft > 0.0001f && lastRmsRight > 0.0001f) {
            20f * log10(lastRmsLeft / lastRmsRight)
        } else {
            0f
        }
        return AudioQualityReport(
            peakLevelLinear = lastPeakLevel,
            peakLevelDb = peakDb,
            rmsLeftLinear = lastRmsLeft,
            rmsRightLinear = lastRmsRight,
            rmsCombinedDb = rmsDb,
            clippingCount = clippingCount,
            dcOffsetLeft = lastDcOffsetLeft,
            dcOffsetRight = lastDcOffsetRight,
            channelBalanceDb = balDb,
            underrunCount = getUnderrunCount(),
            processingLatencyMs = lastProcessingLatencyMs,
            bufferDurationMs = bufferDurationMs
        )
    }

    /**
     * Processes a direct or heap [ByteBuffer] of 16-bit PCM frames and writes the result to [outputBuffer].
     */
    fun processPcmByteBuffer(inputBuffer: ByteBuffer, outputBuffer: ByteBuffer): Int {
        val remaining = inputBuffer.remaining()
        if (remaining <= 0) return 0
        if (internalWorkBuffer.size < remaining) {
            internalWorkBuffer = ByteArray(remaining)
        }
        inputBuffer.get(internalWorkBuffer, 0, remaining)
        val processedBytes = processPcmBuffer(internalWorkBuffer, 0, remaining, internalWorkBuffer)
        outputBuffer.order(ByteOrder.LITTLE_ENDIAN)
        outputBuffer.put(internalWorkBuffer, 0, processedBytes)
        return processedBytes
    }

    /**
     * Runs the real-time DSP processing loop on [pcmBytes] and writes the processed frames
     * directly to the active [AudioTrack] hardware sink.
     */
    fun writePcmFrames(pcmBytes: ByteArray, offset: Int = 0, length: Int = pcmBytes.size - offset): Int {
        if (internalWorkBuffer.size < length) {
            internalWorkBuffer = ByteArray(length)
        }
        val processedBytes = processPcmBuffer(pcmBytes, offset, length, internalWorkBuffer)
        val track = audioTrack
        if (track != null && isRunning.get() && processedBytes > 0) {
            return track.write(internalWorkBuffer, 0, processedBytes, AudioTrack.WRITE_BLOCKING)
        }
        return processedBytes
    }

    private fun clampWithSoftKnee(x: Float): Float {
        val absX = abs(x)
        if (absX <= 0.90f) return x
        val sign = if (x >= 0f) 1f else -1f
        val excess = absX - 0.90f
        return sign * (0.90f + 0.065f * (excess / (0.065f + excess)))
    }

    @Synchronized
    fun pause() {
        runCatching {
            audioTrack?.pause()
        }
    }

    @Synchronized
    fun flush() {
        dcPrevIn.fill(0f)
        dcPrevOut.fill(0f)
        limiterEnvelope = 1.0f
        runCatching {
            audioTrack?.flush()
        }
    }

    @Synchronized
    fun stopAndRelease() {
        isRunning.set(false)
        runCatching {
            audioTrack?.stop()
            audioTrack?.release()
        }
        audioTrack = null
    }
}
