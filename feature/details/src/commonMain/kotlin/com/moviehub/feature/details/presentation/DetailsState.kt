package com.moviehub.feature.details.presentation

import com.moviehub.core.model.MediaItem
import com.moviehub.core.model.StreamItem
import com.moviehub.core.model.TrailerPlaybackSource
import androidx.compose.runtime.Immutable

@Immutable
data class DetailsState(
    val isLoading: Boolean = false,
    val isSearchingStreams: Boolean = false,
    val isResolvingTrailer: Boolean = false,
    val totalStreamAddons: Int = 0,
    val processedStreamAddons: Int = 0,
    val mediaItem: MediaItem? = null,
    val streams: List<StreamItem> = emptyList(),
    val selectedTrailerSource: TrailerPlaybackSource? = null,
    val error: String? = null
)

sealed interface DetailsAction {
    data class LoadDetails(val id: String, val type: String, val addonUrl: String? = null) : DetailsAction
    data class LoadStreams(val id: String, val type: String) : DetailsAction
    data class LoadTrailer(val videoId: String) : DetailsAction
    data object ClearTrailer : DetailsAction
    data class PlayStream(val stream: StreamItem) : DetailsAction
}
