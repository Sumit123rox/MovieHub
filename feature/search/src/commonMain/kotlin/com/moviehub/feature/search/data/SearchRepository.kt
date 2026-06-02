package com.moviehub.feature.search.data

import com.moviehub.core.database.MediaFtsDao
import com.moviehub.core.database.TmdbSettingsRepository
import com.moviehub.core.model.MediaItem
import com.moviehub.core.network.AddonManager
import com.moviehub.core.network.StremioApiClient
import com.moviehub.core.network.mapper.toDomain
import com.moviehub.core.network.tmdb.TmdbImageUrl
import com.moviehub.core.network.tmdb.TmdbService
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds

interface SearchRepository {
    suspend fun searchMovies(query: String): List<MediaItem>
    suspend fun getSearchSuggestions(query: String): List<String>
}

class SearchRepositoryImpl(
    private val apiClient: StremioApiClient,
    private val addonManager: AddonManager,
    private val mediaFtsDao: MediaFtsDao,
    private val tmdbService: TmdbService,
    private val tmdbSettingsRepository: TmdbSettingsRepository,
) : SearchRepository {

    override suspend fun searchMovies(query: String): List<MediaItem> = supervisorScope {
        val apiKey = tmdbSettingsRepository.getApiKey()
        if (apiKey.isNotBlank()) {
            tmdbService.setApiKey(apiKey)
        }

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

        val tmdbDeferred = async {
            try {
                if (tmdbService.hasApiKey()) {
                    withTimeoutOrNull(4000.milliseconds) {
                        val page1Deferred = async { tmdbService.searchMulti(query, page = 1) }
                        val page2Deferred = async { tmdbService.searchMulti(query, page = 2) }
                        val page1 = page1Deferred.await()
                        val page2 = page2Deferred.await()
                        val combinedResults = (page1?.results.orEmpty() + page2?.results.orEmpty()).distinctBy { it.id }
                        
                        combinedResults.mapNotNull { result ->
                            val mediaType = result.mediaType?.lowercase() ?: return@mapNotNull null
                            if (mediaType != "movie" && mediaType != "tv") return@mapNotNull null

                            MediaItem(
                                id = "${if (mediaType == "movie") "movie" else "series"}:${result.id}",
                                title = result.title ?: result.name ?: "",
                                description = result.overview,
                                posterUrl = TmdbImageUrl.poster(result.posterPath),
                                type = if (mediaType == "movie") {
                                    com.moviehub.core.model.MediaType.MOVIE
                                } else {
                                    com.moviehub.core.model.MediaType.SHOW
                                },
                            )
                        }
                    } ?: emptyList()
                } else {
                    emptyList()
                }
            } catch (_: Exception) {
                emptyList()
            }
        }

        val addonDeferred = async {
            val addons = addonManager.installedAddons.value
            val types = listOf("movie", "series")

            addons.flatMap { manifest ->
                types.map { type ->
                    async {
                        try {
                            val url = addonManager.getAddonUrl(manifest.id) ?: return@async emptyList<MediaItem>()
                            // Strict individual 3-second timeout per provider catalog search to prevent slow/dead addons from hanging
                            withTimeoutOrNull(3000.milliseconds) {
                                val catalog = manifest.catalogs.firstOrNull {
                                    it.type.lowercase() == type.lowercase() && it.extra.any { extra -> extra.name == "search" }
                                } ?: manifest.catalogs.firstOrNull { it.type.lowercase() == type.lowercase() }

                                val actualCatalogId = catalog?.id ?: "top"

                                val response = apiClient.getCatalog(
                                    baseUrl = url,
                                    type = type,
                                    id = actualCatalogId,
                                    extra = mapOf("search" to query),
                                )
                                response?.metas?.toDomain(addonId = manifest.id, addonUrl = url) ?: emptyList()
                            } ?: emptyList()
                        } catch (_: Exception) {
                            emptyList()
                        }
                    }
                }
            }.awaitAll().flatten()
        }

        val ftsResults = ftsDeferred.await()
        val tmdbResults = tmdbDeferred.await()
        val addonResults = addonDeferred.await()

        val mergedResults = mutableListOf<MediaItem>()
        val seenIds = mutableSetOf<String>()

        // 1. Prioritize TMDB Results (high quality, has poster, backdrop, rating, etc.)
        tmdbResults.forEach { item ->
            if (seenIds.add(item.id)) {
                mergedResults.add(item)
            }
        }

        // 2. Add Addon Results (provider catalog metadata)
        addonResults.forEach { item ->
            if (seenIds.add(item.id)) {
                mergedResults.add(item)
            }
        }

        // 3. Add FTS Results (local database fallback)
        ftsResults.forEach { item ->
            if (seenIds.add(item.id)) {
                mergedResults.add(item)
            }
        }

        val queryTokens = query.lowercase()
            .split(Regex("[^a-zA-Z0-9]+"))
            .filter { it.length > 1 && it !in setOf("the", "and", "for", "with", "from", "movie", "show", "series", "season") }

        if (queryTokens.isEmpty()) {
            mergedResults
        } else {
            mergedResults.filter { item ->
                val itemTitleLower = item.title.lowercase()
                itemTitleLower.contains(query.lowercase()) || queryTokens.any { token -> itemTitleLower.contains(token) }
            }
        }
    }

    override suspend fun getSearchSuggestions(query: String): List<String> {
        if (query.isBlank()) return emptyList()

        val apiKey = tmdbSettingsRepository.getApiKey()
        if (apiKey.isNotBlank()) {
            tmdbService.setApiKey(apiKey)
        }

        // Fetch suggestions from TMDB or fallback to public Cinemeta in parallel with local FTS
        return supervisorScope {
            val tmdbDeferred = async {
                try {
                    if (tmdbService.hasApiKey()) {
                        withTimeoutOrNull(2000.milliseconds) {
                            tmdbService.searchMulti(query)?.results
                                ?.mapNotNull { it.title ?: it.name }
                                ?.take(5)
                        } ?: emptyList()
                    } else {
                        emptyList()
                    }
                } catch (_: Exception) {
                    emptyList()
                }
            }

            val cinemetaMoviesDeferred = async {
                try {
                    if (!tmdbService.hasApiKey()) {
                        withTimeoutOrNull(3000.milliseconds) {
                            apiClient.getCatalog(
                                baseUrl = "https://v3-cinemeta.strem.io",
                                type = "movie",
                                id = "top",
                                extra = mapOf("search" to query),
                            )?.metas?.mapNotNull { it.name }?.take(4)
                        } ?: emptyList()
                    } else {
                        emptyList()
                    }
                } catch (_: Exception) {
                    emptyList()
                }
            }

            val cinemetaSeriesDeferred = async {
                try {
                    if (!tmdbService.hasApiKey()) {
                        withTimeoutOrNull(3000.milliseconds) {
                            apiClient.getCatalog(
                                baseUrl = "https://v3-cinemeta.strem.io",
                                type = "series",
                                id = "top",
                                extra = mapOf("search" to query),
                            )?.metas?.mapNotNull { it.name }?.take(4)
                        } ?: emptyList()
                    } else {
                        emptyList()
                    }
                } catch (_: Exception) {
                    emptyList()
                }
            }

            val ftsDeferred = async {
                try {
                    mediaFtsDao.search(query).map { it.title }.take(5)
                } catch (_: Exception) {
                    emptyList()
                }
            }

            val tmdbList = tmdbDeferred.await()
            val cinemetaMoviesList = cinemetaMoviesDeferred.await()
            val cinemetaSeriesList = cinemetaSeriesDeferred.await()
            val ftsList = ftsDeferred.await()

            (tmdbList + cinemetaMoviesList + cinemetaSeriesList + ftsList)
                .distinctBy { it.trim().lowercase() }
                .take(8)
        }
    }
}
