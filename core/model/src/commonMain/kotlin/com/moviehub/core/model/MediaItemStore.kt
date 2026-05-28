package com.moviehub.core.model

import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.internal.SynchronizedObject
import kotlinx.coroutines.internal.synchronized

/**
 * In-memory store for passing MediaItems between screens via navigation.
 * Screens store the item before navigating to Details; DetailsRepository
 * retrieves it as a fallback when addon meta fetch fails.
 */
@OptIn(InternalCoroutinesApi::class)
object MediaItemStore {
    private const val MAX_ENTRIES = 100
    @OptIn(InternalCoroutinesApi::class)
    private val lock = SynchronizedObject()
    private val items = mutableMapOf<String, MediaItem>()

    fun put(id: String, item: MediaItem) = synchronized(lock) {
        items[id] = item
        if (items.size > MAX_ENTRIES) {
            val oldestKey = items.keys.firstOrNull()
            if (oldestKey != null) items.remove(oldestKey)
        }
    }

    fun get(id: String): MediaItem? = synchronized(lock) { items[id] }

    fun remove(id: String) = synchronized(lock) { items.remove(id) }

    fun clear() = synchronized(lock) { items.clear() }
}
