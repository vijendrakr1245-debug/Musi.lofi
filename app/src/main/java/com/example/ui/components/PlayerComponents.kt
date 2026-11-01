package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import coil.compose.AsyncImage
import com.example.R
import com.example.model.LiveSoundMode
import com.example.model.LofiDspParameters
import com.example.model.TrackItem
import com.example.model.VisualizerMode
import com.example.model.VisualizerSnapshot
import com.example.ui.theme.AnalogAmber
import com.example.ui.theme.DeepObsidian
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.ElectricPurpleBright
import com.example.ui.theme.ElevatedCharcoal
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.MidnightCharcoal
import com.example.ui.theme.NeonBlue
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.StudioEmerald
import com.example.ui.theme.SubtleBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VinylRose
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

fun formatDurationMillis(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val min = totalSec / 60L
    val sec = totalSec % 60L
    return String.format(Locale.US, "%02d:%02d", min, sec)
}

/**
 * 1. NEW ANIMATED APP LOGO
 * Modern Headphone + Music Wave inspired logo with:
 * - Glowing Neon Purple, Electric Blue, and Pink gradients
 * - Smooth rotating outer ring
 * - Gentle breathing glow animation
 * - Subtle music wave animation
 */
@Composable
fun AnimatedLofiWaveLogo(
    size: Dp = 46.dp,
    isPlaying: Boolean = true,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "lofi_wave_logo_anim")

    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "logo_ring_rotation"
    )

    val breathingGlow by infiniteTransition.animateFloat(
        initialValue = 0.72f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logo_breathing_glow"
    )

    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI.toFloat()),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isPlaying) 1400 else 3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "logo_wave_phase"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .testTag("animated_lofi_wave_logo")
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height
            val cx = w / 2f
            val cy = h / 2f
            val outerRadius = (minOf(w, h) / 2f) * 0.92f

            // 1. Gentle Breathing Neon Aura Glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        ElectricPurple.copy(alpha = 0.42f * breathingGlow),
                        NeonCyan.copy(alpha = 0.25f * breathingGlow),
                        VinylRose.copy(alpha = 0.14f * breathingGlow),
                        Color.Transparent
                    ),
                    center = Offset(cx, cy),
                    radius = outerRadius * (1.08f * breathingGlow)
                ),
                radius = outerRadius * 1.04f,
                center = Offset(cx, cy)
            )

            // 2. Deep Midnight Purple Core Medallion
            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF0A0618),
                        Color(0xFF1A103C),
                        Color(0xFF080514)
                    )
                ),
                radius = outerRadius * 0.84f,
                center = Offset(cx, cy)
            )

            // 3. Smooth Rotating Outer Ring with Neon Purple, Electric Blue & Pink Sweep Gradient
            rotate(degrees = ringRotation, pivot = Offset(cx, cy)) {
                drawCircle(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            ElectricPurple,
                            NeonCyan,
                            VinylRose,
                            ElectricPurpleBright,
                            NeonBlue,
                            ElectricPurple
                        ),
                        center = Offset(cx, cy)
                    ),
                    radius = outerRadius * 0.88f,
                    center = Offset(cx, cy),
                    style = Stroke(width = (w * 0.065f).coerceAtLeast(2.2f))
                )
            }

            // 4. Modern Headphone Arch
            val archRadius = outerRadius * 0.48f
            val archTopLeft = Offset(cx - archRadius, cy - archRadius * 0.95f)
            drawArc(
                brush = Brush.horizontalGradient(
                    colors = listOf(ElectricPurpleBright, NeonCyan, VinylRose)
                ),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = archTopLeft,
                size = Size(archRadius * 2f, archRadius * 1.85f),
                style = Stroke(
                    width = (w * 0.082f).coerceAtLeast(2.6f),
                    cap = StrokeCap.Round
                )
            )

            // 5. Left & Right Glowing Ear Cups
            val cupW = w * 0.115f
            val cupH = h * 0.26f
            val cupY = cy - cupH * 0.22f
            drawRoundRect(
                brush = Brush.verticalGradient(listOf(NeonCyan, ElectricPurple)),
                topLeft = Offset(cx - archRadius - cupW * 0.45f, cupY),
                size = Size(cupW, cupH),
                cornerRadius = CornerRadius(cupW * 0.5f, cupW * 0.5f)
            )
            drawRoundRect(
                brush = Brush.verticalGradient(listOf(VinylRose, ElectricPurple)),
                topLeft = Offset(cx + archRadius - cupW * 0.55f, cupY),
                size = Size(cupW, cupH),
                cornerRadius = CornerRadius(cupW * 0.5f, cupW * 0.5f)
            )

            // 6. Subtle Animated Music Wave Bars Inside the Headphones
            val barCount = 5
            val totalWaveW = archRadius * 1.18f
            val spacing = totalWaveW / (barCount - 1)
            val startX = cx - totalWaveW / 2f
            val baseCenterY = cy + h * 0.06f
            val barStroke = (w * 0.056f).coerceAtLeast(2f)

            for (i in 0 until barCount) {
                val x = startX + i * spacing
                val envelope = if (i == 2) 1.0f else if (i == 1 || i == 3) 0.76f else 0.48f
                val waveMod = 0.55f + 0.45f * sin(wavePhase + i * 0.95f)
                val halfBarH = (h * 0.19f) * envelope * waveMod
                val barColor = when (i) {
                    0, 4 -> NeonCyan
                    1, 3 -> ElectricPurpleBright
                    else -> VinylRose
                }
                drawLine(
                    color = barColor,
                    start = Offset(x, baseCenterY - halfBarH),
                    end = Offset(x, baseCenterY + halfBarH),
                    strokeWidth = barStroke,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

/**
 * Premium Animated Splash Screen Overlay when the app opens.
 * Features smooth logo rotation, subtle glowing particles, gentle wave movement,
 * and smooth transition into the Home Screen.
 */
@Composable
fun LofiWaveSplashOverlay(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "splash_glow")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "splash_pulse_scale"
    )
    val particlePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI.toFloat()),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "splash_particle_phase"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        DeepObsidian,
                        Color(0xFF140B2E),
                        DeepObsidian
                    )
                )
            )
            .clickable { onDismiss() }
            .testTag("lofi_wave_splash_overlay")
    ) {
        // Subtle glowing particles & gentle background wave movement
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // 18 subtle floating neon glowing particles
            for (i in 0 until 18) {
                val baseX = ((i * 73 + 19) % 100) / 100f * w
                val baseY = ((i * 41 + 11) % 100) / 100f * h
                val driftX = sin(particlePhase + i * 0.7f) * 18.dp.toPx()
                val driftY = cos(particlePhase * 0.8f + i * 0.9f) * 22.dp.toPx()
                val alphaMod = 0.25f + 0.35f * (0.5f + 0.5f * sin(particlePhase + i * 1.1f))
                val pColor = when (i % 3) {
                    0 -> NeonCyan
                    1 -> ElectricPurpleBright
                    else -> VinylRose
                }
                val radius = (2.5f + (i % 4) * 1.4f).dp.toPx()
                val center = Offset(baseX + driftX, baseY + driftY)
                drawCircle(
                    color = pColor.copy(alpha = alphaMod * 0.32f),
                    radius = radius * 2.4f,
                    center = center
                )
                drawCircle(
                    color = pColor.copy(alpha = alphaMod),
                    radius = radius,
                    center = center
                )
            }

            // Gentle glowing wave movement across lower canvas
            val wavePath = Path()
            val waveYBase = h * 0.78f
            val steps = 40
            for (s in 0..steps) {
                val x = (s.toFloat() / steps) * w
                val y = waveYBase + sin(particlePhase + s * 0.25f) * 14.dp.toPx()
                if (s == 0) wavePath.moveTo(x, y) else wavePath.lineTo(x, y)
            }
            drawPath(
                path = wavePath,
                brush = Brush.horizontalGradient(
                    listOf(
                        Color.Transparent,
                        ElectricPurple.copy(alpha = 0.45f),
                        NeonCyan.copy(alpha = 0.55f),
                        VinylRose.copy(alpha = 0.45f),
                        Color.Transparent
                    )
                ),
                style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            AnimatedLofiWaveLogo(
                size = 136.dp,
                isPlaying = true,
                modifier = Modifier.scale(pulseScale)
            )

            Spacer(modifier = Modifier.height(26.dp))

            Text(
                text = "LOFI WAVE",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 32.sp,
                    letterSpacing = 3.sp
                ),
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "ANIME LO-FI STUDIO • 8D & 3D AUDIO",
                style = MaterialTheme.typography.labelLarge,
                color = NeonCyan
            )

            Spacer(modifier = Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .width(148.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(ElectricPurple, NeonCyan, VinylRose)
                        )
                    )
            )
        }
    }
}

