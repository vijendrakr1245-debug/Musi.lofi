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
        color = ElevatedCharcoal.copy(alpha = 0.97f),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        tonalElevation = 8.dp,
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = SubtleBorder,
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            )
            .clickable { onExpandNowPlaying() }
            .testTag("mini_player_bar")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Thin progress bar
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
                                if (dspParams.isLiveLofiEnabled) listOf(ElectricPurple, NeonCyan)
                                else listOf(StudioEmerald, NeonCyan)
                            )
                        )
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                RotatingVinylArtwork(
                    albumArtUri = track.albumArtUri,
                    isPlaying = isPlaying,
                    isLofiMode = dspParams.isLiveLofiEnabled,
                    size = 46.dp
                )

                Spacer(modifier = Modifier.width(10.dp))

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
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (dspParams.isLiveLofiEnabled) ElectricPurple.copy(alpha = 0.25f)
                                    else StudioEmerald.copy(alpha = 0.25f)
                                )
                                .clickable { onToggleLofiMode(!dspParams.isLiveLofiEnabled) }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                .testTag("mini_player_mode_badge")
                        ) {
                            Text(
                                text = if (dspParams.isLiveLofiEnabled) "LO-FI" else "HI-FI",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (dspParams.isLiveLofiEnabled) NeonCyan else StudioEmerald
                            )
                        }
                    }
                    Text(
                        text = "${track.artist} • ${dspParams.presetName}",
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
 * Full-Screen Now Playing Screen with Rotating Vinyl, Real-Time Audio Visualizer,
 * Hi-Fi / Live Lo-Fi Switch, Playback Speed, Lyrics, Queue, and Sleep Timer.
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
    onOpenLofiStudio: () -> Unit
) {
    BackHandler(onBack = onClose)

    // 0 = Player & Visualizer, 1 = Lyrics/Studio Notes, 2 = Queue, 3 = Sleep Timer & Speed
    var activeSubTab by remember { mutableIntStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                         Color(0xFF1A102E),
                        DeepObsidian,
                        DeepObsidian
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag("fullscreen_now_playing")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            // Top Header
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
                        text = "${track.formatBadge} • ${if (dspParams.isLiveLofiEnabled) "LIVE LO-FI DSP" else "HI-FI ORIGINAL"}",
                        style = MaterialTheme.typography.labelSmall,
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

            Spacer(modifier = Modifier.height(6.dp))

            // Instant Hi-Fi / Live Lo-Fi Switch Banner
            HifiLofiModeToggleBanner(
                dspParams = dspParams,
                onToggleMode = onToggleLofiMode,
                onOpenStudio = {
                    onClose()
                    onOpenLofiStudio()
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Sub-view Switcher (Vinyl & Visualizer / Lyrics / Queue / Speed & Sleep)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val tabs = listOf(
                    Triple(0, "Vinyl & DSP", Icons.Default.Album),
                    Triple(1, "Lyrics", Icons.Default.Lyrics),
                    Triple(2, "Queue (${queue.size})", Icons.AutoMirrored.Filled.QueueMusic),
                    Triple(3, "Speed/Timer", Icons.Default.Speed)
                )
                for ((idx, label, icon) in tabs) {
                    FilterChip(
                        selected = activeSubTab == idx,
                        onClick = { activeSubTab = idx },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ElectricPurple.copy(alpha = 0.28f),
                            selectedLabelColor = TextPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Center Dynamic Content Area
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
                            verticalArrangement = Arrangement.SpaceEvenly,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            RotatingVinylArtwork(
                                albumArtUri = track.albumArtUri,
                                isPlaying = isPlaying,
                                isLofiMode = dspParams.isLiveLofiEnabled,
                                size = 210.dp
                            )

                            RealTimeAudioVisualizer(
                                snapshot = visualizerSnapshot,
                                isPlaying = isPlaying,
                                isLofiMode = dspParams.isLiveLofiEnabled,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(92.dp)
                            )
                        }
                    }
                    1 -> {
                        Surface(
                            color = ElevatedCharcoal,
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .border(1.dp, SubtleBorder, RoundedCornerShape(18.dp))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Text(
                                    text = "SYNCED LYRICS & STUDIO DSP NOTES",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = NeonCyan
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = track.lyricsOrNotes.ifBlank {
                                        "Playing ${track.title} by ${track.artist}.\nReal-time PCM audio processing is active."
                                    },
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                    2 -> {
                        Surface(
                            color = ElevatedCharcoal,
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .border(1.dp, SubtleBorder, RoundedCornerShape(18.dp))
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
                                            .clip(RoundedCornerShape(12.dp))
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
                                                text = "${qTrack.artist} • ${qTrack.formatBadge}",
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
                            color = ElevatedCharcoal,
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .border(1.dp, SubtleBorder, RoundedCornerShape(18.dp))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Text(
                                    text = "PLAYBACK SPEED & TAPE PITCH (${String.format(Locale.US, "%.2fx", dspParams.playbackSpeed)})",
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
                                    listOf(0.85f to "0.85x Slowed", 0.94f to "0.94x Lo-Fi", 1.0f to "1.0x Normal", 1.15f to "1.15x Nightcore").forEach { (spd, lbl) ->
                                        FilterChip(
                                            selected = kotlin.math.abs(dspParams.playbackSpeed - spd) < 0.02f,
                                            onClick = { onSpeedChange(spd) },
                                            label = { Text(lbl, style = MaterialTheme.typography.labelSmall) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Bedtime, contentDescription = null, tint = AnalogAmber)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (sleepTimerRemainingSec != null) {
                                            "SLEEP TIMER ACTIVE: ${sleepTimerRemainingSec / 60}m ${sleepTimerRemainingSec % 60}s"
                                        } else {
                                            "SLEEP TIMER (AUTO-PAUSE)"
                                        },
                                        style = MaterialTheme.typography.labelLarge,
                                        color = AnalogAmber
                                    )
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    listOf(0 to "Off", 5 to "5m", 15 to "15m", 30 to "30m", 60 to "60m").forEach { (mins, text) ->
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

            Spacer(modifier = Modifier.height(10.dp))

            // Track Metadata & Favorite Row
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
                        contentDescription = "Toggle Favorite",
                        tint = if (isFavorite) VinylRose else TextSecondary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Seek Bar & Timestamps
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

            Spacer(modifier = Modifier.height(6.dp))

            // Main Transport Controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
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
                        .size(68.dp)
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

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
