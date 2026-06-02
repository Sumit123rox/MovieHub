package com.moviehub.feature.profile.presentation

import androidx.compose.runtime.Composable

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // iOS system back gesture is handled natively by the navigation shell
}
