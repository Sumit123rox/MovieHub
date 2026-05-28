package com.moviehub.core.utils

/**
 * Analytics event tracking abstraction. Register in Koin; swap in Firebase
 * Analytics or Amplitude without changing feature code.
 */
expect object AnalyticsTracker {
    /** Track a named event with optional key-value parameters. */
    fun trackEvent(name: String, params: Map<String, String> = emptyMap())

    /** Track a screen view. */
    fun trackScreen(screenName: String)

    /** Track an error event with type and message. */
    fun trackError(errorType: String, message: String)
}
