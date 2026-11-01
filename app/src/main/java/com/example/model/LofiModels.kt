package com.example.model

import androidx.compose.runtime.Immutable
import kotlin.math.max

/**
 * 10 Real-Time Live Audio Modes (Part 2 – Advanced Live Audio Modes).
 */
enum class LiveSoundMode(
    val id: String,
    val displayName: String,
    val badge: String,
    val subtitle: String,
    val technicalDetail: String
) {
    ORIGINAL_STEREO(
        id = "original_stereo",
        displayName = "Original Stereo",
        badge = "HI-FI STEREO",
        subtitle = "Bit-transparent original stereo audio with zero coloration",
        technicalDetail = "Bypasses Lo-Fi, spatial, and coloration stages while maintaining true-peak ceiling protection"
    ),
    LIVE_LOFI(
        id = "live_lofi",
        displayName = "Live Lo-Fi",
        badge = "ANALOG LO-FI",
        subtitle = "Warm tape saturation, Butterworth low-pass, wow/flutter & vinyl texture",
        technicalDetail = "Full 10-stage analog Lo-Fi DSP pipeline driven by the active Lo-Fi preset"
    ),
    EIGHT_D_ULTRA(
        id = "eight_d_ultra",
        displayName = "8D Ultra",
        badge = "8D ORBIT",
        subtitle = "Continuous 360° stereo orbit, smooth panning, cross-feed & room halo",
        technicalDetail = "Quadrature LFO azimuth panner + interaural delay + rear shadow filter + spatial reverb"
    ),
    THREE_D_SPATIAL(
        id = "three_d_spatial",
        displayName = "3D Spatial Audio",
        badge = "BINAURAL HRTF",
        subtitle = "Spherical-head ITD/ILD + pinna elevation notch binaural headphone stage",
        technicalDetail = "Woodworth spherical-head ITD fractional delay + head-shadow ILD + 7.2kHz pinna notch & concha cues"
    ),
    ULTRA_SURROUND(
        id = "ultra_surround",
        displayName = "Ultra Surround",
        badge = "M/S SURROUND",
        subtitle = "Controlled stereo widening, spatial depth & rock-solid mono-safe center",
        technicalDetail = "High-passed Side (>180Hz) M/S expansion + Haas all-pass decorrelation with mono compatibility"
    ),
    BASS_BOOST(
        id = "bass_boost",
        displayName = "Bass Boost",
        badge = "PUNCH BASS",
        subtitle = "Tight 95Hz low-shelf punch with harmonic warmth & clean vocal midrange",
        technicalDetail = "+4.5 dB 95Hz biquad low-shelf + 2nd-order harmonic exciter + anti-clip headroom trim"
    ),
    DEEP_BASS(
        id = "deep_bass",
        displayName = "Deep Bass",
        badge = "SUB DEPTH",
        subtitle = "Extended 58Hz sub-bass weight with warm analog saturation",
        technicalDetail = "+5.2 dB 58Hz sub-shelf + 110Hz warmth contour + soft-knee bus glue"
    ),
    SOFT_BASS(
        id = "soft_bass",
        displayName = "Soft Bass",
        badge = "VELVET LOWS",
        subtitle = "Smooth, rounded acoustic bass cushion without transient harshness",
        technicalDetail = "+3.0 dB warm low-shelf + gentle transient rounding + silky 9.5kHz top-end"
    ),
    CLEAR_VOCAL(
        id = "clear_vocal",
        displayName = "Clear Vocal",
        badge = "VOCAL FOCUS",
        subtitle = "Articulate 2.4kHz vocal presence, reduced 220Hz mud & airy harmonics",
        technicalDetail = "110Hz high-pass + -2.2 dB 220Hz mud cut + +3.4 dB 2.4kHz vocal presence peak"
    ),
    DREAMY_SOUND(
        id = "dreamy_sound",
        displayName = "Dreamy Sound",
        badge = "ETHEREAL",
        subtitle = "Lush stereo chorus drift, velvet Schroeder reverb & shimmering air",
        technicalDetail = "Dual-tap modulated chorus delay + 74% room size Schroeder reverb + 122% M/S width"
    )
}

