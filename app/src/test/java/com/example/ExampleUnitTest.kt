package com.example

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import com.example.audio.AudioEngine
import com.example.audio.LofiDspAudioProcessor
import com.example.audio.MusicConverterEngine
import com.example.audio.MusicMoodAnalyzer
import com.example.model.BuiltInConverterPresets
import com.example.model.BuiltInLofiPresets
import com.example.model.ConverterCategory
import com.example.model.DetectedMusicMood
import com.example.model.EqualizerState
import com.example.model.LiveSoundMode
import com.example.model.LofiDspParameters
import com.example.model.TrackItem
import com.example.ui.theme.AccentTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
    fun verifyTwelvePremiumThemesAndAllPresetsExist() {
        assertEquals("Expected 12 primary studio themes", 12, AccentTheme.PRIMARY_TWELVE.size)
        val themeLabels = AccentTheme.PRIMARY_TWELVE.map { it.label }
        val expectedThemes = listOf(
            "Anime Lo-Fi Night",
            "Midnight Purple",
            "Cyberpunk Neon",
            "AMOLED Black",
            "Ocean Blue",
            "Sakura Pink",
            "Sunset Orange",
            "Galaxy Dreams",
            "Rainy Window",
            "Retro Synthwave",
            "Forest Calm",
            "Minimal Premium"
        )
        for (expected in expectedThemes) {
            assertTrue("Expected theme '$expected' to exist", expected in themeLabels)
        }

        val presetIds = BuiltInLofiPresets.ALL.map { it.id }
        val requiredPresets = listOf(
            "original",
            "soft_lofi",
            "deep_lofi",
            "vinyl_classic",
            "rainy_night",
            "dreamy_lofi",
            "cassette_tape",
            "balanced_lofi",
            "soft_ambient",
            "warm_lofi"
        )
        for (reqId in requiredPresets) {
            assertTrue("Expected preset '$reqId' to exist", reqId in presetIds)
        }
        assertEquals(7, DetectedMusicMood.entries.size)
        for (mood in DetectedMusicMood.entries) {
            assertTrue(
                "Expected mood ${mood.displayName} primary preset '${mood.primaryPresetId}' to exist",
                mood.primaryPresetId in presetIds
            )
        }
    }

    @Test
    fun verifyAllTenLiveSoundModesAndBinauralHrtfAnd8DRotation() {
        assertEquals(10, LiveSoundMode.entries.size)

        val processor = LofiDspAudioProcessor()
        processor.configure(AudioProcessor.AudioFormat(44100, 2, C.ENCODING_PCM_16BIT))
        processor.flush()
        processor.updateEqualizerState(
            EqualizerState(isEnabled = false, volumeNormalization = false, smoothTransitions = false, fadeInOutMs = 0)
        )

        val frames = 2048
        fun makeStereoTestBuffer(): ByteBuffer {
            val buf = ByteBuffer.allocateDirect(frames * 4).order(ByteOrder.LITTLE_ENDIAN)
            for (i in 0 until frames) {
                val t = i.toDouble() / 44100.0
                val leftSig = 0.45 * sin(2.0 * PI * 180.0 * t) + 0.35 * sin(2.0 * PI * 2200.0 * t)
                val rightSig = 0.45 * sin(2.0 * PI * 180.0 * t) + 0.25 * sin(2.0 * PI * 3400.0 * t)
                buf.putShort((leftSig * 26000.0).toInt().toShort())
                buf.putShort((rightSig * 26000.0).toInt().toShort())
            }
            buf.flip()
            return buf
        }

        // Test all 10 LiveSoundModes for non-clipping valid output
        for (mode in LiveSoundMode.entries) {
            processor.flush()
            processor.updateDspParameters(
                LofiDspParameters(
                    isLiveLofiEnabled = mode == LiveSoundMode.LIVE_LOFI,
                    soundMode = mode,
                    masterIntensity = 0.78f,
                    spatialIntensity = 0.85f,
                    eightDRotationSpeedHz = 0.25f,
                    spatialDistance = 0.45f,
                    spatialAzimuthDeg = 45f,
                    spatialAutoOrbit = false
                )
            )
            processor.queueInput(makeStereoTestBuffer())
            val out = processor.output.order(ByteOrder.LITTLE_ENDIAN)
            assertEquals("Expected $frames stereo frames for mode $mode", frames * 4, out.remaining())
            var maxAbsSample = 0
            while (out.hasRemaining()) {
                val s = abs(out.short.toInt())
                if (s > maxAbsSample) maxAbsSample = s
            }
            assertTrue("Expected non-silent output for mode $mode", maxAbsSample > 1000)
            assertTrue("Expected zero hard digital clipping for mode $mode, got $maxAbsSample", maxAbsSample < 32767)
        }

        // Verify Binaural HRTF 3D Spatial Audio creates genuine interaural level/time difference for +60 deg azimuth
        processor.flush()
        processor.updateDspParameters(
            LofiDspParameters(
                isLiveLofiEnabled = true,
                soundMode = LiveSoundMode.THREE_D_SPATIAL,
                masterIntensity = 1.0f,
                spatialIntensity = 1.0f,
                spatialAzimuthDeg = 60f,
                spatialAutoOrbit = false,
                spatialDistance = 0.25f
            )
        )
        fun makeMono1500HzBuffer(): ByteBuffer {
            val monoIn = ByteBuffer.allocateDirect(frames * 4).order(ByteOrder.LITTLE_ENDIAN)
            for (i in 0 until frames) {
                val t = i.toDouble() / 44100.0
                val s = (sin(2.0 * PI * 1500.0 * t) * 20000.0).toInt().toShort()
                monoIn.putShort(s)
                monoIn.putShort(s)
            }
            monoIn.flip()
            return monoIn
        }
        // Warm up 25ms crossfade ramp so steady-state HRTF is measured
        processor.queueInput(makeMono1500HzBuffer())
        processor.output
        processor.queueInput(makeMono1500HzBuffer())
        val hrtfOut = processor.output.order(ByteOrder.LITTLE_ENDIAN)
        var energyL = 0.0
        var energyR = 0.0
        for (i in 0 until frames) {
            val l = hrtfOut.short.toDouble()
            val r = hrtfOut.short.toDouble()
            energyL += l * l
            energyR += r * r
        }
        assertTrue(
            "Binaural HRTF at +60 deg right azimuth must produce higher ipsilateral Right ear energy ($energyR) than shadowed Left ear ($energyL)",
            energyR > energyL * 1.15
        )
    }

    @Test
    fun verifyMusicConverterEngineAndAllStudioConversionPresets() {
        assertEquals("Expected 21 clean converter presets across 4 Studio categories", 21, BuiltInConverterPresets.ALL.size)
        assertFalse(
            "Unwanted Rainy Lo-Fi preset must be removed",
            BuiltInConverterPresets.ALL.any { it.id == "conv_rainy_lofi" }
        )
        for (cat in ConverterCategory.entries) {
            assertTrue(
                "Expected category ${cat.displayName} to have presets",
                BuiltInConverterPresets.byCategory(cat).isNotEmpty()
            )
        }

        val sampleRate = 44100
        val frames = 1024
        val rawPcm = ByteArray(frames * 4)
        val bb = ByteBuffer.wrap(rawPcm).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until frames) {
            val t = i.toDouble() / sampleRate
            val s = (sin(2.0 * PI * 220.0 * t) * 18000.0).toInt().toShort()
            bb.putShort(s)
            bb.putShort(s)
        }

        val slowedPreset = BuiltInConverterPresets.ALL.first { it.id == "conv_slowed_reverb" }
        val slowedOut = MusicConverterEngine.processPcmBufferWithPreset(
            inputPcm16Stereo = rawPcm,
            sampleRateHz = sampleRate,
            preset = slowedPreset,
            intensity = 0.85f
        )
        // Slowed + Reverb has playbackSpeed = 0.84f -> output buffer has more frames than input
        assertTrue(
            "Slowed + Reverb output (${slowedOut.size} bytes) should be longer than input (${rawPcm.size} bytes)",
            slowedOut.size > rawPcm.size
        )

        val nightcorePreset = BuiltInConverterPresets.ALL.first { it.id == "conv_nightcore" }
        val nightcoreOut = MusicConverterEngine.processPcmBufferWithPreset(
            inputPcm16Stereo = rawPcm,
            sampleRateHz = sampleRate,
            preset = nightcorePreset,
            intensity = 0.85f
        )
        // Nightcore has playbackSpeed = 1.22f -> output buffer is shorter/faster than input
        assertTrue(
            "Nightcore output (${nightcoreOut.size} bytes) should be shorter than input (${rawPcm.size} bytes)",
            nightcoreOut.size < rawPcm.size
        )
    }

    @Test
    fun verifyStudioCategoriesAndCleanAudioWithoutAmbientRainNoise() {
        val categories = ConverterCategory.entries.map { it.displayName }
        assertEquals(
            listOf("Lo-Fi", "Phonk", "Bass", "Other Audio Effects"),
            categories
        )
        val lofiNames = BuiltInConverterPresets.byCategory(ConverterCategory.LOFI).map { it.name }
        assertTrue("Classic Lo-Fi" in lofiNames)
        assertTrue("Soft Lo-Fi" in lofiNames)
        assertTrue("Dreamy Lo-Fi" in lofiNames)
        assertTrue("Vinyl-style Lo-Fi" in lofiNames)

        val phonkNames = BuiltInConverterPresets.byCategory(ConverterCategory.PHONK).map { it.name }
        assertTrue("Drift Phonk" in phonkNames)
        assertTrue("Dark Phonk" in phonkNames)
        assertTrue("Aggressive Phonk" in phonkNames)

        val bassNames = BuiltInConverterPresets.byCategory(ConverterCategory.BASS).map { it.name }
        assertTrue("Bass Boost" in bassNames)
        assertTrue("Deep Bass" in bassNames)
        assertTrue("Ultra Bass" in bassNames)
        assertTrue("Punchy Bass" in bassNames)

        val otherNames = BuiltInConverterPresets.byCategory(ConverterCategory.OTHER_STYLES).map { it.name }
        assertTrue("Slowed + Reverb" in otherNames)
        assertTrue("Nightcore" in otherNames)
        assertTrue("Echo" in otherNames)
        assertTrue("Retro/Vintage" in otherNames)
        assertTrue("Treble Boost" in otherNames)
        assertTrue("Vocal Focus" in otherNames)
        assertTrue("8D Audio" in otherNames)
        assertTrue("3D Spatial Audio" in otherNames)
    }

    @Test
    fun verifyMusicMoodAnalyzerDetectsDifferentSongTypesAndWithholdsConfidenceOnSilence() {
        val sampleRate = 44100
        val seconds = 4
        val totalFrames = sampleRate * seconds

        val testTrack = TrackItem(
            id = "test_song_1",
            title = "Test Acoustic Track",
            artist = "Test Artist",
            album = "Test Album",
            durationMs = 4000L,
            uriString = "file:///tmp/nonexistent.wav"
        )

        // 1. Silent PCM buffer -> must NOT invent confidence (confidencePercent == null, isSignalSufficient == false)
        val silentPcm = ByteArray(totalFrames * 4)
        val silentResult = MusicMoodAnalyzer.analyzePcmBuffer(
            track = testTrack,
            pcmBytes = silentPcm,
            sampleRateHz = sampleRate,
            channelCount = 2,
            liveSnapshot = null
        )
        assertFalse("Silent audio must not claim signal sufficiency", silentResult.isSignalSufficient)
        assertNull("Silent audio must withhold confidence percentage", silentResult.confidencePercent)

        // 2. High-energy rhythmic track with 128 BPM pulses & bright upper harmonics
        val energeticPcm = ByteArray(totalFrames * 4)
        val energeticBb = ByteBuffer.wrap(energeticPcm).order(ByteOrder.LITTLE_ENDIAN)
        val beatPeriodSec = 60.0 / 128.0
        for (i in 0 until totalFrames) {
            val t = i.toDouble() / sampleRate
            val phaseInBeat = (t % beatPeriodSec) / beatPeriodSec
            val env = if (phaseInBeat < 0.18) 0.95 else 0.12
            val sig = env * (
                0.45 * sin(2.0 * PI * 140.0 * t) +
                    0.35 * sin(2.0 * PI * 2600.0 * t) +
                    0.20 * sin(2.0 * PI * 4200.0 * t)
                )
            val pcmVal = (sig * 28000.0).toInt().coerceIn(-32767, 32767).toShort()
            energeticBb.putShort(pcmVal)
            energeticBb.putShort(pcmVal)
        }
        val energeticResult = MusicMoodAnalyzer.analyzePcmBuffer(
            track = testTrack,
            pcmBytes = energeticPcm,
            sampleRateHz = sampleRate,
            channelCount = 2,
            liveSnapshot = null
        )
        assertTrue(energeticResult.isSignalSufficient)
        assertNotNull(energeticResult.confidencePercent)
        assertTrue(
            "Expected Energetic or Happy mood for fast bright transient song, got ${energeticResult.detectedMood}",
            energeticResult.detectedMood == DetectedMusicMood.ENERGETIC ||
                energeticResult.detectedMood == DetectedMusicMood.HAPPY
        )
        assertTrue(
            energeticResult.suggestedPreset.id == "balanced_lofi" ||
                energeticResult.suggestedPreset.id == "warm_lofi"
        )

        // 3. Slow, dark, sub-bass heavy melancholic song (low brightness, slow ~68 BPM swell)
        val sadPcm = ByteArray(totalFrames * 4)
        val sadBb = ByteBuffer.wrap(sadPcm).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until totalFrames) {
            val t = i.toDouble() / sampleRate
            val slowEnv = 0.25 + 0.10 * sin(2.0 * PI * (68.0 / 60.0) * t)
            val sig = slowEnv * (0.75 * sin(2.0 * PI * 110.0 * t) + 0.25 * sin(2.0 * PI * 220.0 * t))
            val pcmVal = (sig * 12000.0).toInt().coerceIn(-32767, 32767).toShort()
            sadBb.putShort(pcmVal)
            sadBb.putShort(pcmVal)
        }
        val sadResult = MusicMoodAnalyzer.analyzePcmBuffer(
            track = testTrack,
            pcmBytes = sadPcm,
            sampleRateHz = sampleRate,
            channelCount = 2,
            liveSnapshot = null
        )
        assertTrue(sadResult.isSignalSufficient)
        assertNotNull(sadResult.confidencePercent)
        assertTrue(
            "Expected Sad, Relaxing, or Peaceful mood for slow dark low-energy song, got ${sadResult.detectedMood}",
            sadResult.detectedMood in setOf(
                DetectedMusicMood.SAD,
                DetectedMusicMood.RELAXING,
                DetectedMusicMood.PEACEFUL
            )
        )
        assertNotEquals(
            "Differing acoustic songs must produce distinct feature profiles",
            energeticResult.acousticFeatures.spectralBrightnessNormalized,
            sadResult.acousticFeatures.spectralBrightnessNormalized
        )
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

    @Test
    fun verifyPart14ProfessionalEqualizerBassEnhancementAndSimultaneousEffects() {
        // 1. Verify Rock, Pop, Classical, Jazz, and Vocal built-in EQ presets exist
        val eqPresets = EqualizerState.BUILT_IN_EQ_PRESETS
        for (required in listOf("Rock", "Pop", "Classical", "Jazz", "Vocal")) {
            val curve = eqPresets[required]
            assertNotNull("Expected built-in EQ preset '$required' to exist", curve)
            assertEquals(10, curve!!.size)
        }
        assertEquals(5, EqualizerState.FIVE_BAND_INDICES.size)

        // 2. Verify 32-bit Float PCM input decoding at 48kHz into clean 16-bit stereo output
        val processor = LofiDspAudioProcessor()
        val floatFormat = processor.configure(AudioProcessor.AudioFormat(48000, 2, C.ENCODING_PCM_FLOAT))
        assertEquals(48000, floatFormat.sampleRate)
        assertEquals(C.ENCODING_PCM_16BIT, floatFormat.encoding)
        processor.updatePlaybackQuality(com.example.model.AudioPlaybackQuality.MAXIMUM_AVAILABLE_QUALITY)

        // 3. Simultaneously enable Equalizer (Rock + Bass/Midrange/Treble controls) + Deep Bass + Live Lo-Fi + 8D Audio
        processor.updateEqualizerState(
            EqualizerState(
                isEnabled = true,
                isFiveBandMode = true,
                presetName = "Rock",
                bandGainsDb = eqPresets.getValue("Rock"),
                bassBoostDb = 4.5f,
                midrangeBoostDb = 2.0f,
                trebleBoostDb = 2.5f,
                volumeNormalization = true,
                smoothTransitions = false
            )
        )
        processor.updateDspParameters(
            LofiDspParameters(
                isLiveLofiEnabled = true,
                soundMode = LiveSoundMode.EIGHT_D_ULTRA,
                masterIntensity = 0.85f,
                lowPassCutoffHz = 5200f,
                warmBassBoostDb = 6.5f,
                midrangeGainDb = 1.5f,
                analogSaturation = 0.35f,
                vinylCrackle = 0.18f,
                spatialIntensity = 0.90f,
                eightDRotationSpeedHz = 0.22f
            )
        )
        processor.flush()

        val frames = 2048
        val floatInput = ByteBuffer.allocateDirect(frames * 2 * 4).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until frames) {
            val t = i.toDouble() / 48000.0
            val sample = (0.55 * sin(2.0 * PI * 65.0 * t) + 0.35 * sin(2.0 * PI * 1800.0 * t)).toFloat()
            floatInput.putFloat(sample)
            floatInput.putFloat(sample)
        }
        floatInput.flip()

        processor.queueInput(floatInput)
        val out = processor.output.order(ByteOrder.LITTLE_ENDIAN)
        assertEquals(frames * 4, out.remaining())

        var maxPeak = 0
        while (out.hasRemaining()) {
            val absVal = abs(out.short.toInt())
            if (absVal > maxPeak) maxPeak = absVal
        }
        assertTrue("Expected audible output when stacking EQ + Bass + Lo-Fi + 8D", maxPeak > 2000)
        assertTrue("Expected anti-clipping headroom to prevent hard clipping (< 32767), got $maxPeak", maxPeak < 32767)
    }

    @Test
    fun verifyPhase3GenuineBinaural8DEngineItdIldFrontRearAndCwCcwPauseControls() {
        val processor = LofiDspAudioProcessor()
        processor.configure(AudioProcessor.AudioFormat(48000, 2, C.ENCODING_PCM_16BIT))
        processor.flush()
        processor.updateEqualizerState(
            EqualizerState(isEnabled = false, volumeNormalization = false, smoothTransitions = false, fadeInOutMs = 0)
        )

        val frames = 2048
        fun makeDualBandBuffer(freqHz: Double): ByteBuffer {
            val buf = ByteBuffer.allocateDirect(frames * 4).order(ByteOrder.LITTLE_ENDIAN)
            for (i in 0 until frames) {
                val t = i.toDouble() / 48000.0
                val s = (sin(2.0 * PI * freqHz * t) * 16000.0).toInt().toShort()
                buf.putShort(s)
                buf.putShort(s)
            }
            buf.flip()
            return buf
        }

        // 1. Paused at +90 deg (Right ear ipsilateral, Left ear contralateral)
        processor.flush()
        processor.updateDspParameters(
            LofiDspParameters.forLiveSoundMode(LiveSoundMode.EIGHT_D_ULTRA).copy(
                eightDPauseMovement = true,
                spatialAzimuthDeg = 90f,
                spatialIntensity = 1.0f,
                eightDMovementDepth = 0.25f
            )
        )
        repeat(4) {
            processor.queueInput(makeDualBandBuffer(4200.0))
            processor.output
        }
        val rightReport = processor.getLatestQualityReport()
        assertTrue(
            "Expected +90 deg Right azimuth to yield higher Right ear RMS than Left ear RMS: L=${rightReport.rmsLeftLinear}, R=${rightReport.rmsRightLinear}",
            rightReport.rmsRightLinear > rightReport.rmsLeftLinear * 1.35f
        )

        // 2. Paused at -90 deg (Left ear ipsilateral, Right ear contralateral)
        processor.flush()
        processor.updateDspParameters(
            LofiDspParameters.forLiveSoundMode(LiveSoundMode.EIGHT_D_ULTRA).copy(
                eightDPauseMovement = true,
                spatialAzimuthDeg = -90f,
                spatialIntensity = 1.0f,
                eightDMovementDepth = 0.25f
            )
        )
        repeat(4) {
            processor.queueInput(makeDualBandBuffer(4200.0))
            processor.output
        }
        val leftReport = processor.getLatestQualityReport()
        assertTrue(
            "Expected -90 deg Left azimuth to yield higher Left ear RMS than Right ear RMS: L=${leftReport.rmsLeftLinear}, R=${leftReport.rmsRightLinear}",
            leftReport.rmsLeftLinear > leftReport.rmsRightLinear * 1.35f
        )

        // 3. Frequency-Dependent Head-Shadow Diffraction: Low frequencies (220 Hz) diffract around the head
        // with much less interaural attenuation than high frequencies (4200 Hz)
        processor.flush()
        processor.updateDspParameters(
            LofiDspParameters.forLiveSoundMode(LiveSoundMode.EIGHT_D_ULTRA).copy(
                eightDPauseMovement = true,
                spatialAzimuthDeg = 90f,
                spatialIntensity = 1.0f,
                eightDMovementDepth = 0.20f
            )
        )
        repeat(4) {
            processor.queueInput(makeDualBandBuffer(220.0))
            processor.output
        }
        val lowFreqReport = processor.getLatestQualityReport()
        val highFreqIldRatio = rightReport.rmsRightLinear / rightReport.rmsLeftLinear.coerceAtLeast(0.0001f)
        val lowFreqIldRatio = lowFreqReport.rmsRightLinear / lowFreqReport.rmsLeftLinear.coerceAtLeast(0.0001f)
        assertTrue(
            "Expected high-frequency head shadow ILD ($highFreqIldRatio) to exceed low-frequency diffracted ILD ($lowFreqIldRatio)",
            highFreqIldRatio > lowFreqIldRatio * 1.15f
        )

        // 4. Front (0 deg) vs Rear (180 deg) Pinna Spectral Cues at 3.5 kHz
        processor.flush()
        processor.updateDspParameters(
            LofiDspParameters.forLiveSoundMode(LiveSoundMode.EIGHT_D_ULTRA).copy(
                eightDPauseMovement = true,
                spatialAzimuthDeg = 0f,
                spatialIntensity = 1.0f
            )
        )
        repeat(4) {
            processor.queueInput(makeDualBandBuffer(3500.0))
            processor.output
        }
        val frontRms = processor.getLatestQualityReport().rmsLeftLinear

        processor.flush()
        processor.updateDspParameters(
            LofiDspParameters.forLiveSoundMode(LiveSoundMode.EIGHT_D_ULTRA).copy(
                eightDPauseMovement = true,
                spatialAzimuthDeg = 180f,
                spatialIntensity = 1.0f
            )
        )
        repeat(4) {
            processor.queueInput(makeDualBandBuffer(3500.0))
            processor.output
        }
        val rearRms = processor.getLatestQualityReport().rmsLeftLinear
        assertTrue(
            "Expected Front 0 deg concha presence ($frontRms) to exceed Rear 180 deg pinna shadow ($rearRms) at 3.5 kHz",
            frontRms > rearRms * 1.25f
        )

        // 5. Clockwise vs Anticlockwise rotation moves in opposite directions from 0 deg
        processor.flush()
        processor.updateDspParameters(
            LofiDspParameters.forLiveSoundMode(LiveSoundMode.EIGHT_D_ULTRA).copy(
                eightDClockwise = true,
                eightDPauseMovement = false,
                eightDRotationSpeedHz = 0.45f,
                spatialAzimuthDeg = 0f,
                spatialIntensity = 1.0f
            )
        )
        repeat(3) {
            processor.queueInput(makeDualBandBuffer(2500.0))
            processor.output
        }
        val cwBalance = processor.getLatestQualityReport().channelBalanceDb

        processor.flush()
        processor.updateDspParameters(
            LofiDspParameters.forLiveSoundMode(LiveSoundMode.EIGHT_D_ULTRA).copy(
                eightDClockwise = false,
                eightDPauseMovement = false,
                eightDRotationSpeedHz = 0.45f,
                spatialAzimuthDeg = 0f,
                spatialIntensity = 1.0f
            )
        )
        repeat(3) {
            processor.queueInput(makeDualBandBuffer(2500.0))
            processor.output
        }
        val ccwBalance = processor.getLatestQualityReport().channelBalanceDb
        assertTrue(
            "Expected Clockwise ($cwBalance dB) and Anticlockwise ($ccwBalance dB) to move toward opposite ears",
            cwBalance * ccwBalance < 0f
        )
    }

    @Test
    fun verifyPhase4Dedicated3DSurroundEngineDecorrelationWidthDepthAndCenterFocus() {
        val processor = LofiDspAudioProcessor()
        processor.configure(AudioProcessor.AudioFormat(48000, 2, C.ENCODING_PCM_16BIT))
        processor.flush()
        processor.updateEqualizerState(
            EqualizerState(isEnabled = false, volumeNormalization = false, smoothTransitions = false, fadeInOutMs = 0)
        )

        val surroundDefault = LofiDspParameters.forLiveSoundMode(LiveSoundMode.THREE_D_SPATIAL)
        assertEquals(false, surroundDefault.spatialAutoOrbit)

        val frames = 2048
        fun makeMonoVocalBuffer(): ByteBuffer {
            val buf = ByteBuffer.allocateDirect(frames * 4).order(ByteOrder.LITTLE_ENDIAN)
            for (i in 0 until frames) {
                val t = i.toDouble() / 48000.0
                val sample = (0.35 * sin(2.0 * PI * 440.0 * t) + 0.25 * sin(2.0 * PI * 1200.0 * t))
                val s = (sample * 32767.0).toInt().toShort()
                buf.putShort(s)
                buf.putShort(s)
            }
            buf.flip()
            return buf
        }

        // Feed mono input into 3D Surround: quadrature all-pass decorrelation + early reflections must create stereo side width
        processor.updateDspParameters(
            surroundDefault.copy(
                spatialIntensity = 0.90f,
                surroundWidth = 1.65f,
                surroundDepth = 0.70f,
                surroundAmbience = 0.45f,
                surroundCenterFocus = 0.80f
            )
        )
        repeat(3) {
            processor.queueInput(makeMonoVocalBuffer())
            processor.output
        }
        processor.queueInput(makeMonoVocalBuffer())
        val out = processor.output.order(ByteOrder.LITTLE_ENDIAN)

        var midEnergy = 0.0
        var sideEnergy = 0.0
        for (i in 0 until frames) {
            val l = out.short.toDouble() / 32768.0
            val r = out.short.toDouble() / 32768.0
            val m = (l + r) * 0.5
            val s = (l - r) * 0.5
            midEnergy += m * m
            sideEnergy += s * s
        }
        assertTrue("Expected 3D Surround quadrature decorrelation & early reflections to generate spatial side energy ($sideEnergy)", sideEnergy > 0.05)
        assertTrue("Expected 3D Surround Center Vocal Focus to preserve strong center Mid energy ($midEnergy)", midEnergy > sideEnergy)
        assertEquals(0, processor.getLatestQualityReport().clippingCount)
    }

    @Test
    fun verifyPhase5AllNineRequiredLofiPresetsProduceDistinctMultiStageDspSignatures() {
        val requiredNine = BuiltInLofiPresets.REQUIRED_NINE
        assertEquals(9, requiredNine.size)
        val expectedIds = listOf(
            "classic_lofi",
            "rainy_night",
            "vinyl_vintage",
            "deep_warm_bass",
            "dreamy_lofi",
            "chill_study",
            "dark_lofi",
            "soft_piano_lofi",
            "relaxing_lofi"
        )
        assertEquals(expectedIds, requiredNine.map { it.id })

        val processor = LofiDspAudioProcessor()
        processor.configure(AudioProcessor.AudioFormat(44100, 2, C.ENCODING_PCM_16BIT))
        processor.updateEqualizerState(
            EqualizerState(isEnabled = false, volumeNormalization = false, smoothTransitions = false, fadeInOutMs = 0)
        )

        val frames = 2048
        fun makeMusicalRefBuffer(): ByteBuffer {
            val buf = ByteBuffer.allocateDirect(frames * 4).order(ByteOrder.LITTLE_ENDIAN)
            for (i in 0 until frames) {
                val t = i.toDouble() / 44100.0
                val sample = 0.30 * sin(2.0 * PI * 85.0 * t) +
                    0.30 * sin(2.0 * PI * 660.0 * t) +
                    0.25 * sin(2.0 * PI * 4800.0 * t)
                val sL = (sample * 32767.0).toInt().toShort()
                val sR = ((sample * 0.92) * 32767.0).toInt().toShort()
                buf.putShort(sL)
                buf.putShort(sR)
            }
            buf.flip()
            return buf
        }

        val signatures = mutableMapOf<String, Long>()
        for (preset in requiredNine) {
            processor.flush()
            processor.updateDspParameters(preset.parameters.copy(isLiveLofiEnabled = true, soundMode = LiveSoundMode.LIVE_LOFI))
            repeat(2) {
                processor.queueInput(makeMusicalRefBuffer())
                processor.output
            }
            processor.queueInput(makeMusicalRefBuffer())
            val out = processor.output.order(ByteOrder.LITTLE_ENDIAN)
            var weightedHash = 0L
            for (idx in 0 until frames * 2) {
                val sampleVal = out.short.toLong()
                weightedHash += kotlin.math.abs(sampleVal) * ((idx % 17) + 1)
            }
            val report = processor.getLatestQualityReport()
            assertEquals("Expected zero clipping for preset ${preset.id}", 0, report.clippingCount)
            assertTrue("Expected audible output for preset ${preset.id}", report.rmsLeftLinear > 0.02f)
            signatures[preset.id] = weightedHash
        }

        assertEquals(
            "Expected all 9 required LoFi presets to produce distinct PCM signatures",
            9,
            signatures.values.toSet().size
        )
    }

    @Test
    fun verifyPhase9ChannelSeparationContinuitySmoothTransitionsAndFourPrimaryModes() {
        // Verify all 4 PrimaryAudioModes map cleanly and independently
        assertEquals(4, com.example.model.PrimaryAudioMode.entries.size)
        assertEquals(
            com.example.model.PrimaryAudioMode.NORMAL,
            com.example.model.PrimaryAudioMode.fromLiveSoundMode(LiveSoundMode.ORIGINAL_STEREO)
        )
        assertEquals(
            com.example.model.PrimaryAudioMode.EIGHT_D_AUDIO,
            com.example.model.PrimaryAudioMode.fromLiveSoundMode(LiveSoundMode.EIGHT_D_ULTRA)
        )
        assertEquals(
            com.example.model.PrimaryAudioMode.THREE_D_SURROUND,
            com.example.model.PrimaryAudioMode.fromLiveSoundMode(LiveSoundMode.THREE_D_SPATIAL)
        )
        assertEquals(
            com.example.model.PrimaryAudioMode.LOFI_AUDIO,
            com.example.model.PrimaryAudioMode.fromLiveSoundMode(LiveSoundMode.LIVE_LOFI)
        )

        // Verify strict Left/Right channel isolation in ORIGINAL_STEREO
        val processor = LofiDspAudioProcessor()
        processor.configure(AudioProcessor.AudioFormat(48000, 2, C.ENCODING_PCM_16BIT))
        processor.flush()
        processor.updateEqualizerState(
            EqualizerState(isEnabled = false, volumeNormalization = false, smoothTransitions = false, fadeInOutMs = 0)
        )
        processor.updateDspParameters(LofiDspParameters.forLiveSoundMode(LiveSoundMode.ORIGINAL_STEREO))

        val frames = 1024
        val leftOnlyBuf = ByteBuffer.allocateDirect(frames * 4).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until frames) {
            val s = (sin(2.0 * PI * 1000.0 * (i.toDouble() / 48000.0)) * 20000.0).toInt().toShort()
            leftOnlyBuf.putShort(s) // Left
            leftOnlyBuf.putShort(0) // Right silent
        }
        leftOnlyBuf.flip()

        processor.queueInput(leftOnlyBuf)
        val out = processor.output.order(ByteOrder.LITTLE_ENDIAN)
        var maxLeft = 0
        var maxRight = 0
        for (i in 0 until frames) {
            val l = abs(out.short.toInt())
            val r = abs(out.short.toInt())
            if (l > maxLeft) maxLeft = l
            if (r > maxRight) maxRight = r
        }
        assertTrue("Expected Left channel signal to pass cleanly ($maxLeft)", maxLeft > 19000)
        assertEquals("Expected zero Right channel bleed in NORMAL Original Stereo mode", 0, maxRight)
    }

    @Test
    fun verifyPhase7EqualizerPhase8BassAndPhase9AdvancedControlsAndLongDurationStability() {
        // 1. Verify all 10 required Phase 7 Equalizer Presets exist and have 10 bands
        val requiredEqNames = EqualizerState.REQUIRED_TEN_EQ_PRESET_NAMES
        assertEquals(10, requiredEqNames.size)
        for (name in requiredEqNames) {
            val bands = EqualizerState.BUILT_IN_EQ_PRESETS[name]
            assertNotNull("Expected Phase 7 EQ preset '$name' to exist", bands)
            assertEquals("Expected 10 frequency bands in '$name'", 10, bands!!.size)
        }

        val processor = LofiDspAudioProcessor()
        processor.configure(AudioProcessor.AudioFormat(48000, 2, C.ENCODING_PCM_16BIT))
        processor.flush()
        processor.updateDspParameters(LofiDspParameters.forLiveSoundMode(LiveSoundMode.ORIGINAL_STEREO))

        val frames = 2048
        fun makeSubAndVocalBuffer(): ByteBuffer {
            val buf = ByteBuffer.allocateDirect(frames * 4).order(ByteOrder.LITTLE_ENDIAN)
            for (i in 0 until frames) {
                val t = i.toDouble() / 48000.0
                val sigL = 0.25 * sin(2.0 * PI * 52.0 * t) + 0.22 * sin(2.0 * PI * 2400.0 * t)
                val sigR = 0.25 * sin(2.0 * PI * 52.0 * t) - 0.18 * sin(2.0 * PI * 2400.0 * t)
                buf.putShort((sigL * 32767.0).toInt().toShort())
                buf.putShort((sigR * 32767.0).toInt().toShort())
            }
            buf.flip()
            return buf
        }

        // 2. Measure Flat baseline RMS in Normal mode
        processor.updateEqualizerState(
            EqualizerState(isEnabled = false, smoothTransitions = false, fadeInOutMs = 0)
        )
        processor.queueInput(makeSubAndVocalBuffer())
        processor.output
        val flatRms = processor.getLatestQualityReport().rmsLeftLinear

        // 3. Verify Phase 8 Sub-Bass + Bass Harmonic Enhancement + Low-Frequency Compression in Normal mode
        processor.flush()
        processor.updateEqualizerState(
            EqualizerState(
                isEnabled = true,
                audioEnhancementEnabled = true,
                subBassBoostDb = 6.0f,
                bassBoostDb = 4.5f,
                bassHarmonicDrive = 0.55f,
                lowFreqCompression = 0.40f,
                smoothTransitions = false,
                fadeInOutMs = 0
            )
        )
        repeat(3) {
            processor.queueInput(makeSubAndVocalBuffer())
            processor.output
        }
        val bassReport = processor.getLatestQualityReport()
        assertTrue(
            "Expected Phase 8 Sub-Bass + Bass Boost to increase low-end energy (${bassReport.rmsLeftLinear} > $flatRms)",
            bassReport.rmsLeftLinear > flatRms * 1.12f
        )
        assertEquals("Expected zero clipping with Phase 8 Low-Frequency Compression & Limiter", 0, bassReport.clippingCount)

        // 4. Verify Phase 9 Preamp (+6 dB), Master Stereo Width (0.0 Mono collapse), and Dynamic Range Control
        processor.flush()
        processor.updateEqualizerState(
            EqualizerState(
                isEnabled = true,
                audioEnhancementEnabled = true,
                preampGainDb = 4.0f,
                masterStereoWidth = 0.0f, // Full mono collapse
                dynamicRangeControl = 0.65f,
                outputLimiterEnabled = true,
                smoothTransitions = false,
                fadeInOutMs = 0
            )
        )
        repeat(3) {
            processor.queueInput(makeSubAndVocalBuffer())
            processor.output
        }
        processor.queueInput(makeSubAndVocalBuffer())
        val monoOut = processor.output.order(ByteOrder.LITTLE_ENDIAN)
        var maxSideDiff = 0
        for (i in 0 until frames) {
            val l = monoOut.short.toInt()
            val r = monoOut.short.toInt()
            val diff = abs(l - r)
            if (diff > maxSideDiff) maxSideDiff = diff
        }
        assertTrue(
            "Expected Master Stereo Width = 0.0f to collapse stereo into identical L/R mono (maxDiff=$maxSideDiff)",
            maxSideDiff <= 1
        )

        // 5. Verify Phase 9 Audio Enhancement OFF bypasses enhancement stages cleanly
        processor.flush()
        processor.updateEqualizerState(
            EqualizerState(
                isEnabled = false,
                audioEnhancementEnabled = false,
                preampGainDb = 8.0f,
                masterStereoWidth = 0.0f,
                bassHarmonicDrive = 0.9f,
                dynamicRangeControl = 0.9f,
                smoothTransitions = false,
                fadeInOutMs = 0
            )
        )
        processor.queueInput(makeSubAndVocalBuffer())
        processor.output
        val bypassedRms = processor.getLatestQualityReport().rmsLeftLinear
        assertEquals("Audio Enhancement OFF must restore exact flat RMS", flatRms, bypassedRms, 0.002f)

        // 6. Phase 13 Long-Duration Stability & Multi-Sample-Rate (96 kHz) Verification
        processor.configure(AudioProcessor.AudioFormat(96000, 2, C.ENCODING_PCM_16BIT))
        processor.flush()
        processor.updateDspParameters(LofiDspParameters.forLiveSoundMode(LiveSoundMode.EIGHT_D_ULTRA))
        processor.updateEqualizerState(
            EqualizerState(
                isEnabled = true,
                presetName = "Acoustic",
                bandGainsDb = EqualizerState.BUILT_IN_EQ_PRESETS.getValue("Acoustic"),
                smoothTransitions = true,
                fadeInOutMs = 120
            )
        )
        repeat(25) {
            processor.queueInput(makeSubAndVocalBuffer())
            processor.output
        }
        val longRunReport = processor.getLatestQualityReport()
        assertEquals(0, longRunReport.clippingCount)
        assertTrue(longRunReport.peakLevelLinear <= 0.966f)
        processor.reset()
    }
}

