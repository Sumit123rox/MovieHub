package com.moviehub.feature.details.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moviehub.feature.details.data.DetailsRepository
import com.moviehub.core.network.YouTubePlaybackResolver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DetailsViewModel(
    private val repository: DetailsRepository,
    private val ytResolver: YouTubePlaybackResolver
) : ViewModel() {

    private val _state = MutableStateFlow(DetailsState())
    val state: StateFlow<DetailsState> = _state.asStateFlow()

    fun onAction(action: DetailsAction) {
        when (action) {
            is DetailsAction.LoadDetails -> loadDetails(action.id, action.type, action.addonUrl)
            is DetailsAction.LoadStreams -> loadStreams(action.id, action.type)
            is DetailsAction.LoadTrailer -> loadTrailer(action.videoId)
            is DetailsAction.ClearTrailer -> _state.value = _state.value.copy(selectedTrailerSource = null)
            is DetailsAction.PlayStream -> {
                // Handled in UI navigation
            }
        }
    }

    private fun loadDetails(id: String, type: String, addonUrl: String?) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null, streams = emptyList(), isSearchingStreams = false)
            
            // 1. Fetch Metadata
            val details = repository.getMediaDetails(id, type, addonUrl)
            
            _state.value = _state.value.copy(
                isLoading = false,
                mediaItem = details,
                error = if (details == null) "Failed to load details" else null
            )

            if (details != null && type.lowercase() == "movie") {
                // 2. Fetch Streams ONLY for movies by default
                loadStreams(id, type)
            }
        }
    }

    private fun loadStreams(id: String, type: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                isSearchingStreams = true,
                totalStreamAddons = 1,
                processedStreamAddons = 0
            )

            try {
                val results = repository.getStreams(id, type)
                _state.value = _state.value.copy(
                    processedStreamAddons = 1,
                    streams = results.sortedByDescending { it.name?.contains("4K", ignoreCase = true) == true }
                )
            } catch (e: Exception) {
                // Ignore failure
            } finally {
                _state.value = _state.value.copy(isSearchingStreams = false)
            }
        }
    }

    private fun loadTrailer(videoId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isResolvingTrailer = true)
            val source = ytResolver.resolveFromYouTubeId(videoId)
            _state.value = _state.value.copy(
                isResolvingTrailer = false,
                selectedTrailerSource = source
            )
        }
    }
}
