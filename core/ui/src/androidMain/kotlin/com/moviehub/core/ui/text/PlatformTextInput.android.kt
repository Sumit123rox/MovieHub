package com.moviehub.core.ui.text

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.text.input.PlatformImeOptions

@ExperimentalComposeUiApi
actual fun nativeTextFieldImeOptions(enabled: Boolean): PlatformImeOptions {
    // On Android text input is always native (EditText-backed).
    // The enabled parameter is accepted for API consistency.
    return PlatformImeOptions()
}
