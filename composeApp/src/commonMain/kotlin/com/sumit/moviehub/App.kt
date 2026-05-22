package com.sumit.moviehub

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.moviehub.core.ui.theme.MovieHubTheme
import com.moviehub.navigation.RootNavGraph
import io.kamel.core.config.KamelConfig
import io.kamel.core.config.httpUrlFetcher
import io.kamel.core.config.takeFrom
import io.kamel.image.config.Default
import io.kamel.image.config.LocalKamelConfig

val movieHubKamelConfig = KamelConfig {
    takeFrom(KamelConfig.Default)
    imageBitmapCacheSize = 500
    imageVectorCacheSize = 100
}

@Composable
fun App() {
    CompositionLocalProvider(LocalKamelConfig provides movieHubKamelConfig) {
        MovieHubTheme {
            RootNavGraph()
        }
    }
}
