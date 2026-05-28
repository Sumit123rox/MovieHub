package com.moviehub.core.ui.text

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.text.input.PlatformImeOptions

@ExperimentalComposeUiApi
actual fun nativeTextFieldImeOptions(enabled: Boolean): PlatformImeOptions {
    return PlatformImeOptions {
        usingNativeTextInput(enabled)
    }
}
