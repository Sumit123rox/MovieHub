package com.moviehub.core.utils

import co.touchlab.kermit.Logger

/**
 * iOS AnalyticsTracker — Kermit-based stub.
 * Swap to Firebase Analytics or Amplitude when SDK is added.
 */
actual object AnalyticsTracker {
    private val log = Logger.withTag("Analytics")

    actual fun trackEvent(name: String, params: Map<String, String>) {
        log.d { "event:$name params:${params.entries.joinToString { "${it.key}=${it.value}" }}" }
    }

    actual fun trackScreen(screenName: String) {
        log.d { "screen:$screenName" }
    }

    actual fun trackError(errorType: String, message: String) {
        log.d { "error:$errorType msg:$message" }
    }
}
