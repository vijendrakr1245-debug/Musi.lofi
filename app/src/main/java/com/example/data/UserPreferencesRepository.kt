package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AudioPlaybackQuality
import com.example.model.EqualizerState
import com.example.model.LiveSoundMode
import com.example.model.LofiDspParameters
import com.example.ui.theme.AccentTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.lofiDataStore: DataStore<Preferences> by preferencesDataStore(name = "lofi_wave_preferences")

data class AppPreferencesState(
    val accentTheme: AccentTheme = AccentTheme.ANIME_LOFI_NIGHT,
    val pureAmoledBlack: Boolean = false,
    val highQualityDecoding: Boolean = true,
    val audioPlaybackQuality: AudioPlaybackQuality = AudioPlaybackQuality.HIGH_QUALITY,
    val lastSelectedPresetId: String = "rainy_night",
    val lastPlayedTrackId: String? = null,
    val lastPlayedPositionMs: Long = 0L,
    val dspParams: LofiDspParameters = LofiDspParameters(),
    val eqState: EqualizerState = EqualizerState()
)

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val ACCENT_THEME = stringPreferencesKey("accent_theme")
        val PURE_AMOLED = booleanPreferencesKey("pure_amoled_black")
        val HIGH_QUALITY_DECODING = booleanPreferencesKey("high_quality_decoding")
        val AUDIO_PLAYBACK_QUALITY = stringPreferencesKey("audio_playback_quality")
        val LAST_PLAYED_TRACK_ID = stringPreferencesKey("last_played_track_id")
        val LAST_PLAYED_POSITION_MS = longPreferencesKey("last_played_position_ms")

        // DSP State Keys
        val IS_LIVE_LOFI = booleanPreferencesKey("is_live_lofi")
        val SOUND_MODE = stringPreferencesKey("sound_mode")
        val PRESET_ID = stringPreferencesKey("preset_id")
        val PRESET_NAME = stringPreferencesKey("preset_name")
        val MASTER_INTENSITY = floatPreferencesKey("master_intensity")
        val MASTER_GAIN_DB = floatPreferencesKey("master_gain_db")
        val LOW_PASS_HZ = floatPreferencesKey("low_pass_hz")
        val HIGH_PASS_HZ = floatPreferencesKey("high_pass_hz")
        val BIT_DEPTH = floatPreferencesKey("bit_depth")
        val SAMPLE_RATE_DIV = floatPreferencesKey("sample_rate_div")
        val ANALOG_SAT = floatPreferencesKey("analog_sat")
        val VINYL_CRACKLE = floatPreferencesKey("vinyl_crackle")
        val VINYL_NOISE = floatPreferencesKey("vinyl_noise")
        val TAPE_HISS = floatPreferencesKey("tape_hiss")
        val WOW_DEPTH = floatPreferencesKey("wow_depth")
        val WOW_RATE = floatPreferencesKey("wow_rate")
        val WARM_BASS_DB = floatPreferencesKey("warm_bass_db")
        val MIDRANGE_GAIN_DB = floatPreferencesKey("midrange_gain_db")
        val SOFT_DIST = floatPreferencesKey("soft_dist")
        val REVERB_MIX = floatPreferencesKey("reverb_mix")
        val REVERB_ROOM = floatPreferencesKey("reverb_room")
        val ECHO_MIX = floatPreferencesKey("echo_mix")
        val ECHO_DELAY_MS = floatPreferencesKey("echo_delay_ms")
        val ECHO_FEEDBACK = floatPreferencesKey("echo_feedback")
        val STEREO_WIDTH = floatPreferencesKey("stereo_width")
        val SPATIAL_INTENSITY = floatPreferencesKey("spatial_intensity")
        val EIGHT_D_SPEED_HZ = floatPreferencesKey("eight_d_speed_hz")
        val SPATIAL_DISTANCE = floatPreferencesKey("spatial_distance")
        val SOFT_COMP = floatPreferencesKey("soft_comp")
        val HIGH_FREQ_RED_DB = floatPreferencesKey("high_freq_red_db")
        val TREBLE_GAIN_DB = floatPreferencesKey("treble_gain_db")
        val PLAYBACK_SPEED = floatPreferencesKey("playback_speed")
        val PITCH_SEMITONES = floatPreferencesKey("pitch_semitones")

        // EQ State Keys
        val EQ_ENABLED = booleanPreferencesKey("eq_enabled")
        val EQ_FIVE_BAND_MODE = booleanPreferencesKey("eq_five_band_mode")
        val EQ_PRESET_NAME = stringPreferencesKey("eq_preset_name")
        val EQ_BANDS_CSV = stringPreferencesKey("eq_bands_csv")
        val EQ_BASS_DB = floatPreferencesKey("eq_bass_db")
        val EQ_MIDRANGE_DB = floatPreferencesKey("eq_midrange_db")
        val EQ_TREBLE_DB = floatPreferencesKey("eq_treble_db")
        val EQ_BALANCE = floatPreferencesKey("eq_balance")
        val EQ_NORMALIZE = booleanPreferencesKey("eq_normalize")
        val EQ_SMOOTH_TRANS = booleanPreferencesKey("eq_smooth_trans")
        val EQ_FADE_MS = intPreferencesKey("eq_fade_ms")
        val EQ_GAPLESS = booleanPreferencesKey("eq_gapless")
    }

    val preferencesFlow: Flow<AppPreferencesState> = context.lofiDataStore.data.map { prefs ->
        val defDsp = LofiDspParameters()
        val defEq = EqualizerState()

        val themeName = prefs[Keys.ACCENT_THEME] ?: AccentTheme.ANIME_LOFI_NIGHT.name
        val accentTheme = runCatching { AccentTheme.valueOf(themeName) }.getOrDefault(AccentTheme.ANIME_LOFI_NIGHT)

        val qualityName = prefs[Keys.AUDIO_PLAYBACK_QUALITY] ?: AudioPlaybackQuality.HIGH_QUALITY.name
        val playbackQuality = runCatching { AudioPlaybackQuality.valueOf(qualityName) }
            .getOrDefault(AudioPlaybackQuality.HIGH_QUALITY)

        val soundModeName = prefs[Keys.SOUND_MODE] ?: defDsp.soundMode.name
        val soundMode = runCatching { LiveSoundMode.valueOf(soundModeName) }.getOrDefault(defDsp.soundMode)

        val bandsCsv = prefs[Keys.EQ_BANDS_CSV]
        val parsedBands = bandsCsv?.split(",")?.mapNotNull { it.toFloatOrNull() }
        val finalBands = if (parsedBands != null && parsedBands.size == 10) parsedBands else defEq.bandGainsDb

        val dsp = LofiDspParameters(
            isLiveLofiEnabled = prefs[Keys.IS_LIVE_LOFI] ?: defDsp.isLiveLofiEnabled,
            soundMode = soundMode,
            presetId = prefs[Keys.PRESET_ID] ?: defDsp.presetId,
            presetName = prefs[Keys.PRESET_NAME] ?: defDsp.presetName,
            masterIntensity = prefs[Keys.MASTER_INTENSITY] ?: defDsp.masterIntensity,
            lowPassCutoffHz = prefs[Keys.LOW_PASS_HZ] ?: defDsp.lowPassCutoffHz,
            highPassCutoffHz = prefs[Keys.HIGH_PASS_HZ] ?: defDsp.highPassCutoffHz,
            bitDepth = prefs[Keys.BIT_DEPTH] ?: defDsp.bitDepth,
            sampleRateDivisor = prefs[Keys.SAMPLE_RATE_DIV] ?: defDsp.sampleRateDivisor,
            analogSaturation = prefs[Keys.ANALOG_SAT] ?: defDsp.analogSaturation,
            vinylCrackle = prefs[Keys.VINYL_CRACKLE] ?: defDsp.vinylCrackle,
            vinylNoise = prefs[Keys.VINYL_NOISE] ?: defDsp.vinylNoise,
            tapeHiss = prefs[Keys.TAPE_HISS] ?: defDsp.tapeHiss,
            wowFlutterDepth = prefs[Keys.WOW_DEPTH] ?: defDsp.wowFlutterDepth,
            wowFlutterRateHz = prefs[Keys.WOW_RATE] ?: defDsp.wowFlutterRateHz,
            warmBassBoostDb = prefs[Keys.WARM_BASS_DB] ?: defDsp.warmBassBoostDb,
            midrangeGainDb = prefs[Keys.MIDRANGE_GAIN_DB] ?: defDsp.midrangeGainDb,
            softDistortion = prefs[Keys.SOFT_DIST] ?: defDsp.softDistortion,
            reverbMix = prefs[Keys.REVERB_MIX] ?: defDsp.reverbMix,
            reverbRoomSize = prefs[Keys.REVERB_ROOM] ?: defDsp.reverbRoomSize,
            echoMix = prefs[Keys.ECHO_MIX] ?: defDsp.echoMix,
            echoDelayMs = prefs[Keys.ECHO_DELAY_MS] ?: defDsp.echoDelayMs,
            echoFeedback = prefs[Keys.ECHO_FEEDBACK] ?: defDsp.echoFeedback,
            stereoWidth = prefs[Keys.STEREO_WIDTH] ?: defDsp.stereoWidth,
            spatialIntensity = prefs[Keys.SPATIAL_INTENSITY] ?: defDsp.spatialIntensity,
            eightDRotationSpeedHz = prefs[Keys.EIGHT_D_SPEED_HZ] ?: defDsp.eightDRotationSpeedHz,
            spatialDistance = prefs[Keys.SPATIAL_DISTANCE] ?: defDsp.spatialDistance,
            softCompression = prefs[Keys.SOFT_COMP] ?: defDsp.softCompression,
            highFreqReductionDb = prefs[Keys.HIGH_FREQ_RED_DB] ?: defDsp.highFreqReductionDb,
            trebleGainDb = prefs[Keys.TREBLE_GAIN_DB] ?: defDsp.trebleGainDb,
            playbackSpeed = prefs[Keys.PLAYBACK_SPEED] ?: defDsp.playbackSpeed,
            pitchSemitones = prefs[Keys.PITCH_SEMITONES] ?: defDsp.pitchSemitones,
            masterGainDb = prefs[Keys.MASTER_GAIN_DB] ?: defDsp.masterGainDb
        )

        val eq = EqualizerState(
            isEnabled = prefs[Keys.EQ_ENABLED] ?: defEq.isEnabled,
            isFiveBandMode = prefs[Keys.EQ_FIVE_BAND_MODE] ?: defEq.isFiveBandMode,
            presetName = prefs[Keys.EQ_PRESET_NAME] ?: defEq.presetName,
            bandGainsDb = finalBands,
            bassBoostDb = prefs[Keys.EQ_BASS_DB] ?: defEq.bassBoostDb,
            midrangeBoostDb = prefs[Keys.EQ_MIDRANGE_DB] ?: defEq.midrangeBoostDb,
            trebleBoostDb = prefs[Keys.EQ_TREBLE_DB] ?: defEq.trebleBoostDb,
            stereoBalance = prefs[Keys.EQ_BALANCE] ?: defEq.stereoBalance,
            volumeNormalization = prefs[Keys.EQ_NORMALIZE] ?: defEq.volumeNormalization,
            smoothTransitions = prefs[Keys.EQ_SMOOTH_TRANS] ?: defEq.smoothTransitions,
            fadeInOutMs = prefs[Keys.EQ_FADE_MS] ?: defEq.fadeInOutMs,
            gaplessPlayback = prefs[Keys.EQ_GAPLESS] ?: defEq.gaplessPlayback
        )

        AppPreferencesState(
            accentTheme = accentTheme,
            pureAmoledBlack = prefs[Keys.PURE_AMOLED] ?: (accentTheme == AccentTheme.AMOLED_BLACK),
            highQualityDecoding = prefs[Keys.HIGH_QUALITY_DECODING] ?: true,
            audioPlaybackQuality = playbackQuality,
            lastSelectedPresetId = dsp.presetId,
            lastPlayedTrackId = prefs[Keys.LAST_PLAYED_TRACK_ID],
            lastPlayedPositionMs = prefs[Keys.LAST_PLAYED_POSITION_MS] ?: 0L,
            dspParams = dsp,
            eqState = eq
        )
    }

    suspend fun saveLastPlayedTrackState(trackId: String, positionMs: Long) {
        context.lofiDataStore.edit { prefs ->
            prefs[Keys.LAST_PLAYED_TRACK_ID] = trackId
            prefs[Keys.LAST_PLAYED_POSITION_MS] = positionMs.coerceAtLeast(0L)
        }
    }

    suspend fun saveDspParameters(params: LofiDspParameters) {
        context.lofiDataStore.edit { prefs ->
            prefs[Keys.IS_LIVE_LOFI] = params.isLiveLofiEnabled
            prefs[Keys.SOUND_MODE] = params.soundMode.name
            prefs[Keys.PRESET_ID] = params.presetId
            prefs[Keys.PRESET_NAME] = params.presetName
            prefs[Keys.MASTER_INTENSITY] = params.masterIntensity
            prefs[Keys.MASTER_GAIN_DB] = params.masterGainDb
            prefs[Keys.LOW_PASS_HZ] = params.lowPassCutoffHz
            prefs[Keys.HIGH_PASS_HZ] = params.highPassCutoffHz
            prefs[Keys.BIT_DEPTH] = params.bitDepth
            prefs[Keys.SAMPLE_RATE_DIV] = params.sampleRateDivisor
            prefs[Keys.ANALOG_SAT] = params.analogSaturation
            prefs[Keys.VINYL_CRACKLE] = params.vinylCrackle
            prefs[Keys.VINYL_NOISE] = params.vinylNoise
            prefs[Keys.TAPE_HISS] = params.tapeHiss
            prefs[Keys.WOW_DEPTH] = params.wowFlutterDepth
            prefs[Keys.WOW_RATE] = params.wowFlutterRateHz
            prefs[Keys.WARM_BASS_DB] = params.warmBassBoostDb
            prefs[Keys.MIDRANGE_GAIN_DB] = params.midrangeGainDb
            prefs[Keys.SOFT_DIST] = params.softDistortion
            prefs[Keys.REVERB_MIX] = params.reverbMix
            prefs[Keys.REVERB_ROOM] = params.reverbRoomSize
            prefs[Keys.ECHO_MIX] = params.echoMix
            prefs[Keys.ECHO_DELAY_MS] = params.echoDelayMs
            prefs[Keys.ECHO_FEEDBACK] = params.echoFeedback
            prefs[Keys.STEREO_WIDTH] = params.stereoWidth
            prefs[Keys.SPATIAL_INTENSITY] = params.spatialIntensity
            prefs[Keys.EIGHT_D_SPEED_HZ] = params.eightDRotationSpeedHz
            prefs[Keys.SPATIAL_DISTANCE] = params.spatialDistance
            prefs[Keys.SOFT_COMP] = params.softCompression
            prefs[Keys.HIGH_FREQ_RED_DB] = params.highFreqReductionDb
            prefs[Keys.TREBLE_GAIN_DB] = params.trebleGainDb
            prefs[Keys.PLAYBACK_SPEED] = params.playbackSpeed
            prefs[Keys.PITCH_SEMITONES] = params.pitchSemitones
        }
    }

    suspend fun saveEqualizerState(eq: EqualizerState) {
        context.lofiDataStore.edit { prefs ->
            prefs[Keys.EQ_ENABLED] = eq.isEnabled
            prefs[Keys.EQ_FIVE_BAND_MODE] = eq.isFiveBandMode
            prefs[Keys.EQ_PRESET_NAME] = eq.presetName
            prefs[Keys.EQ_BANDS_CSV] = eq.bandGainsDb.joinToString(",")
            prefs[Keys.EQ_BASS_DB] = eq.bassBoostDb
            prefs[Keys.EQ_MIDRANGE_DB] = eq.midrangeBoostDb
            prefs[Keys.EQ_TREBLE_DB] = eq.trebleBoostDb
            prefs[Keys.EQ_BALANCE] = eq.stereoBalance
            prefs[Keys.EQ_NORMALIZE] = eq.volumeNormalization
            prefs[Keys.EQ_SMOOTH_TRANS] = eq.smoothTransitions
            prefs[Keys.EQ_FADE_MS] = eq.fadeInOutMs
            prefs[Keys.EQ_GAPLESS] = eq.gaplessPlayback
        }
    }

    suspend fun saveThemePreferences(theme: AccentTheme, pureAmoled: Boolean, highQualityDecoding: Boolean) {
        context.lofiDataStore.edit { prefs ->
            prefs[Keys.ACCENT_THEME] = theme.name
            prefs[Keys.PURE_AMOLED] = pureAmoled
            prefs[Keys.HIGH_QUALITY_DECODING] = highQualityDecoding
        }
    }

    suspend fun saveAudioPlaybackQuality(quality: AudioPlaybackQuality) {
        context.lofiDataStore.edit { prefs ->
            prefs[Keys.AUDIO_PLAYBACK_QUALITY] = quality.name
            prefs[Keys.HIGH_QUALITY_DECODING] = quality != AudioPlaybackQuality.ORIGINAL_QUALITY
        }
    }
}
