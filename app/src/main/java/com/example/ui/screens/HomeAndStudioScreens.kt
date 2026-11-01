package com.example.ui.screens

import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.PlaybackHistoryEntity
import com.example.model.AiMoodRecommendation
import com.example.model.BuiltInLofiPresets
import com.example.model.LibraryFilterTab
import com.example.model.LofiDspParameters
import com.example.model.LofiPreset
import com.example.model.MusicMoodAnalysisResult
import com.example.model.TrackItem
import com.example.model.VisualizerSnapshot
import com.example.ui.components.HifiLofiModeToggleBanner
import com.example.ui.components.OriginalLofiSegmentedControl
import com.example.ui.components.RealTimeAudioVisualizer
import com.example.ui.components.formatDurationMillis
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

private data class HomeMoodCardSpec(
    val presetId: String,
    val displayTitle: String,
    val subtitle: String,
    val icon: ImageVector,
    val accent: Color
)

@Composable
fun HomeScreen(
    allTracks: List<TrackItem>,
    favoriteIds: Set<String>,
    history: List<PlaybackHistoryEntity>,
    presets: List<LofiPreset>,
    dspParams: LofiDspParameters,
    currentTrack: TrackItem?,
    isPlaying: Boolean,
    currentPositionMs: Long = 0L,
    durationMs: Long = 0L,
    moodResult: MusicMoodAnalysisResult? = null,
    onPlayTrack: (TrackItem) -> Unit,
    onTogglePlayPause: () -> Unit = {},
    onSkipPrevious: () -> Unit = {},
    onSkipNext: () -> Unit = {},
    onSeekTo: (Long) -> Unit = {},
    onToggleFavorite: (String) -> Unit,
    onSelectPreset: (LofiPreset) -> Unit,
    onToggleLofiMode: (Boolean) -> Unit,
    onApplyAiRecommendation: (AiMoodRecommendation) -> Unit,
    onOpenMoodScreen: () -> Unit = {},
    onApplyMoodPreset: (LofiPreset) -> Unit = {},
    onImportSongsClick: () -> Unit,
    onScanDeviceClick: () -> Unit,
    onNavigateToStudio: () -> Unit,
    onOpenSettings: () -> Unit = {},
    onOpenLibraryCollection: (LibraryFilterTab) -> Unit = {},
    onExpandNowPlaying: () -> Unit
) {
    val activeTrack = currentTrack ?: allTracks.firstOrNull()
    val isFavorite = activeTrack?.id in favoriteIds
    val effectiveDuration = if (durationMs > 0L) durationMs else (activeTrack?.durationMs ?: 180_000L)
    val safeDuration = effectiveDuration.coerceAtLeast(1L)

    val playButtonScale by animateFloatAsState(
        targetValue = if (isPlaying) 1.04f else 1.0f,
        animationSpec = tween(durationMillis = 280),
        label = "home_play_scale"
    )

    val homeMoodSpecs = remember {
        listOf(
            HomeMoodCardSpec(
                presetId = "soft_lofi",
                displayTitle = "Soft Lo-Fi",
                subtitle = "Warm tape & gentle low-pass",
                icon = Icons.Default.Spa,
                accent = ElectricPurpleBright
            ),
            HomeMoodCardSpec(
                presetId = "rainy_night",
                displayTitle = "Rainy Night",
                subtitle = "Cozy rain & midnight warmth",
                icon = Icons.Default.WaterDrop,
                accent = NeonCyan
            ),
            HomeMoodCardSpec(
                presetId = "dreamy_lofi",
                displayTitle = "Dreamy",
                subtitle = " Lush stereo air & drift",
                icon = Icons.Default.AutoAwesome,
                accent = VinylRose
            ),
            HomeMoodCardSpec(
                presetId = "deep_lofi",
                displayTitle = "Deep Bass",
                subtitle = "Rich sub-bass & dark filter",
                icon = Icons.Default.NightlightRound,
                accent = NeonBlue
            )
        )
    }

    LazyColumn(
        contentPadding = PaddingValues(top = 12.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(26.dp),
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        DeepObsidian,
                        Color(0xFF121029),
                        DeepObsidian
                    )
                )
            )
            .padding(horizontal = 20.dp)
            .testTag("home_screen")
    ) {
        // 1. TOP SECTION: LOFI WAVE Logo, "Your Music, Your Mood", Settings Icon
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(ElectricPurple, NeonCyan)
                                )
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "LOFI WAVE Logo",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "LOFI WAVE",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Your Music, Your Mood",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(ElevatedCharcoal)
                        .border(1.dp, SubtleBorder, CircleShape)
                        .testTag("home_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // 2. MAIN SECTION: Cinematic Anime Lo-Fi Artwork + Current Song + Controls + Progress + Favorite
        item {
            Surface(
                color = ElevatedCharcoal.copy(alpha = 0.88f),
                shape = RoundedCornerShape(30.dp),
                tonalElevation = 10.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                ElectricPurple.copy(alpha = 0.55f),
                                NeonCyan.copy(alpha = 0.35f),
                                SubtleBorder.copy(alpha = 0.5f)
                            )
                        ),
                        shape = RoundedCornerShape(30.dp)
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    // Large Cinematic Anime Lo-Fi Artwork
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(225.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .border(1.dp, SubtleBorder.copy(alpha = 0.7f), RoundedCornerShape(22.dp))
                            .clickable {
                                if (currentTrack != null) {
                                    onExpandNowPlaying()
                                } else if (activeTrack != null) {
                                    onPlayTrack(activeTrack)
                                }
                            }
                            .testTag("home_hero_artwork")
                    ) {
                        if (!activeTrack?.albumArtUri.isNullOrBlank()) {
                            AsyncImage(
                                model = Uri.parse(activeTrack?.albumArtUri),
                                contentDescription = "Anime Lo-Fi Artwork",
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

                        // Subtle Twilight Gradient Overlay
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            DeepObsidian.copy(alpha = 0.15f),
                                            Color.Transparent,
                                            DeepObsidian.copy(alpha = 0.78f)
                                        )
                                    )
                                )
                        )

                        // Active Sound Mode Pill Overlay
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(DeepObsidian.copy(alpha = 0.72f))
                                    .border(
                                        1.dp,
                                        if (dspParams.isLiveLofiEnabled) ElectricPurple else NeonCyan,
                                        RoundedCornerShape(50)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 5.dp)
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

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(DeepObsidian.copy(alpha = 0.68f))
                                    .clickable { onNavigateToStudio() }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                                    .testTag("home_open_studio_button")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = "Studio",
                                        tint = NeonCyan,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Studio",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = NeonCyan
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Current Song Title, Artist & Favorite Button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    if (currentTrack != null) onExpandNowPlaying()
                                }
                        ) {
                            Text(
                                text = activeTrack?.title ?: "Select a Song to Begin",
                                style = MaterialTheme.typography.headlineMedium,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = activeTrack?.let { "${it.artist} • ${it.album}" }
                                    ?: "Import or tap any track below",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        IconButton(
                            onClick = {
                                activeTrack?.let { onToggleFavorite(it.id) }
                            },
                            modifier = Modifier.testTag("home_favorite_button")
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (isFavorite) VinylRose else TextSecondary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Song Progress Slider & Duration Timestamps
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
                            .testTag("home_seek_slider")
                    )

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = formatDurationMillis(currentPositionMs),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        Text(
                            text = formatDurationMillis(safeDuration),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Previous, Large Play/Pause, and Next Controls
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(
                            onClick = onSkipPrevious,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(GlassSurface.copy(alpha = 0.65f))
                                .testTag("home_previous_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = "Previous",
                                tint = TextPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(24.dp))

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(66.dp)
                                .scale(playButtonScale)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(ElectricPurple, NeonCyan)
                                    )
                                )
                                .clickable {
                                    if (currentTrack != null) {
                                        onTogglePlayPause()
                                    } else if (activeTrack != null) {
                                        onPlayTrack(activeTrack)
                                    }
                                }
                                .testTag("home_play_pause_button")
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(34.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(24.dp))

                        IconButton(
                            onClick = onSkipNext,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(GlassSurface.copy(alpha = 0.65f))
                                .testTag("home_next_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Next",
                                tint = TextPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
        }

        // 3. SOUND MODE: Two-State Control (ORIGINAL / LO-FI)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "SOUND MODE",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextSecondary
                    )
                    Text(
                        text = if (dspParams.isLiveLofiEnabled) "Real-Time Lo-Fi Active" else "Pure Original Audio",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (dspParams.isLiveLofiEnabled) NeonCyan else TextMuted
                    )
                }

                OriginalLofiSegmentedControl(
                    isLofiEnabled = dspParams.isLiveLofiEnabled,
                    activePresetName = dspParams.presetName,
                    onToggleMode = onToggleLofiMode,
                    testTagPrefix = "home_mode"
                )
            }
        }

        // 4. LO-FI MOODS: Soft Lo-Fi, Rainy Night, Dreamy, Deep Bass (Compact Horizontal Carousel)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "LO-FI MOODS",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextSecondary
                    )
                    Text(
                        text = "Fine-Tune DSP →",
                        style = MaterialTheme.typography.labelSmall,
                        color = ElectricPurpleBright,
                        modifier = Modifier.clickable { onNavigateToStudio() }
                    )
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(homeMoodSpecs, key = { it.presetId }) { moodSpec ->
                        val matchedPreset = presets.find { it.id == moodSpec.presetId }
                        val isSelected = dspParams.isLiveLofiEnabled && dspParams.presetId == moodSpec.presetId
                        val cardBg by animateColorAsState(
                            targetValue = if (isSelected) moodSpec.accent.copy(alpha = 0.22f) else ElevatedCharcoal,
                            animationSpec = tween(durationMillis = 220),
                            label = "mood_bg_${moodSpec.presetId}"
                        )
                        val borderColor by animateColorAsState(
                            targetValue = if (isSelected) moodSpec.accent else SubtleBorder,
                            animationSpec = tween(durationMillis = 220),
                            label = "mood_border_${moodSpec.presetId}"
                        )

                        Surface(
                            color = cardBg,
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .width(152.dp)
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = borderColor,
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .clickable {
                                    if (matchedPreset != null) {
                                        onSelectPreset(matchedPreset)
                                    }
                                }
                                .testTag("home_preset_${moodSpec.presetId}")
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(moodSpec.accent.copy(alpha = 0.20f))
                                ) {
                                    Icon(
                                        imageVector = moodSpec.icon,
                                        contentDescription = moodSpec.displayTitle,
                                        tint = moodSpec.accent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = moodSpec.displayTitle,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = moodSpec.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4B. AI MUSIC MOOD DETECTION (On-Device Acoustic Mood & Smart Preset Suggestion)
        item {
            val suggestedPreset = remember(moodResult, presets) {
                moodResult?.suggestedPreset
                    ?: presets.find { it.id == "soft_lofi" }
                    ?: BuiltInLofiPresets.ALL.first { it.id == "soft_lofi" }
            }
            val isSuggestedAlreadyActive = dspParams.isLiveLofiEnabled && dspParams.presetId == suggestedPreset.id

            Surface(
                color = ElevatedCharcoal.copy(alpha = 0.90f),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                ElectricPurple.copy(alpha = 0.65f),
                                NeonCyan.copy(alpha = 0.50f)
                            )
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .clickable { onOpenMoodScreen() }
                    .testTag("home_ai_mood_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "AI Music Mood Detection",
                                tint = NeonCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "AI MUSIC MOOD DETECTION",
                                style = MaterialTheme.typography.labelMedium,
                                color = NeonCyan
                            )
                        }

                        if (moodResult != null && moodResult.isSignalSufficient && moodResult.confidencePercent != null) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(ElectricPurple.copy(alpha = 0.22f))
                                    .border(1.dp, ElectricPurpleBright.copy(alpha = 0.55f), RoundedCornerShape(50))
                                    .padding(horizontal = 9.dp, vertical = 2.dp)
                                    .testTag("home_mood_confidence_badge")
                            ) {
                                Text(
                                    text = "${moodResult.confidencePercent}% Match",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextPrimary
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (moodResult != null) {
                                    "${moodResult.detectedMood.displayName} ${moodResult.detectedMood.emoji}"
                                } else {
                                    "Tap to Analyze Song Mood ✨"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (moodResult != null) {
                                    "Suggested Preset: ${moodResult.detectedMood.primaryPresetLabel}"
                                } else {
                                    "100% On-Device PCM Acoustic Analysis"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        if (moodResult != null && !moodResult.isSuggestionIgnored) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(
                                        if (isSuggestedAlreadyActive) {
                                            StudioEmerald.copy(alpha = 0.22f)
                                        } else {
                                            ElectricPurple.copy(alpha = 0.85f)
                                        }
                                    )
                                    .border(
                                        1.dp,
                                        if (isSuggestedAlreadyActive) StudioEmerald else NeonCyan.copy(alpha = 0.7f),
                                        RoundedCornerShape(50)
                                    )
                                    .clickable {
                                        if (!isSuggestedAlreadyActive) {
                                            onApplyMoodPreset(suggestedPreset)
                                        } else {
                                            onOpenMoodScreen()
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                                    .testTag("home_apply_mood_preset_button")
                            ) {
                                Text(
                                    text = if (isSuggestedAlreadyActive) "Active ✓" else "Apply Preset",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSuggestedAlreadyActive) StudioEmerald else Color.White
                                )
                            }
                        } else {
                            Text(
                                text = "Details →",
                                style = MaterialTheme.typography.labelSmall,
                                color = ElectricPurpleBright
                            )
                        }
                    }
                }
            }
        }

        // 5. MUSIC COLLECTION: Recently Played, All Songs, Favorites, Add Music
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "MUSIC COLLECTION",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextSecondary
                )

                val favCount = allTracks.count { it.id in favoriteIds }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CollectionMinimalCard(
                        title = "Recently Played",
                        subtitle = "${history.size} tracks",
                        icon = Icons.Default.History,
                        accent = NeonCyan,
                        onClick = { onOpenLibraryCollection(LibraryFilterTab.RECENTLY_PLAYED) },
                        testTag = "collection_recently_played",
                        modifier = Modifier.weight(1f)
                    )
                    CollectionMinimalCard(
                        title = "All Songs",
                        subtitle = "${allTracks.size} tracks",
                        icon = Icons.Default.LibraryMusic,
                        accent = ElectricPurpleBright,
                        onClick = { onOpenLibraryCollection(LibraryFilterTab.ALL_SONGS) },
                        testTag = "collection_all_songs",
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CollectionMinimalCard(
                        title = "Favorites",
                        subtitle = "$favCount liked",
                        icon = Icons.Default.Favorite,
                        accent = VinylRose,
                        onClick = { onOpenLibraryCollection(LibraryFilterTab.FAVORITES) },
                        testTag = "collection_favorites",
                        modifier = Modifier.weight(1f)
                    )
                    CollectionMinimalCard(
                        title = "Add Music",
                        subtitle = "Import audio",
                        icon = Icons.Default.Add,
                        accent = StudioEmerald,
                        onClick = onImportSongsClick,
                        testTag = "home_import_songs_button",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 6. CLEAN TRACK LIST PREVIEW (Uncrowded with Generous Whitespace)
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "YOUR TRACKS",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextSecondary
                )
                Text(
                    text = "Scan Device",
                    style = MaterialTheme.typography.labelSmall,
                    color = NeonCyan,
                    modifier = Modifier.clickable { onScanDeviceClick() }
                )
            }
        }

        items(allTracks.take(8), key = { it.id }) { track ->
            TrackRowCard(
                track = track,
                isCurrent = currentTrack?.id == track.id,
                isPlaying = isPlaying,
                isFavorite = track.id in favoriteIds,
                onPlay = { onPlayTrack(track) },
                onToggleFavorite = { onToggleFavorite(track.id) }
            )
        }
    }
}

