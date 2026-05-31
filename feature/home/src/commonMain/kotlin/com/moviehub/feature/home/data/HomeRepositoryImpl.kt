package com.moviehub.feature.home.data

import co.touchlab.kermit.Logger
import com.moviehub.core.database.CacheService
import com.moviehub.core.model.CatalogResponse
import com.moviehub.core.model.MediaItem
import com.moviehub.core.network.AddonManager
import com.moviehub.core.network.StremioApiClient
import com.moviehub.core.network.mapper.toDomain
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import kotlinx.serialization.json.Json

class HomeRepositoryImpl(
    private val apiClient: StremioApiClient,
    private val addonManager: AddonManager,
    private val cacheService: CacheService,
    private val json: Json,
) : HomeRepository {

    private val logger = Logger.withTag("HomeRepository")

    override suspend fun getTrendingMovies(addonId: String?): List<MediaItem> = getCatalog("movie", "top", addonId)

    override suspend fun getPopularMovies(addonId: String?): List<MediaItem> = getCatalog("movie", "popular", addonId)

    override suspend fun getTrendingShows(addonId: String?): List<MediaItem> = getCatalog("series", "top", addonId)

    override suspend fun getPopularShows(addonId: String?): List<MediaItem> = getCatalog("series", "popular", addonId)

    /**
     * Load catalogs from network — checks cache first as fallback on error.
     * Used by CatalogViewModel for "See All" pages.
     */
    override suspend fun getCatalog(type: String, catalogId: String, addonId: String?, skip: Int): List<MediaItem> = supervisorScope {
        val allAddons = addonManager.installedAddons.value
        val originalId = catalogId.removePrefix("merged_")
        val originalCatalog = allAddons.flatMap { it.catalogs }.firstOrNull {
            it.type.lowercase() == type.lowercase() && it.id.lowercase() == originalId.lowercase()
        }
        val originalName = originalCatalog?.name

        val capableAddons = if (addonId != null && addonId != "multi_addons") {
            allAddons.filter { it.id == addonId }
        } else {
            allAddons.filter { manifest ->
                val matchesType = manifest.types.isEmpty() ||
                    manifest.types.any { it.lowercase() == type.lowercase() } ||
                    (type == "series" && manifest.types.any { it.lowercase() in listOf("show", "tv") })

                val hasCatalog = manifest.catalogs.any { catalog ->
                    catalog.type.lowercase() == type.lowercase() && (
                        catalog.id.lowercase() == originalId.lowercase() ||
                        catalog.id.lowercase() == catalogId.lowercase() ||
                        (originalName != null && catalog.name?.lowercase() == originalName.lowercase()) ||
                        originalId == "top"
                    )
                }

                matchesType && (hasCatalog || manifest.catalogs.isNotEmpty())
            }
        }

        if (capableAddons.isEmpty()) {
            logger.w { "HomeRepository: No capable addons found for $type/$catalogId (addonId=$addonId)" }
        }

        val deferredItems = capableAddons.map { manifest ->
            async {
                try {
                    val url = addonManager.getAddonUrl(manifest.id) ?: return@async emptyList<MediaItem>()
                    val extra = if (skip > 0) mapOf("skip" to skip.toString()) else emptyMap()

                    val actualCatalogId = manifest.catalogs.firstOrNull {
                        it.type.lowercase() == type.lowercase() && (
                            it.id.lowercase() == originalId.lowercase() ||
                            (originalName != null && it.name?.lowercase() == originalName.lowercase())
                        )
                    }?.id ?: manifest.catalogs.firstOrNull { it.type.lowercase() == type.lowercase() }?.id ?: originalId

                    val cacheKey = CacheService.catalogKey(type, actualCatalogId, manifest.id, skip)

                    // 1. Fetch from network
                    try {
                        val response = apiClient.getCatalog(
                            baseUrl = url,
                            type = type,
                            id = actualCatalogId,
                            extra = extra,
                        )
                        if (response != null) {
                            cacheService.putCache(cacheKey, "catalog", json.encodeToString(CatalogResponse.serializer(), response))
                            return@async response.metas.toDomain(addonId = manifest.id, addonUrl = url)
                        }
                    } catch (e: Exception) {
                        logger.e(e) { "Error fetching catalog for ${manifest.id}" }
                    }

                    // 2. Fallback to cache
                    val cached = cacheService.getCachedCatalogParsed(cacheKey, CatalogResponse.serializer())
                    if (cached != null) {
                        logger.i { "Using cached catalog for ${manifest.id}/$actualCatalogId" }
                        return@async cached.metas.toDomain(addonId = manifest.id, addonUrl = url)
                    }

                    emptyList()
                } catch (e: Exception) {
                    logger.e(e) { "Error in catalog fetch for ${manifest.id}" }
                    emptyList()
                }
            }
        }

        val fetchedLists = deferredItems.awaitAll()
        val result = if (addonId == "multi_addons" || catalogId.startsWith("merged_")) {
            val interleaved = mutableListOf<MediaItem>()
            var index = 0
            while (true) {
                var addedAny = false
                capableAddons.forEachIndexed { addonIdx, _ ->
                    val itemsFromAddon = fetchedLists[addonIdx]
                    if (index < itemsFromAddon.size) {
                        interleaved.add(itemsFromAddon[index])
                        addedAny = true
                    }
                }
                if (!addedAny) break
                index++
            }
            interleaved.distinctBy { it.id }
        } else {
            fetchedLists.flatten().distinctBy { it.id }
        }

        logger.i { "HomeRepository: Fetched ${result.size} items for $type/$catalogId (skip=$skip)" }
        result
    }

    /**
     * Load catalogs from Room cache only — no network calls.
     * Returns immediately for cached items, empty for uncached.
     */
    override suspend fun getCachedCatalogs(
        addons: List<com.moviehub.core.model.StremioManifest>,
        type: String,
        catalogId: String,
    ): List<MediaItem> = supervisorScope {
        addons.map { manifest ->
            async {
                try {
                    val url = addonManager.getAddonUrl(manifest.id) ?: return@async emptyList<MediaItem>()
                    val actualCatalogId = manifest.catalogs.firstOrNull {
                        it.type.lowercase() == type.lowercase() && it.id.lowercase() == catalogId.lowercase()
                    }?.id ?: manifest.catalogs.firstOrNull { it.type.lowercase() == type.lowercase() }?.id ?: catalogId

                    val cacheKey = CacheService.catalogKey(type, actualCatalogId, manifest.id, 0)
                    val cached = cacheService.getCachedCatalogParsed(cacheKey, CatalogResponse.serializer())
                    if (cached != null) {
                        cached.metas.toDomain(addonId = manifest.id, addonUrl = url)
                    } else {
                        emptyList()
                    }
                } catch (e: Exception) {
                    emptyList()
                }
            }
        }.awaitAll().flatten().distinctBy { it.id }
    }

    /**
     * Refresh catalog from network — saves to cache on success, returns the fresh items.
     * Does NOT check cache first.
     */
    override suspend fun refreshCatalogFromNetwork(
        type: String,
        catalogId: String,
        addonId: String?,
        skip: Int,
    ): List<MediaItem> = supervisorScope {
        val allAddons = addonManager.installedAddons.value

        val capableAddons = if (addonId != null) {
            allAddons.filter { it.id == addonId }
        } else {
            allAddons.filter { manifest ->
                val matchesType = manifest.types.isEmpty() ||
                    manifest.types.any { it.lowercase() == type.lowercase() } ||
                    (type == "series" && manifest.types.any { it.lowercase() in listOf("show", "tv") })

                val hasCatalog = manifest.catalogs.any {
                    it.type.lowercase() == type.lowercase() &&
                        (it.id.lowercase() == catalogId.lowercase() || catalogId == "top")
                }

                matchesType && (hasCatalog || manifest.catalogs.isNotEmpty())
            }
        }

        if (capableAddons.isEmpty()) {
            return@supervisorScope emptyList()
        }

        capableAddons.map { manifest ->
            async {
                try {
                    val url = addonManager.getAddonUrl(manifest.id) ?: return@async emptyList<MediaItem>()
                    val extra = if (skip > 0) mapOf("skip" to skip.toString()) else emptyMap()

                    val actualCatalogId = manifest.catalogs.firstOrNull {
                        it.type.lowercase() == type.lowercase() && it.id.lowercase() == catalogId.lowercase()
                    }?.id ?: manifest.catalogs.firstOrNull { it.type.lowercase() == type.lowercase() }?.id ?: catalogId

                    val cacheKey = CacheService.catalogKey(type, actualCatalogId, manifest.id, skip)
                    val response = apiClient.getCatalog(url, type, actualCatalogId, extra)

                    if (response != null) {
                        cacheService.putCache(cacheKey, "catalog", json.encodeToString(CatalogResponse.serializer(), response))
                        response.metas.toDomain(addonId = manifest.id, addonUrl = url)
                    } else {
                        emptyList()
                    }
                } catch (e: Exception) {
                    logger.e(e) { "Network refresh failed for ${manifest.id}/$catalogId" }
                    emptyList()
                }
            }
        }.awaitAll().flatten().distinctBy { it.id }
    }
}
