package com.moviehub.core.utils

/**
 * Crash reporting abstraction. Register in Koin; swap in Firebase Crashlytics
 * or Sentry actual without changing any feature code.
 */
expect object CrashReporter {
    /** One-time initialization (called from Application.onCreate). */
    fun init()

    /** Log a non-fatal message for breadcrumb trails. */
    fun log(message: String)

    /** Record a non-fatal exception. */
    fun recordException(throwable: Throwable)

    /** Associate current user for session attribution. */
    fun setUser(userId: String?)

    /** Internal logging before CrashReporter is fully initialized. */
    fun internalLog(message: String)
}
