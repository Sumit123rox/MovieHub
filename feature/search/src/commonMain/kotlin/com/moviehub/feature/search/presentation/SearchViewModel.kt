package com.moviehub.feature.search.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moviehub.feature.search.data.SearchRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SearchViewModel(private val repository: SearchRepository) : ViewModel() {
    private val _state = MutableStateFlow(SearchState())
    val state: StateFlow<SearchState> = _state.asStateFlow()

    fun onAction(action: SearchAction) {
        when (action) {
            is SearchAction.QueryChanged -> {
                _state.value = _state.value.copy(query = action.query)
            }
            is SearchAction.PerformSearch -> {
                performSearch()
            }
        }
    }

    private fun performSearch() {
        val currentQuery = _state.value.query
        if (currentQuery.isBlank()) return

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val results = repository.searchMovies(currentQuery)
            _state.value = _state.value.copy(
                isLoading = false,
                results = results,
                error = if (results.isEmpty()) "No results found for '$currentQuery'" else null
            )
        }
    }
}
