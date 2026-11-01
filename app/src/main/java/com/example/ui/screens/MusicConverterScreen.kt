package com.example.ui.screens

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.AmbientLayerType
import com.example.model.BuiltInConverterPresets
import com.example.model.ConverterCategory
import com.example.model.ExportProgressState
import com.example.model.ExportQualityOption
import com.example.model.LiveSoundMode
import com.example.model.LofiDspParameters
import com.example.model.MusicConverterPreset
import com.example.model.TrackItem
import com.example.model.VisualizerMode
import com.example.model.VisualizerSnapshot
import com.example.ui.components.RealTimeAudioVisualizer
import com.example.ui.theme.AnalogAmber
import com.example.ui.theme.DeepObsidian
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.ElectricPurpleBright
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
import kotlin.math.roundToInt

/**
 * Reusable Live Sound Modes Selector Panel (Part 2: 10 Real-Time Sound Modes).
 * Allows instant zero-restart switching between Original Stereo, Live Lo-Fi, 8D Ultra,
 * 3D Spatial Audio (Binaural HRTF), Ultra Surround, Bass Boost, Deep Bass, Soft Bass,
 * Clear Vocal, and Dreamy Sound.
 */
@Composable
fun LiveSoundModesSelectorSection(
    dspParams: LofiDspParameters,
    onSelectSoundMode: (LiveSoundMode) -> Unit,
    onUpdateDsp: (((LofiDspParameters) -> LofiDspParameters) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val activeMode = dspParams.soundMode

    Surface(
        color = ElevatedCharcoal.copy(alpha = 0.92f),
        shape = RoundedCornerShape(22.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, SubtleBorder, RoundedCornerShape(22.dp))
            .testTag("live_sound_modes_section")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Headphones,
                        contentDescription = "Sound Modes",
                        tint = NeonCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "LIVE SOUND MODES",
                            style = MaterialTheme.typography.labelLarge,
                            color = NeonCyan
                        )
                        Text(
                            text = "Active: ${activeMode.displayName} • ${activeMode.subtitle}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(ElectricPurple.copy(alpha = 0.24f))
                        .border(1.dp, ElectricPurpleBright, RoundedCornerShape(50))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = activeMode.badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(LiveSoundMode.entries, key = { it.name }) { mode ->
                    val isSelected = activeMode == mode
                    Surface(
                        color = if (isSelected) ElectricPurple.copy(alpha = 0.30f) else DeepObsidian,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .width(150.dp)
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) NeonCyan else SubtleBorder,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { onSelectSoundMode(mode) }
                            .testTag("sound_mode_chip_${mode.name.lowercase()}")
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = mode.badge,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) NeonCyan else ElectricPurpleBright,
                                    fontWeight = FontWeight.Bold
                                )
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(StudioEmerald)
                                    )
                                }
                            }
                            Text(
                                text = mode.displayName,
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = mode.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Dedicated real-time controls when 8D Ultra, 3D Spatial, or Ultra Surround is active
            if (onUpdateDsp != null && (activeMode == LiveSoundMode.EIGHT_D_ULTRA ||
                    activeMode == LiveSoundMode.THREE_D_SPATIAL ||
                    activeMode == LiveSoundMode.ULTRA_SURROUND)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(DeepObsidian.copy(alpha = 0.7f))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DspParameterSlider(
                        label = "Spatial / Surround Intensity",
                        valueText = "${(dspParams.spatialIntensity * 100).roundToInt()}%",
                        value = dspParams.spatialIntensity,
                        valueRange = 0.1f..1.0f,
                        accentColor = NeonCyan,
                        testTag = "slider_spatial_intensity",
                        onValueChange = { v -> onUpdateDsp { it.copy(spatialIntensity = v) } }
                    )
                    if (activeMode == LiveSoundMode.EIGHT_D_ULTRA) {
                        DspParameterSlider(
                            label = "8D Rotation Orbit Speed",
                            valueText = String.format(Locale.US, "%.2f Hz", dspParams.eightDRotationSpeedHz),
                            value = dspParams.eightDRotationSpeedHz,
                            valueRange = 0.04f..0.45f,
                            accentColor = ElectricPurpleBright,
                            testTag = "slider_8d_rotation_speed",
                            onValueChange = { v -> onUpdateDsp { it.copy(eightDRotationSpeedHz = v) } }
                        )
                    }
                    if (activeMode == LiveSoundMode.THREE_D_SPATIAL) {
                        DspParameterSlider(
                            label = "HRTF Virtual Stage Distance",
                            valueText = "${(dspParams.spatialDistance * 100).roundToInt()}%",
                            value = dspParams.spatialDistance,
                            valueRange = 0.05f..1.0f,
                            accentColor = AnalogAmber,
                            testTag = "slider_3d_spatial_distance",
                            onValueChange = { v -> onUpdateDsp { it.copy(spatialDistance = v) } }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Reusable Ambient Sound Mixer Panel (Part 6: 6 simultaneous background ambient layers).
 */
@Composable
fun AmbientSoundMixerSection(
    layerVolumes: Map<AmbientLayerType, Float>,
    onChangeLayerVolume: (AmbientLayerType, Float) -> Unit,
    onStopAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeCount = layerVolumes.values.count { it > 0.01f }

    Surface(
        color = ElevatedCharcoal.copy(alpha = 0.92f),
        shape = RoundedCornerShape(22.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, SubtleBorder, RoundedCornerShape(22.dp))
            .testTag("ambient_sound_mixer_section")
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
                        imageVector = Icons.Default.Spa,
                        contentDescription = "Ambient Sound Mixer",
                        tint = StudioEmerald,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "AMBIENT SOUND MIXER",
                            style = MaterialTheme.typography.labelLarge,
                            color = StudioEmerald
                        )
                        Text(
                            text = if (activeCount > 0) {
                                "$activeCount ambient soundscape(s) mixing live with music"
                            } else {
                                "Mix Rain, Ocean, Wind, Night, Birds & Thunder with music"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                if (activeCount > 0) {
                    OutlinedButton(
                        onClick = onStopAll,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("ambient_stop_all_button")
                    ) {
                        Text("Mute All", style = MaterialTheme.typography.labelSmall, color = VinylRose)
                    }
                }
            }

            AmbientLayerType.entries.forEach { layer ->
                val vol = layerVolumes[layer] ?: 0f
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "${layer.emoji} ${layer.displayName} (${layer.subtitle})",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (vol > 0.01f) TextPrimary else TextSecondary
                        )
                        Text(
                            text = "${(vol * 100).roundToInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (vol > 0.01f) StudioEmerald else TextMuted
                        )
                    }
                    Slider(
                        value = vol,
                        onValueChange = { newVol -> onChangeLayerVolume(layer, newVol) },
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = if (vol > 0.01f) StudioEmerald else TextSecondary,
                            activeTrackColor = StudioEmerald,
                            inactiveTrackColor = DeepObsidian
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(26.dp)
                            .testTag("ambient_slider_${layer.id}")
                    )
                }
            }
        }
    }
}

