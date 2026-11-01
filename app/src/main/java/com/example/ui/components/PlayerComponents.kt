package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import coil.compose.AsyncImage
import com.example.R
import com.example.model.LofiDspParameters
import com.example.model.TrackItem
import com.example.model.VisualizerSnapshot
import com.example.ui.theme.AnalogAmber
import com.example.ui.theme.DeepObsidian
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.ElevatedCharcoal
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.MidnightCharcoal
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.StudioEmerald
import com.example.ui.theme.SubtleBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VinylRose
import java.util.Locale

fun formatDurationMillis(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val min = totalSec / 60L
    val sec = totalSec % 60L
    return String.format(Locale.US, "%02d:%02d", min, sec)
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
    showWaveformOverlay: Boolean = true
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        DeepObsidian.copy(alpha = 0.9f),
                        ElevatedCharcoal.copy(alpha = 0.85f)
                    )
                )
            )
            .border(1.dp, SubtleBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag("realtime_audio_visualizer")
    ) {
        val w = size.width
        val h = size.height
        val bars = snapshot.spectrum.size.coerceAtLeast(1)
        val gap = 4.dp.toPx()
        val barWidth = ((w - gap * (bars - 1)) / bars).coerceAtLeast(2f)

        // Draw subtle studio grid lines
        for (row in 1..3) {
            val y = h * (row / 4f)
            drawLine(
                color = SubtleBorder.copy(alpha = 0.4f),
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = 1f
            )
        }

        // Draw 32 real-time frequency spectrum bars from actual PCM DFT
        for (i in 0 until bars) {
            val rawMag = if (isPlaying) snapshot.spectrum[i] else 0.04f
            val normalizedHeight = (rawMag.coerceIn(0.04f, 1.0f)) * (h * 0.86f)
            val x = i * (barWidth + gap)
            val topY = h - normalizedHeight

            val barBrush = if (isLofiMode) {
                Brush.verticalGradient(
                    colors = listOf(NeonCyan, primaryColor, primaryColor.copy(alpha = 0.4f)),
                    startY = topY,
                    endY = h
                )
            } else {
                Brush.verticalGradient(
                    colors = listOf(StudioEmerald, secondaryColor, secondaryColor.copy(alpha = 0.4f)),
                    startY = topY,
                    endY = h
                )
            }

            drawRoundRect(
                brush = barBrush,
                topLeft = Offset(x, topY),
                size = Size(barWidth, normalizedHeight),
                cornerRadius = CornerRadius(4f, 4f)
            )
        }

        // Draw live PCM Oscilloscope Waveform overlay
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
                color = if (isLofiMode) AnalogAmber.copy(alpha = 0.9f) else NeonCyan.copy(alpha = 0.9f),
                style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
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
        // Outer neon aura glow
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

        // Vinyl disc body
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
                            if (isLofiMode) NeonCyan else StudioEmerald,
                            SubtleBorder
                        )
                    ),
                    shape = CircleShape
                )
        ) {
            // Vinyl grooves rings
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

            // Center album cover artwork
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

                // Vinyl spindle hole
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
                    if (isLofi) listOf(ElectricPurple, NeonCyan)
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
                        tint = if (isLofi) ElectricPurple else StudioEmerald
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
    val originalBgAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (!isLofiEnabled) 1f else 0f,
        animationSpec = tween(durationMillis = 240),
        label = "orig_bg_alpha"
    )
    val lofiBgAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isLofiEnabled) 1f else 0f,
        animationSpec = tween(durationMillis = 240),
        label = "lofi_bg_alpha"
    )

    Surface(
        color = MidnightCharcoal.copy(alpha = 0.92f),
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
                            Modifier.border(1.dp, NeonCyan.copy(alpha = 0.65f), RoundedCornerShape(50))
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
                                ElectricPurple.copy(alpha = 0.85f * lofiBgAlpha),
                                NeonCyan.copy(alpha = 0.75f * lofiBgAlpha)
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
                            "LO-FI"
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
        color = ElevatedCharcoal.copy(alpha = 0.96f),
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
        tonalElevation = 6.dp,
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = SubtleBorder.copy(alpha = 0.75f),
                shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)
            )
            .clickable { onExpandNowPlaying() }
            .testTag("mini_player_bar")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Subtle progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp)
                    .background(DeepObsidian)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(2.5.dp)
                        .background(
                            Brush.horizontalGradient(
                                if (dspParams.isLiveLofiEnabled) listOf(ElectricPurple, NeonCyan)
                                else listOf(NeonCyan, ElectricPurple)
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
                // Rounded Anime Album Art Thumbnail
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, SubtleBorder, RoundedCornerShape(12.dp))
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
                                    if (dspParams.isLiveLofiEnabled) ElectricPurple.copy(alpha = 0.25f)
                                    else GlassSurface
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (dspParams.isLiveLofiEnabled) ElectricPurple else SubtleBorder,
                                    shape = RoundedCornerShape(50)
                                )
                                .clickable { onToggleLofiMode(!dspParams.isLiveLofiEnabled) }
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                .testTag("mini_player_mode_badge")
                        ) {
                            Text(
                                text = if (dspParams.isLiveLofiEnabled) "LO-FI" else "ORIGINAL",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (dspParams.isLiveLofiEnabled) NeonCyan else TextSecondary
                            )
                        }
                    }
                    Text(
                        text = track.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier.testTag("mini_play_pause_button")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = TextPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                IconButton(
                    onClick = onSkipNext,
                    modifier = Modifier.testTag("mini_next_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next Track",
                        tint = TextSecondary
                    )
                }
            }
        }
    }
}

