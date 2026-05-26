package com.moviehub.feature.home.presentation

import androidx.compose.runtime.Immutable
import com.moviehub.core.model.MediaItem

@Immutable
data class CatalogState(
    val isLoading: Boolean = false,
    val isPaginating: Boolean = false,
    val canPaginate: Boolean = true,
    val displayedItems: List<MediaItem> = emptyList(),
    val watchedMediaIds: Set<String> = emptySet(),
    val error: String? = null
)
