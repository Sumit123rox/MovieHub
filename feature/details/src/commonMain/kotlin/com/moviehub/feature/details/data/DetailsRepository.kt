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

    /**
     * Strips a single known Stremio/TMDB prefix from the ID.
     * Uses explicit matching rather than chained removePrefix to avoid
     * silently corrupting IDs like "series:movie:123".
     */
    private fun stripKnownPrefix(id: String): String {
        val knownPrefixes = listOf("tmdb:", "series:", "movie:", "tv:")
        for (prefix in knownPrefixes) {
            if (id.startsWith(prefix)) {
                return id.removePrefix(prefix)
            }
        }
        return id
    }

    override suspend fun getMediaDetails(id: String, type: String, addonUrl: String?): MediaItem? {
        val normalizedType = when (type.lowercase()) {
            "show", "tv", "series" -> "series"
            "movie" -> "movie"
            else -> type.lowercase()
        }

        val enrichedCacheKey = "enriched:meta:$id"

        // 0. Try persistent enriched cache (blazing fast offline-first TMDB-enriched metadata)
        try {
            val cachedEnriched = cacheService.getCachedParsed(enrichedCacheKey, MediaItem.serializer(), CacheService.META_TTL_MS)
            if (cachedEnriched != null) {
                logger.i { "Retrieved fully enriched MediaItem from persistent cache for $id" }
                MediaItemStore.put(id, cachedEnriched)
                return cachedEnriched
            }
        } catch (_: Exception) { logger.d { "Cache operation skipped (non-critical)" } }

        // 1. Try in-memory store first (fastest, with robust variation lookup)
        val cleanId = stripKnownPrefix(id)
        val storeCached = MediaItemStore.get(id)
            ?: MediaItemStore.get(cleanId)
            ?: MediaItemStore.get("series:$cleanId")
            ?: MediaItemStore.get("movie:$cleanId")

        storeCached?.let { cached ->
            if (cached.voteCount != null && cached.moreLikeThis.isNotEmpty()) {
                logger.i { "Retrieved fully enriched MediaItem from in-memory store for $id" }
                return cached
            }
            logger.i { "Retrieved basic MediaItem from in-memory store for $id. Running enrichment..." }
            val enriched = try {
                tmdbEnrichment.enrich(cached)
            } catch (e: Exception) {
                logger.w { "TMDB enrichment failed for in-memory item: ${e.message}" }
                cached
            }
            MediaItemStore.put(id, enriched)
            try {
                cacheService.putCacheSerialized(enrichedCacheKey, "enriched_meta", enriched, MediaItem.serializer())
            } catch (_: Exception) { logger.d { "Cache operation skipped (non-critical)" } }
            return enriched
        }

        // 2. Try persistent cache
        val cacheKey = CacheService.metaKey(id)
        val cachedResponse = cacheService.getCachedParsed(cacheKey, metaSerializer)
        if (cachedResponse != null) {
            logger.i { "Retrieved MediaItem from persistent cache for $id" }
            // Enrich with TMDB before returning from cache
            val mediaItem = cachedResponse.meta.toDomain(addonUrl = addonUrl)
            val enriched = try {
                tmdbEnrichment.enrich(mediaItem)
            } catch (e: Exception) {
                logger.w { "TMDB enrichment failed for cached item: ${e.message}" }
                mediaItem
            }
            MediaItemStore.put(id, enriched)
            try {
                cacheService.putCacheSerialized(enrichedCacheKey, "enriched_meta", enriched, MediaItem.serializer())
            } catch (_: Exception) { logger.d { "Cache operation skipped (non-critical)" } }
            return enriched
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

        // 4. Try User-Installed Fallbacks (Concurrently with 4s timeout to avoid sequential delays or loading screens forever)
        if (mediaItem == null) {
            val metaAddons = addonManager.getAddonsProviding("meta", normalizedType)
            if (metaAddons.isNotEmpty()) {
                supervisorScope {
                    val deferreds = metaAddons.map { (url, manifest) ->
                        if (url != addonUrl) {
                            async {
                                try {
                                    withTimeout(4000L) {
                                        val response = apiClient.getMeta(url, normalizedType, id)
                                        if (response != null) {
                                            Pair(response.meta.toDomain(addonId = manifest.id, addonUrl = url), response)
                                        } else {
                                            null
                                        }
                                    }
                                } catch (_: Exception) {
                                    null
                                }
                            }
                        } else {
                            null
                        }
                    }.filterNotNull()

                    val results = deferreds.awaitAll().filterNotNull()
                    val firstResult = results.firstOrNull()
                    if (firstResult != null) {
                        mediaItem = firstResult.first
                        val response = firstResult.second
                        try {
                            logger.i { "Metadata found via parallel fallback addon" }
                            cacheService.putCache(cacheKey, "meta", json.encodeToString(metaSerializer, response))
                        } catch (_: Exception) { logger.d { "Cache operation skipped (non-critical)" } }
                    }
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

        // Save the fully resolved/enriched MediaItem to the persistent cache
        if (mediaItem != null) {
            try {
                cacheService.putCacheSerialized(enrichedCacheKey, "enriched_meta", mediaItem, MediaItem.serializer())
            } catch (_: Exception) { logger.d { "Cache operation skipped (non-critical)" } }
            MediaItemStore.put(id, mediaItem)
        }

        return mediaItem
    }

    override suspend fun getStreams(id: String, type: String): List<StreamItem> = supervisorScope {
        val normalizedType = when (type.lowercase()) {
            "show", "tv", "series" -> "series"
            "movie" -> "movie"
            else -> type.lowercase()
        }

        // Resolve TMDB ID to IMDb ID if necessary for Stremio addon compatibility
        var queryId = id
        val isTmdb = id.startsWith("tmdb:") || id.startsWith("movie:") || id.startsWith("series:") || id.toIntOrNull() != null
        if (isTmdb) {
            val parts = id.split(":")
            val hasPrefix = parts[0] == "tmdb" || parts[0] == "series" || parts[0] == "tv" || parts[0] == "movie"
            val baseId = if (hasPrefix) "${parts[0]}:${parts[1]}" else parts[0]
            val rawNumericId = baseId.removePrefix("tmdb:").removePrefix("series:").removePrefix("movie:").removePrefix("tv:")
            
            // Try different prefix variations to query MediaItemStore and CacheService
            val cachedMedia = MediaItemStore.get(baseId)
                ?: MediaItemStore.get("series:$rawNumericId")
                ?: MediaItemStore.get("movie:$rawNumericId")
                ?: MediaItemStore.get(rawNumericId)
                ?: run {
                    val variations = listOf(baseId, "series:$rawNumericId", "movie:$rawNumericId", rawNumericId)
                    var found: MediaItem? = null
                    for (variant in variations) {
                        val cacheKey = CacheService.metaKey(variant)
                        try {
                            found = cacheService.getCachedParsed(cacheKey, metaSerializer)?.meta?.toDomain()
                            if (found != null) break
                        } catch (_: Exception) { }
                    }
                    found
                }

            val resolvedImdbId = cachedMedia?.imdbId
            if (!resolvedImdbId.isNullOrBlank()) {
                if (normalizedType == "series" && parts.size >= (if (hasPrefix) 4 else 3)) {
                    val seasonIdx = if (hasPrefix) 2 else 1
                    val episodeIdx = if (hasPrefix) 3 else 2
                    val s = parts[seasonIdx]
                    val e = parts[episodeIdx]
                    queryId = "$resolvedImdbId:$s:$e"
                } else {
                    queryId = resolvedImdbId
                }
                logger.i { "Resolved TMDB stream ID $id to IMDb ID $queryId in getStreams" }
            }
        }

        // Extract IMDb ID and season/episode from the resolved Stremio-style ID
        val queryParts = queryId.split(":")
        val imdbId = queryParts.getOrNull(0) ?: queryId
        val season = queryParts.getOrNull(1)?.toIntOrNull()
        val episode = queryParts.getOrNull(2)?.toIntOrNull()

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
                                id = queryId,
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

        // Resolve TMDB ID to IMDb ID if necessary for Stremio addon compatibility
        var queryId = id
        val isTmdb = id.startsWith("tmdb:") || id.startsWith("movie:") || id.startsWith("series:") || id.toIntOrNull() != null
        
        val baseIdForMeta = if (isTmdb) {
            val parts = id.split(":")
            val hasPrefix = parts[0] == "tmdb" || parts[0] == "series" || parts[0] == "tv" || parts[0] == "movie"
            if (hasPrefix) "${parts[0]}:${parts[1]}" else parts[0]
        } else {
            id.split(":")[0]
        }
        val rawNumericId = baseIdForMeta.removePrefix("tmdb:").removePrefix("series:").removePrefix("movie:").removePrefix("tv:")
        
        val requestedMedia = MediaItemStore.get(baseIdForMeta)
            ?: MediaItemStore.get("series:$rawNumericId")
            ?: MediaItemStore.get("movie:$rawNumericId")
            ?: MediaItemStore.get(rawNumericId)
            ?: MediaItemStore.get(id)
            ?: run {
                val variations = listOf(baseIdForMeta, "series:$rawNumericId", "movie:$rawNumericId", rawNumericId, id)
                var found: MediaItem? = null
                for (variant in variations) {
                    val cacheKey = CacheService.metaKey(variant)
                    try {
                        found = cacheService.getCachedParsed(cacheKey, metaSerializer)?.meta?.toDomain()
                        if (found != null) break
                    } catch (_: Exception) { }
                }
                found
            }

        if (isTmdb) {
            val parts = id.split(":")
            val hasPrefix = parts[0] == "tmdb" || parts[0] == "series" || parts[0] == "tv" || parts[0] == "movie"
            val resolvedImdbId = requestedMedia?.imdbId
            if (!resolvedImdbId.isNullOrBlank()) {
                if (normalizedType == "series" && parts.size >= (if (hasPrefix) 4 else 3)) {
                    val seasonIdx = if (hasPrefix) 2 else 1
                    val episodeIdx = if (hasPrefix) 3 else 2
                    val s = parts[seasonIdx]
                    val e = parts[episodeIdx]
                    queryId = "$resolvedImdbId:$s:$e"
                } else {
                    queryId = resolvedImdbId
                }
                logger.i { "Resolved TMDB stream ID $id to IMDb ID $queryId in getStreamsFlow" }
            }
        }

        // Extract IMDb ID and season/episode from the resolved Stremio-style ID
        val queryParts = queryId.split(":")
        val hasPrefix = queryParts.getOrNull(0) == "tmdb" || queryParts.getOrNull(0) == "series" || queryParts.getOrNull(0) == "tv" || queryParts.getOrNull(0) == "movie"
        val imdbId = if (hasPrefix) (queryParts.getOrNull(1) ?: queryId) else (queryParts.getOrNull(0) ?: queryId)
        val season = if (queryParts.size >= 3) queryParts[queryParts.size - 2].toIntOrNull() else null
        val episode = if (queryParts.size >= 3) queryParts[queryParts.size - 1].toIntOrNull() else null

        val streamSerializer = com.moviehub.core.model.StremioStreamResponse.serializer()
        val cacheKey = CacheService.streamKey(id, type)

        // ── Shared mutable state (guarded by mutex) ──
        val seen = mutableSetOf<String>()
        val accumulated = mutableListOf<StreamItem>()
        val allProviders = linkedMapOf<String, AddonStreamStatus>()
        val mutex = Mutex()

        suspend fun addUnique(streams: List<StreamItem>) {
            streams.forEach { stream ->
                val key = stream.url ?: stream.infoHash ?: stream.externalUrl
                if (key == null) return@forEach
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
        val softTimeout = 25_000L    // 25s — generous for slow connections, allows ~65 addons to complete
        val hardTimeout = 20_000L    // 20s per request — even 3G connections get a chance
        val semaphore = Semaphore(permits = 8)    // 8 concurrent — balanced throughput

        try {
            supervisorScope {
            // ── Launch all HTTP addon requests concurrently ──
            val addonJobs = streamAddons.map { (url, manifest) ->
                async {
                    val providerName= manifest.name

                    // Mark Fetching
                    val initialStatusMap = mutex.withLock {
                        allProviders[providerName] = AddonStreamStatus.Fetching
                        allProviders.toMap()
                    }
                    try {
                        send(
                            StreamsEvent.ProviderStatusChanged(
                                providerName, AddonStreamStatus.Fetching, initialStatusMap,
                            ),
                        )
                    } catch (_: Exception) {
                        // Channel closed or flow cancelled -- no point continuing
                        return@async
                    }

                    try {
                        val streams = withTimeout(hardTimeout) {
                            semaphore.withPermit {
                                apiClient.getStreams(
                                    baseUrl = url,
                                    type = normalizedType,
                                    id = queryId,
                                    addonName = manifest.name,
                                    addonId = manifest.id,
                                )
                            }
                        }
                        mutex.withLock {
                            addUnique(streams)
                            allProviders[providerName] = AddonStreamStatus.Completed(streams.size)
                        }
                        logger.i { "Addon $providerName: ${streams.size} streams returned, ${accumulated.size} total accumulated" }
                    } catch (_: TimeoutCancellationException) {
                        logger.d { "Stream addon $providerName timed out" }
                        mutex.withLock {
                            allProviders[providerName] = AddonStreamStatus.TimedOut(hardTimeout)
                        }
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        logger.d { "Stream addon $providerName cancelled (soft timeout)" }
                        mutex.withLock {
                            allProviders[providerName] = AddonStreamStatus.TimedOut(softTimeout)
                        }
                    } catch (e: Exception) {
                        logger.w { "Stream addon $providerName failed: ${e.message?.take(80)}" }
                        mutex.withLock {
                            allProviders[providerName] = AddonStreamStatus.Failed(e.message)
                        }
                    }

                    // Emit status update + current streams (without holding mutex to avoid channel flow deadlocks)
                    // Wrapped in try-catch: if the channel closes mid-send (flow cancelled / timeout),
                    // an uncaught exception here would crash the entire flow coroutine,
                    // preventing send(Completed) from ever being reached.
                    val (finalStatus, finalMap, finalStreams) = mutex.withLock {
                        Triple(allProviders[providerName]!!, allProviders.toMap(), accumulated.toList())
                    }
                    try {
                        send(StreamsEvent.ProviderStatusChanged(providerName, finalStatus, finalMap))
                        send(StreamsEvent.StreamsUpdated(finalStreams))
                    } catch (_: Exception) {
                        logger.w { "Failed to send stream update for $providerName (channel closed)" }
                    }
                }
            }

            // ── Launch all scraper requests concurrently ──
            val scraperJobs = scrapers.map { scraper ->
                async {
                    val providerName= "${scraper.name} (Plugin)"

                    // Mark Fetching
                    val initialStatusMap = mutex.withLock {
                        allProviders[providerName] = AddonStreamStatus.Fetching
                        allProviders.toMap()
                    }
                    try {
                        send(
                            StreamsEvent.ProviderStatusChanged(
                                providerName, AddonStreamStatus.Fetching, initialStatusMap,
                            ),
                        )
                    } catch (_: Exception) {
                        // Channel closed or flow cancelled -- no point continuing
                        return@async
                    }

                    try {
                        val scraperStreams = withTimeout(hardTimeout) {
                            semaphore.withPermit {
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
                        logger.i { "Scraper $providerName: ${scraperStreams.size} streams returned, ${accumulated.size} total accumulated" }
                    } catch (_: TimeoutCancellationException) {
                        logger.d { "Scraper $providerName timed out" }
                        mutex.withLock {
                            allProviders[providerName] = AddonStreamStatus.TimedOut(hardTimeout)
                        }
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        logger.d { "Scraper $providerName cancelled (soft timeout)" }
                        mutex.withLock {
                            allProviders[providerName] = AddonStreamStatus.TimedOut(softTimeout)
                        }
                    } catch (e: Exception) {
                        logger.w { "Scraper $providerName failed: ${e.message?.take(80)}" }
                        mutex.withLock {
                            allProviders[providerName] = AddonStreamStatus.Failed(e.message)
                        }
                    }

                    // Emit status update + current streams (without holding mutex to avoid channel flow deadlocks)
                    // Wrapped in try-catch: if the channel closes mid-send (flow cancelled / timeout),
                    // an uncaught exception here would crash the entire flow coroutine,
                    // preventing send(Completed) from ever being reached.
                    val (finalStatus, finalMap, finalStreams) = mutex.withLock {
                        Triple(allProviders[providerName]!!, allProviders.toMap(), accumulated.toList())
                    }
                    try {
                        send(StreamsEvent.ProviderStatusChanged(providerName, finalStatus, finalMap))
                        send(StreamsEvent.StreamsUpdated(finalStreams))
                    } catch (_: Exception) {
                        logger.w { "Failed to send scraper update for $providerName (channel closed)" }
                    }
                }
            }

            // Wait for all providers or timeout
            val completedNormally = withTimeoutOrNull(softTimeout) {
                (addonJobs + scraperJobs).awaitAll()
            }
            if (completedNormally == null) {
                val pendingAddons = addonJobs.count { it.isActive }
                val pendingScrapers = scraperJobs.count { it.isActive }
                val completed = (addonJobs + scraperJobs).size - pendingAddons - pendingScrapers
                logger.i { "Streams resolved: $completed completed, ${pendingAddons + pendingScrapers} timed out (${softTimeout}ms)" }
                addonJobs.forEach { if (it.isActive) it.cancel() }
                scraperJobs.forEach { if (it.isActive) it.cancel() }
            }
        }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) {
                logger.d { "StreamsFlow cancelled by collector" }
                throw e  // MUST re-throw to maintain Kotlin coroutine cancellation contract
            }
            logger.w(e) { "StreamsFlow unexpected error — emitting partial results" }
        } finally {
            // ═══════════════════════════════════════════
            // GUARANTEED COMPLETION: Always emit the Completed event
            // regardless of how the supervisorScope exited (normal, timeout, exception).
            // This is the single safety net that prevents the UI from being stuck
            // in the "Searching Addons" loading state forever.
            // ═══════════════════════════════════════════
            val finalStreams: List<StreamItem>
            val finalProviders: Map<String, AddonStreamStatus>
            mutex.withLock {
                // Mark any providers still in Pending/Fetching as timed out
                allProviders.keys.forEach { name ->
                    val status = allProviders[name]
                    if (status == AddonStreamStatus.Pending || status == AddonStreamStatus.Fetching) {
                        allProviders[name] = AddonStreamStatus.TimedOut(softTimeout)
                    }
                }
                finalStreams = accumulated.toList()
                finalProviders = allProviders.toMap()
            }

            logger.i { "StreamsFlow resolved ${finalStreams.size} unique streams (${finalProviders.size} providers)" }

            // Cache the final merged result
            try {
                cacheService.putCache(
                    cacheKey, "stream",
                    json.encodeToString(streamSerializer, com.moviehub.core.model.StremioStreamResponse(streams = finalStreams)),
                )
            } catch (_: Exception) { }

            try {
                send(StreamsEvent.Completed(finalStreams, finalProviders))
            } catch (_: Exception) {
                // Channel already closed — collector cancelled, nothing we can do
                logger.d { "StreamsFlow: collector cancelled before Completed could be sent" }
            }
        }
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

    private fun validateStreamItem(
        stream: StreamItem,
        media: MediaItem?,
        requestedType: String,
        requestedSeason: Int?,
        requestedEpisode: Int?
    ): Boolean {
        // Direct-play streams (URL-based, no infoHash) are pre-validated by the addon.
        // Only apply content validation to torrent streams which carry descriptive
        // metadata that may be mismatched (wrong movie, wrong season, etc.).
        if (stream.url != null && stream.infoHash == null) return true

        // Stream name, description and filename combination for thorough scanning
        val streamText = listOfNotNull(
            stream.name,
            stream.description,
            stream.behaviorHints.filename
        ).joinToString(" ").lowercase()

        // 1. Content Type Checking
        if (requestedType.lowercase() == "movie" && media != null) {
            // Reject movie streams with episode/season indicators
            val episodeRegex = Regex("""\b(s\d+e\d+|s\d+\s*ep\d+|ep\d+|episode\s*\d+|ep\s*\d+|ep\.\s*\d+|\d{1,2}x\d{1,2})\b""")
            val hasEpisodeIndicator = episodeRegex.containsMatchIn(streamText)
            val requestedTitleHasEpisode = media.title.lowercase().let { it.contains("episode") || it.contains("ep ") || it.contains("part") }
            if (hasEpisodeIndicator && !requestedTitleHasEpisode) {
                logger.w { "Rejecting stream because it has episode indicators but a movie was requested: ${stream.name} - ${stream.description}" }
                return false
            }
        }

        // 2. Anime Provider Validation
        val isAnimeProvider = listOfNotNull(stream.name, stream.addonName, stream.sourceName)
            .any { it.lowercase().contains("anime") || it.lowercase().contains("kitsu") || it.lowercase().contains("gogo") }
        if (isAnimeProvider && media != null) {
            val isRequestedAnime = media.genres.any { it.lowercase().contains("anime") || it.lowercase().contains("animation") } ||
                    media.title.lowercase().contains("anime") || media.description?.lowercase()?.contains("anime") == true
            
            if (!isRequestedAnime) {
                // If it's an anime provider, but the requested content is not anime, verify title matches exactly
                val cleanedReqTitle = cleanTitle(media.title)
                val cleanStreamText = cleanTitle(streamText)
                if (cleanedReqTitle.isNotEmpty() && !cleanStreamText.contains(cleanedReqTitle)) {
                    logger.w { "Rejecting anime provider stream for non-anime content: ${stream.name} - ${stream.description}" }
                    return false
                }
            }
        }

        // 3. Season/Episode Coordinate Matching
        if (requestedType.lowercase() == "series" || requestedType.lowercase() == "show") {
            // Check if stream lists a season/episode and whether it mismatches the requested ones
            val sxxexxMatch = Regex("""\bs(\d+)\s*e(\d+)\b""").find(streamText)
            if (sxxexxMatch != null) {
                val s = sxxexxMatch.groupValues[1].toIntOrNull()
                val e = sxxexxMatch.groupValues[2].toIntOrNull()
                if (s != null && requestedSeason != null && s != requestedSeason) {
                    logger.w { "Rejecting stream due to season mismatch: requested S$requestedSeason, stream has S$s" }
                    return false
                }
                if (e != null && requestedEpisode != null && e != requestedEpisode) {
                    logger.w { "Rejecting stream due to episode mismatch: requested E$requestedEpisode, stream has E$e" }
                    return false
                }
            }
        }

        // 4. Robust Title Word Verification (prevents completely unrelated contents)
        // Only applies to torrent streams (infoHash present) where names are descriptive.
        // Skipped for direct-play / web streams (URL-based) whose names are often
        // generic quality labels (e.g. "1080p", "HD Stream") that don't carry the title.
        if (media != null && stream.infoHash != null) {
            val cleanedReqTitle = cleanTitle(media.title)
            if (cleanedReqTitle.isNotEmpty()) {
                val reqWords = cleanedReqTitle.split(" ")
                    .filter { it.length > 2 && it !in listOf("the", "and", "for", "with", "from", "der", "die", "das") }
                if (reqWords.isNotEmpty()) {
                    val cleanStreamText = cleanTitle(streamText)
                    val matchesAny = reqWords.any { cleanStreamText.contains(it) }
                    if (!matchesAny) {
                        logger.w { "Rejecting stream due to title mismatch: requested '${media.title}', stream: '${stream.name} | ${stream.description}'" }
                        return false
                    }
                }
            }
        }

        // 5. Release Year Validation (off by more than 1 year, if both have years)
        if (media != null) {
            val yearRegex = Regex("""\b(19\d\d|20\d\d)\b""")
            val requestedYear = media.releaseDate?.take(4)?.toIntOrNull()
                ?: media.releaseInfo?.take(4)?.toIntOrNull()
            if (requestedYear != null) {
                val yearsInStream = yearRegex.findAll(streamText).mapNotNull { it.value.toIntOrNull() }.toList()
                if (yearsInStream.isNotEmpty()) {
                    val hasMatchingYear = yearsInStream.any { kotlin.math.abs(it - requestedYear) <= 1 }
                    if (!hasMatchingYear) {
                        logger.w { "Rejecting stream due to release year mismatch: requested $requestedYear, stream has $yearsInStream" }
                        return false
                    }
                }
            }
        }

        return true
    }

    private fun cleanTitle(title: String): String {
        return title.lowercase()
            .replace(Regex("""[^a-z0-9\s]"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
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
