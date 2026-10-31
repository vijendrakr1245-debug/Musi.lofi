package com.example.model

import androidx.compose.runtime.Immutable
import kotlin.math.max

@Immutable
data class LofiDspParameters(
    val isLiveLofiEnabled: Boolean = true,
    val presetId: String = "soft_lofi",
    val presetName: String = "Soft Lo-Fi",
    val masterIntensity: Float = 0.72f,
    val lowPassCutoffHz: Float = 8800f,
    val highPassCutoffHz: Float = 28f,
    val bitDepth: Float = 16.0f,
    val sampleRateDivisor: Float = 1.0f,
    val analogSaturation: Float = 0.18f,
    val vinylCrackle: Float = 0.05f,
    val vinylNoise: Float = 0.06f,
    val tapeHiss: Float = 0.02f,
    val wowFlutterDepth: Float = 0.08f,
    val wowFlutterRateHz: Float = 0.95f,
    val warmBassBoostDb: Float = 1.5f,
    val softDistortion: Float = 0.0f,
    val reverbMix: Float = 0.12f,
    val reverbRoomSize: Float = 0.56f,
    val stereoWidth: Float = 1.05f,
    val softCompression: Float = 0.10f,
    val highFreqReductionDb: Float = 1.0f,
    val trebleGainDb: Float = -0.5f,
    val playbackSpeed: Float = 1.0f,
    val masterGainDb: Float = 0.0f
) {
    /**
     * Combined noise intensity (0.0 .. 1.0) representing Vinyl Noise, Crackle, and Tape Hiss.
     */
    val noiseIntensity: Float
        get() = max(vinylNoise, max(vinylCrackle, tapeHiss)).coerceIn(0f, 1f)

    /**
     * Normalized Bitcrusher intensity (0.0 = 16-bit / 1.0x clean, 1.0 = 6-bit / 4.0x vintage sampler).
     */
    val bitcrusherIntensity: Float
        get() {
            val depthNorm = ((16.0f - bitDepth.coerceIn(6f, 16f)) / 10.0f).coerceIn(0f, 1f)
            val srNorm = ((sampleRateDivisor.coerceIn(1f, 4f) - 1.0f) / 3.0f).coerceIn(0f, 1f)
            return max(depthNorm, srNorm)
        }
}

@Immutable
data class LofiPreset(
    val id: String,
    val name: String,
    val subtitle: String,
    val moodCategory: String,
    val isCustom: Boolean = false,
    val parameters: LofiDspParameters
)

@Immutable
data class EqualizerState(
    val isEnabled: Boolean = false,
    val presetName: String = "Flat Studio",
    // 10 bands: 31Hz, 62Hz, 125Hz, 250Hz, 500Hz, 1kHz, 2kHz, 4kHz, 8kHz, 16kHz
    val bandGainsDb: List<Float> = listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f),
    val bassBoostDb: Float = 0.0f,
    val trebleBoostDb: Float = 0.0f,
    val stereoBalance: Float = 0.0f,
    val volumeNormalization: Boolean = true,
    val smoothTransitions: Boolean = true,
    val fadeInOutMs: Int = 250,
    val gaplessPlayback: Boolean = true
) {
    companion object {
        val BAND_FREQUENCIES = listOf(31, 62, 125, 250, 500, 1000, 2000, 4000, 8000, 16000)
        val BAND_LABELS = listOf("31", "62", "125", "250", "500", "1k", "2k", "4k", "8k", "16k")

        val BUILT_IN_EQ_PRESETS: Map<String, List<Float>> = linkedMapOf(
            "Flat Studio" to listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f),
            "Lo-Fi Warmth" to listOf(1.5f, 2.0f, 1.2f, 0.4f, 0.0f, -0.3f, -0.6f, -1.2f, -1.8f, -2.2f),
            "Deep Sub Bass" to listOf(3.0f, 2.8f, 1.8f, 0.5f, 0.0f, 0.0f, 0.2f, 0.5f, 0.5f, 0.0f),
            "Warm Cassette EQ" to listOf(-1.0f, 1.5f, 2.2f, 1.5f, 0.6f, 0.0f, -1.0f, -2.0f, -3.0f, -3.5f),
            "Vocal & Piano Clarity" to listOf(-1.0f, -0.5f, 0.0f, 1.2f, 2.2f, 2.8f, 2.2f, 1.2f, 0.5f, 0.0f),
            "Late Night Study" to listOf(1.0f, 1.5f, 1.0f, 0.2f, 0.0f, -0.5f, -1.2f, -1.8f, -2.4f, -3.0f)
        )
    }
}

