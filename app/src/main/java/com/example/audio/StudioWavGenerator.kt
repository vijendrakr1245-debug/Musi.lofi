package com.example.audio

import android.content.Context
import android.net.Uri
import com.example.model.TrackItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Generates real 44.1kHz 16-bit stereo WAV multi-instrument tracks on device storage
 * so users on fresh emulators or devices without local MP3s can immediately test
 * Original Hi-Fi vs. Live Lo-Fi DSP conversion, while also supporting any device MP3/WAV/FLAC/M4A/OGG.
 */
object StudioWavGenerator {

    private data class StudioSongSpec(
        val id: String,
        val fileName: String,
        val title: String,
        val artist: String,
        val album: String,
        val bpm: Float,
        val durationSeconds: Int,
        val chordProgressionMidi: Array<IntArray>,
        val melodyMidi: IntArray,
        val lyricsOrNotes: String
    )

    private val STUDIO_SONGS = listOf(
        StudioSongSpec(
            id = "studio_midnight_tokyo",
            fileName = "Midnight_Tokyo_Rain_Master.wav",
            title = "Midnight Tokyo Rain",
            artist = "Kitsune & The Analog Waves",
            album = "Shibuya Tape Sessions Vol. 4",
            bpm = 76f,
            durationSeconds = 24,
            chordProgressionMidi = arrayOf(
                intArrayOf(53, 57, 60, 64, 67), // Fmaj9
                intArrayOf(52, 55, 59, 62, 67), // Em7
                intArrayOf(50, 53, 57, 60, 64), // Dm9
                intArrayOf(48, 52, 55, 59, 62)  // Cmaj9
            ),
            melodyMidi = intArrayOf(72, 71, 67, 64, 69, 67, 64, 60, 65, 64, 60, 57, 64, 62, 59, 55),
            lyricsOrNotes = """
                [00:00] ♪ Warm Rhodes electric piano & deep sub-bass groove
                [00:04] Neon reflections shimmer across wet Shibuya crosswalks
                [00:08] Try toggling HI-FI ORIGINAL vs LIVE LO-FI DSP in real time
                [00:12] Notice how Vintage Vinyl adds dust crackle & turntable flutter
                [00:16] Adjust Low-Pass Cutoff & Reverb in the Lo-Fi Studio tab
                [00:20] Pure 44.1kHz 16-bit stereo PCM processed live on device ♪
            """.trimIndent()
        ),
        StudioSongSpec(
            id = "studio_velvet_rhodes",
            fileName = "Velvet_Rhodes_Study_Session.wav",
            title = "Velvet Rhodes Study",
            artist = "Nocturne Tape Collective",
            album = "Late Night Desk Lamp",
            bpm = 82f,
            durationSeconds = 22,
            chordProgressionMidi = arrayOf(
                intArrayOf(50, 53, 57, 60, 64), // Dm9
                intArrayOf(55, 59, 62, 65, 69), // G13
                intArrayOf(48, 52, 55, 59, 62), // Cmaj9
                intArrayOf(45, 49, 52, 55, 60)  // A7alt
            ),
            melodyMidi = intArrayOf(69, 72, 74, 76, 74, 71, 67, 69, 67, 64, 60, 62, 64, 67, 69, 64),
            lyricsOrNotes = """
                [00:00] ♪ Jazz-inflected Rhodes chords (Dm9 - G13 - Cmaj9 - A7)
                [00:05] Engineered with rich upper harmonics to showcase Low-Pass filtering
                [00:10] Switch to 'Warm Cassette' preset for authentic tape hiss & wow/flutter
                [00:15] Try the 'Anime Memories' preset for 10-bit SP-404 sampler crunch ♪
            """.trimIndent()
        ),
        StudioSongSpec(
            id = "studio_sunset_drive",
            fileName = "Analog_Sunset_Highway.wav",
            title = "Analog Sunset Drive",
            artist = "Lofi Wave Sound Lab",
            album = "Pacific Coast Cassette",
            bpm = 88f,
            durationSeconds = 22,
            chordProgressionMidi = arrayOf(
                intArrayOf(45, 52, 55, 59, 64), // Am9
                intArrayOf(41, 48, 53, 57, 60), // Fmaj7
                intArrayOf(48, 52, 55, 59, 62), // Cmaj9
                intArrayOf(43, 50, 55, 59, 62)  // G6
            ),
            melodyMidi = intArrayOf(76, 74, 72, 69, 72, 69, 65, 64, 67, 69, 71, 72, 74, 71, 67, 62),
            lyricsOrNotes = """
                [00:00] ♪ Driving warm synth-piano arpeggios & punchy boom-bap kick/snare
                [00:06] Select 'Midnight Drive' preset for +7.2dB sub-bass & 160% stereo width
                [00:12] Open the 10-Band Equalizer to sculpt sub-bass (31Hz-62Hz) & air (16kHz) ♪
            """.trimIndent()
        ),
        StudioSongSpec(
            id = "studio_autumn_cafe",
            fileName = "Autumn_Espresso_Acoustic.wav",
            title = "Autumn Espresso",
            artist = "Kyoto Vinyl Society",
            album = "wooden tables & rain",
            bpm = 72f,
            durationSeconds = 22,
            chordProgressionMidi = arrayOf(
                intArrayOf(51, 55, 58, 62, 65), // Ebmaj9
                intArrayOf(50, 53, 57, 60, 65), // Dm7b5
                intArrayOf(48, 51, 55, 58, 62), // Cm9
                intArrayOf(46, 50, 53, 57, 60)  // Bbmaj9
            ),
            melodyMidi = intArrayOf(70, 67, 65, 62, 65, 62, 60, 57, 63, 62, 58, 55, 62, 60, 57, 53),
            lyricsOrNotes = """
                [00:00] ♪ Intimate felt-piano voicing & mellow upright bassline
                [00:06] Pair with 'Coffee Shop' or 'Emotional Piano' in Lo-Fi Studio
                [00:12] Use the AI Music Assistant to auto-match mood presets & EQ curves ♪
            """.trimIndent()
        )
    )

