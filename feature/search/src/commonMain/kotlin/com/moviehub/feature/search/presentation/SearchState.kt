package com.moviehub.feature.search.presentation

import androidx.compose.runtime.Immutable
import com.moviehub.core.model.MediaItem

@Immutable
data class SearchState(
    val query: String = "",
    val isLoading: Boolean = false,
    val results: List<MediaItem> = emptyList(),
    val suggestions: List<String> = emptyList(),
    val recentSearches: List<String> = emptyList(),
    val error: String? = null,
)

sealed interface SearchAction {
    data class QueryChanged(val query: String) : SearchAction
    data object PerformSearch : SearchAction
    data object ClearSearchHistory : SearchAction
    data class RemoveSearch(val query: String) : SearchAction
    data class SelectRecentSearch(val query: String) : SearchAction
    data class SelectSuggestion(val query: String) : SearchAction
}