/**
 * Real-Time Audio Visualizer reacting to actual PCM spectrum and waveform data
 * emitted by LofiDspAudioProcessor.
 */
@Composable
fun RealTimeAudioVisualizer(
    snapshot: VisualizerSnapshot,
    isPlaying: Boolean,
    isLofiMode: Boolean,
    modifier: Modifier = Modifier,
    showWaveformOverlay: Boolean = true,
    visualizerMode: VisualizerMode = VisualizerMode.SPECTRUM_BARS
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    val infiniteTransition = rememberInfiniteTransition(label = "viz_idle_wave")
    val animPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI.toFloat()),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "viz_phase"
    )

    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        DeepObsidian.copy(alpha = 0.92f),
                        ElevatedCharcoal.copy(alpha = 0.88f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        ElectricPurple.copy(alpha = 0.55f),
                        NeonCyan.copy(alpha = 0.45f),
                        VinylRose.copy(alpha = 0.45f)
                    )
                ),
                shape = RoundedCornerShape(18.dp)
            )
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag("realtime_audio_visualizer")
    ) {
        val w = size.width
        val h = size.height
        val bars = snapshot.spectrum.size.coerceAtLeast(1)

        when (visualizerMode) {
            VisualizerMode.SPECTRUM_BARS -> {
                val gap = 4.dp.toPx()
                val barWidth = ((w - gap * (bars - 1)) / bars).coerceAtLeast(2f)

                for (row in 1..3) {
                    val y = h * (row / 4f)
                    drawLine(
                        color = SubtleBorder.copy(alpha = 0.35f),
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1f
                    )
                }

                for (i in 0 until bars) {
                    val pcmMag = snapshot.spectrum[i]
                    val fallbackPulse = if (isPlaying) {
                        0.18f + 0.14f * sin(animPhase + i * 0.45f).coerceAtLeast(0f)
                    } else 0.06f
                    val rawMag = if (isPlaying) maxOf(pcmMag, fallbackPulse) else 0.06f
                    val normalizedHeight = (rawMag.coerceIn(0.06f, 1.0f)) * (h * 0.86f)
                    val x = i * (barWidth + gap)
                    val topY = h - normalizedHeight

                    val barBrush = if (isLofiMode) {
                        Brush.verticalGradient(
                            colors = listOf(NeonCyan, ElectricPurple, VinylRose.copy(alpha = 0.75f)),
                            startY = topY,
                            endY = h
                        )
                    } else {
                        Brush.verticalGradient(
                            colors = listOf(NeonCyan, secondaryColor, primaryColor.copy(alpha = 0.55f)),
                            startY = topY,
                            endY = h
                        )
                    }

                    drawRoundRect(
                        brush = barBrush,
                        topLeft = Offset(x, topY),
                        size = Size(barWidth, normalizedHeight),
                        cornerRadius = CornerRadius(6f, 6f)
                    )
                }

                if (showWaveformOverlay && snapshot.waveform.isNotEmpty()) {
                    val path = Path()
                    val pts = snapshot.waveform.size
                    val midY = h * 0.36f
                    val ampScale = h * 0.28f
                    for (i in 0 until pts) {
                        val px = (i.toFloat() / (pts - 1).coerceAtLeast(1)) * w
                        val sample = if (isPlaying) snapshot.waveform[i] else 0f
                        val py = (midY - sample * ampScale).coerceIn(2f, h - 2f)
                        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                    }
                    drawPath(
                        path = path,
                        brush = Brush.horizontalGradient(listOf(NeonCyan, ElectricPurpleBright, VinylRose)),
                        style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }

            VisualizerMode.WAVEFORM -> {
                val midY = h * 0.5f
                drawLine(
                    color = SubtleBorder.copy(alpha = 0.5f),
                    start = Offset(0f, midY),
                    end = Offset(w, midY),
                    strokeWidth = 1f
                )
                if (snapshot.waveform.isNotEmpty()) {
                    val path = Path()
                    val pts = snapshot.waveform.size
                    val ampScale = h * 0.44f
                    for (i in 0 until pts) {
                        val px = (i.toFloat() / (pts - 1).coerceAtLeast(1)) * w
                        val sample = if (isPlaying) snapshot.waveform[i] else 0f
                        val py = (midY - sample * ampScale).coerceIn(2f, h - 2f)
                        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                    }
                    drawPath(
                        path = path,
                        brush = Brush.horizontalGradient(listOf(NeonCyan, primaryColor, VinylRose)),
                        style = Stroke(width = 2.8.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }

            VisualizerMode.CIRCULAR -> {
                val cx = w * 0.5f
                val cy = h * 0.5f
                val baseRadius = (minOf(w, h) * 0.24f).coerceAtLeast(10f)
                val maxSpike = (minOf(w, h) * 0.22f).coerceAtLeast(8f)
                val rmsPulse = if (isPlaying) ((snapshot.rmsLeft + snapshot.rmsRight) * 0.5f).coerceIn(0f, 1f) else 0.05f

                drawCircle(
                    color = primaryColor.copy(alpha = 0.22f + 0.35f * rmsPulse),
                    radius = baseRadius * (0.95f + 0.2f * rmsPulse),
                    center = Offset(cx, cy)
                )

                for (i in 0 until bars) {
                    val angle = (i.toFloat() / bars) * (2.0 * PI)
                    val mag = if (isPlaying) snapshot.spectrum[i].coerceIn(0.08f, 1.0f) else 0.06f
                    val r0 = baseRadius
                    val r1 = baseRadius + mag * maxSpike
                    val x0 = cx + cos(angle).toFloat() * r0
                    val y0 = cy + sin(angle).toFloat() * r0
                    val x1 = cx + cos(angle).toFloat() * r1
                    val y1 = cy + sin(angle).toFloat() * r1
                    drawLine(
                        color = when (i % 3) {
                            0 -> NeonCyan
                            1 -> ElectricPurple
                            else -> VinylRose
                        },
                        start = Offset(x0, y0),
                        end = Offset(x1, y1),
                        strokeWidth = 2.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }

            VisualizerMode.THREE_D_STAGE -> {
                val cx = w * 0.5f
                val cy = h * 0.5f
                val maxR = minOf(w, h) * 0.44f
                for (ring in 1..3) {
                    drawCircle(
                        color = SubtleBorder.copy(alpha = 0.45f),
                        radius = maxR * (ring / 3f),
                        center = Offset(cx, cy),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
                val lEnergy = if (isPlaying) snapshot.rmsLeft.coerceIn(0.05f, 1f) else 0.08f
                val rEnergy = if (isPlaying) snapshot.rmsRight.coerceIn(0.05f, 1f) else 0.08f
                val panX = ((rEnergy - lEnergy) * w * 0.28f).coerceIn(-w * 0.35f, w * 0.35f)
                val sourceCenter = Offset(cx + panX, cy)

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(NeonCyan.copy(alpha = 0.55f), Color.Transparent),
                        center = sourceCenter,
                        radius = maxR * 0.7f
                    ),
                    radius = maxR * 0.7f,
                    center = sourceCenter
                )
                drawCircle(
                    color = NeonCyan,
                    radius = 6.dp.toPx(),
                    center = sourceCenter
                )
            }
        }
    }
}

/**
 * Large Circular Album Artwork with Animated Circular Neon Audio Visualizer Ring
 * for the dedicated Full-Screen Music Player.
 */
@Composable
fun CircularVisualizerArtwork(
    albumArtUri: String?,
    isPlaying: Boolean,
    isLofiMode: Boolean,
    snapshot: VisualizerSnapshot,
    size: Dp = 264.dp,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "circular_player_art")
    val discRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "disc_rotation"
    )
    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "outer_neon_ring_rotation"
    )
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI.toFloat()),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "circular_viz_wave"
    )

    val artScale by animateFloatAsState(
        targetValue = if (isPlaying) 1.0f else 0.95f,
        animationSpec = tween(durationMillis = 360),
        label = "circular_art_scale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .scale(artScale)
            .testTag("full_player_circular_artwork")
    ) {
        // 1. Outer Radial Neon Glow + Circular Spectrum Visualizer Bars
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = this.size.width / 2f
            val cy = this.size.height / 2f
            val maxR = this.size.minDimension / 2f
            val artR = maxR * 0.72f
            val rmsPulse = if (isPlaying) {
                ((snapshot.rmsLeft + snapshot.rmsRight) * 0.5f).coerceIn(0.08f, 1f)
            } else 0.05f

            // Ambient Neon Violet & Electric Blue Glow Halo
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        ElectricPurple.copy(alpha = 0.38f + 0.22f * rmsPulse),
                        NeonCyan.copy(alpha = 0.22f + 0.16f * rmsPulse),
                        VinylRose.copy(alpha = 0.12f),
                        Color.Transparent
                    ),
                    center = Offset(cx, cy),
                    radius = maxR
                ),
                radius = maxR,
                center = Offset(cx, cy)
            )

            // Rotating Outer Neon Gradient Ring
            rotate(degrees = ringRotation, pivot = Offset(cx, cy)) {
                drawCircle(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            ElectricPurple,
                            NeonCyan,
                            VinylRose,
                            ElectricPurpleBright,
                            NeonCyan,
                            ElectricPurple
                        ),
                        center = Offset(cx, cy)
                    ),
                    radius = artR + 5.dp.toPx(),
                    center = Offset(cx, cy),
                    style = Stroke(width = 3.dp.toPx())
                )
            }

            // 48-Bar Circular Neon Audio Visualizer around the Album Artwork
            val totalBars = 48
            val specSize = snapshot.spectrum.size.coerceAtLeast(1)
            val baseSpikeR = artR + 10.dp.toPx()
            val maxSpikeLen = (maxR - baseSpikeR).coerceAtLeast(12f)

            for (i in 0 until totalBars) {
                val angleRad = (i.toFloat() / totalBars) * 2.0 * PI
                val specIdx = (i * specSize / totalBars).coerceIn(0, specSize - 1)
                val rawSpec = snapshot.spectrum[specIdx]
                val rhythmicFallback = if (isPlaying) {
                    0.18f + 0.22f * sin(wavePhase + i * 0.35f).coerceAtLeast(0f)
                } else 0.06f
                val magnitude = if (isPlaying) maxOf(rawSpec, rhythmicFallback).coerceIn(0.08f, 1.0f) else 0.07f

                val rStart = baseSpikeR
                val rEnd = baseSpikeR + magnitude * maxSpikeLen
                val x0 = cx + cos(angleRad).toFloat() * rStart
                val y0 = cy + sin(angleRad).toFloat() * rStart
                val x1 = cx + cos(angleRad).toFloat() * rEnd
                val y1 = cy + sin(angleRad).toFloat() * rEnd

                val barColor = when (i % 3) {
                    0 -> NeonCyan
                    1 -> ElectricPurpleBright
                    else -> VinylRose
                }

                drawLine(
                    color = barColor.copy(alpha = if (isPlaying) 0.92f else 0.42f),
                    start = Offset(x0, y0),
                    end = Offset(x1, y1),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        // 2. Large Circular Album Artwork Disc
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(size * 0.72f)
                .rotate(if (isPlaying) discRotation else 0f)
                .clip(CircleShape)
                .background(ElevatedCharcoal)
                .border(2.dp, SubtleBorder, CircleShape)
        ) {
            if (!albumArtUri.isNullOrBlank()) {
                AsyncImage(
                    model = Uri.parse(albumArtUri),
                    contentDescription = "Circular Album Artwork",
                    contentScale = ContentScale.Crop,
                    error = painterResource(id = R.drawable.img_lofi_studio_banner),
                    placeholder = painterResource(id = R.drawable.img_lofi_studio_banner),
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.img_lofi_studio_banner),
                    contentDescription = "Circular Anime Lo-Fi Artwork",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Subtle vinyl center hub ring
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(DeepObsidian.copy(alpha = 0.86f))
                    .border(
                        width = 2.dp,
                        brush = Brush.linearGradient(listOf(ElectricPurple, NeonCyan, VinylRose)),
                        shape = CircleShape
                    )
            )
        }
    }
}

