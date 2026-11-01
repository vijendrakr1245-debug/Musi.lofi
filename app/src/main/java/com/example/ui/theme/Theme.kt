package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

enum class AccentTheme(val label: String, val primary: Color, val secondary: Color) {
    ELECTRIC_PURPLE("Soft Violet & Sky Blue", ElectricPurple, NeonCyan),
    CYBER_BLUE("Celestial Blue & Sakura", NeonBlue, VinylRose),
    WARM_TAPE("Sunset Amber & Lavender", AnalogAmber, ElectricPurpleBright),
    MIDNIGHT_EMERALD("Mint Aurora & Violet", StudioEmerald, ElectricPurple)
}

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(34.dp)
)

@Composable
fun MyApplicationTheme(
    accentTheme: AccentTheme = AccentTheme.ELECTRIC_PURPLE,
    pureAmoledBlack: Boolean = true,
    content: @Composable () -> Unit
) {
    val bg = if (pureAmoledBlack) DeepObsidian else Color(0xFF0E1124)
    val surface = if (pureAmoledBlack) MidnightCharcoal else Color(0xFF161932)
    val colorScheme = darkColorScheme(
        primary = accentTheme.primary,
        onPrimary = Color.White,
        primaryContainer = ElevatedCharcoal,
        onPrimaryContainer = TextPrimary,
        secondary = accentTheme.secondary,
        onSecondary = DeepObsidian,
        secondaryContainer = GlassSurface,
        onSecondaryContainer = TextPrimary,
        tertiary = AnalogAmber,
        onTertiary = DeepObsidian,
        background = bg,
        onBackground = TextPrimary,
        surface = surface,
        onSurface = TextPrimary,
        surfaceVariant = ElevatedCharcoal,
        onSurfaceVariant = TextSecondary,
        outline = SubtleBorder,
        error = VinylRose
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}

