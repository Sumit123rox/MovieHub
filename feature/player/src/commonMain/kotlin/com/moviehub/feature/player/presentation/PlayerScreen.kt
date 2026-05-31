package com.moviehub.feature.player.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.moviehub.core.database.ProfileRepository
import com.moviehub.core.database.UserPreferencesDao
import com.moviehub.core.database.WatchHistoryDao
import com.moviehub.core.database.WatchProgress
import com.moviehub.core.database.WatchProgressDao
import com.moviehub.core.model.PlayerPlaybackState
import com.moviehub.core.model.StreamItem
import com.moviehub.core.model.SubtitleStyle
import com.moviehub.core.model.VideoScale
import com.moviehub.core.player.MoviePlayerController
import com.moviehub.core.ui.components.SmartStatusBar
import com.moviehub.core.ui.theme.MovieHubColors
import com.moviehub.core.ui.theme.MovieHubDimens
import com.moviehub.feature.player.presentation.components.PlayerControls
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.compose.koinInject

@Composable
expect fun VideoPlayer(
    url: String,
    headers: Map<String, String> = emptyMap(),
    onPlaybackStateChanged: (PlayerPlaybackState) -> Unit = {},
    forceLandscape: Boolean = true,
    brightness: Float = 1f,
    videoScale: VideoScale = VideoScale.FIT,
    subtitleBottomMargin: Int = 0,
    subtitleStyle: SubtitleStyle = SubtitleStyle(),
    drmLicenseUrl: String? = null,
    drmScheme: String? = null,
    modifier: Modifier = Modifier,
)

sealed interface PlayerAction {
    data class SeekTo(val positionMs: Long) : PlayerAction

    data class SetSpeed(val speed: Float) : PlayerAction

    /** @param groupIndex TrackGroup index, -1 for subtitles = disabled */
    data class SelectAudioTrack(val groupIndex: Int, val trackIndex: Int) : PlayerAction

    /** @param groupIndex TrackGroup index, -1 = disable subtitles */
    data class SelectSubtitleTrack(val groupIndex: Int, val trackIndex: Int) : PlayerAction

    data class SelectVideoTrack(val groupIndex: Int, val trackIndex: Int) : PlayerAction

    data object Play : PlayerAction

    data object Pause : PlayerAction

    data class SetVolume(val volume: Float) : PlayerAction

    data class SetScale(val scale: VideoScale) : PlayerAction

    data object ResetZoom : PlayerAction

    data object EnterPip : PlayerAction
}