@Immutable
data class TrackItem(
    val id: String,
    val uriString: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val formatBadge: String = "WAV",
    val folderName: String = "Studio Library",
    val dateAddedMillis: Long = System.currentTimeMillis(),
    val albumArtUri: String? = null,
    val lyricsOrNotes: String = "",
    val isBuiltInSample: Boolean = false
)

data class VisualizerSnapshot(
    val waveform: FloatArray = FloatArray(64),
    val spectrum: FloatArray = FloatArray(32),
    val rmsLeft: Float = 0f,
    val rmsRight: Float = 0f,
    val peakDb: Float = -60f,
    val peakLevelLinear: Float = 0f,
    val gainReductionDb: Float = 0f,
    val isLofiActive: Boolean = true,
    val effectiveSampleRateHz: Int = 44100,
    val activeLowPassHz: Int = 8800,
    val activeBitDepth: Float = 16.0f,
    val bufferLatencyMs: Float = 3.8f,
    val clippingCount: Int = 0,
    val dcOffsetLeft: Float = 0f,
    val dcOffsetRight: Float = 0f,
    val channelBalanceDb: Float = 0f,
    val underrunCount: Int = 0,
    val dspCpuTimeUs: Float = 0f
)

enum class LibrarySortOrder(val label: String) {
    TITLE("Title (A-Z)"),
    ARTIST("Artist (A-Z)"),
    RECENTLY_ADDED("Recently Added"),
    DURATION("Duration")
}

enum class LibraryFilterTab(val label: String) {
    ALL_SONGS("All Songs"),
    RECENTLY_ADDED("Recently Added"),
    RECENTLY_PLAYED("Recently Played"),
    FAVORITES("Favorites"),
    ALBUMS("Albums"),
    ARTISTS("Artists"),
    FOLDERS("Folders"),
    CUSTOM_PLAYLISTS("Custom Playlists")
}

@Immutable
data class AiMoodRecommendation(
    val moodId: String,
    val moodTitle: String,
    val emojiTag: String,
    val description: String,
    val recommendedPresetId: String,
    val recommendedPresetName: String,
    val dspRecipeSummary: String,
    val eqPresetName: String,
    val customParameters: LofiDspParameters,
    val suggestedTrackKeywords: List<String>
)

