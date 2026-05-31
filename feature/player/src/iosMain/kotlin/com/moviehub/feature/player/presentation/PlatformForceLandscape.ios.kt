package com.moviehub.feature.player.presentation

import platform.Foundation.NSNotificationCenter

private const val lockPlayerToLandscapeNotification = "MovieHubPlayerLockLandscape"
private const val unlockPlayerOrientationNotification = "MovieHubPlayerUnlockOrientation"

actual fun forceLandscapeOrientation() {
    NSNotificationCenter.defaultCenter.postNotificationName(
        lockPlayerToLandscapeNotification,
        null,
    )
}

actual fun unlockOrientation() {
    NSNotificationCenter.defaultCenter.postNotificationName(
        unlockPlayerOrientationNotification,
        null,
    )
}
