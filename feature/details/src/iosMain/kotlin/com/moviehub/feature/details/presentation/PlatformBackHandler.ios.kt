package com.moviehub.feature.details.presentation

import androidx.compose.runtime.Composable

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // iOS doesn't have a system back button — navigation back is handled by swipe gestures in the
    // navigation framework. The trailer dismiss on back is handled by the detail screen's own UI.
}