/**
 * Animated Rotating Vinyl Album Artwork with vinyl grooves and studio center label.
 */
@Composable
fun RotatingVinylArtwork(
    albumArtUri: String?,
    isPlaying: Boolean,
    isLofiMode: Boolean,
    size: Dp = 240.dp,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "vinyl_spin")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isLofiMode) 14000 else 11000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "vinyl_angle"
    )

    val currentRotation = if (isPlaying) angle else 0f

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(size)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = this.size.minDimension / 2f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        if (isLofiMode) ElectricPurple.copy(alpha = 0.35f) else NeonCyan.copy(alpha = 0.28f),
                        Color.Transparent
                    )
                ),
                radius = radius
            )
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(size * 0.92f)
                .rotate(currentRotation)
                .clip(CircleShape)
                .background(Color(0xFF0A0910))
                .border(
                    width = 2.dp,
                    brush = Brush.sweepGradient(
                        listOf(
                            SubtleBorder,
                            if (isLofiMode) ElectricPurple else NeonCyan,
                            SubtleBorder,
                            if (isLofiMode) NeonCyan else VinylRose,
                            SubtleBorder
                        )
                    ),
                    shape = CircleShape
                )
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val r = this.size.minDimension / 2f
                for (ring in 1..6) {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.04f),
                        radius = r * (0.48f + ring * 0.08f),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(size * 0.56f)
                    .clip(CircleShape)
                    .border(2.dp, SubtleBorder, CircleShape)
            ) {
                if (!albumArtUri.isNullOrBlank()) {
                    AsyncImage(
                        model = Uri.parse(albumArtUri),
                        contentDescription = "Album Artwork",
                        contentScale = ContentScale.Crop,
                        error = painterResource(id = R.drawable.img_default_vinyl_art),
                        placeholder = painterResource(id = R.drawable.img_default_vinyl_art),
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.img_default_vinyl_art),
                        contentDescription = "Default Vinyl Artwork",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(DeepObsidian)
                        .border(2.dp, SubtleBorder, CircleShape)
                )
            }
        }
    }
}

