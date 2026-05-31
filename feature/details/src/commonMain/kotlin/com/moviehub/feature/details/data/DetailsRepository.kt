package com.moviehub.feature.details.data

import co.touchlab.kermit.Logger
import com.moviehub.core.database.CacheService
import com.moviehub.core.model.MediaItem
import com.moviehub.core.model.MediaItemStore
import com.moviehub.core.model.MetaResponse
import com.moviehub.core.model.StreamItem
import com.moviehub.core.network.AddonManager
import com.moviehub.core.network.StremioApiClient
import com.moviehub.core.network.mapper.toDomain
import com.moviehub.core.network.scraper.PluginRepository
import com.moviehub.core.network.scraper.ScraperManager
import com.moviehub.core.network.tmdb.TmdbEnrichmentService
import com.moviehub.core.network.tmdb.TmdbService
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json

interface DetailsRepository {
    suspend fun getMediaDetails(id: String, type: String, addonUrl: String? = null): MediaItem?
    suspend fun getStreams(id: String, type: String): List<StreamItem>
    fun getStreamsFlow(id: String, type: String): Flow<StreamsEvent>
    suspend fun getStreamAddonCount(type: String): Int
}

class DetailsRepositoryImpl(
    private val apiClient: StremioApiClient,
    private val addonManager: AddonManager,
    private val scraperManager: ScraperManager,
    private val tmdbEnrichment: TmdbEnrichmentService,
    private val pluginRepository: PluginRepository,
    private val cacheService: CacheService,
    private val json: Json,
) : DetailsRepository {

    init {
        pluginRepository.initialize()
    }

    private val logger = Logger.withTag("DetailsRepository")
    private val metaSerializer = MetaResponse.serializer()

    override suspend fun getMediaDetails(id: String, type: String, addonUrl: String?): MediaItem? {
        val normalizedType = when (type.lowercase()) {
            "show", "tv", "series" -> "series"
            "movie" -> "movie"
            else -> type.lowercase()
        }

        // 1. Try in-memory store first (fastest)
        MediaItemStore.get(id)?.let { cached ->
            logger.i { "Retrieved MediaItem from in-memory store for $id" }
            val enriched = try {
                tmdbEnrichment.enrich(cached)
            } catch (e: Exception) {
                logger.w { "TMDB enrichment failed for in-memory item: ${e.message}" }
                cached
            }
            MediaItemStore.put(id, enriched)
            return enriched
        }

        // 2. Try persistent cache
        val cacheKey = CacheService.metaKey(id)
        val cachedResponse = cacheService.getCachedParsed(cacheKey, metaSerializer)
        if (cachedResponse != null) {
            logger.i { "Retrieved MediaItem from persistent cache for $id" }
            // Enrich with TMDB before returning from cache
            val mediaItem = cachedResponse.meta.toDomain(addonUrl = addonUrl)
            return try {
                tmdbEnrichment.enrich(mediaItem)
            } catch (e: Exception) {
                logger.w { "TMDB enrichment failed for cached item: ${e.message}" }
                mediaItem
            }
        }

        var mediaItem: MediaItem? = null

        // 3. Try Primary Source (from card click)
        if (addonUrl != null) {
            val primaryAddon = addonManager.getAddonByUrl(addonUrl)
            val supportsMeta = primaryAddon?.resources?.contains("meta") == true
            if (supportsMeta) {
                try {
                    val response = apiClient.getMeta(addonUrl, normalizedType, id)
                    if (response != null) {
                        mediaItem = response.meta.toDomain(addonUrl = addonUrl)
                        cacheService.putCache(cacheKey, "meta", json.encodeToString(metaSerializer, response))
                    }
                } catch (e: Exception) {
                    logger.w { "Primary meta fetch failed for $id at $addonUrl: ${e.message}" }
                }
            } else {
                logger.i { "Skipping primary addon $addonUrl: does not provide 'meta' resource" }
            }
        }

        // 4. Try User-Installed Fallbacks
        if (mediaItem == null) {
            val metaAddons = addonManager.getAddonsProviding("meta", normalizedType)
            for ((url, manifest) in metaAddons) {
                if (url == addonUrl) continue
                try {
                    val response = apiClient.getMeta(url, normalizedType, id)
                    if (response != null) {
                        logger.i { "Metadata found via fallback addon: ${manifest.name}" }
                        mediaItem = response.meta.toDomain(addonId = manifest.id, addonUrl = url)
                        cacheService.putCache(cacheKey, "meta", json.encodeToString(metaSerializer, response))
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
            // Store in in-memory cache so downstream consumers (StreamsScreen) find it
            MediaItemStore.put(id, mediaItem)
        }

        // 5. TMDB Fallback — create MediaItem from scratch when no addon provides metadata
        if (mediaItem == null) {
            // 5a. Try IMDb ID
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

        // 5b. Try TMDB numeric ID (for More Like This, recommendations, collections, etc.)
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

        // 6. In-memory fallback (already checked at top, but double-check)
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

        val cacheKey = CacheService.streamKey(id, type)

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
                                addonId = manifest.id,
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

        val merged = mergeStreams(scraperStreams, torrentStreams)
        logger.i { "Stream merge: ${scraperStreams.size} direct-play + ${torrentStreams.size} torrent = ${merged.size} unique" }

        // Cache the merged result for offline fallback
        val streamSerializer = com.moviehub.core.model.StremioStreamResponse.serializer()
        cacheService.putCache(cacheKey, "stream", json.encodeToString(streamSerializer, com.moviehub.core.model.StremioStreamResponse(streams = merged)))

        merged
    }

    override fun getStreamsFlow(id: String, type: String): Flow<StreamsEvent> = channelFlow {
        val normalizedType = when (type.lowercase()) {
            "show", "tv", "series" -> "series"
            "movie" -> "movie"
            else -> type.lowercase()
        }

        // 0. Warm up AddonManager and PluginRepository so they have loaded data for the active profile
        addonManager.isLoaded.first { it }
        pluginRepository.isInitialized.first { it }

        val parts = id.split(":")
        val imdbId = parts.getOrNull(0) ?: id
        val season = parts.getOrNull(1)?.toIntOrNull()
        val episode = parts.getOrNull(2)?.toIntOrNull()

        val streamSerializer = com.moviehub.core.model.StremioStreamResponse.serializer()
        val cacheKey = CacheService.streamKey(id, type)

        // ── Shared mutable state (guarded by mutex) ──
        val seen = mutableSetOf<String>()
        val accumulated = mutableListOf<StreamItem>()
        val allProviders = linkedMapOf<String, AddonStreamStatus>()
        val mutex = Mutex()

        suspend fun addUnique(streams: List<StreamItem>) {
            streams.forEach { stream ->
                val key = stream.url ?: stream.infoHash ?: "${stream.name}:${stream.description}"
                if (key !in seen) {
                    seen.add(key)
                    accumulated.add(stream)
                }
            }
        }

        // ═══════════════════════════════════════════
        // PHASE 1: Emit cached streams (offline-first)
        // ═══════════════════════════════════════════
        try {
            val cached = cacheService.getCachedParsed(cacheKey, streamSerializer)
            if (cached != null && cached.streams.isNotEmpty()) {
                addUnique(cached.streams)
                send(StreamsEvent.CachedStreams(accumulated.toList()))
            }
        } catch (_: Exception) { }

        // ═══════════════════════════════════════════
        // PHASE 2: Build provider list
        // ═══════════════════════════════════════════
        val streamAddons = addonManager.getAddonsProviding("stream", normalizedType)
        val scrapers = scraperManager.getRegisteredScrapers()

        streamAddons.forEach { (_, manifest) ->
            allProviders[manifest.name] = AddonStreamStatus.Pending
        }
        scrapers.forEach { scraper ->
            val label = "${scraper.name} (Plugin)"
            allProviders[label] = AddonStreamStatus.Pending
        }

        if (allProviders.isEmpty()) {
            send(StreamsEvent.Completed(accumulated.toList(), emptyMap()))
            return@channelFlow
        }

        send(StreamsEvent.LoadingStarted(allProviders.toMap()))

        // ═══════════════════════════════════════════
        // PHASE 3: Parallel fetch — all addons + scrapers at once with semaphore throttling
        // ═══════════════════════════════════════════
        val perAddonTimeout = 15_000L
        val semaphore = Semaphore(permits = 10)

        supervisorScope {
            // ── Launch all HTTP addon requests concurrently ──
            val addonJobs = streamAddons.map { (url, manifest) ->
                async {
                    val providerName = manifest.name

                    // Mark Fetching
                    val initialStatusMap = mutex.withLock {
                        allProviders[providerName] = AddonStreamStatus.Fetching
                        allProviders.toMap()
                    }
                    send(
                        StreamsEvent.ProviderStatusChanged(
                            providerName, AddonStreamStatus.Fetching, initialStatusMap,
                        ),
                    )

                    try {
                        val streams = semaphore.withPermit {
                            withTimeout(perAddonTimeout) {
                                apiClient.getStreams(
                                    baseUrl = url,
                                    type = normalizedType,
                                    id = id,
                                    addonName = manifest.name,
                                    addonId = manifest.id,
                                )
                            }
                        }
                        mutex.withLock {
                            addUnique(streams)
                            allProviders[providerName] = AddonStreamStatus.Completed(streams.size)
                        }
                    } catch (_: TimeoutCancellationException) {
                        logger.w { "Stream addon $providerName timed out after ${perAddonTimeout}ms" }
                        mutex.withLock {
                            allProviders[providerName] = AddonStreamStatus.TimedOut(perAddonTimeout)
                        }
                    } catch (e: Exception) {
                        logger.w { "Stream addon $providerName failed: ${e.message}" }
                        mutex.withLock {
                            allProviders[providerName] = AddonStreamStatus.Failed(e.message)
                        }
                    }

                    // Emit status update + current streams (without holding mutex to avoid channel flow deadlocks)
                    val (finalStatus, finalMap, finalStreams) = mutex.withLock {
                        Triple(allProviders[providerName]!!, allProviders.toMap(), accumulated.toList())
                    }
                    send(StreamsEvent.ProviderStatusChanged(providerName, finalStatus, finalMap))
                    send(StreamsEvent.StreamsUpdated(finalStreams))
                }
            }

            // ── Launch all scraper requests concurrently ──
            val scraperJobs = scrapers.map { scraper ->
                async {
                    val providerName = "${scraper.name} (Plugin)"

                    // Mark Fetching
                    val initialStatusMap = mutex.withLock {
                        allProviders[providerName] = AddonStreamStatus.Fetching
                        allProviders.toMap()
                    }
                    send(
                        StreamsEvent.ProviderStatusChanged(
                            providerName, AddonStreamStatus.Fetching, initialStatusMap,
                        ),
                    )

                    try {
                        val scraperStreams = semaphore.withPermit {
                            withTimeout(perAddonTimeout) {
                                scraperManager.getStreamsFromScraper(
                                    scraperId = scraper.id,
                                    imdbId = imdbId,
                                    type = normalizedType,
                                    season = season,
                                    episode = episode,
                                )
                            }
                        }
                        mutex.withLock {
                            addUnique(scraperStreams)
                            allProviders[providerName] = AddonStreamStatus.Completed(scraperStreams.size)
                        }
                    } catch (_: TimeoutCancellationException) {
                        logger.w { "Scraper $providerName timed out after ${perAddonTimeout}ms" }
                        mutex.withLock {
                            allProviders[providerName] = AddonStreamStatus.TimedOut(perAddonTimeout)
                        }
                    } catch (e: Exception) {
                        logger.w { "Scraper $providerName failed: ${e.message}" }
                        mutex.withLock {
                            allProviders[providerName] = AddonStreamStatus.Failed(e.message)
                        }
                    }

                    // Emit status update + current streams (without holding mutex to avoid channel flow deadlocks)
                    val (finalStatus, finalMap, finalStreams) = mutex.withLock {
                        Triple(allProviders[providerName]!!, allProviders.toMap(), accumulated.toList())
                    }
                    send(StreamsEvent.ProviderStatusChanged(providerName, finalStatus, finalMap))
                    send(StreamsEvent.StreamsUpdated(finalStreams))
                }
            }

            // Wait for ALL providers to finish (or timeout/fail)
            (addonJobs + scraperJobs).awaitAll()
        }

        logger.i { "StreamsFlow resolved ${accumulated.size} unique streams (${allProviders.size} providers)" }

        // Cache the final merged result
        cacheService.putCache(
            cacheKey, "stream",
            json.encodeToString(streamSerializer, com.moviehub.core.model.StremioStreamResponse(streams = accumulated)),
        )

        send(StreamsEvent.Completed(accumulated.toList(), allProviders.toMap()))
    }

    private fun mergeStreams(directPlay: List<StreamItem>, torrent: List<StreamItem>): List<StreamItem> {
        val seen = mutableSetOf<String>()
        val result = mutableListOf<StreamItem>()

        // Direct-play first, then torrent
        (directPlay + torrent).forEach { stream ->
            val key = stream.url
                ?: stream.infoHash
                ?: "${stream.name}:${stream.description}"
            if (key !in seen) {
                seen.add(key)
                result.add(stream)
            }
        }
        return result
    }

    override suspend fun getStreamAddonCount(type: String): Int {
        val normalizedType = when (type.lowercase()) {
            "show", "tv", "series" -> "series"
            "movie" -> "movie"
            else -> type.lowercase()
        }
        // Wait until AddonManager and PluginRepository have finished loading active profile data
        addonManager.isLoaded.first { it }
        pluginRepository.isInitialized.first { it }
        val addonCount = addonManager.getAddonsProviding("stream", normalizedType).size
        val scraperCount = scraperManager.getRegisteredScrapers().size
        return addonCount + scraperCount
    }
}
