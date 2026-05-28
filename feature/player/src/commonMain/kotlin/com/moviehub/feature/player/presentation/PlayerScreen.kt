package com.moviehub.feature.player.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moviehub.core.database.ProfileRepository
import com.moviehub.core.database.UserPreferencesDao
import com.moviehub.core.database.WatchHistoryDao
import com.moviehub.core.database.WatchHistoryEntity
import com.moviehub.core.database.WatchProgress
import com.moviehub.core.database.WatchProgressDao
import com.moviehub.core.model.PlayerPlaybackState
import com.moviehub.core.model.StreamItem
import com.moviehub.core.model.SubtitleStyle
import com.moviehub.core.model.VideoScale
import com.moviehub.core.ui.theme.MovieHubColors
import com.moviehub.feature.player.presentation.components.PlayerControls
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
expect fun VideoPlayer(
    url: String,
    headers: Map<String, String> = emptyMap(),
    onPlaybackStateChanged: (PlayerPlaybackState) -> Unit = {},
    requestedAction: PlayerAction? = null,
    onActionConsumed: () -> Unit = {},
    forceLandscape: Boolean = true,
    brightness: Float = 1f,
    videoScale: VideoScale = VideoScale.FIT,
    subtitleBottomMargin: Int = 0,
    subtitleStyle: SubtitleStyle = SubtitleStyle(),
    drmLicenseUrl: String? = null,
    drmScheme: String? = null,
    modifier: Modifier = Modifier
)

sealed interface PlayerAction {
    data class SeekTo(val positionMs: Long) : PlayerAction
    data class SetSpeed(val speed: Float) : PlayerAction
    /** @param groupIndex TrackGroup index, -1 for subtitles = disabled */
    data class SelectAudioTrack(val groupIndex: Int, val trackIndex: Int) : PlayerAction
    /** @param groupIndex TrackGroup index, -1 = disable subtitles */
    data class SelectSubtitleTrack(val groupIndex: Int, val trackIndex: Int) : PlayerAction
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
    var playbackState by remember { mutableStateOf(PlayerPlaybackState()) }
    var requestedAction by remember { mutableStateOf<PlayerAction?>(null) }
    var isControlsVisible by remember { mutableStateOf(true) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }
    var showLoading by remember { mutableStateOf(true) }
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
    var isScreenLocked by remember { mutableStateOf(false) }
    var videoScale by remember { mutableStateOf(VideoScale.FIT) }
    var freeZoomScale by remember { mutableStateOf(1f) }
    var freeZoomOffset by remember { mutableStateOf(Offset.Zero) }
    var showDebugOverlay by remember { mutableStateOf(false) }
    var pendingStreamSwitchPosition by remember { mutableStateOf(-1L) }
    var isSettingsSheetOpen by remember { mutableStateOf(false) }
    var sleepTimerRemainingMs by remember { mutableStateOf<Long?>(null) }
    var subtitleStyle by remember { mutableStateOf(SubtitleStyle()) }
    var hasTriggeredAutoPlay by remember { mutableStateOf(false) }

