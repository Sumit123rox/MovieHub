package com.moviehub.core.utils

import co.touchlab.kermit.Logger

/**
 * Android CrashReporter — Kermit-based stub.
 * Swap to Firebase Crashlytics or Sentry when SDK is added.
 */
actual object CrashReporter {
    private val log = Logger.withTag("CrashReporter")

    actual fun init() {
        log.d { "CrashReporter initialized" }
    }

    actual fun log(message: String) {
        log.d { message }
    }

    actual fun recordException(throwable: Throwable) {
        log.e(throwable) { throwable.message ?: "No message" }
    }

    actual fun setUser(userId: String?) {
        log.d { "setUser: $userId" }
    }

    actual fun internalLog(message: String) {
        log.d { message }
    }
}