@Composable
fun PlayerScreen(
    stream: StreamItem,
    streams: List<StreamItem> = emptyList(),
    onBackClick: () -> Unit,
    title: String = "Playing...",
    mediaId: String? = null,
    mediaType: String? = null,
    posterUrl: String? = null,
    onNextEpisode: (() -> Unit)? = null,
    onPreviousEpisode: (() -> Unit)? = null,
) {
    var activeStream by remember { mutableStateOf(stream) }
    var activeMediaId by remember { mutableStateOf(mediaId) }
    var activeTitle by remember { mutableStateOf(title) }

    LaunchedEffect(mediaId) {
        activeMediaId = mediaId
    }
    LaunchedEffect(title) {
        activeTitle = title
    }

    var playbackState by remember { mutableStateOf(PlayerPlaybackState()) }
    var requestedAction by remember { mutableStateOf<PlayerAction?>(null) }
    var isActionPending by remember { mutableStateOf(false) }
    var optimisticIsPlaying by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(optimisticIsPlaying) {
        if (optimisticIsPlaying != null) {
            kotlinx.coroutines.delay(1500)
            optimisticIsPlaying = null
        }
    }

    val playerController: MoviePlayerController = koinInject()

    LaunchedEffect(requestedAction) {
        requestedAction?.let { action ->
            when (action) {
                is PlayerAction.Play -> playerController.play()
                is PlayerAction.Pause -> playerController.pause()
                is PlayerAction.SeekTo -> playerController.seekTo(action.positionMs)
                is PlayerAction.SetSpeed -> playerController.setSpeed(action.speed)
                is PlayerAction.SetVolume -> playerController.setVolume(action.volume)
                is PlayerAction.SelectAudioTrack -> {
                    val flatIndex = playbackState.audioTracks.indexOfFirst {
                        it.index == action.groupIndex && it.id == action.trackIndex.toString()
                    }
                    if (flatIndex != -1) playerController.selectAudioTrack(flatIndex)
                }

                is PlayerAction.SelectSubtitleTrack -> {
                    if (action.groupIndex == -1) {
                        playerController.selectSubtitleTrack(-1)
                    } else {
                        val flatIndex = playbackState.subtitleTracks.indexOfFirst {
                            it.index == action.groupIndex && it.id == action.trackIndex.toString()
                        }
                        if (flatIndex != -1) playerController.selectSubtitleTrack(flatIndex)
                    }
                }

                is PlayerAction.SelectVideoTrack -> {
                    val flatIndex = playbackState.videoTracks.indexOfFirst {
                        it.index == action.groupIndex && it.id == action.trackIndex.toString()
                    }
                    if (flatIndex != -1) playerController.selectVideoTrack(flatIndex)
                }

                is PlayerAction.SetScale -> playerController.setVideoScale(action.scale)
                is PlayerAction.ResetZoom -> playerController.setVideoScale(VideoScale.FIT)
                is PlayerAction.EnterPip -> playerController.enterPip()
                else -> { // no-op
                }
            }
            requestedAction = null
        }
    }
    val isInPip = rememberIsInPipMode()
    var lastSeekTime by remember { mutableStateOf(0L) }
    var isControlsVisible by remember { mutableStateOf(true) }
    var isScreenLocked by remember { mutableStateOf(false) }
    val blurRadius by animateDpAsState(
        targetValue = if (isControlsVisible && !isScreenLocked && !isIosPlatform && !isInPip) MovieHubDimens.Player.controlsBlurRadius else MovieHubDimens.Spacing.dp0,
        animationSpec = tween(durationMillis = 300),
    )
    var feedbackMessage by remember { mutableStateOf<String?>(null) }
    var showLoading by remember { mutableStateOf(true) }
    var isChangingAudioTrack by remember { mutableStateOf(false) }
    var isChangingVideoTrack by remember { mutableStateOf(false) }
    var hasSavedHistory by remember { mutableStateOf(false) }
    var hasStartedPlaying by remember { mutableStateOf(false) }
    var hasResumed by remember { mutableStateOf(false) }
    var currentAudioGroupIndex by remember { mutableStateOf(-2) }
    var currentAudioTrackIndex by remember { mutableStateOf(-2) }
    var currentSubtitleGroupIndex by remember { mutableStateOf(-2) }
    var currentSubtitleTrackIndex by remember { mutableStateOf(-2) }
    val initialVolume = rememberSystemVolume()
    val initialBrightness = rememberSystemBrightness()
    var volume by remember { mutableStateOf(initialVolume) }
    var brightness by remember { mutableStateOf(initialBrightness) }
    var transientVolume by remember { mutableStateOf<Float?>(null) }
    var transientBrightness by remember { mutableStateOf<Float?>(null) }
    var videoScale by remember { mutableStateOf(VideoScale.FIT) }
    var freeZoomScale by remember { mutableStateOf(1f) }
    var freeZoomOffset by remember { mutableStateOf(Offset.Zero) }
    var showDebugOverlay by remember { mutableStateOf(false) }
    var pendingSeekPosition by remember { mutableStateOf(-1L) }
    var isSettingsSheetOpen by remember { mutableStateOf(false) }
    var sleepTimerRemainingMs by remember { mutableStateOf<Long?>(null) }
    var subtitleStyle by remember { mutableStateOf(SubtitleStyle()) }
    val posterResource = if (posterUrl != null) asyncPainterResource(data = posterUrl) else null
    var hasTriggeredAutoPlay by remember { mutableStateOf(false) }
    var retryTrigger by remember { mutableStateOf(0) }

    LaunchedEffect(isInPip) {
        if (isInPip) {
            isControlsVisible = false
        }
    }

    val clipboardManager = LocalClipboardManager.current
    val uriHandler = LocalUriHandler.current
    val watchProgressDao: WatchProgressDao = koinInject()
    val watchHistoryDao: WatchHistoryDao = koinInject()
    val profileRepository: ProfileRepository = koinInject()
    val userPreferencesDao: UserPreferencesDao = koinInject()
    val torrentResolver: com.moviehub.core.network.torrent.HybridStreamResolver = koinInject()
    val playbackPrefsRepository: com.moviehub.core.database.PlaybackPreferencesRepository = koinInject()
    val addonManager: com.moviehub.core.network.AddonManager = koinInject()
    val stremioApiClient: com.moviehub.core.network.StremioApiClient = koinInject()
    val scope = rememberCoroutineScope()

    val playbackPreferences by playbackPrefsRepository.getPreferencesFlow().collectAsState(com.moviehub.core.database.PlaybackPreferences())

    var showAutoPlayCountdown by remember { mutableStateOf(false) }
    var autoPlayCountdownSeconds by remember { mutableIntStateOf(10) }
    var nextEpisodeStream by remember { mutableStateOf<StreamItem?>(null) }
    var nextEpisodeTitle by remember { mutableStateOf<String?>(null) }
    var isPreFetchingNextStream by remember { mutableStateOf(false) }
    var nextEpisodeId by remember { mutableStateOf<String?>(null) }
    var hasTriggeredBingeCountdown by remember { mutableStateOf(false) }

    val triggerPlayNext: () -> Unit = {
        showAutoPlayCountdown = false
        val resolvedStream = nextEpisodeStream
        if (resolvedStream != null) {
            val parts = activeMediaId!!.split(":")
            val seriesId = parts[0]
            val season = parts.getOrNull(1)?.toIntOrNull() ?: 1
            val episode = parts.getOrNull(2)?.toIntOrNull() ?: 1

            pendingSeekPosition = 0L
            hasStartedPlaying = false
            showLoading = true
            hasSavedHistory = false
            optimisticIsPlaying = null
            playbackState = playbackState.copy(
                isPlaying = false,
                isLoading = true,
                currentPositionMs = 0L,
                durationMs = 0L,
                bufferedPositionMs = 0L,
                error = null,
            )
            activeStream = resolvedStream
            activeMediaId = nextEpisodeId
            activeTitle = "Season $season Episode ${episode + 1}"

            // Reset binge conditions for next loop
            nextEpisodeStream = null
            nextEpisodeId = null
            hasTriggeredBingeCountdown = false
        }
    }

    var seekIncrement by remember { mutableStateOf(10) }
    var seekIndicatorText by remember { mutableStateOf<String?>(null) }
    var interactionTick by remember { mutableStateOf(0) }
    var lastSeekTimeMs by remember { mutableStateOf(-1L) }

    val streamUrl = activeStream.url ?: activeStream.externalUrl ?: ""
    val headers = activeStream.behaviorHints.proxyHeaders?.request ?: emptyMap()

    // Resolve torrent streams: Debrid → P2P fallback
    var torrentResolvedForHash by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(activeStream, torrentResolvedForHash) {
        if (activeStream.isTorrentStream && activeStream.infoHash != null && torrentResolvedForHash != activeStream.infoHash) {
            torrentResolvedForHash = activeStream.infoHash
            val result = torrentResolver.resolve(activeStream)
            when (result) {
                is com.moviehub.core.network.torrent.ResolveResult.Direct -> {
                    if (result.stream.url != activeStream.url) activeStream = result.stream
                }

                is com.moviehub.core.network.torrent.ResolveResult.P2p -> {
                    activeStream = result.stream
                }

                is com.moviehub.core.network.torrent.ResolveResult.Unavailable -> {
                    // Leave active stream as-is — player error state will show
                }
            }
        }
    }

    // Sort streams with active one first
    val sortedStreams = remember(streams, activeStream) {
        if (streams.isEmpty()) {
            streams
        } else {
            val active = activeStream
            val activeIndex = streams.indexOfFirst { it.url == active.url && it.externalUrl == active.externalUrl }
            if (activeIndex > 0) {
                listOf(streams[activeIndex]) + streams.filterIndexed { i, _ -> i != activeIndex }
            } else {
                streams
            }
        }
    }

    // Format video resolution string for the info badges
    val videoResolutionLabel = remember(playbackState.videoWidth, playbackState.videoHeight) {
        val w = playbackState.videoWidth
        val h = playbackState.videoHeight
        when {
            w >= 7680 || h >= 4320 -> "8K"
            w >= 3840 || h >= 2160 -> "4K"
            w >= 2560 || h >= 1440 -> "1440p"
            w >= 1920 || h >= 1080 -> "1080p"
            w >= 1280 || h >= 720 -> "720p"
            w >= 854 || h >= 480 -> "480p"
            w > 0 || h > 0 -> "${w}x$h"
            else -> ""
        }
    }

    // Merge state from VideoPlayer to preserve tracks/error when one platform
    // sends a partial update (e.g. Android polling loop without track/error data)
    val onStateChanged: (PlayerPlaybackState) -> Unit = remember {
        { update ->
            if (optimisticIsPlaying == update.isPlaying) {
                optimisticIsPlaying = null
            }
            playbackState = playbackState.copy(
                isPlaying = update.isPlaying,
                isLoading = update.isLoading,
                error = update.error ?: playbackState.error,
                currentPositionMs = update.currentPositionMs,
                durationMs = update.durationMs,
                bufferedPositionMs = update.bufferedPositionMs,
                playbackSpeed = update.playbackSpeed,
                selectedAudioTrackIndex = if (update.audioTracks.isNotEmpty()) update.selectedAudioTrackIndex else playbackState.selectedAudioTrackIndex,
                selectedSubtitleTrackIndex = if (update.subtitleTracks.isNotEmpty()) update.selectedSubtitleTrackIndex else playbackState.selectedSubtitleTrackIndex,
                audioTracks = if (update.audioTracks.isNotEmpty()) update.audioTracks else playbackState.audioTracks,
                subtitleTracks = if (update.subtitleTracks.isNotEmpty()) update.subtitleTracks else playbackState.subtitleTracks,
                videoTracks = if (update.videoTracks.isNotEmpty()) update.videoTracks else playbackState.videoTracks,
                videoWidth = if (update.videoWidth > 0) update.videoWidth else playbackState.videoWidth,
                videoHeight = if (update.videoHeight > 0) update.videoHeight else playbackState.videoHeight,
                videoCodec = update.videoCodec ?: playbackState.videoCodec,
                currentCueCount = update.currentCueCount,
                videoBitrate = if (update.videoBitrate > 0) update.videoBitrate else playbackState.videoBitrate,
                audioBitrate = if (update.audioBitrate > 0) update.audioBitrate else playbackState.audioBitrate,
                chapters = if (update.chapters.isNotEmpty()) update.chapters else playbackState.chapters,
            )
        }
    }

    val saveProgressImmediate: (Long) -> Unit = remember(
        playbackState.durationMs,
        activeMediaId,
        currentAudioGroupIndex,
        currentAudioTrackIndex,
        currentSubtitleGroupIndex,
        currentSubtitleTrackIndex,
    ) {
        { positionMs ->
            if (positionMs > 0 && playbackState.durationMs > 0) {
                scope.launch {
                    try {
                        val pId = profileRepository.activeProfile.value?.id
                        val vId = activeMediaId
                        if (pId != null && vId != null) {
                            watchProgressDao.insertOrUpdate(
                                WatchProgress(
                                    mediaId = vId,
                                    profileId = pId,
                                    type = mediaType ?: "movie",
                                    progressMs = positionMs,
                                    durationMs = playbackState.durationMs,
                                    audioGroupIndex = currentAudioGroupIndex,
                                    audioTrackIndex = currentAudioTrackIndex,
                                    subtitleGroupIndex = currentSubtitleGroupIndex,
                                    subtitleTrackIndex = currentSubtitleTrackIndex,
                                ),
                            )
                        }
                    } catch (_: Exception) {
                        // DB may be closed during activity teardown — best-effort save
                    }
                }
            }
        }
    }

    // Smart status bar — dark icons for player's black background
    SmartStatusBar(isDark = true, color = Color.Black)

    // Intercept system back button — delegate to navigation popBackStack
    PlayerBackHandler(enabled = true, onBack = onBackClick)

    // Unlock orientation when leaving the player
    DisposableEffect(Unit) {
        onDispose { unlockOrientation() }
    }

    // Load seek increment and subtitle style from user preferences
    LaunchedEffect(activeMediaId) {
        val profileId = profileRepository.activeProfile.value?.id ?: return@LaunchedEffect
        val prefs = userPreferencesDao.getPreference(profileId)
        seekIncrement = prefs?.seekIncrement ?: 10
        if (prefs != null && prefs.subtitleStyleJson.isNotBlank()) {
            try {
                val loadedStyle = Json.decodeFromString<SubtitleStyle>(prefs.subtitleStyleJson)
                subtitleStyle = loadedStyle
            } catch (_: Exception) {
                // Keep default if decode fails
            }
        }
    }

    // Auto-resume to last saved position and track preferences on load
    LaunchedEffect(activeMediaId) {
        val profileId = profileRepository.activeProfile.value?.id ?: return@LaunchedEffect
        val videoId = activeMediaId ?: return@LaunchedEffect
        val progress = watchProgressDao.getProgress(videoId, profileId).firstOrNull()
        if (progress != null && !progress.isWatched) {
            // Restore playback position (if > 5 seconds)
            if (progress.progressMs > 5_000) {
                pendingSeekPosition = progress.progressMs
                hasResumed = true
            }
            // Restore audio track preference (best-effort)
            if (progress.audioGroupIndex >= 0 && playbackState.audioTracks.isNotEmpty()) {
                delay(200)
                requestedAction = PlayerAction.SelectAudioTrack(progress.audioGroupIndex, progress.audioTrackIndex)
                currentAudioGroupIndex = progress.audioGroupIndex
                currentAudioTrackIndex = progress.audioTrackIndex
            }
            // Restore subtitle track preference (best-effort)
            if (progress.subtitleGroupIndex != -2 && playbackState.subtitleTracks.isNotEmpty()) {
                delay(200)
                requestedAction = PlayerAction.SelectSubtitleTrack(progress.subtitleGroupIndex, progress.subtitleTrackIndex)
                currentSubtitleGroupIndex = progress.subtitleGroupIndex
                currentSubtitleTrackIndex = progress.subtitleTrackIndex
            }
        }
    }

    // Modern, reactive, and bulletproof progress restoration trigger
    // Seeks EXACTLY when the media duration is resolved (meaning Media3 has successfully loaded/prepared the stream)
    LaunchedEffect(playbackState.durationMs, playbackState.isPlaying) {
        val seekPos = pendingSeekPosition
        if (seekPos > 0 && playbackState.durationMs > 0 && (playbackState.isPlaying || hasStartedPlaying)) {
            requestedAction = PlayerAction.SeekTo(seekPos)
            pendingSeekPosition = -1L // Consume seek token
        }
    }

    // Save watch history on first playback start
    LaunchedEffect(playbackState.isPlaying) {
        if (playbackState.isPlaying && !hasSavedHistory) {
            hasSavedHistory = true
            hasStartedPlaying = true
            val profileId = profileRepository.activeProfile.value?.id ?: return@LaunchedEffect
            val videoId = activeMediaId ?: return@LaunchedEffect
            watchHistoryDao.conditionalUpsertWatchHistory(
                mediaId = videoId,
                profileId = profileId,
                title = activeTitle,
                type = mediaType ?: "movie",
                posterPath = posterUrl,
            )
        }
    }

    // Persist progress every 15 seconds while playing (repeating, not one-shot)
    val periodicSaveScope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        while (isActive) {
            delay(15000)
            // Only save progress when ALL values are valid — prevents saving
            // WatchProgress(durationMs=0) which causes HomeScreen to show
            // spurious "watched" + "<1 min left" states.
            if (playbackState.isPlaying &&
                playbackState.currentPositionMs > 0 &&
                playbackState.durationMs > 0 &&
                playbackState.currentPositionMs < playbackState.durationMs
            ) {
                try {
                    val profileId = profileRepository.activeProfile.value?.id ?: continue
                    val videoId = activeMediaId ?: continue
                    watchProgressDao.insertOrUpdate(
                        WatchProgress(
                            mediaId = videoId,
                            profileId = profileId,
                            type = mediaType ?: "movie",
                            progressMs = playbackState.currentPositionMs,
                            durationMs = playbackState.durationMs,
                            audioGroupIndex = currentAudioGroupIndex,
                            audioTrackIndex = currentAudioTrackIndex,
                            subtitleGroupIndex = currentSubtitleGroupIndex,
                            subtitleTrackIndex = currentSubtitleTrackIndex,
                        ),
                    )
                } catch (_: Exception) {
                    // Best-effort save — DB or lifecycle may be unavailable
                }
            }
        }
    }

    // Save current position when user navigates away from PlayerScreen
    // Wrapped in try-catch to prevent crash if DB is already closed during activity teardown
    DisposableEffect(Unit) {
        onDispose {
            if (hasStartedPlaying && playbackState.currentPositionMs > 0 && playbackState.durationMs > 0 && playbackState.currentPositionMs < playbackState.durationMs) {
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    try {
                        val profileId = profileRepository.activeProfile.value?.id ?: return@launch
                        val videoId = activeMediaId ?: return@launch
                        watchProgressDao.insertOrUpdate(
                            WatchProgress(
                                mediaId = videoId,
                                profileId = profileId,
                                type = mediaType ?: "movie",
                                progressMs = playbackState.currentPositionMs,
                                durationMs = playbackState.durationMs,
                                audioGroupIndex = currentAudioGroupIndex,
                                audioTrackIndex = currentAudioTrackIndex,
                                subtitleGroupIndex = currentSubtitleGroupIndex,
                                subtitleTrackIndex = currentSubtitleTrackIndex,
                            ),
                        )
                    } catch (_: Exception) {
                        // DB may be closed during activity teardown — best-effort save
                    }
                }
            }
        }
    }

    // Mark as watched when near the end (90%) — use insertOrUpdate to ensure the row exists
    LaunchedEffect(playbackState.durationMs) {
        val reachedEnd = playbackState.durationMs > 0 &&
            playbackState.currentPositionMs >= playbackState.durationMs * 0.9 &&
            playbackState.currentPositionMs > 0
        if (reachedEnd) {
            try {
                val profileId = profileRepository.activeProfile.value?.id ?: return@LaunchedEffect
                val videoId = activeMediaId ?: return@LaunchedEffect
                watchProgressDao.insertOrUpdate(
                    WatchProgress(
                        mediaId = videoId,
                        profileId = profileId,
                        type = mediaType ?: "movie",
                        progressMs = playbackState.currentPositionMs,
                        durationMs = playbackState.durationMs,
                        isWatched = true,
                        audioGroupIndex = currentAudioGroupIndex,
                        audioTrackIndex = currentAudioTrackIndex,
                        subtitleGroupIndex = currentSubtitleGroupIndex,
                        subtitleTrackIndex = currentSubtitleTrackIndex,
                    ),
                )
            } catch (_: Exception) {
                // Best-effort
            }
        }
    }

    // Save progress on pause (only after playback has actually started)
    LaunchedEffect(playbackState.isPlaying) {
        if (!playbackState.isPlaying && hasStartedPlaying && playbackState.currentPositionMs > 0) {
            try {
                val profileId = profileRepository.activeProfile.value?.id ?: return@LaunchedEffect
                val videoId = activeMediaId ?: return@LaunchedEffect
                watchProgressDao.insertOrUpdate(
                    WatchProgress(
                        mediaId = videoId,
                        profileId = profileId,
                        type = mediaType ?: "movie",
                        progressMs = playbackState.currentPositionMs,
                        durationMs = playbackState.durationMs,
                        audioGroupIndex = currentAudioGroupIndex,
                        audioTrackIndex = currentAudioTrackIndex,
                        subtitleGroupIndex = currentSubtitleGroupIndex,
                        subtitleTrackIndex = currentSubtitleTrackIndex,
                    ),
                )
            } catch (_: Exception) {
                // Best-effort save on pause
            }
        }
    }

    // Auto-hide controls (resets on user interaction via interactionTick)
    LaunchedEffect(isControlsVisible, playbackState.isPlaying, interactionTick) {
        if (isControlsVisible && playbackState.isPlaying && !isSettingsSheetOpen) {
            delay(3000)
            isControlsVisible = false
        }
    }

    // Auto-clear feedback messages after 2 seconds
    LaunchedEffect(feedbackMessage) {
        if (feedbackMessage != null) {
            delay(2000)
            feedbackMessage = null
        }
    }

    // Auto-clear seek indicator after 800ms
    LaunchedEffect(seekIndicatorText) {
        if (seekIndicatorText != null) {
            delay(800)
            seekIndicatorText = null
        }
    }

    // Sleep timer countdown — self-triggering via state change
    LaunchedEffect(sleepTimerRemainingMs) {
        val remaining = sleepTimerRemainingMs
        when {
            remaining == null -> {} // No timer active
            remaining <= 0 -> {
                requestedAction = PlayerAction.Pause
                sleepTimerRemainingMs = null
            }

            else -> {
                delay(1000)
                sleepTimerRemainingMs = remaining - 1000
            }
        }
    }

    // Determine if we are playing a series episode
    val isSeriesEpisode = remember(mediaType, activeMediaId) {
        mediaType == "series" && activeMediaId != null && activeMediaId!!.contains(":")
    }

    // Dynamic next episode preloading and stream resolution
    LaunchedEffect(isSeriesEpisode, playbackState.currentPositionMs, playbackState.durationMs) {
        if (!isSeriesEpisode || playbackState.durationMs <= 0) return@LaunchedEffect

        // Trigger pre-fetching 30 seconds before completion
        val remainingMs = playbackState.durationMs - playbackState.currentPositionMs
        if (remainingMs in 1000L..30000L && nextEpisodeStream == null && !isPreFetchingNextStream) {
            isPreFetchingNextStream = true
            val parts = activeMediaId!!.split(":")
            val seriesId = parts[0]
            val season = parts.getOrNull(1)?.toIntOrNull() ?: 1
            val episode = parts.getOrNull(2)?.toIntOrNull() ?: 1
            val nextEpId = "$seriesId:$season:${episode + 1}"
            nextEpisodeId = nextEpId
            nextEpisodeTitle = "Season $season Episode ${episode + 1}"

            scope.launch {
                val streamAddons = addonManager.getAddonsProviding("stream", "series")

                // Fetch streams from all addons parallelly
                val fetchedStreams = mutableListOf<StreamItem>()
                streamAddons.map { (url, manifest) ->
                    async {
                        try {
                            stremioApiClient.getStreams(
                                baseUrl = url,
                                type = "series",
                                id = nextEpId,
                                addonName = manifest.name,
                                addonId = manifest.id,
                            )
                        } catch (_: Exception) {
                            emptyList()
                        }
                    }
                }.forEach { deferred ->
                    fetchedStreams.addAll(deferred.await())
                }

                if (fetchedStreams.isNotEmpty()) {
                    val bestStream = fetchedStreams.maxByOrNull { it.playbackPriority }
                    if (bestStream != null) {
                        // If it's torrent/magnet, resolve Debrid links in advance!
                        val resolved = if (bestStream.isTorrentStream && bestStream.infoHash != null) {
                            val res = torrentResolver.resolve(bestStream)
                            when (res) {
                                is com.moviehub.core.network.torrent.ResolveResult.Direct -> res.stream
                                is com.moviehub.core.network.torrent.ResolveResult.P2p -> res.stream
                                else -> bestStream
                            }
                        } else {
                            bestStream
                        }

                        nextEpisodeStream = resolved

                        // Warm up TLS/TCP connection!
                        try {
                            val urlToWarm = resolved.url ?: resolved.externalUrl
                            if (!urlToWarm.isNullOrBlank()) {
                                withContext(Dispatchers.IO) {
                                    val headRequest = okhttp3.Request.Builder()
                                        .url(urlToWarm)
                                        .head()
                                        .build()
                                    // Make a simple client to ping head
                                    okhttp3.OkHttpClient().newCall(headRequest).execute().close()
                                }
                            }
                        } catch (_: Exception) {
                        }
                    }
                }
                isPreFetchingNextStream = false
            }
        }
    }

    // Triggers binge-watching countdown 10 seconds before completion
    LaunchedEffect(isSeriesEpisode, playbackState.currentPositionMs, playbackState.durationMs) {
        if (!isSeriesEpisode || playbackState.durationMs <= 0 || !playbackPreferences.autoPlayNext) return@LaunchedEffect

        val remainingMs = playbackState.durationMs - playbackState.currentPositionMs
        if (remainingMs in 1000L..12000L && nextEpisodeStream != null && !showAutoPlayCountdown && !hasTriggeredBingeCountdown) {
            hasTriggeredBingeCountdown = true
            showAutoPlayCountdown = true
            autoPlayCountdownSeconds = 10
        }
    }

    // Handles the countdown timer tick
    LaunchedEffect(showAutoPlayCountdown) {
        if (showAutoPlayCountdown) {
            while (autoPlayCountdownSeconds > 0 && showAutoPlayCountdown) {
                delay(1000)
                autoPlayCountdownSeconds--
            }
            if (autoPlayCountdownSeconds == 0 && showAutoPlayCountdown) {
                triggerPlayNext()
            }
        }
    }

    // Auto-exit player when movie playback completes (for movies only, not series)
    LaunchedEffect(mediaType, isSeriesEpisode) {
        if (mediaType != "movie" || isSeriesEpisode) return@LaunchedEffect
        while (isActive) {
            delay(1000)
            if (playbackState.durationMs > 0 &&
                playbackState.currentPositionMs >= playbackState.durationMs * 0.95
            ) {
                delay(2000) // Small grace period before auto-exit
                onBackClick()
                break
            }
        }
    }

    // Auto-dismiss transient volume/brightness indicators after 1.5s of inactivity
    LaunchedEffect(transientVolume, transientBrightness) {
        if (transientVolume != null || transientBrightness != null) {
            delay(1500)
            transientVolume = null
            transientBrightness = null
        }
    }

    // Unified robust reactive playback loading overlay dismissal manager
    val isPlayerReady = !playbackState.isLoading && playbackState.durationMs > 0
    val shouldDismissLoader = playbackState.isPlaying || isPlayerReady

    LaunchedEffect(playbackState.isPlaying, playbackState.isLoading, playbackState.durationMs, shouldDismissLoader) {
        if (shouldDismissLoader) {
            showLoading = false
            hasStartedPlaying = true
        } else if (playbackState.isLoading) {
            if (hasStartedPlaying) {
                delay(250) // Brief debounce for mid-playback buffering to avoid microscopic blinks
            }
            showLoading = true
        }
    }

    // Dismiss audio track change loading indicator when selection completes
    LaunchedEffect(playbackState.selectedAudioTrackIndex) {
        isChangingAudioTrack = false
    }

    // Safety timeout for audio track switching loader (max 2 seconds)
    LaunchedEffect(isChangingAudioTrack) {
        if (isChangingAudioTrack) {
            delay(2000)
            isChangingAudioTrack = false
        }
    }

    // Dismiss video track change loading indicator when selection completes
    LaunchedEffect(playbackState.videoWidth, playbackState.videoHeight) {
        isChangingVideoTrack = false
    }

    // Safety timeout for video track switching loader (max 2 seconds)
    LaunchedEffect(isChangingVideoTrack) {
        if (isChangingVideoTrack) {
            delay(2000)
            isChangingVideoTrack = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                if (!isScreenLocked) {
                    isControlsVisible = !isControlsVisible
                    interactionTick++
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        if (streamUrl.isNotBlank()) {
            // Zoom layer wraps the video for free zoom/pinch
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = freeZoomScale,
                        scaleY = freeZoomScale,
                        translationX = freeZoomOffset.x,
                        translationY = freeZoomOffset.y,
                    )
                    .blur(blurRadius),
            ) {
                key(retryTrigger) {
                    VideoPlayer(
                        url = streamUrl,
                        headers = headers,
                        onPlaybackStateChanged = onStateChanged,
                        brightness = brightness,
                        videoScale = videoScale,
                        subtitleBottomMargin = if (isControlsVisible) 110 else 0,
                        subtitleStyle = subtitleStyle,
                        drmLicenseUrl = activeStream.drmLicenseUrl,
                        drmScheme = activeStream.drmScheme,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            // Loading bar at top (only during initial load, not during playback streaming)
            if (playbackState.isLoading && !hasStartedPlaying) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(MovieHubDimens.Spacing.dp3)
                        .align(Alignment.TopCenter),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.White.copy(alpha = 0.15f),
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
                )
            }

            // Screen lock indicator + double-tap to unlock gesture
            if (isScreenLocked) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(MovieHubDimens.Spacing.ml))
                        .padding(horizontal = MovieHubDimens.Spacing.xxl, vertical = MovieHubDimens.Spacing.lg),
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.xs),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(MovieHubDimens.Icon.md),
                        )
                        Text(
                            text = "Screen Locked",
                            color = Color.White.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = "Double-tap to unlock",
                            color = Color.White.copy(alpha = 0.45f),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
                // Gesture layer over full screen for unlock
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = { isScreenLocked = false },
                            )
                        },
                )
            }

            // Gesture & tap overlay — captures touches that native video views swallow
            // Left/right edges: vertical drag for brightness/volume; center: tap to toggle controls
            // Pinch: free zoom
            if (playbackState.error == null && !isScreenLocked) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                if (zoom != 1f) {
                                    // Zoom symmetrically from center (like YouTube)
                                    freeZoomScale = (freeZoomScale * zoom).coerceIn(1f, 5f)
                                }
                                if (freeZoomScale > 1f && pan != Offset.Zero) {
                                    val maxPanX = (freeZoomScale - 1f) * size.width / 2f
                                    val maxPanY = (freeZoomScale - 1f) * size.height / 2f
                                    freeZoomOffset = Offset(
                                        x = (freeZoomOffset.x + pan.x).coerceIn(-maxPanX, maxPanX),
                                        y = (freeZoomOffset.y + pan.y).coerceIn(-maxPanY, maxPanY),
                                    )
                                } else if (freeZoomScale <= 1f) {
                                    freeZoomOffset = Offset.Zero
                                }
                            }
                        }
                        .pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onDragEnd = {
                                    transientVolume = null
                                    transientBrightness = null
                                },
                                onVerticalDrag = { change, dragAmount ->
                                    val x = change.position.x
                                    val y = change.position.y
                                    change.consume()
                                    val w = size.width.toFloat()
                                    val h = size.height.toFloat()
                                    // Only process in middle area (avoid top/bottom control bars)
                                    if (y >= h * 0.08f && y <= h * 0.92f) {
                                        val sens = h * 0.5f
                                        when {
                                            x < w * 0.25f -> {
                                                val raw = if (brightness < 0f) 1f else brightness
                                                brightness = (raw - dragAmount / sens).coerceIn(0f, 1f)
                                                transientBrightness = brightness
                                            }

                                            x > w * 0.75f -> {
                                                val raw = if (volume < 0f) 1f else volume
                                                volume = (raw - dragAmount / sens).coerceIn(0f, 1f)
                                                transientVolume = volume
                                                requestedAction = PlayerAction.SetVolume(volume)
                                            }
                                        }
                                    }
                                },
                            )
                        }
                        .pointerInput(playbackState.currentPositionMs, playbackState.durationMs) {
                            detectHorizontalDragGestures(
                                onHorizontalDrag = { change, dragAmount ->
                                    change.consume()
                                    val seekMs = (dragAmount * 10).toLong() // ~10s per full screen swipe
                                    if (kotlin.math.abs(seekMs) > 200) {
                                        lastSeekTimeMs = playerTimeMillis()
                                        requestedAction = PlayerAction.SeekTo(
                                            (playbackState.currentPositionMs + seekMs)
                                                .coerceIn(0L, playbackState.durationMs.coerceAtLeast(0L)),
                                        )
                                        interactionTick++
                                    }
                                },
                            )
                        }
                        .pointerInput(seekIncrement) {
                            detectTapGestures(
                                onDoubleTap = { offset ->
                                    val w = size.width.toFloat()
                                    val seekMs = seekIncrement * 1000L
                                    if (offset.x < w * 0.3f) {
                                        lastSeekTimeMs = playerTimeMillis()
                                        requestedAction = PlayerAction.SeekTo(
                                            (playbackState.currentPositionMs - seekMs).coerceAtLeast(0L),
                                        )
                                        seekIndicatorText = "-${seekIncrement}s"
                                        interactionTick++
                                    } else if (offset.x > w * 0.7f) {
                                        lastSeekTimeMs = playerTimeMillis()
                                        requestedAction = PlayerAction.SeekTo(
                                            (playbackState.currentPositionMs + seekMs).coerceAtMost(playbackState.durationMs),
                                        )
                                        seekIndicatorText = "+${seekIncrement}s"
                                        interactionTick++
                                    }
                                },
                                onTap = {
                                    isControlsVisible = !isControlsVisible
                                    interactionTick++
                                },
                            )
                        },
                )
            }

            // Transient volume indicator (left side) with slide animation
            AnimatedVisibility(
                visible = transientVolume != null,
                enter = slideInHorizontally { -it } + fadeIn(tween(150)),
                exit = slideOutHorizontally { -it } + fadeOut(tween(250)),
                modifier = Modifier.fillMaxHeight().align(Alignment.CenterStart),
            ) {
                SideSliderIndicator(
                    value = transientVolume ?: 0f,
                    icon = if ((transientVolume ?: 0f) <= 0f) {
                        Icons.Default.VolumeDown
                    } else {
                        Icons.Default.VolumeUp
                    },
                )
            }

            // Transient brightness indicator (right side) with slide animation
            AnimatedVisibility(
                visible = transientBrightness != null,
                enter = slideInHorizontally { it } + fadeIn(tween(150)),
                exit = slideOutHorizontally { it } + fadeOut(tween(250)),
                modifier = Modifier.fillMaxHeight().align(Alignment.CenterEnd),
            ) {
                SideSliderIndicator(
                    value = transientBrightness ?: 0f,
                    icon = if ((transientBrightness ?: 0f) < 0.3f) {
                        Icons.Default.KeyboardArrowDown
                    } else {
                        Icons.Default.KeyboardArrowUp
                    },
                )
            }

            PlayerControls(
                isVisible = isControlsVisible && !isScreenLocked && !isInPip,
                isPlaying = optimisticIsPlaying ?: playbackState.isPlaying,
                title = activeTitle,
                seekIncrement = seekIncrement,
                onSeekIndicatorChange = { seekIndicatorText = it },
                onPlayPauseToggle = {
                    val now = playerTimeMillis()
                    if (now - lastSeekTime < 500) return@PlayerControls
                    lastSeekTime = now
                    val currentPlaying = optimisticIsPlaying ?: playbackState.isPlaying
                    val nextPlaying = !currentPlaying
                    optimisticIsPlaying = nextPlaying
                    requestedAction = if (nextPlaying) PlayerAction.Play else PlayerAction.Pause
                    interactionTick++
                },
                onBackClick = onBackClick,
                onSeek = { progressFraction ->
                    val now = playerTimeMillis()
                    lastSeekTimeMs = now
                    // Debounce: don't send more than 10 seeks/second
                    if (now - lastSeekTime < 100) return@PlayerControls
                    lastSeekTime = now
                    val seekPos = (progressFraction * playbackState.durationMs).toLong()
                        .coerceAtLeast(0L)
                    requestedAction = PlayerAction.SeekTo(seekPos)
                    interactionTick++
                },
                progress = if (playbackState.durationMs > 0) playbackState.currentPositionMs.toFloat() / playbackState.durationMs else 0f,
                duration = playbackState.durationMs,
                currentTime = playbackState.currentPositionMs,
                onSpeedChange = { requestedAction = PlayerAction.SetSpeed(it) },
                onAudioTrackChange = { groupIndex, trackIndex ->
                    isChangingAudioTrack = true
                    requestedAction = PlayerAction.SelectAudioTrack(groupIndex, trackIndex)
                    currentAudioGroupIndex = groupIndex
                    currentAudioTrackIndex = trackIndex
                    periodicSaveScope.launch {
                        val pId = profileRepository.activeProfile.value?.id ?: return@launch
                        val vId = activeMediaId ?: return@launch
                        watchProgressDao.insertOrUpdate(
                            WatchProgress(
                                mediaId = vId,
                                profileId = pId,
                                type = mediaType ?: "movie",
                                progressMs = playbackState.currentPositionMs,
                                durationMs = playbackState.durationMs,
                                audioGroupIndex = groupIndex,
                                audioTrackIndex = trackIndex,
                                subtitleGroupIndex = currentSubtitleGroupIndex,
                                subtitleTrackIndex = currentSubtitleTrackIndex,
                            ),
                        )
                    }
                },
                onSubtitleTrackChange = { groupIndex, trackIndex ->
                    requestedAction = PlayerAction.SelectSubtitleTrack(groupIndex, trackIndex)
                    currentSubtitleGroupIndex = groupIndex
                    currentSubtitleTrackIndex = trackIndex
                    periodicSaveScope.launch {
                        val pId = profileRepository.activeProfile.value?.id ?: return@launch
                        val vId = activeMediaId ?: return@launch
                        watchProgressDao.insertOrUpdate(
                            WatchProgress(
                                mediaId = vId,
                                profileId = pId,
                                type = mediaType ?: "movie",
                                progressMs = playbackState.currentPositionMs,
                                durationMs = playbackState.durationMs,
                                audioGroupIndex = currentAudioGroupIndex,
                                audioTrackIndex = currentAudioTrackIndex,
                                subtitleGroupIndex = groupIndex,
                                subtitleTrackIndex = trackIndex,
                            ),
                        )
                    }
                },
                onScaleChange = {
                    videoScale = it;
                    requestedAction = PlayerAction.SetScale(it)
                },
                onScaleCycle = {
                    val nextIndex = (VideoScale.entries.indexOf(videoScale) + 1) % VideoScale.entries.size
                    val nextScale = VideoScale.entries[nextIndex]
                    videoScale = nextScale
                    requestedAction = PlayerAction.SetScale(nextScale)
                },
                onResetZoom = {
                    freeZoomScale = 1f
                    freeZoomOffset = Offset.Zero
                    requestedAction = PlayerAction.ResetZoom
                },
                currentScale = videoScale,
                freeZoomScale = freeZoomScale,
                audioTracks = playbackState.audioTracks,
                videoTracks = playbackState.videoTracks,
                onVideoTrackChange = { index ->
                    val track = playbackState.videoTracks.getOrNull(index)
                    if (track != null) {
                        isChangingVideoTrack = true
                        requestedAction = PlayerAction.SelectVideoTrack(track.index, track.id.toIntOrNull() ?: 0)
                    }
                },
                subtitleTracks = playbackState.subtitleTracks,
                currentSpeed = playbackState.playbackSpeed,
                currentStream = activeStream,
                streams = sortedStreams,
                onStreamChange = { newStream ->
                    val pos = playbackState.currentPositionMs
                    saveProgressImmediate(pos)
                    pendingSeekPosition = pos
                    hasStartedPlaying = false
                    showLoading = true
                    hasSavedHistory = false
                    optimisticIsPlaying = null
                    playbackState = playbackState.copy(
                        isPlaying = false,
                        isLoading = true,
                        currentPositionMs = 0L,
                        durationMs = 0L,
                        bufferedPositionMs = 0L,
                        error = null,
                    )
                    activeStream = newStream
                },
                currentVolume = volume,
                onVolumeChange = { v ->
                    volume = v
                    requestedAction = PlayerAction.SetVolume(v)
                },
                isScreenLocked = isScreenLocked,
                onScreenLockToggle = { isScreenLocked = !isScreenLocked },
                onEnterPip = { requestedAction = PlayerAction.EnterPip },
                onNextEpisode = onNextEpisode,
                onPreviousEpisode = onPreviousEpisode,
                isSettingsSheetOpen = isSettingsSheetOpen,
                onSettingsSheetOpenChange = { isSettingsSheetOpen = it },
                sleepTimerRemainingMs = sleepTimerRemainingMs,
                onSleepTimerSet = { sleepTimerRemainingMs = it },
                subtitleStyle = subtitleStyle,
                onSubtitleStyleChange = { newStyle ->
                    subtitleStyle = newStyle
                    scope.launch {
                        val profileId = profileRepository.activeProfile.value?.id ?: return@launch
                        val currentPrefs = userPreferencesDao.getPreference(profileId) ?: com.moviehub.core.database.UserPreferencesEntity(profileId)
                        val styleJson = Json.encodeToString(newStyle)
                        userPreferencesDao.setPreference(currentPrefs.copy(subtitleStyleJson = styleJson))
                    }
                },
                videoResolution = videoResolutionLabel,
                videoCodec = playbackState.videoCodec,
                videoBitrate = playbackState.videoBitrate,
                audioBitrate = playbackState.audioBitrate,
                chapters = playbackState.chapters,
                showDebugOverlay = showDebugOverlay,
                onToggleDebug = { showDebugOverlay = !showDebugOverlay },
                preferredResolution = playbackPreferences.preferredResolution,
                onResolutionChange = { selectedRes ->
                    scope.launch {
                        playbackPrefsRepository.updatePreferences(
                            playbackPreferences.copy(preferredResolution = selectedRes),
                        )
                        // Trigger resolution/quality stream switch!
                        val matchingStream = streams.find { stream ->
                            val nameLc = stream.name?.lowercase() ?: ""
                            when (selectedRes) {
                                "4K" -> nameLc.contains("4k") || nameLc.contains("2160")
                                "1080p" -> nameLc.contains("1080") || nameLc.contains("fhd")
                                "720p" -> nameLc.contains("720") || nameLc.contains("hd")
                                "SD" -> nameLc.contains("480") || nameLc.contains("sd")
                                else -> false
                            }
                        }
                        if (matchingStream != null && matchingStream.url != activeStream.url) {
                            val pos = playbackState.currentPositionMs
                            saveProgressImmediate(pos)
                            pendingSeekPosition = pos
                            hasStartedPlaying = false
                            showLoading = true
                            hasSavedHistory = false
                            optimisticIsPlaying = null
                            playbackState = playbackState.copy(
                                isPlaying = false,
                                isLoading = true,
                                currentPositionMs = 0L,
                                durationMs = 0L,
                                bufferedPositionMs = 0L,
                                error = null,
                            )
                            activeStream = matchingStream
                        }
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )

            // Seek indicator overlay (on top of controls, positioned at sides like YouTube)
            if (seekIndicatorText != null) {
                val isRewind = seekIndicatorText!!.startsWith("-")
                Box(
                    modifier = Modifier
                        .align(if (isRewind) Alignment.CenterStart else Alignment.CenterEnd)
                        .padding(horizontal = MovieHubDimens.Icon.xxxl)
                        .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(MovieHubDimens.Spacing.lg))
                        .padding(horizontal = MovieHubDimens.Icon.xl, vertical = MovieHubDimens.Spacing.xl),
                ) {
                    Text(
                        text = seekIndicatorText!!,
                        color = Color.White,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            // Debug overlay (shows stream details — stays on top of everything)
            if (showDebugOverlay) {
                DebugOverlay(
                    stream = activeStream,
                    playbackState = playbackState,
                    videoScale = videoScale,
                    freeZoomScale = freeZoomScale,
                    isControlsVisible = isControlsVisible,
                    brightness = brightness,
                    volume = volume,
                    onDismiss = { showDebugOverlay = false },
                    modifier = Modifier.fillMaxSize(),
                )
            }

            // Premium Error Overlay (only for actual errors)
            if (playbackState.error != null) {
                var showTechDetails by remember { mutableStateOf(false) }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.85f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { /* Intercept and absorb clicks */ },
                    contentAlignment = Alignment.Center,
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF161426), // Premium, theme-matching deep purple-gray
                            contentColor = Color.White,
                        ),
                        shape = RoundedCornerShape(MovieHubDimens.Spacing.xxl),
                        border = BorderStroke(
                            width = MovieHubDimens.Spacing.dp1,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                                ),
                            ),
                        ),
                        modifier = Modifier
                            .width(MovieHubDimens.Player.overlaySheetMaxWidth)
                            .padding(MovieHubDimens.Spacing.xxl),
                    ) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            IconButton(
                                onClick = onBackClick,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(MovieHubDimens.Spacing.sm),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close Player",
                                    tint = Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier.size(MovieHubDimens.Spacing.lg),
                                )
                            }

                            Column(
                                modifier = Modifier
                                    .padding(horizontal = MovieHubDimens.Spacing.xxl, vertical = MovieHubDimens.Spacing.xl)
                                    .verticalScroll(rememberScrollState()),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.md),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(MovieHubDimens.Icon.xxxl)
                                        .background(
                                            Color.Red.copy(alpha = 0.15f),
                                            CircleShape,
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "Playback Failed",
                                        tint = Color.Red,
                                        modifier = Modifier.size(MovieHubDimens.Icon.xl),
                                    )
                                }

                                Text(
                                    text = "Playback Failed",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    textAlign = TextAlign.Center,
                                )

                                Text(
                                    text = "We encountered an issue playing this stream. Please choose another source, try playing in an external player, or retry below.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center,
                                )

                                if (!feedbackMessage.isNullOrBlank()) {
                                    Text(
                                        text = feedbackMessage!!,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MovieHubColors.Success,
                                        fontWeight = FontWeight.SemiBold,
                                        textAlign = TextAlign.Center,
                                    )
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.sm),
                                    modifier = Modifier.fillMaxWidth().padding(top = MovieHubDimens.Spacing.xs),
                                ) {
                                    Button(
                                        onClick = {
                                            playbackState = playbackState.copy(error = null, isLoading = true)
                                            showLoading = true
                                            hasStartedPlaying = false
                                            retryTrigger++
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = Color.White,
                                        ),
                                        shape = RoundedCornerShape(MovieHubDimens.Spacing.md),
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.xs),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = "Retry",
                                                tint = Color.White,
                                                modifier = Modifier.size(MovieHubDimens.Spacing.md),
                                            )
                                            Text("Retry", fontWeight = FontWeight.Bold, maxLines = 1)
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            try {
                                                uriHandler.openUri(streamUrl)
                                            } catch (e: Exception) {
                                                feedbackMessage = "No compatible player found"
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color.White.copy(alpha = 0.08f),
                                            contentColor = Color.White,
                                        ),
                                        shape = RoundedCornerShape(MovieHubDimens.Spacing.md),
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Text("Play Externally", fontWeight = FontWeight.Bold, maxLines = 1)
                                    }
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.sm),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(streamUrl))
                                            feedbackMessage = "Link copied to clipboard!"
                                        },
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = Color.White,
                                        ),
                                        shape = RoundedCornerShape(MovieHubDimens.Spacing.md),
                                        border = BorderStroke(MovieHubDimens.Spacing.dp1, Color.White.copy(alpha = 0.2f)),
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Text("Copy Link", maxLines = 1)
                                    }

                                    OutlinedButton(
                                        onClick = onBackClick,
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = Color.White.copy(alpha = 0.6f),
                                        ),
                                        shape = RoundedCornerShape(MovieHubDimens.Spacing.md),
                                        border = BorderStroke(MovieHubDimens.Spacing.dp1, Color.White.copy(alpha = 0.12f)),
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Text("Go Back", maxLines = 1)
                                    }
                                }

                                // Technical details accordion
                                Spacer(modifier = Modifier.height(MovieHubDimens.Spacing.xxs))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(MovieHubDimens.Spacing.sm))
                                        .clickable { showTechDetails = !showTechDetails }
                                        .padding(horizontal = MovieHubDimens.Spacing.md, vertical = MovieHubDimens.Spacing.xs),
                                ) {
                                    Text(
                                        text = if (showTechDetails) "Hide Technical Details" else "Show Technical Details",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.5f),
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Icon(
                                        imageVector = if (showTechDetails) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.5f),
                                        modifier = Modifier.size(MovieHubDimens.Spacing.md),
                                    )
                                }

                                if (showTechDetails) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = MovieHubDimens.Player.subtitlePreviewHeight)
                                            .clip(RoundedCornerShape(MovieHubDimens.Spacing.sm))
                                            .background(Color.Black.copy(alpha = 0.5f))
                                            .border(MovieHubDimens.Spacing.dp1, Color.White.copy(alpha = 0.08f), RoundedCornerShape(MovieHubDimens.Spacing.sm))
                                            .padding(MovieHubDimens.Spacing.md),
                                    ) {
                                        Text(
                                            text = playbackState.error ?: "Unknown error",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                                fontSize = MovieHubDimens.Font.xxs,
                                            ),
                                            color = Color(0xFFFF8B8B),
                                            modifier = Modifier.verticalScroll(rememberScrollState()),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Text(
                text = "Invalid Stream URL",
                color = MaterialTheme.colorScheme.error,
            )
        }

        // ═══════════════════════════════════════════
        // Loading overlay with poster blur backdrop and crossfade exit
        // ═══════════════════════════════════════════
        AnimatedVisibility(
            visible = (showLoading || isChangingAudioTrack || isChangingVideoTrack) && playbackState.error == null,
            enter = fadeIn(animationSpec = tween(200)),
            exit = fadeOut(animationSpec = tween(500)),
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                // Blurred poster backdrop (if poster URL is available)
                if (posterUrl != null && posterResource != null) {
                    KamelImage(
                        resource = { posterResource },
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(MovieHubDimens.Player.posterBlurRadius),
                        contentScale = ContentScale.Crop,
                        onLoading = {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color(0xFF0F0C20),
                                                Color(0xFF15102A),
                                                Color(0xFF06040A),
                                            ),
                                        ),
                                    ),
                            )
                        },
                        onFailure = {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color(0xFF0F0C20),
                                                Color(0xFF15102A),
                                                Color(0xFF06040A),
                                            ),
                                        ),
                                    ),
                            )
                        },
                    )
                    // Dark overlay on top of the poster for spinner contrast
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f)),
                    )
                } else {
                    // No poster — fall back to rich gradient overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF0F0C20),
                                        Color(0xFF15102A),
                                        Color(0xFF06040A),
                                    ),
                                ),
                            ),
                    )
                }

                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = MovieHubDimens.Spacing.xxs,
                    modifier = Modifier.size(MovieHubDimens.Icon.xxxl),
                )
            }
        }

        // Binge Mode Auto-Play Countdown Card (Netflix Style)
        AnimatedVisibility(
            visible = showAutoPlayCountdown && nextEpisodeStream != null,
            enter = fadeIn(animationSpec = tween(400)) + slideInVertically(initialOffsetY = { it / 2 }),
            exit = fadeOut(animationSpec = tween(400)) + slideOutVertically(targetOffsetY = { it / 2 }),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = MovieHubDimens.Player.bingeCardSafeAreaEnd, bottom = MovieHubDimens.Player.bingeCardSafeAreaBottom),
        ) {
            Box(
                modifier = Modifier
                    .width(MovieHubDimens.Player.bingeCardWidth)
                    .clip(RoundedCornerShape(MovieHubDimens.Player.bingeCardCornerRadius))
                    .background(Color.Black.copy(alpha = 0.75f))
                    .border(
                        width = MovieHubDimens.Spacing.dp1,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                            ),
                        ),
                        shape = RoundedCornerShape(MovieHubDimens.Player.bingeCardCornerRadius),
                    )
                    .padding(MovieHubDimens.Player.bingeCardPadding),
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(MovieHubDimens.Player.bingeCardItemSpacing),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = "BINGE WATCHING",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = MovieHubDimens.Font.trackingUltraWide,
                        )

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(MovieHubDimens.Player.bingeCountdownBubbleSize)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        ) {
                            Text(
                                text = "$autoPlayCountdownSeconds",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }

                    Text(
                        text = "Next: $nextEpisodeTitle",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(MovieHubDimens.Player.bingeCardButtonSpacing),
                    ) {
                        Button(
                            onClick = { triggerPlayNext() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                            shape = RoundedCornerShape(MovieHubDimens.Player.bingeCardButtonCorner),
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Play Now", style = MaterialTheme.typography.labelMedium)
                        }

                        OutlinedButton(
                            onClick = { showAutoPlayCountdown = false },
                            border = BorderStroke(MovieHubDimens.Spacing.dp1, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)),
                            shape = RoundedCornerShape(MovieHubDimens.Player.bingeCardButtonCorner),
                        ) {
                            Text("Cancel", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SideSliderIndicator(
    value: Float,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .width(MovieHubDimens.Player.sideSliderWidth)
            .padding(vertical = MovieHubDimens.Player.shimmerHeight)
            .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(MovieHubDimens.Icon.md))
            .padding(horizontal = MovieHubDimens.Spacing.sm, vertical = MovieHubDimens.Spacing.lg),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.xs),
            modifier = Modifier.fillMaxHeight(),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(MovieHubDimens.Spacing.lg),
            )

            // Vertical track (pill)
            Box(
                modifier = Modifier
                    .width(MovieHubDimens.Spacing.dp3)
                    .weight(1f)
                    .background(
                        Color.White.copy(alpha = 0.12f),
                        RoundedCornerShape(MovieHubDimens.Spacing.dp2),
                    ),
                contentAlignment = Alignment.BottomCenter,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(value.coerceIn(0f, 1f))
                        .background(
                            MaterialTheme.colorScheme.primary,
                            RoundedCornerShape(MovieHubDimens.Spacing.dp2),
                        ),
                )
            }

            // Percentage label
            Text(
                text = "${(value.coerceIn(0f, 1f) * 100).toInt()}%",
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = MovieHubDimens.Font.xxs,
                    fontWeight = FontWeight.Medium,
                ),
            )
        }
    }
}

