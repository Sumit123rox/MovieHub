package com.moviehub.core.ui.text

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.text.input.PlatformImeOptions

/**
 * Creates platform-specific [PlatformImeOptions] that enables native text input behavior.
 *
 * On iOS this activates native UIView-backed text input (precise caret, selection handles,
 * Autofill, Translate context menu). On Android native input is always active — the
 * parameter is accepted for API consistency but has no effect.
 */
@ExperimentalComposeUiApi
expect fun nativeTextFieldImeOptions(enabled: Boolean = true): PlatformImeOptions
