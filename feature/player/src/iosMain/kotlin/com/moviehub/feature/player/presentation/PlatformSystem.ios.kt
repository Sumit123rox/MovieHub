package com.moviehub.feature.player.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.AVFAudio.AVAudioSession
import platform.UIKit.UIScreen
import platform.Foundation.NSDate

@Composable
actual fun rememberSystemVolume(): Float = remember {
    try {
        AVAudioSession.sharedInstance().outputVolume.toFloat()
    } catch (_: Exception) {
        1f
    }
}

@Composable
actual fun rememberSystemBrightness(): Float = remember {
    try {
        UIScreen.mainScreen.brightness.toFloat()
    } catch (_: Exception) {
        1f
    }
}

internal actual fun playerTimeMillis(): Long =
    (NSDate().timeIntervalSince1970 * 1000).toLong()
