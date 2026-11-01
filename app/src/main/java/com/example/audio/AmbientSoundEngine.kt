package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.example.model.AmbientLayerType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.tanh

/**
 * Real-time procedural stereo Ambient Sound Mixer engine.
 *
 * Generates and mixes 6 continuous stereo ambient soundscapes simultaneously alongside
 * music playback without interrupting ExoPlayer:
 *  - Rain (filtered pink-noise rainfall + stereo droplet patter)
 *  - Ocean (0.11 Hz rhythmic surf swell + stereo shore motion)
 *  - Wind (sweeping resonant air currents)
 *  - Night Ambience (warm nocturnal cricket harmonics + midnight breeze)
 *  - Birds (gentle frequency-modulated forest bird calls in stereo space)
 *  - Thunderstorm (rolling distant low-frequency thunder rumble + rain)
 */
class AmbientSoundEngine {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var playbackJob: Job? = null
    private var audioTrack: AudioTrack? = null

    private val _layerVolumes = MutableStateFlow(
        AmbientLayerType.entries.associateWith { 0f }
    )
    val layerVolumes: StateFlow<Map<AmbientLayerType, Float>> = _layerVolumes.asStateFlow()

    // Deterministic PRNG and filter states for smooth continuous synthesis
    private var rngState: Long = 0x5DEECE66DL
    private var rainLpL = 0f
    private var rainLpR = 0f
    private var oceanLpL = 0f
    private var oceanLpR = 0f
    private var oceanPhase = 0.0
    private var windLpL = 0f
    private var windLpR = 0f
    private var windPhase = 0.0
    private var nightPhase = 0.0
    private var cricketCarrierPhase = 0.0
    private var birdPhase = 0.0
    private var birdCarrierPhase = 0.0
    private var thunderRumbleLp = 0f
    private var thunderEnvelope = 0f
    private var thunderTriggerCooldown = 0

    private fun nextSignedFloat(): Float {
        rngState = (rngState * 25214903917L + 11L) and ((1L shl 48) - 1)
        val bits = (rngState ushr 24).toInt()
        return (bits / 8388608.0f) - 1.0f
    }

    fun setLayerVolume(layer: AmbientLayerType, volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        val updated = _layerVolumes.value.toMutableMap().apply {
            this[layer] = clamped
        }
        _layerVolumes.value = updated
        updatePlaybackState(updated)
    }

    fun setAllLayerVolumes(volumes: Map<AmbientLayerType, Float>) {
        val normalized = AmbientLayerType.entries.associateWith { (volumes[it] ?: 0f).coerceIn(0f, 1f) }
        _layerVolumes.value = normalized
        updatePlaybackState(normalized)
    }

    fun stopAll() {
        val cleared = AmbientLayerType.entries.associateWith { 0f }
        _layerVolumes.value = cleared
        stopAudioLoop()
    }

    fun hasAnyActiveLayer(): Boolean = _layerVolumes.value.values.any { it > 0.005f }

    private fun updatePlaybackState(volumes: Map<AmbientLayerType, Float>) {
        val anyActive = volumes.values.any { it > 0.005f }
        if (anyActive && (playbackJob == null || playbackJob?.isActive != true)) {
            startAudioLoop()
        } else if (!anyActive) {
            stopAudioLoop()
        }
    }

