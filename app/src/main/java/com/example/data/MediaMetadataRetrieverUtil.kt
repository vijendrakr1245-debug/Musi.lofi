package com.example.data

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs

/**
 * Extracted audio file metadata obtained via Android's [MediaMetadataRetriever]
 * with RIFF WAVE / header fallback inspection for uncompressed PCM files.
 */
data class ExtractedAudioMetadata(
    val title: String,
    val artist: String,
    val album: String,
    val albumArtUri: String?,
    val durationMs: Long,
    val genre: String,
    val audioFormat: String,
    val mimeType: String,
    val sampleRateHz: Int,
    val bitrateKbps: Int,
    val trackNumber: Int?,
    val channelCount: Int,
    val bitsPerSample: Int
)

/**
 * Utility that uses Android's [MediaMetadataRetriever] to extract song titles, artist names,
 * album titles, embedded album artwork (cached locally for Coil rendering), duration, genre,
 * audio format, sample rate, bitrate, and track number from local storage files and content URIs.
 */
object MediaMetadataRetrieverUtil {

    private val SUPPORTED_AUDIO_EXTENSIONS = setOf(
        "mp3", "wav", "flac", "aac", "m4a", "ogg", "opus"
    )

    fun isSupportedAudioExtension(fileNameOrPath: String): Boolean {
        val ext = fileNameOrPath.substringAfterLast('.', "").lowercase()
        return ext in SUPPORTED_AUDIO_EXTENSIONS
    }

    /**
     * Extracts comprehensive metadata and embedded album artwork from a local [File] or [Uri].
     * Always handles missing tags gracefully and releases native [MediaMetadataRetriever] resources.
     */
    fun extractMetadata(
        context: Context,
        uri: Uri,
        fallbackFileName: String? = null,
        fallbackArtist: String = "Local Artist",
        fallbackAlbum: String = "Local Audio",
        fallbackGenre: String = "Music",
        existingAlbumArtUri: String? = null
    ): ExtractedAudioMetadata {
        val rawFileName = fallbackFileName
            ?: uri.lastPathSegment?.substringAfterLast('/')
            ?: "Audio Track"
        val cleanFallbackTitle = cleanDisplayTitle(rawFileName)
        val extension = rawFileName.substringAfterLast('.', "").lowercase()

        var rawTitle: String? = null
        var rawArtist: String? = null
        var rawAlbum: String? = null
        var rawGenre: String? = null
        var rawMime: String? = null
        var durationMs = 0L
        var sampleRateHz = 0
        var bitrateKbps = 0
        var trackNumber: Int? = null
        var channelCount = 2
        var bitsPerSample = 16
        var embeddedArtBytes: ByteArray? = null

        val retriever = MediaMetadataRetriever()
        try {
            if (uri.scheme == "file") {
                val path = uri.path
                if (!path.isNullOrBlank() && File(path).exists()) {
                    retriever.setDataSource(path)
                } else {
                    retriever.setDataSource(context, uri)
                }
            } else {
                retriever.setDataSource(context, uri)
            }

            rawTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?.trim()?.takeIf { it.isNotBlank() }

            rawArtist = (retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?.trim()?.takeIf { it.isNotBlank() && it != "<unknown>" }
                ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)
                    ?.trim()?.takeIf { it.isNotBlank() && it != "<unknown>" }
                ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_AUTHOR)
                    ?.trim()?.takeIf { it.isNotBlank() && it != "<unknown>" })

            rawAlbum = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                ?.trim()?.takeIf { it.isNotBlank() && it != "<unknown>" }

