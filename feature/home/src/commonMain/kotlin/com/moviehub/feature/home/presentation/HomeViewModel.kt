package com.moviehub.feature.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moviehub.core.database.ProfileRepository
import com.moviehub.core.database.WatchHistoryDao
import com.moviehub.core.database.WatchProgressDao
import com.moviehub.core.model.ContinueWatchingItem
import com.moviehub.feature.home.data.HomeRepository
import com.moviehub.core.network.AddonManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll

class HomeViewModel(
    private val repository: HomeRepository,
    private val addonManager: AddonManager,
    private val watchProgressDao: WatchProgressDao,
    private val watchHistoryDao: WatchHistoryDao,
    private val profileRepository: ProfileRepository
) : ViewModel() {
    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    private val _event = MutableSharedFlow<HomeEvent>()
    val event: SharedFlow<HomeEvent> = _event.asSharedFlow()

    init {
        observeContinueWatching()
        observeWatchedState()
        viewModelScope.launch {
            try {
                var lastLoadedAddons: List<com.moviehub.core.model.StremioManifest>? = null
                addonManager.installedAddons.collectLatest { addons ->
                    try {
                        _state.value = _state.value.copy(installedAddons = addons)

                        val isSame = lastLoadedAddons != null &&
                                lastLoadedAddons!!.size == addons.size &&
                                lastLoadedAddons!!.zip(addons).all { (a, b) -> a.id == b.id && a.version == b.version }

                        if (isSame && _state.value.dynamicSections.isNotEmpty()) {
                            return@collectLatest
                        }

                        lastLoadedAddons = addons

                        if (addons.isNotEmpty()) {
                            loadDynamicCatalogs(addons)
                        } else {
                            _state.value = _state.value.copy(
                                dynamicSections = emptyList(),
                                activeAddonId = null,
                                activeAddonName = null
                            )
                        }
                    } catch (e: Exception) {
                        _state.value = _state.value.copy(error = "Error processing addons: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = "Failed to observe addons: ${e.message}")
            }
        }
    }

    private fun observeWatchedState() {
        viewModelScope.launch {
            try {
                profileRepository.activeProfile.collectLatest { profile ->
                    if (profile != null) {
                        try {
                            watchProgressDao.getWatchedMediaIds(profile.id).collectLatest { watchedIds ->
                                _state.value = _state.value.copy(watchedMediaIds = watchedIds.toSet())
                            }
                        } catch (e: Exception) {
                            _state.value = _state.value.copy(watchedMediaIds = emptySet())
                        }
                    } else {
                        _state.value = _state.value.copy(watchedMediaIds = emptySet())
                    }
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(watchedMediaIds = emptySet())
            }
        }
    }

    private fun observeContinueWatching() {
        viewModelScope.launch {
            try {
                profileRepository.activeProfile.collectLatest { profile ->
                    if (profile != null) {
                        try {
                            watchProgressDao.getInProgress(profile.id, 20).collectLatest { progressList ->
                                try {
                                    if (progressList.isEmpty()) {
                                        _state.value = _state.value.copy(continueWatching = emptyList())
                                        return@collectLatest
                                    }
                                    val mediaIds = progressList.map { it.mediaId }
                                    val historyMap = watchHistoryDao.getWatchHistoryBatch(mediaIds, profile.id)
                                        .associateBy { it.mediaId }

                                    _state.value = _state.value.copy(
                                        continueWatching = progressList.mapNotNull { progress ->
                                            val history = historyMap[progress.mediaId]
                                            if (history != null && progress.durationMs > 0) {
                                                ContinueWatchingItem(
                                                    mediaId = progress.mediaId,
                                                    title = history.title,
                                                    type = progress.type,
                                                    posterUrl = history.posterPath,
                                                    progressMs = progress.progressMs,
                                                    durationMs = progress.durationMs,
                                                    lastWatchedAt = history.lastWatchedAt
                                                )
                                            } else null
                                        }
                                    )
                                } catch (e: Exception) {
                                    _state.value = _state.value.copy(continueWatching = emptyList())
                                }
                            }
                        } catch (e: Exception) {
                            _state.value = _state.value.copy(continueWatching = emptyList())
                        }
                    } else {
                        _state.value = _state.value.copy(continueWatching = emptyList())
                    }
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(continueWatching = emptyList())
            }
        }
    }

    fun onAction(action: HomeAction) {
        when (action) {
            is HomeAction.Refresh -> {
                viewModelScope.launch {
                    loadDynamicCatalogs(_state.value.installedAddons)
                }
            }
            is HomeAction.SelectAddon -> {
                val addon = _state.value.installedAddons.find { it.id == action.addonId }
                _state.value = _state.value.copy(
                    activeAddonId = if (action.addonId.isEmpty()) null else action.addonId,
                    activeAddonName = if (action.addonId.isEmpty()) null else addon?.name
                )
            }
        }
    }

    private suspend fun loadDynamicCatalogs(addons: List<com.moviehub.core.model.StremioManifest>) {
        _state.value = _state.value.copy(isLoading = true, dynamicSections = emptyList(), featuredItems = emptyList())
        
        try {
            val allSections = supervisorScope {
                addons.flatMap { manifest ->
                    manifest.catalogs.map { catalog ->
                        async {
                            try {
                                val items = repository.getCatalog(
                                    type = catalog.type,
                                    catalogId = catalog.id,
                                    addonId = manifest.id
                                ).take(20) // Fetch up to 20 to have enough after merging/filtering
                                
                                if (items.isNotEmpty()) {
                                    CatalogSection(
                                        addonId = manifest.id,
                                        addonName = manifest.name,
                                        catalogId = catalog.id,
                                        catalogName = catalog.name ?: catalog.id.replaceFirstChar { it.uppercase() },
                                        type = catalog.type,
                                        items = items
                                    )
                                } else null
                            } catch (e: Exception) {
                                null
                            }
                        }
                    }
                }.awaitAll().filterNotNull()
            }
            
            // 1. Capture Featured Items ONLY ONCE for the Hero
            val featuredSection = allSections.find { it.items.size >= 5 } ?: allSections.firstOrNull()
            val featured = featuredSection?.items?.take(5) ?: emptyList()
            val featuredIds = featured.map { it.id }.toSet()
            
            // 2. Group and consolidate sections by catalogName (case-insensitive) and type
            val groupedSections = allSections.groupBy { "${it.catalogName.trim().lowercase()}_${it.type}" }
            
            val consolidatedSections = groupedSections.map { (groupKey, sections) ->
                val firstSection = sections.first()
                val catalogName = firstSection.catalogName
                val type = firstSection.type
                
                // Combine all items from all sections in this group, then deduplicate by id
                val combinedItems = sections.flatMap { it.items }
                val deduplicatedItems = mutableListOf<com.moviehub.core.model.MediaItem>()
                val seenIds = mutableSetOf<String>()
                
                combinedItems.forEach { item ->
                    if (item.id !in seenIds && item.id !in featuredIds) {
                        seenIds.add(item.id)
                        deduplicatedItems.add(item)
                    }
                }
                
                val uniqueAddons = sections.map { it.addonName }.distinct()
                val addonName = if (uniqueAddons.size == 1) uniqueAddons.first() else "Multiple Providers"
                val addonId = if (uniqueAddons.size == 1) firstSection.addonId else "multi_addons"
                val catalogId = if (uniqueAddons.size == 1) firstSection.catalogId else "merged_${firstSection.catalogId}"
                
                CatalogSection(
                    addonId = addonId,
                    addonName = addonName,
                    catalogId = catalogId,
                    catalogName = catalogName,
                    type = type,
                    items = deduplicatedItems
                )
            }.filter { it.items.isNotEmpty() }
            
            // Sort consolidated sections by type and then catalogName for premium layout
            val finalSections = consolidatedSections.sortedWith(
                compareBy({ it.type }, { it.catalogName })
            )
            
            _state.value = _state.value.copy(
                featuredItems = featured,
                dynamicSections = finalSections,
                isLoading = false
            )
        } catch (e: Exception) {
            _state.value = _state.value.copy(
                isLoading = false,
                error = e.message ?: "Failed to load catalogs"
            )
        }
    }
}