/**
 * Immersive Full-Screen Player:
 * - Large anime-inspired background artwork with soft midnight navy & deep purple gradient overlay
 * - Crisp rounded anime centerpiece artwork with subtle playing scale transition
 * - Song information & Favorite action
 * - Two-state ORIGINAL / LO-FI toggle
 * - Seek progress bar with timestamps
 * - Shuffle, Previous, Large Glowing Play/Pause, Next, and Repeat controls
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
    onOpenMoodScreen: () -> Unit = {}
) {
    BackHandler(onBack = onClose)

    // 0 = Main Anime Artwork View, 1 = Lyrics, 2 = Queue, 3 = Speed & Sleep Timer
    var activeSubTab by remember { mutableIntStateOf(0) }

    val artworkScale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isPlaying) 1.0f else 0.95f,
        animationSpec = tween(durationMillis = 380),
        label = "full_player_art_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepObsidian)
            .testTag("fullscreen_now_playing")
    ) {
        // 1. Large Anime-Inspired Background Artwork Layer
        if (!track.albumArtUri.isNullOrBlank()) {
            AsyncImage(
                model = Uri.parse(track.albumArtUri),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                error = painterResource(id = R.drawable.img_lofi_studio_banner),
                placeholder = painterResource(id = R.drawable.img_lofi_studio_banner),
                alpha = 0.34f,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Image(
                painter = painterResource(id = R.drawable.img_lofi_studio_banner),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alpha = 0.36f,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 2. Soft Midnight Navy & Deep Purple Gradient Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            DeepObsidian.copy(alpha = 0.62f),
                            Color(0xFF14122D).copy(alpha = 0.86f),
                            DeepObsidian.copy(alpha = 0.97f),
                            DeepObsidian
                        )
                    )
                )
        )

        // 3. Foreground Player Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            // Minimal Top Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.testTag("close_now_playing_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Minimize Player",
                        tint = TextPrimary,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "NOW PLAYING",
                        style = MaterialTheme.typography.labelMedium,
                        color = NeonCyan
                    )
                    Text(
                        text = if (dspParams.isLiveLofiEnabled) {
                            "Lo-Fi Mood • ${dspParams.presetName}"
                        } else {
                            "Original Hi-Fi Audio"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                IconButton(
                    onClick = {
                        onClose()
                        onOpenLofiStudio()
                    },
                    modifier = Modifier.testTag("now_playing_open_studio_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Open Lo-Fi Studio",
                        tint = ElectricPurple
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Centerpiece Area (Anime Artwork or Optional Lyrics / Queue / Timer Panel)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (activeSubTab) {
                    0 -> {
                        // Large Rounded Anime Lo-Fi Centerpiece Artwork with Subtle Glow
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            // Soft Violet & Blue radial glow behind artwork
                            Canvas(modifier = Modifier.size(290.dp)) {
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            if (dspParams.isLiveLofiEnabled) {
                                                ElectricPurple.copy(alpha = 0.34f)
                                            } else {
                                                NeonCyan.copy(alpha = 0.24f)
                                            },
                                            Color.Transparent
                                        )
                                    ),
                                    radius = size.minDimension / 1.75f
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(28.dp),
                                color = ElevatedCharcoal,
                                tonalElevation = 12.dp,
                                modifier = Modifier
                                    .size((260 * artworkScale).dp)
                                    .border(
                                        width = 1.5.dp,
                                        brush = Brush.linearGradient(
                                            colors = if (dspParams.isLiveLofiEnabled) {
                                                listOf(ElectricPurple, NeonCyan)
                                            } else {
                                                listOf(SubtleBorder, NeonCyan.copy(alpha = 0.6f))
                                            }
                                        ),
                                        shape = RoundedCornerShape(28.dp)
                                    )
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    if (!track.albumArtUri.isNullOrBlank()) {
                                        AsyncImage(
                                            model = Uri.parse(track.albumArtUri),
                                            contentDescription = "Track Artwork",
                                            contentScale = ContentScale.Crop,
                                            error = painterResource(id = R.drawable.img_lofi_studio_banner),
                                            placeholder = painterResource(id = R.drawable.img_lofi_studio_banner),
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Image(
                                            painter = painterResource(id = R.drawable.img_lofi_studio_banner),
                                            contentDescription = "Cinematic Anime Lo-Fi Artwork",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }

                                    // Subtle bottom gradient inside artwork for mood badge
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = listOf(
                                                        Color.Transparent,
                                                        Color.Transparent,
                                                        DeepObsidian.copy(alpha = 0.72f)
                                                    )
                                                )
                                            )
                                    )

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .fillMaxWidth()
                                            .padding(14.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(50))
                                                .background(DeepObsidian.copy(alpha = 0.75f))
                                                .border(
                                                    1.dp,
                                                    if (dspParams.isLiveLofiEnabled) ElectricPurple else NeonCyan,
                                                    RoundedCornerShape(50)
                                                )
                                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = if (dspParams.isLiveLofiEnabled) {
                                                    "LO-FI • ${dspParams.presetName}"
                                                } else {
                                                    "ORIGINAL HI-FI"
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextPrimary
                                            )
                                        }

                                        Text(
                                            text = track.formatBadge,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = NeonCyan
                                        )
                                    }
                                }
                            }
                        }
                    }
                    1 -> {
                        Surface(
                            color = ElevatedCharcoal.copy(alpha = 0.9f),
                            shape = RoundedCornerShape(22.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .border(1.dp, SubtleBorder, RoundedCornerShape(22.dp))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(18.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Text(
                                    text = "LYRICS & TRACK NOTES",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = NeonCyan
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = track.lyricsOrNotes.ifBlank {
                                        "Playing ${track.title} by ${track.artist}.\nReal-time Lo-Fi audio engine ready."
                                    },
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                    2 -> {
                        Surface(
                            color = ElevatedCharcoal.copy(alpha = 0.9f),
                            shape = RoundedCornerShape(22.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .border(1.dp, SubtleBorder, RoundedCornerShape(22.dp))
                        ) {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp)
                            ) {
                                items(queue, key = { it.id }) { qTrack ->
                                    val isCurrent = qTrack.id == track.id
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(
                                                if (isCurrent) ElectricPurple.copy(alpha = 0.22f)
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
                    3 -> {
                        Surface(
                            color = ElevatedCharcoal.copy(alpha = 0.9f),
                            shape = RoundedCornerShape(22.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .border(1.dp, SubtleBorder, RoundedCornerShape(22.dp))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
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
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Song Information & Favorite Action
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${track.artist} • ${track.album}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.testTag("now_playing_favorite_button")
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) VinylRose else TextSecondary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Two-State ORIGINAL / LO-FI Toggle
            OriginalLofiSegmentedControl(
                isLofiEnabled = dspParams.isLiveLofiEnabled,
                activePresetName = dspParams.presetName,
                onToggleMode = onToggleLofiMode,
                testTagPrefix = "full_player_mode"
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Seek Progress Bar with Timestamps
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

            Spacer(modifier = Modifier.height(10.dp))

            // Main Playback Controls (Shuffle, Previous, Large Play/Pause, Next, Repeat)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
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
                        .testTag("previous_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = TextPrimary,
                        modifier = Modifier.size(34.dp)
                    )
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(ElectricPurple, NeonCyan)
                            )
                        )
                        .clickable { onTogglePlayPause() }
                        .testTag("play_pause_main_button")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                IconButton(
                    onClick = onSkipNext,
                    modifier = Modifier
                        .size(52.dp)
                        .testTag("next_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = TextPrimary,
                        modifier = Modifier.size(34.dp)
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

            // Subtle Minimal Bottom Pill Bar for Artwork / Lyrics / Queue / Sleep Timer / AI Mood
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                val subViews = listOf(
                    Triple(0, "Artwork", Icons.Default.Album),
                    Triple(1, "Lyrics", Icons.Default.Lyrics),
                    Triple(2, "Queue (${queue.size})", Icons.AutoMirrored.Filled.QueueMusic),
                    Triple(3, "Timer", Icons.Default.Bedtime)
                )
                for ((idx, label, icon) in subViews) {
                    val selected = activeSubTab == idx
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (selected) GlassSurface else Color.Transparent)
                            .clickable { activeSubTab = idx }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = if (selected) NeonCyan else TextMuted,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) TextPrimary else TextMuted
                        )
                    }
                }

                // AI Mood Screen quick entry
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(ElectricPurple.copy(alpha = 0.22f))
                        .clickable {
                            onClose()
                            onOpenMoodScreen()
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("now_playing_open_mood_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI Mood",
                        tint = NeonCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Mood",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonCyan
                    )
                }
            }
        }
    }
}

