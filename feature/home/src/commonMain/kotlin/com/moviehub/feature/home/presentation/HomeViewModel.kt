package com.moviehub.feature.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moviehub.core.database.ProfileRepository
import com.moviehub.core.database.WatchHistoryDao
import com.moviehub.core.database.WatchProgressDao
import com.moviehub.core.model.ContinueWatchingItem
import com.moviehub.core.model.StremioManifest
import com.moviehub.core.network.AddonManager
import com.moviehub.core.network.StremioApiClient
import com.moviehub.core.utils.PerformanceMonitor
import com.moviehub.feature.home.data.HomeRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

class HomeViewModel(
    private val repository: HomeRepository,
    private val addonManager: AddonManager,
    private val watchProgressDao: WatchProgressDao,
    private val watchHistoryDao: WatchHistoryDao,
    private val profileRepository: ProfileRepository,
    private val stremioApiClient: StremioApiClient? = null,
) : ViewModel() {
    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    private val pageSize = 5
    private var pendingCatalogs: List<PendingCatalog> = emptyList()

    private data class CatalogSource(
        val addonId: String,
        val addonName: String,
        val catalogId: String,
    )

    private data class PendingCatalog(
        val type: String,
        val catalogName: String,
        val sources: List<CatalogSource>,
    )

    init {
        observeContinueWatching()
        observeWatchedState()
        viewModelScope.launch {
            try {
                var lastLoadedAddons: List<StremioManifest>? = null
                addonManager.installedAddons
                    .distinctUntilChanged { old, new ->
                        old.size == new.size && old.zip(new).all { (a, b) -> a.id == b.id && a.version == b.version }
                    }
                    .collect { addons ->
                        try {
                            _state.value = _state.value.copy(installedAddons = addons)

                            val isSame = lastLoadedAddons != null &&
                                lastLoadedAddons?.size == addons.size &&
                                lastLoadedAddons?.zip(addons)?.all { (a, b) -> a.id == b.id && a.version == b.version } == true

                            if (isSame && _state.value.dynamicSections.isNotEmpty()) {
                                return@collect
                            }

                            lastLoadedAddons = addons

                            if (addons.isNotEmpty()) {
                                loadDynamicCatalogs(addons)
                            } else {
                                _state.value = _state.value.copy(
                                    dynamicSections = emptyList(),
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
                                                    lastWatchedAt = history.lastWatchedAt,
                                                )
                                            } else {
                                                null
                                            }
                                        }.sortedByDescending { it.lastWatchedAt },
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
                    _state.value = _state.value.copy(isRefreshing = true)
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
                            isWatched = true,
                        ),
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
                            var loadedAny = false
                            while (pendingCatalogs.isNotEmpty() && !loadedAny) {
                                val prevCount = _state.value.dynamicSections.size
                                loadNextCatalogPage()
                                if (_state.value.dynamicSections.size > prevCount) {
                                    loadedAny = true
                                }
                            }
                        } catch (e: Exception) {
                            // Ignore — individual requests are already caught internally
                        } finally {
                            _state.value = _state.value.copy(
                                isLoadingMore = false,
                                hasMoreSections = pendingCatalogs.isNotEmpty(),
                            )
                        }
                    }
                }
            }

            is HomeAction.PrewarmCatalogItem -> prewarmStreams(action.mediaId, action.type, action.addonId)
        }
    }

    /**
     * Fire-and-forget stream pre-warming when a catalog item is hovered for
     * [MovieHubDimens.PrefetchTiming.catalogItemHoverMs]. The fetched stream data is silently
     * discarded — but DNS and TCP connection caches will be warm when the user taps.
     */
    private fun prewarmStreams(mediaId: String, type: String, addonId: String?) {
        val client = stremioApiClient ?: return
        viewModelScope.launch {
            try {
                val addons = addonManager.installedAddons.value
                val targetAddons = if (addonId != null) addons.filter { it.id == addonId } else addons
                supervisorScope {
                    targetAddons.take(3).map { manifest ->
                        async {
                            val addonUrl = addonManager.getAddonUrl(manifest.id) ?: return@async
                            try {
                                client.getStreams(
                                    baseUrl = addonUrl,
                                    type = type,
                                    id = mediaId,
                                    addonName = manifest.name,
                                    addonId = manifest.id,
                                )
                            } catch (_: Exception) { /* silent — best effort */
                            }
                        }
                    }.awaitAll()
                }
            } catch (_: Exception) { /* silent — best effort */
            }
        }
    }

    private suspend fun loadDynamicCatalogs(addons: List<StremioManifest>) {
        PerformanceMonitor.beginSection("HomeVM.loadCatalogs")
        PerformanceMonitor.counter("HomeVM.catalogCount", addons.sumOf { it.catalogs.size }.toLong())
        try {
            // Show shimmer immediately — prevents empty-state text from flashing
            // during the gap between addon detection and cache query completion
            _state.value = _state.value.copy(isLoading = true)

            // Group catalogs by name and type across all addons to ensure complete pre-consolidation
            val allCatalogs = addons.flatMap { manifest ->
                manifest.catalogs.map { catalog ->
                    val name = catalog.name ?: catalog.id.replaceFirstChar { it.uppercase() }
                    Triple(
                        catalog.type,
                        name,
                        CatalogSource(
                            addonId = manifest.id,
                            addonName = manifest.name,
                            catalogId = catalog.id,
                        )
                    )
                }
            }

            pendingCatalogs = allCatalogs.groupBy { Pair(it.first, it.second.trim()) }
                .map { (key, list) ->
                    PendingCatalog(
                        type = key.first,
                        catalogName = key.second,
                        sources = list.map { it.third }.distinctBy { "${it.addonId}_${it.catalogId}" }
                    )
                }

            // ═══════════════════════════════════════════
            // PHASE 1: Load first page from cache only (fast, no network)
            // Only loads pageSize catalogs — avoids rendering too many sections at once
            // ═══════════════════════════════════════════
            val firstPage = pendingCatalogs.take(pageSize)

            val cachedSections = supervisorScope {
                firstPage.map { pending ->
                    async {
                        try {
                            val deferredItems = pending.sources.map { source ->
                                async {
                                    try {
                                        val addon = addons.find { it.id == source.addonId } ?: return@async emptyList<com.moviehub.core.model.MediaItem>()
                                        repository.getCachedCatalogs(
                                            addons = listOf(addon),
                                            type = pending.type,
                                            catalogId = source.catalogId,
                                        ).take(10)
                                    } catch (e: Exception) {
                                        emptyList()
                                    }
                                }
                            }
                            val combinedLists = deferredItems.awaitAll()

                            // Interleave cached items across sources
                            val interleaved = mutableListOf<com.moviehub.core.model.MediaItem>()
                            var index = 0
                            while (true) {
                                var addedAny = false
                                pending.sources.forEachIndexed { sourceIdx, _ ->
                                    val itemsFromSource = combinedLists[sourceIdx]
                                    if (index < itemsFromSource.size) {
                                        interleaved.add(itemsFromSource[index])
                                        addedAny = true
                                    }
                                }
                                if (!addedAny) break
                                index++
                            }
                            val items = interleaved.distinctBy { it.id }

                            if (items.isNotEmpty()) {
                                val uniqueAddons = pending.sources.map { it.addonName }.distinct()
                                CatalogSection(
                                    addonId = if (uniqueAddons.size == 1) pending.sources.first().addonId else "multi_addons",
                                    addonName = if (uniqueAddons.size == 1) uniqueAddons.first() else "Multiple Providers",
                                    catalogId = if (uniqueAddons.size == 1) pending.sources.first().catalogId else "merged_${pending.sources.first().catalogId}",
                                    catalogName = pending.catalogName,
                                    type = pending.type,
                                    items = items,
                                )
                            } else {
                                null
                            }
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
                    isLoading = false,
                    isRefreshing = true, // cache shown, network refresh in background
                )
            } else {
                // Keep isRefreshing unchanged — if user pulled to refresh, preserve the spinner
                _state.value = _state.value.copy(isLoading = true)
            }

            // ═══════════════════════════════════════════
            // PHASE 2: Load first page from network
            // Subsequent pages load as user scrolls (via LoadMore action)
            // Loop until we have at least one dynamic section to display on the screen
            // ═══════════════════════════════════════════
            var loadedAny = _state.value.dynamicSections.isNotEmpty()
            while (pendingCatalogs.isNotEmpty() && !loadedAny) {
                val prevCount = _state.value.dynamicSections.size
                loadNextCatalogPage()
                if (_state.value.dynamicSections.size > prevCount) {
                    loadedAny = true
                }
            }

            // Re-consolidate after network data arrives to populate featuredItems
            // if Phase 1 had empty cache
            if (_state.value.featuredItems.isEmpty() && _state.value.dynamicSections.isNotEmpty()) {
                val netConsolidated = consolidateSections(_state.value.dynamicSections)
                _state.value = _state.value.copy(featuredItems = netConsolidated.first)
            }

            _state.value = _state.value.copy(
                isLoading = false,
                isRefreshing = false,
                hasMoreSections = pendingCatalogs.isNotEmpty(),
            )
        } catch (e: Exception) {
            _state.value = _state.value.copy(
                isLoading = false,
                isRefreshing = false,
                hasMoreSections = false,
                error = if (_state.value.dynamicSections.isEmpty()) e.message ?: "Failed to load catalogs" else null,
            )
        } finally {
            PerformanceMonitor.endSection()
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
                        val deferredItems = pending.sources.map { source ->
                            async {
                                try {
                                    repository.refreshCatalogFromNetwork(
                                        type = pending.type,
                                        catalogId = source.catalogId,
                                        addonId = source.addonId,
                                    ).take(10)
                                } catch (e: Exception) {
                                    emptyList()
                                }
                            }
                        }
                        val combinedLists = deferredItems.awaitAll()

                        // Interleave network items across sources
                        val interleaved = mutableListOf<com.moviehub.core.model.MediaItem>()
                        var index = 0
                        while (true) {
                            var addedAny = false
                            pending.sources.forEachIndexed { sourceIdx, _ ->
                                val itemsFromSource = combinedLists[sourceIdx]
                                if (index < itemsFromSource.size) {
                                    interleaved.add(itemsFromSource[index])
                                    addedAny = true
                                }
                            }
                            if (!addedAny) break
                            index++
                        }
                        val items = interleaved.distinctBy { it.id }

                        if (items.isNotEmpty()) {
                            val uniqueAddons = pending.sources.map { it.addonName }.distinct()
                            CatalogSection(
                                addonId = if (uniqueAddons.size == 1) pending.sources.first().addonId else "multi_addons",
                                addonName = if (uniqueAddons.size == 1) uniqueAddons.first() else "Multiple Providers",
                                catalogId = if (uniqueAddons.size == 1) pending.sources.first().catalogId else "merged_${pending.sources.first().catalogId}",
                                catalogName = pending.catalogName,
                                type = pending.type,
                                items = items,
                            )
                        } else {
                            null
                        }
                    } catch (e: Exception) {
                        null
                    }
                }
            }.awaitAll().filterNotNull()
        }

        if (networkSections.isNotEmpty()) {
            val currentSections = _state.value.dynamicSections.toMutableList()

            networkSections.forEach { section ->
                val key = "${section.catalogName.trim().lowercase()}_${section.type}"
                val existingIndex = currentSections.indexOfFirst {
                    "${it.catalogName.trim().lowercase()}_${it.type}" == key
                }
                if (existingIndex >= 0) {
                    // Update existing section with fresh network items if they are not empty (replacing cached items)
                    if (section.items.isNotEmpty()) {
                        val existing = currentSections[existingIndex]
                        val isMultiAddon = existing.addonId != section.addonId || existing.addonId == "multi_addons"
                        currentSections[existingIndex] = existing.copy(
                            items = section.items,
                            addonId = if (isMultiAddon) "multi_addons" else existing.addonId,
                            addonName = if (isMultiAddon) "Multiple Providers" else existing.addonName,
                        )
                    }
                } else {
                    // Append completely new section
                    currentSections.add(section)
                }
            }

            val consolidated = consolidateSections(currentSections)
            val updatedSections = consolidated.second

            // Cap at 30 to bound memory growth
            _state.value = _state.value.copy(
                dynamicSections = if (updatedSections.size > 30) updatedSections.take(30) else updatedSections,
                featuredItems = if (_state.value.featuredItems.isEmpty()) consolidated.first else _state.value.featuredItems
            )
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
        val featured = featuredSection?.items?.take(10) ?: emptyList()
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
                items = deduplicatedItems,
            )
        }.filter { it.items.isNotEmpty() }

        val sorted = consolidated.sortedWith(compareBy({ it.type }, { it.catalogName }))
        return Pair(featured, sorted)
    }
}
