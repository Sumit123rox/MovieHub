package com.moviehub.feature.search.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moviehub.core.database.ProfileRepository
import com.moviehub.core.database.SearchHistoryDao
import com.moviehub.core.database.SearchHistoryEntity
import com.moviehub.feature.search.data.SearchRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class SearchViewModel(
    private val repository: SearchRepository,
    private val searchHistoryDao: SearchHistoryDao,
    private val profileRepository: ProfileRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(SearchState())
    val state: StateFlow<SearchState> = _state.asStateFlow()

    init {
        loadRecentSearches()
    }

    fun onAction(action: SearchAction) {
        when (action) {
            is SearchAction.QueryChanged -> {
                _state.value = _state.value.copy(query = action.query)
            }
            is SearchAction.PerformSearch -> {
                performSearch()
            }
            is SearchAction.ClearSearchHistory -> clearHistory()
            is SearchAction.RemoveSearch -> removeSearch(action.query)
            is SearchAction.SelectRecentSearch -> {
                _state.value = _state.value.copy(query = action.query)
                performSearch()
            }
        }
    }

    private fun loadRecentSearches() {
        viewModelScope.launch {
            try {
                val profileId = profileRepository.activeProfile.value?.id ?: return@launch
                try {
                    searchHistoryDao.getRecentSearches(profileId).collect { history ->
                        _state.value = _state.value.copy(
                            recentSearches = history.map { it.query }
                        )
                    }
                } catch (e: Exception) {
                    _state.value = _state.value.copy(recentSearches = emptyList())
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(recentSearches = emptyList())
            }
        }
    }

    private fun performSearch() {
        val currentQuery = _state.value.query
        if (currentQuery.isBlank()) return

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            // Save to search history
            val profileId = profileRepository.activeProfile.value?.id
            if (profileId != null) {
                searchHistoryDao.insertSearch(
                    SearchHistoryEntity(query = currentQuery, profileId = profileId)
                )
            }

            val results = repository.searchMovies(currentQuery)
            _state.value = _state.value.copy(
                isLoading = false,
                results = results,
                error = if (results.isEmpty()) "No results found for '$currentQuery'" else null
            )
        }
    }

    private fun clearHistory() {
        viewModelScope.launch {
            val profileId = profileRepository.activeProfile.value?.id ?: return@launch
            searchHistoryDao.clearSearchHistory(profileId)
        }
    }

    private fun removeSearch(query: String) {
        viewModelScope.launch {
            val profileId = profileRepository.activeProfile.value?.id ?: return@launch
            searchHistoryDao.deleteSearch(query, profileId)
        }
    }
}
