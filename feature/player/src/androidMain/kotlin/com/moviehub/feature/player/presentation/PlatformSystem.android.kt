package com.moviehub.feature.player.presentation

import android.content.Context
import android.media.AudioManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

@Composable
actual fun rememberIsInPipMode(): Boolean {
    val context = LocalContext.current
    val activity = remember(context) {
        var current = context
        while (current is android.content.ContextWrapper) {
            if (current is android.app.Activity) return@remember current
            current = current.baseContext
        }
        null
    }
    if (activity == null) return false

    var isInPip by remember { androidx.compose.runtime.mutableStateOf(activity.isInPictureInPictureMode) }

    androidx.compose.runtime.DisposableEffect(activity) {
        val configListener = androidx.core.util.Consumer<android.content.res.Configuration> { config ->
            isInPip = activity.isInPictureInPictureMode
        }
        val pipListener = androidx.core.util.Consumer<androidx.core.app.PictureInPictureModeChangedInfo> { info ->
            isInPip = info.isInPictureInPictureMode
        }
        val componentActivity = activity as? androidx.activity.ComponentActivity
        componentActivity?.addOnConfigurationChangedListener(configListener)
        componentActivity?.addOnPictureInPictureModeChangedListener(pipListener)
        onDispose {
            componentActivity?.removeOnConfigurationChangedListener(configListener)
            componentActivity?.removeOnPictureInPictureModeChangedListener(pipListener)
        }
    }

    return isInPip
}
