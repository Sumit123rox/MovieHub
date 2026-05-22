package com.moviehub.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = MovieHubColors.Primary,
    secondary = MovieHubColors.Secondary,
    tertiary = MovieHubColors.Tertiary,
    background = MovieHubColors.Background,
    surface = MovieHubColors.Surface,
    surfaceVariant = MovieHubColors.SurfaceVariant,
    error = MovieHubColors.Error,
    onPrimary = Color.White, // Still White for Primary in Dark Theme
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = MovieHubColors.Gray400
)

@Composable
fun MovieHubTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = MovieHubTypography(),
        content = content
    )
}
