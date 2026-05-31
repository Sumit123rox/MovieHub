package com.moviehub.core.ui.theme

import androidx.compose.ui.graphics.Color

// ===== ACCENT COLORS (10 options) =====
enum class AccentType(val label: String) {
    RED("Red"),
    BLUE("Blue"),
    PURPLE("Purple"),
    GREEN("Green"),
    ORANGE("Orange"),
    PINK("Pink"),
    TEAL("Teal"),
    YELLOW("Yellow"),
    CYAN("Cyan"),
    ROSE("Rose"),
}

data class AccentPalette(
    val primary: Color,
    val primaryDark: Color,
    val primaryLight: Color,
    val onPrimary: Color = Color.White,
    val secondary: Color,
    val tertiary: Color,
)

object Accents {
    val Red = AccentPalette(
        primary = Color(0xFFE50914),
        primaryDark = Color(0xFFB20710),
        primaryLight = Color(0xFFFF3B44),
        secondary = Color(0xFFCCC2DC),
        tertiary = Color(0xFFEFB8C8),
    )

    val Blue = AccentPalette(
        primary = Color(0xFF2196F3),
        primaryDark = Color(0xFF1976D2),
        primaryLight = Color(0xFF64B5F6),
        secondary = Color(0xFFB0BEC5),
        tertiary = Color(0xFF80DEEA),
    )

    val Purple = AccentPalette(
        primary = Color(0xFF9C27B0),
        primaryDark = Color(0xFF7B1FA2),
        primaryLight = Color(0xFFCE93D8),
        secondary = Color(0xFFB39DDB),
        tertiary = Color(0xFFEA80FC),
    )

    val Green = AccentPalette(
        primary = Color(0xFF4CAF50),
        primaryDark = Color(0xFF388E3C),
        primaryLight = Color(0xFF81C784),
        secondary = Color(0xFFA5D6A7),
        tertiary = Color(0xFF66BB6A),
    )

    val Orange = AccentPalette(
        primary = Color(0xFFFF9800),
        primaryDark = Color(0xFFF57C00),
        primaryLight = Color(0xFFFFB74D),
        secondary = Color(0xFFFFCC80),
        tertiary = Color(0xFFFFAB40),
    )

    val Pink = AccentPalette(
        primary = Color(0xFFE91E63),
        primaryDark = Color(0xFFC2185B),
        primaryLight = Color(0xFFF48FB1),
        secondary = Color(0xFFF8BBD0),
        tertiary = Color(0xFFFF80AB),
    )

    val Teal = AccentPalette(
        primary = Color(0xFF009688),
        primaryDark = Color(0xFF00796B),
        primaryLight = Color(0xFF4DB6AC),
        secondary = Color(0xFF80CBC4),
        tertiary = Color(0xFF26A69A),
    )

    val Yellow = AccentPalette(
        primary = Color(0xFFFFEB3B),
        primaryDark = Color(0xFFFBC02D),
        primaryLight = Color(0xFFFFF176),
        onPrimary = Color(0xFF1C1B1F),
        secondary = Color(0xFFE6EE9C),
        tertiary = Color(0xFFFFF59D),
    )

    val Cyan = AccentPalette(
        primary = Color(0xFF00BCD4),
        primaryDark = Color(0xFF0097A7),
        primaryLight = Color(0xFF4DD0E1),
        secondary = Color(0xFF80DEEA),
        tertiary = Color(0xFF18FFFF),
    )

    val Rose = AccentPalette(
        primary = Color(0xFFFF5252),
        primaryDark = Color(0xFFD32F2F),
        primaryLight = Color(0xFFFF8A80),
        secondary = Color(0xFFEF9A9A),
        tertiary = Color(0xFFFFABAB),
    )

    fun fromType(type: AccentType): AccentPalette = when (type) {
        AccentType.RED -> Red
        AccentType.BLUE -> Blue
        AccentType.PURPLE -> Purple
        AccentType.GREEN -> Green
        AccentType.ORANGE -> Orange
        AccentType.PINK -> Pink
        AccentType.TEAL -> Teal
        AccentType.YELLOW -> Yellow
        AccentType.CYAN -> Cyan
        AccentType.ROSE -> Rose
    }

    val all: List<AccentPalette> = AccentType.entries.map { fromType(it) }
}

// ===== THEME TYPES (5 options) =====
enum class ThemeType(val label: String) {
    LIGHT("Light"),
    NUVIO_DARK("Nuvio Dark"),
    AMOLED_DARK("AMOLED Dark"),
    SEPIA("Sepia"),
    MOCHA("Mocha"),
}

data class ThemePalette(
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val onBackground: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val isDark: Boolean,
)

object Themes {
    val AmoledDark = ThemePalette(
        background = Color.Black,
        surface = Color.Black,
        surfaceVariant = Color(0xFF0A0A0A),
        onBackground = Color.White,
        onSurface = Color.White,
        onSurfaceVariant = Color(0xFFD1D1D1),
        isDark = true,
    )

    val Light = ThemePalette(
        background = Color(0xFFF5F5F5),
        surface = Color.White,
        surfaceVariant = Color(0xFFE8E8E8),
        onBackground = Color(0xFF1C1B1F),
        onSurface = Color(0xFF1C1B1F),
        onSurfaceVariant = Color(0xFF49454F),
        isDark = false,
    )

    val NuvioDark = ThemePalette(
        background = Color(0xFF0D0D0D),
        surface = Color(0xFF161616),
        surfaceVariant = Color(0xFF1E1E1E),
        onBackground = Color.White,
        onSurface = Color.White,
        onSurfaceVariant = Color(0xFFB0B0B0),
        isDark = true,
    )

    val Sepia = ThemePalette(
        background = Color(0xFF1C1612),
        surface = Color(0xFF26201C),
        surfaceVariant = Color(0xFF302A26),
        onBackground = Color(0xFFE8DDD3),
        onSurface = Color(0xFFE8DDD3),
        onSurfaceVariant = Color(0xFFC4B8AC),
        isDark = true,
    )

    val Mocha = ThemePalette(
        background = Color(0xFF131016),
        surface = Color(0xFF1D1A1E),
        surfaceVariant = Color(0xFF272428),
        onBackground = Color(0xFFE6E0E8),
        onSurface = Color(0xFFE6E0E8),
        onSurfaceVariant = Color(0xFFC2BCC8),
        isDark = true,
    )

    fun fromType(type: ThemeType): ThemePalette = when (type) {
        ThemeType.AMOLED_DARK -> AmoledDark
        ThemeType.LIGHT -> Light
        ThemeType.NUVIO_DARK -> NuvioDark
        ThemeType.SEPIA -> Sepia
        ThemeType.MOCHA -> Mocha
    }
}

// Shared colors
object MovieHubColors {
    val Error = Color(0xFFCF6679)
    val Success = Color(0xFF4CAF50)
    val Gray400 = Color(0xFFD1D1D1)
    val Gray600 = Color(0xFF888888)
}
