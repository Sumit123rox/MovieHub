package com.moviehub.feature.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moviehub.feature.home.data.HomeRepository
import com.moviehub.core.network.AddonManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class HomeViewModel(
    private val repository: HomeRepository,
    private val addonManager: AddonManager
) : ViewModel() {
    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    private val _event = MutableSharedFlow<HomeEvent>()
    val event: SharedFlow<HomeEvent> = _event.asSharedFlow()

    private val sectionsMutex = Mutex()

    init {
        viewModelScope.launch {
            addonManager.installedAddons.collectLatest { addons ->
                _state.value = _state.value.copy(installedAddons = addons)
                
                if (addons.isNotEmpty()) {
                    loadDynamicCatalogs(addons)
                } else {
                    _state.value = _state.value.copy(
                        dynamicSections = emptyList(),
                        activeAddonId = null,
                        activeAddonName = null
                    )
                }
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

    private suspend fun loadDynamicCatalogs(addons: List<com.moviehub.core.model.StremioManifest>) = coroutineScope {
        _state.value = _state.value.copy(isLoading = true, dynamicSections = emptyList(), featuredItems = emptyList())
        
        val activeSections = mutableListOf<CatalogSection>()
        var featuredItemsCaptured = false
        
        addons.forEach { manifest ->
            manifest.catalogs.forEach { catalog ->
                launch {
                    try {
                        val items = repository.getCatalog(
                            type = catalog.type,
                            catalogId = catalog.id,
                            addonId = manifest.id
                        ).take(20)
                        
                        if (items.isNotEmpty()) {
                            sectionsMutex.withLock {
                                // 1. Capture Featured Items ONLY ONCE for the Hero
                                if (!featuredItemsCaptured && items.size >= 5) {
                                    _state.value = _state.value.copy(featuredItems = items.take(5))
                                    featuredItemsCaptured = true
                                }

                                // 2. Add to Dynamic Sections, potentially filtering out hero items if they overlap
                                val section = CatalogSection(
                                    addonId = manifest.id,
                                    addonName = manifest.name,
                                    catalogId = catalog.id,
                                    catalogName = catalog.name ?: catalog.id.replaceFirstChar { it.uppercase() },
                                    type = catalog.type,
                                    items = if (featuredItemsCaptured && _state.value.featuredItems.any { h -> items.any { i -> i.id == h.id } }) {
                                        // If this section contains hero items, we skip the first few or just show it as is
                                        // User said "showing same data on two places is not good", let's filter
                                        items.filter { item -> _state.value.featuredItems.none { it.id == item.id } }
                                    } else items
                                )
                                
                                if (section.items.isNotEmpty()) {
                                    activeSections.add(section)
                                    // Sort by addon name and then catalog name for a stable premium look
                                    _state.value = _state.value.copy(
                                        dynamicSections = activeSections.toList().sortedWith(
                                            compareBy({ it.addonName }, { it.catalogName })
                                        ),
                                        isLoading = false
                                    )
                                }
                            }
                        }
                    } catch (e: Exception) {
                        // Log catalog failure but don't crash
                    }
                }
            }
        }
    }
}
