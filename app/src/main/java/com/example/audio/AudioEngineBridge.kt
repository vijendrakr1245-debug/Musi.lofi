package com.example.audio

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
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
import com.example.model.HeadphoneRouteStatus
import com.example.model.LiveSoundMode
import com.example.model.LofiDspParameters
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.math.pow

/**
 * Singleton Audio Engine Bridge connecting the Foreground MediaSessionService,
 * the Real-Time LofiDspAudioProcessor, Headphone Route Monitoring, and the UI ViewModel.
 */
@OptIn(UnstableApi::class)
object AudioEngineBridge {

    val dspProcessor = LofiDspAudioProcessor()
    val pcmAudioEngine = AudioEngine()

    private val _headphoneRouteStatus = MutableStateFlow(HeadphoneRouteStatus())
    val headphoneRouteStatus: StateFlow<HeadphoneRouteStatus> = _headphoneRouteStatus.asStateFlow()

    @Volatile
    private var exoPlayerInstance: ExoPlayer? = null

    @Volatile
    private var audioDeviceCallbackRegistered = false

    private var audioManagerRef: AudioManager? = null

    private val audioDeviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
            refreshAudioRouteStatus()
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            refreshAudioRouteStatus()
        }
    }

    fun registerAudioRouteMonitoring(context: Context) {
        val appContext = context.applicationContext
        val am = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        audioManagerRef = am
        if (!audioDeviceCallbackRegistered) {
            runCatching {
                am.registerAudioDeviceCallback(audioDeviceCallback, null)
                audioDeviceCallbackRegistered = true
            }
        }
        refreshAudioRouteStatus()
    }

    fun refreshAudioRouteStatus() {
        val am = audioManagerRef ?: return
        val outputs = runCatching { am.getDevices(AudioManager.GET_DEVICES_OUTPUTS) }.getOrNull() ?: emptyArray()
        var wired = false
        var bluetooth = false
        var activeName = "Device Stereo Speakers"

        for (device in outputs) {
            when (device.type) {
                AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
                AudioDeviceInfo.TYPE_WIRED_HEADSET,
                AudioDeviceInfo.TYPE_USB_HEADSET,
                AudioDeviceInfo.TYPE_USB_DEVICE -> {
                    wired = true
                    val prod = device.productName?.toString()?.takeIf { it.isNotBlank() }
                    activeName = prod ?: "Wired Studio Headphones"
                }
                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> {
                    bluetooth = true
                    val prod = device.productName?.toString()?.takeIf { it.isNotBlank() }
                    activeName = prod ?: "Bluetooth Headphones"
                }
                else -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                        (device.type == AudioDeviceInfo.TYPE_BLE_HEADSET || device.type == AudioDeviceInfo.TYPE_BLE_SPEAKER)
                    ) {
                        bluetooth = true
                        val prod = device.productName?.toString()?.takeIf { it.isNotBlank() }
                        activeName = prod ?: "BLE Audio Headphones"
                    }
                }
            }
        }

        val sampleRate = runCatching {
            am.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)?.toIntOrNull()
        }.getOrNull() ?: 48000

        val framesPerBuffer = runCatching {
            am.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER)?.toIntOrNull()
        }.getOrNull() ?: 256

        val baseHwLatencyMs = ((framesPerBuffer.toFloat() / sampleRate.coerceAtLeast(8000)) * 1000f).coerceIn(4f, 60f)
        val estimatedLatencyMs = when {
            bluetooth -> baseHwLatencyMs + 145f
            wired -> baseHwLatencyMs + 6f
            else -> baseHwLatencyMs + 12f
        }

        _headphoneRouteStatus.value = HeadphoneRouteStatus(
            isHeadphonesConnected = wired || bluetooth,
            isBluetoothAudio = bluetooth,
            isWiredHeadphones = wired,
            routeName = activeName,
            estimatedOutputLatencyMs = estimatedLatencyMs
        )
    }

    @Synchronized
    fun getOrCreatePlayer(context: Context): ExoPlayer {
        registerAudioRouteMonitoring(context)
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

        val isDspOrModeActive = when (params.soundMode) {
            LiveSoundMode.ORIGINAL_STEREO -> false
            LiveSoundMode.LIVE_LOFI -> params.isLiveLofiEnabled &&
                params.presetId != "original" &&
                params.masterIntensity > 0.01f
            else -> params.masterIntensity > 0.01f
        } || abs(params.playbackSpeed - 1.0f) > 0.01f || abs(params.pitchSemitones) > 0.05f

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
            2.0.pow(pitchSemitones.toDouble() / 12.0).toFloat().coerceIn(0.55f, 1.65f)
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
        runCatching {
            if (audioDeviceCallbackRegistered) {
                audioManagerRef?.unregisterAudioDeviceCallback(audioDeviceCallback)
                audioDeviceCallbackRegistered = false
            }
        }
        pcmAudioEngine.stopAndRelease()
        exoPlayerInstance?.release()
        exoPlayerInstance = null
    }
}
