package com.moviehub.core.ui.theme

import androidx.compose.runtime.Composable

@Composable
actual fun SetStatusBarStyle(isDark: Boolean) {
    // No-op on iOS — status bar appearance is handled via Info.plist
}
