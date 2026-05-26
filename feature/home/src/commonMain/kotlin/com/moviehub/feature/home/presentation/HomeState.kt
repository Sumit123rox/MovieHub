package com.moviehub.feature.home.presentation

import com.moviehub.core.model.ContinueWatchingItem
import com.moviehub.core.model.MediaItem
import com.moviehub.core.model.StremioManifest
import com.moviehub.core.model.StremioCatalog
import androidx.compose.runtime.Immutable

@Immutable
data class HomeState(
    val isLoading: Boolean = false,
    val activeAddonId: String? = null,
    val activeAddonName: String? = null,
    val installedAddons: List<StremioManifest> = emptyList(),
    val dynamicSections: List<CatalogSection> = emptyList(),
    val featuredItems: List<MediaItem> = emptyList(),
    val continueWatching: List<ContinueWatchingItem> = emptyList(),
    val watchedMediaIds: Set<String> = emptySet(),
    val error: String? = null
)

@Immutable
data class CatalogSection(
    val addonId: String,
    val addonName: String,
    val catalogId: String,
    val catalogName: String,
    val type: String,
    val items: List<MediaItem> = emptyList()
)

sealed interface HomeAction {
    object Refresh : HomeAction
    data class SelectAddon(val addonId: String) : HomeAction
}

sealed interface HomeEvent {
    data class Error(val message: String) : HomeEvent
}