    val clipboardManager = LocalClipboardManager.current
    val uriHandler = LocalUriHandler.current
    val watchProgressDao: WatchProgressDao = koinInject()
    val watchHistoryDao: WatchHistoryDao = koinInject()
    val profileRepository: ProfileRepository = koinInject()
    val userPreferencesDao: UserPreferencesDao = koinInject()
    val torrentResolver: com.moviehub.core.network.torrent.HybridStreamResolver = koinInject()

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
        if (streams.isEmpty()) streams
        else {
            val active = activeStream
            val activeIndex = streams.indexOfFirst { it.url == active.url && it.externalUrl == active.externalUrl }
            if (activeIndex > 0) {
                listOf(streams[activeIndex]) + streams.filterIndexed { i, _ -> i != activeIndex }
            } else streams
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
            w > 0 || h > 0 -> "${w}x${h}"
            else -> ""
        }
    }

    // Merge state from VideoPlayer to preserve tracks/error when one platform
    // sends a partial update (e.g. Android polling loop without track/error data)
    val onStateChanged: (PlayerPlaybackState) -> Unit = remember {
        { update ->
            playbackState = playbackState.copy(
                isPlaying = update.isPlaying,
                isLoading = update.isLoading,
                error = update.error ?: playbackState.error,
                currentPositionMs = update.currentPositionMs,
                durationMs = update.durationMs,
                bufferedPositionMs = update.bufferedPositionMs,
                playbackSpeed = update.playbackSpeed,
                selectedAudioTrackIndex = update.selectedAudioTrackIndex,
                selectedSubtitleTrackIndex = update.selectedSubtitleTrackIndex,
                audioTracks = if (update.audioTracks.isNotEmpty()) update.audioTracks else playbackState.audioTracks,
                subtitleTracks = if (update.subtitleTracks.isNotEmpty()) update.subtitleTracks else playbackState.subtitleTracks,
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

    // Intercept system back button — delegate to navigation popBackStack
    PlayerBackHandler(enabled = true, onBack = onBackClick)

    // Load seek increment from user preferences
    LaunchedEffect(mediaId) {
        val profileId = profileRepository.activeProfile.value?.id ?: return@LaunchedEffect
        val prefs = userPreferencesDao.getPreference(profileId)
        seekIncrement = prefs?.seekIncrement ?: 10
    }

    // Auto-resume to last saved position and track preferences on load
    LaunchedEffect(mediaId) {
        val profileId = profileRepository.activeProfile.value?.id ?: return@LaunchedEffect
        val videoId = mediaId ?: return@LaunchedEffect
        val progress = watchProgressDao.getProgress(videoId, profileId).firstOrNull()
        if (progress != null && !progress.isWatched) {
            // Restore playback position (if > 5 seconds)
            if (progress.progressMs > 5_000) {
                delay(500) // Wait for player to initialize
                requestedAction = PlayerAction.SeekTo(progress.progressMs)
                hasResumed = true
            }
            // Wait for audio tracks to actually be loaded (player prepare is async)
            while (playbackState.audioTracks.isEmpty() && isActive) {
                delay(100)
            }
            // Restore audio track preference (user explicitly selected a track)
            if (progress.audioGroupIndex >= 0 && playbackState.audioTracks.isNotEmpty()) {
                delay(300) // Small buffer after tracks detected
                requestedAction = PlayerAction.SelectAudioTrack(progress.audioGroupIndex, progress.audioTrackIndex)
                currentAudioGroupIndex = progress.audioGroupIndex
                currentAudioTrackIndex = progress.audioTrackIndex
            }
            // Wait for subtitle tracks to be available
            while (playbackState.subtitleTracks.isEmpty() && isActive) {
                delay(100)
            }
            // Restore subtitle track preference (user explicitly enabled/disabled)
            if (progress.subtitleGroupIndex != -2 && playbackState.subtitleTracks.isNotEmpty()) {
                delay(300)
                requestedAction = PlayerAction.SelectSubtitleTrack(progress.subtitleGroupIndex, progress.subtitleTrackIndex)
                currentSubtitleGroupIndex = progress.subtitleGroupIndex
                currentSubtitleTrackIndex = progress.subtitleTrackIndex
            }
        }
    }

    // Auto-resume position after switching stream source
    LaunchedEffect(streamUrl) {
        val savedPos = pendingStreamSwitchPosition
        if (savedPos > 0) {
            // Wait for new stream to start playing before seeking
            val pollDelays = listOf(500L, 500L, 1000L, 1000L)
            for (waitMs in pollDelays) {
                delay(waitMs)
                if (playbackState.isPlaying) break
            }
            requestedAction = PlayerAction.SeekTo(savedPos)
            pendingStreamSwitchPosition = -1
        }
    }

    // Save watch history on first playback start
    LaunchedEffect(playbackState.isPlaying) {
        if (playbackState.isPlaying && !hasSavedHistory) {
            hasSavedHistory = true
            hasStartedPlaying = true
            val profileId = profileRepository.activeProfile.value?.id ?: return@LaunchedEffect
            val videoId = mediaId ?: return@LaunchedEffect
            watchHistoryDao.conditionalUpsertWatchHistory(
                mediaId = videoId,
                profileId = profileId,
                title = title,
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
            if (playbackState.isPlaying && playbackState.currentPositionMs > 0) {
                try {
                    val profileId = profileRepository.activeProfile.value?.id ?: continue
                    val videoId = mediaId ?: continue
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
                        )
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
            if (hasStartedPlaying && playbackState.currentPositionMs > 0) {
                periodicSaveScope.launch {
                    try {
                        val profileId = profileRepository.activeProfile.value?.id ?: return@launch
                        val videoId = mediaId ?: return@launch
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
                            )
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
                val videoId = mediaId ?: return@LaunchedEffect
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
                    )
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
                val videoId = mediaId ?: return@LaunchedEffect
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
                    )
                )
            } catch (_: Exception) {
                // Best-effort save on pause
            }
        }
    }

    // Auto-hide controls (resets on user interaction via interactionTick)
    LaunchedEffect(isControlsVisible, playbackState.isPlaying, isSettingsSheetOpen, interactionTick) {
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

    // Auto-play next episode when near end
    LaunchedEffect(onNextEpisode != null) {
        if (onNextEpisode == null) return@LaunchedEffect
        hasTriggeredAutoPlay = false
        while (isActive) {
            delay(1000)
            if (!hasTriggeredAutoPlay && playbackState.durationMs > 0
                && playbackState.currentPositionMs >= playbackState.durationMs * 0.95) {
                hasTriggeredAutoPlay = true
                delay(1500)
                onNextEpisode?.invoke()
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

    // Immediately hide loading overlay when playback actually starts
    LaunchedEffect(playbackState.isPlaying) {
        if (playbackState.isPlaying) {
            val recentSeek = (playerTimeMillis() - lastSeekTimeMs) < 2000
            if (recentSeek) {
                showLoading = false // No delay for post-seek resume
            } else {
                delay(100)
                showLoading = false
            }
        }
    }

    // Loading state: show during initial load AND during post-seek rebuffering.
    // Only dismiss loading when playback has actually started or an error occurred.
    // Prevents the initial isLoading=false from immediately hiding the overlay.
    LaunchedEffect(playbackState.isLoading) {
        val recentSeek = (playerTimeMillis() - lastSeekTimeMs) < 2000
        if (playbackState.isLoading && (!hasStartedPlaying || recentSeek)) {
            if (hasStartedPlaying) delay(400) // Debounce for seek rebuffer only
            showLoading = true
        } else if (hasStartedPlaying || playbackState.error != null) {
            showLoading = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                if (!isScreenLocked) {
                    isControlsVisible = !isControlsVisible
                    interactionTick++
                }
            },
        contentAlignment = Alignment.Center
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
                        translationY = freeZoomOffset.y
                    )
            ) {
                VideoPlayer(
                    url = streamUrl,
                    headers = headers,
                    onPlaybackStateChanged = onStateChanged,
                    requestedAction = requestedAction,
                    onActionConsumed = { requestedAction = null },
                    brightness = brightness,
                    videoScale = videoScale,
                    subtitleBottomMargin = if (isControlsVisible) 110 else 0,
                    subtitleStyle = subtitleStyle,
                    drmLicenseUrl = activeStream.drmLicenseUrl,
                    drmScheme = activeStream.drmScheme,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Loading bar at top (only during initial load, not during playback streaming)
            if (playbackState.isLoading && !hasStartedPlaying) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
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
                        .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "Screen Locked",
                            color = Color.White.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Double-tap to unlock",
                            color = Color.White.copy(alpha = 0.45f),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
                // Gesture layer over full screen for unlock
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = { isScreenLocked = false }
                            )
                        }
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
                                        y = (freeZoomOffset.y + pan.y).coerceIn(-maxPanY, maxPanY)
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
                                }
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
                                                .coerceIn(0L, playbackState.durationMs.coerceAtLeast(0L))
                                        )
                                        interactionTick++
                                    }
                                }
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
                                            (playbackState.currentPositionMs - seekMs).coerceAtLeast(0L)
                                        )
                                        seekIndicatorText = "-${seekIncrement}s"
                                        interactionTick++
                                    } else if (offset.x > w * 0.7f) {
                                        lastSeekTimeMs = playerTimeMillis()
                                        requestedAction = PlayerAction.SeekTo(
                                            (playbackState.currentPositionMs + seekMs).coerceAtMost(playbackState.durationMs)
                                        )
                                        seekIndicatorText = "+${seekIncrement}s"
                                        interactionTick++
                                    }
                                },
                                onTap = {
                                    isControlsVisible = !isControlsVisible
                                    interactionTick++
                                }
                            )
                        }
                )
            }

            // Transient volume indicator (left side) with slide animation
            AnimatedVisibility(
                visible = transientVolume != null,
                enter = slideInHorizontally { -it } + fadeIn(tween(150)),
                exit = slideOutHorizontally { -it } + fadeOut(tween(250)),
                modifier = Modifier.fillMaxHeight().align(Alignment.CenterStart)
            ) {
                SideSliderIndicator(
                    value = transientVolume ?: 0f,
                    icon = if ((transientVolume ?: 0f) <= 0f) Icons.Default.VolumeDown
                           else Icons.Default.VolumeUp,
                )
            }

            // Transient brightness indicator (right side) with slide animation
            AnimatedVisibility(
                visible = transientBrightness != null,
                enter = slideInHorizontally { it } + fadeIn(tween(150)),
                exit = slideOutHorizontally { it } + fadeOut(tween(250)),
                modifier = Modifier.fillMaxHeight().align(Alignment.CenterEnd)
            ) {
                SideSliderIndicator(
                    value = transientBrightness ?: 0f,
                    icon = if ((transientBrightness ?: 0f) < 0.3f) Icons.Default.KeyboardArrowDown
                           else Icons.Default.KeyboardArrowUp,
                )
            }

            PlayerControls(
                isVisible = isControlsVisible && !isScreenLocked,
                isPlaying = playbackState.isPlaying,
                title = title,
                seekIncrement = seekIncrement,
                onSeekIndicatorChange = { seekIndicatorText = it },
                onPlayPauseToggle = {
                    requestedAction = if (playbackState.isPlaying) PlayerAction.Pause else PlayerAction.Play
                    interactionTick++
                },
                onBackClick = onBackClick,
                onSeek = {
                    lastSeekTimeMs = playerTimeMillis()
                    requestedAction = PlayerAction.SeekTo((it * playbackState.durationMs).toLong())
                    interactionTick++
                },
                progress = if (playbackState.durationMs > 0) playbackState.currentPositionMs.toFloat() / playbackState.durationMs else 0f,
                duration = playbackState.durationMs,
                currentTime = playbackState.currentPositionMs,
                onSpeedChange = { requestedAction = PlayerAction.SetSpeed(it) },
                onAudioTrackChange = { groupIndex, trackIndex ->
                    requestedAction = PlayerAction.SelectAudioTrack(groupIndex, trackIndex)
                    currentAudioGroupIndex = groupIndex
                    currentAudioTrackIndex = trackIndex
                    periodicSaveScope.launch {
                        val pId = profileRepository.activeProfile.value?.id ?: return@launch
                        val vId = mediaId ?: return@launch
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
                            )
                        )
                    }
                },
                onSubtitleTrackChange = { groupIndex, trackIndex ->
                    requestedAction = PlayerAction.SelectSubtitleTrack(groupIndex, trackIndex)
                    currentSubtitleGroupIndex = groupIndex
                    currentSubtitleTrackIndex = trackIndex
                    periodicSaveScope.launch {
                        val pId = profileRepository.activeProfile.value?.id ?: return@launch
                        val vId = mediaId ?: return@launch
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
                            )
                        )
                    }
                },
                onScaleChange = { videoScale = it; requestedAction = PlayerAction.SetScale(it) },
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
                subtitleTracks = playbackState.subtitleTracks,
                currentSpeed = playbackState.playbackSpeed,
                currentStream = activeStream,
                streams = sortedStreams,
                onStreamChange = { newStream ->
                    pendingStreamSwitchPosition = playbackState.currentPositionMs
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
                onSubtitleStyleChange = { subtitleStyle = it },
                videoResolution = videoResolutionLabel,
                videoCodec = playbackState.videoCodec,
                videoBitrate = playbackState.videoBitrate,
                audioBitrate = playbackState.audioBitrate,
                chapters = playbackState.chapters,
                showDebugOverlay = showDebugOverlay,
                onToggleDebug = { showDebugOverlay = !showDebugOverlay },
                modifier = Modifier.fillMaxSize()
            )

            // Seek indicator overlay (on top of controls, positioned at sides like YouTube)
            if (seekIndicatorText != null) {
                val isRewind = seekIndicatorText!!.startsWith("-")
                Box(
                    modifier = Modifier
                        .align(if (isRewind) Alignment.CenterStart else Alignment.CenterEnd)
                        .padding(horizontal = 48.dp)
                        .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 28.dp, vertical = 20.dp)
                ) {
                    Text(
                        text = seekIndicatorText!!,
                        color = Color.White,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
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
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Premium Error Overlay (only for actual errors)
            if (playbackState.error != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.85f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { /* Intercept and absorb clicks */ },
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF161616),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .width(420.dp)
                            .padding(24.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        Color.Red.copy(alpha = 0.15f),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Playback Failed",
                                    tint = Color.Red,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            Text(
                                text = "Playback Failed",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text = "An error occurred: ${playbackState.error}. You can try playing this stream in an external player, copy the direct link, or choose another source.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )

                            if (!feedbackMessage.isNullOrBlank()) {
                                Text(
                                    text = feedbackMessage!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MovieHubColors.Success,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Button(
                                    onClick = {
                                        try {
                                            uriHandler.openUri(streamUrl)
                                        } catch (e: Exception) {
                                            feedbackMessage = "No compatible player found"
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1.5f)
                                ) {
                                    Text("Play Externally", fontWeight = FontWeight.Bold, maxLines = 1)
                                }

                                OutlinedButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(streamUrl))
                                        feedbackMessage = "Link copied to clipboard!"
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                                    modifier = Modifier.weight(1.2f)
                                ) {
                                    Text("Copy Link", maxLines = 1)
                                }
                            }

                            TextButton(
                                onClick = onBackClick,
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = Color.White.copy(alpha = 0.6f)
                                )
                            ) {
                                Text("Go Back")
                            }
                        }
                    }
                }
            }
        } else {
            Text(
                text = "Invalid Stream URL",
                color = MaterialTheme.colorScheme.error
            )
        }

        // Loading overlay — drawn after all video content so it renders on top
        if (showLoading && playbackState.error == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 4.dp,
                    modifier = Modifier.size(48.dp)
                )
            }
        }
    }
}

