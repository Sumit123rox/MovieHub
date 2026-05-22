package com.moviehub.feature.home.data

import co.touchlab.kermit.Logger
import com.moviehub.core.model.MediaItem
import com.moviehub.core.network.StremioApiClient
import com.moviehub.core.network.AddonManager
import com.moviehub.core.network.mapper.toDomain
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope

class HomeRepositoryImpl(
    private val apiClient: StremioApiClient,
    private val addonManager: AddonManager
) : HomeRepository {

    private val logger = Logger.withTag("HomeRepository")

    override suspend fun getTrendingMovies(addonId: String?): List<MediaItem> = getCatalog("movie", "top", addonId)

    override suspend fun getPopularMovies(addonId: String?): List<MediaItem> = getCatalog("movie", "popular", addonId)

    override suspend fun getTrendingShows(addonId: String?): List<MediaItem> = getCatalog("series", "top", addonId)

    override suspend fun getPopularShows(addonId: String?): List<MediaItem> = getCatalog("series", "popular", addonId)

    override suspend fun getCatalog(type: String, catalogId: String, addonId: String?, skip: Int): List<MediaItem> = supervisorScope {
        val allAddons = addonManager.installedAddons.value
        
        // Relaxed Filter: If addonId is provided, we trust the caller. 
        // Otherwise, find addons that claim to provide this catalog or have catalogs of this type.
        val capableAddons = if (addonId != null) {
            allAddons.filter { it.id == addonId }
        } else {
            allAddons.filter { manifest ->
                val matchesType = manifest.types.any { it.lowercase() == type.lowercase() } ||
                                 (type == "series" && manifest.types.any { it.lowercase() in listOf("show", "tv") })
                
                val hasCatalog = manifest.catalogs.any { 
                    it.type.lowercase() == type.lowercase() && 
                    (it.id.lowercase() == catalogId.lowercase() || catalogId == "top") 
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
                    
                    // Use the specific catalogId if it exists in manifest, else use the first of type, else fallback to provided ID
                    val actualCatalogId = manifest.catalogs.firstOrNull { 
                        it.type.lowercase() == type.lowercase() && it.id.lowercase() == catalogId.lowercase() 
                    }?.id ?: manifest.catalogs.firstOrNull { it.type.lowercase() == type.lowercase() }?.id ?: catalogId

                    val response = apiClient.getCatalog(
                        baseUrl = url,
                        type = type,
                        id = actualCatalogId,
                        extra = extra
                    )
                    
                    response?.metas?.toDomain(addonId = manifest.id, addonUrl = url) ?: emptyList()
                } catch (e: Exception) {
                    logger.e(e) { "Error fetching catalog for ${manifest.id}" }
                    emptyList()
                }
            }
        }
        
        val result = deferredItems.awaitAll().flatten().distinctBy { it.id }
        logger.i { "HomeRepository: Fetched ${result.size} items for $type/$catalogId (skip=$skip)" }
        result
    }
}
