package com.moviehub.core.network

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Prevents duplicate in-flight HTTP requests by deduplicating on a string key (typically the URL).
 * When two callers request the same key concurrently, only one network call is made;
 * the second caller awaits and reuses the first result.
 */
class DeduplicatingCache {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutex = Mutex()
    private val inFlight = mutableMapOf<String, Deferred<*>>()

    suspend fun <T> get(key: String, block: suspend () -> T): T {
        mutex.withLock { inFlight[key] }?.let { existing ->
            @Suppress("UNCHECKED_CAST")
            return existing.await() as T
        }

        val deferred = scope.async { block() }
        mutex.withLock { inFlight[key] = deferred }

        return try {
            @Suppress("UNCHECKED_CAST")
            deferred.await() as T
        } finally {
            mutex.withLock { inFlight.remove(key) }
        }
    }
}
