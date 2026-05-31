package com.moviehub.feature.home.presentation

import androidx.compose.runtime.Immutable
import com.moviehub.core.model.ContinueWatchingItem
import com.moviehub.core.model.MediaItem
import com.moviehub.core.model.StremioManifest

@Immutable
data class HomeState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMoreSections: Boolean = false,
    val installedAddons: List<StremioManifest> = emptyList(),
    val dynamicSections: List<CatalogSection> = emptyList(),
    val featuredItems: List<MediaItem> = emptyList(),
    val continueWatching: List<ContinueWatchingItem> = emptyList(),
    val watchedMediaIds: Set<String> = emptySet(),
    val error: String? = null,
)

@Immutable
data class CatalogSection(
    val addonId: String,
    val addonName: String,
    val catalogId: String,
    val catalogName: String,
    val type: String,
    val items: List<MediaItem> = emptyList(),
)

sealed interface HomeAction {
    data object Refresh : HomeAction
    data class MarkAsWatched(val mediaId: String) : HomeAction
    data class RemoveFromContinue(val mediaId: String) : HomeAction
    data object LoadMore : HomeAction

    /**
     * Fired after [MovieHubDimens.PrefetchTiming.catalogItemHoverMs] ms of hover/press on a
     * catalog item. The ViewModel pre-warms stream connections in the background.
     */
    data class PrewarmCatalogItem(val mediaId: String, val type: String, val addonId: String?) : HomeAction
}
