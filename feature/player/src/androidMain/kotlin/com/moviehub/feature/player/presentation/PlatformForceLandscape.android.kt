package com.moviehub.feature.player.presentation

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo

internal fun findActivity(context: Context?): Activity? {
    var current = context
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return current as? Activity
}

/** Module-level storage so orientation survives source switches */
private var originalOrientation: Int = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
private var lastActivity: Activity? = null
private var isLandscapeLocked = false

actual fun forceLandscapeOrientation() {
    isLandscapeLocked = true
}

actual fun unlockOrientation() {
    isLandscapeLocked = false
    val activity = lastActivity ?: return
    // Must run on UI thread — orientation changes are UI operations
    activity.runOnUiThread {
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER
    }
    originalOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
}

internal fun applyLandscapeLock(context: Context) {
    val activity = findActivity(context) ?: return
    lastActivity = activity
    activity.runOnUiThread {
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    }
}

internal fun applyOrientationRestore(context: Context) {
    val activity = findActivity(context) ?: return
    if (originalOrientation != ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
        activity.requestedOrientation = originalOrientation
        originalOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }
}
