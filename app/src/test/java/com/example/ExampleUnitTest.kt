package com.example

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import com.example.audio.AudioEngine
import com.example.audio.LofiDspAudioProcessor
import com.example.model.BuiltInLofiPresets
import com.example.model.EqualizerState
import com.example.model.LofiDspParameters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.sqrt

class ExampleUnitTest {

    @Test
    fun verifyAudioEngineProcessesRawPcmByteBuffersWithGainAndFilterStages() {
        val engine = AudioEngine(sampleRate = 44100, channelCount = 2)
        engine.setSoftClipEnabled(false)

        val frames = 1024
        val rawBytes = ByteArray(frames * 4)
        val bb = ByteBuffer.wrap(rawBytes).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until frames) {
            val sampleShort = 8000.toShort()
            bb.putShort(sampleShort) // Left
            bb.putShort(sampleShort) // Right
        }

        // 1. Test -6 dB gain attenuation (~0.5x amplitude) after slewing settles
        engine.setMasterGainLinear(0.5f)
        val outBytes = ByteArray(frames * 4)
        repeat(3) {
            engine.processPcmBuffer(rawBytes, 0, rawBytes.size, outBytes)
        }
        val outBuf = ByteBuffer.wrap(outBytes).order(ByteOrder.LITTLE_ENDIAN)
        val firstLeft = outBuf.short.toInt()
        val firstRight = outBuf.short.toInt()
        assertTrue("Expected ~4000 PCM value after 0.5x gain, got $firstLeft", firstLeft in 3950..4050)
        assertEquals(firstLeft, firstRight)
        assertTrue(engine.processedFramesCount >= frames * 3L)

