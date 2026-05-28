package com.sumit.moviehub

import android.app.Application

/**
 * Application class for wiring ProfileInstaller (AOT baseline profiles),
 * global error handling, and platform lifecycle hooks.
 *
 * ProfileInstaller is auto-discovered via Jetpack Startup's content-provider
 * manifest merge — no manual initialization needed.
 */
class MovieHubApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Global uncaught exception handler for crash diagnostics
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            // CrashReporter.instrument(throwable) — wired once Monitoring infrastructure is ready
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
