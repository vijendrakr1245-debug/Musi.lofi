package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

enum class AccentTheme(val label: String, val primary: Color, val secondary: Color) {
    ELECTRIC_PURPLE("Electric Purple & Cyan", ElectricPurple, NeonCyan),
    CYBER_BLUE("Neon Blue & Magenta", NeonBlue, Color(0xFFEC4899)),
    WARM_TAPE("Warm Cassette Amber", AnalogAmber, ElectricPurpleBright),
    MIDNIGHT_EMERALD("Midnight Emerald & Purple", StudioEmerald, ElectricPurple)
}

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun MyApplicationTheme(
    accentTheme: AccentTheme = AccentTheme.ELECTRIC_PURPLE,
    pureAmoledBlack: Boolean = true,
    content: @Composable () -> Unit
) {
    val bg = if (pureAmoledBlack) DeepObsidian else Color(0xFF0E0C18)
    val surface = if (pureAmoledBlack) MidnightCharcoal else Color(0xFF161326)
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
