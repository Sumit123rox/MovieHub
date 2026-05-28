package com.moviehub.core.utils

import android.os.Build
import android.os.Trace

actual object PerformanceMonitor {
    actual fun beginSection(name: String) {
        Trace.beginSection(name)
    }

    actual fun endSection() {
        Trace.endSection()
    }

    actual fun traceAsync(name: String, cookie: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Trace.beginAsyncSection(name, cookie)
            Trace.endAsyncSection(name, cookie)
        }
    }

    actual fun counter(name: String, value: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Trace.setCounter(name, value)
        }
    }
}
