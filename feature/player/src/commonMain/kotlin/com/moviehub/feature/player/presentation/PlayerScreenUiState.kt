package com.moviehub.feature.player.presentation

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import com.moviehub.core.model.PlayerPlaybackState
import com.moviehub.core.model.StreamItem
import com.moviehub.core.model.SubtitleStyle
import com.moviehub.core.model.VideoScale

/**
 * Consolidated UI state for PlayerScreen — replaces 45+ individual [androidx.compose.runtime.mutableStateOf]
 * calls with a single source of truth, reducing combinatorial state explosion and enabling
 * future extraction to a dedicated PlayerViewModel.
 */
@Immutable
data class PlayerScreenUiState(
    // ── Core player identity ──
    val activeStream: StreamItem? = null,
    val activeMediaId: String? = null,
    val activeTitle: String = "Playing...",
    // ── Action queue ──
    val requestedAction: PlayerAction? = null,
    val optimisticIsPlaying: Boolean? = null,
    // ── Playback state (managed externally via onStateChanged callback) ──
    val playbackState: PlayerPlaybackState = PlayerPlaybackState(),
    // ── Audio / Subtitle / Video preferences ──
    val preferredAudioLanguage: String? = null,
    val preferredSubtitleLanguage: String? = null,
    val subtitlesEnabled: Boolean = true,
    val preferredVideoHeight: Int = 0,
    val currentAudioGroupIndex: Int = -2,
    val currentAudioTrackIndex: Int = -2,
    val currentSubtitleGroupIndex: Int = -2,
    val currentSubtitleTrackIndex: Int = -2,
    val isChangingAudioTrack: Boolean = false,
    val isChangingVideoTrack: Boolean = false,
    val hasAppliedPreferencesForCurrentSource: Boolean = false,
    // ── UI visibility toggles ──
    val isControlsVisible: Boolean = true,
    val isScreenLocked: Boolean = false,
    val isSettingsSheetOpen: Boolean = false,
    val showDebugOverlay: Boolean = false,
    val wasPlayingBeforeSheetOpen: Boolean = false,
    // ── Loading / History lifecycle ──
    val showLoading: Boolean = true,
    val hasSavedHistory: Boolean = false,
    val hasStartedPlaying: Boolean = false,
    val hasResumed: Boolean = false,
    val hasTriggeredAutoPlay: Boolean = false,
    // ── Seek / Interaction ──
    val lastSeekTime: Long = 0L,
    val lastSeekTimeMs: Long = -1L,
    val pendingSeekPosition: Long = -1L,
    val seekIncrement: Int = 10,
    val seekIndicatorText: String? = null,
    val interactionTick: Int = 0,
    // ── Volume / Brightness ──
    val volume: Float = 1f,
    val brightness: Float = 1f,
    val transientVolume: Float? = null,
    val transientBrightness: Float? = null,
    // ── Video scale / Zoom ──
    val videoScale: VideoScale = VideoScale.FIT,
    val freeZoomScale: Float = 1f,
    val freeZoomOffset: Offset = Offset.Zero,
    // ── Subtitle style ──
    val subtitleStyle: SubtitleStyle = SubtitleStyle(),
    val saveSubtitlesGlobally: Boolean = true,
    // ── Sleep timer ──
    val sleepTimerRemainingMs: Long? = null,
    // ── Binge / Auto-play next episode ──
    val showAutoPlayCountdown: Boolean = false,
    val autoPlayCountdownSeconds: Int = 10,
    val nextEpisodeStream: StreamItem? = null,
    val nextEpisodeTitle: String? = null,
    val nextEpisodeId: String? = null,
    val isPreFetchingNextStream: Boolean = false,
    val hasTriggeredBingeCountdown: Boolean = false,
    // ── Torrent / Retry ──
    val torrentResolvedForHash: String? = null,
    val retryTrigger: Int = 0,
    // ── Transient feedback ──
    val feedbackMessage: String? = null,
)