    suspend fun ensureStudioTracksGenerated(context: Context): List<TrackItem> = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "studio_tracks")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        val items = mutableListOf<TrackItem>()
        for ((idx, spec) in STUDIO_SONGS.withIndex()) {
            val file = File(dir, spec.fileName)
            if (!file.exists() || file.length() < 44100L) {
                writeStereoWavFile(file, spec)
            }
            items.add(
                TrackItem(
                    id = spec.id,
                    uriString = Uri.fromFile(file).toString(),
                    title = spec.title,
                    artist = spec.artist,
                    album = spec.album,
                    durationMs = spec.durationSeconds * 1000L,
                    formatBadge = "WAV 16-BIT",
                    folderName = "Lofi Wave Studio Masters",
                    dateAddedMillis = System.currentTimeMillis() - (idx * 60_000L),
                    albumArtUri = null,
                    lyricsOrNotes = spec.lyricsOrNotes,
                    isBuiltInSample = true
                )
            )
        }
        items
    }

    private fun midiToFreq(midi: Int): Double {
        return 440.0 * Math.pow(2.0, (midi - 69) / 12.0)
    }

    private fun writeStereoWavFile(file: File, spec: StudioSongSpec) {
        val sampleRate = 44100
        val channels = 2
        val totalFrames = sampleRate * spec.durationSeconds
        val dataSize = totalFrames * channels * 2
        val totalFileSize = 36 + dataSize

        // Precompute frequencies
        val chordFreqs = Array(spec.chordProgressionMidi.size) { cIdx ->
            DoubleArray(spec.chordProgressionMidi[cIdx].size) { nIdx ->
                midiToFreq(spec.chordProgressionMidi[cIdx][nIdx])
            }
        }
        val melodyFreqs = DoubleArray(spec.melodyMidi.size) { mIdx ->
            midiToFreq(spec.melodyMidi[mIdx])
        }

        val secondsPerBeat = 60.0 / spec.bpm
        val secondsPerBar = secondsPerBeat * 4.0
        val secondsPerEighth = secondsPerBeat * 0.5

        BufferedOutputStream(FileOutputStream(file), 65536).use { out ->
            val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
            header.put("RIFF".toByteArray(Charsets.US_ASCII))
            header.putInt(totalFileSize)
            header.put("WAVE".toByteArray(Charsets.US_ASCII))
            header.put("fmt ".toByteArray(Charsets.US_ASCII))
            header.putInt(16) // PCM chunk size
            header.putShort(1) // PCM format
            header.putShort(channels.toShort())
            header.putInt(sampleRate)
            header.putInt(sampleRate * channels * 2) // byteRate
            header.putShort((channels * 2).toShort()) // blockAlign
            header.putShort(16) // bitsPerSample
            header.put("data".toByteArray(Charsets.US_ASCII))
            header.putInt(dataSize)
            out.write(header.array())

            val chunkFrames = 2048
            val chunkBuffer = ByteBuffer.allocate(chunkFrames * 4).order(ByteOrder.LITTLE_ENDIAN)
            var frame = 0
            var noiseSeed = 0x2468ACE1u

            while (frame < totalFrames) {
                chunkBuffer.clear()
                val framesToWrite = minOf(chunkFrames, totalFrames - frame)
                for (i in 0 until framesToWrite) {
                    val globalFrame = frame + i
                    val t = globalFrame.toDouble() / sampleRate.toDouble()

                    val barIndex = (t / secondsPerBar).toInt()
                    val chordIdx = barIndex % chordFreqs.size
                    val timeInBar = t - (barIndex * secondsPerBar)
                    val chordEnv = exp(-timeInBar * 1.15).toFloat()

                    val chord = chordFreqs[chordIdx]
                    var rhodesL = 0f
                    var rhodesR = 0f

                    // Warm stereo Rhodes electric piano with 2nd & 3rd harmonics (great for testing Low-Pass & Saturation!)
                    for (n in chord.indices) {
                        val f = chord[n]
                        val fundamental = sin(2.0 * PI * f * t).toFloat()
                        val harm2 = sin(2.0 * PI * (f * 2.0) * t).toFloat() * 0.28f * exp(-timeInBar * 2.8).toFloat()
                        val harm3 = sin(2.0 * PI * (f * 3.0) * t).toFloat() * 0.12f * exp(-timeInBar * 4.2).toFloat()
                        val voice = (fundamental + harm2 + harm3) * chordEnv * 0.14f
                        val pan = (n.toFloat() / (chord.size - 1).coerceAtLeast(1))
                        rhodesL += voice * (1.0f - pan * 0.45f)
                        rhodesR += voice * (0.55f + pan * 0.45f)
                    }

                    // Sub-bass root note
                    val bassFreq = chord[0] * 0.5
                    val beatIndex = (t / secondsPerBeat).toInt()
                    val timeInBeat = t - (beatIndex * secondsPerBeat)
                    val bassEnv = exp(-timeInBeat * 2.2).toFloat()
                    val subBass = (sin(2.0 * PI * bassFreq * t) + 0.35 * sin(2.0 * PI * bassFreq * 2.0 * t)).toFloat() * bassEnv * 0.24f

                    // Lead bell/piano melody on eighth notes
                    val eighthIndex = (t / secondsPerEighth).toInt()
                    val timeInEighth = t - (eighthIndex * secondsPerEighth)
                    val melFreq = melodyFreqs[eighthIndex % melodyFreqs.size]
                    val melEnv = exp(-timeInEighth * 5.2).toFloat()
                    val melody = (sin(2.0 * PI * melFreq * t) + 0.4 * sin(2.0 * PI * melFreq * 2.0 * t) + 0.18 * sin(2.0 * PI * melFreq * 4.0 * t)).toFloat() * melEnv * 0.15f

                    // Crisp boom-bap drum groove (Kick on beats 0 & 2.5, Snare on beats 1 & 3, Hi-hat on 8ths)
                    val beatInBar = timeInBar / secondsPerBeat
                    var drums = 0f

                    // Kick drum on beat 0 and beat 2
                    val beatMod2 = timeInBar % (secondsPerBeat * 2.0)
                    if (beatMod2 < 0.18) {
                        val kt = beatMod2
                        val kFreq = 52.0 + 110.0 * exp(-kt * 38.0)
                        drums += (sin(2.0 * PI * kFreq * kt) * exp(-kt * 16.0)).toFloat() * 0.38f
                    }

                    // Snare + brush on beat 1 and beat 3
                    val snarePhase = (timeInBar + secondsPerBeat) % (secondsPerBeat * 2.0)
                    noiseSeed = noiseSeed xor (noiseSeed shl 13)
                    noiseSeed = noiseSeed xor (noiseSeed shr 17)
                    noiseSeed = noiseSeed xor (noiseSeed shl 5)
                    val white = ((noiseSeed and 0x7FFFFFFFu).toFloat() / 1073741824.0f) - 1.0f

                    if (snarePhase < 0.16) {
                        val st = snarePhase
                        val snareBody = sin(2.0 * PI * 185.0 * st).toFloat() * exp(-st * 26.0).toFloat() * 0.18f
                        val snareSnap = white * exp(-st * 22.0).toFloat() * 0.22f
                        drums += snareBody + snareSnap
                    }

                    // Crispy hi-hat on every eighth note (makes Low-Pass & High-Frequency reduction immediately audible!)
                    if (timeInEighth < 0.065) {
                        val hatEnv = exp(-timeInEighth * 55.0).toFloat()
                        drums += white * hatEnv * 0.11f
                    }

                    val mixL = (rhodesL + subBass + melody * 0.85f + drums).coerceIn(-0.95f, 0.95f)
                    val mixR = (rhodesR + subBass + melody * 1.15f + drums).coerceIn(-0.95f, 0.95f)

                    chunkBuffer.putShort((mixL * 32767f).toInt().toShort())
                    chunkBuffer.putShort((mixR * 32767f).toInt().toShort())
                }
                out.write(chunkBuffer.array(), 0, framesToWrite * 4)
                frame += framesToWrite
            }
        }
    }
}