/**
 * Instant Zero-Restart Hi-Fi Original vs. Live Lo-Fi Mode Toggle Bar.
 */
@Composable
fun HifiLofiModeToggleBanner(
    dspParams: LofiDspParameters,
    onToggleMode: (Boolean) -> Unit,
    onOpenStudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLofi = dspParams.isLiveLofiEnabled
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (isLofi) ElevatedCharcoal else GlassSurface,
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.5.dp,
                brush = Brush.horizontalGradient(
                    if (isLofi) listOf(ElectricPurple, NeonCyan, VinylRose)
                    else listOf(StudioEmerald, NeonCyan)
                ),
                shape = RoundedCornerShape(18.dp)
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onOpenStudio() }
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (isLofi) ElectricPurple.copy(alpha = 0.22f)
                            else StudioEmerald.copy(alpha = 0.22f)
                        )
                ) {
                    Icon(
                        imageVector = if (isLofi) Icons.Default.GraphicEq else Icons.Default.Equalizer,
                        contentDescription = "Audio Mode",
                        tint = if (isLofi) ElectricPurpleBright else StudioEmerald
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isLofi) "REAL-TIME LIVE LO-FI" else "ORIGINAL HI-FI MODE",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isLofi) NeonCyan else StudioEmerald
                        )
                    }
                    Text(
                        text = if (isLofi) {
                            "Preset: ${dspParams.presetName} • ${(dspParams.masterIntensity * 100).toInt()}% Wet"
                        } else {
                            "Lo-Fi DSP Bypassed • Pure Original Decoded Audio"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Switch(
                checked = isLofi,
                onCheckedChange = onToggleMode,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = ElectricPurple,
                    uncheckedThumbColor = StudioEmerald,
                    uncheckedTrackColor = DeepObsidian
                ),
                modifier = Modifier.testTag("toggle_live_lofi_switch")
            )
        }
    }
}

