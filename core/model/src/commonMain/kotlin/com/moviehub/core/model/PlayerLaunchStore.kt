package com.moviehub.core.model

import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.internal.SynchronizedObject
import kotlinx.coroutines.internal.synchronized
import kotlinx.serialization.Serializable

@Serializable
data class PlayerLaunch(
    val stream: StreamItem,
    val streams: List<StreamItem> = emptyList(),
    val title: String? = null,
    val mediaId: String? = null,
    val mediaType: String? = null,
    val posterUrl: String? = null
)

@OptIn(InternalCoroutinesApi::class)
object PlayerLaunchStore {
    private val lock = SynchronizedObject()
    private var nextLaunchId = 1L
    private val launches = mutableMapOf<Long, PlayerLaunch>()

    fun put(launch: PlayerLaunch): Long = synchronized(lock) {
        val launchId = nextLaunchId++
        launches[launchId] = launch
        launchId
    }

    fun get(launchId: Long): PlayerLaunch? = synchronized(lock) { launches[launchId] }

    fun remove(launchId: Long) = synchronized(lock) { launches.remove(launchId) }

    fun clear() = synchronized(lock) {
        nextLaunchId = 1L
        launches.clear()
    }
}