    private fun startAudioLoop() {
        playbackJob?.cancel()
        playbackJob = scope.launch {
            val sampleRate = 44100
            val chunkFrames = 1024
            val minBufBytes = runCatching {
                AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
            }.getOrDefault(8192).coerceAtLeast(chunkFrames * 4 * 2)

            val track = runCatching {
                AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                            .build()
                    )
                    .setBufferSizeInBytes(minBufBytes)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
            }.getOrNull()

            audioTrack = track
            runCatching { track?.play() }

            while (isActive && hasAnyActiveLayer()) {
                val pcmChunk = synthesizeStereoChunk(
                    volumes = _layerVolumes.value,
                    frames = chunkFrames,
                    sampleRate = sampleRate
                )
                val written = runCatching {
                    track?.write(pcmChunk, 0, pcmChunk.size) ?: -1
                }.getOrDefault(-1)
                if (written <= 0) {
                    kotlinx.coroutines.delay(20L)
                }
            }

            runCatching {
                track?.pause()
                track?.flush()
                track?.release()
            }
            if (audioTrack === track) {
                audioTrack = null
            }
        }
    }

    private fun stopAudioLoop() {
        playbackJob?.cancel()
        playbackJob = null
        runCatching {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.release()
        }
        audioTrack = null
    }

    /**
     * Synthesizes [frames] stereo PCM16 frames (length = frames * 2 shorts) for the active
     * ambient sound layers. Safe to call directly in unit tests.
     */
    fun synthesizeStereoChunk(
        volumes: Map<AmbientLayerType, Float>,
        frames: Int,
        sampleRate: Int = 44100
    ): ShortArray {
        val out = ShortArray(frames * 2)
        val rainVol = (volumes[AmbientLayerType.RAIN] ?: 0f).coerceIn(0f, 1f)
        val oceanVol = (volumes[AmbientLayerType.OCEAN] ?: 0f).coerceIn(0f, 1f)
        val windVol = (volumes[AmbientLayerType.WIND] ?: 0f).coerceIn(0f, 1f)
        val nightVol = (volumes[AmbientLayerType.NIGHT_AMBIENCE] ?: 0f).coerceIn(0f, 1f)
        val birdsVol = (volumes[AmbientLayerType.BIRDS] ?: 0f).coerceIn(0f, 1f)
        val thunderVol = (volumes[AmbientLayerType.THUNDERSTORM] ?: 0f).coerceIn(0f, 1f)

        val invSr = 1.0 / sampleRate.toDouble()

        for (i in 0 until frames) {
            var mixL = 0f
            var mixR = 0f

            // 1. RAIN: Warm low-passed pink-like rainfall + fine stereo droplets
            if (rainVol > 0.005f) {
                val nL = nextSignedFloat()
                val nR = nextSignedFloat()
                rainLpL += 0.14f * (nL - rainLpL)
                rainLpR += 0.14f * (nR - rainLpR)
                val droplet = if ((rngState and 0x7FFL) == 0x3FFL) nextSignedFloat() * 0.32f else 0f
                mixL += (rainLpL * 0.22f + droplet) * rainVol
                mixR += (rainLpR * 0.22f - droplet * 0.7f) * rainVol
            }

            // 2. OCEAN: Slow 0.11 Hz wave swell with stereo shore movement
            if (oceanVol > 0.005f) {
                oceanPhase += 2.0 * PI * 0.11 * invSr
                if (oceanPhase > 2.0 * PI) oceanPhase -= 2.0 * PI
                val swell = (0.55f + 0.45f * sin(oceanPhase).toFloat())
                val pan = 0.5f + 0.25f * cos(oceanPhase * 0.5).toFloat()
                val raw = nextSignedFloat()
                oceanLpL += (0.04f + 0.06f * swell) * (raw - oceanLpL)
                oceanLpR += (0.04f + 0.06f * swell) * (nextSignedFloat() - oceanLpR)
                mixL += oceanLpL * swell * pan * 0.30f * oceanVol
                mixR += oceanLpR * swell * (1f - pan * 0.5f) * 0.30f * oceanVol
            }

            // 3. WIND: Sweeping resonant breeze
            if (windVol > 0.005f) {
                windPhase += 2.0 * PI * 0.19 * invSr
                if (windPhase > 2.0 * PI) windPhase -= 2.0 * PI
                val sweep = 0.025f + 0.035f * (0.5f + 0.5f * sin(windPhase).toFloat())
                windLpL += sweep * (nextSignedFloat() - windLpL)
                windLpR += sweep * (nextSignedFloat() - windLpR)
                mixL += windLpL * 0.26f * windVol
                mixR += windLpR * 0.26f * windVol
            }

            // 4. NIGHT AMBIENCE: Gentle nocturnal cricket pulse + soft air
            if (nightVol > 0.005f) {
                nightPhase += 2.0 * PI * 3.8 * invSr
                if (nightPhase > 2.0 * PI) nightPhase -= 2.0 * PI
                cricketCarrierPhase += 2.0 * PI * 4320.0 * invSr
                if (cricketCarrierPhase > 2.0 * PI) cricketCarrierPhase -= 2.0 * PI
                val pulseEnv = sin(nightPhase).toFloat().coerceAtLeast(0f)
                val chirp = sin(cricketCarrierPhase).toFloat() * pulseEnv * pulseEnv * 0.06f
                val air = nextSignedFloat() * 0.015f
                mixL += (chirp * 0.85f + air) * nightVol
                mixR += (chirp * 1.0f + air) * nightVol
            }

            // 5. BIRDS: Periodic melodic FM bird chirps + gentle forest canopy air
            if (birdsVol > 0.005f) {
                birdPhase += 2.0 * PI * 1.65 * invSr
                if (birdPhase > 2.0 * PI) birdPhase -= 2.0 * PI
                val callGate = sin(birdPhase + 1.15).toFloat()
                val canopyAir = nextSignedFloat() * 0.016f
                if (callGate > 0.35f) {
                    val norm = (callGate - 0.35f) / 0.65f
                    val freq = 2350.0 + 950.0 * sin(norm * PI * 3.0)
                    birdCarrierPhase += 2.0 * PI * freq * invSr
                    if (birdCarrierPhase > 2.0 * PI) birdCarrierPhase -= 2.0 * PI
                    val birdSample = sin(birdCarrierPhase).toFloat() * sin(norm * PI).toFloat() * 0.14f
                    mixL += (birdSample * 0.9f + canopyAir) * birdsVol
                    mixR += (birdSample * 0.65f + canopyAir) * birdsVol
                } else {
                    mixL += canopyAir * birdsVol
                    mixR += canopyAir * birdsVol
                }
            }

            // 6. THUNDERSTORM: Rolling distant low-frequency thunder + storm rain
            if (thunderVol > 0.005f) {
                if (thunderTriggerCooldown > 0) {
                    thunderTriggerCooldown--
                } else if ((rngState and 0x3FFFL) == 0x1FFFL) {
                    thunderEnvelope = 0.95f
                    thunderTriggerCooldown = sampleRate * 4
                }
                thunderEnvelope *= 0.99993f
                val rumbleNoise = nextSignedFloat()
                thunderRumbleLp += 0.012f * (rumbleNoise - thunderRumbleLp)
                val stormRain = nextSignedFloat() * 0.08f
                val thunderSig = (thunderRumbleLp * (0.35f + thunderEnvelope * 1.4f) + stormRain) * 0.32f * thunderVol
                mixL += thunderSig
                mixR += thunderSig * 0.96f
            }

            // Soft-clip limiter at -0.5 dBFS to guarantee zero clipping
            val safeL = (tanh(mixL.toDouble()) * 0.92).toFloat().coerceIn(-0.95f, 0.95f)
            val safeR = (tanh(mixR.toDouble()) * 0.92).toFloat().coerceIn(-0.95f, 0.95f)

            out[i * 2] = (safeL * 32767.0f).roundToInt().coerceIn(-32767, 32767).toShort()
            out[i * 2 + 1] = (safeR * 32767.0f).roundToInt().coerceIn(-32767, 32767).toShort()
        }
        return out
    }
}
