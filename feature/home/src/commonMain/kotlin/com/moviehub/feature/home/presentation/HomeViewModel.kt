package com.moviehub.feature.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
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
    private val logger = Logger.withTag("HomeViewModel")
    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    private val pageSize = 5
    private var pendingCatalogs: List<PendingCatalog> = emptyList()
    private var isPageLoading = false

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
                                logger.d { "init collect: addons empty, clearing sections (isLoading=${_state.value.isLoading})" }
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
                    if (!isPageLoading && !_state.value.isLoadingMore && pendingCatalogs.isNotEmpty()) {
                        isPageLoading = true
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
                            logger.w(e) { "LoadMore page failed: ${e.message?.take(80)}" }
                        } finally {
                            isPageLoading = false
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
        logger.i { "loadDynamicCatalogs START — ${addons.size} addons, isLoading=${_state.value.isLoading}" }
        PerformanceMonitor.beginSection("HomeVM.loadCatalogs")
        PerformanceMonitor.counter("HomeVM.catalogCount", addons.sumOf { it.catalogs.size }.toLong())
        try {
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

            val grouped = allCatalogs.groupBy { Pair(it.first, it.second.trim().lowercase()) }
            pendingCatalogs = grouped.map { (key, list) ->
                val displayName = list.map { it.second.trim() }
                    .firstOrNull { it.any { c -> c.isUpperCase() } }
                    ?: list.first().second.trim()

                PendingCatalog(
                    type = key.first,
                    catalogName = displayName,
                    sources = list.map { it.third }.distinctBy { "${it.addonId}_${it.catalogId}" }
                )
            }.sortedWith(compareBy({ it.type }, { it.catalogName }))

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
                val featured = cachedConsolidated.first.ifEmpty {
                    cachedConsolidated.second.firstOrNull()?.items?.take(5) ?: emptyList()
                }
                val hasData = cachedConsolidated.second.isNotEmpty()
                _state.value = _state.value.copy(
                    featuredItems = featured,
                    dynamicSections = cachedConsolidated.second,
                    isLoading = !hasData,       // stay in shimmer if cache produced nothing
                    isRefreshing = hasData,
                )
            }

            // Phase 2: Network batch — interleaves multiple addons' items
            try {
                if (pendingCatalogs.isNotEmpty()) {
                    loadNextCatalogPage()
                }
            } catch (e: Exception) {
                logger.w(e) { "Phase 2 network batch failed" }
            }

            val hasContent = _state.value.dynamicSections.isNotEmpty()
            logger.i { "loadDynamicCatalogs DONE — sections=${_state.value.dynamicSections.size}, featured=${_state.value.featuredItems.size}" }
            // Shimmer stays until we ACTUALLY have data — no empty state flash
            _state.value = _state.value.copy(
                isLoading = !hasContent,       // keep shimmer if nothing loaded
                isRefreshing = hasContent,     // subtle refresh if we have cached data
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
            var changed = false

            networkSections.forEach { section ->
                val key = "${section.catalogName.trim().lowercase()}_${section.type}"
                val existingIndex = currentSections.indexOfFirst {
                    "${it.catalogName.trim().lowercase()}_${it.type}" == key
                }
                if (existingIndex >= 0) {
                    // Section exists — interleave new items from other addons
                    // instead of appending, so all addons appear together naturally
                    val existing = currentSections[existingIndex]
                    val existingIds = existing.items.map { it.id }.toSet()
                    val newItems = section.items.filter { it.id !in existingIds }
                    if (newItems.isNotEmpty()) {
                        val interleaved = mutableListOf<com.moviehub.core.model.MediaItem>()
                        val maxLen = maxOf(existing.items.size, newItems.size)
                        for (i in 0 until maxLen) {
                            if (i < existing.items.size) interleaved.add(existing.items[i])
                            if (i < newItems.size) interleaved.add(newItems[i])
                        }
                        currentSections[existingIndex] = existing.copy(items = interleaved)
                        changed = true
                    }
                } else {
                    // Brand new section — append
                    currentSections.add(section)
                    changed = true
                }
            }

            if (changed) {
                val sections = if (currentSections.size > 30) currentSections.take(30) else currentSections
                val cons = consolidateSections(sections)
                val featured = cons.first.ifEmpty {
                    cons.second.firstOrNull()?.items?.take(5) ?: emptyList()
                }
                _state.value = _state.value.copy(
                    dynamicSections = sections,
                    featuredItems = featured,
                )
            }
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

        // Deduplicate identical catalog display names across different types (e.g. "Popular" movie vs "Popular" series)
        val nameCounts = consolidated.groupBy { it.catalogName.trim().lowercase() }
        val finalSections = consolidated.map { section ->
            val count = nameCounts[section.catalogName.trim().lowercase()]?.size ?: 0
            if (count > 1) {
                val suffix = when (section.type.lowercase()) {
                    "movie" -> " Movies"
                    "series" -> " Series"
                    "anime" -> " Anime"
                    "channel" -> " Channels"
                    else -> " ${section.type.replaceFirstChar { it.uppercase() }}"
                }
                // Avoid double appending if it already ends with the suffix
                if (!section.catalogName.endsWith(suffix, ignoreCase = true)) {
                    section.copy(catalogName = "${section.catalogName.trim()}$suffix")
                } else {
                    section
                }
            } else {
                section
            }
        }

        return Pair(featured, finalSections)
    }
}
