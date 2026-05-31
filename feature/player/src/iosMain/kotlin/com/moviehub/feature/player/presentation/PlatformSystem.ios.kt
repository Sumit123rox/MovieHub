package com.moviehub.feature.player.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.AVFAudio.AVAudioSession
import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970
import platform.Foundation.valueForKey
import platform.UIKit.UIScreen

@Composable
actual fun rememberSystemVolume(): Float = remember {
    @Suppress("UNCHECKED_CAST")
    try {
        // outputVolume is a Float property on AVAudioSession in iOS
        val session = AVAudioSession.sharedInstance()
        val vol = session.valueForKey("outputVolume") as? Float
        vol ?: 1f
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

actual val isIosPlatform: Boolean = true

@Composable
actual fun rememberIsInPipMode(): Boolean = false
