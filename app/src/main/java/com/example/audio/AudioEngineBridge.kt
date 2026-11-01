package com.example.audio

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import com.example.model.AudioPlaybackQuality
import com.example.model.EqualizerState
import com.example.model.LiveSoundMode
import com.example.model.LofiDspParameters
import kotlin.math.abs
import kotlin.math.pow

/**
 * Singleton Audio Engine Bridge connecting the Foreground MediaSessionService,
 * the Real-Time LofiDspAudioProcessor, and the UI ViewModel.
 */
@OptIn(UnstableApi::class)
object AudioEngineBridge {

    val dspProcessor = LofiDspAudioProcessor()
    val pcmAudioEngine = AudioEngine()

    @Volatile
    private var exoPlayerInstance: ExoPlayer? = null

    @Synchronized
    fun getOrCreatePlayer(context: Context): ExoPlayer {
        val existing = exoPlayerInstance
        if (existing != null) return existing

        val appContext = context.applicationContext
        val renderersFactory = object : DefaultRenderersFactory(appContext) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean
            ): AudioSink {
                return DefaultAudioSink.Builder(context)
                    .setAudioProcessors(arrayOf(dspProcessor))
                    .setEnableFloatOutput(false)
                    .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                    .build()
            }
        }

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        val player = ExoPlayer.Builder(appContext, renderersFactory)
            .setAudioAttributes(audioAttributes, true) // Automatic Audio Focus handling
            .setHandleAudioBecomingNoisy(true) // Automatic pause on headphone disconnect
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()

        player.repeatMode = Player.REPEAT_MODE_ALL
        exoPlayerInstance = player
        return player
    }

    fun applyAudioPlaybackQuality(quality: AudioPlaybackQuality) {
        dspProcessor.updatePlaybackQuality(quality)
    }

    fun applyLofiParameters(params: LofiDspParameters) {
        dspProcessor.updateDspParameters(params)
        pcmAudioEngine.setMasterGainDb(params.masterGainDb)
        val player = exoPlayerInstance ?: return

        val isDspOrModeActive = (params.isLiveLofiEnabled && params.presetId != "original") ||
            params.soundMode != LiveSoundMode.ORIGINAL_STEREO
        val rawSpeed = if (isDspOrModeActive) {
            params.playbackSpeed.coerceIn(0.65f, 1.35f)
        } else {
            1.0f
        }
        val effectiveSpeed = if (abs(rawSpeed - 1.0f) < 0.005f) 1.0f else rawSpeed
        val pitchSemitones = if (isDspOrModeActive) params.pitchSemitones.coerceIn(-12f, 12f) else 0f
        val pitchMultiplier = if (abs(pitchSemitones) < 0.05f) {
            effectiveSpeed // Analog tape-locked pitch when no explicit semitone shift is set
        } else {
            (effectiveSpeed * 2.0.pow(pitchSemitones.toDouble() / 12.0).toFloat()).coerceIn(0.55f, 1.65f)
        }

        val targetPlaybackParams = if (effectiveSpeed == 1.0f && abs(pitchMultiplier - 1.0f) < 0.005f) {
            PlaybackParameters.DEFAULT
        } else {
            PlaybackParameters(effectiveSpeed, pitchMultiplier)
        }

        if (player.playbackParameters != targetPlaybackParams) {
            player.playbackParameters = targetPlaybackParams
        }
    }

    fun applyEqualizerState(eqState: EqualizerState) {
        dspProcessor.updateEqualizerState(eqState)
        val bal = eqState.stereoBalance.coerceIn(-1f, 1f)
        val leftGain = if (bal > 0f) 1f - bal else 1f
        val rightGain = if (bal < 0f) 1f + bal else 1f
        pcmAudioEngine.setChannelGains(leftGain, rightGain)
        pcmAudioEngine.setSoftClipEnabled(eqState.volumeNormalization)
        exoPlayerInstance?.pauseAtEndOfMediaItems = !eqState.gaplessPlayback
    }

    @Synchronized
    fun releasePlayer() {
        pcmAudioEngine.stopAndRelease()
        exoPlayerInstance?.release()
        exoPlayerInstance = null
    }
}
