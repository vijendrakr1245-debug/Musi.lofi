package com.example.data

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import com.example.model.TrackItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object DeviceMusicScanner {

    private val SUPPORTED_EXTENSIONS = setOf("mp3", "wav", "flac", "aac", "m4a", "ogg", "opus")

    /**
     * Scans device MediaStore for audio files (MP3, WAV, FLAC, AAC, M4A, OGG).
     */
    suspend fun scanMediaStoreAudio(context: Context): List<TrackItem> = withContext(Dispatchers.IO) {
        val discovered = mutableListOf<TrackItem>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DISPLAY_NAME
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= 2000"
        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        runCatching {
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val displayCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)

                val albumArtBaseUri = Uri.parse("content://media/external/audio/albumart")

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                    val displayName = cursor.getString(displayCol) ?: "Track_$id"
                    val rawTitle = cursor.getString(titleCol)?.takeIf { it.isNotBlank() }
                        ?: displayName.substringBeforeLast('.')
                    val rawArtist = cursor.getString(artistCol)?.takeIf { it.isNotBlank() && it != "<unknown>" }
                        ?: "Device Artist"
                    val rawAlbum = cursor.getString(albumCol)?.takeIf { it.isNotBlank() && it != "<unknown>" }
                        ?: "Local Audio"
                    val albumId = cursor.getLong(albumIdCol)
                    val durationMs = cursor.getLong(durationMsIndex = durationCol).coerceAtLeast(1000L)
                    val mime = cursor.getString(mimeCol) ?: "audio/mpeg"
                    val dateAdded = cursor.getLong(dateCol) * 1000L

                    val formatBadge = detectFormatBadge(displayName, mime)
                    val mediaStoreArtUri = ContentUris.withAppendedId(albumArtBaseUri, albumId).toString()

                    val extracted = MediaMetadataRetrieverUtil.extractMetadata(
                        context = context,
                        uri = contentUri,
                        fallbackFileName = displayName,
                        fallbackArtist = rawArtist,
                        fallbackAlbum = rawAlbum,
                        fallbackGenre = "Local Music",
                        existingAlbumArtUri = mediaStoreArtUri
                    )

                    discovered.add(
                        TrackItem(
                            id = "mediastore_$id",
                            uriString = contentUri.toString(),
                            title = extracted.title.ifBlank { rawTitle },
                            artist = extracted.artist.ifBlank { rawArtist },
                            album = extracted.album.ifBlank { rawAlbum },
                            durationMs = if (extracted.durationMs > 0) extracted.durationMs else durationMs,
                            formatBadge = extracted.audioFormat.ifBlank { formatBadge },
                            folderName = "Device Music",
                            dateAddedMillis = if (dateAdded > 0) dateAdded else System.currentTimeMillis(),
                            albumArtUri = extracted.albumArtUri ?: mediaStoreArtUri,
                            lyricsOrNotes = "Loaded from Android MediaStore (${extracted.audioFormat} • ${extracted.sampleRateHz}Hz • ${extracted.bitrateKbps}kbps)\nOriginal file preserved unchanged. Real-time PCM Lo-Fi DSP active.",
                            isBuiltInSample = false,
                            genre = extracted.genre,
                            sampleRateHz = extracted.sampleRateHz,
                            bitrateKbps = extracted.bitrateKbps,
                            trackNumber = extracted.trackNumber,
                            channelCount = extracted.channelCount,
                            bitsPerSample = extracted.bitsPerSample
                        )
                    )
                }
            }
        }
        discovered
    }

    private fun android.database.Cursor.getLong(durationMsIndex: Int): Long = getLong(durationMsIndex)

    /**
     * Takes persistable URI permission and extracts metadata for a list of SAF document URIs.
     */
    suspend fun importDocumentUris(context: Context, uris: List<Uri>): List<ImportedTrackEntity> =
        withContext(Dispatchers.IO) {
            val imported = mutableListOf<ImportedTrackEntity>()
            for (uri in uris) {
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
                extractTrackFromUri(context, uri, folderLabel = "Imported Songs")?.let {
                    imported.add(it)
                }
            }
            imported
        }

    /**
     * Takes persistable URI permission on a folder tree URI and imports all supported audio files inside.
     */
    suspend fun importFolderTreeUri(context: Context, treeUri: Uri): List<ImportedTrackEntity> =
        withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    treeUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            val rootDoc = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext emptyList()
            val folderTitle = rootDoc.name ?: "Imported Folder"
            val results = mutableListOf<ImportedTrackEntity>()

            fun traverse(dir: DocumentFile, currentFolder: String, depth: Int) {
                if (depth > 3) return
                for (file in dir.listFiles()) {
                    if (file.isDirectory) {
                        traverse(file, file.name ?: currentFolder, depth + 1)
                    } else if (file.isFile) {
                        val name = file.name ?: continue
                        val ext = name.substringAfterLast('.', "").lowercase()
                        val mime = file.type ?: ""
                        if (ext in SUPPORTED_EXTENSIONS || mime.startsWith("audio/")) {
                            extractTrackFromUri(context, file.uri, currentFolder, name)?.let {
                                results.add(it)
                            }
                        }
                    }
                }
            }

            traverse(rootDoc, folderTitle, 0)
            results
        }

    private fun extractTrackFromUri(
        context: Context,
        uri: Uri,
        folderLabel: String,
        fallbackFileName: String? = null
    ): ImportedTrackEntity? {
        val docFile = DocumentFile.fromSingleUri(context, uri)
        val displayName = fallbackFileName ?: docFile?.name ?: uri.lastPathSegment ?: "Audio Track"
        val mime = docFile?.type ?: context.contentResolver.getType(uri) ?: "audio/mpeg"

        val extracted = MediaMetadataRetrieverUtil.extractMetadata(
            context = context,
            uri = uri,
            fallbackFileName = displayName,
            fallbackArtist = "Local Artist",
            fallbackAlbum = folderLabel,
            fallbackGenre = "Local Music",
            existingAlbumArtUri = null
        )

        val badge = extracted.audioFormat.ifBlank { detectFormatBadge(displayName, mime) }
        return ImportedTrackEntity(
            id = "saf_${uri.toString().hashCode()}",
            uriString = uri.toString(),
            title = extracted.title,
            artist = extracted.artist,
            album = extracted.album,
            durationMs = extracted.durationMs,
            formatBadge = badge,
            folderName = folderLabel,
            dateAddedMillis = System.currentTimeMillis(),
            albumArtUri = extracted.albumArtUri,
            lyricsOrNotes = "Imported via Android Storage Access Framework ($badge • ${extracted.sampleRateHz}Hz • ${extracted.bitrateKbps}kbps)\nPersistent URI permission active. Original audio file untouched.",
            genre = extracted.genre,
            sampleRateHz = extracted.sampleRateHz,
            bitrateKbps = extracted.bitrateKbps,
            trackNumber = extracted.trackNumber,
            channelCount = extracted.channelCount,
            bitsPerSample = extracted.bitsPerSample
        )
    }

    /**
     * Checks if a TrackItem's URI is still accessible (handles moved/deleted files gracefully).
     */
    suspend fun isTrackUriAccessible(context: Context, track: TrackItem): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                val uri = Uri.parse(track.uriString)
                when (uri.scheme) {
                    "file" -> {
                        val path = uri.path ?: return@runCatching false
                        File(path).exists()
                    }
                    "content" -> {
                        context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { true } ?: false
                    }
                    else -> true
                }
            }.getOrDefault(false)
        }

    private fun detectFormatBadge(fileName: String, mimeType: String): String {
        val ext = fileName.substringAfterLast('.', "").uppercase()
        if (ext in setOf("MP3", "WAV", "FLAC", "AAC", "M4A", "OGG", "OPUS")) {
            return ext
        }
        val lowerMime = mimeType.lowercase()
        return when {
            "flac" in lowerMime -> "FLAC"
            "wav" in lowerMime -> "WAV"
            "mp4" in lowerMime || "m4a" in lowerMime -> "M4A"
            "aac" in lowerMime -> "AAC"
            "ogg" in lowerMime || "vorbis" in lowerMime -> "OGG"
            else -> "MP3"
        }
    }
}
