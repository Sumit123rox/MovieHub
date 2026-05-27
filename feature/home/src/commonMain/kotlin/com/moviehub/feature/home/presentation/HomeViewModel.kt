package com.moviehub.feature.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moviehub.core.database.ProfileRepository
import com.moviehub.core.database.WatchHistoryDao
import com.moviehub.core.database.WatchProgressDao
import com.moviehub.core.model.ContinueWatchingItem
import com.moviehub.feature.home.data.HomeRepository
import com.moviehub.core.network.AddonManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private val pageSize = 5
    private var pendingCatalogs: List<PendingCatalog> = emptyList()

    private data class PendingCatalog(
        val addonId: String,
        val addonName: String,
        val type: String,
        val catalogId: String,
        val catalogName: String?
    )

    init {
        observeContinueWatching()
        observeWatchedState()
        viewModelScope.launch {
            try {
                var lastLoadedAddons: List<com.moviehub.core.model.StremioManifest>? = null
                addonManager.installedAddons.collect { addons ->
                    try {
                        _state.value = _state.value.copy(installedAddons = addons)

                        val isSame = lastLoadedAddons != null &&
                                lastLoadedAddons!!.size == addons.size &&
                                lastLoadedAddons!!.zip(addons).all { (a, b) -> a.id == b.id && a.version == b.version }

                        if (isSame && _state.value.dynamicSections.isNotEmpty()) {
                            return@collect
                        }

                        lastLoadedAddons = addons

                        if (addons.isNotEmpty()) {
                            loadDynamicCatalogs(addons)
                        } else {
                            _state.value = _state.value.copy(
                                dynamicSections = emptyList()
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
                profileRepository.activeProfile.collect { profile ->
                    if (profile != null) {
                        try {
                            watchProgressDao.getWatchedMediaIds(profile.id).collect { watchedIds ->
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
                profileRepository.activeProfile.collect { profile ->
                    if (profile != null) {
                        try {
                            watchProgressDao.getInProgress(profile.id, 20).collect { progressList ->
                                try {
                                    if (progressList.isEmpty()) {
                                        _state.value = _state.value.copy(continueWatching = emptyList())
                                        return@collect
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
                                        }.sortedByDescending { it.lastWatchedAt }
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
            is HomeAction.MarkAsWatched -> {
                viewModelScope.launch {
                    val profileId = profileRepository.activeProfile.value?.id ?: return@launch
                    watchProgressDao.deleteProgress(action.mediaId, profileId)
                    watchProgressDao.insertOrUpdate(
                        com.moviehub.core.database.WatchProgress(
                            mediaId = action.mediaId,
                            profileId = profileId,
                            type = "movie",
                            progressMs = 0,
                            durationMs = 0,
                            isWatched = true
                        )
                    )
                }
            }
            is HomeAction.RemoveFromContinue -> {
                viewModelScope.launch {
                    val profileId = profileRepository.activeProfile.value?.id ?: return@launch
                    watchProgressDao.deleteProgress(action.mediaId, profileId)
                }
            }
            is HomeAction.LoadMore -> {
                viewModelScope.launch {
                    if (!_state.value.isLoadingMore && pendingCatalogs.isNotEmpty()) {
                        _state.value = _state.value.copy(isLoadingMore = true)
                        try {
                            loadNextCatalogPage()
                        } catch (e: Exception) {
                            // Ignore — individual requests are already caught internally
                        }
                        _state.value = _state.value.copy(
                            isLoadingMore = false,
                            hasMoreSections = pendingCatalogs.isNotEmpty()
                        )
                    }
                }
            }
        }
    }

    private suspend fun loadDynamicCatalogs(addons: List<com.moviehub.core.model.StremioManifest>) {
        try {
            // Show shimmer immediately — prevents empty-state text from flashing
            // during the gap between addon detection and cache query completion
            _state.value = _state.value.copy(isLoading = true)

            // Build full pending list of all catalog combos from all addons,
            // deduplicated by (catalogId, type) — multiple addons often expose
            // the same catalog, causing wasted network requests and duplicate sections.
            pendingCatalogs = addons.flatMap { manifest ->
                manifest.catalogs.map { catalog ->
                    PendingCatalog(
                        addonId = manifest.id,
                        addonName = manifest.name,
                        type = catalog.type,
                        catalogId = catalog.id,
                        catalogName = catalog.name
                    )
                }
            }.distinctBy { "${it.catalogId}_${it.type}" }

            // ═══════════════════════════════════════════
            // PHASE 1: Load first page from cache only (fast, no network)
            // Only loads pageSize catalogs — avoids rendering too many sections at once
            // ═══════════════════════════════════════════
            val firstPage = pendingCatalogs.take(pageSize)
            pendingCatalogs = pendingCatalogs.drop(pageSize)

            val cachedSections = supervisorScope {
                firstPage.map { pending ->
                    async {
                        try {
                            val addon = addons.find { it.id == pending.addonId } ?: return@async null
                            val items = repository.getCachedCatalogs(
                                addons = listOf(addon),
                                type = pending.type,
                                catalogId = pending.catalogId
                            ).take(10)

                            if (items.isNotEmpty()) {
                                CatalogSection(
                                    addonId = pending.addonId,
                                    addonName = pending.addonName,
                                    catalogId = pending.catalogId,
                                    catalogName = pending.catalogName ?: pending.catalogId.replaceFirstChar { it.uppercase() },
                                    type = pending.type,
                                    items = items
                                )
                            } else null
                        } catch (e: Exception) {
                            null
                        }
                    }
                }.awaitAll().filterNotNull()
            }

            if (cachedSections.isNotEmpty()) {
                val cachedConsolidated = consolidateSections(cachedSections)
                _state.value = _state.value.copy(
                    featuredItems = cachedConsolidated.first,
                    dynamicSections = cachedConsolidated.second,
                    isLoading = false
                )
            } else {
                _state.value = _state.value.copy(isLoading = true)
            }

            // ═══════════════════════════════════════════
            // PHASE 2: Load first page from network
            // Subsequent pages load as user scrolls (via LoadMore action)
            // ═══════════════════════════════════════════
            loadNextCatalogPage()

            // Re-consolidate after network data arrives to populate featuredItems
            // if Phase 1 had empty cache
            if (_state.value.featuredItems.isEmpty() && _state.value.dynamicSections.isNotEmpty()) {
                val netConsolidated = consolidateSections(_state.value.dynamicSections)
                _state.value = _state.value.copy(featuredItems = netConsolidated.first)
            }

            _state.value = _state.value.copy(
                isLoading = false,
                hasMoreSections = pendingCatalogs.isNotEmpty()
            )
        } catch (e: Exception) {
            _state.value = _state.value.copy(
                isLoading = false,
                hasMoreSections = false,
                error = if (_state.value.dynamicSections.isEmpty()) e.message ?: "Failed to load catalogs" else null
            )
        }
    }

    /**
     * Load the next page of catalog sections from network.
     * Called once initially, then on each scroll-triggered LoadMore action.
     */
    private suspend fun loadNextCatalogPage() {
        val batch = pendingCatalogs.take(pageSize)
        if (batch.isEmpty()) return
        pendingCatalogs = pendingCatalogs.drop(pageSize)

        val networkSections = supervisorScope {
            batch.map { pending ->
                async {
                    try {
                        val items = repository.refreshCatalogFromNetwork(
                            type = pending.type,
                            catalogId = pending.catalogId,
                            addonId = pending.addonId
                        ).take(10)

                        if (items.isNotEmpty()) {
                            CatalogSection(
                                addonId = pending.addonId,
                                addonName = pending.addonName,
                                catalogId = pending.catalogId,
                                catalogName = pending.catalogName ?: pending.catalogId.replaceFirstChar { it.uppercase() },
                                type = pending.type,
                                items = items
                            )
                        } else null
                    } catch (e: Exception) {
                        null
                    }
                }
            }.awaitAll().filterNotNull()
        }

        if (networkSections.isNotEmpty()) {
            val currentSections = _state.value.dynamicSections.toMutableList()
            val existingKeys = currentSections.map { "${it.catalogName.trim().lowercase()}_${it.type}" }.toSet()
            // Update existing sections in place — keeps their position unchanged
            networkSections.forEach { section ->
                val key = "${section.catalogName.trim().lowercase()}_${section.type}"
                val existingIndex = currentSections.indexOfFirst {
                    "${it.catalogName.trim().lowercase()}_${it.type}" == key
                }
                if (existingIndex >= 0) {
                    currentSections[existingIndex] = currentSections[existingIndex].copy(items = section.items)
                }
            }
            // Append completely new sections at the end (preserves insertion order)
            val newSections = networkSections.filter { "${it.catalogName.trim().lowercase()}_${it.type}" !in existingKeys }
            currentSections.addAll(newSections)
            _state.value = _state.value.copy(dynamicSections = currentSections)
        }
    }

    /**
     * Consolidate raw sections into final display sections:
     * 1. Pick featured items (first section with 5+ items)
     * 2. Deduplicate by ID across merged sections
     * 3. Sort by type then name
     */
    private fun consolidateSections(sections: List<CatalogSection>): Pair<List<com.moviehub.core.model.MediaItem>, List<CatalogSection>> {
        if (sections.isEmpty()) return Pair(emptyList(), emptyList())

        val featuredSection = sections.find { it.items.size >= 10 } ?: sections.find { it.items.size >= 5 } ?: sections.firstOrNull()
        val featured = featuredSection?.items?.take(10)?.shuffled() ?: emptyList()
        val featuredIds = featured.map { it.id }.toSet()

        val groupedSections = sections.groupBy { "${it.catalogName.trim().lowercase()}_${it.type}" }

        val consolidated = groupedSections.map { (_, groupSections) ->
            val first = groupSections.first()
            val combinedItems = groupSections.flatMap { it.items }
            val deduplicatedItems = mutableListOf<com.moviehub.core.model.MediaItem>()
            val seenIds = mutableSetOf<String>()

            combinedItems.forEach { item ->
                if (item.id !in seenIds && item.id !in featuredIds) {
                    seenIds.add(item.id)
                    deduplicatedItems.add(item)
                }
            }

            val uniqueAddons = groupSections.map { it.addonName }.distinct()
            CatalogSection(
                addonId = if (uniqueAddons.size == 1) first.addonId else "multi_addons",
                addonName = if (uniqueAddons.size == 1) uniqueAddons.first() else "Multiple Providers",
                catalogId = if (uniqueAddons.size == 1) first.catalogId else "merged_${first.catalogId}",
                catalogName = first.catalogName,
                type = first.type,
                items = deduplicatedItems
            )
        }.filter { it.items.isNotEmpty() }

        val sorted = consolidated.sortedWith(compareBy({ it.type }, { it.catalogName }))
        return Pair(featured, sorted)
    }
}