        // 2. Test pluggable PcmFilterChainStage hook inside AudioEngine
        engine.setMasterGainLinear(1.0f)
        engine.addFilterStage { stereoFrame, _ ->
            // Invert phase and halve right channel in custom filter stage
            stereoFrame[0] = stereoFrame[0] * 0.25f
            stereoFrame[1] = stereoFrame[1] * 0.75f
        }
        repeat(3) {
            engine.processPcmBuffer(rawBytes, 0, rawBytes.size, outBytes)
        }
        val stageBuf = ByteBuffer.wrap(outBytes).order(ByteOrder.LITTLE_ENDIAN)
        val stageL = stageBuf.short.toInt()
        val stageR = stageBuf.short.toInt()
        assertTrue("Expected custom filter stage L ~2000, got $stageL", stageL in 1950..2050)
        assertTrue("Expected custom filter stage R ~6000, got $stageR", stageR in 5950..6050)
    }

    @Test
    fun verifyAllTwelveProfessionalPresetsExist() {
        assertEquals(12, BuiltInLofiPresets.ALL.size)
        val expectedNames = listOf(
            "Rainy Night",
            "Deep Chill",
            "Vintage Vinyl",
            "Warm Cassette",
            "Dreamy Lo-Fi",
            "Dark Academia",
            "Study Focus",
            "Sleep Relax",
            "Coffee Shop",
            "Anime Memories",
            "Midnight Drive",
            "Emotional Piano"
        )
        assertEquals(expectedNames, BuiltInLofiPresets.ALL.map { it.name })
    }

    @Test
    fun verifyLowPassFilterAttenuatesHighFrequencyPcm() {
        val processor = LofiDspAudioProcessor()
        processor.configure(AudioProcessor.AudioFormat(44100, 2, C.ENCODING_PCM_16BIT))
        processor.flush()
        processor.updateEqualizerState(
            EqualizerState(isEnabled = false, volumeNormalization = false, smoothTransitions = false, fadeInOutMs = 0)
        )

        processor.updateDspParameters(
            LofiDspParameters(
                isLiveLofiEnabled = true,
                masterIntensity = 1.0f,
                lowPassCutoffHz = 800f,
                highPassCutoffHz = 20f,
                bitDepth = 16f,
                sampleRateDivisor = 1.0f,
                analogSaturation = 0f,
                vinylCrackle = 0f,
                vinylNoise = 0f,
                tapeHiss = 0f,
                wowFlutterDepth = 0f,
                warmBassBoostDb = 0f,
                softDistortion = 0f,
                reverbMix = 0f,
                stereoWidth = 1.0f,
                softCompression = 0f,
                highFreqReductionDb = 0f,
                trebleGainDb = 0f
            )
        )

        val frames = 2048
        fun make8kHzBuffer(): ByteBuffer {
            val buf = ByteBuffer.allocateDirect(frames * 4).order(ByteOrder.LITTLE_ENDIAN)
            for (i in 0 until frames) {
                val t = i.toDouble() / 44100.0
                val s = (sin(2.0 * PI * 8000.0 * t) * 24000.0).toInt().toShort()
                buf.putShort(s)
                buf.putShort(s)
            }
            buf.flip()
            return buf
        }

        repeat(6) {
            processor.queueInput(make8kHzBuffer())
            processor.output
        }
        processor.queueInput(make8kHzBuffer())
        val out = processor.output.order(ByteOrder.LITTLE_ENDIAN)
        var sumSq = 0.0
        for (i in 0 until frames * 2) {
            val v = out.short.toDouble() / 32768.0
            sumSq += v * v
        }
        val rmsOut = sqrt(sumSq / (frames * 2))
        assertTrue("Expected 8kHz tone to be strongly attenuated by 800Hz Low-Pass Filter, got $rmsOut", rmsOut < 0.08)
    }

    @Test
    fun verifyBitcrusherAndVinylNoiseSynthesizeRealPcmEffects() {
        val processor = LofiDspAudioProcessor()
        processor.configure(AudioProcessor.AudioFormat(44100, 2, C.ENCODING_PCM_16BIT))
        processor.flush()
        processor.updateEqualizerState(
            EqualizerState(isEnabled = false, volumeNormalization = false, smoothTransitions = false, fadeInOutMs = 0)
        )

        processor.updateDspParameters(
            LofiDspParameters(
                isLiveLofiEnabled = true,
                masterIntensity = 1.0f,
                vinylCrackle = 0.85f,
                vinylNoise = 0.80f,
                tapeHiss = 0.40f
            )
        )
        val silentBuf = ByteBuffer.allocateDirect(2048 * 4).order(ByteOrder.LITTLE_ENDIAN)
        processor.queueInput(silentBuf)
        val vinylOut = processor.output.order(ByteOrder.LITTLE_ENDIAN)
        var nonZeroSamples = 0
        for (i in 0 until 2048 * 2) {
            if (abs(vinylOut.short.toInt()) > 0) nonZeroSamples++
        }
        assertTrue("Expected Vinyl Noise & Crackle engine to synthesize non-zero analog texture on silent input", nonZeroSamples > 2000)
    }

    @Test
    fun verifyRealTimeLofiDspModifiesPcmSamplesAndHifiModeBypassesLofi() {
        val processor = LofiDspAudioProcessor()
        val format = AudioProcessor.AudioFormat(44100, 2, C.ENCODING_PCM_16BIT)
        val outFormat = processor.configure(format)
        processor.flush()
        assertEquals(44100, outFormat.sampleRate)
        assertTrue(processor.isActive)

        processor.updateEqualizerState(
            EqualizerState(
                isEnabled = false,
                volumeNormalization = false,
                smoothTransitions = false,
                fadeInOutMs = 0
            )
        )

        val frames = 2048
        fun makeTestBuffer(): ByteBuffer {
            val buf = ByteBuffer.allocateDirect(frames * 4).order(ByteOrder.LITTLE_ENDIAN)
            for (i in 0 until frames) {
                val t = i.toDouble() / 44100.0
                val sample = (0.4 * sin(2.0 * PI * 440.0 * t) + 0.4 * sin(2.0 * PI * 5000.0 * t))
                val s = (sample * 32767.0).toInt().toShort()
                buf.putShort(s)
                buf.putShort(s)
            }
            buf.flip()
            return buf
        }

        val rainyPreset = BuiltInLofiPresets.ALL.first { it.id == "rainy_night" }.parameters
        processor.updateDspParameters(rainyPreset.copy(isLiveLofiEnabled = true))
        val lofiInput = makeTestBuffer()
        processor.queueInput(lofiInput)
        val lofiOut = processor.output.order(ByteOrder.LITTLE_ENDIAN)

        var lofiDiffSum = 0L
        val originalRef = makeTestBuffer()
        for (i in 0 until frames * 2) {
            val orig = originalRef.short.toInt()
            val proc = lofiOut.short.toInt()
            lofiDiffSum += abs(orig - proc)
        }
        assertTrue("Expected Live Lo-Fi DSP to transform PCM samples", lofiDiffSum > 1_000_000L)

        processor.updateDspParameters(LofiDspParameters(isLiveLofiEnabled = false, masterIntensity = 0f))
        repeat(3) {
            processor.queueInput(makeTestBuffer())
            processor.output
        }
        processor.queueInput(makeTestBuffer())
        val hifiOut = processor.output.order(ByteOrder.LITTLE_ENDIAN)
        val hifiRef = makeTestBuffer()
        var hifiDiffSum = 0L
        for (i in 0 until frames * 2) {
            val orig = hifiRef.short.toInt()
            val proc = hifiOut.short.toInt()
            hifiDiffSum += abs(orig - proc)
        }
        assertNotEquals(lofiDiffSum, hifiDiffSum)
        assertTrue("Expected Hi-Fi mode to bypass Lo-Fi DSP", hifiDiffSum < 5000L)
    }
}
