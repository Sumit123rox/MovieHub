package com.moviehub.feature.details.data

import co.touchlab.kermit.Logger
import com.moviehub.core.model.MediaItem
import com.moviehub.core.network.StremioApiClient
import com.moviehub.core.network.mapper.toDomain
import com.moviehub.core.model.StreamItem

import com.moviehub.core.network.AddonManager
import com.moviehub.core.network.scraper.ScraperManager
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope

interface DetailsRepository {
    suspend fun getMediaDetails(id: String, type: String, addonUrl: String? = null): MediaItem?
    suspend fun getStreams(id: String, type: String): List<StreamItem>
}

class DetailsRepositoryImpl(
    private val apiClient: StremioApiClient,
    private val addonManager: AddonManager,
    private val scraperManager: ScraperManager,
) : DetailsRepository {

    private val logger = Logger.withTag("DetailsRepository")
    private val CINEMETA_URL = "https://v3-cinemeta.strem.io"

    override suspend fun getMediaDetails(id: String, type: String, addonUrl: String?): MediaItem? {
        val normalizedType = when (type.lowercase()) {
            "show", "tv", "series" -> "series"
            "movie" -> "movie"
            else -> type.lowercase()
        }

        // 1. Try Primary Source (from card click)
        if (addonUrl != null) {
            try {
                val response = apiClient.getMeta(addonUrl, normalizedType, id)
                if (response != null) {
                    return response.meta.toDomain(addonUrl = addonUrl)
                }
            } catch (e: Exception) {
                logger.w { "Primary meta fetch failed for $id at $addonUrl" }
            }
        }

        // 2. Try Installed Fallbacks
        val metaAddons = addonManager.getAddonsProviding("meta", normalizedType)
        for ((url, manifest) in metaAddons) {
            if (url == addonUrl) continue 
            try {
                val response = apiClient.getMeta(url, normalizedType, id)
                if (response != null) {
                    logger.i { "Metadata found via fallback addon: ${manifest.name}" }
                    return response.meta.toDomain(addonId = manifest.id, addonUrl = url)
                }
            } catch (e: Exception) {
                // Continue
            }
        }

        // 3. Global Hardcoded Fallback: Cinemeta (The gold standard)
        try {
            logger.i { "All installed addons failed for $id. Attempting Cinemeta fallback..." }
            val response = apiClient.getMeta(CINEMETA_URL, normalizedType, id)
            if (response != null) {
                logger.i { "Metadata recovered via Cinemeta global fallback" }
                return response.meta.toDomain(addonId = "cinemeta", addonUrl = CINEMETA_URL)
            }
        } catch (e: Exception) {
            logger.e(e) { "Cinemeta fallback also failed for $id" }
        }
        
        return null
    }

    override suspend fun getStreams(id: String, type: String): List<StreamItem> = supervisorScope {
        val normalizedType = when (type.lowercase()) {
            "show", "tv", "series" -> "series"
            "movie" -> "movie"
            else -> type.lowercase()
        }

        // Extract IMDb ID and season/episode from the Stremio-style ID (e.g. "tt12345:1:3")
        val parts = id.split(":")
        val imdbId = parts.getOrNull(0) ?: id
        val season = parts.getOrNull(1)?.toIntOrNull()
        val episode = parts.getOrNull(2)?.toIntOrNull()

        // 1. Launch Stremio HTTP addon stream queries concurrently
        val torrentDeferred = async {
            try {
                val streamAddons = addonManager.getAddonsProviding("stream", normalizedType)
                val deferredStreams = streamAddons.map { (url, manifest) ->
                    async {
                        try {
                            apiClient.getStreams(
                                baseUrl = url,
                                type = normalizedType,
                                id = id,
                                addonName = manifest.name,
                                addonId = manifest.id
                            )
                        } catch (e: Exception) {
                            logger.w { "Stream addon ${manifest.name} failed: ${e.message}" }
                            emptyList()
                        }
                    }
                }
                deferredStreams.awaitAll().flatten()
            } catch (e: Exception) {
                logger.e(e) { "Torrent addon stream fetch failed" }
                emptyList()
            }
        }

        // 2. Launch direct-play scrapers concurrently
        val scraperDeferred = async {
            try {
                scraperManager.getStreams(
                    imdbId = imdbId,
                    type = normalizedType,
                    season = season,
                    episode = episode,
                )
            } catch (e: Exception) {
                logger.e(e) { "Scraper engine failed" }
                emptyList()
            }
        }

        // 3. Merge results: direct-play streams first (instant), then torrent streams
        val scraperStreams = scraperDeferred.await()
        val torrentStreams = torrentDeferred.await()

        logger.i { "Stream merge: ${scraperStreams.size} direct-play + ${torrentStreams.size} torrent = ${scraperStreams.size + torrentStreams.size} total" }

        scraperStreams + torrentStreams
    }
}
