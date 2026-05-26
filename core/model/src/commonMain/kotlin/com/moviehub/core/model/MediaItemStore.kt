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
    @OptIn(InternalCoroutinesApi::class)
    private val lock = SynchronizedObject()
    private val items = mutableMapOf<String, MediaItem>()

    fun put(id: String, item: MediaItem) = synchronized(lock) { items[id] = item }

    fun get(id: String): MediaItem? = synchronized(lock) { items[id] }

    fun remove(id: String) = synchronized(lock) { items.remove(id) }

    fun clear() = synchronized(lock) { items.clear() }
}