/**
 * Sleek Two-State Segmented Control: ORIGINAL / LO-FI
 * Connects directly to existing zero-restart Live Lo-Fi DSP toggle.
 */
@Composable
fun OriginalLofiSegmentedControl(
    isLofiEnabled: Boolean,
    activePresetName: String,
    onToggleMode: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    testTagPrefix: String = "mode_selector"
) {
    val originalBgAlpha by animateFloatAsState(
        targetValue = if (!isLofiEnabled) 1f else 0f,
        animationSpec = tween(durationMillis = 240),
        label = "orig_bg_alpha"
    )
    val lofiBgAlpha by animateFloatAsState(
        targetValue = if (isLofiEnabled) 1f else 0f,
        animationSpec = tween(durationMillis = 240),
        label = "lofi_bg_alpha"
    )

    Surface(
        color = MidnightCharcoal.copy(alpha = 0.94f),
        shape = RoundedCornerShape(50),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, SubtleBorder.copy(alpha = 0.85f), RoundedCornerShape(50))
            .testTag("${testTagPrefix}_container")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            // ORIGINAL state button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .clip(RoundedCornerShape(50))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                GlassSurface.copy(alpha = originalBgAlpha),
                                ElevatedCharcoal.copy(alpha = originalBgAlpha)
                            )
                        )
                    )
                    .then(
                        if (!isLofiEnabled) {
                            Modifier.border(1.dp, NeonCyan.copy(alpha = 0.75f), RoundedCornerShape(50))
                        } else Modifier
                    )
                    .clickable { onToggleMode(false) }
                    .testTag("${testTagPrefix}_original")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (!isLofiEnabled) NeonCyan else TextMuted.copy(alpha = 0.4f))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ORIGINAL",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (!isLofiEnabled) FontWeight.Bold else FontWeight.Medium,
                        color = if (!isLofiEnabled) TextPrimary else TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // LO-FI state button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .clip(RoundedCornerShape(50))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                ElectricPurple.copy(alpha = 0.92f * lofiBgAlpha),
                                NeonCyan.copy(alpha = 0.82f * lofiBgAlpha),
                                VinylRose.copy(alpha = 0.82f * lofiBgAlpha)
                            )
                        )
                    )
                    .clickable { onToggleMode(true) }
                    .testTag("${testTagPrefix}_lofi")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = if (isLofiEnabled) Color.White else TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isLofiEnabled && activePresetName.isNotBlank() && activePresetName != "Original") {
                            "LO-FI • ${activePresetName.uppercase()}"
                        } else {
                            "LIVE LO-FI"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isLofiEnabled) FontWeight.Bold else FontWeight.Medium,
                        color = if (isLofiEnabled) Color.White else TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * Persistent Mini Player Bar displayed above the NavigationBar.
 * Tapping anywhere on the MiniPlayerBar opens the dedicated Full-Screen Music Player.
 */
@Composable
fun MiniPlayerBar(
    track: TrackItem,
    isPlaying: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    dspParams: LofiDspParameters,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onToggleLofiMode: (Boolean) -> Unit,
    onExpandNowPlaying: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = if (durationMs > 0L) {
        (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Surface(
        color = ElevatedCharcoal.copy(alpha = 0.97f),
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
        tonalElevation = 8.dp,
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        ElectricPurple.copy(alpha = 0.65f),
                        NeonCyan.copy(alpha = 0.5f),
                        VinylRose.copy(alpha = 0.55f)
                    )
                ),
                shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)
            )
            .clickable { onExpandNowPlaying() }
            .testTag("mini_player_bar")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Neon gradient progress indicator bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(DeepObsidian)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(3.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(ElectricPurple, NeonCyan, VinylRose)
                            )
                        )
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 9.dp)
            ) {
                // Circular / Rounded Anime Album Art Thumbnail with Neon Border
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .border(
                            width = 1.5.dp,
                            brush = Brush.linearGradient(listOf(ElectricPurple, NeonCyan, VinylRose)),
                            shape = CircleShape
                        )
                ) {
                    if (!track.albumArtUri.isNullOrBlank()) {
                        AsyncImage(
                            model = Uri.parse(track.albumArtUri),
                            contentDescription = "Mini Player Artwork",
                            contentScale = ContentScale.Crop,
                            error = painterResource(id = R.drawable.img_lofi_studio_banner),
                            placeholder = painterResource(id = R.drawable.img_lofi_studio_banner),
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.img_lofi_studio_banner),
                            contentDescription = "Mini Player Anime Artwork",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = track.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (dspParams.isLiveLofiEnabled) ElectricPurple.copy(alpha = 0.28f)
                                    else GlassSurface
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (dspParams.isLiveLofiEnabled) NeonCyan else SubtleBorder,
                                    shape = RoundedCornerShape(50)
                                )
                                .clickable { onToggleLofiMode(!dspParams.isLiveLofiEnabled) }
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                .testTag("mini_player_mode_badge")
                        ) {
                            Text(
                                text = if (dspParams.isLiveLofiEnabled) dspParams.soundMode.badge else "ORIGINAL",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (dspParams.isLiveLofiEnabled) NeonCyan else TextSecondary
                            )
                        }
                    }
                    Text(
                        text = "${track.artist} • Tap for Full Player",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(ElectricPurple, NeonCyan))
                        )
                        .clickable { onTogglePlayPause() }
                        .testTag("mini_play_pause_button")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                IconButton(
                    onClick = onSkipNext,
                    modifier = Modifier.testTag("mini_next_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next Track",
                        tint = TextPrimary
                    )
                }
            }
        }
    }
}