@Composable
private fun DebugOverlay(
    stream: StreamItem?,
    playbackState: PlayerPlaybackState,
    videoScale: VideoScale,
    freeZoomScale: Float,
    isControlsVisible: Boolean,
    brightness: Float,
    volume: Float,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val streamUrl = stream?.url ?: stream?.externalUrl ?: ""
    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.8f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MovieHubDimens.Spacing.xxl)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.sm),
        ) {
            Text(
                text = "🎬 Debug Info",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            HorizontalDivider(color = Color.White.copy(alpha = 0.2f))

            DebugRow("Title", stream?.addonName ?: stream?.name ?: "N/A")
            DebugRow("Source URL", streamUrl)
            DebugRow("Info Hash", stream?.infoHash ?: "N/A")
            DebugRow("File Index", stream?.fileIdx?.toString() ?: "N/A")

            HorizontalDivider(color = Color.White.copy(alpha = 0.15f))

            val resStr = if (playbackState.videoWidth > 0 && playbackState.videoHeight > 0) {
                "${playbackState.videoWidth}x${playbackState.videoHeight}"
            } else {
                "N/A"
            }
            DebugRow("Resolution", resStr)
            DebugRow("Codec", playbackState.videoCodec ?: "N/A")
            DebugRow("Decoder", playbackState.decoderName ?: "N/A")
            DebugRow("HW Decode", if (playbackState.hardwareDecoding) "Yes" else "No")

            val bitrateStr = when {
                playbackState.videoBitrate >= 1_000_000 -> "${playbackState.videoBitrate / 1_000_000}.${(playbackState.videoBitrate % 1_000_000) / 100_000} Mbps"
                playbackState.videoBitrate > 0 -> "${playbackState.videoBitrate / 1000} kbps"
                else -> "N/A"
            }
            DebugRow("Video Bitrate", bitrateStr)
            DebugRow("HDR Mode", playbackState.hdrMode ?: "N/A")
            DebugRow("Dropped Frames", playbackState.droppedFrameCount.toString())
            DebugRow("Display FPS", if (playbackState.displayFps > 0) "${playbackState.displayFps} fps" else "N/A")
            DebugRow("Video Scale", videoScale.label)

            HorizontalDivider(color = Color.White.copy(alpha = 0.15f))

            DebugRow("Audio Codec", playbackState.audioCodec ?: "N/A")
            DebugRow("Audio Sample Rate", if (playbackState.audioSampleRate > 0) "${playbackState.audioSampleRate} Hz" else "N/A")
            DebugRow("Audio Channels", if (playbackState.audioChannels > 0) playbackState.audioChannels.toString() else "N/A")

            HorizontalDivider(color = Color.White.copy(alpha = 0.15f))

            DebugRow("Demuxer", playbackState.demuxerName ?: "N/A")
            DebugRow("Container", playbackState.containerFormat ?: "N/A")
            DebugRow("Backend", playbackState.backendInfo?.name ?: playbackState.backendInfo?.backend?.name ?: "N/A")

            HorizontalDivider(color = Color.White.copy(alpha = 0.15f))

            DebugRow("Position", formatDuration(playbackState.currentPositionMs))
            DebugRow("Duration", formatDuration(playbackState.durationMs))
            DebugRow("Buffered", formatDuration(playbackState.bufferedPositionMs))
            DebugRow("Speed", "${playbackState.playbackSpeed}x")
            DebugRow("Volume", "${(volume * 100).toInt()}%")
            DebugRow("Brightness", "${(brightness * 100).toInt()}%")
            DebugRow("Free Zoom", "${(freeZoomScale * 100).toInt()}%")
            DebugRow("Controls Visible", if (isControlsVisible) "Yes" else "No")

            HorizontalDivider(color = Color.White.copy(alpha = 0.15f))

            DebugRow("Playing", if (playbackState.isPlaying) "Yes" else "No")
            DebugRow("Loading", if (playbackState.isLoading) "Yes" else "No")
            DebugRow("Error", playbackState.error ?: "None")

            val chapterCount = playbackState.chapters.size
            if (chapterCount > 0) {
                val currentChapter = playbackState.chapters.indexOfFirst { c ->
                    playbackState.currentPositionMs in c.startMs..c.endMs
                }
                DebugRow("Chapters", "$chapterCount available")
                if (currentChapter >= 0) {
                    DebugRow("Current Chapter", playbackState.chapters[currentChapter].title)
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.15f))

            val selAudio = playbackState.audioTracks.firstOrNull { it.isSelected }
            DebugRow("Audio Track", selAudio?.let { "${it.label} (${it.language ?: "unknown"})" } ?: "None selected (${playbackState.audioTracks.size} available)")

            val selSub = playbackState.subtitleTracks.firstOrNull { it.isSelected }
            DebugRow("Subtitle Track", selSub?.let { "${it.label} (${it.language ?: "unknown"})" } ?: "Off (${playbackState.subtitleTracks.size} available)")
            DebugRow("Cue Count", "${playbackState.currentCueCount}")

            Spacer(modifier = Modifier.height(MovieHubDimens.Spacing.lg))

            Text(
                text = "Tap anywhere to dismiss",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun DebugRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.weight(0.35f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(0.65f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "$hours:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    } else {
        "$minutes:${seconds.toString().padStart(2, '0')}"
    }
}
