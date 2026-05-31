package com.moviehub.core.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * iOS SmartStatusBar.
 *
 * Status bar style on iOS is set via Info.plist (UIStatusBarStyle).
 * Dynamic per-screen status bar changes would require modifying the root
 * UIViewController's preferredStatusBarStyle via a Swift helper.
 * For now, the theme-level SetStatusBarStyle handles this as a no-op.
 */
@Composable
actual fun SmartStatusBar(
    isDark: Boolean,
    color: Color,
) {
    // No-op on iOS — dynamically changing status bar per screen requires
    // bridging to Swift's UIViewController preferredStatusBarStyle.
    // The Info.plist UIStatusBarStyle provides the default.
}
