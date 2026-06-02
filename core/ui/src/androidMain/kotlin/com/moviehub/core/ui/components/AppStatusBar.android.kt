package com.moviehub.core.ui.components

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.graphics.luminance

@Composable
actual fun SmartStatusBar(
    isDark: Boolean,
    color: Color,
) {
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, isDark, color) {
        fun updateStatusBar() {
            val window = (view.context as? Activity)?.window ?: return
            val insetsController = WindowCompat.getInsetsController(window, view)
            
            val isAppearanceLight = if (color != Color.Unspecified && color != Color.Transparent) {
                color.luminance() > 0.5f
            } else {
                !isDark
            }
            insetsController.isAppearanceLightStatusBars = isAppearanceLight
            
            if (color != Color.Unspecified) {
                window.statusBarColor = color.toArgb()
            }
        }

        val attachListener = object : android.view.View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: android.view.View) {
                updateStatusBar()
            }
            override fun onViewDetachedFromWindow(v: android.view.View) {}
        }
        view.addOnAttachStateChangeListener(attachListener)

        if (view.isAttachedToWindow) {
            updateStatusBar()
        }

        // Monitor lifecycle state changes to apply when returning from the backstack
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                view.post {
                    updateStatusBar()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            view.removeOnAttachStateChangeListener(attachListener)
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}
