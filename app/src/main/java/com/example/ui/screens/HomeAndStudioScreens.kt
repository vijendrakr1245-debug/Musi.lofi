package com.example.ui.screens

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
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.PlaybackHistoryEntity
import com.example.model.AiMoodRecommendation
import com.example.model.BuiltInLofiPresets
import com.example.model.LofiDspParameters
import com.example.model.LofiPreset
import com.example.model.TrackItem
import com.example.model.VisualizerSnapshot
import com.example.ui.components.HifiLofiModeToggleBanner
import com.example.ui.components.RealTimeAudioVisualizer
import com.example.ui.components.formatDurationMillis
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

@Composable
fun HomeScreen(
    allTracks: List<TrackItem>,
    favoriteIds: Set<String>,
    history: List<PlaybackHistoryEntity>,
    presets: List<LofiPreset>,
    dspParams: LofiDspParameters,
    currentTrack: TrackItem?,
    isPlaying: Boolean,
    onPlayTrack: (TrackItem) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onSelectPreset: (LofiPreset) -> Unit,
    onToggleLofiMode: (Boolean) -> Unit,
    onApplyAiRecommendation: (AiMoodRecommendation) -> Unit,
    onImportSongsClick: () -> Unit,
    onScanDeviceClick: () -> Unit,
    onNavigateToStudio: () -> Unit,
    onExpandNowPlaying: () -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("home_screen")
    ) {
        item {
            Spacer(modifier = Modifier.height(6.dp))
            // Hero Studio Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(195.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.dp, SubtleBorder, RoundedCornerShape(24.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_lofi_studio_banner),
                    contentDescription = "Lofi Wave Studio Banner",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    DeepObsidian.copy(alpha = 0.25f),
                                    DeepObsidian.copy(alpha = 0.85f),
                                    DeepObsidian.copy(alpha = 0.96f)
                                )
                            )
                        )
                )
                Column(
                    verticalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(ElectricPurple.copy(alpha = 0.3f))
                                .border(1.dp, ElectricPurple, RoundedCornerShape(50))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "REAL-TIME PCM AUDIO DSP ENGINE",
                                style = MaterialTheme.typography.labelSmall,
                                color = NeonCyan
                            )
                        }

                        if (currentTrack != null) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(GlassSurface.copy(alpha = 0.9f))
                                    .clickable { onExpandNowPlaying() }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Now Playing ↗",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextPrimary
                                )
                            }
                        }
                    }

                    Column {
                        Text(
                            text = "LOFI WAVE",
                            style = MaterialTheme.typography.displayMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "Convert any song on your phone into warm vintage Lo-Fi in real time without altering the original file.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            maxLines = 2
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = onImportSongsClick,
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                modifier = Modifier
                                    .height(38.dp)
                                    .testTag("home_import_songs_button")
                            ) {
                                Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Select Music", style = MaterialTheme.typography.labelLarge)
                            }

                            OutlinedButton(
                                onClick = onNavigateToStudio,
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                modifier = Modifier
                                    .height(38.dp)
                                    .testTag("home_open_studio_button")
                            ) {
                                Icon(Icons.Default.Tune, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Lo-Fi Studio", style = MaterialTheme.typography.labelLarge, color = NeonCyan)
                            }
                        }
                    }
                }
            }
        }

        // Instant Hi-Fi / Live Lo-Fi Switch
        item {
            HifiLofiModeToggleBanner(
                dspParams = dspParams,
                onToggleMode = onToggleLofiMode,
                onOpenStudio = onNavigateToStudio
            )
        }

        // Quick Lo-Fi Presets Strip
        item {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "LIVE LO-FI PRESETS (${presets.size})",
                        style = MaterialTheme.typography.labelLarge,
                        color = NeonCyan
                    )
                    Text(
                        text = "Customize in Studio →",
                        style = MaterialTheme.typography.labelSmall,
                        color = ElectricPurple,
                        modifier = Modifier.clickable { onNavigateToStudio() }
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(presets, key = { it.id }) { preset ->
                        val isSelected = dspParams.isLiveLofiEnabled && dspParams.presetId == preset.id
                        Surface(
                            color = if (isSelected) ElectricPurple.copy(alpha = 0.26f) else ElevatedCharcoal,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .width(168.dp)
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) NeonCyan else SubtleBorder,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable { onSelectPreset(preset) }
                                .testTag("home_preset_${preset.id}")
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = preset.moodCategory.uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSelected) NeonCyan else AnalogAmber,
                                        maxLines = 1
                                    )
                                    Icon(
                                        imageVector = Icons.Default.GraphicEq,
                                        contentDescription = null,
                                        tint = if (isSelected) NeonCyan else TextMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
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

        // AI Music Assistant Section
        item {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = AnalogAmber,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "AI MUSIC ASSISTANT • MOOD & DSP ENGINE",
                        style = MaterialTheme.typography.labelLarge,
                        color = AnalogAmber
                    )
                }
                Text(
                    text = "Select your current mood for instant AI Lo-Fi DSP effect chains & smart EQ tuning (100% on-device):",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(BuiltInLofiPresets.AI_MOOD_RECOMMENDATIONS, key = { it.moodId }) { rec ->
                        Surface(
                            color = ElevatedCharcoal,
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .width(250.dp)
                                .border(1.dp, SubtleBorder, RoundedCornerShape(18.dp))
                                .clickable { onApplyAiRecommendation(rec) }
                                .testTag("ai_mood_card_${rec.moodId}")
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "${rec.emojiTag} ${rec.moodTitle}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "APPLY",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = NeonCyan
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = rec.dspRecipeSummary,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ElectricPurple,
                                    maxLines = 2
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = rec.description,
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

        // Recently Played Section (if any)
        if (history.isNotEmpty()) {
            item {
                Column {
                    Text(
                        text = "RECENTLY PLAYED",
                        style = MaterialTheme.typography.labelLarge,
                        color = NeonCyan
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(history.take(8), key = { it.trackId }) { h ->
                            val matched = allTracks.find { it.id == h.trackId }
                            Surface(
                                color = ElevatedCharcoal,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .width(175.dp)
                                    .border(1.dp, SubtleBorder, RoundedCornerShape(14.dp))
                                    .clickable {
                                        if (matched != null) onPlayTrack(matched)
                                    }
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = h.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = h.artist,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary,
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${h.formatBadge} • Played ${h.playCount}x",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ElectricPurple
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Favorites Quick Strip (if any)
        val favTracks = allTracks.filter { it.id in favoriteIds }
        if (favTracks.isNotEmpty()) {
            item {
                Text(
                    text = "FAVORITE SONGS (${favTracks.size})",
                    style = MaterialTheme.typography.labelLarge,
                    color = VinylRose
                )
            }
            items(favTracks.take(4), key = { "fav_${it.id}" }) { track ->
                TrackRowCard(
                    track = track,
                    isCurrent = currentTrack?.id == track.id,
                    isPlaying = isPlaying,
                    isFavorite = true,
                    onPlay = { onPlayTrack(track) },
                    onToggleFavorite = { onToggleFavorite(track.id) }
                )
            }
        }

        // All / Recently Added Songs
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "RECENTLY ADDED & STUDIO MASTERS (${allTracks.size})",
                    style = MaterialTheme.typography.labelLarge,
                    color = NeonCyan
                )
                Text(
                    text = "Scan MediaStore",
                    style = MaterialTheme.typography.labelSmall,
                    color = ElectricPurple,
                    modifier = Modifier.clickable { onScanDeviceClick() }
                )
            }
        }

        items(allTracks, key = { it.id }) { track ->
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
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (isCurrent) NeonCyan else SubtleBorder,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onPlay() }
            .testTag("track_card_${track.id}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
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
                            .clip(RoundedCornerShape(4.dp))
                            .background(GlassSurface)
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = track.formatBadge,
                            style = MaterialTheme.typography.labelSmall,
                            color = AnalogAmber
                        )
                    }
                }
                Text(
                    text = "${track.artist} • ${track.album} • ${formatDurationMillis(track.durationMs)}",
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
