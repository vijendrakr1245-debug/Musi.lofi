package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.model.LofiDspParameters
import com.example.model.LofiPreset
import com.example.model.TrackItem
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "imported_tracks")
data class ImportedTrackEntity(
    @PrimaryKey val id: String,
    val uriString: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val formatBadge: String,
    val folderName: String,
    val dateAddedMillis: Long,
    val albumArtUri: String?,
    val lyricsOrNotes: String,
    val genre: String = "Lo-Fi / Instrumental",
    val sampleRateHz: Int = 44100,
    val bitrateKbps: Int = 1411,
    val trackNumber: Int? = null,
    val channelCount: Int = 2,
    val bitsPerSample: Int = 16
) {
    fun toTrackItem(): TrackItem = TrackItem(
        id = id,
        uriString = uriString,
        title = title,
        artist = artist,
        album = album,
        durationMs = durationMs,
        formatBadge = formatBadge,
        folderName = folderName,
        dateAddedMillis = dateAddedMillis,
        albumArtUri = albumArtUri,
        lyricsOrNotes = lyricsOrNotes,
        isBuiltInSample = false,
        genre = genre,
        sampleRateHz = sampleRateHz,
        bitrateKbps = bitrateKbps,
        trackNumber = trackNumber,
        channelCount = channelCount,
        bitsPerSample = bitsPerSample
    )
}

@Entity(tableName = "favorite_tracks")
data class FavoriteTrackEntity(
    @PrimaryKey val trackId: String,
    val addedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val playlistId: Long = 0,
    val name: String,
    val description: String,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "playlist_tracks", primaryKeys = ["playlistId", "trackId"])
data class PlaylistTrackCrossRef(
    val playlistId: Long,
    val trackId: String,
    val sortIndex: Int = 0,
    val addedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "playback_history")
data class PlaybackHistoryEntity(
    @PrimaryKey val trackId: String,
    val title: String,
    val artist: String,
    val album: String,
    val uriString: String,
    val formatBadge: String,
    val durationMs: Long,
    val lastPlayedAtMillis: Long = System.currentTimeMillis(),
    val playCount: Int = 1
)

@Entity(tableName = "custom_lofi_presets")
data class CustomLofiPresetEntity(
    @PrimaryKey val id: String,
    val name: String,
    val subtitle: String,
    val moodCategory: String,
    val masterIntensity: Float,
    val lowPassCutoffHz: Float,
    val highPassCutoffHz: Float,
    val bitDepth: Float,
    val sampleRateDivisor: Float,
    val analogSaturation: Float,
    val vinylCrackle: Float,
    val vinylNoise: Float,
    val tapeHiss: Float,
    val wowFlutterDepth: Float,
    val wowFlutterRateHz: Float,
    val warmBassBoostDb: Float,
    val softDistortion: Float,
    val reverbMix: Float,
    val reverbRoomSize: Float,
    val stereoWidth: Float,
    val softCompression: Float,
    val highFreqReductionDb: Float,
    val trebleGainDb: Float,
    val playbackSpeed: Float,
    val createdAtMillis: Long = System.currentTimeMillis()
) {
    fun toLofiPreset(): LofiPreset = LofiPreset(
        id = id,
        name = name,
        subtitle = subtitle,
        moodCategory = moodCategory,
        isCustom = true,
        parameters = LofiDspParameters(
            isLiveLofiEnabled = true,
            presetId = id,
            presetName = name,
            masterIntensity = masterIntensity,
            lowPassCutoffHz = lowPassCutoffHz,
            highPassCutoffHz = highPassCutoffHz,
            bitDepth = bitDepth,
            sampleRateDivisor = sampleRateDivisor,
            analogSaturation = analogSaturation,
            vinylCrackle = vinylCrackle,
            vinylNoise = vinylNoise,
            tapeHiss = tapeHiss,
            wowFlutterDepth = wowFlutterDepth,
            wowFlutterRateHz = wowFlutterRateHz,
            warmBassBoostDb = warmBassBoostDb,
            softDistortion = softDistortion,
            reverbMix = reverbMix,
            reverbRoomSize = reverbRoomSize,
            stereoWidth = stereoWidth,
            softCompression = softCompression,
            highFreqReductionDb = highFreqReductionDb,
            trebleGainDb = trebleGainDb,
            playbackSpeed = playbackSpeed
        )
    )
}

