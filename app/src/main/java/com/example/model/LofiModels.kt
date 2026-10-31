package com.example.model

import androidx.compose.runtime.Immutable

@Immutable
data class LofiDspParameters(
    val isLiveLofiEnabled: Boolean = true,
    val presetId: String = "rainy_night",
    val presetName: String = "Rainy Night",
    val masterIntensity: Float = 0.85f,
    val lowPassCutoffHz: Float = 3200f,
    val highPassCutoffHz: Float = 90f,
    val bitDepth: Float = 12.5f,
    val sampleRateDivisor: Float = 1.6f,
    val analogSaturation: Float = 0.38f,
    val vinylCrackle: Float = 0.34f,
    val vinylNoise: Float = 0.26f,
    val tapeHiss: Float = 0.16f,
    val wowFlutterDepth: Float = 0.35f,
    val wowFlutterRateHz: Float = 1.35f,
    val warmBassBoostDb: Float = 4.5f,
    val softDistortion: Float = 0.14f,
    val reverbMix: Float = 0.34f,
    val reverbRoomSize: Float = 0.76f,
    val stereoWidth: Float = 1.20f,
    val softCompression: Float = 0.42f,
    val highFreqReductionDb: Float = 5.0f,
    val trebleGainDb: Float = -3.0f,
    val playbackSpeed: Float = 0.94f,
    val masterGainDb: Float = 0.0f
)

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
    val isEnabled: Boolean = true,
    val presetName: String = "Lo-Fi Warmth",
    // 10 bands: 31Hz, 62Hz, 125Hz, 250Hz, 500Hz, 1kHz, 2kHz, 4kHz, 8kHz, 16kHz
    val bandGainsDb: List<Float> = listOf(2.5f, 3.5f, 2.0f, 0.5f, -0.5f, -1.0f, -1.5f, -2.5f, -3.5f, -4.0f),
    val bassBoostDb: Float = 3.0f,
    val trebleBoostDb: Float = -2.0f,
    val stereoBalance: Float = 0.0f,
    val volumeNormalization: Boolean = true,
    val smoothTransitions: Boolean = true,
    val fadeInOutMs: Int = 800,
    val gaplessPlayback: Boolean = true
) {
    companion object {
        val BAND_FREQUENCIES = listOf(31, 62, 125, 250, 500, 1000, 2000, 4000, 8000, 16000)
        val BAND_LABELS = listOf("31", "62", "125", "250", "500", "1k", "2k", "4k", "8k", "16k")

        val BUILT_IN_EQ_PRESETS: Map<String, List<Float>> = linkedMapOf(
            "Flat Studio" to listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f),
            "Lo-Fi Warmth" to listOf(2.5f, 3.5f, 2.0f, 0.5f, -0.5f, -1.0f, -1.5f, -2.5f, -3.5f, -4.0f),
            "Deep Sub Bass" to listOf(6.0f, 5.5f, 3.5f, 1.0f, 0.0f, 0.0f, 0.5f, 1.0f, 1.5f, 1.0f),
            "Warm Cassette EQ" to listOf(-2.0f, 2.5f, 4.0f, 2.5f, 1.0f, 0.0f, -2.0f, -4.5f, -6.5f, -8.0f),
            "Vocal & Piano Clarity" to listOf(-1.5f, -0.5f, 0.5f, 2.0f, 3.5f, 4.0f, 3.0f, 1.5f, 0.5f, 0.0f),
            "Late Night Study" to listOf(1.5f, 2.5f, 1.5f, 0.0f, -1.0f, -2.0f, -3.0f, -4.0f, -5.0f, -6.0f)
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
    val gainReductionDb: Float = 0f,
    val isLofiActive: Boolean = true,
    val effectiveSampleRateHz: Int = 44100,
    val activeLowPassHz: Int = 3200,
    val activeBitDepth: Float = 12.5f,
    val bufferLatencyMs: Float = 3.8f
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
        LofiPreset(
            id = "rainy_night",
            name = "Rainy Night",
            subtitle = "Warm muffled Rhodes, cozy vinyl crackle & deep atmospheric room reverb",
            moodCategory = "Chill & Rainy",
            parameters = LofiDspParameters(
                isLiveLofiEnabled = true,
                presetId = "rainy_night",
                presetName = "Rainy Night",
                masterIntensity = 0.86f,
                lowPassCutoffHz = 2900f,
                highPassCutoffHz = 95f,
                bitDepth = 12.5f,
                sampleRateDivisor = 1.6f,
                analogSaturation = 0.40f,
                vinylCrackle = 0.42f,
                vinylNoise = 0.34f,
                tapeHiss = 0.15f,
                wowFlutterDepth = 0.32f,
                wowFlutterRateHz = 1.25f,
                warmBassBoostDb = 5.0f,
                softDistortion = 0.12f,
                reverbMix = 0.38f,
                reverbRoomSize = 0.80f,
                stereoWidth = 1.25f,
                softCompression = 0.45f,
                highFreqReductionDb = 5.5f,
                trebleGainDb = -3.5f,
                playbackSpeed = 0.94f
            )
        ),
        LofiPreset(
            id = "deep_chill",
            name = "Deep Chill",
            subtitle = "Velvet sub-bass, slow pitch drift, wide stereo field & silky high roll-off",
            moodCategory = "Relax & Unwind",
            parameters = LofiDspParameters(
                isLiveLofiEnabled = true,
                presetId = "deep_chill",
                presetName = "Deep Chill",
                masterIntensity = 0.82f,
                lowPassCutoffHz = 2600f,
                highPassCutoffHz = 70f,
                bitDepth = 13.5f,
                sampleRateDivisor = 1.4f,
                analogSaturation = 0.45f,
                vinylCrackle = 0.20f,
                vinylNoise = 0.18f,
                tapeHiss = 0.14f,
                wowFlutterDepth = 0.38f,
                wowFlutterRateHz = 0.95f,
                warmBassBoostDb = 6.5f,
                softDistortion = 0.10f,
                reverbMix = 0.42f,
                reverbRoomSize = 0.84f,
                stereoWidth = 1.38f,
                softCompression = 0.50f,
                highFreqReductionDb = 6.0f,
                trebleGainDb = -4.0f,
                playbackSpeed = 0.90f
            )
        ),
        LofiPreset(
            id = "vintage_vinyl",
            name = "Vintage Vinyl",
            subtitle = "Authentic 1970s turntable dust crackle, needle rumble & analog warmth",
            moodCategory = "Nostalgic Analog",
            parameters = LofiDspParameters(
                isLiveLofiEnabled = true,
                presetId = "vintage_vinyl",
                presetName = "Vintage Vinyl",
                masterIntensity = 0.88f,
                lowPassCutoffHz = 3600f,
                highPassCutoffHz = 125f,
                bitDepth = 12.0f,
                sampleRateDivisor = 1.8f,
                analogSaturation = 0.52f,
                vinylCrackle = 0.56f,
                vinylNoise = 0.44f,
                tapeHiss = 0.08f,
                wowFlutterDepth = 0.44f,
                wowFlutterRateHz = 1.65f,
                warmBassBoostDb = 3.8f,
                softDistortion = 0.22f,
                reverbMix = 0.20f,
                reverbRoomSize = 0.58f,
                stereoWidth = 0.82f,
                softCompression = 0.48f,
                highFreqReductionDb = 4.5f,
                trebleGainDb = -2.5f,
                playbackSpeed = 0.96f
            )
        ),
        LofiPreset(
            id = "warm_cassette",
            name = "Warm Cassette",
            subtitle = "Magnetic tape saturation, noticeable tape hiss, wow/flutter & soft knee compression",
            moodCategory = "Tape & Retro",
            parameters = LofiDspParameters(
                isLiveLofiEnabled = true,
                presetId = "warm_cassette",
                presetName = "Warm Cassette",
                masterIntensity = 0.90f,
                lowPassCutoffHz = 3100f,
                highPassCutoffHz = 140f,
                bitDepth = 11.5f,
                sampleRateDivisor = 2.0f,
                analogSaturation = 0.62f,
                vinylCrackle = 0.10f,
                vinylNoise = 0.12f,
                tapeHiss = 0.46f,
                wowFlutterDepth = 0.54f,
                wowFlutterRateHz = 2.10f,
                warmBassBoostDb = 4.2f,
                softDistortion = 0.26f,
                reverbMix = 0.24f,
                reverbRoomSize = 0.62f,
                stereoWidth = 0.88f,
                softCompression = 0.62f,
                highFreqReductionDb = 5.8f,
                trebleGainDb = -3.8f,
                playbackSpeed = 0.95f
            )
        ),
        LofiPreset(
            id = "dreamy_lofi",
            name = "Dreamy Lo-Fi",
            subtitle = "Lush hall reverb, shimmering wide stereo image & gentle chorus pitch drift",
            moodCategory = "Dreamy & Ethereal",
            parameters = LofiDspParameters(
                isLiveLofiEnabled = true,
                presetId = "dreamy_lofi",
                presetName = "Dreamy Lo-Fi",
                masterIntensity = 0.85f,
                lowPassCutoffHz = 3800f,
                highPassCutoffHz = 80f,
                bitDepth = 13.0f,
                sampleRateDivisor = 1.5f,
                analogSaturation = 0.34f,
                vinylCrackle = 0.24f,
                vinylNoise = 0.18f,
                tapeHiss = 0.20f,
                wowFlutterDepth = 0.48f,
                wowFlutterRateHz = 1.15f,
                warmBassBoostDb = 3.5f,
                softDistortion = 0.10f,
                reverbMix = 0.52f,
                reverbRoomSize = 0.90f,
                stereoWidth = 1.55f,
                softCompression = 0.38f,
                highFreqReductionDb = 4.0f,
                trebleGainDb = -2.0f,
                playbackSpeed = 0.91f
            )
        ),
        LofiPreset(
            id = "dark_academia",
            name = "Dark Academia",
            subtitle = "Candlelit library acoustics, warm midrange focus & vintage gramophone texture",
            moodCategory = "Study & Reading",
            parameters = LofiDspParameters(
                isLiveLofiEnabled = true,
                presetId = "dark_academia",
                presetName = "Dark Academia",
                masterIntensity = 0.84f,
                lowPassCutoffHz = 2750f,
                highPassCutoffHz = 130f,
                bitDepth = 12.0f,
                sampleRateDivisor = 1.9f,
                analogSaturation = 0.46f,
                vinylCrackle = 0.38f,
                vinylNoise = 0.30f,
                tapeHiss = 0.18f,
                wowFlutterDepth = 0.36f,
                wowFlutterRateHz = 1.40f,
                warmBassBoostDb = 3.2f,
                softDistortion = 0.16f,
                reverbMix = 0.40f,
                reverbRoomSize = 0.78f,
                stereoWidth = 1.05f,
                softCompression = 0.44f,
                highFreqReductionDb = 6.2f,
                trebleGainDb = -4.2f,
                playbackSpeed = 0.93f
            )
        ),
        LofiPreset(
            id = "study_focus",
            name = "Study Focus",
            subtitle = "Distraction-free high roll-off, balanced dynamics & subtle analog warmth",
            moodCategory = "Study & Reading",
            parameters = LofiDspParameters(
                isLiveLofiEnabled = true,
                presetId = "study_focus",
                presetName = "Study Focus",
                masterIntensity = 0.75f,
                lowPassCutoffHz = 3400f,
                highPassCutoffHz = 95f,
                bitDepth = 14.0f,
                sampleRateDivisor = 1.3f,
                analogSaturation = 0.28f,
                vinylCrackle = 0.18f,
                vinylNoise = 0.15f,
                tapeHiss = 0.10f,
                wowFlutterDepth = 0.20f,
                wowFlutterRateHz = 1.10f,
                warmBassBoostDb = 3.6f,
                softDistortion = 0.08f,
                reverbMix = 0.26f,
                reverbRoomSize = 0.68f,
                stereoWidth = 1.12f,
                softCompression = 0.55f,
                highFreqReductionDb = 4.8f,
                trebleGainDb = -3.2f,
                playbackSpeed = 0.96f
            )
        ),
        LofiPreset(
            id = "sleep_relax",
            name = "Sleep Relax",
            subtitle = "Ultra-dark 1.8kHz low-pass blanket, gentle ocean-like noise & slow tempo",
            moodCategory = "Sleep & Calm",
            parameters = LofiDspParameters(
                isLiveLofiEnabled = true,
                presetId = "sleep_relax",
                presetName = "Sleep Relax",
                masterIntensity = 0.92f,
                lowPassCutoffHz = 1850f,
                highPassCutoffHz = 65f,
                bitDepth = 13.5f,
                sampleRateDivisor = 1.5f,
                analogSaturation = 0.30f,
                vinylCrackle = 0.14f,
                vinylNoise = 0.28f,
                tapeHiss = 0.22f,
                wowFlutterDepth = 0.28f,
                wowFlutterRateHz = 0.75f,
                warmBassBoostDb = 4.8f,
                softDistortion = 0.06f,
                reverbMix = 0.48f,
                reverbRoomSize = 0.88f,
                stereoWidth = 1.30f,
                softCompression = 0.60f,
                highFreqReductionDb = 8.5f,
                trebleGainDb = -6.5f,
                playbackSpeed = 0.86f
            )
        ),
        LofiPreset(
            id = "coffee_shop",
            name = "Coffee Shop",
            subtitle = "Cozy cafe speaker resonance, warm acoustic presence & light vinyl texture",
            moodCategory = "Cozy & Social",
            parameters = LofiDspParameters(
                isLiveLofiEnabled = true,
                presetId = "coffee_shop",
                presetName = "Coffee Shop",
                masterIntensity = 0.78f,
                lowPassCutoffHz = 4100f,
                highPassCutoffHz = 150f,
                bitDepth = 13.0f,
                sampleRateDivisor = 1.5f,
                analogSaturation = 0.36f,
                vinylCrackle = 0.26f,
                vinylNoise = 0.22f,
                tapeHiss = 0.14f,
                wowFlutterDepth = 0.25f,
                wowFlutterRateHz = 1.30f,
                warmBassBoostDb = 3.0f,
                softDistortion = 0.12f,
                reverbMix = 0.32f,
                reverbRoomSize = 0.65f,
                stereoWidth = 1.08f,
                softCompression = 0.46f,
                highFreqReductionDb = 3.8f,
                trebleGainDb = -2.2f,
                playbackSpeed = 0.97f
            )
        ),
        LofiPreset(
            id = "anime_memories",
            name = "Anime Memories",
            subtitle = "Nostalgic SP-404 bitcrushed sampler crunch, warm tape drift & sunset reverb",
            moodCategory = "Nostalgic Analog",
            parameters = LofiDspParameters(
                isLiveLofiEnabled = true,
                presetId = "anime_memories",
                presetName = "Anime Memories",
                masterIntensity = 0.88f,
                lowPassCutoffHz = 3300f,
                highPassCutoffHz = 105f,
                bitDepth = 10.0f,
                sampleRateDivisor = 2.6f,
                analogSaturation = 0.48f,
                vinylCrackle = 0.34f,
                vinylNoise = 0.24f,
                tapeHiss = 0.25f,
                wowFlutterDepth = 0.42f,
                wowFlutterRateHz = 1.55f,
                warmBassBoostDb = 4.6f,
                softDistortion = 0.20f,
                reverbMix = 0.36f,
                reverbRoomSize = 0.75f,
                stereoWidth = 1.22f,
                softCompression = 0.50f,
                highFreqReductionDb = 5.0f,
                trebleGainDb = -3.0f,
                playbackSpeed = 0.93f
            )
        ),
        LofiPreset(
            id = "midnight_drive",
            name = "Midnight Drive",
            subtitle = "Punchy synthwave sub-bass, wide stereo highway reverb & slowed night groove",
            moodCategory = "Late Night",
            parameters = LofiDspParameters(
                isLiveLofiEnabled = true,
                presetId = "midnight_drive",
                presetName = "Midnight Drive",
                masterIntensity = 0.86f,
                lowPassCutoffHz = 3900f,
                highPassCutoffHz = 60f,
                bitDepth = 12.5f,
                sampleRateDivisor = 1.5f,
                analogSaturation = 0.50f,
                vinylCrackle = 0.16f,
                vinylNoise = 0.15f,
                tapeHiss = 0.20f,
                wowFlutterDepth = 0.30f,
                wowFlutterRateHz = 1.20f,
                warmBassBoostDb = 7.2f,
                softDistortion = 0.18f,
                reverbMix = 0.44f,
                reverbRoomSize = 0.85f,
                stereoWidth = 1.60f,
                softCompression = 0.54f,
                highFreqReductionDb = 4.2f,
                trebleGainDb = -2.4f,
                playbackSpeed = 0.91f
            )
        ),
        LofiPreset(
            id = "emotional_piano",
            name = "Emotional Piano",
            subtitle = "Intimate felt-piano low-pass warmth, delicate tape flutter & concert hall tail",
            moodCategory = "Emotional & Sad",
            parameters = LofiDspParameters(
                isLiveLofiEnabled = true,
                presetId = "emotional_piano",
                presetName = "Emotional Piano",
                masterIntensity = 0.84f,
                lowPassCutoffHz = 2500f,
                highPassCutoffHz = 85f,
                bitDepth = 13.5f,
                sampleRateDivisor = 1.4f,
                analogSaturation = 0.35f,
                vinylCrackle = 0.28f,
                vinylNoise = 0.22f,
                tapeHiss = 0.26f,
                wowFlutterDepth = 0.40f,
                wowFlutterRateHz = 1.05f,
                warmBassBoostDb = 3.8f,
                softDistortion = 0.09f,
                reverbMix = 0.48f,
                reverbRoomSize = 0.86f,
                stereoWidth = 1.32f,
                softCompression = 0.42f,
                highFreqReductionDb = 6.5f,
                trebleGainDb = -4.5f,
                playbackSpeed = 0.92f
            )
        )
    )

    val AI_MOOD_RECOMMENDATIONS: List<AiMoodRecommendation> = listOf(
        AiMoodRecommendation(
            moodId = "sad_mood",
            moodTitle = "Sad & Melancholic",
            emojiTag = "🌧️",
            description = "Intimate tape warmth with softened high frequencies and deep reflective hall reverb.",
            recommendedPresetId = "warm_cassette",
            recommendedPresetName = "Warm Cassette + Soft Reverb",
            dspRecipeSummary = "Warm Cassette + Soft Reverb (42%) + Reduced Highs (-5.8dB) + Gentle Bass (+4.2dB)",
            eqPresetName = "Warm Cassette EQ",
            customParameters = ALL.first { it.id == "warm_cassette" }.parameters.copy(
                presetName = "AI: Sad Mood Mix",
                reverbMix = 0.42f,
                reverbRoomSize = 0.82f,
                highFreqReductionDb = 6.2f,
                warmBassBoostDb = 4.2f,
                playbackSpeed = 0.91f
            ),
            suggestedTrackKeywords = listOf("Rain", "Piano", "Velvet", "Night", "Memories")
        ),
        AiMoodRecommendation(
            moodId = "deep_focus",
            moodTitle = "Deep Coding & Study",
            emojiTag = "🎧",
            description = "Attenuates sharp vocal sibilance (2–6kHz) and stabilizes dynamics for effortless concentration.",
            recommendedPresetId = "study_focus",
            recommendedPresetName = "Study Focus + Soft Compression",
            dspRecipeSummary = "3.2kHz Low-Pass + Soft Compression (58%) + Subtle Vinyl Crackle (18%) + Warm Sub",
            eqPresetName = "Late Night Study",
            customParameters = ALL.first { it.id == "study_focus" }.parameters.copy(
                presetName = "AI: Deep Focus Flow",
                lowPassCutoffHz = 3200f,
                softCompression = 0.58f,
                vinylCrackle = 0.18f
            ),
            suggestedTrackKeywords = listOf("Study", "Focus", "Rhodes", "Coffee", "Chill")
        ),
        AiMoodRecommendation(
            moodId = "nostalgic_90s",
            moodTitle = "90s Anime Nostalgia",
            emojiTag = "📼",
            description = "Classic 10-bit SP-404 sampler crunch paired with vinyl dust and sunset tape flutter.",
            recommendedPresetId = "anime_memories",
            recommendedPresetName = "Anime Memories + 10-Bit Sampler",
            dspRecipeSummary = "Bitcrusher (10-bit / 2.6x) + Vinyl Crackle (36%) + Analog Saturation (50%)",
            eqPresetName = "Lo-Fi Warmth",
            customParameters = ALL.first { it.id == "anime_memories" }.parameters.copy(
                presetName = "AI: 90s Sampler Vibe"
            ),
            suggestedTrackKeywords = listOf("Sunset", "Tokyo", "Anime", "Vinyl", "Tape")
        ),
        AiMoodRecommendation(
            moodId = "late_night_cruising",
            moodTitle = "2 AM Midnight Drive",
            emojiTag = "🌃",
            description = "Boosted 85Hz sub-bass warmth, 160% stereo width expansion, and slowed 0.91x pitch.",
            recommendedPresetId = "midnight_drive",
            recommendedPresetName = "Midnight Drive + Wide Sub",
            dspRecipeSummary = "Warm Bass (+7.2dB) + Stereo Width (160%) + Lush Reverb (44%) + 0.91x Speed",
            eqPresetName = "Deep Sub Bass",
            customParameters = ALL.first { it.id == "midnight_drive" }.parameters.copy(
                presetName = "AI: 2 AM Highway"
            ),
            suggestedTrackKeywords = listOf("Drive", "Midnight", "Sunset", "Neon", "Wave")
        ),
        AiMoodRecommendation(
            moodId = "sleep_dream",
            moodTitle = "Drift to Sleep",
            emojiTag = "🌙",
            description = "Ultra-gentle 1.8kHz low-pass blanket with soothing vinyl noise floor and slow tempo.",
            recommendedPresetId = "sleep_relax",
            recommendedPresetName = "Sleep Relax + Velvet Filter",
            dspRecipeSummary = "1.85kHz Low-Pass + High-Freq Cut (-8.5dB) + Ambient Reverb (48%) + 0.86x Speed",
            eqPresetName = "Late Night Study",
            customParameters = ALL.first { it.id == "sleep_relax" }.parameters.copy(
                presetName = "AI: Sleep Sanctuary"
            ),
            suggestedTrackKeywords = listOf("Sleep", "Dream", "Rain", "Calm", "Moon")
        ),
        AiMoodRecommendation(
            moodId = "cozy_rain_cafe",
            moodTitle = "Rainy Tokyo Cafe",
            emojiTag = "☕",
            description = "Warm vintage vinyl crackle blended with intimate coffee-shop room reflections.",
            recommendedPresetId = "rainy_night",
            recommendedPresetName = "Rainy Night + Cafe Acoustics",
            dspRecipeSummary = "2.9kHz Low-Pass + Vinyl Crackle (42%) + Analog Warmth (40%) + Room Reverb",
            eqPresetName = "Lo-Fi Warmth",
            customParameters = ALL.first { it.id == "rainy_night" }.parameters.copy(
                presetName = "AI: Rainy Tokyo Cafe"
            ),
            suggestedTrackKeywords = listOf("Coffee", "Tokyo", "Rain", "Autumn", "Jazz")
        )
    )
}
