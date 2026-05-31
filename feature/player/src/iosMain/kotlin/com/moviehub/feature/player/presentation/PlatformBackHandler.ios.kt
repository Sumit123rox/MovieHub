package com.moviehub.feature.player.presentation

import androidx.compose.runtime.Composable

@Composable
actual fun PlayerBackHandler(
    enabled: Boolean,
    onBack: () -> Unit,
) {
    // iOS handles its own back gesture via the navigation controller
}
