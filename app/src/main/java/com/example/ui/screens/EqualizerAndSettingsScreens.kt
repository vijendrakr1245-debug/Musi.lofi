package com.example.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.CustomEqPresetEntity
import com.example.model.AudioPlaybackQuality
import com.example.model.EqualizerState
import com.example.model.TrackItem
import com.example.ui.theme.AccentTheme
import com.example.ui.theme.AnalogAmber
import com.example.ui.theme.DeepObsidian
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.ElevatedCharcoal
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.StudioEmerald
import com.example.ui.theme.SubtleBorder
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun EqualizerScreen(
    eqState: EqualizerState,
    customEqPresets: List<CustomEqPresetEntity>,
    onUpdateEq: ((EqualizerState) -> EqualizerState) -> Unit,
    onUpdateBand: (Int, Float) -> Unit,
    onSelectBuiltInPreset: (String, List<Float>) -> Unit,
    onSaveCustomEqPreset: (String) -> Unit,
    onApplyBassEnhancementPreset: (String) -> Unit = {},
    onResetAllEffects: () -> Unit = {}
) {
    var showSaveDialog by remember { mutableStateOf(false) }
    var presetNameInput by remember { mutableStateOf("") }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            containerColor = ElevatedCharcoal,
            title = { Text("Save Custom 10-Band EQ Preset", style = MaterialTheme.typography.titleLarge) },
            text = {
                OutlinedTextField(
                    value = presetNameInput,
                    onValueChange = { presetNameInput = it },
                    label = { Text("EQ Preset Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveCustomEqPreset(presetNameInput)
                        presetNameInput = ""
                        showSaveDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple)
                ) {
                    Text("Save EQ")
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
            .testTag("equalizer_screen")
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
                        text = "10-BAND EQUALIZER",
                        style = MaterialTheme.typography.headlineLarge,
                        color = TextPrimary
                    )
                    Text(
                        text = "Precision Biquad Peaking Filter Bank & Mastering",
                        style = MaterialTheme.typography.bodySmall,
                        color = NeonCyan
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Switch(
                        checked = eqState.isEnabled,
                        onCheckedChange = { enabled -> onUpdateEq { it.copy(isEnabled = enabled) } },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = ElectricPurple
                        ),
                        modifier = Modifier.testTag("eq_enable_switch")
                    )
                    Button(
                        onClick = { showSaveDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        // EQ Frequency Response Curve Canvas
        item {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(DeepObsidian)
                    .border(1.dp, SubtleBorder, RoundedCornerShape(18.dp))
                    .padding(14.dp)
            ) {
                val w = size.width
                val h = size.height
                val midY = h / 2f

                // 0 dB center line
                drawLine(
                    color = SubtleBorder,
                    start = Offset(0f, midY),
                    end = Offset(w, midY),
                    strokeWidth = 1.dp.toPx()
                )

                val bands = eqState.bandGainsDb
                if (bands.isNotEmpty()) {
                    val path = Path()
                    val fillPath = Path()
                    for (i in bands.indices) {
                        val x = (i.toFloat() / (bands.size - 1).coerceAtLeast(1)) * w
                        val normalized = (bands[i].coerceIn(-12f, 12f) / 12f)
                        val y = midY - normalized * (h * 0.42f)
                        if (i == 0) {
                            path.moveTo(x, y)
                            fillPath.moveTo(x, h)
                            fillPath.lineTo(x, y)
                        } else {
                            path.lineTo(x, y)
                            fillPath.lineTo(x, y)
                        }
                        drawCircle(
                            color = NeonCyan,
                            radius = 4.dp.toPx(),
                            center = Offset(x, y)
                        )
                    }
                    fillPath.lineTo(w, h)
                    fillPath.close()

                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(ElectricPurple.copy(alpha = 0.35f), Color.Transparent)
                        )
                    )
                    drawPath(
                        path = path,
                        color = if (eqState.isEnabled) NeonCyan else TextSecondary,
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }
        }

        // Built-In & Custom EQ Presets
        item {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "EQ PRESETS (ACTIVE: ${eqState.presetName.uppercase()})",
                        style = MaterialTheme.typography.labelLarge,
                        color = NeonCyan
                    )
                    TextButton(
                        onClick = {
                            onSelectBuiltInPreset("Flat Studio", List(10) { 0f })
                        }
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Flat 0dB", style = MaterialTheme.typography.labelSmall)
                    }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(EqualizerState.BUILT_IN_EQ_PRESETS.entries.toList(), key = { it.key }) { (name, gains) ->
                        FilterChip(
                            selected = eqState.presetName == name,
                            onClick = { onSelectBuiltInPreset(name, gains) },
                            label = { Text(name, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricPurple.copy(alpha = 0.28f),
                                selectedLabelColor = NeonCyan
                            )
                        )
                    }
                    items(customEqPresets, key = { "cust_eq_${it.name}" }) { custom ->
                        FilterChip(
                            selected = eqState.presetName == custom.name,
                            onClick = {
                                val parsed = custom.gainsCsv.split(",").mapNotNull { it.toFloatOrNull() }
                                if (parsed.size == 10) {
                                    onUpdateEq {
                                        it.copy(
                                            isEnabled = true,
                                            presetName = custom.name,
                                            bandGainsDb = parsed,
                                            bassBoostDb = custom.bassBoostDb,
                                            midrangeBoostDb = custom.midrangeBoostDb,
                                            trebleBoostDb = custom.trebleBoostDb
                                        )
                                    }
                                }
                            },
                            label = { Text("★ ${custom.name}", style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        }

        // 5-Band or 10-Band Mode Toggle + Individual Frequency Controls
        item {
            StudioRackSection(
                title = if (eqState.isFiveBandMode) {
                    "5-BAND GRAPHIC FREQUENCY SLIDERS (-12dB TO +12dB)"
                } else {
                    "10-BAND GRAPHIC FREQUENCY SLIDERS (-12dB TO +12dB)"
                },
                subtitle = "Switch between 5-Band Quick EQ and 10-Band Studio Precision"
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilterChip(
                        selected = !eqState.isFiveBandMode,
                        onClick = { onUpdateEq { it.copy(isFiveBandMode = false) } },
                        label = { Text("10-Band Studio (31Hz–16kHz)", style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ElectricPurple.copy(alpha = 0.28f),
                            selectedLabelColor = NeonCyan
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("eq_mode_10band_chip")
                    )
                    FilterChip(
                        selected = eqState.isFiveBandMode,
                        onClick = { onUpdateEq { it.copy(isFiveBandMode = true) } },
                        label = { Text("5-Band Quick (62Hz–16kHz)", style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ElectricPurple.copy(alpha = 0.28f),
                            selectedLabelColor = NeonCyan
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("eq_mode_5band_chip")
                    )
                }

                val activeIndices = if (eqState.isFiveBandMode) {
                    EqualizerState.FIVE_BAND_INDICES
                } else {
                    (0 until 10).toList()
                }

                for (i in activeIndices) {
                    val freqLabel = EqualizerState.BAND_LABELS[i]
                    val gain = eqState.bandGainsDb.getOrElse(i) { 0f }
                    DspParameterSlider(
                        label = "$freqLabel Hz Band",
                        valueText = String.format(Locale.US, "%+.1f dB", gain),
                        value = gain,
                        valueRange = -12f..12f,
                        accentColor = if (i < 3) ElectricPurple else if (i < 7) NeonCyan else AnalogAmber,
                        testTag = "eq_band_slider_$i",
                        onValueChange = { newGain ->
                            if (eqState.isFiveBandMode) {
                                val updatedBands = eqState.bandGainsDb.toMutableList()
                                val clamped = newGain.coerceIn(-12f, 12f)
                                updatedBands[i] = clamped
                                if (i - 1 >= 0) {
                                    updatedBands[i - 1] = ((updatedBands[i - 1] + clamped) * 0.5f).coerceIn(-12f, 12f)
                                }
                                onUpdateEq {
                                    it.copy(
                                        presetName = "Custom Curve",
                                        bandGainsDb = updatedBands
                                    )
                                }
                            } else {
                                onUpdateBand(i, newGain)
                            }
                        }
                    )
                }
            }
        }

        // Bass, Sub-Bass, Midrange, Treble & Low-Frequency Mastering (Phase 8)
        item {
            StudioRackSection(
                title = "BASS, SUB-BASS, MIDRANGE & TREBLE MASTERING",
                subtitle = "Phase 8 Low-Frequency EQ, Harmonic Exciter & Low-Band Compressor with Vocal Protection"
            ) {
                DspParameterSlider(
                    label = "Bass Control (105 Hz Warm Punch)",
                    valueText = String.format(Locale.US, "%+.1f dB", eqState.bassBoostDb),
                    value = eqState.bassBoostDb,
                    valueRange = -12f..12f,
                    accentColor = ElectricPurple,
                    testTag = "eq_bass_slider",
                    onValueChange = { v -> onUpdateEq { it.copy(bassBoostDb = v, isEnabled = true) } }
                )
                DspParameterSlider(
                    label = "Sub-Bass Control (55 Hz Deep Sub Extension)",
                    valueText = String.format(Locale.US, "%+.1f dB", eqState.subBassBoostDb),
                    value = eqState.subBassBoostDb,
                    valueRange = -12f..12f,
                    accentColor = ElectricPurple,
                    testTag = "eq_sub_bass_slider",
                    onValueChange = { v -> onUpdateEq { it.copy(subBassBoostDb = v, isEnabled = true) } }
                )
                DspParameterSlider(
                    label = "Bass Harmonic Enhancement (Warm 2nd/3rd Overtones)",
                    valueText = "${(eqState.bassHarmonicDrive * 100).toInt()}%",
                    value = eqState.bassHarmonicDrive,
                    valueRange = 0f..1f,
                    accentColor = AnalogAmber,
                    testTag = "eq_bass_harmonic_slider",
                    onValueChange = { v -> onUpdateEq { it.copy(bassHarmonicDrive = v) } }
                )
                DspParameterSlider(
                    label = "Low-Frequency Compression (<125 Hz Tightness)",
                    valueText = "${(eqState.lowFreqCompression * 100).toInt()}%",
                    value = eqState.lowFreqCompression,
                    valueRange = 0f..1f,
                    accentColor = StudioEmerald,
                    testTag = "eq_low_freq_comp_slider",
                    onValueChange = { v -> onUpdateEq { it.copy(lowFreqCompression = v) } }
                )
                DspParameterSlider(
                    label = "Midrange Control (Vocal Presence & Clarity)",
                    valueText = String.format(Locale.US, "%+.1f dB", eqState.midrangeBoostDb),
                    value = eqState.midrangeBoostDb,
                    valueRange = -12f..12f,
                    accentColor = StudioEmerald,
                    testTag = "eq_midrange_slider",
                    onValueChange = { v -> onUpdateEq { it.copy(midrangeBoostDb = v, isEnabled = true) } }
                )
                DspParameterSlider(
                    label = "Treble Control (Crisp Air & Detail)",
                    valueText = String.format(Locale.US, "%+.1f dB", eqState.trebleBoostDb),
                    value = eqState.trebleBoostDb,
                    valueRange = -12f..12f,
                    accentColor = NeonCyan,
                    testTag = "eq_treble_slider",
                    onValueChange = { v -> onUpdateEq { it.copy(trebleBoostDb = v, isEnabled = true) } }
                )
            }
        }

        // Phase 9: Advanced Audio Controls (Preamp, Stereo Width, Balance, DRC, Limiter, Enhancement ON/OFF, Reset)
        item {
            StudioRackSection(
                title = "ADVANCED MASTERING & AUDIO CONTROLS",
                subtitle = "Preamp • Stereo Width • Balance • Dynamic Range Control • True-Peak Limiter"
            ) {
                ToggleSettingRow(
                    title = "Audio Enhancement Engine (Master ON/OFF)",
                    subtitle = "Enable or bypass Preamp, Bass Enhancement, Stereo Width & DRC stages",
                    checked = eqState.audioEnhancementEnabled,
                    onCheckedChange = { v -> onUpdateEq { it.copy(audioEnhancementEnabled = v) } }
                )
                DspParameterSlider(
                    label = "Preamp Gain (-12 dB to +12 dB)",
                    valueText = String.format(Locale.US, "%+.1f dB", eqState.preampGainDb),
                    value = eqState.preampGainDb,
                    valueRange = -12f..12f,
                    accentColor = NeonCyan,
                    testTag = "eq_preamp_slider",
                    onValueChange = { v -> onUpdateEq { it.copy(preampGainDb = v) } }
                )
                DspParameterSlider(
                    label = "Master Stereo Width (0% Mono – 200% Wide)",
                    valueText = "${(eqState.masterStereoWidth * 100).toInt()}%",
                    value = eqState.masterStereoWidth,
                    valueRange = 0f..2f,
                    accentColor = ElectricPurple,
                    testTag = "eq_stereo_width_slider",
                    onValueChange = { v -> onUpdateEq { it.copy(masterStereoWidth = v) } }
                )
                DspParameterSlider(
                    label = "Stereo Balance (Left / Center / Right)",
                    valueText = when {
                        eqState.stereoBalance < -0.05f -> String.format(Locale.US, "L %.0f%%", -eqState.stereoBalance * 100)
                        eqState.stereoBalance > 0.05f -> String.format(Locale.US, "R %.0f%%", eqState.stereoBalance * 100)
                        else -> "CENTER"
                    },
                    value = eqState.stereoBalance,
                    valueRange = -1f..1f,
                    accentColor = AnalogAmber,
                    testTag = "eq_balance_slider",
                    onValueChange = { v -> onUpdateEq { it.copy(stereoBalance = v) } }
                )
                DspParameterSlider(
                    label = "Dynamic Range Control (DRC Bus Compression)",
                    valueText = "${(eqState.dynamicRangeControl * 100).toInt()}%",
                    value = eqState.dynamicRangeControl,
                    valueRange = 0f..1f,
                    accentColor = StudioEmerald,
                    testTag = "eq_drc_slider",
                    onValueChange = { v -> onUpdateEq { it.copy(dynamicRangeControl = v) } }
                )
            }
        }

        // Advanced Bass Enhancement Quick-Select
        item {
            StudioRackSection(
                title = "ADVANCED BASS ENHANCEMENT PRESETS",
                subtitle = "Clean, powerful low-end presets with automatic headroom & vocal protection"
            ) {
                val bassEnhancements = listOf(
                    Triple("conv_deep_bass", "Deep Bass", "Warm 55Hz sub-shelf with clean vocal separation"),
                    Triple("conv_punchy_bass", "Punchy Bass", "Tight 90Hz kick transient punch without boominess"),
                    Triple("conv_sub_bass", "Sub Bass", "Focused 32–64Hz cinema sub-bass extension"),
                    Triple("conv_bass_boost", "Bass Boost", "Balanced analog low-end lift (+4.5 dB)")
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    bassEnhancements.chunked(2).forEach { rowItems ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            rowItems.forEach { (presetId, title, desc) ->
                                Surface(
                                    color = DeepObsidian,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .border(1.dp, ElectricPurple.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                                        .clickable { onApplyBassEnhancementPreset(presetId) }
                                        .testTag("bass_preset_$presetId")
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = title,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = NeonCyan
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = desc,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // High-Quality Audio, Output Limiter & Reset All Effects
        item {
            StudioRackSection(
                title = "HIGH-QUALITY AUDIO, LIMITER & TRANSITIONS",
                subtitle = "True-peak output limiter (-0.3 dBFS), loudness normalization & gapless playback"
            ) {
                ToggleSettingRow(
                    title = "Output True-Peak Limiter (-0.3 dBFS Protection)",
                    subtitle = "Envelope-tracked brickwall ceiling preventing digital clipping at any gain",
                    checked = eqState.outputLimiterEnabled,
                    onCheckedChange = { v -> onUpdateEq { it.copy(outputLimiterEnabled = v) } }
                )
                ToggleSettingRow(
                    title = "Volume Normalization",
                    subtitle = "Maintains balanced loudness across presets and tracks",
                    checked = eqState.volumeNormalization,
                    onCheckedChange = { v -> onUpdateEq { it.copy(volumeNormalization = v) } }
                )
                ToggleSettingRow(
                    title = "Smooth Parameter & Track Transitions",
                    subtitle = "Applies click-free slewing and ${eqState.fadeInOutMs}ms fade-in envelope",
                    checked = eqState.smoothTransitions,
                    onCheckedChange = { v -> onUpdateEq { it.copy(smoothTransitions = v) } }
                )
                ToggleSettingRow(
                    title = "Gapless Playback",
                    subtitle = "Seamless transitions between queued tracks without silence gaps",
                    checked = eqState.gaplessPlayback,
                    onCheckedChange = { v -> onUpdateEq { it.copy(gaplessPlayback = v) } }
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        onUpdateEq { EqualizerState() }
                        onResetAllEffects()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("eq_reset_all_effects_button")
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp), tint = NeonCyan)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reset All Audio Effects & EQ to Flat Normal", style = MaterialTheme.typography.labelLarge, color = NeonCyan)
                }
            }
        }
    }
}

@Composable
fun ThemesScreen(
    accentTheme: AccentTheme,
    pureAmoledBlack: Boolean,
    highQualityDecoding: Boolean,
    onUpdateThemePrefs: (AccentTheme, Boolean, Boolean) -> Unit
) {
    var previewedTheme by remember(accentTheme) { mutableStateOf(accentTheme) }

    LazyColumn(
        contentPadding = PaddingValues(bottom = 115.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("themes_screen")
    ) {
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "THEMES GALLERY",
                style = MaterialTheme.typography.headlineLarge,
                color = TextPrimary
            )
            Text(
                text = "12 Complete Anime & Minimal Visual Identities • Saved Automatically",
                style = MaterialTheme.typography.bodySmall,
                color = NeonCyan
            )
        }

        item {
            StudioRackSection(
                title = "12 COMPLETE PREMIUM THEMES",
                subtitle = "Select any theme to immediately update the entire app appearance"
            ) {
                // Live Interactive Theme Preview Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(previewedTheme.wallpaperTop, previewedTheme.wallpaperBottom)
                            )
                        )
                        .border(
                            width = 1.5.dp,
                            brush = Brush.horizontalGradient(
                                listOf(previewedTheme.primary, previewedTheme.secondary, previewedTheme.tertiary)
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(16.dp)
                        .testTag("theme_live_preview_card")
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "LIVE THEME PREVIEW: ${previewedTheme.label.uppercase()}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = previewedTheme.secondary
                                )
                                Text(
                                    text = previewedTheme.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextPrimary
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(previewedTheme.surfaceBg)
                                    .border(1.dp, previewedTheme.tertiary, RoundedCornerShape(50))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = previewedTheme.playerStyleBadge,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = previewedTheme.tertiary
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(previewedTheme.cardBg)
                                .border(1.dp, previewedTheme.primary.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(previewedTheme.primary)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(previewedTheme.secondary)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(previewedTheme.tertiary)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = if (previewedTheme == accentTheme) "Selected Theme ✓" else "Tap Apply to Activate",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextPrimary
                                )
                            }

                            Button(
                                onClick = {
                                    val useAmoled = previewedTheme == AccentTheme.AMOLED_BLACK || pureAmoledBlack
                                    onUpdateThemePrefs(previewedTheme, useAmoled, highQualityDecoding)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = previewedTheme.primary),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("apply_previewed_theme_button")
                            ) {
                                Text(
                                    text = if (previewedTheme == accentTheme) "Active ✓" else "Apply Theme",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (theme in AccentTheme.PRIMARY_TWELVE) {
                        val isApplied = theme == accentTheme
                        val isPreviewed = theme == previewedTheme
                        Surface(
                            color = if (isApplied) theme.primary.copy(alpha = 0.24f) else theme.surfaceBg,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = if (isApplied || isPreviewed) 1.5.dp else 1.dp,
                                    brush = if (isApplied || isPreviewed) {
                                        Brush.horizontalGradient(
                                            listOf(theme.primary, theme.secondary, theme.tertiary)
                                        )
                                    } else {
                                        Brush.horizontalGradient(listOf(SubtleBorder, SubtleBorder))
                                    },
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable {
                                    previewedTheme = theme
                                    val useAmoled = theme == AccentTheme.AMOLED_BLACK
                                    onUpdateThemePrefs(theme, useAmoled, highQualityDecoding)
                                }
                                .testTag("theme_item_${theme.name.lowercase()}")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                Brush.linearGradient(
                                                    listOf(theme.wallpaperTop, theme.wallpaperBottom, theme.primary)
                                                )
                                            )
                                            .border(1.dp, theme.secondary.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                                    ) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(theme.primary)
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(theme.secondary)
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(theme.tertiary)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = theme.label,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = "${theme.subtitle} • ${theme.playerStyleBadge}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { previewedTheme = theme },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Text("Preview", style = MaterialTheme.typography.labelSmall, color = theme.secondary)
                                    }
                                    Button(
                                        onClick = {
                                            previewedTheme = theme
                                            val useAmoled = theme == AccentTheme.AMOLED_BLACK
                                            onUpdateThemePrefs(theme, useAmoled, highQualityDecoding)
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isApplied) StudioEmerald else theme.primary
                                        ),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Text(
                                            text = if (isApplied) "Active ✓" else "Apply",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isApplied) DeepObsidian else Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                ToggleSettingRow(
                    title = "Pure AMOLED Deep Black (#000000)",
                    subtitle = "Maximum contrast for dark rooms and night listening",
                    checked = pureAmoledBlack,
                    onCheckedChange = { amoled ->
                        onUpdateThemePrefs(accentTheme, amoled, highQualityDecoding)
                    }
                )
            }
        }
    }
}

@Composable
fun SettingsScreen(
    accentTheme: AccentTheme,
    pureAmoledBlack: Boolean,
    highQualityDecoding: Boolean,
    sleepTimerRemainingSec: Int?,
    eqState: EqualizerState = EqualizerState(),
    audioPlaybackQuality: AudioPlaybackQuality = AudioPlaybackQuality.HIGH_QUALITY,
    currentTrack: TrackItem? = null,
    onSelectAudioPlaybackQuality: (AudioPlaybackQuality) -> Unit = {},
    onUpdateEq: ((EqualizerState) -> EqualizerState) -> Unit = {},
    onUpdateThemePrefs: (AccentTheme, Boolean, Boolean) -> Unit,
    onSetSleepTimerMinutes: (Int) -> Unit,
    onImportSongsClick: () -> Unit,
    onImportFolderClick: () -> Unit,
    onScanDeviceClick: () -> Unit,
    onClearHistory: () -> Unit,
    onOpenStudio: () -> Unit = {},
    onOpenEqualizer: () -> Unit = {},
    onOpenMusicConverter: () -> Unit = {},
    onOpenThemes: () -> Unit = {}
) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = 115.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("settings_screen")
    ) {
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "SETTINGS",
                style = MaterialTheme.typography.headlineLarge,
                color = TextPrimary
            )
            Text(
                text = "Audio Quality • Equalizer • Background Playback • Sleep Timer • Themes • Appearance",
                style = MaterialTheme.typography.bodySmall,
                color = NeonCyan
            )
        }

        // 1. AUDIO QUALITY
        item {
            StudioRackSection(
                title = "AUDIO QUALITY",
                subtitle = "Honest playback quality options & real-time 64-bit internal DSP precision"
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AudioPlaybackQuality.entries.forEach { option ->
                        val isSelected = audioPlaybackQuality == option
                        Surface(
                            color = if (isSelected) ElectricPurple.copy(alpha = 0.22f) else DeepObsidian,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) NeonCyan else SubtleBorder,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable { onSelectAudioPlaybackQuality(option) }
                                .testTag("playback_quality_${option.name.lowercase()}")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = option.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = if (isSelected) NeonCyan else TextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = option.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(if (isSelected) StudioEmerald else ElevatedCharcoal)
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (isSelected) "Active ✓" else "Select",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSelected) DeepObsidian else TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                // Honest Current Track Technical Stream Readout
                Surface(
                    color = DeepObsidian,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SubtleBorder, RoundedCornerShape(14.dp))
                        .testTag("settings_audio_stream_info")
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "CURRENT TRACK SOURCE STREAM (VERIFIED METADATA)",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonCyan
                        )
                        if (currentTrack != null) {
                            Text(
                                text = "${currentTrack.title} — ${currentTrack.artist}",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary
                            )
                            Text(
                                text = "Format: ${currentTrack.formatBadge} • Sample Rate: ${currentTrack.sampleRateKhzText} (${currentTrack.sampleRateHz} Hz) • Bitrate: ${currentTrack.bitrateKbps} kbps • Channels: ${if (currentTrack.channelCount == 1) "Mono" else "Stereo"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = StudioEmerald
                            )
                        } else {
                            Text(
                                text = "Supported Formats: MP3, WAV, FLAC, AAC, M4A, OGG (8kHz–192kHz, 16/24/32-bit PCM)",
                                style = MaterialTheme.typography.bodySmall,
                                color = StudioEmerald
                            )
                        }
                        Text(
                            text = "Transparency Note: LOFI WAVE preserves the native fidelity of your audio files without unnecessary re-compression. Compressed files (such as MP3/AAC/OGG) are decoded cleanly with 64-bit DSP headroom, without falsely claiming lossy files become lossless.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        // 2. EQUALIZER & STUDIO TOOLS
        item {
            StudioRackSection(
                title = "EQUALIZER & AUDIO STUDIO",
                subtitle = "Active EQ Preset: ${eqState.presetName} • 10-Band Parametric Equalizer"
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = onOpenEqualizer,
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("settings_open_equalizer_button")
                    ) {
                        Text("Open 10-Band EQ", style = MaterialTheme.typography.labelSmall)
                    }
                    Button(
                        onClick = onOpenMusicConverter,
                        colors = ButtonDefaults.buttonColors(containerColor = StudioEmerald),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("settings_open_converter_button")
                    ) {
                        Text("Music Studio", style = MaterialTheme.typography.labelSmall, color = DeepObsidian)
                    }
                    OutlinedButton(
                        onClick = onOpenStudio,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("settings_open_studio_button")
                    ) {
                        Text("DSP Rack", style = MaterialTheme.typography.labelSmall, color = NeonCyan)
                    }
                }
            }
        }

        // 3. PLAYBACK SETTINGS
        item {
            StudioRackSection(
                title = "PLAYBACK SETTINGS",
                subtitle = "Loudness normalization, crossfades & gapless transitions"
            ) {
                ToggleSettingRow(
                    title = "Volume Normalization",
                    subtitle = "Maintain consistent loudness across songs and presets",
                    checked = eqState.volumeNormalization,
                    onCheckedChange = { enabled ->
                        onUpdateEq { it.copy(volumeNormalization = enabled) }
                    }
                )
                ToggleSettingRow(
                    title = "Smooth Track Transitions",
                    subtitle = "Gentle micro-fade when switching tracks or seeking",
                    checked = eqState.smoothTransitions,
                    onCheckedChange = { enabled ->
                        onUpdateEq { it.copy(smoothTransitions = enabled) }
                    }
                )
                ToggleSettingRow(
                    title = "Gapless Playback",
                    subtitle = "Eliminate silence between consecutive album tracks",
                    checked = eqState.gaplessPlayback,
                    onCheckedChange = { enabled ->
                        onUpdateEq { it.copy(gaplessPlayback = enabled) }
                    }
                )
            }
        }

        // 4. BACKGROUND PLAYBACK
        item {
            StudioRackSection(
                title = "BACKGROUND PLAYBACK",
                subtitle = "Native Media3 ExoPlayer + Foreground MediaSessionService"
            ) {
                Text(
                    text = "• Active: Music continues playing smoothly when the screen is locked or while using other apps.\n• Lock-screen & notification media controls (Play, Pause, Previous, Next, Seek) are automatically synced.\n• Automatic Audio Focus & Bluetooth headphone disconnect pause enabled.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        // 5. SLEEP TIMER
        item {
            StudioRackSection(
                title = "SLEEP TIMER",
                subtitle = if (sleepTimerRemainingSec != null) {
                    "Countdown active: ${sleepTimerRemainingSec / 60}m ${sleepTimerRemainingSec % 60}s remaining"
                } else {
                    "Automatically pause playback when you fall asleep"
                }
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf(0 to "Off", 10 to "10m", 20 to "20m", 30 to "30m", 60 to "60m").forEach { (mins, label) ->
                        FilterChip(
                            selected = (mins == 0 && sleepTimerRemainingSec == null),
                            onClick = { onSetSleepTimerMinutes(mins) },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 6. THEME SELECTION & APP APPEARANCE
        item {
            StudioRackSection(
                title = "THEME SELECTION & APP APPEARANCE",
                subtitle = "Active Theme: ${accentTheme.label} (${accentTheme.playerStyleBadge})"
            ) {
                Button(
                    onClick = onOpenThemes,
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_open_themes_button")
                ) {
                    Text(
                        text = "Open All 12 Premium Themes (${accentTheme.label} Active ✓)",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White
                    )
                }

                ToggleSettingRow(
                    title = "Pure AMOLED Deep Obsidian Black (#000000)",
                    subtitle = "Maximum contrast for dark rooms and night listening",
                    checked = pureAmoledBlack,
                    onCheckedChange = { amoled ->
                        onUpdateThemePrefs(accentTheme, amoled, highQualityDecoding)
                    }
                )
            }
        }

        // 7. STORAGE & PERMISSIONS
        item {
            StudioRackSection(
                title = "MUSIC LIBRARY & STORAGE ACCESS",
                subtitle = "Import local songs or folders with persistent URI permissions"
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = onImportSongsClick,
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pick Songs", style = MaterialTheme.typography.labelSmall)
                    }
                    OutlinedButton(
                        onClick = onImportFolderClick,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Pick Folder", style = MaterialTheme.typography.labelSmall, color = NeonCyan)
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = onScanDeviceClick,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Scan MediaStore", style = MaterialTheme.typography.labelSmall, color = AnalogAmber)
                    }
                    OutlinedButton(
                        onClick = onClearHistory,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Clear History", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                }
            }
        }

        // 8. ABOUT LOFI WAVE
        item {
            StudioRackSection(
                title = "ABOUT LOFI WAVE",
                subtitle = "Version 3.0 • Premium Anime Lo-Fi Player & Real-Time Audio Transformation Studio"
            ) {
                Text(
                    text = "LOFI WAVE combines an AMOLED Anime Lo-Fi visual experience with a real-time 64-bit DSP audio engine, 10-band equalizer, Binaural HRTF 3D Spatial & 8D Audio, on-device AI Music Mood Detection, and permanent offline WAV conversion.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun ToggleSettingRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = ElectricPurple
            )
        )
    }
}
