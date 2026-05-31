package com.moviehub.feature.player.presentation

/**
 * Platform-specific function to force landscape orientation.
 * On Android, uses Activity.setRequestedOrientation.
 * On iOS, bridges to UIKit's UIDevice orientation via a C helper.
 */
expect fun forceLandscapeOrientation()

/**
 * Platform-specific function to unlock orientation, restoring portrait support.
 * Should be called when leaving the player screen.
 */
expect fun unlockOrientation()
