package com.moviehub.feature.search.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moviehub.core.database.ProfileRepository
import com.moviehub.core.database.SearchHistoryDao
import com.moviehub.core.database.SearchHistoryEntity
import com.moviehub.core.utils.PerformanceMonitor
import com.moviehub.feature.search.data.SearchRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

class SearchViewModel(
    private val repository: SearchRepository,
    private val searchHistoryDao: SearchHistoryDao,
    private val profileRepository: ProfileRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(SearchState())
    val state: StateFlow<SearchState> = _state.asStateFlow()

    private var suggestionsJob: Job? = null

    init {
        loadRecentSearches()
    }

    fun onAction(action: SearchAction) {
        when (action) {
            is SearchAction.QueryChanged -> {
                _state.value = _state.value.copy(query = action.query)
                triggerSuggestions(action.query)
            }
            is SearchAction.PerformSearch -> {
                suggestionsJob?.cancel()
                _state.value = _state.value.copy(suggestions = emptyList())
                performSearch()
            }
            is SearchAction.ClearSearchHistory -> clearHistory()
            is SearchAction.RemoveSearch -> removeSearch(action.query)
            is SearchAction.SelectRecentSearch -> {
                suggestionsJob?.cancel()
                _state.value = _state.value.copy(
                    query = action.query,
                    suggestions = emptyList(),
                )
                performSearch()
            }
            is SearchAction.SelectSuggestion -> {
                suggestionsJob?.cancel()
                _state.value = _state.value.copy(
                    query = action.query,
                    suggestions = emptyList(),
                )
                performSearch()
            }
        }
    }

    private fun triggerSuggestions(query: String) {
        suggestionsJob?.cancel()
        if (query.trim().length < 2) {
            _state.value = _state.value.copy(suggestions = emptyList())
            return
        }

        suggestionsJob = viewModelScope.launch {
            // Debounce for 300ms to avoid spamming the APIs as the user types
            delay(300)
            try {
                val list = repository.getSearchSuggestions(query)
                // Double-check the query hasn't changed under us
                if (_state.value.query == query) {
                    _state.value = _state.value.copy(suggestions = list)
                }
            } catch (_: Exception) {
                if (_state.value.query == query) {
                    _state.value = _state.value.copy(suggestions = emptyList())
                }
            }
        }
    }

    private fun loadRecentSearches() {
        viewModelScope.launch {
            PerformanceMonitor.beginSection("VM:Search:loadRecentSearches")
            try {
                try {
                    val profileId = profileRepository.activeProfile.value?.id ?: return@launch
                    try {
                        searchHistoryDao.getRecentSearches(profileId).collect { history ->
                            _state.value = _state.value.copy(
                                recentSearches = history.map { it.query },
                            )
                        }
                    } catch (e: Exception) {
                        _state.value = _state.value.copy(recentSearches = emptyList())
                    }
                } catch (e: Exception) {
                    _state.value = _state.value.copy(recentSearches = emptyList())
                }
            } finally {
                PerformanceMonitor.endSection()
            }
        }
    }

    private fun performSearch() {
        val currentQuery = _state.value.query
        if (currentQuery.isBlank()) return

        viewModelScope.launch {
            PerformanceMonitor.beginSection("VM:Search:performSearch")
            try {
                _state.value = _state.value.copy(isLoading = true, error = null)

                // Save to search history
                val profileId = profileRepository.activeProfile.value?.id
                if (profileId != null) {
                    searchHistoryDao.insertSearch(
                        SearchHistoryEntity(query = currentQuery, profileId = profileId),
                    )
                }

                val results = withTimeout(15_000) { repository.searchMovies(currentQuery) }
                _state.value = _state.value.copy(
                    isLoading = false,
                    results = results,
                    error = if (results.isEmpty()) "No results found for '$currentQuery'" else null,
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Search failed",
                )
            } finally {
                PerformanceMonitor.endSection()
            }
        }
    }

    private fun clearHistory() {
        viewModelScope.launch {
            PerformanceMonitor.beginSection("VM:Search:clearHistory")
            try {
                val profileId = profileRepository.activeProfile.value?.id ?: return@launch
                searchHistoryDao.clearSearchHistory(profileId)
            } finally {
                PerformanceMonitor.endSection()
            }
        }
    }

    private fun removeSearch(query: String) {
        viewModelScope.launch {
            PerformanceMonitor.beginSection("VM:Search:removeSearch")
            try {
                val profileId = profileRepository.activeProfile.value?.id ?: return@launch
                searchHistoryDao.deleteSearch(query, profileId)
            } finally {
                PerformanceMonitor.endSection()
            }
        }
    }
}