object BuiltInLofiPresets {
    val ALL: List<LofiPreset> = listOf(
        // 1. Original – no DSP effects (Pure Hi-Fi bypass)
        LofiPreset(
            id = "original",
            name = "Original",
            subtitle = "Bit-transparent Hi-Fi playback with all Lo-Fi DSP stages bypassed",
            moodCategory = "Pure Hi-Fi",
            parameters = LofiDspParameters(
                isLiveLofiEnabled = false,
                presetId = "original",
                presetName = "Original",
                masterIntensity = 0.0f,
                lowPassCutoffHz = 18000f,
                highPassCutoffHz = 20f,
                bitDepth = 16.0f,
                sampleRateDivisor = 1.0f,
                analogSaturation = 0.0f,
                vinylCrackle = 0.0f,
                vinylNoise = 0.0f,
                tapeHiss = 0.0f,
                wowFlutterDepth = 0.0f,
                wowFlutterRateHz = 0.95f,
                warmBassBoostDb = 0.0f,
                softDistortion = 0.0f,
                reverbMix = 0.0f,
                reverbRoomSize = 0.50f,
                stereoWidth = 1.0f,
                softCompression = 0.0f,
                highFreqReductionDb = 0.0f,
                trebleGainDb = 0.0f,
                playbackSpeed = 1.0f,
                masterGainDb = 0.0f
            )
        ),
        // 2. Soft Lo-Fi – clean and warm
        LofiPreset(
            id = "soft_lofi",
            name = "Soft Lo-Fi",
            subtitle = "Clean, warm analog tape character with articulate vocals and balanced bass",
            moodCategory = "Clean & Warm",
            parameters = LofiDspParameters(
                isLiveLofiEnabled = true,
                presetId = "soft_lofi",
                presetName = "Soft Lo-Fi",
                masterIntensity = 0.72f,
                lowPassCutoffHz = 8800f,
                highPassCutoffHz = 28f,
                bitDepth = 16.0f,
                sampleRateDivisor = 1.0f,
                analogSaturation = 0.18f,
                vinylCrackle = 0.05f,
                vinylNoise = 0.06f,
                tapeHiss = 0.02f,
                wowFlutterDepth = 0.08f,
                wowFlutterRateHz = 0.95f,
                warmBassBoostDb = 1.5f,
                softDistortion = 0.0f,
                reverbMix = 0.12f,
                reverbRoomSize = 0.56f,
                stereoWidth = 1.05f,
                softCompression = 0.10f,
                highFreqReductionDb = 1.0f,
                trebleGainDb = -0.5f,
                playbackSpeed = 1.0f,
                masterGainDb = 0.0f
            )
        ),
        // 3. Deep Lo-Fi – controlled bass
        LofiPreset(
            id = "deep_lofi",
            name = "Deep Lo-Fi",
            subtitle = "Controlled, punchy low-end warmth with silky top-end roll-off and clear mids",
            moodCategory = "Controlled Bass",
            parameters = LofiDspParameters(
                isLiveLofiEnabled = true,
                presetId = "deep_lofi",
                presetName = "Deep Lo-Fi",
                masterIntensity = 0.76f,
                lowPassCutoffHz = 6200f,
                highPassCutoffHz = 32f,
                bitDepth = 16.0f,
                sampleRateDivisor = 1.0f,
                analogSaturation = 0.22f,
                vinylCrackle = 0.06f,
                vinylNoise = 0.08f,
                tapeHiss = 0.03f,
                wowFlutterDepth = 0.12f,
                wowFlutterRateHz = 0.85f,
                warmBassBoostDb = 2.6f,
                softDistortion = 0.0f,
                reverbMix = 0.16f,
                reverbRoomSize = 0.62f,
                stereoWidth = 1.08f,
                softCompression = 0.14f,
                highFreqReductionDb = 1.8f,
                trebleGainDb = -1.2f,
                playbackSpeed = 1.0f,
                masterGainDb = 0.0f
            )
        ),
        // 4. Vinyl Classic – subtle vinyl texture
        LofiPreset(
            id = "vinyl_classic",
            name = "Vinyl Classic",
            subtitle = "Subtle 33⅓ RPM phono warmth, gentle dust crackle & natural stereo groove",
            moodCategory = "Nostalgic Analog",
            parameters = LofiDspParameters(
                isLiveLofiEnabled = true,
                presetId = "vinyl_classic",
                presetName = "Vinyl Classic",
                masterIntensity = 0.75f,
                lowPassCutoffHz = 7200f,
                highPassCutoffHz = 42f,
                bitDepth = 16.0f,
                sampleRateDivisor = 1.0f,
                analogSaturation = 0.22f,
                vinylCrackle = 0.16f,
                vinylNoise = 0.14f,
                tapeHiss = 0.02f,
                wowFlutterDepth = 0.12f,
                wowFlutterRateHz = 1.10f,
                warmBassBoostDb = 1.8f,
                softDistortion = 0.02f,
                reverbMix = 0.10f,
                reverbRoomSize = 0.52f,
                stereoWidth = 0.98f,
                softCompression = 0.12f,
                highFreqReductionDb = 1.5f,
                trebleGainDb = -1.0f,
                playbackSpeed = 1.0f,
                masterGainDb = 0.0f
            )
        ),
        // 5. Rainy Night – soft atmospheric sound
        LofiPreset(
            id = "rainy_night",
            name = "Rainy Night",
            subtitle = "Soft atmospheric room reflections, warm electric piano tone & delicate vinyl air",
            moodCategory = "Chill & Rainy",
            parameters = LofiDspParameters(
                isLiveLofiEnabled = true,
                presetId = "rainy_night",
                presetName = "Rainy Night",
                masterIntensity = 0.76f,
                lowPassCutoffHz = 6500f,
                highPassCutoffHz = 35f,
                bitDepth = 16.0f,
                sampleRateDivisor = 1.0f,
                analogSaturation = 0.20f,
                vinylCrackle = 0.11f,
                vinylNoise = 0.12f,
                tapeHiss = 0.04f,
                wowFlutterDepth = 0.10f,
                wowFlutterRateHz = 1.00f,
                warmBassBoostDb = 2.0f,
                softDistortion = 0.0f,
                reverbMix = 0.22f,
                reverbRoomSize = 0.68f,
                stereoWidth = 1.12f,
                softCompression = 0.12f,
                highFreqReductionDb = 1.8f,
                trebleGainDb = -1.2f,
                playbackSpeed = 1.0f,
                masterGainDb = 0.0f
            )
        ),
        // 6. Dreamy Lo-Fi – spacious and smooth
        LofiPreset(
            id = "dreamy_lofi",
            name = "Dreamy Lo-Fi",
            subtitle = "Spacious stereo stage, smooth velvet room reverb & gentle analog chorus drift",
            moodCategory = "Dreamy & Ethereal",
            parameters = LofiDspParameters(
                isLiveLofiEnabled = true,
                presetId = "dreamy_lofi",
                presetName = "Dreamy Lo-Fi",
                masterIntensity = 0.75f,
                lowPassCutoffHz = 7600f,
                highPassCutoffHz = 30f,
                bitDepth = 16.0f,
                sampleRateDivisor = 1.0f,
                analogSaturation = 0.16f,
                vinylCrackle = 0.05f,
                vinylNoise = 0.06f,
                tapeHiss = 0.04f,
                wowFlutterDepth = 0.16f,
                wowFlutterRateHz = 0.90f,
                warmBassBoostDb = 1.6f,
                softDistortion = 0.0f,
                reverbMix = 0.26f,
                reverbRoomSize = 0.74f,
                stereoWidth = 1.18f,
                softCompression = 0.10f,
                highFreqReductionDb = 1.4f,
                trebleGainDb = -0.8f,
                playbackSpeed = 1.0f,
                masterGainDb = 0.0f
            )
        ),
        // 7. Cassette Tape – gentle tape character
        LofiPreset(
            id = "cassette_tape",
            name = "Cassette Tape",
            subtitle = "Gentle magnetic tape saturation, subtle capstan flutter & warm mid-band focus",
            moodCategory = "Tape & Retro",
            parameters = LofiDspParameters(
                isLiveLofiEnabled = true,
                presetId = "cassette_tape",
                presetName = "Cassette Tape",
                masterIntensity = 0.76f,
                lowPassCutoffHz = 6600f,
                highPassCutoffHz = 48f,
                bitDepth = 16.0f,
                sampleRateDivisor = 1.0f,
                analogSaturation = 0.26f,
                vinylCrackle = 0.02f,
                vinylNoise = 0.04f,
                tapeHiss = 0.10f,
                wowFlutterDepth = 0.18f,
                wowFlutterRateHz = 1.25f,
                warmBassBoostDb = 1.9f,
                softDistortion = 0.03f,
                reverbMix = 0.12f,
                reverbRoomSize = 0.56f,
                stereoWidth = 0.98f,
                softCompression = 0.15f,
                highFreqReductionDb = 1.8f,
                trebleGainDb = -1.2f,
                playbackSpeed = 1.0f,
                masterGainDb = 0.0f
            )
        ),
        // Additional studio presets (re-calibrated for zero clipping and natural vocal clarity)
        LofiPreset(
            id = "deep_chill",
            name = "Deep Chill",
            subtitle = "Smooth sub-warmth, wide stereo image & relaxed high-frequency roll-off",
            moodCategory = "Relax & Unwind",
            parameters = LofiDspParameters(
                isLiveLofiEnabled = true,
                presetId = "deep_chill",
                presetName = "Deep Chill",
                masterIntensity = 0.74f,
                lowPassCutoffHz = 6000f,
                highPassCutoffHz = 30f,
                bitDepth = 16.0f,
                sampleRateDivisor = 1.0f,
                analogSaturation = 0.20f,
                vinylCrackle = 0.05f,
                vinylNoise = 0.07f,
                tapeHiss = 0.03f,
                wowFlutterDepth = 0.12f,
                wowFlutterRateHz = 0.85f,
                warmBassBoostDb = 2.5f,
                softDistortion = 0.0f,
                reverbMix = 0.20f,
                reverbRoomSize = 0.68f,
                stereoWidth = 1.14f,
                softCompression = 0.12f,
                highFreqReductionDb = 1.8f,
                trebleGainDb = -1.2f,
                playbackSpeed = 1.0f
            )
        ),
        LofiPreset(
            id = "vintage_vinyl",
            name = "Vintage Vinyl",
            subtitle = "Classic turntable warmth with balanced needle texture and clear vocal midrange",
            moodCategory = "Nostalgic Analog",
            parameters = LofiDspParameters(
                isLiveLofiEnabled = true,
                presetId = "vintage_vinyl",
                presetName = "Vintage Vinyl",
                masterIntensity = 0.76f,
                lowPassCutoffHz = 6800f,
                highPassCutoffHz = 45f,
                bitDepth = 16.0f,
                sampleRateDivisor = 1.0f,
                analogSaturation = 0.24f,
                vinylCrackle = 0.18f,
                vinylNoise = 0.15f,
                tapeHiss = 0.02f,
                wowFlutterDepth = 0.14f,
                wowFlutterRateHz = 1.15f,
                warmBassBoostDb = 1.8f,
                softDistortion = 0.02f,
                reverbMix = 0.12f,
                reverbRoomSize = 0.54f,
                stereoWidth = 0.96f,
                softCompression = 0.14f,
                highFreqReductionDb = 1.6f,
                trebleGainDb = -1.0f,
                playbackSpeed = 1.0f
            )
        ),
        LofiPreset(
            id = "warm_cassette",
            name = "Warm Cassette",
            subtitle = "Rich analog tape harmonics, gentle tape hiss & smooth dynamic glue",
            moodCategory = "Tape & Retro",
            parameters = LofiDspParameters(
                isLiveLofiEnabled = true,
                presetId = "warm_cassette",
                presetName = "Warm Cassette",
                masterIntensity = 0.76f,
                lowPassCutoffHz = 6400f,
                highPassCutoffHz = 48f,
                bitDepth = 16.0f,
                sampleRateDivisor = 1.0f,
                analogSaturation = 0.28f,
                vinylCrackle = 0.03f,
                vinylNoise = 0.04f,
                tapeHiss = 0.11f,
                wowFlutterDepth = 0.18f,
                wowFlutterRateHz = 1.25f,
                warmBassBoostDb = 2.0f,
                softDistortion = 0.03f,
                reverbMix = 0.14f,
                reverbRoomSize = 0.58f,
                stereoWidth = 0.98f,
                softCompression = 0.16f,
                highFreqReductionDb = 1.8f,
                trebleGainDb = -1.2f,
                playbackSpeed = 1.0f
            )
        ),
        LofiPreset(
            id = "study_focus",
            name = "Study Focus",
            subtitle = "Fatigue-free treble smoothing, articulate vocals & subtle analog warmth",
            moodCategory = "Study & Reading",
            parameters = LofiDspParameters(
                isLiveLofiEnabled = true,
                presetId = "study_focus",
                presetName = "Study Focus",
                masterIntensity = 0.68f,
                lowPassCutoffHz = 7800f,
                highPassCutoffHz = 35f,
                bitDepth = 16.0f,
                sampleRateDivisor = 1.0f,
                analogSaturation = 0.15f,
                vinylCrackle = 0.05f,
                vinylNoise = 0.05f,
                tapeHiss = 0.02f,
                wowFlutterDepth = 0.06f,
                wowFlutterRateHz = 0.90f,
                warmBassBoostDb = 1.4f,
                softDistortion = 0.0f,
                reverbMix = 0.14f,
                reverbRoomSize = 0.58f,
                stereoWidth = 1.04f,
                softCompression = 0.12f,
                highFreqReductionDb = 1.2f,
                trebleGainDb = -0.8f,
                playbackSpeed = 1.0f
            )
        ),
        LofiPreset(
            id = "dark_academia",
            name = "Dark Academia",
            subtitle = "Intimate wooden room acoustics, warm midrange presence & light vinyl air",
            moodCategory = "Study & Reading",
            parameters = LofiDspParameters(
                isLiveLofiEnabled = true,
                presetId = "dark_academia",
                presetName = "Dark Academia",
                masterIntensity = 0.74f,
                lowPassCutoffHz = 6200f,
                highPassCutoffHz = 45f,
                bitDepth = 16.0f,
                sampleRateDivisor = 1.0f,
                analogSaturation = 0.22f,
                vinylCrackle = 0.12f,
                vinylNoise = 0.10f,
                tapeHiss = 0.04f,
                wowFlutterDepth = 0.12f,
                wowFlutterRateHz = 1.05f,
                warmBassBoostDb = 1.6f,
                softDistortion = 0.02f,
                reverbMix = 0.20f,
                reverbRoomSize = 0.66f,
                stereoWidth = 1.02f,
                softCompression = 0.12f,
                highFreqReductionDb = 1.8f,
                trebleGainDb = -1.2f,
                playbackSpeed = 1.0f
            )
        ),
        LofiPreset(
            id = "sleep_relax",
            name = "Sleep Relax",
            subtitle = "Soothing 4.8kHz warm filter, gentle room ambience & calm dynamics",
            moodCategory = "Sleep & Calm",
            parameters = LofiDspParameters(
                isLiveLofiEnabled = true,
                presetId = "sleep_relax",
                presetName = "Sleep Relax",
                masterIntensity = 0.78f,
                lowPassCutoffHz = 4800f,
                highPassCutoffHz = 30f,
                bitDepth = 16.0f,
                sampleRateDivisor = 1.0f,
                analogSaturation = 0.16f,
                vinylCrackle = 0.04f,
                vinylNoise = 0.08f,
                tapeHiss = 0.04f,
                wowFlutterDepth = 0.10f,
                wowFlutterRateHz = 0.75f,
                warmBassBoostDb = 1.8f,
                softDistortion = 0.0f,
                reverbMix = 0.24f,
                reverbRoomSize = 0.72f,
                stereoWidth = 1.10f,
                softCompression = 0.14f,
                highFreqReductionDb = 2.4f,
                trebleGainDb = -1.8f,
                playbackSpeed = 1.0f
            )
        ),
        LofiPreset(
            id = "coffee_shop",
            name = "Coffee Shop",
            subtitle = "Cozy acoustic room warmth, natural vocal clarity & light vinyl texture",
            moodCategory = "Cozy & Social",
            parameters = LofiDspParameters(
                isLiveLofiEnabled = true,
                presetId = "coffee_shop",
                presetName = "Coffee Shop",
                masterIntensity = 0.70f,
                lowPassCutoffHz = 7800f,
                highPassCutoffHz = 45f,
                bitDepth = 16.0f,
                sampleRateDivisor = 1.0f,
                analogSaturation = 0.18f,
                vinylCrackle = 0.08f,
                vinylNoise = 0.08f,
                tapeHiss = 0.03f,
                wowFlutterDepth = 0.08f,
                wowFlutterRateHz = 1.00f,
                warmBassBoostDb = 1.5f,
                softDistortion = 0.0f,
                reverbMix = 0.18f,
                reverbRoomSize = 0.60f,
                stereoWidth = 1.05f,
                softCompression = 0.12f,
                highFreqReductionDb = 1.2f,
                trebleGainDb = -0.8f,
                playbackSpeed = 1.0f
            )
        ),
        LofiPreset(
            id = "anime_memories",
            name = "Anime Memories",
            subtitle = "Anti-aliased 13-bit vintage sampler warmth, gentle tape drift & sunset reverb",
            moodCategory = "Nostalgic Analog",
            parameters = LofiDspParameters(
                isLiveLofiEnabled = true,
                presetId = "anime_memories",
                presetName = "Anime Memories",
                masterIntensity = 0.75f,
                lowPassCutoffHz = 6800f,
                highPassCutoffHz = 38f,
                bitDepth = 13.0f,
                sampleRateDivisor = 1.4f,
                analogSaturation = 0.22f,
                vinylCrackle = 0.10f,
                vinylNoise = 0.08f,
                tapeHiss = 0.05f,
                wowFlutterDepth = 0.14f,
                wowFlutterRateHz = 1.15f,
                warmBassBoostDb = 1.8f,
                softDistortion = 0.02f,
                reverbMix = 0.20f,
                reverbRoomSize = 0.65f,
                stereoWidth = 1.10f,
                softCompression = 0.14f,
                highFreqReductionDb = 1.5f,
                trebleGainDb = -1.0f,
                playbackSpeed = 1.0f
            )
        ),
        LofiPreset(
            id = "midnight_drive",
            name = "Midnight Drive",
            subtitle = "Balanced low-end punch, wide stereo stage & smooth night highway reverb",
            moodCategory = "Late Night",
            parameters = LofiDspParameters(
                isLiveLofiEnabled = true,
                presetId = "midnight_drive",
                presetName = "Midnight Drive",
                masterIntensity = 0.76f,
                lowPassCutoffHz = 7500f,
                highPassCutoffHz = 30f,
                bitDepth = 16.0f,
                sampleRateDivisor = 1.0f,
                analogSaturation = 0.22f,
                vinylCrackle = 0.05f,
                vinylNoise = 0.06f,
                tapeHiss = 0.04f,
                wowFlutterDepth = 0.10f,
                wowFlutterRateHz = 0.95f,
                warmBassBoostDb = 2.8f,
                softDistortion = 0.02f,
                reverbMix = 0.22f,
                reverbRoomSize = 0.70f,
                stereoWidth = 1.22f,
                softCompression = 0.14f,
                highFreqReductionDb = 1.2f,
                trebleGainDb = -0.8f,
                playbackSpeed = 1.0f
            )
        ),
        LofiPreset(
            id = "emotional_piano",
            name = "Emotional Piano",
            subtitle = "Intimate felt-piano warmth, delicate tape flutter & natural room decay",
            moodCategory = "Emotional & Sad",
            parameters = LofiDspParameters(
                isLiveLofiEnabled = true,
                presetId = "emotional_piano",
                presetName = "Emotional Piano",
                masterIntensity = 0.74f,
                lowPassCutoffHz = 6000f,
                highPassCutoffHz = 35f,
                bitDepth = 16.0f,
                sampleRateDivisor = 1.0f,
                analogSaturation = 0.18f,
                vinylCrackle = 0.08f,
                vinylNoise = 0.08f,
                tapeHiss = 0.05f,
                wowFlutterDepth = 0.14f,
                wowFlutterRateHz = 0.90f,
                warmBassBoostDb = 1.8f,
                softDistortion = 0.0f,
                reverbMix = 0.26f,
                reverbRoomSize = 0.74f,
                stereoWidth = 1.14f,
                softCompression = 0.12f,
                highFreqReductionDb = 1.8f,
                trebleGainDb = -1.2f,
                playbackSpeed = 1.0f
            )
        )
    )