@Entity(tableName = "custom_eq_presets")
data class CustomEqPresetEntity(
    @PrimaryKey val name: String,
    val gainsCsv: String,
    val bassBoostDb: Float,
    val trebleBoostDb: Float,
    val midrangeBoostDb: Float = 0.0f,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Dao
interface LofiDao {
    // Imported tracks
    @Query("SELECT * FROM imported_tracks ORDER BY dateAddedMillis DESC")
    fun getAllImportedTracks(): Flow<List<ImportedTrackEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImportedTracks(tracks: List<ImportedTrackEntity>)

    @Query("DELETE FROM imported_tracks WHERE id = :trackId")
    suspend fun deleteImportedTrack(trackId: String)

    // Favorites
    @Query("SELECT * FROM favorite_tracks ORDER BY addedAtMillis DESC")
    fun getAllFavorites(): Flow<List<FavoriteTrackEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(entity: FavoriteTrackEntity)

    @Query("DELETE FROM favorite_tracks WHERE trackId = :trackId")
    suspend fun removeFavorite(trackId: String)

    // Playlists
    @Query("SELECT * FROM playlists ORDER BY createdAtMillis DESC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Query("UPDATE playlists SET name = :name, description = :description WHERE playlistId = :playlistId")
    suspend fun updatePlaylistMetadata(playlistId: Long, name: String, description: String)

    @Query("DELETE FROM playlists WHERE playlistId = :playlistId")
    suspend fun deletePlaylist(playlistId: Long)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId")
    suspend fun clearPlaylistTracks(playlistId: Long)

    @Query("SELECT * FROM playlist_tracks ORDER BY playlistId ASC, sortIndex ASC, addedAtMillis ASC")
    fun getAllPlaylistTrackLinks(): Flow<List<PlaylistTrackCrossRef>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addTrackToPlaylist(ref: PlaylistTrackCrossRef)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistTrackLinks(refs: List<PlaylistTrackCrossRef>)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: String)

    @Query("DELETE FROM playlist_tracks WHERE trackId = :trackId")
    suspend fun removeTrackFromAllPlaylists(trackId: String)

    // Playback History
    @Query("SELECT * FROM playback_history ORDER BY lastPlayedAtMillis DESC LIMIT 50")
    fun getPlaybackHistory(): Flow<List<PlaybackHistoryEntity>>

    @Query("SELECT * FROM playback_history WHERE trackId = :trackId LIMIT 1")
    suspend fun getHistoryEntry(trackId: String): PlaybackHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHistoryEntry(entry: PlaybackHistoryEntity)

    @Query("DELETE FROM playback_history")
    suspend fun clearPlaybackHistory()

    // Custom Lo-Fi Presets
    @Query("SELECT * FROM custom_lofi_presets ORDER BY createdAtMillis DESC")
    fun getCustomLofiPresets(): Flow<List<CustomLofiPresetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCustomLofiPreset(preset: CustomLofiPresetEntity)

    @Query("UPDATE custom_lofi_presets SET name = :newName, subtitle = :newSubtitle WHERE id = :presetId")
    suspend fun renameCustomLofiPreset(presetId: String, newName: String, newSubtitle: String)

    @Query("DELETE FROM custom_lofi_presets WHERE id = :presetId")
    suspend fun deleteCustomLofiPreset(presetId: String)

    // Custom EQ Presets
    @Query("SELECT * FROM custom_eq_presets ORDER BY createdAtMillis DESC")
    fun getCustomEqPresets(): Flow<List<CustomEqPresetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCustomEqPreset(preset: CustomEqPresetEntity)

    @Query("DELETE FROM custom_eq_presets WHERE name = :name")
    suspend fun deleteCustomEqPreset(name: String)
}

@Database(
    entities = [
        ImportedTrackEntity::class,
        FavoriteTrackEntity::class,
        PlaylistEntity::class,
        PlaylistTrackCrossRef::class,
        PlaybackHistoryEntity::class,
        CustomLofiPresetEntity::class,
        CustomEqPresetEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class LofiDatabase : RoomDatabase() {
    abstract fun lofiDao(): LofiDao

    companion object {
        @Volatile
        private var INSTANCE: LofiDatabase? = null

        fun getInstance(context: Context): LofiDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LofiDatabase::class.java,
                    "lofi_wave_studio.db"
                ).fallbackToDestructiveMigration(true).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