            rawGenre = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)
                ?.trim()?.takeIf { it.isNotBlank() && it != "<unknown>" }

            rawMime = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
                ?.trim()?.takeIf { it.isNotBlank() }

            durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()?.coerceAtLeast(0L) ?: 0L

            val bitrateBitsPerSec = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
                ?.toLongOrNull() ?: 0L
            if (bitrateBitsPerSec > 0L) {
                bitrateKbps = (bitrateBitsPerSec / 1000L).toInt().coerceIn(32, 9216)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                sampleRateHz = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)
                    ?.toIntOrNull()?.coerceIn(8000, 192000) ?: 0
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                bitsPerSample = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITS_PER_SAMPLE)
                    ?.toIntOrNull()?.coerceIn(8, 32) ?: 16
            }

            val rawTrackNum = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)
                ?.trim()
            if (!rawTrackNum.isNullOrBlank()) {
                trackNumber = rawTrackNum.substringBefore('/').trim().toIntOrNull()
            }

            embeddedArtBytes = try {
                retriever.embeddedPicture
            } catch (_: Exception) {
                null
            }
        } catch (_: Exception) {
            // Gracefully handle unreadable or header-only files
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {}
        }

        // If WAV or missing technical properties, inspect RIFF WAVE header directly
        val wavInfo = inspectWavHeaderIfApplicable(context, uri, extension, rawMime)
        if (wavInfo != null) {
            if (sampleRateHz <= 0) sampleRateHz = wavInfo.sampleRateHz
            if (bitrateKbps <= 0) bitrateKbps = wavInfo.bitrateKbps
            if (durationMs <= 0L) durationMs = wavInfo.durationMs
            channelCount = wavInfo.channels
            bitsPerSample = wavInfo.bitsPerSample
            if (rawMime.isNullOrBlank()) rawMime = "audio/wav"
        }

        val resolvedFormat = resolveFormatBadge(extension, rawMime)
        if (sampleRateHz <= 0) {
            sampleRateHz = 44100
        }
        if (bitrateKbps <= 0) {
            bitrateKbps = when (resolvedFormat) {
                "WAV" -> (sampleRateHz * channelCount * bitsPerSample) / 1000
                "FLAC" -> 960
                "MP3" -> 320
                "AAC", "M4A" -> 256
                "OGG", "OPUS" -> 192
                else -> 320
            }
        }
        if (durationMs <= 0L) {
            durationMs = 180_000L
        }

        val cachedArtUri = if (embeddedArtBytes != null && embeddedArtBytes.isNotEmpty()) {
            cacheEmbeddedArtwork(context, uri, embeddedArtBytes) ?: existingAlbumArtUri
        } else {
            existingAlbumArtUri
        }

        // Parse "Artist - Title" pattern from filename when ID3 tags are absent
        var finalTitle = rawTitle ?: cleanFallbackTitle
        var finalArtist = rawArtist ?: fallbackArtist
        if (rawTitle == null && rawArtist == null && cleanFallbackTitle.contains(" - ")) {
            val parts = cleanFallbackTitle.split(" - ", limit = 2)
            if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
                finalArtist = parts[0].trim()
                finalTitle = parts[1].trim()
            }
        }

        return ExtractedAudioMetadata(
            title = finalTitle.ifBlank { "Untitled Track" },
            artist = finalArtist.ifBlank { fallbackArtist },
            album = (rawAlbum ?: fallbackAlbum).ifBlank { fallbackAlbum },
            albumArtUri = cachedArtUri,
            durationMs = durationMs,
            genre = (rawGenre ?: fallbackGenre).ifBlank { fallbackGenre },
            audioFormat = resolvedFormat,
            mimeType = rawMime ?: mimeFromExtension(extension),
            sampleRateHz = sampleRateHz,
            bitrateKbps = bitrateKbps,
            trackNumber = trackNumber,
            channelCount = channelCount.coerceIn(1, 8),
            bitsPerSample = bitsPerSample.coerceIn(8, 32)
        )
    }

    private fun cleanDisplayTitle(fileName: String): String {
        return fileName
            .substringBeforeLast('.')
            .replace('_', ' ')
            .trim()
            .ifBlank { "Audio Track" }
    }

    private fun resolveFormatBadge(extension: String, mimeType: String?): String {
        val extUpper = extension.uppercase()
        if (extUpper in setOf("WAV", "MP3", "FLAC", "AAC", "M4A", "OGG", "OPUS")) {
            return extUpper
        }
        val mime = mimeType?.lowercase() ?: ""
        return when {
            mime.contains("wav") || mime.contains("x-wav") || mime.contains("pcm") -> "WAV"
            mime.contains("flac") -> "FLAC"
            mime.contains("mpeg") || mime.contains("mp3") -> "MP3"
            mime.contains("mp4") || mime.contains("m4a") -> "M4A"
            mime.contains("aac") -> "AAC"
            mime.contains("ogg") || mime.contains("vorbis") || mime.contains("opus") -> "OGG"
            else -> "AUDIO"
        }
    }

    private fun mimeFromExtension(extension: String): String {
        return when (extension.lowercase()) {
            "wav" -> "audio/wav"
            "flac" -> "audio/flac"
            "mp3" -> "audio/mpeg"
            "m4a" -> "audio/mp4"
            "aac" -> "audio/aac"
            "ogg", "opus" -> "audio/ogg"
            else -> "audio/*"
        }
    }

    private fun cacheEmbeddedArtwork(context: Context, sourceUri: Uri, bytes: ByteArray): String? {
        return try {
            val artDir = File(context.cacheDir, "embedded_album_art").apply { mkdirs() }
            val hash = abs(sourceUri.toString().hashCode())
            val artFile = File(artDir, "cover_$hash.jpg")
            if (!artFile.exists() || artFile.length() != bytes.size.toLong()) {
                FileOutputStream(artFile).use { out ->
                    out.write(bytes)
                    out.flush()
                }
            }
            Uri.fromFile(artFile).toString()
        } catch (_: Exception) {
            null
        }
    }

    private data class WavHeaderTechnicalInfo(
        val sampleRateHz: Int,
        val channels: Int,
        val bitsPerSample: Int,
        val bitrateKbps: Int,
        val durationMs: Long
    )

    private fun inspectWavHeaderIfApplicable(
        context: Context,
        uri: Uri,
        extension: String,
        mimeType: String?
    ): WavHeaderTechnicalInfo? {
        val looksLikeWav = extension == "wav" ||
            (mimeType?.contains("wav", ignoreCase = true) == true) ||
            (uri.path?.endsWith(".wav", ignoreCase = true) == true)
        if (!looksLikeWav) return null

        return try {
            val header = ByteArray(44)
            var totalLengthBytes = 0L

            if (uri.scheme == "file") {
                val f = File(uri.path ?: return null)
                if (!f.exists() || f.length() < 44L) return null
                totalLengthBytes = f.length()
                f.inputStream().use { ins ->
                    if (ins.read(header) < 44) return null
                }
            } else {
                val pfd = context.contentResolver.openFileDescriptor(uri, "r")
                totalLengthBytes = pfd?.statSize ?: 0L
                pfd?.close()
                val ins: InputStream = context.contentResolver.openInputStream(uri) ?: return null
                ins.use {
                    if (it.read(header) < 44) return null
                }
            }

            val riff = String(header, 0, 4, Charsets.US_ASCII)
            val wave = String(header, 8, 4, Charsets.US_ASCII)
            if (riff != "RIFF" || wave != "WAVE") return null

            val bb = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
            val channels = bb.getShort(22).toInt().coerceIn(1, 8)
            val sampleRate = bb.getInt(24).coerceIn(8000, 192000)
            val byteRate = bb.getInt(28)
            val bitsPerSample = bb.getShort(34).toInt().coerceIn(8, 32)
            val dataSubchunkSize = bb.getInt(40).toLong().coerceAtLeast(0L)

            val effectiveByteRate = if (byteRate > 0) {
                byteRate
            } else {
                sampleRate * channels * (bitsPerSample / 8)
            }
            val bitrateKbps = ((effectiveByteRate * 8L) / 1000L).toInt().coerceIn(64, 9216)
            val payloadBytes = if (totalLengthBytes > 44L) (totalLengthBytes - 44L) else dataSubchunkSize
            val durationMs = if (effectiveByteRate > 0 && payloadBytes > 0L) {
                (payloadBytes * 1000L) / effectiveByteRate
            } else {
                0L
            }

            WavHeaderTechnicalInfo(
                sampleRateHz = sampleRate,
                channels = channels,
                bitsPerSample = bitsPerSample,
                bitrateKbps = bitrateKbps,
                durationMs = durationMs
            )
        } catch (_: Exception) {
            null
        }
    }
}
