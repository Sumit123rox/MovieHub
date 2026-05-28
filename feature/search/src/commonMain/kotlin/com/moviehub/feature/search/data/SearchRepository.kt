package com.moviehub.feature.search.data

import com.moviehub.core.database.MediaFtsDao
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
    private val addonManager: AddonManager,
    private val mediaFtsDao: MediaFtsDao,
) : SearchRepository {

    override suspend fun searchMovies(query: String): List<MediaItem> = supervisorScope {
        val ftsDeferred = async {
            try {
                val results = mediaFtsDao.search(query)
                results.map { entity ->
                    MediaItem(
                        id = entity.mediaId,
                        title = entity.title,
                        description = entity.overview,
                        posterUrl = null,
                        type = com.moviehub.core.model.MediaType.MOVIE,
                    )
                }
            } catch (_: Exception) {
                emptyList()
            }
        }

        val addonDeferred = async {
            val addons = addonManager.installedAddons.value
            addons.map { manifest ->
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
                    } catch (_: Exception) {
                        emptyList()
                    }
                }
            }.awaitAll().flatten()
        }

        val ftsResults = ftsDeferred.await()
        val addonResults = addonDeferred.await()
        val ftsIds = ftsResults.map { it.id }.toSet()
        ftsResults + addonResults.filter { it.id !in ftsIds }
    }
}
