package com.moviehub.feature.details.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.moviehub.core.database.FavoriteDao
import com.moviehub.core.database.FavoriteEntity
import com.moviehub.core.database.ProfileRepository
import com.moviehub.core.database.TmdbSettingsRepository
import com.moviehub.core.database.WatchProgress
import com.moviehub.core.database.WatchProgressDao
import com.moviehub.core.network.YouTubePlaybackResolver
import com.moviehub.core.network.tmdb.TmdbService
import com.moviehub.core.utils.PerformanceMonitor
import com.moviehub.core.utils.parseRuntime
import com.moviehub.feature.details.data.DetailsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import com.moviehub.core.database.ContentType as DbContentType

class DetailsViewModel(
    private val repository: DetailsRepository,
    private val ytResolver: YouTubePlaybackResolver,
    private val favoriteDao: FavoriteDao,
    private val watchProgressDao: WatchProgressDao,
    private val profileRepository: ProfileRepository,
    private val tmdbService: TmdbService,
    private val tmdbSettingsRepository: TmdbSettingsRepository,
) : ViewModel() {

    private val logger = Logger.withTag("DetailsViewModel")
    private val _state = MutableStateFlow(DetailsState())
    val state: StateFlow<DetailsState> = _state.asStateFlow()
    private var streamsJob: kotlinx.coroutines.Job? = null

    // Throttle StreamsUpdated to max 4 updates/sec — prevents 95+ recompositions
    private var lastStreamsEmitMs = 0L
    private val streamsThrottleMs = 250L

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
        val cleanId = id.removePrefix("tmdb:").removePrefix("series:").removePrefix("movie:").removePrefix("tv:")
        val currentItem = _state.value.mediaItem
        if (currentItem != null && !_state.value.isLoading && _state.value.error == null) {
            val currentCleanId = currentItem.id.removePrefix("tmdb:").removePrefix("series:").removePrefix("movie:").removePrefix("tv:")
            val matchesId = currentCleanId == cleanId || currentItem.imdbId == cleanId || currentItem.imdbId == id
            if (matchesId) {
                return
            }
        }

        // Reset state for new content (don't wipe currentStreamsId — loadStreams needs it for idempotency)
        // currentStreamsId is managed exclusively by loadStreams
        _state.value = DetailsState()

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
                        error = if (details == null) "Failed to load details" else null,
                    )

                    if (details != null) {
                        val profileId = profileRepository.activeProfile.value?.id
                        if (profileId != null) {
                            try {
                                val isFav = favoriteDao.getFavoriteById(id, profileId)
                                val progress = watchProgressDao.getProgress(id, profileId).firstOrNull()
                                val percent = if (progress != null && progress.durationMs > 0) {
                                    (progress.progressMs.toFloat() / progress.durationMs).coerceIn(0f, 1f)
                                } else {
                                    0f
                                }
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
                                                            type = "Trailer",
                                                        ),
                                                    ),
                                                ),
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
                        error = "Failed to load details: ${e.message}",
                    )
                }
            } finally {
                PerformanceMonitor.endSection()
            }
        }
    }

    private var currentStreamsId: String? = null

    private fun loadStreams(id: String, type: String) {
        if (currentStreamsId == id && _state.value.streams.isNotEmpty() && !_state.value.isSearchingStreams) {
            return
        }
        currentStreamsId = id
        streamsJob?.cancel()
        // Set searching state SYNCHRONOUSLY to prevent the UI from briefly showing
        // "No streams found" before the coroutine starts processing
        _state.value = _state.value.copy(
            isSearchingStreams = true,
            streamsError = null,
            streams = emptyList(),
            addonStreamStatuses = emptyMap(),
        )
        streamsJob = viewModelScope.launch {
            PerformanceMonitor.beginSection("VM:Details:loadStreams")
            try {
                val totalAddons = repository.getStreamAddonCount(type)
                _state.value = _state.value.copy(
                    totalStreamAddons = totalAddons,
                    processedStreamAddons = 0,
                )

                // No safety timeout — repository handles completion via guaranteed finally block
                val safetyJob: kotlinx.coroutines.Job = kotlinx.coroutines.Job().apply { complete() }

                try {
                    repository.getStreamsFlow(id, type).collect { event ->
                        when (event) {
                            is com.moviehub.feature.details.data.StreamsEvent.CachedStreams -> {
                                val sorted = event.streams.sortedByDescending { it.playbackPriority }
                                _state.value = _state.value.copy(streams = sorted)
                            }
                            is com.moviehub.feature.details.data.StreamsEvent.LoadingStarted -> {
                                _state.value = _state.value.copy(
                                    isSearchingStreams = true,  // re-affirm in case old job's finally overwrote it
                                    addonStreamStatuses = event.providers,
                                    processedStreamAddons = 0,
                                )
                            }
                            is com.moviehub.feature.details.data.StreamsEvent.ProviderStatusChanged -> {
                                val completed = event.allProviders.count {
                                    it.value is com.moviehub.feature.details.data.AddonStreamStatus.Completed ||
                                        it.value is com.moviehub.feature.details.data.AddonStreamStatus.TimedOut ||
                                        it.value is com.moviehub.feature.details.data.AddonStreamStatus.Failed
                                }
                                _state.value = _state.value.copy(
                                    addonStreamStatuses = event.allProviders,
                                    processedStreamAddons = completed,
                                )
                            }
                            is com.moviehub.feature.details.data.StreamsEvent.StreamsUpdated -> {
                                // Throttle: skip updates within 250ms to prevent 95+ recompositions
                                val now = com.moviehub.core.utils.currentTimeMillis()
                                if (now - lastStreamsEmitMs < streamsThrottleMs) return@collect
                                lastStreamsEmitMs = now

                                val sorted = event.streams.sortedByDescending { it.playbackPriority }
                                _state.value = _state.value.copy(streams = sorted)
                            }
                            is com.moviehub.feature.details.data.StreamsEvent.Completed -> {
                                val sorted = event.streams.sortedByDescending { it.playbackPriority }
                                logger.i { "VM Completed: ${sorted.size} streams, isSearchingStreams=false" }
                                _state.value = _state.value.copy(
                                    streams = sorted,
                                    addonStreamStatuses = event.providers,
                                    isSearchingStreams = false,
                                    processedStreamAddons = totalAddons,
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    _state.value = _state.value.copy(streamsError = e.message)
                } finally {
                    safetyJob.cancel()
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
                    selectedTrailerSource = source,
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
                    } else {
                        0f
                    }
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
                            isWatched = true,
                        ),
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
                        ),
                    )
                    _state.value = _state.value.copy(isFavorite = true)
                }
            } finally {
                PerformanceMonitor.endSection()
            }
        }
    }
}
