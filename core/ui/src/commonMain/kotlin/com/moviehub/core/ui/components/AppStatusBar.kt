package com.moviehub.core.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Smart status bar that adapts to screen context.
 *
 * - [isDark] = true → light status bar icons (white), for use on dark backgrounds
 * - [isDark] = false → dark status bar icons (black/grey), for use on light backgrounds
 * - [color] = optional tint color overlay for the status bar area (e.g. gradient start)
 *
 * Call this at the top of any screen composable. The effect is platform-specific:
 *  - Android: sets status bar icon colors via WindowInsetsController
 *  - iOS: sets UIStatusBarStyle (light/dark content)
 */
@Composable
expect fun SmartStatusBar(
    isDark: Boolean,
    color: Color = Color.Unspecified,
)
