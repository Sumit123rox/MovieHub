package com.moviehub.core.utils

import co.touchlab.kermit.Logger

actual object PerformanceMonitor {
    private val log = Logger.withTag("Perf")

    actual fun beginSection(name: String) {
        log.d { "begin:$name" }
    }

    actual fun endSection() {
        // Log-only; no os_signpost on iOS (requires separate os cinterop)
    }

    actual fun traceAsync(name: String, cookie: Int) {
        log.d { "async:$name#$cookie" }
    }

    actual fun counter(name: String, value: Long) {
        log.d { "cnt:$name=$value" }
    }
}
