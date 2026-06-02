package com.moviehub.feature.player.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.moviehub.core.database.PlaybackPreferencesRepository
import com.moviehub.core.database.ProfileRepository
import com.moviehub.core.database.UserPreferencesDao
import com.moviehub.core.database.WatchHistoryDao
import com.moviehub.core.database.WatchProgress
import com.moviehub.core.database.WatchProgressDao
import com.moviehub.core.model.VideoScale
import com.moviehub.core.network.AddonManager
import com.moviehub.core.network.StremioApiClient
import com.moviehub.core.network.torrent.HybridStreamResolver
import com.moviehub.core.player.MoviePlayerController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * ViewModel for PlayerScreen — manages playback state, track selection,
 * binge auto-play, and progress persistence. Currently a migration target:
 * PlayerScreen still holds most state via `remember {}` for incremental migration.
 *
 * To complete migration:
 *   1. Move `remember { mutableStateOf(...) }` → `PlayerScreenUiState` fields
 *   2. Move `LaunchedEffect` blocks → `viewModelScope.launch` / `onAction` handlers
 *   3. Replace `koinInject()` calls → constructor injection (deps below)
 */
class PlayerViewModel(
    private val playerController: MoviePlayerController,
    private val watchProgressDao: WatchProgressDao,
    private val watchHistoryDao: WatchHistoryDao,
    private val profileRepository: ProfileRepository,
    private val userPreferencesDao: UserPreferencesDao,
    private val torrentResolver: HybridStreamResolver,
    private val playbackPrefsRepository: PlaybackPreferencesRepository,
    private val addonManager: AddonManager,
    private val stremioApiClient: StremioApiClient,
) : ViewModel() {
    private val logger = Logger.withTag("PlayerViewModel")
    private val _uiState = MutableStateFlow(PlayerScreenUiState())
    val uiState: StateFlow<PlayerScreenUiState> = _uiState.asStateFlow()

    /**
     * Minimal action handler — extend as state migrates from composable to ViewModel.
     */
    fun onAction(action: PlayerAction) {
        when (action) {
            is PlayerAction.Play -> playerController.play()
            is PlayerAction.Pause -> playerController.pause()
            is PlayerAction.SeekTo -> playerController.seekTo(action.positionMs)
            is PlayerAction.SetSpeed -> playerController.setSpeed(action.speed)
            is PlayerAction.SetVolume -> playerController.setVolume(action.volume)
            is PlayerAction.SetScale -> playerController.setVideoScale(action.scale)
            is PlayerAction.ResetZoom -> playerController.setVideoScale(VideoScale.FIT)
            is PlayerAction.EnterPip -> playerController.enterPip()
            is PlayerAction.SelectAudioTrack -> handleSelectAudioTrack(action)
            is PlayerAction.SelectSubtitleTrack -> handleSelectSubtitleTrack(action)
            is PlayerAction.SelectVideoTrack -> handleSelectVideoTrack(action)
        }
    }

    private fun handleSelectAudioTrack(action: PlayerAction.SelectAudioTrack) {
        val state = _uiState.value.playbackState
        val flatIndex = state.audioTracks.indexOfFirst {
            it.index == action.groupIndex && it.id == action.trackIndex.toString()
        }
        if (flatIndex != -1) {
            playerController.selectAudioTrack(flatIndex)
            val track = state.audioTracks[flatIndex]
            _uiState.value = _uiState.value.copy(
                preferredAudioLanguage = track.language ?: track.label,
                currentAudioGroupIndex = action.groupIndex,
                currentAudioTrackIndex = action.trackIndex,
            )
            viewModelScope.launch {
                try {
                    val prefs = playbackPrefsRepository.getPreferencesFlow().first()
                    playbackPrefsRepository.updatePreferences(
                        prefs.copy(preferredAudioLanguage = track.language ?: track.label),
                    )
                } catch (_: Exception) {
                    logger.d { "Failed to persist audio preference" }
                }
            }
        }
    }

    private fun handleSelectSubtitleTrack(action: PlayerAction.SelectSubtitleTrack) {
        if (action.groupIndex == -1) {
            playerController.selectSubtitleTrack(-1)
            _uiState.value = _uiState.value.copy(
                subtitlesEnabled = false,
                currentSubtitleGroupIndex = -1,
                currentSubtitleTrackIndex = -1,
            )
        } else {
            val state = _uiState.value.playbackState
            val flatIndex = state.subtitleTracks.indexOfFirst {
                it.index == action.groupIndex && it.id == action.trackIndex.toString()
            }
            if (flatIndex != -1) {
                playerController.selectSubtitleTrack(flatIndex)
                val track = state.subtitleTracks[flatIndex]
                _uiState.value = _uiState.value.copy(
                    subtitlesEnabled = true,
                    preferredSubtitleLanguage = track.language ?: track.label,
                    currentSubtitleGroupIndex = action.groupIndex,
                    currentSubtitleTrackIndex = action.trackIndex,
                )
            }
        }
    }

    private fun handleSelectVideoTrack(action: PlayerAction.SelectVideoTrack) {
        _uiState.value = _uiState.value.copy(
            isSettingsSheetOpen = false,
            isChangingVideoTrack = true,
        )
        if (action.groupIndex == -1 && action.trackIndex == -1) {
            playerController.selectVideoTrack(-1)
            _uiState.value = _uiState.value.copy(preferredVideoHeight = -1)
        } else {
            val state = _uiState.value.playbackState
            val flatIndex = state.videoTracks.indexOfFirst {
                it.index == action.groupIndex && it.id == action.trackIndex.toString()
            }
            if (flatIndex != -1) {
                playerController.selectVideoTrack(flatIndex)
                val track = state.videoTracks[flatIndex]
                _uiState.value = _uiState.value.copy(preferredVideoHeight = track.height)
            }
        }
    }

    /** Persist current playback progress to Room DB */
    fun saveProgress(
        mediaId: String?,
        mediaType: String?,
        positionMs: Long,
        durationMs: Long,
        audioGroupIndex: Int,
        audioTrackIndex: Int,
        subtitleGroupIndex: Int,
        subtitleTrackIndex: Int,
    ) {
        if (positionMs <= 0 || durationMs <= 0) return
        viewModelScope.launch {
            try {
                val pId = profileRepository.activeProfile.value?.id ?: return@launch
                val vId = mediaId ?: return@launch
                watchProgressDao.insertOrUpdate(
                    WatchProgress(
                        mediaId = vId,
                        profileId = pId,
                        type = mediaType ?: "movie",
                        progressMs = positionMs,
                        durationMs = durationMs,
                        audioGroupIndex = audioGroupIndex,
                        audioTrackIndex = audioTrackIndex,
                        subtitleGroupIndex = subtitleGroupIndex,
                        subtitleTrackIndex = subtitleTrackIndex,
                    ),
                )
            } catch (_: Exception) {
                logger.d { "Progress save skipped (DB may be closed)" }
            }
        }
    }
}
