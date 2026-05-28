package com.moviehub.feature.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moviehub.core.database.ProfileRepository
import com.moviehub.core.database.WatchProgressDao
import com.moviehub.feature.home.data.HomeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.moviehub.core.utils.PerformanceMonitor

class CatalogViewModel(
    private val repository: HomeRepository,
    private val watchProgressDao: WatchProgressDao,
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(CatalogState())
    val state: StateFlow<CatalogState> = _state.asStateFlow()

    private var allFetchedItems: List<com.moviehub.core.model.MediaItem> = emptyList()
    private val pageSize = 15

    init {
        observeWatchedState()
    }

    private fun observeWatchedState() {
        viewModelScope.launch {
            try {
                profileRepository.activeProfile.collectLatest { profile ->
                    if (profile != null) {
                        try {
                            watchProgressDao.getWatchedMediaIds(profile.id).collectLatest { ids ->
                                _state.value = _state.value.copy(watchedMediaIds = ids.toSet())
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

    fun loadCatalog(type: String, catalogId: String, addonId: String?) {
        viewModelScope.launch {
            PerformanceMonitor.beginSection("VM:Catalog:loadCatalog")
            try {
                _state.value = _state.value.copy(isLoading = true, error = null)
                try {
                    val fetched = repository.getCatalog(type, catalogId, addonId, skip = 0)
                    allFetchedItems = fetched
                    _state.value = _state.value.copy(
                        isLoading = false,
                        displayedItems = fetched.take(pageSize),
                        canPaginate = fetched.isNotEmpty()
                    )
                } catch (e: Exception) {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load catalog"
                    )
                }
            } finally {
                PerformanceMonitor.endSection()
            }
        }
    }

    fun loadMore(type: String, catalogId: String, addonId: String?) {
        val currentState = _state.value
        if (currentState.isPaginating || !currentState.canPaginate) return

        _state.value = currentState.copy(isPaginating = true)

        viewModelScope.launch {
            PerformanceMonitor.beginSection("VM:Catalog:loadMore")
            try {
                if (_state.value.displayedItems.size < allFetchedItems.size) {
                    val nextSize = (_state.value.displayedItems.size + pageSize).coerceAtMost(allFetchedItems.size)
                    _state.value = _state.value.copy(
                        displayedItems = allFetchedItems.take(nextSize),
                        isPaginating = false
                    )
                } else {
                    val nextSkip = allFetchedItems.size
                    val newItems = repository.getCatalog(type, catalogId, addonId, skip = nextSkip)

                    val existingIds = allFetchedItems.map { it.id }.toSet()
                    val uniqueNewItems = newItems.filter { it.id !in existingIds }

                    if (uniqueNewItems.isNotEmpty()) {
                        allFetchedItems = allFetchedItems + uniqueNewItems
                        val nextSize = _state.value.displayedItems.size + pageSize
                        _state.value = _state.value.copy(
                            displayedItems = allFetchedItems.take(nextSize),
                            isPaginating = false
                        )
                    } else {
                        _state.value = _state.value.copy(
                            canPaginate = false,
                            isPaginating = false
                        )
                    }
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isPaginating = false,
                    canPaginate = false,
                    error = e.message ?: "Failed to load more items"
                )
            } finally {
                PerformanceMonitor.endSection()
            }
        }
    }
}