    val AI_MOOD_RECOMMENDATIONS: List<AiMoodRecommendation> = listOf(
        AiMoodRecommendation(
            moodId = "sad_mood",
            moodTitle = "Sad & Melancholic",
            emojiTag = "🌧️",
            description = "Intimate tape warmth with softened high frequencies and natural room reverb.",
            recommendedPresetId = "cassette_tape",
            recommendedPresetName = "Cassette Tape + Soft Reverb",
            dspRecipeSummary = "Cassette Tape + Room Reverb (22%) + Gentle Bass (+2.0dB) + Clear Vocals",
            eqPresetName = "Flat Studio",
            customParameters = ALL.first { it.id == "cassette_tape" }.parameters.copy(
                presetName = "AI: Sad Mood Mix",
                reverbMix = 0.22f,
                reverbRoomSize = 0.68f,
                warmBassBoostDb = 2.0f
            ),
            suggestedTrackKeywords = listOf("Rain", "Piano", "Velvet", "Night", "Memories")
        ),
        AiMoodRecommendation(
            moodId = "deep_focus",
            moodTitle = "Deep Coding & Study",
            emojiTag = "🎧",
            description = "Smooths out harshness while keeping vocals crisp and dynamics natural.",
            recommendedPresetId = "soft_lofi",
            recommendedPresetName = "Soft Lo-Fi + Clean Warmth",
            dspRecipeSummary = "8.8kHz Gentle Low-Pass + Subtle Vinyl (5%) + Controlled Bass (+1.5dB)",
            eqPresetName = "Flat Studio",
            customParameters = ALL.first { it.id == "soft_lofi" }.parameters.copy(
                presetName = "AI: Deep Focus Flow"
            ),
            suggestedTrackKeywords = listOf("Study", "Focus", "Rhodes", "Coffee", "Chill")
        ),
        AiMoodRecommendation(
            moodId = "nostalgic_90s",
            moodTitle = "90s Vinyl Nostalgia",
            emojiTag = "📼",
            description = "Authentic 33⅓ RPM vinyl texture paired with warm analog saturation.",
            recommendedPresetId = "vinyl_classic",
            recommendedPresetName = "Vinyl Classic + Warm Tape",
            dspRecipeSummary = "7.2kHz Low-Pass + Subtle Crackle (16%) + Analog Warmth (22%)",
            eqPresetName = "Flat Studio",
            customParameters = ALL.first { it.id == "vinyl_classic" }.parameters.copy(
                presetName = "AI: 90s Vinyl Vibe"
            ),
            suggestedTrackKeywords = listOf("Sunset", "Tokyo", "Anime", "Vinyl", "Tape")
        ),
        AiMoodRecommendation(
            moodId = "late_night_cruising",
            moodTitle = "2 AM Midnight Drive",
            emojiTag = "🌃",
            description = "Controlled low-end punch (+2.6dB), natural stereo width expansion, and smooth reverb.",
            recommendedPresetId = "deep_lofi",
            recommendedPresetName = "Deep Lo-Fi + Wide Stage",
            dspRecipeSummary = "Controlled Bass (+2.6dB) + Stereo Width (108%) + Room Reverb (16%)",
            eqPresetName = "Flat Studio",
            customParameters = ALL.first { it.id == "deep_lofi" }.parameters.copy(
                presetName = "AI: 2 AM Highway"
            ),
            suggestedTrackKeywords = listOf("Drive", "Midnight", "Sunset", "Neon", "Wave")
        ),
        AiMoodRecommendation(
            moodId = "sleep_dream",
            moodTitle = "Drift to Sleep",
            emojiTag = "🌙",
            description = "Spacious Dreamy Lo-Fi atmosphere with smooth high-frequency roll-off.",
            recommendedPresetId = "dreamy_lofi",
            recommendedPresetName = "Dreamy Lo-Fi + Velvet Air",
            dspRecipeSummary = "7.6kHz Low-Pass + Spacious Reverb (26%) + Stereo Width (118%)",
            eqPresetName = "Flat Studio",
            customParameters = ALL.first { it.id == "dreamy_lofi" }.parameters.copy(
                presetName = "AI: Sleep Sanctuary"
            ),
            suggestedTrackKeywords = listOf("Sleep", "Dream", "Rain", "Calm", "Moon")
        ),
        AiMoodRecommendation(
            moodId = "cozy_rain_cafe",
            moodTitle = "Rainy Tokyo Cafe",
            emojiTag = "☕",
            description = "Warm Rainy Night room reflections blended with subtle vinyl texture.",
            recommendedPresetId = "rainy_night",
            recommendedPresetName = "Rainy Night + Cafe Acoustics",
            dspRecipeSummary = "6.5kHz Low-Pass + Subtle Vinyl (11%) + Warmth (20%) + Room Reverb",
            eqPresetName = "Flat Studio",
            customParameters = ALL.first { it.id == "rainy_night" }.parameters.copy(
                presetName = "AI: Rainy Tokyo Cafe"
            ),
            suggestedTrackKeywords = listOf("Coffee", "Tokyo", "Rain", "Autumn", "Jazz")
        )
    )
}
