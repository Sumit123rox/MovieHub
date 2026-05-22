package com.moviehub.feature.search.data

import com.moviehub.core.model.MediaItem
import com.moviehub.core.network.StremioApiClient
import com.moviehub.core.network.AddonManager
import com.moviehub.core.network.mapper.toDomain
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope

interface SearchRepository {
    suspend fun searchMovies(query: String): List<MediaItem>
}

class SearchRepositoryImpl(
    private val apiClient: StremioApiClient,
    private val addonManager: AddonManager
) : SearchRepository {

    override suspend fun searchMovies(query: String): List<MediaItem> = supervisorScope {
        val addons = addonManager.installedAddons.value
        val deferredResults = addons.map { manifest ->
            async {
                try {
                    val url = addonManager.getAddonUrl(manifest.id) ?: return@async emptyList<MediaItem>()
                    val response = apiClient.getCatalog(
                        baseUrl = url,
                        type = "movie",
                        id = "top",
                        extra = mapOf("search" to query)
                    )
                    response?.metas?.toDomain(addonId = manifest.id, addonUrl = url) ?: emptyList()
                } catch (e: Exception) {
                    emptyList()
                }
            }
        }
        deferredResults.awaitAll().flatten().distinctBy { it.id }
    }
}
