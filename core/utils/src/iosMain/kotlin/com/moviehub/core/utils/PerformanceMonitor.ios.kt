package com.moviehub.core.utils

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.Darwin.os_log_create
import platform.Darwin.os_signpost_interval_begin
import platform.Darwin.os_signpost_interval_end
import platform.Darwin.os_signpost_event_emit
import platform.Darwin.OS_LOG_DEFAULT
import platform.Darwin.OS_SIGNPOST_ID_EXCLUSIVE
import platform.Darwin.os_signpost_id_t
import platform.Darwin.os_signpost_id_make_with_pointer
import co.touchlab.kermit.Logger
import platform.darwin.__dso_handle

/**
 * iOS PerformanceMonitor using os_signpost for Instruments profiling.
 *
 * Trace sections appear in Instruments under the "MovieHub" subsystem
 * with the "Performance" category. Use Instruments → os_signpost instrument
 * to visualize trace intervals.
 */
@OptIn(ExperimentalForeignApi::class)
actual object PerformanceMonitor {
    private val subsystem = "com.moviehub.ios"
    private val category = "Performance"
    private val perfLog = os_log_create(subsystem, category)
    private val log = Logger.withTag("Perf")
    private var nextSignpostId: ULong = 1u

    /** Generate unique signpost IDs. In a real implementation we'd use pointers. */
    private fun nextId(): os_signpost_id_t {
        nextSignpostId++
        return nextSignpostId
    }

    actual fun beginSection(name: String) {
        log.d { "begin:$name" }
        val sid = nextId()
        os_signpost_interval_begin(perfLog, sid, "MovieHub", "%{public}s", name)
    }

    actual fun endSection() {
        log.d { "end" }
        // os_signpost_interval_end requires the same signpost ID as beginSection.
        // Since we use unique IDs, each begin/end pair is independent.
        val sid = nextSignpostId
        os_signpost_interval_end(perfLog, sid, "MovieHub")
    }

    actual fun traceAsync(name: String, cookie: Int) {
        log.d { "async:$name#$cookie" }
        val sid = nextId()
        os_signpost_event_emit(perfLog, sid, "MovieHub", "%{public}s (cookie=%d)", name, cookie.toLong())
    }

    actual fun counter(name: String, value: Long) {
        log.d { "cnt:$name=$value" }
        val sid = nextId()
        os_signpost_event_emit(perfLog, sid, "MovieHub", "%{public}s=%lld", name, value)
    }
}
