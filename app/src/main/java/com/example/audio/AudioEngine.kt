package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
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
 * real-time frame-by-frame DSP processing, click-free slewed gain adjustments, and extensible
 * filter chain stages.
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

    private var currentSlewedGain: Float = 1.0f

    // Telemetry metrics updated by the real-time DSP loop
    @Volatile
    var lastRmsLeft: Float = 0f
        private set

    @Volatile
    var lastRmsRight: Float = 0f
        private set

    @Volatile
    var processedFramesCount: Long = 0L
        private set

    private val filterStages = CopyOnWriteArrayList<PcmFilterChainStage>()
    private val scratchStereoFrame = FloatArray(2)
    private var internalWorkBuffer = ByteArray(8192)

    /**
     * Initializes and starts the underlying low-latency [AudioTrack] in streaming PCM mode.
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
                .setBufferSizeInBytes(minBufSize)
                .setTransferMode(AudioTrack.MODE_STREAM)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                builder.setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            }

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
        targetMasterGain = 10.0.pow(clampedDb / 20.0).toFloat()
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
     * applies click-free slewed gain adjustments, executes registered [PcmFilterChainStage]s,
     * applies soft-clipping protection, and re-encodes back to 16-bit little-endian PCM bytes.
     *
     * @param inputPcm Raw 16-bit little-endian PCM byte array.
     * @param offset Starting byte offset in [inputPcm].
     * @param length Number of bytes to process (must be a multiple of frame size).
     * @param outputPcm Destination byte array (may be the same array as [inputPcm] for in-place processing).
     * @return Number of processed PCM bytes written to [outputPcm].
     */
    fun processPcmBuffer(
        inputPcm: ByteArray,
        offset: Int = 0,
        length: Int = inputPcm.size - offset,
        outputPcm: ByteArray = inputPcm
    ): Int {
        val frameSize = BYTES_PER_SAMPLE * channelCount
        val validByteCount = (length / frameSize) * frameSize
        if (validByteCount <= 0) return 0

        val frameCount = validByteCount / frameSize
        val effectiveTargetGain = if (isMuted) 0.0f else targetMasterGain
        val slewStep = 1.0f / (sampleRate * 0.015f) // 15ms zipper-free gain ramp
        val lGain = targetLeftGain
        val rGain = targetRightGain
        val stagesCount = filterStages.size

        var sumSqL = 0f
        var sumSqR = 0f
        var byteIdx = offset
        var outIdx = if (outputPcm === inputPcm) offset else 0

        for (frame in 0 until frameCount) {
            // Smoothly slew master gain per frame to prevent clicks/zipper noise
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

            // 1. Apply slewed master & per-channel gain adjustments
            sampleL *= currentSlewedGain * lGain
            sampleR *= currentSlewedGain * rGain

            // 2. Execute registered DSP filter chain stages (Low-Pass, Bitcrusher, Vinyl Noise, etc.)
            if (stagesCount > 0) {
                scratchStereoFrame[0] = sampleL
                scratchStereoFrame[1] = sampleR
                for (s in 0 until stagesCount) {
                    filterStages[s].processFrame(scratchStereoFrame, sampleRate)
                }
                sampleL = scratchStereoFrame[0]
                sampleR = scratchStereoFrame[1]
            }

            // 3. Apply soft-clipping headroom limiter or hard clamp
            if (softClipEnabled) {
                sampleL = clampWithSoftKnee(sampleL)
                sampleR = clampWithSoftKnee(sampleR)
            } else {
                sampleL = sampleL.coerceIn(-1.0f, 1.0f)
                sampleR = sampleR.coerceIn(-1.0f, 1.0f)
            }

            sumSqL += sampleL * sampleL
            sumSqR += sampleR * sampleR

            // Re-encode normalized float back to 16-bit little-endian PCM bytes
            val outShortL = (sampleL * 32767.0f).toInt().coerceIn(-32768, 32767).toShort().toInt()
            outputPcm[outIdx] = (outShortL and 0xFF).toByte()
            outputPcm[outIdx + 1] = ((outShortL shr 8) and 0xFF).toByte()

            if (channelCount >= 2) {
                val outShortR = (sampleR * 32767.0f).toInt().coerceIn(-32768, 32767).toShort().toInt()
                outputPcm[outIdx + 2] = (outShortR and 0xFF).toByte()
                outputPcm[outIdx + 3] = ((outShortR shr 8) and 0xFF).toByte()
            }

            byteIdx += frameSize
            outIdx += frameSize
        }

        lastRmsLeft = sqrt(sumSqL / frameCount).coerceIn(0f, 1f)
        lastRmsRight = sqrt(sumSqR / frameCount).coerceIn(0f, 1f)
        processedFramesCount += frameCount

        return validByteCount
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
        if (absX <= 0.85f) return x
        val sign = if (x >= 0f) 1f else -1f
        val excess = absX - 0.85f
        return sign * (0.85f + 0.15f * (excess / (0.15f + excess)))
    }

    @Synchronized
    fun pause() {
        runCatching {
            audioTrack?.pause()
        }
    }

    @Synchronized
    fun flush() {
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
