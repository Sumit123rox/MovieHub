package com.moviehub.feature.player.presentation

import android.view.ContextThemeWrapper
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.mediarouter.app.MediaRouteButton
import androidx.mediarouter.media.MediaRouteSelector
import com.google.android.gms.cast.CastMediaControlIntent

@Composable
actual fun CastButton(modifier: Modifier) {
    AndroidView(
        factory = { context ->
            // MediaRouterThemeHelper reads colorBackground for contrast calculation
            // and throws if it's transparent. The Compose theme resolves it as #0,
            // causing IllegalArgumentException. Use applicationContext to bypass
            // the Compose theme, then wrap with a solid-background theme.
            runCatching {
                val ctx = ContextThemeWrapper(
                    context.applicationContext,
                    android.R.style.Theme_DeviceDefault
                )
                MediaRouteButton(ctx).apply {
                    routeSelector = MediaRouteSelector.Builder()
                        .addControlCategory(
                            CastMediaControlIntent.categoryForCast(
                                CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID
                            )
                        )
                        .build()
                }
            }.getOrNull() ?: View(context).also {
                it.visibility = View.GONE
                it.layoutParams = ViewGroup.LayoutParams(1, 1)
            }
        },
        modifier = modifier
    )
}