/**
 * 4. SEPARATE FULL-SCREEN MUSIC PLAYER (CRITICAL):
 * Designed like a flagship modern music streaming app:
 * - Large circular album artwork with rotating neon gradient ring
 * - Beautiful animated neon audio visualizer around the circular album artwork
 * - Animated waveform / equalizer bars when music is playing
 * - Song title and artist name + Favorite button
 * - Seek / progress bar + Current time and total duration
 * - Shuffle, Previous, Gradient Play/Pause, Next, and Repeat controls
 * - Shortcuts for Sound Modes, Equalizer, Sleep Timer, and More Options menu
 */
@Composable
fun FullScreenNowPlayingOverlay(
    track: TrackItem,
    isPlaying: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    shuffleEnabled: Boolean,
    repeatMode: Int,
    isFavorite: Boolean,
    dspParams: LofiDspParameters,
    visualizerSnapshot: VisualizerSnapshot,
    queue: List<TrackItem>,
    sleepTimerRemainingSec: Int?,
    onClose: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleLofiMode: (Boolean) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onSelectQueueTrack: (TrackItem) -> Unit,
    onSetSleepTimerMinutes: (Int) -> Unit,
    onOpenLofiStudio: () -> Unit,
    onOpenEqualizer: () -> Unit = {},
    onOpenMoodScreen: () -> Unit = {},
    onSelectSoundMode: (LiveSoundMode) -> Unit = {},
    onOpenMusicConverter: () -> Unit = {}
) {
    BackHandler(onBack = onClose)

    // 0 = Circular Artwork + Neon Visualizer, 1 = Sound Modes Panel, 2 = Queue & Lyrics, 3 = Sleep Timer & Speed
    var activeSubTab by remember { mutableIntStateOf(0) }
    var showMoreOptionsMenu by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepObsidian)
            .testTag("fullscreen_now_playing")
    ) {
        // 1. Subtle Blurred/Dimmed Anime Artwork Backdrop
        if (!track.albumArtUri.isNullOrBlank()) {
            AsyncImage(
                model = Uri.parse(track.albumArtUri),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                error = painterResource(id = R.drawable.img_lofi_studio_banner),
                placeholder = painterResource(id = R.drawable.img_lofi_studio_banner),
                alpha = 0.24f,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Image(
                painter = painterResource(id = R.drawable.img_lofi_studio_banner),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alpha = 0.26f,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 2. Deep AMOLED Black & Rich Midnight Purple Gradient Scrim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            DeepObsidian.copy(alpha = 0.72f),
                            Color(0xFF140C2E).copy(alpha = 0.88f),
                            DeepObsidian.copy(alpha = 0.96f),
                            DeepObsidian
                        )
                    )
                )
        )

        // 3. Foreground Full-Screen Player Layout
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 22.dp, vertical = 10.dp)
        ) {
            // Top Bar: Minimize Button, Active Mode Header, and More Options Menu
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(ElevatedCharcoal.copy(alpha = 0.85f))
                        .border(1.dp, SubtleBorder, CircleShape)
                        .testTag("close_now_playing_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Minimize Player",
                        tint = TextPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "NOW PLAYING",
                        style = MaterialTheme.typography.labelMedium,
                        color = NeonCyan,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${dspParams.soundMode.displayName} • ${
                            if (dspParams.isLiveLofiEnabled) dspParams.presetName else "Pure Hi-Fi"
                        }",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                Box {
                    IconButton(
                        onClick = { showMoreOptionsMenu = true },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(ElevatedCharcoal.copy(alpha = 0.85f))
                            .border(1.dp, SubtleBorder, CircleShape)
                            .testTag("full_player_more_options_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More Options",
                            tint = TextPrimary
                        )
                    }

                    DropdownMenu(
                        expanded = showMoreOptionsMenu,
                        onDismissRequest = { showMoreOptionsMenu = false },
                        modifier = Modifier.background(ElevatedCharcoal)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Sound Modes (${dspParams.soundMode.displayName})", color = TextPrimary) },
                            leadingIcon = { Icon(Icons.Default.Headphones, contentDescription = null, tint = NeonCyan) },
                            onClick = {
                                showMoreOptionsMenu = false
                                activeSubTab = 1
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("10-Band Equalizer", color = TextPrimary) },
                            leadingIcon = { Icon(Icons.Default.Equalizer, contentDescription = null, tint = ElectricPurpleBright) },
                            onClick = {
                                showMoreOptionsMenu = false
                                onClose()
                                onOpenEqualizer()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Sleep Timer & Playback Speed", color = TextPrimary) },
                            leadingIcon = { Icon(Icons.Default.Bedtime, contentDescription = null, tint = AnalogAmber) },
                            onClick = {
                                showMoreOptionsMenu = false
                                activeSubTab = 3
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Lo-Fi DSP Studio", color = TextPrimary) },
                            leadingIcon = { Icon(Icons.Default.Tune, contentDescription = null, tint = NeonCyan) },
                            onClick = {
                                showMoreOptionsMenu = false
                                onClose()
                                onOpenLofiStudio()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Convert & Export Song", color = TextPrimary) },
                            leadingIcon = { Icon(Icons.Default.GraphicEq, contentDescription = null, tint = StudioEmerald) },
                            onClick = {
                                showMoreOptionsMenu = false
                                onClose()
                                onOpenMusicConverter()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("AI Music Mood Detection", color = TextPrimary) },
                            leadingIcon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = VinylRose) },
                            onClick = {
                                showMoreOptionsMenu = false
                                onClose()
                                onOpenMoodScreen()
                            }
                        )
                        HorizontalDivider(color = SubtleBorder)
                        DropdownMenuItem(
                            text = { Text("Up Next Queue & Lyrics", color = TextPrimary) },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null, tint = TextSecondary) },
                            onClick = {
                                showMoreOptionsMenu = false
                                activeSubTab = 2
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Centerpiece Area: Circular Artwork + Visualizer OR Sound Modes / Queue / Sleep Timer
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (activeSubTab) {
                    0 -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // Large Circular Album Artwork + Circular Neon Audio Visualizer
                            CircularVisualizerArtwork(
                                albumArtUri = track.albumArtUri,
                                isPlaying = isPlaying,
                                isLofiMode = dspParams.isLiveLofiEnabled,
                                snapshot = visualizerSnapshot,
                                size = 248.dp
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Animated Neon Waveform + Equalizer Spectrum Bars
                            RealTimeAudioVisualizer(
                                snapshot = visualizerSnapshot,
                                isPlaying = isPlaying,
                                isLofiMode = dspParams.isLiveLofiEnabled,
                                visualizerMode = VisualizerMode.SPECTRUM_BARS,
                                showWaveformOverlay = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(58.dp)
                            )
                        }
                    }

                    1 -> {
                        // In-Player Interactive Sound Modes Selector Panel
                        Surface(
                            color = ElevatedCharcoal.copy(alpha = 0.94f),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .border(1.dp, SubtleBorder, RoundedCornerShape(24.dp))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "SELECT LIVE SOUND MODE",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = NeonCyan
                                    )
                                    Text(
                                        text = "Back to Artwork",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ElectricPurpleBright,
                                        modifier = Modifier.clickable { activeSubTab = 0 }
                                    )
                                }

                                LiveSoundMode.entries.forEach { mode ->
                                    val selected = dspParams.soundMode == mode
                                    Surface(
                                        color = if (selected) ElectricPurple.copy(alpha = 0.28f) else DeepObsidian,
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(
                                                width = if (selected) 1.5.dp else 1.dp,
                                                color = if (selected) NeonCyan else SubtleBorder,
                                                shape = RoundedCornerShape(14.dp)
                                            )
                                            .clickable { onSelectSoundMode(mode) }
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = mode.displayName,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    color = if (selected) NeonCyan else TextPrimary
                                                )
                                                Text(
                                                    text = mode.subtitle,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = TextSecondary
                                                )
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(50))
                                                    .background(
                                                        if (selected) NeonCyan.copy(alpha = 0.2f) else GlassSurface
                                                    )
                                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = if (selected) "ACTIVE" else mode.badge,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (selected) NeonCyan else ElectricPurpleBright
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        // Queue & Lyrics Panel
                        Surface(
                            color = ElevatedCharcoal.copy(alpha = 0.94f),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .border(1.dp, SubtleBorder, RoundedCornerShape(24.dp))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(14.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "UP NEXT QUEUE (${queue.size})",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = NeonCyan
                                    )
                                    Text(
                                        text = "Back to Artwork",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ElectricPurpleBright,
                                        modifier = Modifier.clickable { activeSubTab = 0 }
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                LazyColumn(modifier = Modifier.fillMaxSize()) {
                                    items(queue, key = { it.id }) { qTrack ->
                                        val isCurrent = qTrack.id == track.id
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(
                                                    if (isCurrent) ElectricPurple.copy(alpha = 0.24f)
                                                    else Color.Transparent
                                                )
                                                .clickable { onSelectQueueTrack(qTrack) }
                                                .padding(10.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isCurrent) Icons.Default.GraphicEq else Icons.Default.Album,
                                                contentDescription = null,
                                                tint = if (isCurrent) NeonCyan else TextSecondary
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = qTrack.title,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    color = if (isCurrent) NeonCyan else TextPrimary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = qTrack.artist,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = TextSecondary,
                                                    maxLines = 1
                                                )
                                            }
                                            Text(
                                                text = formatDurationMillis(qTrack.durationMs),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextMuted
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    3 -> {
                        // Sleep Timer & Speed Panel
                        Surface(
                            color = ElevatedCharcoal.copy(alpha = 0.94f),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .border(1.dp, SubtleBorder, RoundedCornerShape(24.dp))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Bedtime, contentDescription = null, tint = AnalogAmber)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (sleepTimerRemainingSec != null) {
                                                "SLEEP TIMER: ${sleepTimerRemainingSec / 60}m ${sleepTimerRemainingSec % 60}s"
                                            } else {
                                                "SLEEP TIMER"
                                            },
                                            style = MaterialTheme.typography.labelLarge,
                                            color = AnalogAmber
                                        )
                                    }
                                    Text(
                                        text = "Back to Artwork",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ElectricPurpleBright,
                                        modifier = Modifier.clickable { activeSubTab = 0 }
                                    )
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    listOf(0 to "Off", 15 to "15m", 30 to "30m", 60 to "60m").forEach { (mins, text) ->
                                        FilterChip(
                                            selected = (mins == 0 && sleepTimerRemainingSec == null),
                                            onClick = { onSetSleepTimerMinutes(mins) },
                                            label = { Text(text, style = MaterialTheme.typography.labelSmall) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }

                                HorizontalDivider(color = SubtleBorder)

                                Text(
                                    text = "PLAYBACK SPEED (${String.format(Locale.US, "%.2fx", dspParams.playbackSpeed)})",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = NeonCyan
                                )
                                Slider(
                                    value = dspParams.playbackSpeed,
                                    onValueChange = onSpeedChange,
                                    valueRange = 0.65f..1.35f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = NeonCyan,
                                        activeTrackColor = ElectricPurple
                                    )
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    listOf(0.85f to "0.85x", 0.94f to "0.94x", 1.0f to "1.0x", 1.15f to "1.15x").forEach { (spd, lbl) ->
                                        FilterChip(
                                            selected = kotlin.math.abs(dspParams.playbackSpeed - spd) < 0.02f,
                                            onClick = { onSpeedChange(spd) },
                                            label = { Text(lbl, style = MaterialTheme.typography.labelSmall) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Song Title, Artist Name, Album & Verified Metadata Badges
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("full_player_song_title")
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${track.artist} • ${track.album}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("full_player_artist_album")
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.testTag("full_player_metadata_badges")
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(DeepObsidian)
                                .border(1.dp, SubtleBorder, RoundedCornerShape(6.dp))
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = track.technicalMetadataSummary,
                                style = MaterialTheme.typography.labelSmall,
                                color = NeonCyan
                            )
                        }
                        if (track.genre.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(ElectricPurple.copy(alpha = 0.22f))
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if ((track.trackNumber ?: 0) > 0) "${track.genre} • #${track.trackNumber}" else track.genre,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }

                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(
                            if (isFavorite) VinylRose.copy(alpha = 0.20f) else ElevatedCharcoal
                        )
                        .border(
                            1.dp,
                            if (isFavorite) VinylRose else SubtleBorder,
                            CircleShape
                        )
                        .testTag("now_playing_favorite_button")
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) VinylRose else TextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Two-State ORIGINAL / LIVE LO-FI Toggle + Sound Mode Quick Chips
            OriginalLofiSegmentedControl(
                isLofiEnabled = dspParams.isLiveLofiEnabled,
                activePresetName = dspParams.presetName,
                onToggleMode = onToggleLofiMode,
                testTagPrefix = "full_player_mode"
            )

            Spacer(modifier = Modifier.height(6.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("full_player_sound_modes_row")
            ) {
                items(LiveSoundMode.entries, key = { it.name }) { mode ->
                    val selected = dspParams.soundMode == mode
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectSoundMode(mode) },
                        label = {
                            Text(
                                text = mode.displayName,
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ElectricPurple.copy(alpha = 0.32f),
                            selectedLabelColor = NeonCyan
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Seek / Progress Bar + Current Time & Total Duration
            val safeDuration = durationMs.coerceAtLeast(1L)
            Slider(
                value = currentPositionMs.coerceIn(0L, safeDuration).toFloat(),
                onValueChange = { onSeekTo(it.toLong()) },
                valueRange = 0f..safeDuration.toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor = NeonCyan,
                    activeTrackColor = ElectricPurple,
                    inactiveTrackColor = GlassSurface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(26.dp)
                    .testTag("now_playing_seek_slider")
            )

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = formatDurationMillis(currentPositionMs),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
                Text(
                    text = formatDurationMillis(safeDuration),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Main Playback Controls: Shuffle, Previous, Glowing Gradient Play/Pause, Next, Repeat
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp)
            ) {
                IconButton(
                    onClick = onToggleShuffle,
                    modifier = Modifier.testTag("shuffle_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (shuffleEnabled) NeonCyan else TextMuted
                    )
                }

                IconButton(
                    onClick = onSkipPrevious,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(ElevatedCharcoal)
                        .border(1.dp, SubtleBorder, CircleShape)
                        .testTag("previous_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = TextPrimary,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(74.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(ElectricPurple, NeonCyan, VinylRose)
                            )
                        )
                        .border(1.5.dp, Color.White.copy(alpha = 0.38f), CircleShape)
                        .clickable { onTogglePlayPause() }
                        .testTag("play_pause_main_button")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(38.dp)
                    )
                }

                IconButton(
                    onClick = onSkipNext,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(ElevatedCharcoal)
                        .border(1.dp, SubtleBorder, CircleShape)
                        .testTag("next_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = TextPrimary,
                        modifier = Modifier.size(30.dp)
                    )
                }

                IconButton(
                    onClick = onCycleRepeat,
                    modifier = Modifier.testTag("repeat_button")
                ) {
                    Icon(
                        imageVector = if (repeatMode == Player.REPEAT_MODE_ONE) {
                            Icons.Default.RepeatOne
                        } else {
                            Icons.Default.Repeat
                        },
                        contentDescription = "Repeat Mode",
                        tint = if (repeatMode != Player.REPEAT_MODE_OFF) NeonCyan else TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Dedicated Bottom Shortcuts Bar: Sound Modes | Equalizer | Sleep Timer | More Options
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                PlayerBottomShortcutChip(
                    label = "Sound Modes",
                    icon = Icons.Default.Headphones,
                    isActive = activeSubTab == 1,
                    accent = NeonCyan,
                    onClick = { activeSubTab = if (activeSubTab == 1) 0 else 1 },
                    testTag = "full_player_shortcut_sound_modes",
                    modifier = Modifier.weight(1f)
                )

                PlayerBottomShortcutChip(
                    label = "Equalizer",
                    icon = Icons.Default.Equalizer,
                    isActive = false,
                    accent = ElectricPurpleBright,
                    onClick = {
                        onClose()
                        onOpenEqualizer()
                    },
                    testTag = "full_player_shortcut_equalizer",
                    modifier = Modifier.weight(1f)
                )

                PlayerBottomShortcutChip(
                    label = if (sleepTimerRemainingSec != null) {
                        "${sleepTimerRemainingSec / 60}m"
                    } else {
                        "Sleep Timer"
                    },
                    icon = Icons.Default.Bedtime,
                    isActive = activeSubTab == 3 || sleepTimerRemainingSec != null,
                    accent = AnalogAmber,
                    onClick = { activeSubTab = if (activeSubTab == 3) 0 else 3 },
                    testTag = "full_player_shortcut_sleep_timer",
                    modifier = Modifier.weight(1f)
                )

                PlayerBottomShortcutChip(
                    label = "More",
                    icon = Icons.Default.MoreVert,
                    isActive = showMoreOptionsMenu,
                    accent = VinylRose,
                    onClick = { showMoreOptionsMenu = true },
                    testTag = "full_player_shortcut_more",
                    modifier = Modifier.weight(0.85f)
                )
            }
        }
    }
}

@Composable
private fun PlayerBottomShortcutChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    accent: Color,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (isActive) accent.copy(alpha = 0.22f) else ElevatedCharcoal.copy(alpha = 0.9f),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .border(
                width = 1.dp,
                color = if (isActive) accent else SubtleBorder,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 9.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) accent else TextSecondary,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (isActive) TextPrimary else TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