@Composable
private fun CollectionMinimalCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = ElevatedCharcoal,
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
            .border(1.dp, SubtleBorder, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = 0.18f))
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accent,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun TrackRowCard(
    track: TrackItem,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isFavorite: Boolean,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    trailingAction: (@Composable () -> Unit)? = null
) {
    Surface(
        color = if (isCurrent) ElectricPurple.copy(alpha = 0.20f) else ElevatedCharcoal,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (isCurrent) NeonCyan else SubtleBorder,
                shape = RoundedCornerShape(18.dp)
            )
            .clickable { onPlay() }
            .testTag("track_card_${track.id}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 11.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(
                        if (isCurrent) ElectricPurple else GlassSurface
                    )
            ) {
                Icon(
                    imageVector = if (isCurrent && isPlaying) Icons.Default.GraphicEq else Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = if (isCurrent) Color.White else NeonCyan
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isCurrent) NeonCyan else TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(GlassSurface)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = track.formatBadge,
                            style = MaterialTheme.typography.labelSmall,
                            color = ElectricPurpleBright
                        )
                    }
                }
                Text(
                    text = "${track.artist} • ${formatDurationMillis(track.durationMs)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (isFavorite) VinylRose else TextMuted,
                    modifier = Modifier.size(22.dp)
                )
            }

            trailingAction?.invoke()
        }
    }
}