@Composable
fun SideSliderIndicator(
    value: Float,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(44.dp)
            .padding(vertical = 80.dp)
            .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(22.dp))
            .padding(horizontal = 8.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxHeight()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(16.dp)
            )

            // Vertical track (pill)
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .weight(1f)
                    .background(
                        Color.White.copy(alpha = 0.12f),
                        RoundedCornerShape(2.dp)
                    ),
                contentAlignment = Alignment.BottomCenter
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(value.coerceIn(0f, 1f))
                        .background(
                            MaterialTheme.colorScheme.primary,
                            RoundedCornerShape(2.dp)
                        )
                )
            }

            // Percentage label
            Text(
                text = "${(value.coerceIn(0f, 1f) * 100).toInt()}%",
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium
                )
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
    modifier: Modifier = Modifier
) {
    val streamUrl = stream?.url ?: stream?.externalUrl ?: ""
    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.8f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "🎬 Debug Info",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            HorizontalDivider(color = Color.White.copy(alpha = 0.2f))

            DebugRow("Title", stream?.addonName ?: stream?.name ?: "N/A")
            DebugRow("Source URL", streamUrl)
            DebugRow("Info Hash", stream?.infoHash ?: "N/A")
            DebugRow("File Index", stream?.fileIdx?.toString() ?: "N/A")

            HorizontalDivider(color = Color.White.copy(alpha = 0.15f))

            val resStr = if (playbackState.videoWidth > 0 && playbackState.videoHeight > 0) {
                "${playbackState.videoWidth}x${playbackState.videoHeight}"
            } else "N/A"
            DebugRow("Resolution", resStr)
            DebugRow("Codec", playbackState.videoCodec ?: "N/A")

            val bitrateStr = when {
                playbackState.videoBitrate >= 1_000_000 -> "${playbackState.videoBitrate / 1_000_000}.${(playbackState.videoBitrate % 1_000_000) / 100_000} Mbps"
                playbackState.videoBitrate > 0 -> "${playbackState.videoBitrate / 1000} kbps"
                else -> "N/A"
            }
            DebugRow("Video Bitrate", bitrateStr)
            DebugRow("Audio Bitrate", if (playbackState.audioBitrate > 0) "${playbackState.audioBitrate / 1000} kbps" else "N/A")
            DebugRow("Video Scale", videoScale.label)

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

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Tap anywhere to dismiss",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun DebugRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.weight(0.35f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(0.65f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "${hours}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    } else {
        "${minutes}:${seconds.toString().padStart(2, '0')}"
    }
}
