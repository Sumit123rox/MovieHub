package com.moviehub.core.utils

/**
 * Lightweight performance tracing abstraction for tracking critical app paths.
 *
 * Usage:
 *   PerformanceMonitor.beginSection("catalog_load")
 *   // ... do work ...
 *   PerformanceMonitor.endSection()
 *
 * Currently logs via platform tracing APIs (android.os.Trace on Android).
 * Future: wire to Firebase Performance Trace or Sentry spans.
 */
expect object PerformanceMonitor {
    /** Begin a named trace section. Sections can be nested. */
    fun beginSection(name: String)

    /** End the most recently started section. */
    fun endSection()

    /**
     * Trace an asynchronous operation without nesting.
     * Useful for tracking network calls, DB queries, etc.
     */
    fun traceAsync(name: String, cookie: Int = 0)

    /** Metric counter — record a value for a named metric. */
    fun counter(name: String, value: Long)
}
