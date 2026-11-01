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
import com.example.model.EqualizerState
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
    onSaveCustomEqPreset: (String) -> Unit
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
                                    onSelectBuiltInPreset(custom.name, parsed)
                                }
                            },
                            label = { Text("★ ${custom.name}", style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        }

        // 10-Band Individual Frequency Controls
        item {
            StudioRackSection(
                title = "10-BAND GRAPHIC FREQUENCY SLIDERS (-12dB TO +12dB)",
                subtitle = "Real-time biquad peaking filters applied across 31Hz – 16kHz"
            ) {
                for (i in 0 until 10) {
                    val freqLabel = EqualizerState.BAND_LABELS[i]
                    val gain = eqState.bandGainsDb.getOrElse(i) { 0f }
                    DspParameterSlider(
                        label = "$freqLabel Hz Band",
                        valueText = String.format(Locale.US, "%+.1f dB", gain),
                        value = gain,
                        valueRange = -12f..12f,
                        accentColor = if (i < 3) ElectricPurple else if (i < 7) NeonCyan else AnalogAmber,
                        testTag = "eq_band_slider_$i",
                        onValueChange = { newGain -> onUpdateBand(i, newGain) }
                    )
                }
            }
        }

        // Bass, Treble & Stereo Balance
        item {
            StudioRackSection(
                title = "BASS, TREBLE & STEREO BALANCE",
                subtitle = "Master low/high shelf reinforcement and Left/Right channel pan"
            ) {
                DspParameterSlider(
                    label = "Bass Control",
                    valueText = String.format(Locale.US, "%+.1f dB", eqState.bassBoostDb),
                    value = eqState.bassBoostDb,
                    valueRange = -12f..12f,
                    accentColor = ElectricPurple,
                    testTag = "eq_bass_slider",
                    onValueChange = { v -> onUpdateEq { it.copy(bassBoostDb = v) } }
                )
                DspParameterSlider(
                    label = "Treble Control",
                    valueText = String.format(Locale.US, "%+.1f dB", eqState.trebleBoostDb),
                    value = eqState.trebleBoostDb,
                    valueRange = -12f..12f,
                    accentColor = NeonCyan,
                    testTag = "eq_treble_slider",
                    onValueChange = { v -> onUpdateEq { it.copy(trebleBoostDb = v) } }
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
                    accentColor = StudioEmerald,
                    testTag = "eq_balance_slider",
                    onValueChange = { v -> onUpdateEq { it.copy(stereoBalance = v) } }
                )
            }
        }

        // High-Quality Audio & Transitions
        item {
            StudioRackSection(
                title = "HIGH-QUALITY AUDIO & TRANSITIONS",
                subtitle = "Soft limiter normalization, fade-in/out & gapless playback"
            ) {
                ToggleSettingRow(
                    title = "Volume Normalization & Anti-Clip Soft Limiter",
                    subtitle = "Prevents digital clipping when stacking bass boost & saturation",
                    checked = eqState.volumeNormalization,
                    onCheckedChange = { v -> onUpdateEq { it.copy(volumeNormalization = v) } }
                )
                ToggleSettingRow(
                    title = "Smooth Track Transitions (Fade-In)",
                    subtitle = "Applies smooth ${eqState.fadeInOutMs}ms envelope on track start & seek",
                    checked = eqState.smoothTransitions,
                    onCheckedChange = { v -> onUpdateEq { it.copy(smoothTransitions = v) } }
                )
                ToggleSettingRow(
                    title = "Gapless Playback",
                    subtitle = "Seamless transitions between queued tracks without silence gaps",
                    checked = eqState.gaplessPlayback,
                    onCheckedChange = { v -> onUpdateEq { it.copy(gaplessPlayback = v) } }
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
    onUpdateThemePrefs: (AccentTheme, Boolean, Boolean) -> Unit,
    onSetSleepTimerMinutes: (Int) -> Unit,
    onImportSongsClick: () -> Unit,
    onImportFolderClick: () -> Unit,
    onScanDeviceClick: () -> Unit,
    onClearHistory: () -> Unit,
    onOpenStudio: () -> Unit = {},
    onOpenEqualizer: () -> Unit = {},
    onOpenMusicConverter: () -> Unit = {}
) {
    var previewedTheme by remember(accentTheme) { mutableStateOf(accentTheme) }

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
                text = "SETTINGS & 12 STUDIO THEMES",
                style = MaterialTheme.typography.headlineLarge,
                color = TextPrimary
            )
            Text(
                text = "12 Anime & Minimal Visual Themes, Audio Tools, Sleep Timer & Library",
                style = MaterialTheme.typography.bodySmall,
                color = NeonCyan
            )
        }

        // Quick Access to Advanced Audio Tools (Lo-Fi Studio, Music Converter & 10-Band Equalizer)
        item {
            StudioRackSection(
                title = "ADVANCED AUDIO STUDIO & CONVERTER TOOLS",
                subtitle = "Fine-tune real-time Lo-Fi DSP parameters, 10-band EQ, or convert & export songs"
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = onOpenStudio,
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("settings_open_studio_button")
                    ) {
                        Text("Lo-Fi Studio", style = MaterialTheme.typography.labelSmall)
                    }
                    Button(
                        onClick = onOpenMusicConverter,
                        colors = ButtonDefaults.buttonColors(containerColor = StudioEmerald),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("settings_open_converter_button")
                    ) {
                        Text("Converter", style = MaterialTheme.typography.labelSmall, color = DeepObsidian)
                    }
                    OutlinedButton(
                        onClick = onOpenEqualizer,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("settings_open_equalizer_button")
                    ) {
                        Text("10-Band EQ", style = MaterialTheme.typography.labelSmall, color = NeonCyan)
                    }
                }
            }
        }

        // PART 1: 12 COMPLETE PREMIUM THEMES WITH LIVE PREVIEW & APPLY BUTTON
        item {
            StudioRackSection(
                title = "12 COMPLETE PREMIUM THEMES",
                subtitle = "Preview wallpaper gradients, color palettes & player styling — persisted automatically"
            ) {
                // Live Interactive Theme Preview Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(previewedTheme.wallpaperTop, previewedTheme.wallpaperBottom)
                            )
                        )
                        .border(1.5.dp, previewedTheme.primary, RoundedCornerShape(18.dp))
                        .padding(16.dp)
                        .testTag("theme_live_preview_card")
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "THEME PREVIEW: ${previewedTheme.label.uppercase()}",
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

                        // Simulated mini player styling preview bar
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(previewedTheme.cardBg)
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(previewedTheme.primary)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(previewedTheme.secondary)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(previewedTheme.tertiary)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = if (previewedTheme == accentTheme) "Currently Applied Theme" else "Tap Apply to Activate",
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
                                    text = if (previewedTheme == accentTheme) "Applied ✓" else "Apply Theme",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // All 12 Complete Themes List
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (theme in AccentTheme.PRIMARY_TWELVE) {
                        val isApplied = theme == accentTheme
                        val isPreviewed = theme == previewedTheme
                        Surface(
                            color = if (isApplied) theme.primary.copy(alpha = 0.22f) else theme.surfaceBg,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = if (isApplied || isPreviewed) 1.5.dp else 1.dp,
                                    color = when {
                                        isApplied -> theme.secondary
                                        isPreviewed -> theme.primary
                                        else -> SubtleBorder
                                    },
                                    shape = RoundedCornerShape(14.dp)
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
                                    // Wallpaper & Palette Swatch
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                Brush.linearGradient(
                                                    listOf(theme.wallpaperTop, theme.primary, theme.secondary)
                                                )
                                            )
                                            .border(1.dp, theme.secondary.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clip(CircleShape)
                                                .background(theme.tertiary)
                                        )
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
                                            text = if (isApplied) "Active" else "Apply",
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
                    title = "Pure AMOLED Deep Obsidian Black (#000000)",
                    subtitle = "Maximum contrast for dark rooms and night listening",
                    checked = pureAmoledBlack,
                    onCheckedChange = { amoled ->
                        onUpdateThemePrefs(accentTheme, amoled, highQualityDecoding)
                    }
                )
            }
        }

        // Sleep Timer
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

        // Audio Quality & Background Playback Info
        item {
            StudioRackSection(
                title = "AUDIO ENGINE & BACKGROUND SERVICE",
                subtitle = "Native Media3 ExoPlayer + MediaSessionService architecture"
            ) {
                ToggleSettingRow(
                    title = "High-Precision 16-Bit Stereo PCM Pipeline",
                    subtitle = "Zero-latency real-time biquad filtering, reverb & vinyl synthesis",
                    checked = highQualityDecoding,
                    onCheckedChange = { hq ->
                        onUpdateThemePrefs(accentTheme, pureAmoledBlack, hq)
                    }
                )
                Text(
                    text = "• Foreground MediaSessionService keeps music playing when minimized, in other apps, or with screen locked.\n• Automatic Audio Focus & Bluetooth headset disconnect handling enabled.\n• Original device music files are never modified or overwritten.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        // Storage & Permission Management
        item {
            StudioRackSection(
                title = "STORAGE & PERSISTENT URI PERMISSIONS",
                subtitle = "Manage device music access via Android SAF and MediaStore"
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
