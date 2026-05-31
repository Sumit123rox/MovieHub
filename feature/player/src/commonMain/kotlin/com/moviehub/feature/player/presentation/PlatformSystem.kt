package com.moviehub.feature.player.presentation

import androidx.compose.runtime.Composable

/**
 * Returns the current system media volume as a 0..1 float, defaulting to 1f.
 */
@Composable
expect fun rememberSystemVolume(): Float

/**
 * Returns the current system screen brightness as a 0..1 float, defaulting to 1f.
 */
@Composable
expect fun rememberSystemBrightness(): Float

/** Platform-specific current time in milliseconds since Unix epoch. */
internal expect fun playerTimeMillis(): Long

expect val isIosPlatform: Boolean

/** Returns true if the app is currently displaying in Picture-in-Picture mode. */
@Composable
expect fun rememberIsInPipMode(): Boolean
