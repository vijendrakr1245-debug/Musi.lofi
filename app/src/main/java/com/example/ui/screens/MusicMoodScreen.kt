package com.example.ui.screens

import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SentimentDissatisfied
import androidx.compose.material.icons.filled.SentimentVerySatisfied
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.model.DetectedMusicMood
import com.example.model.LofiDspParameters
import com.example.model.LofiPreset
import com.example.model.MusicMoodAnalysisResult
import com.example.model.TrackItem
import com.example.ui.components.formatDurationMillis
import com.example.ui.theme.AnalogAmber
import com.example.ui.theme.DeepObsidian
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.ElectricPurpleBright
import com.example.ui.theme.ElevatedCharcoal
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.NeonBlue
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.StudioEmerald
import com.example.ui.theme.SubtleBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VinylRose
import java.util.Locale

fun moodAccentColor(mood: DetectedMusicMood): Color = when (mood) {
    DetectedMusicMood.HAPPY -> AnalogAmber
    DetectedMusicMood.SAD -> NeonCyan
    DetectedMusicMood.ROMANTIC -> VinylRose
    DetectedMusicMood.RELAXING -> ElectricPurpleBright
    DetectedMusicMood.ENERGETIC -> StudioEmerald
    DetectedMusicMood.PEACEFUL -> NeonBlue
    DetectedMusicMood.DREAMY -> ElectricPurple
}

fun moodVectorIcon(mood: DetectedMusicMood): ImageVector = when (mood) {
    DetectedMusicMood.HAPPY -> Icons.Default.SentimentVerySatisfied
    DetectedMusicMood.SAD -> Icons.Default.SentimentDissatisfied
    DetectedMusicMood.ROMANTIC -> Icons.Default.Favorite
    DetectedMusicMood.RELAXING -> Icons.Default.NightlightRound
    DetectedMusicMood.ENERGETIC -> Icons.Default.Bolt
    DetectedMusicMood.PEACEFUL -> Icons.Default.Spa
    DetectedMusicMood.DREAMY -> Icons.Default.AutoAwesome
}

