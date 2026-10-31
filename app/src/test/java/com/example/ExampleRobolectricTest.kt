package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.audio.AudioEngine
import com.example.audio.StudioWavGenerator
import com.example.data.ImportedTrackEntity
import com.example.data.LofiDatabase
import com.example.data.PlaylistEntity
import com.example.data.PlaylistTrackCrossRef
import com.example.data.UserPreferencesRepository
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

        // Create temporary physical test file to verify removing from app never deletes disk file
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

        // Create two custom playlists and add track_1 to both (multi-playlist feature)
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

        // Verify rename playlist
        dao.updatePlaylistMetadata(pl1Id, "Renamed Late Night", "Updated description")
        val playlists = dao.getAllPlaylists().first()
        val renamed = playlists.first { it.playlistId == pl1Id }
        assertEquals("Renamed Late Night", renamed.name)
        assertEquals("Updated description", renamed.description)

        // Verify drag-and-drop reordering (move track_3 to index 0, track_1 to 1, track_2 to 2)
        val reorderedIds = listOf("track_3", "track_1", "track_2")
        dao.insertPlaylistTrackLinks(
            reorderedIds.mapIndexed { idx, tId ->
                PlaylistTrackCrossRef(playlistId = pl1Id, trackId = tId, sortIndex = idx)
            }
        )
        val pl1Links = dao.getAllPlaylistTrackLinks().first().filter { it.playlistId == pl1Id }
        assertEquals(listOf("track_3", "track_1", "track_2"), pl1Links.map { it.trackId })

        // Verify removing track_1 from app library removes DB reference + playlist cross-refs
        // WITHOUT deleting the original file on disk
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
    fun `verify audio engine pcm dsp processing and last played persistence`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = UserPreferencesRepository(context)
        prefs.saveLastPlayedTrackState("studio_lofi_rainy_night", 42_500L)

        val snapshot = prefs.preferencesFlow.first()
        assertEquals("studio_lofi_rainy_night", snapshot.lastPlayedTrackId)
        assertEquals(42_500L, snapshot.lastPlayedPositionMs)

        // Verify AudioEngine processes raw 16-bit PCM frames with gain and custom filter stage
        val engine = AudioEngine(sampleRate = 44100, channelCount = 2)
        engine.setMasterGainDb(1.5f)
        engine.addFilterStage { frame, _ ->
            frame[0] *= 0.85f
            frame[1] *= 0.85f
        }
        val pcmBuffer = ByteArray(4096) { idx -> (idx % 120).toByte() }
        val processedBytes = engine.processPcmBuffer(
            inputPcm = pcmBuffer,
            offset = 0,
            length = pcmBuffer.size,
            outputPcm = pcmBuffer
        )
        assertEquals(4096, processedBytes)
        assertTrue(engine.processedFramesCount > 0L)
        engine.stopAndRelease()
    }
}
