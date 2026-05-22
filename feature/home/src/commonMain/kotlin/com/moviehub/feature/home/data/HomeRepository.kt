package com.moviehub.feature.home.data

import com.moviehub.core.model.MediaItem

interface HomeRepository {
    suspend fun getTrendingMovies(addonId: String? = null): List<MediaItem>
    suspend fun getPopularMovies(addonId: String? = null): List<MediaItem>
    suspend fun getTrendingShows(addonId: String? = null): List<MediaItem>
    suspend fun getPopularShows(addonId: String? = null): List<MediaItem>
    suspend fun getCatalog(type: String, catalogId: String, addonId: String? = null, skip: Int = 0): List<MediaItem>
}
