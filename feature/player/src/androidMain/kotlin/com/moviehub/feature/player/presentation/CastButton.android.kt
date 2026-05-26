package com.moviehub.feature.player.presentation

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
            MediaRouteButton(context).apply {
                routeSelector = MediaRouteSelector.Builder()
                    .addControlCategory(
                        CastMediaControlIntent.categoryForCast(
                            CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID
                        )
                    )
                    .build()
            }
        },
        modifier = modifier
    )
}
