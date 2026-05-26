package com.moviehub.feature.details.data

import co.touchlab.kermit.Logger
import com.moviehub.core.model.MediaItem
import com.moviehub.core.model.MediaItemStore
import com.moviehub.core.network.StremioApiClient
import com.moviehub.core.network.mapper.toDomain
import com.moviehub.core.model.StreamItem

import com.moviehub.core.network.AddonManager
import com.moviehub.core.network.scraper.ScraperManager
import com.moviehub.core.network.tmdb.TmdbEnrichmentService
import com.moviehub.core.network.tmdb.TmdbService
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope

interface DetailsRepository {
    suspend fun getMediaDetails(id: String, type: String, addonUrl: String? = null): MediaItem?
    suspend fun getStreams(id: String, type: String): List<StreamItem>
    suspend fun getStreamAddonCount(type: String): Int
}

class DetailsRepositoryImpl(
    private val apiClient: StremioApiClient,
    private val addonManager: AddonManager,
    private val scraperManager: ScraperManager,
    private val tmdbEnrichment: TmdbEnrichmentService,
) : DetailsRepository {

    private val logger = Logger.withTag("DetailsRepository")

    override suspend fun getMediaDetails(id: String, type: String, addonUrl: String?): MediaItem? {
        val normalizedType = when (type.lowercase()) {
            "show", "tv", "series" -> "series"
            "movie" -> "movie"
            else -> type.lowercase()
        }

        var mediaItem: MediaItem? = null

        // 1. Try Primary Source (from card click) — only if the addon supports "meta"
        if (addonUrl != null) {
            val primaryAddon = addonManager.getAddonByUrl(addonUrl)
            val supportsMeta = primaryAddon?.resources?.contains("meta") == true
            if (supportsMeta) {
                try {
                    val response = apiClient.getMeta(addonUrl, normalizedType, id)
                    if (response != null) {
                        mediaItem = response.meta.toDomain(addonUrl = addonUrl)
                    }
                } catch (e: Exception) {
                    logger.w { "Primary meta fetch failed for $id at $addonUrl: ${e.message}" }
                }
            } else {
                logger.i { "Skipping primary addon $addonUrl: does not provide 'meta' resource" }
            }
        }

        // 2. Try User-Installed Fallbacks
        if (mediaItem == null) {
            val metaAddons = addonManager.getAddonsProviding("meta", normalizedType)
            for ((url, manifest) in metaAddons) {
                if (url == addonUrl) continue
                try {
                    val response = apiClient.getMeta(url, normalizedType, id)
                    if (response != null) {
                        logger.i { "Metadata found via fallback addon: ${manifest.name}" }
                        mediaItem = response.meta.toDomain(addonId = manifest.id, addonUrl = url)
                        break
                    }
                } catch (e: Exception) {
                    // Continue
                }
            }
        }

        // Enrich with TMDB data (cast photos, ratings, etc.)
        if (mediaItem != null) {
            try {
                mediaItem = tmdbEnrichment.enrich(mediaItem)
            } catch (e: Exception) {
                logger.w { "TMDB enrichment failed: ${e.message}" }
            }
        }

        // 3. TMDB Fallback — create MediaItem from scratch when no addon provides metadata
        if (mediaItem == null) {
            // 3a. Try IMDb ID
            val imdbId = id.split(":", "/", "?", "&").firstOrNull { it.startsWith("tt") && it.length > 2 }
            if (imdbId != null) {
                try {
                    val tmdbItem = tmdbEnrichment.fetchAsMediaItem(imdbId, normalizedType)
                    if (tmdbItem != null) {
                        logger.i { "Created MediaItem from TMDB fallback (IMDb) for $imdbId" }
                        mediaItem = tmdbItem
                    }
                } catch (e: Exception) {
                    logger.w { "TMDB fallback (IMDb) failed for $id: ${e.message}" }
                }
            }
        }

        // 3b. Try TMDB numeric ID (for More Like This, recommendations, collections, etc.)
        if (mediaItem == null) {
            val tmdbId = TmdbService.extractTmdbId(id)
            if (tmdbId != null) {
                try {
                    val tmdbItem = tmdbEnrichment.fetchAsMediaItemFromTmdbId(tmdbId, normalizedType)
                    if (tmdbItem != null) {
                        logger.i { "Created MediaItem from TMDB fallback (TMDB ID) for $tmdbId" }
                        mediaItem = tmdbItem
                    }
                } catch (e: Exception) {
                    logger.w { "TMDB fallback (TMDB ID) failed for $id: ${e.message}" }
                }
            }
        }

        // 4. In-memory fallback — item cached from catalog/HomeScreen before navigation
        if (mediaItem == null) {
            mediaItem = MediaItemStore.get(id)
            if (mediaItem != null) {
                logger.i { "Retrieved MediaItem from in-memory store for $id" }
            }
        }

        return mediaItem
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

    override suspend fun getStreamAddonCount(type: String): Int {
        val normalizedType = when (type.lowercase()) {
            "show", "tv", "series" -> "series"
            "movie" -> "movie"
            else -> type.lowercase()
        }
        return addonManager.getAddonsProviding("stream", normalizedType).size + 1 // +1 for scrapers
    }
}
