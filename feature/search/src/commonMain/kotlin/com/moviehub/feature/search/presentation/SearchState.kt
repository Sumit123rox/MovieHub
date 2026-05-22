package com.moviehub.feature.search.presentation

import com.moviehub.core.model.MediaItem
import androidx.compose.runtime.Immutable

@Immutable
data class SearchState(
    val query: String = "",
    val isLoading: Boolean = false,
    val results: List<MediaItem> = emptyList(),
    val error: String? = null
)

sealed interface SearchAction {
    data class QueryChanged(val query: String) : SearchAction
    object PerformSearch : SearchAction
}
