package com.moviehub.feature.home.data

import com.moviehub.core.model.MediaItem

interface HomeRepository {
    suspend fun getTrendingMovies(addonId: String? = null): List<MediaItem>
    suspend fun getPopularMovies(addonId: String? = null): List<MediaItem>
    suspend fun getTrendingShows(addonId: String? = null): List<MediaItem>
    suspend fun getPopularShows(addonId: String? = null): List<MediaItem>
    suspend fun getCatalog(type: String, catalogId: String, addonId: String? = null, skip: Int = 0): List<MediaItem>

    /** Load catalogs from Room cache only — fast, no network calls */
    suspend fun getCachedCatalogs(addons: List<com.moviehub.core.model.StremioManifest>, type: String, catalogId: String): List<MediaItem>

    /** Refresh catalogs from network — updates cache on success */
    suspend fun refreshCatalogFromNetwork(type: String, catalogId: String, addonId: String? = null, skip: Int = 0): List<MediaItem>
}