@Immutable
data class LofiDspParameters(
    val isLiveLofiEnabled: Boolean = true,
    val soundMode: LiveSoundMode = LiveSoundMode.LIVE_LOFI,
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
    val midrangeGainDb: Float = 0.0f,
    val softDistortion: Float = 0.0f,
    val reverbMix: Float = 0.12f,
    val reverbRoomSize: Float = 0.56f,
    val echoMix: Float = 0.0f,
    val echoDelayMs: Float = 280f,
    val echoFeedback: Float = 0.32f,
    val stereoWidth: Float = 1.05f,
    val spatialIntensity: Float = 0.70f,
    val eightDRotationSpeedHz: Float = 0.14f,
    val spatialDistance: Float = 0.35f,
    val spatialAzimuthDeg: Float = 0.0f,
    val spatialElevationDeg: Float = 15.0f,
    val spatialAutoOrbit: Boolean = true,
    val softCompression: Float = 0.10f,
    val highFreqReductionDb: Float = 1.0f,
    val trebleGainDb: Float = -0.5f,
    val playbackSpeed: Float = 1.0f,
    val pitchSemitones: Float = 0.0f,
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

    companion object {
        /**
         * Builds a dedicated, isolated [LofiDspParameters] configuration for each [LiveSoundMode]
         * so switching modes never leaks unwanted Lo-Fi low-pass muffling, vinyl crackle,
         * or stale playback speed shifts into other modes.
         */
        fun forLiveSoundMode(
            mode: LiveSoundMode,
            masterGainDb: Float = 0f,
            baseLofiPreset: LofiDspParameters? = null
        ): LofiDspParameters {
            return when (mode) {
                LiveSoundMode.ORIGINAL_STEREO -> LofiDspParameters(
                    isLiveLofiEnabled = false,
                    soundMode = LiveSoundMode.ORIGINAL_STEREO,
                    presetId = "original",
                    presetName = "Original Stereo",
                    masterIntensity = 0f,
                    lowPassCutoffHz = 18000f,
                    highPassCutoffHz = 20f,
                    bitDepth = 16f,
                    sampleRateDivisor = 1f,
                    analogSaturation = 0f,
                    vinylCrackle = 0f,
                    vinylNoise = 0f,
                    tapeHiss = 0f,
                    wowFlutterDepth = 0f,
                    warmBassBoostDb = 0f,
                    midrangeGainDb = 0f,
                    softDistortion = 0f,
                    reverbMix = 0f,
                    echoMix = 0f,
                    stereoWidth = 1.0f,
                    softCompression = 0f,
                    highFreqReductionDb = 0f,
                    trebleGainDb = 0f,
                    playbackSpeed = 1.0f,
                    pitchSemitones = 0f,
                    masterGainDb = masterGainDb
                )

                LiveSoundMode.LIVE_LOFI -> {
                    val base = baseLofiPreset?.takeIf {
                        it.presetId != "original" && it.masterIntensity > 0.05f && it.lowPassCutoffHz < 12000f
                    } ?: BuiltInLofiPresets.ALL.first { it.id == "soft_lofi" }.parameters
                    base.copy(
                        isLiveLofiEnabled = true,
                        soundMode = LiveSoundMode.LIVE_LOFI,
                        playbackSpeed = 1.0f,
                        pitchSemitones = 0f,
                        masterGainDb = masterGainDb
                    )
                }

                LiveSoundMode.EIGHT_D_ULTRA -> LofiDspParameters(
                    isLiveLofiEnabled = false,
                    soundMode = LiveSoundMode.EIGHT_D_ULTRA,
                    presetId = "mode_8d_ultra",
                    presetName = "8D Ultra Orbit",
                    masterIntensity = 0.88f,
                    lowPassCutoffHz = 18000f,
                    highPassCutoffHz = 24f,
                    analogSaturation = 0.0f,
                    vinylCrackle = 0.0f,
                    vinylNoise = 0.0f,
                    tapeHiss = 0.0f,
                    wowFlutterDepth = 0.0f,
                    warmBassBoostDb = 1.2f,
                    reverbMix = 0.22f,
                    reverbRoomSize = 0.72f,
                    stereoWidth = 1.32f,
                    spatialIntensity = 0.88f,
                    eightDRotationSpeedHz = 0.18f,
                    highFreqReductionDb = 0.0f,
                    trebleGainDb = 0.5f,
                    playbackSpeed = 1.0f,
                    pitchSemitones = 0f,
                    masterGainDb = masterGainDb
                )

                LiveSoundMode.THREE_D_SPATIAL -> LofiDspParameters(
                    isLiveLofiEnabled = false,
                    soundMode = LiveSoundMode.THREE_D_SPATIAL,
                    presetId = "mode_3d_spatial",
                    presetName = "3D Spatial Audio",
                    masterIntensity = 0.90f,
                    lowPassCutoffHz = 18000f,
                    highPassCutoffHz = 24f,
                    analogSaturation = 0.0f,
                    vinylCrackle = 0.0f,
                    vinylNoise = 0.0f,
                    tapeHiss = 0.0f,
                    wowFlutterDepth = 0.0f,
                    warmBassBoostDb = 0.8f,
                    reverbMix = 0.18f,
                    reverbRoomSize = 0.66f,
                    stereoWidth = 1.24f,
                    spatialIntensity = 0.90f,
                    spatialDistance = 0.35f,
                    spatialElevationDeg = 18.0f,
                    spatialAutoOrbit = true,
                    highFreqReductionDb = 0.0f,
                    trebleGainDb = 0.8f,
                    playbackSpeed = 1.0f,
                    pitchSemitones = 0f,
                    masterGainDb = masterGainDb
                )

                LiveSoundMode.ULTRA_SURROUND -> LofiDspParameters(
                    isLiveLofiEnabled = false,
                    soundMode = LiveSoundMode.ULTRA_SURROUND,
                    presetId = "mode_ultra_surround",
                    presetName = "Ultra Surround",
                    masterIntensity = 0.86f,
                    lowPassCutoffHz = 18000f,
                    highPassCutoffHz = 24f,
                    analogSaturation = 0.0f,
                    vinylCrackle = 0.0f,
                    vinylNoise = 0.0f,
                    tapeHiss = 0.0f,
                    wowFlutterDepth = 0.0f,
                    warmBassBoostDb = 1.4f,
                    reverbMix = 0.14f,
                    reverbRoomSize = 0.62f,
                    stereoWidth = 1.52f,
                    spatialIntensity = 0.86f,
                    highFreqReductionDb = 0.0f,
                    trebleGainDb = 1.0f,
                    playbackSpeed = 1.0f,
                    pitchSemitones = 0f,
                    masterGainDb = masterGainDb
                )

                LiveSoundMode.BASS_BOOST -> LofiDspParameters(
                    isLiveLofiEnabled = false,
                    soundMode = LiveSoundMode.BASS_BOOST,
                    presetId = "mode_bass_boost",
                    presetName = "Bass Boost",
                    masterIntensity = 0.88f,
                    lowPassCutoffHz = 18000f,
                    highPassCutoffHz = 30f,
                    analogSaturation = 0.22f,
                    vinylCrackle = 0.0f,
                    vinylNoise = 0.0f,
                    tapeHiss = 0.0f,
                    wowFlutterDepth = 0.0f,
                    warmBassBoostDb = 5.5f,
                    midrangeGainDb = 0.5f,
                    reverbMix = 0.0f,
                    stereoWidth = 1.04f,
                    softCompression = 0.18f,
                    highFreqReductionDb = 0.0f,
                    trebleGainDb = 0.8f,
                    playbackSpeed = 1.0f,
                    pitchSemitones = 0f,
                    masterGainDb = masterGainDb
                )

                LiveSoundMode.DEEP_BASS -> LofiDspParameters(
                    isLiveLofiEnabled = false,
                    soundMode = LiveSoundMode.DEEP_BASS,
                    presetId = "mode_deep_bass",
                    presetName = "Deep Bass",
                    masterIntensity = 0.90f,
                    lowPassCutoffHz = 17500f,
                    highPassCutoffHz = 22f,
                    analogSaturation = 0.26f,
                    vinylCrackle = 0.0f,
                    vinylNoise = 0.0f,
                    tapeHiss = 0.0f,
                    wowFlutterDepth = 0.0f,
                    warmBassBoostDb = 6.5f,
                    midrangeGainDb = 0.0f,
                    reverbMix = 0.0f,
                    stereoWidth = 1.06f,
                    softCompression = 0.22f,
                    highFreqReductionDb = 0.0f,
                    trebleGainDb = 0.4f,
                    playbackSpeed = 1.0f,
                    pitchSemitones = 0f,
                    masterGainDb = masterGainDb
                )

                LiveSoundMode.SOFT_BASS -> LofiDspParameters(
                    isLiveLofiEnabled = false,
                    soundMode = LiveSoundMode.SOFT_BASS,
                    presetId = "mode_soft_bass",
                    presetName = "Soft Bass",
                    masterIntensity = 0.82f,
                    lowPassCutoffHz = 14000f,
                    highPassCutoffHz = 26f,
                    analogSaturation = 0.14f,
                    vinylCrackle = 0.0f,
                    vinylNoise = 0.0f,
                    tapeHiss = 0.0f,
                    wowFlutterDepth = 0.0f,
                    warmBassBoostDb = 4.2f,
                    reverbMix = 0.06f,
                    reverbRoomSize = 0.52f,
                    stereoWidth = 1.02f,
                    softCompression = 0.14f,
                    highFreqReductionDb = 0.8f,
                    trebleGainDb = -1.0f,
                    playbackSpeed = 1.0f,
                    pitchSemitones = 0f,
                    masterGainDb = masterGainDb
                )

                LiveSoundMode.CLEAR_VOCAL -> LofiDspParameters(
                    isLiveLofiEnabled = false,
                    soundMode = LiveSoundMode.CLEAR_VOCAL,
                    presetId = "mode_clear_vocal",
                    presetName = "Clear Vocal",
                    masterIntensity = 0.88f,
                    lowPassCutoffHz = 18000f,
                    highPassCutoffHz = 110f,
                    analogSaturation = 0.04f,
                    vinylCrackle = 0.0f,
                    vinylNoise = 0.0f,
                    tapeHiss = 0.0f,
                    wowFlutterDepth = 0.0f,
                    warmBassBoostDb = -2.2f,
                    midrangeGainDb = 4.2f,
                    reverbMix = 0.06f,
                    reverbRoomSize = 0.48f,
                    stereoWidth = 0.98f,
                    softCompression = 0.16f,
                    highFreqReductionDb = 0.0f,
                    trebleGainDb = 2.6f,
                    playbackSpeed = 1.0f,
                    pitchSemitones = 0f,
                    masterGainDb = masterGainDb
                )

                LiveSoundMode.DREAMY_SOUND -> LofiDspParameters(
                    isLiveLofiEnabled = false,
                    soundMode = LiveSoundMode.DREAMY_SOUND,
                    presetId = "mode_dreamy_sound",
                    presetName = "Dreamy Sound",
                    masterIntensity = 0.86f,
                    lowPassCutoffHz = 14800f,
                    highPassCutoffHz = 30f,
                    analogSaturation = 0.10f,
                    vinylCrackle = 0.0f,
                    vinylNoise = 0.0f,
                    tapeHiss = 0.0f,
                    wowFlutterDepth = 0.16f,
                    wowFlutterRateHz = 0.85f,
                    warmBassBoostDb = 1.4f,
                    reverbMix = 0.40f,
                    reverbRoomSize = 0.84f,
                    stereoWidth = 1.38f,
                    spatialIntensity = 0.82f,
                    softCompression = 0.10f,
                    highFreqReductionDb = 0.4f,
                    trebleGainDb = 0.8f,
                    playbackSpeed = 1.0f,
                    pitchSemitones = 0f,
                    masterGainDb = masterGainDb
                )
            }
        }
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

/**
 * Audio Playback Quality Modes (Part 14 – Section 6: Audio Quality Settings).
 * Honest, transparent DSP and hardware playback quality options.
 */
enum class AudioPlaybackQuality(
    val label: String,
    val badge: String,
    val description: String
) {
    ORIGINAL_QUALITY(
        label = "Original Quality",
        badge = "NATIVE PCM",
        description = "Bit-transparent native sample rate & original decoded stream without resampling"
    ),
    HIGH_QUALITY(
        label = "High Quality",
        badge = "64-BIT DSP",
        description = "High-headroom 64-bit internal biquad filtering, anti-clipping limiter & smooth transitions"
    ),
    MAXIMUM_AVAILABLE_QUALITY(
        label = "Maximum Available Quality",
        badge = "MAX HARDWARE",
        description = "Maximum device audio output capability with oversampled analog saturation & TPDF dither"
    );

    val title: String get() = label
    val subtitle: String get() = description
}

@Immutable
data class EqualizerState(
    val isEnabled: Boolean = false,
    val presetName: String = "Flat Studio",
    val isFiveBandMode: Boolean = false,
    // 10 bands: 31Hz, 62Hz, 125Hz, 250Hz, 500Hz, 1kHz, 2kHz, 4kHz, 8kHz, 16kHz
    val bandGainsDb: List<Float> = listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f),
    val bassBoostDb: Float = 0.0f,
    val midrangeBoostDb: Float = 0.0f,
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
        val FIVE_BAND_INDICES = listOf(1, 3, 5, 7, 9) // 62Hz, 250Hz, 1kHz, 4kHz, 16kHz

        val BUILT_IN_EQ_PRESETS: Map<String, List<Float>> = linkedMapOf(
            "Flat Studio" to listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f),
            "Rock" to listOf(4.2f, 3.4f, 1.6f, -0.8f, -1.5f, -0.6f, 1.4f, 2.8f, 3.6f, 3.8f),
            "Pop" to listOf(-1.0f, 1.6f, 3.2f, 3.8f, 2.4f, 0.0f, -0.8f, 1.2f, 2.2f, 2.4f),
            "Classical" to listOf(3.2f, 2.6f, 1.8f, 0.6f, -0.6f, -0.6f, 0.0f, 1.6f, 2.6f, 3.2f),
            "Jazz" to listOf(2.8f, 2.2f, 1.0f, 1.6f, -1.2f, -1.2f, 0.0f, 1.4f, 2.4f, 2.8f),
            "Vocal" to listOf(-2.0f, -1.2f, -0.4f, 1.4f, 3.2f, 3.8f, 3.0f, 1.6f, 0.6f, 0.0f),
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
    val isBuiltInSample: Boolean = false,
    val genre: String = "Lo-Fi / Instrumental",
    val sampleRateHz: Int = 44100,
    val bitrateKbps: Int = 1411,
    val trackNumber: Int? = null,
    val channelCount: Int = 2,
    val bitsPerSample: Int = 16
) {
    val sampleRateKhzText: String
        get() = if (sampleRateHz % 1000 == 0) {
            "${sampleRateHz / 1000} kHz"
        } else {
            String.format(java.util.Locale.US, "%.1f kHz", sampleRateHz / 1000.0)
        }

    val technicalMetadataSummary: String
        get() = "$formatBadge • $sampleRateKhzText • $bitrateKbps kbps"
}

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
    CUSTOM_PLAYLISTS("Custom Playlists"),
    CONVERTED_SONGS("Converted Songs")
}

