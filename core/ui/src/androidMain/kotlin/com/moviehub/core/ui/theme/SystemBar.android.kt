package com.moviehub.core.ui.theme

import android.app.Activity
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat

@Composable
actual fun SetStatusBarStyle(isDark: Boolean) {
    val context = LocalContext.current
    SideEffect {
        try {
            var ctx = context
            var act: Activity? = null
            while (ctx is ContextWrapper) {
                if (ctx is Activity) { act = ctx; break }
                ctx = ctx.baseContext
            }
            act?.let {
                WindowCompat.getInsetsController(it.window, it.window.decorView).apply {
                    isAppearanceLightStatusBars = !isDark
                    isAppearanceLightNavigationBars = !isDark
                }
            }
        } catch (_: Exception) { }
    }
}