/**
 * PART 3 & PART 10: MUSIC STYLE CONVERTER STUDIO & EXPORT ENGINE SCREEN.
 */
@Composable
fun MusicConverterScreen(
    currentTrack: TrackItem?,
    allTracks: List<TrackItem>,
    convertedTracks: List<TrackItem>,
    isPlaying: Boolean,
    dspParams: LofiDspParameters,
    visualizerSnapshot: VisualizerSnapshot,
    visualizerMode: VisualizerMode,
    selectedCategory: ConverterCategory,
    selectedPreset: MusicConverterPreset,
    converterIntensity: Float,
    isBeforeOriginalPreview: Boolean,
    selectedExportQuality: ExportQualityOption,
    exportProgressState: ExportProgressState,
    ambientLayerVolumes: Map<AmbientLayerType, Float>,
    onSelectCategory: (ConverterCategory) -> Unit,
    onSelectPreset: (MusicConverterPreset) -> Unit,
    onIntensityChange: (Float) -> Unit,
    onToggleBeforeAfterPreview: (Boolean) -> Unit,
    onSelectExportQuality: (ExportQualityOption) -> Unit,
    onStartExport: () -> Unit,
    onCancelExport: () -> Unit,
    onPlayTrack: (TrackItem) -> Unit,
    onTogglePlayPause: () -> Unit,
    onImportAudioClick: () -> Unit,
    onSelectSoundMode: (LiveSoundMode) -> Unit,
    onSelectVisualizerMode: (VisualizerMode) -> Unit,
    onUpdateDsp: ((LofiDspParameters) -> LofiDspParameters) -> Unit,
    onChangeAmbientVolume: (AmbientLayerType, Float) -> Unit,
    onStopAllAmbient: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenStudio: () -> Unit,
    onBack: () -> Unit
) {
    val activeTrack = currentTrack ?: allTracks.firstOrNull()
    val categoryPresets = remember(selectedCategory) {
        BuiltInConverterPresets.byCategory(selectedCategory)
    }

    LazyColumn(
        contentPadding = PaddingValues(top = 12.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("music_converter_screen")
    ) {
        // 1. TOP HEADER BAR
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(ElevatedCharcoal)
                            .border(1.dp, SubtleBorder, CircleShape)
                            .testTag("converter_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "MUSIC CONVERTER STUDIO",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Lo-Fi • Phonk • Bass • Slowed+Reverb • Nightcore • 8D/3D WAV Export",
                            style = MaterialTheme.typography.bodySmall,
                            color = NeonCyan
                        )
                    }
                }

                OutlinedButton(
                    onClick = onImportAudioClick,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("converter_import_audio_button")
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(15.dp), tint = NeonCyan)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Import", style = MaterialTheme.typography.labelSmall, color = NeonCyan)
                }
            }
        }

        // 2. SOURCE TRACK & REAL-TIME VISUALIZER + BEFORE / AFTER COMPARE BAR
        item {
            Surface(
                color = ElevatedCharcoal,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        brush = Brush.horizontalGradient(listOf(ElectricPurple, NeonCyan)),
                        shape = RoundedCornerShape(24.dp)
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(ElectricPurple.copy(alpha = 0.24f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AudioFile,
                                    contentDescription = null,
                                    tint = NeonCyan
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = activeTrack?.title ?: "No Song Selected",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = activeTrack?.let { "${it.artist} • ${it.formatBadge}" }
                                        ?: "Import a song from your phone to convert",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                if (currentTrack != null) {
                                    onTogglePlayPause()
                                } else if (activeTrack != null) {
                                    onPlayTrack(activeTrack)
                                }
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(ElectricPurple, NeonCyan)))
                                .testTag("converter_play_pause_button")
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause Preview" else "Play Preview",
                                tint = Color.White
                            )
                        }
                    }

                    // 4-Mode Real-Time Visualizer with Mode Switcher
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        VisualizerMode.entries.forEach { vMode ->
                            FilterChip(
                                selected = visualizerMode == vMode,
                                onClick = { onSelectVisualizerMode(vMode) },
                                label = { Text(vMode.displayName, style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ElectricPurple.copy(alpha = 0.28f),
                                    selectedLabelColor = NeonCyan
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    RealTimeAudioVisualizer(
                        snapshot = visualizerSnapshot,
                        isPlaying = isPlaying,
                        isLofiMode = !isBeforeOriginalPreview,
                        visualizerMode = visualizerMode,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(106.dp)
                    )

                    // BEFORE (ORIGINAL) vs AFTER (CONVERTED STYLE) LIVE PREVIEW TOGGLE
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = { onToggleBeforeAfterPreview(true) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isBeforeOriginalPreview) StudioEmerald else GlassSurface
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("converter_before_original_button")
                        ) {
                            Text(
                                text = "BEFORE (ORIGINAL)",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isBeforeOriginalPreview) DeepObsidian else TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Button(
                            onClick = { onToggleBeforeAfterPreview(false) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!isBeforeOriginalPreview) ElectricPurple else GlassSurface
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("converter_after_processed_button")
                        ) {
                            Text(
                                text = "AFTER (${selectedPreset.name.uppercase()})",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // 3. STYLE CATEGORY TABS (Lo-Fi, Phonk, Bass, Other Styles)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "1. CHOOSE CONVERSION STYLE CATEGORY",
                    style = MaterialTheme.typography.labelLarge,
                    color = NeonCyan
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ConverterCategory.entries.forEach { category ->
                        val isSelected = selectedCategory == category
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectCategory(category) },
                            label = {
                                Text(
                                    text = category.displayName,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricPurple.copy(alpha = 0.32f),
                                selectedLabelColor = NeonCyan
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("converter_cat_${category.name.lowercase()}")
                        )
                    }
                }

                // Presets Grid/Carousel for selected category
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categoryPresets, key = { it.id }) { preset ->
                        val isSelected = selectedPreset.id == preset.id
                        Surface(
                            color = if (isSelected) ElectricPurple.copy(alpha = 0.28f) else ElevatedCharcoal,
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .width(182.dp)
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) NeonCyan else SubtleBorder,
                                    shape = RoundedCornerShape(18.dp)
                                )
                                .clickable { onSelectPreset(preset) }
                                .testTag("converter_preset_${preset.id}")
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = preset.badge,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSelected) NeonCyan else AnalogAmber,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.GraphicEq,
                                            contentDescription = null,
                                            tint = NeonCyan,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
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

        // 4. INTENSITY & FINE-TUNE STUDIO CONTROLS (Speed, Pitch, Echo, Reverb, Bass)
        item {
            StudioRackSection(
                title = "2. CONVERSION INTENSITY & MASTERING PARAMETERS",
                subtitle = "Adjust effect strength, speed, pitch semitones, echo & bass before exporting"
            ) {
                DspParameterSlider(
                    label = "Style Conversion Intensity (${selectedPreset.name})",
                    valueText = "${(converterIntensity * 100).roundToInt()}%",
                    value = converterIntensity,
                    valueRange = 0.1f..1.0f,
                    accentColor = NeonCyan,
                    testTag = "converter_intensity_slider",
                    onValueChange = onIntensityChange
                )
                DspParameterSlider(
                    label = "Playback Speed (Slowed / Nightcore)",
                    valueText = String.format(Locale.US, "%.2fx", dspParams.playbackSpeed),
                    value = dspParams.playbackSpeed,
                    valueRange = 0.65f..1.35f,
                    accentColor = AnalogAmber,
                    testTag = "converter_speed_slider",
                    onValueChange = { v -> onUpdateDsp { it.copy(playbackSpeed = v, isLiveLofiEnabled = true) } }
                )
                DspParameterSlider(
                    label = "Pitch Shift (Semitones)",
                    valueText = String.format(Locale.US, "%+.1f st", dspParams.pitchSemitones),
                    value = dspParams.pitchSemitones,
                    valueRange = -12f..12f,
                    accentColor = ElectricPurpleBright,
                    testTag = "converter_pitch_slider",
                    onValueChange = { v -> onUpdateDsp { it.copy(pitchSemitones = v, isLiveLofiEnabled = true) } }
                )
                DspParameterSlider(
                    label = "Echo / Ping-Pong Delay Mix",
                    valueText = "${(dspParams.echoMix * 100).roundToInt()}%",
                    value = dspParams.echoMix,
                    valueRange = 0f..0.85f,
                    accentColor = StudioEmerald,
                    testTag = "converter_echo_slider",
                    onValueChange = { v -> onUpdateDsp { it.copy(echoMix = v, isLiveLofiEnabled = true) } }
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = onOpenStudio,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Full DSP Editor", style = MaterialTheme.typography.labelSmall)
                    }
                    OutlinedButton(
                        onClick = onOpenEqualizer,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("10-Band EQ", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        // 5. EXPORT & SAVE CONVERTED AUDIO ENGINE (Part 10)
        item {
            Surface(
                color = ElevatedCharcoal,
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, NeonCyan.copy(alpha = 0.6f), RoundedCornerShape(22.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "3. EXPORT & SAVE CONVERTED SONG",
                        style = MaterialTheme.typography.labelLarge,
                        color = NeonCyan
                    )
                    Text(
                        text = "Renders a new lossless 16-bit Stereo WAV file into 'Converted Songs' & Android Music storage. Your original song file is never modified.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    // Export Quality Selector
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ExportQualityOption.entries.forEach { option ->
                            FilterChip(
                                selected = selectedExportQuality == option,
                                onClick = { onSelectExportQuality(option) },
                                label = { Text(option.label, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    if (exportProgressState.isExporting) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            LinearProgressIndicator(
                                progress = { exportProgressState.progressFraction.coerceIn(0f, 1f) },
                                color = NeonCyan,
                                trackColor = DeepObsidian,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .testTag("export_progress_indicator")
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = exportProgressState.statusText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NeonCyan,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedButton(
                                    onClick = onCancelExport,
                                    modifier = Modifier.testTag("cancel_export_button")
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Cancel", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    } else {
                        Button(
                            onClick = onStartExport,
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("export_converted_song_button")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "CONVERT & SAVE AS NEW WAV (${selectedPreset.name.uppercase()})",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    // Completed track quick-play card
                    exportProgressState.completedTrack?.let { doneTrack ->
                        Surface(
                            color = StudioEmerald.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, StudioEmerald, RoundedCornerShape(14.dp))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = StudioEmerald
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = doneTrack.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Saved to Converted Songs • ${doneTrack.formatBadge}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = StudioEmerald
                                        )
                                    }
                                }
                                Button(
                                    onClick = {
                                        // Switch to Original Stereo when playing back an already-converted WAV file
                                        onSelectSoundMode(LiveSoundMode.ORIGINAL_STEREO)
                                        onPlayTrack(doneTrack)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = StudioEmerald),
                                    modifier = Modifier.testTag("play_converted_track_button")
                                ) {
                                    Text("Play", color = DeepObsidian, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 6. LIVE SOUND MODES SECTION
        item {
            LiveSoundModesSelectorSection(
                dspParams = dspParams,
                onSelectSoundMode = onSelectSoundMode,
                onUpdateDsp = onUpdateDsp
            )
        }

        // 7. AMBIENT SOUND MIXER SECTION
        item {
            AmbientSoundMixerSection(
                layerVolumes = ambientLayerVolumes,
                onChangeLayerVolume = onChangeAmbientVolume,
                onStopAll = onStopAllAmbient
            )
        }

        // 8. CONVERTED SONGS HISTORY
        if (convertedTracks.isNotEmpty()) {
            item {
                Text(
                    text = "RECENTLY CONVERTED SONGS (${convertedTracks.size})",
                    style = MaterialTheme.typography.labelLarge,
                    color = NeonCyan
                )
            }
            items(convertedTracks.take(6), key = { it.id }) { cTrack ->
                Surface(
                    color = ElevatedCharcoal,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SubtleBorder, RoundedCornerShape(16.dp))
                        .clickable {
                            onSelectSoundMode(LiveSoundMode.ORIGINAL_STEREO)
                            onPlayTrack(cTrack)
                        }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = cTrack.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${cTrack.formatBadge} • ${cTrack.lyricsOrNotes}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play Converted Song",
                            tint = NeonCyan
                        )
                    }
                }
            }
        }
    }
}