/**
 * Live Visualizer Display Modes (Part 7 – Live Visualizer).
 */
enum class VisualizerMode(val label: String) {
    SPECTRUM_BARS("Spectrum Bars"),
    WAVEFORM("Waveform"),
    CIRCULAR("Circular"),
    THREE_D_STAGE("3D Audio");

    val displayName: String get() = label
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
                soundMode = LiveSoundMode.ORIGINAL_STEREO,
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
        ),
        LofiPreset(
            id = "balanced_lofi",
            name = "Balanced Lo-Fi",
            subtitle = "Articulate rhythmic punch, clear 9.2kHz low-pass & controlled analog warmth for energetic tracks",
            moodCategory = "Energetic & Balanced",
            parameters = LofiDspParameters(
                isLiveLofiEnabled = true,
                presetId = "balanced_lofi",
                presetName = "Balanced Lo-Fi",
                masterIntensity = 0.68f,
                lowPassCutoffHz = 9200f,
                highPassCutoffHz = 30f,
                bitDepth = 16.0f,
                sampleRateDivisor = 1.0f,
                analogSaturation = 0.16f,
                vinylCrackle = 0.04f,
                vinylNoise = 0.05f,
                tapeHiss = 0.02f,
                wowFlutterDepth = 0.06f,
                wowFlutterRateHz = 0.95f,
                warmBassBoostDb = 1.6f,
                softDistortion = 0.0f,
                reverbMix = 0.10f,
                reverbRoomSize = 0.54f,
                stereoWidth = 1.06f,
                softCompression = 0.12f,
                highFreqReductionDb = 0.8f,
                trebleGainDb = -0.4f,
                playbackSpeed = 1.0f
            )
        ),
        LofiPreset(
            id = "soft_ambient",
            name = "Soft Ambient",
            subtitle = "Serene stereo room space, gentle 5.6kHz filter & whisper-quiet vinyl air for peaceful listening",
            moodCategory = "Peaceful & Serene",
            parameters = LofiDspParameters(
                isLiveLofiEnabled = true,
                presetId = "soft_ambient",
                presetName = "Soft Ambient",
                masterIntensity = 0.74f,
                lowPassCutoffHz = 5600f,
                highPassCutoffHz = 28f,
                bitDepth = 16.0f,
                sampleRateDivisor = 1.0f,
                analogSaturation = 0.14f,
                vinylCrackle = 0.04f,
                vinylNoise = 0.06f,
                tapeHiss = 0.03f,
                wowFlutterDepth = 0.09f,
                wowFlutterRateHz = 0.80f,
                warmBassBoostDb = 1.4f,
                softDistortion = 0.0f,
                reverbMix = 0.24f,
                reverbRoomSize = 0.72f,
                stereoWidth = 1.15f,
                softCompression = 0.10f,
                highFreqReductionDb = 1.8f,
                trebleGainDb = -1.2f,
                playbackSpeed = 1.0f
            )
        ),
        LofiPreset(
            id = "warm_lofi",
            name = "Warm Lo-Fi",
            subtitle = "Golden harmonic tape warmth, lively midrange clarity & gentle analog groove for upbeat songs",
            moodCategory = "Happy & Warm",
            parameters = LofiDspParameters(
                isLiveLofiEnabled = true,
                presetId = "warm_lofi",
                presetName = "Warm Lo-Fi",
                masterIntensity = 0.72f,
                lowPassCutoffHz = 8200f,
                highPassCutoffHz = 32f,
                bitDepth = 16.0f,
                sampleRateDivisor = 1.0f,
                analogSaturation = 0.22f,
                vinylCrackle = 0.06f,
                vinylNoise = 0.06f,
                tapeHiss = 0.03f,
                wowFlutterDepth = 0.08f,
                wowFlutterRateHz = 1.00f,
                warmBassBoostDb = 1.7f,
                softDistortion = 0.0f,
                reverbMix = 0.14f,
                reverbRoomSize = 0.58f,
                stereoWidth = 1.08f,
                softCompression = 0.12f,
                highFreqReductionDb = 1.0f,
                trebleGainDb = -0.6f,
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

/**
 * The 7 supported moods for On-Device AI Music Mood Detection.
 * Maps each detected musical mood to a primary and secondary Lo-Fi preset.
 */
enum class DetectedMusicMood(
    val displayName: String,
    val emoji: String,
    val subtitle: String,
    val primaryPresetId: String,
    val primaryPresetLabel: String,
    val secondaryPresetId: String
) {
    HAPPY(
        displayName = "Happy",
        emoji = "😊",
        subtitle = "Bright harmonic energy, uplifting tempo & warm midrange presence",
        primaryPresetId = "warm_lofi",
        primaryPresetLabel = "Warm Lo-Fi",
        secondaryPresetId = "coffee_shop"
    ),
    SAD(
        displayName = "Sad",
        emoji = "💔",
        subtitle = "Slow tempo, subdued high frequencies & intimate dynamic contour",
        primaryPresetId = "rainy_night",
        primaryPresetLabel = "Rainy Night",
        secondaryPresetId = "emotional_piano"
    ),
    ROMANTIC(
        displayName = "Romantic",
        emoji = "❤️",
        subtitle = "Rich harmonic midrange, smooth sustained chords & gentle stereo warmth",
        primaryPresetId = "dreamy_lofi",
        primaryPresetLabel = "Dreamy",
        secondaryPresetId = "vinyl_classic"
    ),
    RELAXING(
        displayName = "Relaxing",
        emoji = "🌙",
        subtitle = "Laid-back tempo, warm low-mid foundation & smooth high-frequency roll-off",
        primaryPresetId = "soft_lofi",
        primaryPresetLabel = "Soft Lo-Fi",
        secondaryPresetId = "deep_chill"
    ),
    ENERGETIC(
        displayName = "Energetic",
        emoji = "⚡",
        subtitle = "Driving rhythmic pulse, crisp transient attacks & high dynamic energy",
        primaryPresetId = "balanced_lofi",
        primaryPresetLabel = "Balanced Lo-Fi",
        secondaryPresetId = "midnight_drive"
    ),
    PEACEFUL(
        displayName = "Peaceful",
        emoji = "🍃",
        subtitle = "Soft transient density, gentle acoustic dynamics & tranquil tonal balance",
        primaryPresetId = "soft_ambient",
        primaryPresetLabel = "Soft Ambient",
        secondaryPresetId = "sleep_relax"
    ),
    DREAMY(
        displayName = "Dreamy",
        emoji = "✨",
        subtitle = "Spacious stereo width, shimmering upper harmonics & floating rhythm",
        primaryPresetId = "dreamy_lofi",
        primaryPresetLabel = "Dreamy Lo-Fi",
        secondaryPresetId = "anime_memories"
    )
}

@Immutable
data class AcousticFeatureSummary(
    val estimatedTempoBpm: Float,
    val rmsEnergyNormalized: Float,
    val spectralBrightnessNormalized: Float,
    val transientDensityNormalized: Float,
    val warmMidRatioNormalized: Float,
    val stereoWidthNormalized: Float,
    val analyzedFramesCount: Int,
    val sampleRateHz: Int,
    val analysisMethodLabel: String
)

@Immutable
data class MusicMoodAnalysisResult(
    val trackId: String,
    val verifiedTitle: String,
    val verifiedArtist: String,
    val verifiedAlbum: String,
    val verifiedFormat: String,
    val verifiedDurationMs: Long,
    val detectedMood: DetectedMusicMood,
    val confidencePercent: Int?,
    val isSignalSufficient: Boolean,
    val moodProbabilities: Map<DetectedMusicMood, Float>,
    val suggestedPreset: LofiPreset,
    val secondaryPreset: LofiPreset,
    val acousticFeatures: AcousticFeatureSummary,
    val acousticReasoning: String,
    val analyzedAtMillis: Long = System.currentTimeMillis(),
    val isSuggestionApplied: Boolean = false,
    val isSuggestionIgnored: Boolean = false
)

// =========================================================================
// PART 3 & PART 10 – MUSIC STYLE CONVERTER STUDIO & EXPORT ENGINE MODELS
// =========================================================================

enum class ConverterCategory(val displayName: String, val badge: String) {
    LOFI("Lo-Fi", "4 Presets"),
    PHONK("Phonk", "4 Presets"),
    BASS("Bass", "5 Presets"),
    OTHER_STYLES("Other Audio Effects", "8 Presets")
}

@Immutable
data class MusicConverterPreset(
    val id: String,
    val name: String,
    val category: ConverterCategory,
    val subtitle: String,
    val dspMethodDescription: String,
    val dspParameters: LofiDspParameters,
    val eqBandGainsDb: List<Float> = List(10) { 0f },
    val cowbellResonanceAmount: Float = 0f,
    val subHarmonicAmount: Float = 0f,
    val badge: String = category.displayName
)

enum class ExportQualityOption(
    val id: String,
    val label: String,
    val formatExtension: String,
    val mimeType: String,
    val sampleRateHz: Int,
    val bitRateKbps: Int,
    val description: String
) {
    WAV_44K_16BIT(
        id = "wav_44k",
        label = "44.1 kHz Studio WAV",
        formatExtension = "wav",
        mimeType = "audio/wav",
        sampleRateHz = 44100,
        bitRateKbps = 1411,
        description = "Uncompressed CD-quality stereo RIFF WAV (1411 kbps)"
    ),
    WAV_48K_16BIT(
        id = "wav_48k",
        label = "48 kHz Hi-Res WAV",
        formatExtension = "wav",
        mimeType = "audio/wav",
        sampleRateHz = 48000,
        bitRateKbps = 1536,
        description = "Uncompressed 48 kHz studio master stereo RIFF WAV (1536 kbps)"
    ),
    WAV_24K_VINTAGE(
        id = "wav_24k",
        label = "24 kHz Vintage WAV",
        formatExtension = "wav",
        mimeType = "audio/wav",
        sampleRateHz = 24000,
        bitRateKbps = 768,
        description = "Authentic sampler-rate stereo WAV with smaller file size (768 kbps)"
    );

    companion object {
        val WAV_44_1_KHZ: ExportQualityOption get() = WAV_44K_16BIT
        val WAV_48_KHZ: ExportQualityOption get() = WAV_48K_16BIT
        val WAV_24_KHZ: ExportQualityOption get() = WAV_24K_VINTAGE
    }
}

@Immutable
data class ExportProgressState(
    val isExporting: Boolean = false,
    val progressFraction: Float = 0f,
    val progressPercent: Int = (progressFraction * 100f).toInt(),
    val statusText: String = "Ready to convert & export",
    val exportedFilePath: String? = null,
    val completedTrack: TrackItem? = null,
    val exportedTrack: TrackItem? = completedTrack,
    val wasCancelled: Boolean = false,
    val errorMessage: String? = null
)

object BuiltInConverterPresets {
    fun byCategory(category: ConverterCategory): List<MusicConverterPreset> =
        ALL.filter { it.category == category }

    val ALL: List<MusicConverterPreset> = listOf(
        // ---------------- LO-FI (4) ----------------
        MusicConverterPreset(
            id = "conv_classic_lofi",
            name = "Classic Lo-Fi",
            category = ConverterCategory.LOFI,
            subtitle = "Warm 7.2kHz low-pass, gentle tape wow & nostalgic vinyl dust",
            dspMethodDescription = "Butterworth LPF (7.2kHz) + Tape Saturation (22%) + Wow/Flutter (12%) + Vinyl Texture",
            dspParameters = LofiDspParameters(
                isLiveLofiEnabled = true,
                soundMode = LiveSoundMode.LIVE_LOFI,
                presetId = "conv_classic_lofi",
                presetName = "Classic Lo-Fi",
                masterIntensity = 0.80f,
                lowPassCutoffHz = 7200f,
                highPassCutoffHz = 38f,
                analogSaturation = 0.22f,
                vinylCrackle = 0.14f,
                vinylNoise = 0.12f,
                tapeHiss = 0.04f,
                wowFlutterDepth = 0.12f,
                warmBassBoostDb = 2.0f,
                reverbMix = 0.16f
            )
        ),
        MusicConverterPreset(
            id = "conv_soft_lofi",
            name = "Soft Lo-Fi",
            category = ConverterCategory.LOFI,
            subtitle = "Clean, velvet analog warmth with articulate vocals & smooth highs",
            dspMethodDescription = "8.6kHz Gentle LPF + Warm Low-Shelf (+1.6dB) + Soft Tape Saturation (16%)",
            dspParameters = LofiDspParameters(
                isLiveLofiEnabled = true,
                soundMode = LiveSoundMode.LIVE_LOFI,
                presetId = "conv_soft_lofi",
                presetName = "Soft Lo-Fi",
                masterIntensity = 0.74f,
                lowPassCutoffHz = 8600f,
                highPassCutoffHz = 28f,
                analogSaturation = 0.16f,
                vinylCrackle = 0.05f,
                vinylNoise = 0.06f,
                wowFlutterDepth = 0.08f,
                warmBassBoostDb = 1.6f,
                reverbMix = 0.12f
            )
        ),
        MusicConverterPreset(
            id = "conv_dreamy_lofi",
            name = "Dreamy Lo-Fi",
            category = ConverterCategory.LOFI,
            subtitle = "Lush stereo chorus drift, spacious velvet reverb & floating pitch",
            dspMethodDescription = "Dreamy Sound Mode + 7.5kHz LPF + Chorus Drift (18%) + 124% Stereo Width",
            dspParameters = LofiDspParameters(
                isLiveLofiEnabled = true,
                soundMode = LiveSoundMode.DREAMY_SOUND,
                presetId = "conv_dreamy_lofi",
                presetName = "Dreamy Lo-Fi",
                masterIntensity = 0.80f,
                lowPassCutoffHz = 7500f,
                analogSaturation = 0.16f,
                wowFlutterDepth = 0.18f,
                warmBassBoostDb = 1.6f,
                reverbMix = 0.30f,
                reverbRoomSize = 0.78f,
                stereoWidth = 1.24f
            )
        ),
        MusicConverterPreset(
            id = "conv_vinyl_lofi",
            name = "Vinyl-style Lo-Fi",
            category = ConverterCategory.LOFI,
            subtitle = "Authentic 33⅓ RPM turntable platter swish, needle crackle & phono warmth",
            dspMethodDescription = "6.6kHz Phono Curve + 33.3 RPM Pink Groove Noise (22%) + Resonant Needle Crackle",
            dspParameters = LofiDspParameters(
                isLiveLofiEnabled = true,
                soundMode = LiveSoundMode.LIVE_LOFI,
                presetId = "conv_vinyl_lofi",
                presetName = "Vinyl-style Lo-Fi",
                masterIntensity = 0.84f,
                lowPassCutoffHz = 6600f,
                highPassCutoffHz = 45f,
                analogSaturation = 0.25f,
                vinylCrackle = 0.24f,
                vinylNoise = 0.22f,
                tapeHiss = 0.04f,
                wowFlutterDepth = 0.14f,
                warmBassBoostDb = 1.8f,
                stereoWidth = 0.96f
            )
        ),

        // ---------------- PHONK (4) ----------------
        MusicConverterPreset(
            id = "conv_drift_phonk",
            name = "Drift Phonk",
            category = ConverterCategory.PHONK,
            subtitle = "Memphis tape grit, 850Hz cowbell transient focus, punchy 62Hz bass & saturation",
            dspMethodDescription = "Harmonic Waveshaping (58%) + 850Hz/1.6kHz Midrange Peaking + +4.5dB Low-End Punch + Bus Compression",
            dspParameters = LofiDspParameters(
                isLiveLofiEnabled = true,
                soundMode = LiveSoundMode.BASS_BOOST,
                presetId = "conv_drift_phonk",
                presetName = "Drift Phonk",
                masterIntensity = 0.88f,
                lowPassCutoffHz = 9500f,
                highPassCutoffHz = 36f,
                bitDepth = 13.5f,
                sampleRateDivisor = 1.25f,
                analogSaturation = 0.58f,
                softDistortion = 0.32f,
                vinylCrackle = 0f,
                vinylNoise = 0f,
                tapeHiss = 0.08f,
                wowFlutterDepth = 0.04f,
                warmBassBoostDb = 4.5f,
                midrangeGainDb = 3.5f,
                softCompression = 0.45f,
                reverbMix = 0.10f,
                stereoWidth = 1.12f
            ),
            eqBandGainsDb = listOf(3.5f, 4.5f, 2.0f, -1.5f, 2.5f, 4.2f, 3.0f, 1.0f, -1.0f, -2.0f),
            cowbellResonanceAmount = 0.45f
        ),
        MusicConverterPreset(
            id = "conv_dark_phonk",
            name = "Dark Phonk",
            category = ConverterCategory.PHONK,
            subtitle = "Pitched-down underground cassette atmosphere, heavy sub-bass & dark filter",
            dspMethodDescription = "0.91x Tape Pitch/Speed + 4.8kHz Dark LPF + Sub-Harmonic Weight + Cassette Saturation",
            dspParameters = LofiDspParameters(
                isLiveLofiEnabled = true,
                soundMode = LiveSoundMode.DEEP_BASS,
                presetId = "conv_dark_phonk",
                presetName = "Dark Phonk",
                masterIntensity = 0.88f,
                lowPassCutoffHz = 4800f,
                highPassCutoffHz = 30f,
                bitDepth = 13.0f,
                sampleRateDivisor = 1.4f,
                analogSaturation = 0.52f,
                softDistortion = 0.28f,
                vinylCrackle = 0f,
                vinylNoise = 0f,
                tapeHiss = 0.12f,
                wowFlutterDepth = 0.16f,
                warmBassBoostDb = 4.8f,
                reverbMix = 0.24f,
                reverbRoomSize = 0.72f,
                playbackSpeed = 0.91f,
                pitchSemitones = -1.6f
            ),
            eqBandGainsDb = listOf(4.8f, 4.5f, 2.5f, 0.0f, 1.5f, 2.2f, -1.5f, -3.5f, -5.0f, -6.0f),
            subHarmonicAmount = 0.40f
        ),
        MusicConverterPreset(
            id = "conv_aggressive_phonk",
            name = "Aggressive Phonk",
            category = ConverterCategory.PHONK,
            subtitle = "High-drive analog overdrive, heavy 808 sub-shelf & sharp midrange presence",
            dspMethodDescription = "Overdrive Saturator (72%) + 11.5-Bit Decimation + +5.5dB Bass Shelf + True-Peak Limiter",
            dspParameters = LofiDspParameters(
                isLiveLofiEnabled = true,
                soundMode = LiveSoundMode.BASS_BOOST,
                presetId = "conv_aggressive_phonk",
                presetName = "Aggressive Phonk",
                masterIntensity = 0.92f,
                lowPassCutoffHz = 10500f,
                highPassCutoffHz = 38f,
                bitDepth = 11.5f,
                sampleRateDivisor = 1.5f,
                analogSaturation = 0.72f,
                softDistortion = 0.48f,
                vinylCrackle = 0f,
                vinylNoise = 0f,
                tapeHiss = 0.04f,
                wowFlutterDepth = 0f,
                warmBassBoostDb = 5.5f,
                midrangeGainDb = 4.2f,
                softCompression = 0.60f,
                reverbMix = 0.06f
            ),
            eqBandGainsDb = listOf(4.5f, 5.5f, 3.0f, -1.0f, 2.8f, 5.0f, 3.8f, 1.5f, -0.5f, -2.0f),
            cowbellResonanceAmount = 0.60f
        ),
        MusicConverterPreset(
            id = "conv_brazilian_phonk",
            name = "Brazilian Phonk",
            category = ConverterCategory.PHONK,
            subtitle = "Aggressive rhythmic transient punch, clipped sub-bass & crisp upper-mid bite",
            dspMethodDescription = "Hard-Knee Transient Clipper + 125Hz Kick Punch (+5.0dB) + 2kHz Bite + 12-Bit Crunch",
            dspParameters = LofiDspParameters(
                isLiveLofiEnabled = true,
                soundMode = LiveSoundMode.BASS_BOOST,
                presetId = "conv_brazilian_phonk",
                presetName = "Brazilian Phonk",
                masterIntensity = 0.90f,
                lowPassCutoffHz = 11000f,
                highPassCutoffHz = 42f,
                bitDepth = 12.5f,
                sampleRateDivisor = 1.35f,
                analogSaturation = 0.65f,
                softDistortion = 0.42f,
                vinylCrackle = 0f,
                vinylNoise = 0f,
                tapeHiss = 0f,
                wowFlutterDepth = 0f,
                warmBassBoostDb = 5.0f,
                midrangeGainDb = 4.0f,
                softCompression = 0.55f,
                reverbMix = 0.05f,
                playbackSpeed = 1.04f
            ),
            eqBandGainsDb = listOf(4.0f, 5.2f, 3.5f, -2.0f, 1.5f, 4.5f, 4.0f, 2.0f, 0.0f, -1.5f),
            cowbellResonanceAmount = 0.55f
        ),

        // ---------------- BASS (5) ----------------
        MusicConverterPreset(
            id = "conv_bass_boost",
            name = "Bass Boost",
            category = ConverterCategory.BASS,
            subtitle = "Clean, punchy +5.5 dB low-end reinforcement with anti-clipping headroom",
            dspMethodDescription = "95Hz Low-Shelf (+5.5dB) + Warm Harmonic Exciter + Clear Vocal Midrange Preservation",
            dspParameters = LofiDspParameters(
                isLiveLofiEnabled = false,
                soundMode = LiveSoundMode.BASS_BOOST,
                presetId = "conv_bass_boost",
                presetName = "Bass Boost",
                masterIntensity = 0.88f,
                lowPassCutoffHz = 18000f,
                highPassCutoffHz = 28f,
                analogSaturation = 0.22f,
                vinylCrackle = 0f,
                vinylNoise = 0f,
                tapeHiss = 0f,
                wowFlutterDepth = 0f,
                warmBassBoostDb = 5.5f,
                reverbMix = 0f,
                softCompression = 0.18f,
                highFreqReductionDb = 0f,
                trebleGainDb = 0.6f
            ),
            eqBandGainsDb = listOf(3.8f, 4.5f, 3.0f, 0.8f, 0.0f, 0.4f, 0.8f, 0.6f, 0.4f, 0.0f)
        ),
        MusicConverterPreset(
            id = "conv_deep_bass",
            name = "Deep Bass",
            category = ConverterCategory.BASS,
            subtitle = "Extended 55Hz sub-bass depth with rich theater low-end contour",
            dspMethodDescription = "55Hz Sub-Shelf (+6.5dB) + Sub-Harmonic Synthesizer (35%) + 250Hz Mud Cut (-1.8dB)",
            dspParameters = LofiDspParameters(
                isLiveLofiEnabled = false,
                soundMode = LiveSoundMode.DEEP_BASS,
                presetId = "conv_deep_bass",
                presetName = "Deep Bass",
                masterIntensity = 0.90f,
                lowPassCutoffHz = 17500f,
                highPassCutoffHz = 22f,
                analogSaturation = 0.26f,
                vinylCrackle = 0f,
                vinylNoise = 0f,
                tapeHiss = 0f,
                wowFlutterDepth = 0f,
                warmBassBoostDb = 6.5f,
                reverbMix = 0f,
                softCompression = 0.22f,
                highFreqReductionDb = 0f,
                trebleGainDb = 0.4f
            ),
            eqBandGainsDb = listOf(5.2f, 4.8f, 2.2f, -1.8f, -0.5f, 0.5f, 1.0f, 0.8f, 0.4f, 0.0f),
            subHarmonicAmount = 0.35f
        ),
        MusicConverterPreset(
            id = "conv_ultra_bass",
            name = "Ultra Bass",
            category = ConverterCategory.BASS,
            subtitle = "Maximum controlled subwoofer slam with dynamic soft-knee peak limiting",
            dspMethodDescription = "+7.5dB Dual Low-Shelf + Sub-Harmonic Exciter (50%) + Active True-Peak Ceiling Limiter",
            dspParameters = LofiDspParameters(
                isLiveLofiEnabled = false,
                soundMode = LiveSoundMode.DEEP_BASS,
                presetId = "conv_ultra_bass",
                presetName = "Ultra Bass",
                masterIntensity = 0.94f,
                lowPassCutoffHz = 17000f,
                highPassCutoffHz = 22f,
                analogSaturation = 0.34f,
                vinylCrackle = 0f,
                vinylNoise = 0f,
                tapeHiss = 0f,
                wowFlutterDepth = 0f,
                warmBassBoostDb = 7.5f,
                reverbMix = 0f,
                softCompression = 0.30f,
                highFreqReductionDb = 0f,
                trebleGainDb = 0.4f
            ),
            eqBandGainsDb = listOf(6.0f, 5.6f, 3.4f, -1.5f, 0.0f, 0.8f, 1.2f, 0.8f, 0.2f, 0.0f),
            subHarmonicAmount = 0.50f
        ),
        MusicConverterPreset(
            id = "conv_punchy_bass",
            name = "Punchy Bass",
            category = ConverterCategory.BASS,
            subtitle = "Tight 90Hz–125Hz kick & bass guitar attack with zero sub-rumble bloat",
            dspMethodDescription = "38Hz Tight High-Pass + +5.2dB 110Hz Peaking Punch + Fast Transient Envelope",
            dspParameters = LofiDspParameters(
                isLiveLofiEnabled = false,
                soundMode = LiveSoundMode.BASS_BOOST,
                presetId = "conv_punchy_bass",
                presetName = "Punchy Bass",
                masterIntensity = 0.86f,
                lowPassCutoffHz = 18000f,
                highPassCutoffHz = 38f,
                analogSaturation = 0.22f,
                vinylCrackle = 0f,
                vinylNoise = 0f,
                tapeHiss = 0f,
                wowFlutterDepth = 0f,
                warmBassBoostDb = 5.2f,
                midrangeGainDb = 1.2f,
                reverbMix = 0f,
                softCompression = 0.20f,
                highFreqReductionDb = 0f,
                trebleGainDb = 0.8f
            ),
            eqBandGainsDb = listOf(1.5f, 4.6f, 4.8f, 1.2f, -0.8f, 0.6f, 1.4f, 1.0f, 0.5f, 0.0f)
        ),
        MusicConverterPreset(
            id = "conv_sub_bass",
            name = "Sub Bass",
            category = ConverterCategory.BASS,
            subtitle = "Focused 31Hz–62Hz sub-octave rumble with clean, uncolored midrange",
            dspMethodDescription = "Octave-Divided Sub-Harmonic Generator (45%) + +6.2dB 31Hz/62Hz Sub Filter",
            dspParameters = LofiDspParameters(
                isLiveLofiEnabled = false,
                soundMode = LiveSoundMode.DEEP_BASS,
                presetId = "conv_sub_bass",
                presetName = "Sub Bass",
                masterIntensity = 0.88f,
                lowPassCutoffHz = 17500f,
                highPassCutoffHz = 20f,
                analogSaturation = 0.20f,
                vinylCrackle = 0f,
                vinylNoise = 0f,
                tapeHiss = 0f,
                wowFlutterDepth = 0f,
                warmBassBoostDb = 6.2f,
                reverbMix = 0f,
                softCompression = 0.20f,
                highFreqReductionDb = 0f,
                trebleGainDb = 0.2f
            ),
            eqBandGainsDb = listOf(5.8f, 4.5f, 1.0f, -1.0f, 0.0f, 0.0f, 0.5f, 0.5f, 0.0f, 0.0f),
            subHarmonicAmount = 0.45f
        ),

        // ---------------- OTHER STYLES (8) ----------------
        MusicConverterPreset(
            id = "conv_slowed_reverb",
            name = "Slowed + Reverb",
            category = ConverterCategory.OTHER_STYLES,
            subtitle = "0.85x analog tape speed & pitch (-2.8 semitones) drenched in lush hall reverb",
            dspMethodDescription = "0.85x Resampled Pitch/Speed + 46% Lush Schroeder Hall Reverb + Pre-Delay + Warmth",
            dspParameters = LofiDspParameters(
                isLiveLofiEnabled = false,
                soundMode = LiveSoundMode.DREAMY_SOUND,
                presetId = "conv_slowed_reverb",
                presetName = "Slowed + Reverb",
                masterIntensity = 0.90f,
                lowPassCutoffHz = 11500f,
                highPassCutoffHz = 26f,
                analogSaturation = 0.14f,
                vinylCrackle = 0f,
                vinylNoise = 0f,
                tapeHiss = 0f,
                wowFlutterDepth = 0.06f,
                warmBassBoostDb = 2.6f,
                reverbMix = 0.46f,
                reverbRoomSize = 0.88f,
                stereoWidth = 1.32f,
                highFreqReductionDb = 0.6f,
                trebleGainDb = -0.4f,
                playbackSpeed = 0.85f,
                pitchSemitones = -2.8f
            )
        ),
        MusicConverterPreset(
            id = "conv_nightcore",
            name = "Nightcore",
            category = ConverterCategory.OTHER_STYLES,
            subtitle = "1.22x energetic tempo & pitch (+3.4 semitones) with crisp high-shelf sparkle",
            dspMethodDescription = "1.22x Up-Sampled Pitch/Tempo + +3.2dB High Sparkle + Punchy 105Hz Low-Shelf",
            dspParameters = LofiDspParameters(
                isLiveLofiEnabled = false,
                soundMode = LiveSoundMode.BASS_BOOST,
                presetId = "conv_nightcore",
                presetName = "Nightcore",
                masterIntensity = 0.88f,
                lowPassCutoffHz = 18000f,
                highPassCutoffHz = 32f,
                analogSaturation = 0.12f,
                vinylCrackle = 0f,
                vinylNoise = 0f,
                tapeHiss = 0f,
                wowFlutterDepth = 0f,
                warmBassBoostDb = 3.2f,
                midrangeGainDb = 1.6f,
                reverbMix = 0.06f,
                trebleGainDb = 3.2f,
                highFreqReductionDb = 0f,
                playbackSpeed = 1.22f,
                pitchSemitones = 3.4f
            ),
            eqBandGainsDb = listOf(2.0f, 3.0f, 1.5f, 0.0f, 0.5f, 1.5f, 2.2f, 2.8f, 3.0f, 2.5f)
        ),
        MusicConverterPreset(
            id = "conv_8d_audio",
            name = "8D Audio",
            category = ConverterCategory.OTHER_STYLES,
            subtitle = "Continuous 360° binaural orbit around your head with spatial room reflections",
            dspMethodDescription = "8D Ultra Quadrature Azimuth Panner (0.18Hz) + ITD Delay + Rear Shadow + Spatial Reverb",
            dspParameters = LofiDspParameters(
                isLiveLofiEnabled = false,
                soundMode = LiveSoundMode.EIGHT_D_ULTRA,
                presetId = "conv_8d_audio",
                presetName = "8D Audio",
                masterIntensity = 0.90f,
                lowPassCutoffHz = 18000f,
                highPassCutoffHz = 24f,
                analogSaturation = 0f,
                vinylCrackle = 0f,
                vinylNoise = 0f,
                tapeHiss = 0f,
                wowFlutterDepth = 0f,
                warmBassBoostDb = 1.2f,
                reverbMix = 0.24f,
                reverbRoomSize = 0.72f,
                spatialIntensity = 0.90f,
                eightDRotationSpeedHz = 0.18f,
                stereoWidth = 1.34f,
                highFreqReductionDb = 0f,
                trebleGainDb = 0.5f
            )
        ),
        MusicConverterPreset(
            id = "conv_3d_spatial",
            name = "3D Spatial Audio",
            category = ConverterCategory.OTHER_STYLES,
            subtitle = "Binaural spherical-head HRTF rendering with elevation & front/rear depth cues",
            dspMethodDescription = "Woodworth Spherical-Head ITD + Head-Shadow ILD + 7.2kHz Pinna Notch + Early Reflections",
            dspParameters = LofiDspParameters(
                isLiveLofiEnabled = false,
                soundMode = LiveSoundMode.THREE_D_SPATIAL,
                presetId = "conv_3d_spatial",
                presetName = "3D Spatial Audio",
                masterIntensity = 0.92f,
                lowPassCutoffHz = 18000f,
                highPassCutoffHz = 24f,
                analogSaturation = 0f,
                vinylCrackle = 0f,
                vinylNoise = 0f,
                tapeHiss = 0f,
                wowFlutterDepth = 0f,
                warmBassBoostDb = 0.8f,
                reverbMix = 0.18f,
                reverbRoomSize = 0.66f,
                spatialIntensity = 0.90f,
                spatialDistance = 0.35f,
                spatialElevationDeg = 18f,
                spatialAutoOrbit = true,
                highFreqReductionDb = 0f,
                trebleGainDb = 0.8f
            )
        ),
        MusicConverterPreset(
            id = "conv_echo",
            name = "Echo",
            category = ConverterCategory.OTHER_STYLES,
            subtitle = "Rhythmic stereo tape delay (310ms) with warm feedback repeats & spacious trail",
            dspMethodDescription = "310ms Cross-Ping-Pong Fractional Delay Line (38% Mix, 42% Damped Feedback) + Room Reverb",
            dspParameters = LofiDspParameters(
                isLiveLofiEnabled = false,
                soundMode = LiveSoundMode.DREAMY_SOUND,
                presetId = "conv_echo",
                presetName = "Echo",
                masterIntensity = 0.88f,
                lowPassCutoffHz = 15500f,
                highPassCutoffHz = 28f,
                analogSaturation = 0.08f,
                vinylCrackle = 0f,
                vinylNoise = 0f,
                tapeHiss = 0f,
                wowFlutterDepth = 0.03f,
                warmBassBoostDb = 1.0f,
                echoMix = 0.42f,
                echoDelayMs = 310f,
                echoFeedback = 0.45f,
                reverbMix = 0.22f,
                reverbRoomSize = 0.68f,
                stereoWidth = 1.24f,
                highFreqReductionDb = 0f,
                trebleGainDb = 0.2f
            )
        ),
        MusicConverterPreset(
            id = "conv_retro_vintage",
            name = "Retro/Vintage",
            category = ConverterCategory.OTHER_STYLES,
            subtitle = "1970s reel-to-reel & AM radio warmth, band-limited 4.6kHz highs & analog saturation",
            dspMethodDescription = "110Hz High-Pass + 4.6kHz Vintage LPF + 12-Bit Sampler Warmth + Tape Flutter + Vinyl Crackle",
            dspParameters = LofiDspParameters(
                isLiveLofiEnabled = true,
                soundMode = LiveSoundMode.LIVE_LOFI,
                presetId = "conv_retro_vintage",
                presetName = "Retro/Vintage",
                masterIntensity = 0.86f,
                lowPassCutoffHz = 4600f,
                highPassCutoffHz = 110f,
                bitDepth = 12.5f,
                sampleRateDivisor = 1.4f,
                analogSaturation = 0.38f,
                vinylCrackle = 0.18f,
                vinylNoise = 0.16f,
                tapeHiss = 0.10f,
                wowFlutterDepth = 0.20f,
                midrangeGainDb = 2.4f,
                stereoWidth = 0.78f
            )
        ),
        MusicConverterPreset(
            id = "conv_treble_boost",
            name = "Treble Boost",
            category = ConverterCategory.OTHER_STYLES,
            subtitle = "Crisp acoustic guitar, hi-hat & air band brilliance (4.5kHz–16kHz)",
            dspMethodDescription = "+4.2dB 4.5kHz High-Shelf + 8kHz/16kHz Air Peaking EQ + Tight Sub High-Pass",
            dspParameters = LofiDspParameters(
                isLiveLofiEnabled = false,
                soundMode = LiveSoundMode.CLEAR_VOCAL,
                presetId = "conv_treble_boost",
                presetName = "Treble Boost",
                masterIntensity = 0.86f,
                lowPassCutoffHz = 18000f,
                highPassCutoffHz = 45f,
                analogSaturation = 0.04f,
                vinylCrackle = 0f,
                vinylNoise = 0f,
                tapeHiss = 0f,
                wowFlutterDepth = 0f,
                warmBassBoostDb = -1.0f,
                reverbMix = 0.04f,
                trebleGainDb = 4.6f,
                highFreqReductionDb = 0f,
                midrangeGainDb = 1.6f
            ),
            eqBandGainsDb = listOf(-1.0f, -0.5f, 0.0f, 0.5f, 1.0f, 1.8f, 2.8f, 3.8f, 4.5f, 4.2f)
        ),
        MusicConverterPreset(
            id = "conv_vocal_focus",
            name = "Vocal Focus",
            category = ConverterCategory.OTHER_STYLES,
            subtitle = "Studio vocal intelligibility with 2.4kHz presence boost & low-mid mud removal",
            dspMethodDescription = "110Hz High-Pass + -3.2dB 220Hz Mud Cut + +4.2dB 1.8kHz–3.2kHz Presence + Gentle Bus Glue",
            dspParameters = LofiDspParameters(
                isLiveLofiEnabled = false,
                soundMode = LiveSoundMode.CLEAR_VOCAL,
                presetId = "conv_vocal_focus",
                presetName = "Vocal Focus",
                masterIntensity = 0.88f,
                lowPassCutoffHz = 18000f,
                highPassCutoffHz = 110f,
                analogSaturation = 0.04f,
                vinylCrackle = 0f,
                vinylNoise = 0f,
                tapeHiss = 0f,
                wowFlutterDepth = 0f,
                warmBassBoostDb = -2.0f,
                midrangeGainDb = 4.2f,
                reverbMix = 0.06f,
                trebleGainDb = 2.0f,
                highFreqReductionDb = 0f,
                softCompression = 0.24f,
                stereoWidth = 0.96f
            ),
            eqBandGainsDb = listOf(-3.0f, -2.0f, -1.5f, -2.2f, 1.5f, 3.6f, 4.0f, 2.8f, 1.2f, 0.5f)
        )
    )
}


