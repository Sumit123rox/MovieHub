package com.moviehub.feature.player.presentation

import androidx.compose.runtime.Composable

@Composable
expect fun PlayerBackHandler(enabled: Boolean, onBack: () -> Unit)
