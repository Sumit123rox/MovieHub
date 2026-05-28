package com.moviehub.core.network

import com.moviehub.core.database.CacheService
import com.moviehub.core.model.CatalogResponse
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class CatalogPrefetcher(
    private val apiClient: StremioApiClient,
    private val addonManager: AddonManager,
    private val cacheService: CacheService,
    private val dispatchers: NetworkDispatchers,
    private val json: Json,
) {
    suspend fun prefetchFirstPage() = withContext(dispatchers.io) {
        val addons = addonManager.installedAddons.value
        val tasks = addons.flatMap { manifest ->
            manifest.catalogs.map { catalog ->
                Triple(manifest, catalog, addonManager.getAddonUrl(manifest.id))
            }
        }.take(10)

        coroutineScope {
            tasks.forEach { (manifest, catalog, url) ->
                async {
                    if (url == null) return@async
                    val cacheKey = CacheService.catalogKey(catalog.type, catalog.id, manifest.id, 0)
                    if (cacheService.getCachedCatalog(cacheKey) != null) return@async
                    try {
                        val response = apiClient.getCatalog(url, catalog.type, catalog.id)
                        if (response != null) {
                            cacheService.putCacheSerialized(
                                cacheKey, "catalog", response, CatalogResponse.serializer()
                            )
                        }
                    } catch (_: Exception) { }
                }
            }
        }
    }
}