/**
 * PHASE 3, 4, 5 & 6: THE REAL-TIME LO-FI STUDIO SCREEN (Main Highlight of LOFI WAVE).
 */
@Composable
fun LofiStudioScreen(
    dspParams: LofiDspParameters,
    presets: List<LofiPreset>,
    visualizerSnapshot: VisualizerSnapshot,
    isPlaying: Boolean,
    onToggleLofiMode: (Boolean) -> Unit,
    onSelectPreset: (LofiPreset) -> Unit,
    onUpdateDsp: ((LofiDspParameters) -> LofiDspParameters) -> Unit,
    onSaveCustomPreset: (String, String, String) -> Unit,
    onDeleteCustomPreset: (String) -> Unit
) {
    var showSaveDialog by remember { mutableStateOf(false) }
    var customName by remember { mutableStateOf("") }
    var customSubtitle by remember { mutableStateOf("Custom Studio DSP Configuration") }
    var customCategory by remember { mutableStateOf("Custom Studio") }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            containerColor = ElevatedCharcoal,
            title = {
                Text("Save Custom Lo-Fi Preset", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = customName,
                        onValueChange = { customName = it },
                        label = { Text("Preset Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = customSubtitle,
                        onValueChange = { customSubtitle = it },
                        label = { Text("Description / Notes") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = customCategory,
                        onValueChange = { customCategory = it },
                        label = { Text("Mood Tag (e.g., Late Night)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveCustomPreset(customName, customSubtitle, customCategory)
                        customName = ""
                        showSaveDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple)
                ) {
                    Text("Save Preset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    LazyColumn(
        contentPadding = PaddingValues(bottom = 115.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("lofi_studio_screen")
    ) {
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "LO-FI DSP STUDIO",
                        style = MaterialTheme.typography.headlineLarge,
                        color = TextPrimary
                    )
                    Text(
                        text = "16-Stage Real-Time PCM Audio Processing Rack",
                        style = MaterialTheme.typography.bodySmall,
                        color = NeonCyan
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            val defaultPreset = BuiltInLofiPresets.ALL.first { it.id == "soft_lofi" }
                            onSelectPreset(defaultPreset)
                        },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("reset_to_default_button")
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = "Reset to Default", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset Default", style = MaterialTheme.typography.labelSmall)
                    }
                    Button(
                        onClick = { showSaveDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("save_custom_preset_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Save Preset", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save Preset", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        // Instant Original / Lo-Fi A-B Switch + Effects Bypass
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                HifiLofiModeToggleBanner(
                    dspParams = dspParams,
                    onToggleMode = onToggleLofiMode,
                    onOpenStudio = {}
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val isBypassed = !dspParams.isLiveLofiEnabled || dspParams.presetId == "original"
                    Button(
                        onClick = { onToggleLofiMode(!dspParams.isLiveLofiEnabled) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (dspParams.isLiveLofiEnabled) ElectricPurple else StudioEmerald
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ab_switch_original_lofi")
                    ) {
                        Text(
                            text = if (dspParams.isLiveLofiEnabled) "A/B: LIVE LO-FI ON" else "A/B: ORIGINAL HI-FI",
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1
                        )
                    }

                    OutlinedButton(
                        onClick = { onToggleLofiMode(isBypassed) },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("effects_bypass_toggle")
                    ) {
                        Text(
                            text = if (isBypassed) "EFFECTS BYPASSED (OFF)" else "BYPASS ALL EFFECTS",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isBypassed) StudioEmerald else NeonCyan,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // Real-Time Visualizer + Phase 6 Audio Quality Telemetry (Peak, RMS, Clipping, DC Offset, Balance, Underruns, Latency)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                RealTimeAudioVisualizer(
                    snapshot = visualizerSnapshot,
                    isPlaying = isPlaying,
                    isLofiMode = dspParams.isLiveLofiEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(104.dp)
                )

                Surface(
                    color = ElevatedCharcoal,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SubtleBorder, RoundedCornerShape(12.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "PEAK: ${String.format(Locale.US, "%.1f dB", visualizerSnapshot.peakDb)} • CLIPS: ${visualizerSnapshot.clippingCount}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (visualizerSnapshot.clippingCount == 0) StudioEmerald else VinylRose
                            )
                            Text(
                                text = "LATENCY: ${String.format(Locale.US, "%.1f ms", visualizerSnapshot.bufferLatencyMs)} • UNDERRUNS: ${visualizerSnapshot.underrunCount}",
                                style = MaterialTheme.typography.labelSmall,
                                color = AnalogAmber
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "DC OFFSET: ${String.format(Locale.US, "%+.4f", visualizerSnapshot.dcOffsetLeft)} • BAL: ${String.format(Locale.US, "%+.1f dB", visualizerSnapshot.channelBalanceDb)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = NeonCyan
                            )
                            Text(
                                text = if (dspParams.isLiveLofiEnabled && dspParams.presetId != "original") {
                                    "LPF ${visualizerSnapshot.activeLowPassHz}Hz • ${String.format(Locale.US, "%.1f-BIT", visualizerSnapshot.activeBitDepth)}"
                                } else {
                                    "ORIGINAL HI-FI BYPASS (16-BIT)"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (dspParams.isLiveLofiEnabled) StudioEmerald else TextSecondary
                            )
                        }
                        LinearProgressIndicator(
                            progress = { (visualizerSnapshot.rmsLeft * 2.2f).coerceIn(0.02f, 1f) },
                            color = ElectricPurple,
                            trackColor = DeepObsidian,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                    }
                }

                // Core Tri-Effect Quick Focus Audition Strip (Gentle Low-Pass, Anti-Aliased Bitcrusher, Subtle Vinyl)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = {
                            onUpdateDsp {
                                it.copy(
                                    isLiveLofiEnabled = true,
                                    presetId = "custom_lpf",
                                    presetName = "Warm 4.5kHz Low-Pass",
                                    lowPassCutoffHz = 4500f,
                                    highFreqReductionDb = 2.0f
                                )
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("core_focus_lowpass_button")
                    ) {
                        Text("4.5kHz Low-Pass", style = MaterialTheme.typography.labelSmall, color = NeonCyan, maxLines = 1)
                    }

                    OutlinedButton(
                        onClick = {
                            onUpdateDsp {
                                it.copy(
                                    isLiveLofiEnabled = true,
                                    presetId = "custom_bitcrush",
                                    presetName = "12-Bit Vintage Sampler",
                                    bitDepth = 12.0f,
                                    sampleRateDivisor = 1.6f
                                )
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("core_focus_bitcrusher_button")
                    ) {
                        Text("12-Bit Sampler", style = MaterialTheme.typography.labelSmall, color = VinylRose, maxLines = 1)
                    }

                    OutlinedButton(
                        onClick = {
                            onUpdateDsp {
                                it.copy(
                                    isLiveLofiEnabled = true,
                                    presetId = "vinyl_classic",
                                    presetName = "Vinyl Classic",
                                    vinylCrackle = 0.16f,
                                    vinylNoise = 0.14f,
                                    wowFlutterDepth = 0.12f
                                )
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("core_focus_vinyl_button")
                    ) {
                        Text("Subtle Vinyl", style = MaterialTheme.typography.labelSmall, color = AnalogAmber, maxLines = 1)
                    }
                }
            }
        }

        // Preset Selector Carousel (Original + Soft Lo-Fi + Deep Lo-Fi + Vinyl Classic + Rainy Night + Dreamy Lo-Fi + Cassette Tape + Custom)
        item {
            Column {
                Text(
                    text = "PROFESSIONAL & CUSTOM LO-FI PRESETS",
                    style = MaterialTheme.typography.labelLarge,
                    color = NeonCyan
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(presets, key = { it.id }) { preset ->
                        val selected = if (preset.id == "original") {
                            !dspParams.isLiveLofiEnabled || dspParams.presetId == "original"
                        } else {
                            dspParams.isLiveLofiEnabled && dspParams.presetId == preset.id
                        }
                        Surface(
                            color = if (selected) ElectricPurple.copy(alpha = 0.28f) else ElevatedCharcoal,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .width(165.dp)
                                .border(
                                    width = if (selected) 1.5.dp else 1.dp,
                                    color = if (selected) NeonCyan else SubtleBorder,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable { onSelectPreset(preset) }
                                .testTag("studio_preset_${preset.id}")
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = if (preset.isCustom) "CUSTOM" else preset.moodCategory.uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (preset.isCustom) AnalogAmber else NeonCyan,
                                        maxLines = 1
                                    )
                                    if (preset.isCustom) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Custom Preset",
                                            tint = VinylRose,
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clickable { onDeleteCustomPreset(preset.id) }
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = preset.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = preset.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // RACK 1: Phase 5 Primary User Controls (Master Volume, Bass, Treble, Warmth, Noise, Reverb, Bitcrusher)
        item {
            StudioRackSection(
                title = "1. ESSENTIAL STUDIO CONTROLS",
                subtitle = "Master volume, bass, treble, tape warmth, noise, reverb & anti-aliased bitcrusher"
            ) {
                DspParameterSlider(
                    label = "Master Effect Intensity (Wet/Dry Mix)",
                    valueText = "${(dspParams.masterIntensity * 100).toInt()}%",
                    value = dspParams.masterIntensity,
                    valueRange = 0f..1f,
                    accentColor = NeonCyan,
                    testTag = "slider_master_intensity",
                    onValueChange = { v -> onUpdateDsp { it.copy(masterIntensity = v, isLiveLofiEnabled = true) } }
                )
                DspParameterSlider(
                    label = "Master Volume (Slewed Output Gain)",
                    valueText = String.format(Locale.US, "%+.1f dB", dspParams.masterGainDb),
                    value = dspParams.masterGainDb,
                    valueRange = -12f..6f,
                    accentColor = StudioEmerald,
                    testTag = "slider_master_gain_db",
                    onValueChange = { v -> onUpdateDsp { it.copy(masterGainDb = v) } }
                )
                DspParameterSlider(
                    label = "Bass Control (Controlled 105Hz Shelf)",
                    valueText = String.format(Locale.US, "%+.1f dB", dspParams.warmBassBoostDb),
                    value = dspParams.warmBassBoostDb,
                    valueRange = -6f..6f,
                    accentColor = ElectricPurple,
                    testTag = "slider_warm_bass",
                    onValueChange = { v -> onUpdateDsp { it.copy(warmBassBoostDb = v, isLiveLofiEnabled = true) } }
                )
                DspParameterSlider(
                    label = "Treble Control (4.5kHz High Shelf)",
                    valueText = String.format(Locale.US, "%+.1f dB", dspParams.trebleGainDb),
                    value = dspParams.trebleGainDb,
                    valueRange = -8f..6f,
                    accentColor = ElectricPurple,
                    testTag = "slider_treble_gain",
                    onValueChange = { v -> onUpdateDsp { it.copy(trebleGainDb = v, isLiveLofiEnabled = true) } }
                )
                DspParameterSlider(
                    label = "Warmth Control (Analog Tape Saturation)",
                    valueText = "${(dspParams.analogSaturation * 100).toInt()}%",
                    value = dspParams.analogSaturation,
                    valueRange = 0f..1f,
                    accentColor = AnalogAmber,
                    testTag = "slider_warmth_control",
                    onValueChange = { v -> onUpdateDsp { it.copy(analogSaturation = v, isLiveLofiEnabled = true) } }
                )
                DspParameterSlider(
                    label = "Noise Intensity (Subtle Vinyl & Tape Floor)",
                    valueText = "${(dspParams.noiseIntensity * 100).toInt()}%",
                    value = dspParams.noiseIntensity,
                    valueRange = 0f..1f,
                    accentColor = AnalogAmber,
                    testTag = "slider_noise_intensity",
                    onValueChange = { v ->
                        onUpdateDsp {
                            it.copy(
                                vinylNoise = v,
                                vinylCrackle = (v * 0.85f).coerceIn(0f, 1f),
                                tapeHiss = (v * 0.35f).coerceIn(0f, 1f),
                                isLiveLofiEnabled = true
                            )
                        }
                    }
                )
                DspParameterSlider(
                    label = "Reverb Intensity (Natural Stereo Room)",
                    valueText = "${(dspParams.reverbMix * 100).toInt()}%",
                    value = dspParams.reverbMix,
                    valueRange = 0f..1f,
                    accentColor = NeonCyan,
                    testTag = "slider_reverb_intensity",
                    onValueChange = { v -> onUpdateDsp { it.copy(reverbMix = v, isLiveLofiEnabled = true) } }
                )
                DspParameterSlider(
                    label = "Bitcrusher Intensity (Anti-Aliased)",
                    valueText = "${(dspParams.bitcrusherIntensity * 100).toInt()}%",
                    value = dspParams.bitcrusherIntensity,
                    valueRange = 0f..1f,
                    accentColor = VinylRose,
                    testTag = "slider_bitcrusher_intensity",
                    onValueChange = { v ->
                        val depth = (16.0f - v * 10.0f).coerceIn(6f, 16f)
                        val divisor = (1.0f + v * 3.0f).coerceIn(1f, 4f)
                        onUpdateDsp {
                            it.copy(
                                bitDepth = depth,
                                sampleRateDivisor = divisor,
                                isLiveLofiEnabled = true
                            )
                        }
                    }
                )
            }
        }

        // RACK 2: Resonant Filters & High-Frequency Roll-Off
        item {
            StudioRackSection(
                title = "2. ANALOG FILTER BANK",
                subtitle = "2nd-order Biquad Low-Pass, High-Pass & High-Frequency damping"
            ) {
                DspParameterSlider(
                    label = "Adjustable Low-Pass Filter Cutoff",
                    valueText = "${dspParams.lowPassCutoffHz.toInt()} Hz",
                    value = dspParams.lowPassCutoffHz,
                    valueRange = 400f..16000f,
                    accentColor = NeonCyan,
                    testTag = "slider_low_pass",
                    onValueChange = { v -> onUpdateDsp { it.copy(lowPassCutoffHz = v) } }
                )
                DspParameterSlider(
                    label = "High-Pass Filter (Radio / Tape Body)",
                    valueText = "${dspParams.highPassCutoffHz.toInt()} Hz",
                    value = dspParams.highPassCutoffHz,
                    valueRange = 20f..550f,
                    accentColor = NeonCyan,
                    testTag = "slider_high_pass",
                    onValueChange = { v -> onUpdateDsp { it.copy(highPassCutoffHz = v) } }
                )
                DspParameterSlider(
                    label = "High-Frequency Reduction (Velvet Roll-Off)",
                    valueText = String.format(Locale.US, "-%.1f dB", dspParams.highFreqReductionDb),
                    value = dspParams.highFreqReductionDb,
                    valueRange = 0f..15f,
                    accentColor = AnalogAmber,
                    testTag = "slider_hf_reduction",
                    onValueChange = { v -> onUpdateDsp { it.copy(highFreqReductionDb = v) } }
                )
            }
        }

        // RACK 3: Vinyl Turntable & Magnetic Cassette Tape Modeling
        item {
            StudioRackSection(
                title = "3. VINYL TURNTABLE & CASSETTE TAPE ENGINE",
                subtitle = "Real-time dust crackle impulses, groove rumble, tape hiss & dual-LFO wow/flutter"
            ) {
                DspParameterSlider(
                    label = "Vinyl Dust Crackle",
                    valueText = "${(dspParams.vinylCrackle * 100).toInt()}%",
                    value = dspParams.vinylCrackle,
                    valueRange = 0f..1f,
                    accentColor = AnalogAmber,
                    testTag = "slider_vinyl_crackle",
                    onValueChange = { v -> onUpdateDsp { it.copy(vinylCrackle = v) } }
                )
                DspParameterSlider(
                    label = "Vinyl Surface Noise & Motor Rumble",
                    valueText = "${(dspParams.vinylNoise * 100).toInt()}%",
                    value = dspParams.vinylNoise,
                    valueRange = 0f..1f,
                    accentColor = AnalogAmber,
                    testTag = "slider_vinyl_noise",
                    onValueChange = { v -> onUpdateDsp { it.copy(vinylNoise = v) } }
                )
                DspParameterSlider(
                    label = "Magnetic Tape Hiss",
                    valueText = "${(dspParams.tapeHiss * 100).toInt()}%",
                    value = dspParams.tapeHiss,
                    valueRange = 0f..1f,
                    accentColor = AnalogAmber,
                    testTag = "slider_tape_hiss",
                    onValueChange = { v -> onUpdateDsp { it.copy(tapeHiss = v) } }
                )
                DspParameterSlider(
                    label = "Wow & Flutter Pitch Drift Depth",
                    valueText = "${(dspParams.wowFlutterDepth * 100).toInt()}%",
                    value = dspParams.wowFlutterDepth,
                    valueRange = 0f..1f,
                    accentColor = ElectricPurple,
                    testTag = "slider_wow_flutter_depth",
                    onValueChange = { v -> onUpdateDsp { it.copy(wowFlutterDepth = v) } }
                )
                DspParameterSlider(
                    label = "Wow & Flutter LFO Rate",
                    valueText = String.format(Locale.US, "%.2f Hz", dspParams.wowFlutterRateHz),
                    value = dspParams.wowFlutterRateHz,
                    valueRange = 0.3f..4.5f,
                    accentColor = ElectricPurple,
                    testTag = "slider_wow_flutter_rate",
                    onValueChange = { v -> onUpdateDsp { it.copy(wowFlutterRateHz = v) } }
                )
            }
        }

        // RACK 4: Vintage Sampler Bitcrusher & Tube Saturation
        item {
            StudioRackSection(
                title = "4. VINTAGE SAMPLER & ANALOG SATURATION",
                subtitle = "PCM Bit-depth quantization, sample-rate decimation & harmonic tube warmth"
            ) {
                DspParameterSlider(
                    label = "Bitcrusher / Bit-Depth Reduction",
                    valueText = String.format(Locale.US, "%.1f-bit", dspParams.bitDepth),
                    value = dspParams.bitDepth,
                    valueRange = 4f..16f,
                    accentColor = VinylRose,
                    testTag = "slider_bit_depth",
                    onValueChange = { v -> onUpdateDsp { it.copy(bitDepth = v) } }
                )
                DspParameterSlider(
                    label = "Sample-Rate Reduction (Decimator)",
                    valueText = String.format(Locale.US, "%.1fx", dspParams.sampleRateDivisor),
                    value = dspParams.sampleRateDivisor,
                    valueRange = 1f..8f,
                    accentColor = VinylRose,
                    testTag = "slider_sample_rate_div",
                    onValueChange = { v -> onUpdateDsp { it.copy(sampleRateDivisor = v) } }
                )
                DspParameterSlider(
                    label = "Warm Analog Saturation (Tube Harmonics)",
                    valueText = "${(dspParams.analogSaturation * 100).toInt()}%",
                    value = dspParams.analogSaturation,
                    valueRange = 0f..1f,
                    accentColor = AnalogAmber,
                    testTag = "slider_analog_saturation",
                    onValueChange = { v -> onUpdateDsp { it.copy(analogSaturation = v) } }
                )
                DspParameterSlider(
                    label = "Soft Asymmetric Distortion",
                    valueText = "${(dspParams.softDistortion * 100).toInt()}%",
                    value = dspParams.softDistortion,
                    valueRange = 0f..1f,
                    accentColor = VinylRose,
                    testTag = "slider_soft_distortion",
                    onValueChange = { v -> onUpdateDsp { it.copy(softDistortion = v) } }
                )
            }
        }

        // RACK 5: Stereo Space, Reverb, Dynamics & Tape Speed
        item {
            StudioRackSection(
                title = "5. REVERB, STEREO FIELD & DYNAMICS",
                subtitle = "Schroeder stereo reverb tank, Mid/Side stereo width, soft compression & speed"
            ) {
                DspParameterSlider(
                    label = "Stereo Reverb Wet Mix",
                    valueText = "${(dspParams.reverbMix * 100).toInt()}%",
                    value = dspParams.reverbMix,
                    valueRange = 0f..1f,
                    accentColor = NeonCyan,
                    testTag = "slider_reverb_mix",
                    onValueChange = { v -> onUpdateDsp { it.copy(reverbMix = v) } }
                )
                DspParameterSlider(
                    label = "Reverb Room Size & Decay",
                    valueText = "${(dspParams.reverbRoomSize * 100).toInt()}%",
                    value = dspParams.reverbRoomSize,
                    valueRange = 0.1f..0.98f,
                    accentColor = NeonCyan,
                    testTag = "slider_reverb_room",
                    onValueChange = { v -> onUpdateDsp { it.copy(reverbRoomSize = v) } }
                )
                DspParameterSlider(
                    label = "Stereo Width (Mid/Side Matrix)",
                    valueText = "${(dspParams.stereoWidth * 100).toInt()}%",
                    value = dspParams.stereoWidth,
                    valueRange = 0f..2f,
                    accentColor = StudioEmerald,
                    testTag = "slider_stereo_width",
                    onValueChange = { v -> onUpdateDsp { it.copy(stereoWidth = v) } }
                )
                DspParameterSlider(
                    label = "Soft Knee Bus Compression",
                    valueText = "${(dspParams.softCompression * 100).toInt()}%",
                    value = dspParams.softCompression,
                    valueRange = 0f..1f,
                    accentColor = ElectricPurple,
                    testTag = "slider_soft_compression",
                    onValueChange = { v -> onUpdateDsp { it.copy(softCompression = v) } }
                )
                DspParameterSlider(
                    label = "Tape Playback Speed & Pitch",
                    valueText = String.format(Locale.US, "%.2fx", dspParams.playbackSpeed),
                    value = dspParams.playbackSpeed,
                    valueRange = 0.65f..1.35f,
                    accentColor = AnalogAmber,
                    testTag = "slider_playback_speed",
                    onValueChange = { v -> onUpdateDsp { it.copy(playbackSpeed = v) } }
                )
            }
        }
    }
}

@Composable
fun StudioRackSection(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    Surface(
        color = ElevatedCharcoal,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SubtleBorder, RoundedCornerShape(20.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = NeonCyan
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            content()
        }
    }
}

@Composable
fun DspParameterSlider(
    label: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    accentColor: Color,
    testTag: String,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.labelMedium,
                color = accentColor
            )
        }
        Slider(
            value = value.coerceIn(valueRange.start, valueRange.endInclusive),
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor,
                inactiveTrackColor = DeepObsidian
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag)
        )
    }
}
