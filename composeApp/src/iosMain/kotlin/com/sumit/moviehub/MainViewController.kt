package com.sumit.moviehub

import androidx.compose.ui.window.ComposeUIViewController
import com.moviehub.core.utils.initKoin
import com.moviehub.di.appModules

fun MainViewController() = ComposeUIViewController(
    configure = {
        initKoin(modules = appModules())
    }
) {
    App()
}
