package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * 12+ Complete Visual Themes for LOFI WAVE – Ultimate Premium Music Experience.
 * Each theme defines a unique visual identity, wallpaper gradient palette,
 * surface hierarchy, and player accent styling.
 */
enum class AccentTheme(
    val label: String,
    val subtitle: String,
    val primary: Color,
    val secondary: Color,
    val tertiary: Color = AnalogAmber,
    val bgTop: Color = Color(0xFF0A0914),
    val bgBottom: Color = Color(0xFF15122E),
    val surfaceColor: Color = Color(0xFF131222),
    val cardColor: Color = Color(0xFF1B1930),
    val wallpaperBadge: String = "Anime Twilight"
) {
    ANIME_LOFI_NIGHT(
        label = "Anime Lo-Fi Night",
        subtitle = "Twilight indigo sky, glowing bedroom window & soft violet aura",
        primary = Color(0xFF9D6BFF),
        secondary = Color(0xFF5CE1E6),
        tertiary = Color(0xFFFFB86C),
        bgTop = Color(0xFF090814),
        bgBottom = Color(0xFF191436),
        surfaceColor = Color(0xFF121024),
        cardColor = Color(0xFF1B1833),
        wallpaperBadge = "Tokyo Rooftop 2 AM"
    ),
    MIDNIGHT_PURPLE(
        label = "Midnight Purple",
        subtitle = "Deep velvet amethyst, royal lavender & midnight studio shadow",
        primary = Color(0xFFB37FEB),
        secondary = Color(0xFF85A5FF),
        tertiary = Color(0xFFFF85C0),
        bgTop = Color(0xFF0B0716),
        bgBottom = Color(0xFF22113B),
        surfaceColor = Color(0xFF150E26),
        cardColor = Color(0xFF21163A),
        wallpaperBadge = "Velvet Nocturne"
    ),
    CYBERPUNK_NEON(
        label = "Cyberpunk Neon",
        subtitle = "High-contrast electric magenta, laser cyan & Neo-Tokyo grid",
        primary = Color(0xFFFF2E93),
        secondary = Color(0xFF00F0FF),
        tertiary = Color(0xFFFFE600),
        bgTop = Color(0xFF070612),
        bgBottom = Color(0xFF1A0B2E),
        surfaceColor = Color(0xFF110D22),
        cardColor = Color(0xFF1E1436),
        wallpaperBadge = "Neo-Shinjuku 2099"
    ),
    AMOLED_BLACK(
        label = "AMOLED Black",
        subtitle = "Zero-glow pure #000000 OLED pitch black with crisp studio emerald",
        primary = Color(0xFF2ED573),
        secondary = Color(0xFF5CE1E6),
        tertiary = Color(0xFF9D6BFF),
        bgTop = Color(0xFF000000),
        bgBottom = Color(0xFF050508),
        surfaceColor = Color(0xFF08080C),
        cardColor = Color(0xFF111118),
        wallpaperBadge = "Pure OLED Zero-Light"
    ),
    OCEAN_BLUE(
        label = "Ocean Blue",
        subtitle = "Deep Pacific abyss, bioluminescent aqua & tranquil seafoam",
        primary = Color(0xFF2B86FF),
        secondary = Color(0xFF38EF7D),
        tertiary = Color(0xFF64DFDF),
        bgTop = Color(0xFF050D1A),
        bgBottom = Color(0xFF0B2545),
        surfaceColor = Color(0xFF0A192F),
        cardColor = Color(0xFF112240),
        wallpaperBadge = "Deep Sea Bioluminescence"
    ),
    SAKURA_PINK(
        label = "Sakura Pink",
        subtitle = "Kyoto spring cherry blossoms, rose quartz & warm cream twilight",
        primary = Color(0xFFFF7597),
        secondary = Color(0xFFFFC6FF),
        tertiary = Color(0xFFFFD6A5),
        bgTop = Color(0xFF140A14),
        bgBottom = Color(0xFF2B142C),
        surfaceColor = Color(0xFF1D101E),
        cardColor = Color(0xFF2A182C),
        wallpaperBadge = "Kyoto Petal Drift"
    ),
    SUNSET_ORANGE(
        label = "Sunset Orange",
        subtitle = "Golden hour cassette warmth, amber horizon & dusk coral",
        primary = Color(0xFFFF8C42),
        secondary = Color(0xFFFF5E7E),
        tertiary = Color(0xFFFFD166),
        bgTop = Color(0xFF140B08),
        bgBottom = Color(0xFF2E1510),
        surfaceColor = Color(0xFF1F110E),
        cardColor = Color(0xFF2C1915),
        wallpaperBadge = "Shibuya Golden Hour"
    ),
    GALAXY_DREAMS(
        label = "Galaxy Dreams",
        subtitle = "Cosmic nebula stardust, astral violet & celeste shimmer",
        primary = Color(0xFF7B61FF),
        secondary = Color(0xFF00D2FF),
        tertiary = Color(0xFFF97794),
        bgTop = Color(0xFF06071B),
        bgBottom = Color(0xFF151940),
        surfaceColor = Color(0xFF0D102B),
        cardColor = Color(0xFF171C42),
        wallpaperBadge = "Andromeda Stardust"
    ),
    RAINY_WINDOW(
        label = "Rainy Window",
        subtitle = "Misty slate blue glass, warm streetlamp reflections & cozy rain",
        primary = Color(0xFF64B5F6),
        secondary = Color(0xFF80CBC4),
        tertiary = Color(0xFFFFCC80),
        bgTop = Color(0xFF0A1017),
        bgBottom = Color(0xFF162433),
        surfaceColor = Color(0xFF101A24),
        cardColor = Color(0xFF192736),
        wallpaperBadge = "Midnight Raindrop Pane"
    ),
    RETRO_SYNTHWAVE(
        label = "Retro Synthwave",
        subtitle = "1984 analog outrun horizon, chrome sunset & VHS neon purple",
        primary = Color(0xFFF72585),
        secondary = Color(0xFF4CC9F0),
        tertiary = Color(0xFFFF9E00),
        bgTop = Color(0xFF0D0221),
        bgBottom = Color(0xFF261447),
        surfaceColor = Color(0xFF190B33),
        cardColor = Color(0xFF28124D),
        wallpaperBadge = "Outrun Analog 1984"
    ),
    FOREST_CALM(
        label = "Forest Calm",
        subtitle = "Ghibli moss sanctuary, misty pine canopy & firefly amber",
        primary = Color(0xFF52B788),
        secondary = Color(0xFF95D5B2),
        tertiary = Color(0xFFFFD166),
        bgTop = Color(0xFF07120D),
        bgBottom = Color(0xFF122A1F),
        surfaceColor = Color(0xFF0C1C14),
        cardColor = Color(0xFF152D22),
        wallpaperBadge = "Misty Cedar Sanctuary"
    ),
    MINIMAL_PREMIUM(
        label = "Minimal Premium",
        subtitle = "Architectural titanium slate, warm platinum & studio champagne",
        primary = Color(0xFFE2E8F0),
        secondary = Color(0xFF94A3B8),
        tertiary = Color(0xFFF59E0B),
        bgTop = Color(0xFF0B0D11),
        bgBottom = Color(0xFF161A22),
        surfaceColor = Color(0xFF11141B),
        cardColor = Color(0xFF1B202B),
        wallpaperBadge = "Mastering Console Slate"
    ),

    // Backward-compatible legacy enum entries mapped to the design system
    ELECTRIC_PURPLE(
        label = "Soft Violet & Sky Blue",
        subtitle = "Classic LOFI WAVE violet & cyan studio palette",
        primary = ElectricPurple,
        secondary = NeonCyan,
        tertiary = AnalogAmber,
        bgTop = DeepObsidian,
        bgBottom = Color(0xFF14112E),
        surfaceColor = MidnightCharcoal,
        cardColor = ElevatedCharcoal,
        wallpaperBadge = "Classic Lo-Fi Wave"
    ),
    CYBER_BLUE(
        label = "Celestial Blue & Sakura",
        subtitle = "Vibrant sky blue paired with sakura vinyl rose",
        primary = NeonBlue,
        secondary = VinylRose,
        tertiary = NeonCyan,
        bgTop = DeepObsidian,
        bgBottom = Color(0xFF0E1B33),
        surfaceColor = MidnightCharcoal,
        cardColor = ElevatedCharcoal,
        wallpaperBadge = "Celestial Horizon"
    ),
    WARM_TAPE(
        label = "Sunset Amber & Lavender",
        subtitle = "Analog tape reel amber with soft lavender accents",
        primary = AnalogAmber,
        secondary = ElectricPurpleBright,
        tertiary = VinylRose,
        bgTop = DeepObsidian,
        bgBottom = Color(0xFF241712),
        surfaceColor = MidnightCharcoal,
        cardColor = ElevatedCharcoal,
        wallpaperBadge = "Analog Reel Warmth"
    ),
    MIDNIGHT_EMERALD(
        label = "Mint Aurora & Violet",
        subtitle = "Crisp emerald studio meters with deep violet",
        primary = StudioEmerald,
        secondary = ElectricPurple,
        tertiary = NeonCyan,
        bgTop = DeepObsidian,
        bgBottom = Color(0xFF0D241E),
        surfaceColor = MidnightCharcoal,
        cardColor = ElevatedCharcoal,
        wallpaperBadge = "Northern Aurora"
    );

    val wallpaperTop: Color get() = bgTop
    val wallpaperBottom: Color get() = bgBottom
    val surfaceBg: Color get() = surfaceColor
    val cardBg: Color get() = cardColor
    val playerStyleBadge: String get() = wallpaperBadge

    companion object {
        /**
         * Returns the 12 primary showcase themes in order.
         */
        val PRIMARY_TWELVE: List<AccentTheme> = listOf(
            ANIME_LOFI_NIGHT,
            MIDNIGHT_PURPLE,
            CYBERPUNK_NEON,
            AMOLED_BLACK,
            OCEAN_BLUE,
            SAKURA_PINK,
            SUNSET_ORANGE,
            GALAXY_DREAMS,
            RAINY_WINDOW,
            RETRO_SYNTHWAVE,
            FOREST_CALM,
            MINIMAL_PREMIUM
        )
    }
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
    accentTheme: AccentTheme = AccentTheme.ANIME_LOFI_NIGHT,
    pureAmoledBlack: Boolean = false,
    content: @Composable () -> Unit
) {
    val forceAmoled = pureAmoledBlack || accentTheme == AccentTheme.AMOLED_BLACK
    val bg = if (forceAmoled) Color(0xFF000000) else accentTheme.bgTop
    val surface = if (forceAmoled) Color(0xFF07070B) else accentTheme.surfaceColor
    val cardVariant = if (forceAmoled) Color(0xFF101018) else accentTheme.cardColor

    val colorScheme = darkColorScheme(
        primary = accentTheme.primary,
        onPrimary = Color.White,
        primaryContainer = cardVariant,
        onPrimaryContainer = TextPrimary,
        secondary = accentTheme.secondary,
        onSecondary = DeepObsidian,
        secondaryContainer = GlassSurface,
        onSecondaryContainer = TextPrimary,
        tertiary = accentTheme.tertiary,
        onTertiary = DeepObsidian,
        background = bg,
        onBackground = TextPrimary,
        surface = surface,
        onSurface = TextPrimary,
        surfaceVariant = cardVariant,
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
