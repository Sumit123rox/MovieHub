package com.moviehub.feature.player.presentation

import android.content.Context
import android.media.AudioManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberSystemVolume(): Float {
    val context = LocalContext.current
    return remember {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        if (max > 0) current.toFloat() / max else 1f
    }
}

@Composable
actual fun rememberSystemBrightness(): Float {
    val context = LocalContext.current
    return remember {
        try {
            val current = android.provider.Settings.System.getInt(
                context.contentResolver,
                android.provider.Settings.System.SCREEN_BRIGHTNESS,
            )
            current.toFloat() / 255f
        } catch (_: Exception) {
            1f
        }
    }
}

internal actual fun playerTimeMillis(): Long = System.currentTimeMillis()

actual val isIosPlatform: Boolean = false