@Composable
fun MusicMoodScreen(
    currentTrack: TrackItem?,
    moodResult: MusicMoodAnalysisResult?,
    isAnalyzing: Boolean,
    dspParams: LofiDspParameters,
    onAnalyzeAgain: () -> Unit,
    onApplyPreset: (LofiPreset) -> Unit,
    onIgnoreSuggestion: () -> Unit,
    onBack: () -> Unit
) {
    val activeMood = moodResult?.detectedMood ?: DetectedMusicMood.RELAXING
    val accent by animateColorAsState(
        targetValue = moodAccentColor(activeMood),
        animationSpec = tween(durationMillis = 320),
        label = "mood_screen_accent"
    )

    LazyColumn(
        contentPadding = PaddingValues(top = 12.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        DeepObsidian,
                        Color(0xFF13112B),
                        DeepObsidian
                    )
                )
            )
            .padding(horizontal = 20.dp)
            .testTag("music_mood_screen")
    ) {
        // 1. Top Navigation & Screen Header
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
                            .testTag("mood_screen_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "AI MUSIC MOOD",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "On-Device Acoustic Signal Analysis • Private & Offline",
                            style = MaterialTheme.typography.bodySmall,
                            color = NeonCyan
                        )
                    }
                }

                IconButton(
                    onClick = onAnalyzeAgain,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(ElevatedCharcoal)
                        .border(1.dp, SubtleBorder, CircleShape)
                        .testTag("mood_header_refresh_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Analyze Again",
                        tint = NeonCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // 2. Current Song Card (Verified File Metadata)
        item {
            Surface(
                color = ElevatedCharcoal,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SubtleBorder, RoundedCornerShape(24.dp))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, SubtleBorder, RoundedCornerShape(16.dp))
                    ) {
                        if (!currentTrack?.albumArtUri.isNullOrBlank()) {
                            AsyncImage(
                                model = Uri.parse(currentTrack?.albumArtUri),
                                contentDescription = "Song Artwork",
                                contentScale = ContentScale.Crop,
                                error = painterResource(id = R.drawable.img_lofi_studio_banner),
                                placeholder = painterResource(id = R.drawable.img_lofi_studio_banner),
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Image(
                                painter = painterResource(id = R.drawable.img_lofi_studio_banner),
                                contentDescription = "Anime Lo-Fi Artwork",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = null,
                                tint = StudioEmerald,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "VERIFIED TRACK METADATA",
                                style = MaterialTheme.typography.labelSmall,
                                color = StudioEmerald
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = currentTrack?.title ?: moodResult?.verifiedTitle ?: "No Track Selected",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("mood_song_title")
                        )
                        Text(
                            text = currentTrack?.artist ?: moodResult?.verifiedArtist ?: "Select a song to analyze",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("mood_song_artist")
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${currentTrack?.formatBadge ?: "WAV"} • ${formatDurationMillis(currentTrack?.durationMs ?: 0L)} • ${currentTrack?.album ?: "Local Storage"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // 3. Detected Mood & Visual Representation Hero Card
        item {
            Surface(
                color = ElevatedCharcoal.copy(alpha = 0.92f),
                shape = RoundedCornerShape(28.dp),
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.5.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                accent.copy(alpha = 0.75f),
                                ElectricPurple.copy(alpha = 0.35f),
                                SubtleBorder
                            )
                        ),
                        shape = RoundedCornerShape(28.dp)
                    )
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp)
                ) {
                    // Estimated vs Verified distinction pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(GlassSurface)
                            .border(1.dp, accent.copy(alpha = 0.55f), RoundedCornerShape(50))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isAnalyzing) {
                                "ANALYZING LOCAL AUDIO FRAMES..."
                            } else {
                                "ESTIMATED ACOUSTIC MOOD • ON-DEVICE DSP"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = accent
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Glowing Mood Icon & Aura Representation
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(116.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        accent.copy(alpha = 0.38f),
                                        ElectricPurple.copy(alpha = 0.14f),
                                        Color.Transparent
                                    )
                                ),
                                radius = size.minDimension / 1.8f
                            )
                        }

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(82.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            accent.copy(alpha = 0.32f),
                                            GlassSurface
                                        )
                                    )
                                )
                                .border(2.dp, accent, CircleShape)
                        ) {
                            Icon(
                                imageVector = moodVectorIcon(activeMood),
                                contentDescription = activeMood.displayName,
                                tint = accent,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Detected Mood Name + Emoji
                    Text(
                        text = if (moodResult != null && moodResult.isSignalSufficient) {
                            "${activeMood.displayName} ${activeMood.emoji}"
                        } else {
                            "Signal Needed"
                        },
                        style = MaterialTheme.typography.displayMedium,
                        color = TextPrimary,
                        modifier = Modifier.testTag("mood_detected_title")
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Confidence Percentage Badge (ONLY shown when analysis supports it)
                    if (moodResult != null && moodResult.isSignalSufficient && moodResult.confidencePercent != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(accent.copy(alpha = 0.18f))
                                .border(1.dp, accent.copy(alpha = 0.65f), RoundedCornerShape(50))
                                .padding(horizontal = 14.dp, vertical = 5.dp)
                                .testTag("mood_confidence_badge")
                        ) {
                            Text(
                                text = "${moodResult.confidencePercent}% Acoustic Confidence",
                                style = MaterialTheme.typography.labelLarge,
                                color = accent
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(GlassSurface)
                                .padding(horizontal = 14.dp, vertical = 5.dp)
                                .testTag("mood_insufficient_badge")
                        ) {
                            Text(
                                text = "Confidence withheld — play audio to measure signal",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = if (moodResult != null && moodResult.isSignalSufficient) {
                            activeMood.subtitle
                        } else {
                            "Start playback or select a readable local track to measure tempo, energy, and spectral balance."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Visual Representation: Multi-Bar Acoustic Spectrum & Feature Radar Bars
                    if (moodResult != null && moodResult.isSignalSufficient) {
                        AcousticVisualRepresentation(
                            result = moodResult,
                            accent = accent
                        )
                    }
                }
            }
        }

        // 4. Smart Lo-Fi Preset Suggestion Card (Apply or Ignore — Never Auto-Applied)
        if (moodResult != null) {
            item {
                val suggested = moodResult.suggestedPreset
                val secondary = moodResult.secondaryPreset
                val isAlreadyActive = dspParams.isLiveLofiEnabled && dspParams.presetId == suggested.id

                Surface(
                    color = ElevatedCharcoal,
                    shape = RoundedCornerShape(26.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = if (isAlreadyActive) StudioEmerald else accent.copy(alpha = 0.65f),
                            shape = RoundedCornerShape(26.dp)
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.GraphicEq,
                                    contentDescription = null,
                                    tint = accent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "SUGGESTED LO-FI PRESET",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = accent
                                )
                            }

                            Text(
                                text = "${activeMood.displayName} → ${suggested.name}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }

                        // Primary Suggested Preset Box
                        Surface(
                            color = GlassSurface.copy(alpha = 0.75f),
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, SubtleBorder, RoundedCornerShape(18.dp))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = suggested.name,
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = TextPrimary,
                                        modifier = Modifier.testTag("mood_suggested_preset_name")
                                    )
                                    if (isAlreadyActive || moodResult.isSuggestionApplied) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = StudioEmerald,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "ACTIVE",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = StudioEmerald
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = suggested.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "DSP Profile: ${suggested.parameters.lowPassCutoffHz.toInt()}Hz Low-Pass • " +
                                        "${(suggested.parameters.reverbMix * 100).toInt()}% Reverb • " +
                                        "+${suggested.parameters.warmBassBoostDb}dB Warm Bass",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NeonCyan
                                )
                            }
                        }

                        // Alternative / Secondary Suggestion Pill
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(DeepObsidian.copy(alpha = 0.55f))
                                .clickable { onApplyPreset(secondary) }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .testTag("mood_secondary_preset_button")
                        ) {
                            Text(
                                text = "Alternative match: ${secondary.name}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            Text(
                                text = "Try ${secondary.name} →",
                                style = MaterialTheme.typography.labelSmall,
                                color = ElectricPurpleBright
                            )
                        }

                        // User Permission Note
                        Text(
                            text = "Audio effects are never changed automatically. Apply the suggested preset or ignore to keep your current sound.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )

                        // Action Buttons: Apply Preset & Ignore Suggestion
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = { onApplyPreset(suggested) },
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple),
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(46.dp)
                                    .testTag("mood_apply_preset_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isAlreadyActive) "Preset Applied" else "Apply Preset",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }

                            OutlinedButton(
                                onClick = onIgnoreSuggestion,
                                modifier = Modifier
                                    .weight(0.9f)
                                    .height(46.dp)
                                    .testTag("mood_ignore_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (moodResult.isSuggestionIgnored) "Ignored" else "Ignore",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = TextSecondary
                                )
                            }
                        }

                        // Analyze Again Button
                        OutlinedButton(
                            onClick = onAnalyzeAgain,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("mood_analyze_again_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isAnalyzing) "Analyzing Audio..." else "Analyze Again",
                                style = MaterialTheme.typography.labelLarge,
                                color = NeonCyan
                            )
                        }
                    }
                }
            }
        }

        // 5. All 7 Moods Probability Breakdown & Scientific Transparency Note
        if (moodResult != null && moodResult.isSignalSufficient) {
            item {
                Surface(
                    color = ElevatedCharcoal,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SubtleBorder, RoundedCornerShape(24.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "MOOD SPECTRUM BREAKDOWN (7 MOODS)",
                            style = MaterialTheme.typography.labelLarge,
                            color = TextSecondary
                        )

                        val orderedMoods = DetectedMusicMood.entries.sortedByDescending {
                            moodResult.moodProbabilities[it] ?: 0f
                        }

                        for (mood in orderedMoods) {
                            val prob = (moodResult.moodProbabilities[mood] ?: 0f).coerceIn(0f, 1f)
                            val isTop = mood == moodResult.detectedMood
                            val barColor = moodAccentColor(mood)

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "${mood.emoji} ${mood.displayName}  →  ${mood.primaryPresetLabel}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = if (isTop) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isTop) TextPrimary else TextSecondary
                                    )
                                    Text(
                                        text = "${(prob * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isTop) barColor else TextMuted
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { prob.coerceIn(0.03f, 1f) },
                                    color = barColor,
                                    trackColor = DeepObsidian,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Note: ${moodResult.acousticReasoning} Acoustic signal measurements estimate musical characteristics (tempo, spectral centroid, and dynamics) rather than verified human emotions. 100% processed locally on your device.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AcousticVisualRepresentation(
    result: MusicMoodAnalysisResult,
    accent: Color
) {
    val f = result.acousticFeatures
    val tempoNorm = ((f.estimatedTempoBpm - 60f) / 85f).coerceIn(0.08f, 1f)

    val metrics = listOf(
        Triple("Tempo", "${f.estimatedTempoBpm.toInt()} BPM", tempoNorm),
        Triple("Energy", "${(f.rmsEnergyNormalized * 100).toInt()}%", f.rmsEnergyNormalized),
        Triple("Brightness", "${(f.spectralBrightnessNormalized * 100).toInt()}%", f.spectralBrightnessNormalized),
        Triple("Rhythm", "${(f.transientDensityNormalized * 100).toInt()}%", f.transientDensityNormalized),
        Triple("Warmth", "${(f.warmMidRatioNormalized * 100).toInt()}%", f.warmMidRatioNormalized),
        Triple("Stereo", "${(f.stereoWidthNormalized * 100).toInt()}%", f.stereoWidthNormalized)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(DeepObsidian.copy(alpha = 0.65f))
            .border(1.dp, SubtleBorder, RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "MEASURED ACOUSTIC FEATURES",
                style = MaterialTheme.typography.labelSmall,
                color = NeonCyan
            )
            Text(
                text = String.format(Locale.US, "%,d PCM frames", f.analyzedFramesCount),
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
        }

        // Visual Bar Graph Canvas
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
        ) {
            val w = size.width
            val h = size.height
            val count = metrics.size
            val gap = 12.dp.toPx()
            val barWidth = ((w - gap * (count - 1)) / count).coerceAtLeast(8f)

            metrics.forEachIndexed { idx, (_, _, normVal) ->
                val clamped = normVal.coerceIn(0.10f, 1f)
                val barH = h * clamped
                val x = idx * (barWidth + gap)
                val topY = h - barH

                // Background track bar
                drawRoundRect(
                    color = GlassSurface,
                    topLeft = Offset(x, 0f),
                    size = Size(barWidth, h),
                    cornerRadius = CornerRadius(10f, 10f)
                )

                // Active feature bar
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(accent, ElectricPurple),
                        startY = topY,
                        endY = h
                    ),
                    topLeft = Offset(x, topY),
                    size = Size(barWidth, barH),
                    cornerRadius = CornerRadius(10f, 10f)
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            for ((label, valueStr, _) in metrics) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = valueStr,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextPrimary,
                        maxLines = 1
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
