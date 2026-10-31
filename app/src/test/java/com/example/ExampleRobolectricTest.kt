package com.example

import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.audio.AudioEngine
import com.example.audio.LofiDspAudioProcessor
import com.example.audio.StudioWavGenerator
import com.example.data.ImportedTrackEntity
import com.example.data.LofiDatabase
import com.example.data.PlaylistEntity
import com.example.data.PlaylistTrackCrossRef
import com.example.data.UserPreferencesRepository
import com.example.model.BuiltInLofiPresets
import com.example.model.EqualizerState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.net.URI
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app name and verify studio wav generation`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Lofi Wave", appName)

        val tracks = StudioWavGenerator.ensureStudioTracksGenerated(context)
        assertTrue(tracks.size >= 4)
    }

    @Test
    fun `verify permanent playlist system drag reorder and app-only song removal`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, LofiDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val dao = db.lofiDao()

        val tempAudioFile = File(context.cacheDir, "unit_test_song.wav").apply {
            writeBytes(ByteArray(128) { 0x10 })
        }
        assertTrue(tempAudioFile.exists())

        val track1 = ImportedTrackEntity(
            id = "track_1",
            uriString = tempAudioFile.toURI().toString(),
            title = "Midnight Study",
            artist = "LoFi Dreamer",
            album = "Night Tapes",
            durationMs = 180_000L,
            formatBadge = "WAV",
            folderName = "Downloads",
            dateAddedMillis = 1000L,
            albumArtUri = null,
            lyricsOrNotes = ""
        )
        val track2 = ImportedTrackEntity(
            id = "track_2",
            uriString = "file:///storage/emulated/0/Music/rainy.wav",
            title = "Rainy Window",
            artist = "LoFi Dreamer",
            album = "Night Tapes",
            durationMs = 200_000L,
            formatBadge = "WAV",
            folderName = "Music",
            dateAddedMillis = 2000L,
            albumArtUri = null,
            lyricsOrNotes = ""
        )
        val track3 = ImportedTrackEntity(
            id = "track_3",
            uriString = "file:///storage/emulated/0/Music/coffee.wav",
            title = "Coffee Steam",
            artist = "Analog Soul",
            album = "Warm Cups",
            durationMs = 160_000L,
            formatBadge = "WAV",
            folderName = "Music",
            dateAddedMillis = 3000L,
            albumArtUri = null,
            lyricsOrNotes = ""
        )

        dao.insertImportedTracks(listOf(track1, track2, track3))
        assertEquals(3, dao.getAllImportedTracks().first().size)

        val pl1Id = dao.insertPlaylist(PlaylistEntity(name = "Late Night", description = "Chill"))
        val pl2Id = dao.insertPlaylist(PlaylistEntity(name = "Focus Coding", description = "Deep work"))

        dao.insertPlaylistTrackLinks(
            listOf(
                PlaylistTrackCrossRef(playlistId = pl1Id, trackId = "track_1", sortIndex = 0),
                PlaylistTrackCrossRef(playlistId = pl1Id, trackId = "track_2", sortIndex = 1),
                PlaylistTrackCrossRef(playlistId = pl1Id, trackId = "track_3", sortIndex = 2),
                PlaylistTrackCrossRef(playlistId = pl2Id, trackId = "track_1", sortIndex = 0)
            )
        )

        dao.updatePlaylistMetadata(pl1Id, "Renamed Late Night", "Updated description")
        val playlists = dao.getAllPlaylists().first()
        val renamed = playlists.first { it.playlistId == pl1Id }
        assertEquals("Renamed Late Night", renamed.name)
        assertEquals("Updated description", renamed.description)

        val reorderedIds = listOf("track_3", "track_1", "track_2")
        dao.insertPlaylistTrackLinks(
            reorderedIds.mapIndexed { idx, tId ->
                PlaylistTrackCrossRef(playlistId = pl1Id, trackId = tId, sortIndex = idx)
            }
        )
        val pl1Links = dao.getAllPlaylistTrackLinks().first().filter { it.playlistId == pl1Id }
        assertEquals(listOf("track_3", "track_1", "track_2"), pl1Links.map { it.trackId })

        dao.deleteImportedTrack("track_1")
        dao.removeTrackFromAllPlaylists("track_1")

        assertTrue("Original audio file on disk must NEVER be deleted", tempAudioFile.exists())
        assertEquals(2, dao.getAllImportedTracks().first().size)
        val remainingLinks: List<PlaylistTrackCrossRef> = dao.getAllPlaylistTrackLinks().first()
        assertFalse(remainingLinks.any { ref -> ref.trackId == "track_1" })

        tempAudioFile.delete()
        db.close()
    }

    @Test
    fun `phase 6 validation - original bypass bit transparency dc blocker limiter and preset telemetry`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = UserPreferencesRepository(context)
        prefs.saveLastPlayedTrackState("studio_lofi_rainy_night", 42_500L)
        val snapshot = prefs.preferencesFlow.first()
        assertEquals("studio_lofi_rainy_night", snapshot.lastPlayedTrackId)
        assertEquals(42_500L, snapshot.lastPlayedPositionMs)

        val sampleRate = 44100
        val channels = 2
        val numFrames = 8192
        val bufferBytes = numFrames * channels * 2

        // 1. Build a rich stereo multi-tone test PCM signal (80Hz bass + 1.2kHz vocal + 5kHz treble)
        val testSignalBytes = ByteArray(bufferBytes)
        val srcBuf = ByteBuffer.wrap(testSignalBytes).order(ByteOrder.LITTLE_ENDIAN)
        for (n in 0 until numFrames) {
            val t = n.toDouble() / sampleRate.toDouble()
            val bass = 0.25 * sin(2.0 * PI * 80.0 * t)
            val vocal = 0.35 * sin(2.0 * PI * 1200.0 * t)
            val air = 0.15 * sin(2.0 * PI * 5000.0 * t)
            val stereoDiff = 0.05 * sin(2.0 * PI * 440.0 * t)
            val sL = ((bass + vocal + air + stereoDiff) * 32767.0).roundToInt().coerceIn(-32768, 32767).toShort()
            val sR = ((bass + vocal + air - stereoDiff) * 32767.0).roundToInt().coerceIn(-32768, 32767).toShort()
            srcBuf.putShort(sL)
            srcBuf.putShort(sR)
        }

        val processor = LofiDspAudioProcessor()
        processor.configure(AudioProcessor.AudioFormat(sampleRate, channels, C.ENCODING_PCM_16BIT))
        processor.updateEqualizerState(EqualizerState(isEnabled = false, smoothTransitions = false))

        // 2. Verify ORIGINAL mode bypasses all DSP effects and preserves original PCM samples
        val originalPreset = BuiltInLofiPresets.ALL.first { it.id == "original" }
        processor.updateDspParameters(originalPreset.parameters)
        processor.flush()

        // Warm up crossfade to 0.0f
        val warmIn = ByteBuffer.wrap(testSignalBytes.copyOf()).order(ByteOrder.LITTLE_ENDIAN)
        val warmOut = ByteBuffer.allocate(bufferBytes).order(ByteOrder.LITTLE_ENDIAN)
        processor.processAndMeasureBuffer(warmIn, warmOut, sampleRate, channels)

        val origIn = ByteBuffer.wrap(testSignalBytes.copyOf()).order(ByteOrder.LITTLE_ENDIAN)
        val origOut = ByteBuffer.allocate(bufferBytes).order(ByteOrder.LITTLE_ENDIAN)
        val origReport = processor.processAndMeasureBuffer(origIn, origOut, sampleRate, channels)

        assertEquals("Original mode must have zero clipping", 0, origReport.clippingCount)
        assertTrue(
            "Original mode DC offset must be near zero (actual=${origReport.dcOffsetLeft})",
            abs(origReport.dcOffsetLeft) < 0.005f
        )

        // Compare input vs output samples in Original mode (must match within 1 LSB where below 0.90 linear ceiling)
        origOut.flip()
        val verifyIn = ByteBuffer.wrap(testSignalBytes).order(ByteOrder.LITTLE_ENDIAN)
        var maxSampleDiff = 0
        for (i in 0 until numFrames * channels) {
            val diff = abs(verifyIn.short.toInt() - origOut.short.toInt())
            if (diff > maxSampleDiff) maxSampleDiff = diff
        }
        assertTrue("Original mode must preserve PCM samples within 1 LSB (actual max diff=$maxSampleDiff)", maxSampleDiff <= 1)

        // 3. Verify DC Offset Blocker & True-Peak Limiter under extreme +25% DC bias and +6dB overdrive
        val dcBiasedBytes = ByteArray(bufferBytes)
        val dcBuf = ByteBuffer.wrap(dcBiasedBytes).order(ByteOrder.LITTLE_ENDIAN)
        for (n in 0 until numFrames) {
            val t = n.toDouble() / sampleRate.toDouble()
            // +0.25 DC offset + 0.72 sine wave
            val s = ((0.25 + 0.72 * sin(2.0 * PI * 440.0 * t)) * 32767.0).roundToInt().coerceIn(-32768, 32767).toShort()
            dcBuf.putShort(s)
            dcBuf.putShort(s)
        }
        val softLofi = BuiltInLofiPresets.ALL.first { it.id == "soft_lofi" }
        processor.updateDspParameters(softLofi.parameters.copy(masterIntensity = 1.0f, masterGainDb = 6.0f))
        // Settle DC blocker filter state on first buffer, measure on second buffer
        processor.processAndMeasureBuffer(
            ByteBuffer.wrap(dcBiasedBytes.copyOf()).order(ByteOrder.LITTLE_ENDIAN),
            ByteBuffer.allocate(bufferBytes),
            sampleRate,
            channels
        )
        val hotReport = processor.processAndMeasureBuffer(
            ByteBuffer.wrap(dcBiasedBytes.copyOf()).order(ByteOrder.LITTLE_ENDIAN),
            ByteBuffer.allocate(bufferBytes),
            sampleRate,
            channels
        )
        assertEquals("True-peak limiter must guarantee 0 clipped samples even at +6dB gain", 0, hotReport.clippingCount)
        assertTrue("Peak level must stay at or below 0.966 ceiling (actual=${hotReport.peakLevelLinear})", hotReport.peakLevelLinear <= 0.966f)
        assertTrue("DC blocker must remove +0.25 DC bias down to < 0.015 (actual=${hotReport.dcOffsetLeft})", abs(hotReport.dcOffsetLeft) < 0.015f)

        // 4. Validate all 7 Phase-4 Presets on real generated Studio WAV music frames
        val studioTracks = StudioWavGenerator.ensureStudioTracksGenerated(context)
        val wavFile = File(URI(studioTracks.first().uriString))
        val wavBytes = wavFile.readBytes()
        // Skip 44-byte WAV header and read 16384 stereo frames (65536 bytes) from active music section
        val musicSlice = wavBytes.copyOfRange(44 + 44100 * 4, 44 + 44100 * 4 + 65536)

        val corePresetIds = listOf(
            "original",
            "soft_lofi",
            "deep_lofi",
            "vinyl_classic",
            "rainy_night",
            "dreamy_lofi",
            "cassette_tape"
        )

        println("=== PHASE 6 DSP QUALITY VALIDATION REPORT ===")
        for (presetId in corePresetIds) {
            val preset = BuiltInLofiPresets.ALL.first { it.id == presetId }
            processor.updateDspParameters(preset.parameters)
            processor.flush()

            // Process warmup buffer + measurement buffer
            processor.processAndMeasureBuffer(
                ByteBuffer.wrap(musicSlice.copyOf()).order(ByteOrder.LITTLE_ENDIAN),
                ByteBuffer.allocate(musicSlice.size),
                sampleRate,
                channels
            )
            val report = processor.processAndMeasureBuffer(
                ByteBuffer.wrap(musicSlice.copyOf()).order(ByteOrder.LITTLE_ENDIAN),
                ByteBuffer.allocate(musicSlice.size),
                sampleRate,
                channels
            )

            println(
                String.format(
                    "Preset %-14s | Peak: %6.2f dB (%.3f) | RMS: %6.2f dB | Clips: %d | DC(L/R): %+.5f/%+.5f | Bal: %+.2f dB | Latency: %.2f ms",
                    preset.name,
                    report.peakLevelDb,
                    report.peakLevelLinear,
                    report.rmsCombinedDb,
                    report.clippingCount,
                    report.dcOffsetLeft,
                    report.dcOffsetRight,
                    report.channelBalanceDb,
                    report.processingLatencyMs
                )
            )

            assertEquals("Preset ${preset.name} must have 0 clipped samples", 0, report.clippingCount)
            assertTrue("Preset ${preset.name} peak must not exceed limiter ceiling", report.peakLevelLinear <= 0.966f)
            assertTrue("Preset ${preset.name} must maintain healthy RMS loudness", report.rmsCombinedDb in -26.0f..-4.0f)
            assertTrue("Preset ${preset.name} DC offset L must be minimal", abs(report.dcOffsetLeft) < 0.005f)
            assertTrue("Preset ${preset.name} DC offset R must be minimal", abs(report.dcOffsetRight) < 0.005f)
            assertTrue("Preset ${preset.name} stereo balance must be natural", abs(report.channelBalanceDb) < 2.5f)
            assertEquals("Preset ${preset.name} must have 0 underruns", 0, report.underrunCount)
            assertTrue("Preset ${preset.name} processing latency must be real-time safe", report.processingLatencyMs < report.bufferDurationMs * 0.5f)
        }

        // 5. Verify AudioEngine standalone PCM loop & telemetry
        val engine = AudioEngine(sampleRate = 44100, channelCount = 2)
        engine.setMasterGainDb(1.5f)
        engine.addFilterStage { frame, _ ->
            frame[0] *= 0.90f
            frame[1] *= 0.90f
        }
        val engineSlice = musicSlice.copyOf(8192)
        val processedBytes = engine.processPcmBuffer(engineSlice, 0, engineSlice.size, engineSlice)
        val engineReport = engine.getLatestQualityReport(processedBytes)
        assertEquals(8192, processedBytes)
        assertEquals(0, engineReport.clippingCount)
        assertTrue(abs(engineReport.dcOffsetLeft) < 0.005f)
        assertTrue(engine.processedFramesCount > 0L)
        engine.stopAndRelease()
    }
}
