package com.moviehub.core.model

import kotlinx.serialization.Serializable

@Serializable
data class PlayerLaunch(
    val stream: StreamItem,
    val streams: List<StreamItem> = emptyList(),
    val title: String? = null,
    val mediaId: String? = null,
    val mediaType: String? = null
)

object PlayerLaunchStore {
    private var nextLaunchId = 1L
    private val launches = mutableMapOf<Long, PlayerLaunch>()

    fun put(launch: PlayerLaunch): Long {
        val launchId = nextLaunchId++
        launches[launchId] = launch
        return launchId
    }

    fun get(launchId: Long): PlayerLaunch? = launches[launchId]

    fun remove(launchId: Long) {
        launches.remove(launchId)
    }

    fun clear() {
        nextLaunchId = 1L
        launches.clear()
    }
}
