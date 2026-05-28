package com.moviehub.feature.details.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moviehub.core.database.FavoriteDao
import com.moviehub.core.database.FavoriteEntity
import com.moviehub.core.database.ContentType as DbContentType
import com.moviehub.core.database.ProfileRepository
import com.moviehub.core.database.TmdbSettingsRepository
import com.moviehub.core.database.WatchProgress
import com.moviehub.core.database.WatchProgressDao
import com.moviehub.feature.details.data.DetailsRepository
import com.moviehub.core.network.YouTubePlaybackResolver
import com.moviehub.core.network.tmdb.TmdbService
import com.moviehub.core.utils.parseRuntime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import com.moviehub.core.utils.PerformanceMonitor

class DetailsViewModel(
    private val repository: DetailsRepository,
    private val ytResolver: YouTubePlaybackResolver,
    private val favoriteDao: FavoriteDao,
    private val watchProgressDao: WatchProgressDao,
    private val profileRepository: ProfileRepository,
    private val tmdbService: TmdbService,
    private val tmdbSettingsRepository: TmdbSettingsRepository,
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
            is DetailsAction.ToggleFavorite -> toggleFavorite()
            is DetailsAction.ToggleWatched -> toggleWatched()
            is DetailsAction.RefreshLocalState -> refreshLocalState()
        }
    }

    private fun loadDetails(id: String, type: String, addonUrl: String?) {
        if (_state.value.mediaItem?.id == id && !_state.value.isLoading && _state.value.error == null) {
            return
        }
        viewModelScope.launch {
            PerformanceMonitor.beginSection("VM:Details:loadDetails")
            try {
                _state.value = _state.value.copy(
                    isLoading = true,
                    error = null,
                )

                try {
                    // Sync TMDB API key before loading
                    val tmdbKey = tmdbSettingsRepository.getApiKey()
                    val isTmdbConfig = tmdbKey.isNotBlank()
                    if (isTmdbConfig) {
                        tmdbService.setApiKey(tmdbKey)
                    }
                    _state.value = _state.value.copy(isTmdbConfigured = isTmdbConfig)

                    // 1. Fetch Metadata
                    val details = repository.getMediaDetails(id, type, addonUrl)

                    _state.value = _state.value.copy(
                        isLoading = false,
                        mediaItem = details,
                        error = if (details == null) "Failed to load details" else null
                    )

                    if (details != null) {
                        val profileId = profileRepository.activeProfile.value?.id
                        if (profileId != null) {
                            try {
                                val isFav = favoriteDao.getFavoriteById(id, profileId)
                                val progress = watchProgressDao.getProgress(id, profileId).firstOrNull()
                                val percent = if (progress != null && progress.durationMs > 0) {
                                    (progress.progressMs.toFloat() / progress.durationMs).coerceIn(0f, 1f)
                                } else 0f
                                _state.value = _state.value.copy(
                                    isFavorite = isFav != null,
                                    isWatched = progress?.isWatched == true,
                                    isInProgress = progress != null && !progress.isWatched && progress.progressMs > 0,
                                    watchProgressPercent = percent,
                                )
                            } catch (e: Exception) {
                                // Non-critical: favorite/progress lookup failure shouldn't block details
                            }
                        }

                        // 2. Fallback trailer search if empty
                        if (details.trailers.isEmpty()) {
                            launch {
                                try {
                                    val searchTitle = details.title
                                    val year = details.releaseInfo?.take(4) ?: ""
                                    val query = "$searchTitle $year official trailer"
                                    val ytId = ytResolver.searchTrailer(query)
                                    if (ytId != null) {
                                        val currentMediaItem = _state.value.mediaItem
                                        if (currentMediaItem != null && currentMediaItem.id == details.id) {
                                            _state.value = _state.value.copy(
                                                mediaItem = currentMediaItem.copy(
                                                    trailers = listOf(
                                                        com.moviehub.core.model.MediaTrailer(
                                                            id = ytId,
                                                            url = ytId,
                                                            name = "Official Trailer",
                                                            type = "Trailer"
                                                        )
                                                    )
                                                )
                                            )
                                        }
                                    }
                                } catch (e: Exception) {
                                    // Non-critical: trailer search failure is fine
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "Failed to load details: ${e.message}"
                    )
                }
            } finally {
                PerformanceMonitor.endSection()
            }
        }
    }

    private fun loadStreams(id: String, type: String) {
        viewModelScope.launch {
            PerformanceMonitor.beginSection("VM:Details:loadStreams")
            try {
                val totalAddons = repository.getStreamAddonCount(type)
                _state.value = _state.value.copy(
                    isSearchingStreams = true,
                    totalStreamAddons = totalAddons,
                    processedStreamAddons = 0
                )

                try {
                    var processedCount = 0
                    repository.getStreamsFlow(id, type).collect { partialStreams ->
                        processedCount++
                        val sorted = partialStreams.sortedByDescending { it.playbackPriority }
                        _state.value = _state.value.copy(
                            streams = sorted,
                            processedStreamAddons = processedCount.coerceAtMost(totalAddons)
                        )
                    }
                } catch (e: Exception) {
                    _state.value = _state.value.copy(streamsError = e.message)
                } finally {
                    _state.value = _state.value.copy(isSearchingStreams = false)
                }
            } finally {
                PerformanceMonitor.endSection()
            }
        }
    }

    private fun loadTrailer(videoId: String) {
        viewModelScope.launch {
            PerformanceMonitor.beginSection("VM:Details:loadTrailer")
            try {
                _state.value = _state.value.copy(isResolvingTrailer = true)
                val source = ytResolver.resolveFromYouTubeId(videoId)
                _state.value = _state.value.copy(
                    isResolvingTrailer = false,
                    selectedTrailerSource = source
                )
            } finally {
                PerformanceMonitor.endSection()
            }
        }
    }

    /**
     * Refreshes only local DB-backed state (favorite, watched) without any network calls.
     * Called when the screen resumes from the backstack.
     */
    private fun refreshLocalState() {
        viewModelScope.launch {
            PerformanceMonitor.beginSection("VM:Details:refreshLocalState")
            try {
                val mediaItem = _state.value.mediaItem ?: return@launch
                val profileId = profileRepository.activeProfile.value?.id ?: return@launch
                try {
                    val isFav = favoriteDao.getFavoriteById(mediaItem.id, profileId)
                    val progress = watchProgressDao.getProgress(mediaItem.id, profileId).firstOrNull()
                    val percent = if (progress != null && progress.durationMs > 0) {
                        (progress.progressMs.toFloat() / progress.durationMs).coerceIn(0f, 1f)
                    } else 0f
                    _state.value = _state.value.copy(
                        isFavorite = isFav != null,
                        isWatched = progress?.isWatched == true,
                        isInProgress = progress != null && !progress.isWatched && progress.progressMs > 0,
                        watchProgressPercent = percent,
                    )
                } catch (e: Exception) { }
            } finally {
                PerformanceMonitor.endSection()
            }
        }
    }

    private fun toggleWatched() {
        viewModelScope.launch {
            PerformanceMonitor.beginSection("VM:Details:toggleWatched")
            try {
                val mediaItem = _state.value.mediaItem ?: return@launch
                val profileId = profileRepository.activeProfile.value?.id ?: return@launch
                val current = _state.value.isWatched

                if (current) {
                    watchProgressDao.deleteProgress(mediaItem.id, profileId)
                    _state.value = _state.value.copy(isWatched = false, isInProgress = false, watchProgressPercent = 0f)
                } else {
                    watchProgressDao.deleteProgress(mediaItem.id, profileId)
                    watchProgressDao.insertOrUpdate(
                        WatchProgress(
                            mediaId = mediaItem.id,
                            profileId = profileId,
                            type = if (mediaItem.type.name == "SHOW") "series" else "movie",
                            progressMs = 0,
                            durationMs = mediaItem.runtime?.let { parseRuntime(it) } ?: 0L,
                            isWatched = true
                        )
                    )
                    _state.value = _state.value.copy(isWatched = true, isInProgress = false, watchProgressPercent = 0f)
                }
            } finally {
                PerformanceMonitor.endSection()
            }
        }
    }

    private fun toggleFavorite() {
        viewModelScope.launch {
            PerformanceMonitor.beginSection("VM:Details:toggleFavorite")
            try {
                val mediaItem = _state.value.mediaItem ?: return@launch
                val profileId = profileRepository.activeProfile.value?.id ?: return@launch
                val current = _state.value.isFavorite

                if (current) {
                    favoriteDao.deleteFavoriteById(mediaItem.id, profileId)
                    _state.value = _state.value.copy(isFavorite = false)
                } else {
                    favoriteDao.insertFavorite(
                        FavoriteEntity(
                            contentId = mediaItem.id,
                            profileId = profileId,
                            contentType = if (mediaItem.type.name == "SHOW") DbContentType.SHOW else DbContentType.MOVIE,
                            title = mediaItem.title,
                            posterUrl = mediaItem.posterUrl,
                        )
                    )
                    _state.value = _state.value.copy(isFavorite = true)
                }
            } finally {
                PerformanceMonitor.endSection()
            }
        }
    }
}
