package com.moviehub.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
fun MovieHubTheme(
    themeType: ThemeType = ThemeType.NUVIO_DARK,
    accentType: AccentType = AccentType.BLUE,
    content: @Composable () -> Unit,
) {
    val accent = remember(accentType) { Accents.fromType(accentType) }
    val theme = remember(themeType) { Themes.fromType(themeType) }

    val colorScheme = remember(themeType, accentType) {
        if (theme.isDark) {
            darkColorScheme(
                primary = accent.primary,
                onPrimary = accent.onPrimary,
                primaryContainer = accent.primaryDark,
                secondary = accent.secondary,
                tertiary = accent.tertiary,
                background = theme.background,
                surface = theme.surface,
                surfaceVariant = theme.surfaceVariant,
                error = MovieHubColors.Error,
                onBackground = theme.onBackground,
                onSurface = theme.onSurface,
                onSurfaceVariant = theme.onSurfaceVariant,
            )
        } else {
            lightColorScheme(
                primary = accent.primary,
                onPrimary = accent.onPrimary,
                primaryContainer = accent.primaryLight,
                secondary = accent.secondary,
                tertiary = accent.tertiary,
                background = theme.background,
                surface = theme.surface,
                surfaceVariant = theme.surfaceVariant,
                error = MovieHubColors.Error,
                onBackground = theme.onBackground,
                onSurface = theme.onSurface,
                onSurfaceVariant = theme.onSurfaceVariant,
            )
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MovieHubTypography(),
        content = {
            SetStatusBarStyle(theme.isDark)
            content()
        },
    )
}
