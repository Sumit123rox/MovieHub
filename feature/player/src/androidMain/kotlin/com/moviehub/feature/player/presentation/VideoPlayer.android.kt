@file:OptIn(androidx.media3.common.util.UnstableApi::class)

package com.moviehub.feature.player.presentation

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.media.AudioManager
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.text.Cue
import androidx.media3.common.text.CueGroup
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import androidx.media3.ui.SubtitleView
import com.moviehub.core.model.AudioTrack
import com.moviehub.core.model.ChapterInfo
import com.moviehub.core.model.PlayerPlaybackState
import com.moviehub.core.model.SubtitleStyle
import com.moviehub.core.model.SubtitleTrack
import com.moviehub.core.model.VideoScale
import com.moviehub.core.model.VideoTrack
import com.moviehub.core.player.MoviePlayer
import com.moviehub.core.player.MoviePlayerController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.Protocol
import org.koin.compose.koinInject
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.milliseconds

class AndroidMoviePlayer(
    private val context: Context,
    private val exoPlayer: ExoPlayer,
    private val getPlayerView: () -> PlayerView?,
    private val getCues: () -> List<Cue>?,
) : MoviePlayer {
    private val _playbackState = MutableStateFlow(PlayerPlaybackState())
    override val playbackState: StateFlow<PlayerPlaybackState> = _playbackState.asStateFlow()

    fun updateState(state: PlayerPlaybackState) {
        _playbackState.value = _playbackState.value.copy(
            isPlaying = state.isPlaying,
            isLoading = state.isLoading,
            error = state.error ?: _playbackState.value.error,
            currentPositionMs = state.currentPositionMs,
            durationMs = state.durationMs,
            bufferedPositionMs = state.bufferedPositionMs,
            playbackSpeed = state.playbackSpeed,
            selectedAudioTrackIndex = if (state.audioTracks.isNotEmpty()) state.selectedAudioTrackIndex else _playbackState.value.selectedAudioTrackIndex,
            selectedSubtitleTrackIndex = if (state.subtitleTracks.isNotEmpty()) state.selectedSubtitleTrackIndex else _playbackState.value.selectedSubtitleTrackIndex,
            audioTracks = if (state.audioTracks.isNotEmpty()) state.audioTracks else _playbackState.value.audioTracks,
            subtitleTracks = if (state.subtitleTracks.isNotEmpty()) state.subtitleTracks else _playbackState.value.subtitleTracks,
            videoTracks = if (state.videoTracks.isNotEmpty()) state.videoTracks else _playbackState.value.videoTracks,
            videoWidth = if (state.videoWidth > 0) state.videoWidth else _playbackState.value.videoWidth,
            videoHeight = if (state.videoHeight > 0) state.videoHeight else _playbackState.value.videoHeight,
            videoCodec = state.videoCodec ?: _playbackState.value.videoCodec,
            videoBitrate = if (state.videoBitrate > 0) state.videoBitrate else _playbackState.value.videoBitrate,
            audioBitrate = if (state.audioBitrate > 0) state.audioBitrate else _playbackState.value.audioBitrate,
            audioCodec = state.audioCodec ?: _playbackState.value.audioCodec,
            audioSampleRate = if (state.audioSampleRate > 0) state.audioSampleRate else _playbackState.value.audioSampleRate,
            audioChannels = if (state.audioChannels > 0) state.audioChannels else _playbackState.value.audioChannels,
            hdrMode = state.hdrMode ?: _playbackState.value.hdrMode,
            decoderName = state.decoderName ?: _playbackState.value.decoderName,
            hardwareDecoding = state.hardwareDecoding ?: _playbackState.value.hardwareDecoding,
            demuxerName = state.demuxerName ?: _playbackState.value.demuxerName,
            containerFormat = state.containerFormat ?: _playbackState.value.containerFormat,
            displayFps = state.displayFps ?: _playbackState.value.displayFps,
            chapters = if (state.chapters.isNotEmpty()) state.chapters else _playbackState.value.chapters,
            currentCueCount = state.currentCueCount,
        )
    }

    override fun play() {
        exoPlayer.play()
    }

    override fun pause() {
        exoPlayer.pause()
    }

    override fun seekTo(positionMs: Long) {
        exoPlayer.seekTo(positionMs)
    }

    override fun setSpeed(speed: Float) {
        exoPlayer.playbackParameters = PlaybackParameters(speed)
    }

    override fun setVolume(volume: Float) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val newVolume = (volume.coerceIn(0f, 1f) * max).toInt().coerceIn(0, max)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, AudioManager.FLAG_SHOW_UI)
    }

    override fun selectAudioTrack(trackIndex: Int) {
        val track = playbackState.value.audioTracks.getOrNull(trackIndex) ?: return
        val groups = exoPlayer.currentTracks.groups
        val groupIndex = track.index
        val innerTrackIndex = track.id.toIntOrNull() ?: return
        if (groupIndex in 0 until groups.size) {
            val trackGroup = groups[groupIndex]
            if (trackGroup.type == C.TRACK_TYPE_AUDIO && innerTrackIndex in 0 until trackGroup.length) {
                exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                    .buildUpon()
                    .setOverrideForType(
                        TrackSelectionOverride(
                            trackGroup.mediaTrackGroup,
                            innerTrackIndex,
                        ),
                    )
                    .build()
            }
        }
    }

    override fun selectSubtitleTrack(trackIndex: Int) {
        if (trackIndex == -1) {
            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                .buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                .build()
        } else {
            val track = playbackState.value.subtitleTracks.getOrNull(trackIndex) ?: return
            val groups = exoPlayer.currentTracks.groups
            val groupIndex = track.index
            val innerTrackIndex = track.id.toIntOrNull() ?: return
            if (groupIndex in 0 until groups.size) {
                val trackGroup = groups[groupIndex]
                if (trackGroup.type == C.TRACK_TYPE_TEXT && innerTrackIndex in 0 until trackGroup.length) {
                    exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                        .buildUpon()
                        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                        .setOverrideForType(
                            TrackSelectionOverride(
                                trackGroup.mediaTrackGroup,
                                innerTrackIndex,
                            ),
                        )
                        .build()
                    getCues()?.let { cues ->
                        getPlayerView()?.subtitleView?.let { sv ->
                            sv.setCues(cues)
                            sv.setVisibility(android.view.View.VISIBLE)
                            sv.invalidate()
                        }
                    }
                }
            }
        }
    }

    override fun selectVideoTrack(trackIndex: Int) {
        if (trackIndex == -1) {
            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                .buildUpon()
                .clearOverridesOfType(C.TRACK_TYPE_VIDEO)
                .build()
        } else {
            val track = playbackState.value.videoTracks.getOrNull(trackIndex) ?: return
            val groups = exoPlayer.currentTracks.groups
            val groupIndex = track.index
            val innerTrackIndex = track.id.toIntOrNull() ?: return
            if (groupIndex in 0 until groups.size) {
                val trackGroup = groups[groupIndex]
                if (trackGroup.type == C.TRACK_TYPE_VIDEO && innerTrackIndex in 0 until trackGroup.length) {
                    exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                        .buildUpon()
                        .setOverrideForType(
                            TrackSelectionOverride(
                                trackGroup.mediaTrackGroup,
                                innerTrackIndex,
                            ),
                        )
                        .build()
                }
            }
        }
    }

    override fun setVideoScale(scale: VideoScale) {
        getPlayerView()?.setResizeMode(
            when (scale) {
                VideoScale.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                VideoScale.FILL -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                VideoScale.ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                VideoScale.STRETCH -> AspectRatioFrameLayout.RESIZE_MODE_FILL
            },
        )
    }

    override fun setSubtitleStyle(style: SubtitleStyle) {
        getPlayerView()?.subtitleView?.let { sv ->
            applySubtitleStyleToView(sv, style)
        }
    }

    override fun enterPip() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            try {
                var current = context
                var activity: Activity? = null
                while (current is android.content.ContextWrapper) {
                    if (current is Activity) {
                        activity = current
                        break
                    }
                    current = current.baseContext
                }
                if (current is Activity) activity = current

                if (activity != null) {
                    val videoFormat = exoPlayer.videoFormat
                    val rational = if (videoFormat != null && videoFormat.width > 0 && videoFormat.height > 0) {
                        val aspect = videoFormat.width.toFloat() / videoFormat.height.toFloat()
                        if (aspect in 0.4184f..2.39f) {
                            android.util.Rational(videoFormat.width, videoFormat.height)
                        } else {
                            null
                        }
                    } else {
                        null
                    }
                    val builder = android.app.PictureInPictureParams.Builder()
                    if (rational != null) builder.setAspectRatio(rational)
                    activity.enterPictureInPictureMode(builder.build())
                }
            } catch (_: Exception) {
            }
        }
    }

    override fun release() {}
}

@OptIn(UnstableApi::class)
@Composable
actual fun VideoPlayer(
    url: String,
    headers: Map<String, String>,
    onPlaybackStateChanged: (PlayerPlaybackState) -> Unit,
    forceLandscape: Boolean,
    brightness: Float,
    videoScale: VideoScale,
    subtitleBottomMargin: Int,
    subtitleStyle: SubtitleStyle,
    drmLicenseUrl: String?,
    drmScheme: String?,
    modifier: Modifier,
) {
    val playerController: MoviePlayerController = koinInject()
    val context = LocalContext.current
    val activity = remember(context) {
        var current = context
        var act: Activity? = null
        while (current is ContextWrapper) {
            if (current is Activity) {
                act = current
                break
            }
            current = current.baseContext
        }
        if (current is Activity) act = current
        act
    }
    DisposableEffect(forceLandscape) {
        if (forceLandscape) {
            applyLandscapeLock(context)
        }

        // Immersive Fullscreen Mode (only in forced landscape)
        val window = activity?.window
        var previousBehavior = 0
        var controller: WindowInsetsControllerCompat? = null
        if (forceLandscape && window != null) {
            controller =
                androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
            previousBehavior = controller.systemBarsBehavior
            controller.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        onDispose {
            // Orientation never restored here — PlayerScreen.kt handles the final
            // restore via applyOrientationRestore() in its own DisposableEffect.
            // This prevents the landscape→portrait→landscape flicker on source switch.
            if (window != null && controller != null) {
                controller.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior = previousBehavior
            }
        }
    }

    // Apply screen brightness and sync to system brightness settings
    LaunchedEffect(brightness) {
        activity?.window?.let { window ->
            val lp = window.attributes
            lp.screenBrightness = brightness.coerceIn(0f, 1f)
            window.attributes = lp
        }
        // Sync with system brightness setting if permission is granted
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M &&
                android.provider.Settings.System.canWrite(context)
            ) {
                android.provider.Settings.System.putInt(
                    context.contentResolver,
                    android.provider.Settings.System.SCREEN_BRIGHTNESS,
                    (brightness.coerceIn(0f, 1f) * 255).toInt().coerceIn(0, 255),
                )
            }
        } catch (_: Exception) {
        }
    }

    val okHttpClient = remember {
        OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS) // faster failure for dead hosts
            .readTimeout(30, TimeUnit.SECONDS) // proxy extractors need time for redirect chains
            .writeTimeout(3, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .connectionPool(ConnectionPool(10, 10, TimeUnit.MINUTES)) // larger pool for proxy redirects
            .protocols(listOf(okhttp3.Protocol.HTTP_2, okhttp3.Protocol.HTTP_1_1))
            .build()
    }

    // Rebuild ExoPlayer when headers change (source switch) so the data source factory uses new proxy headers
    val exoPlayer = remember(headers) {
        val httpDataSourceFactory = OkHttpDataSource.Factory(okHttpClient)
            .setDefaultRequestProperties(headers)

        val loadControl = androidx.media3.exoplayer.DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                // minBufferMs — minimum before playback considered "ready"
                15_000,
                // maxBufferMs — maximum to buffer ahead
                60_000,
                // bufferForPlaybackMs — initial buffer before starting
                1_500,
                // bufferForPlaybackAfterRebufferMs — after a stall
                2_000,
            )
            .setPrioritizeTimeOverSizeThresholds(true) // start sooner, buffer less
            .setBackBuffer(15_000, false)
            .build()

        val mediaSourceFactory = DefaultMediaSourceFactory(context)
            .setDataSourceFactory(httpDataSourceFactory)

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()

        val builder = ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .setAudioAttributes(audioAttributes, true)

        builder.build()
    }

    // MediaSession for lock screen controls and media notification
    val mediaSession = remember(exoPlayer) {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val sessionId = "moviehub_session_" + java.util.UUID.randomUUID().toString()
        if (launchIntent != null) {
            val pendingIntent = android.app.PendingIntent.getActivity(
                context, 0, launchIntent,
                android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT,
            )
            MediaSession.Builder(context, exoPlayer)
                .setId(sessionId)
                .setSessionActivity(pendingIntent)
                .build()
        } else {
            MediaSession.Builder(context, exoPlayer)
                .setId(sessionId)
                .build()
        }
    }

    DisposableEffect(mediaSession) {
        onDispose { mediaSession.release() }
    }

    // Safely load and prepare stream media inside a LaunchedEffect to prevent crashes
    LaunchedEffect(exoPlayer, url) {
        if (url.isBlank()) {
            onPlaybackStateChanged(
                PlayerPlaybackState(
                    isPlaying = false,
                    isLoading = false,
                    error = "Failed to load stream: Stream URL is blank",
                ),
            )
            return@LaunchedEffect
        }

        try {
            // Reset track selection params on source switch to clear old overrides
            exoPlayer.trackSelectionParameters =
                androidx.media3.common.TrackSelectionParameters.getDefaults(context)
            // Build MediaItem with proxy headers applied to the request
            val mediaItem = if (drmLicenseUrl != null) {
                val drmUuid = when (drmScheme?.lowercase()) {
                    "widevine" -> androidx.media3.common.C.WIDEVINE_UUID
                    "clearkey" -> androidx.media3.common.C.CLEARKEY_UUID
                    else -> androidx.media3.common.C.WIDEVINE_UUID
                }
                androidx.media3.common.MediaItem.Builder()
                    .setUri(url)
                    .setDrmConfiguration(
                        androidx.media3.common.MediaItem.DrmConfiguration.Builder(drmUuid)
                            .setLicenseUri(drmLicenseUrl)
                            .build(),
                    )
                    .build()
            } else {
                androidx.media3.common.MediaItem.Builder()
                    .setUri(url)
                    .setCustomCacheKey(url)
                    .build()
            }
            exoPlayer.setMediaItem(mediaItem)

            // Preconnect with proxy headers — warm extractor, then warm the final CDN
            try {
                withContext(Dispatchers.IO) {
                    val headRequest = okhttp3.Request.Builder()
                        .url(url).head()
                        .apply { headers.forEach { (k, v) -> addHeader(k, v) } }
                        .build()
                    val response = okHttpClient.newCall(headRequest).execute()
                    val finalUrl = response.request.url.toString()
                    response.close()
                    // Pre-warm actual media CDN (post-redirect)
                    if (finalUrl != url) {
                        okHttpClient.newCall(
                            okhttp3.Request.Builder().url(finalUrl).head().build(),
                        ).execute().close()
                    }
                }
            } catch (_: Exception) {
                // Best-effort preconnect
            }
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
        } catch (e: Exception) {
            onPlaybackStateChanged(
                PlayerPlaybackState(
                    isPlaying = false,
                    isLoading = false,
                    error = "Failed to load stream: ${e.localizedMessage ?: e.message ?: "Invalid Stream URL"}",
                ),
            )
        }
    }

    // Track PlayerView for resize mode, subtitle, and action handling
    // Declared early so lambdas inside DisposableEffect can reference it
    var playerView by remember { mutableStateOf<PlayerView?>(null) }
    var lastCueList by remember { mutableStateOf<List<Cue>?>(null) }

    val platformPlayer = remember(exoPlayer) {
        AndroidMoviePlayer(
            context = context,
            exoPlayer = exoPlayer,
            getPlayerView = { playerView },
            getCues = { lastCueList },
        )
    }

    DisposableEffect(platformPlayer) {
        playerController.registerPlayer(platformPlayer)
        onDispose {
            playerController.unregisterPlayer(platformPlayer)
        }
    }

    val interceptedStateChanged: (PlayerPlaybackState) -> Unit = { state ->
        onPlaybackStateChanged(state)
        platformPlayer.updateState(state)
    }

    // Efficient state updates using Listener instead of polling
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onCues(cueGroup: CueGroup) {
                lastCueList = cueGroup.cues
                // Force-push cues to subtitle view in case PlayerView misses them
                playerView?.subtitleView?.let { sv ->
                    sv.setCues(cueGroup.cues)
                    sv.setVisibility(android.view.View.VISIBLE)
                    sv.invalidate()
                }
            }

            override fun onEvents(
                player: Player,
                events: Player.Events,
            ) {
                val hasTrackChanges = events.contains(Player.EVENT_TRACKS_CHANGED) ||
                    events.contains(Player.EVENT_MEDIA_METADATA_CHANGED) ||
                    events.contains(Player.EVENT_PLAYBACK_STATE_CHANGED)
                val result = if (hasTrackChanges) extractTracks(player) else null
                val currentCues = player.currentCues.cues
                val cueCount = currentCues.size

                interceptedStateChanged(
                    PlayerPlaybackState(
                        isPlaying = player.isPlaying,
                        isLoading = player.isLoading,
                        error = player.playerError?.message ?: player.playerError?.localizedMessage,
                        currentPositionMs = player.currentPosition,
                        durationMs = player.duration.coerceAtLeast(0L),
                        bufferedPositionMs = player.bufferedPosition,
                        playbackSpeed = player.playbackParameters.speed,
                        selectedAudioTrackIndex = if (result != null) result.selectedAudioTrackIndex else platformPlayer.playbackState.value.selectedAudioTrackIndex,
                        selectedSubtitleTrackIndex = if (result != null) result.selectedSubtitleTrackIndex else platformPlayer.playbackState.value.selectedSubtitleTrackIndex,
                        selectedVideoTrackIndex = if (result != null) result.selectedVideoTrackIndex else platformPlayer.playbackState.value.selectedVideoTrackIndex,
                        audioTracks = if (result != null) result.audioTracks else platformPlayer.playbackState.value.audioTracks,
                        subtitleTracks = if (result != null) result.subtitleTracks else platformPlayer.playbackState.value.subtitleTracks,
                        videoTracks = if (result != null) result.videoTracks else platformPlayer.playbackState.value.videoTracks,
                        videoWidth = if (result != null && result.videoWidth > 0) result.videoWidth else platformPlayer.playbackState.value.videoWidth,
                        videoHeight = if (result != null && result.videoHeight > 0) result.videoHeight else platformPlayer.playbackState.value.videoHeight,
                        videoCodec = if (result != null) result.videoCodec else platformPlayer.playbackState.value.videoCodec,
                        videoBitrate = if (result != null && result.videoBitrate > 0) result.videoBitrate else platformPlayer.playbackState.value.videoBitrate,
                        audioBitrate = if (result != null && result.audioBitrate > 0) result.audioBitrate else platformPlayer.playbackState.value.audioBitrate,
                        audioCodec = if (result != null) result.audioCodec else platformPlayer.playbackState.value.audioCodec,
                        audioSampleRate = if (result != null && result.audioSampleRate > 0) result.audioSampleRate else platformPlayer.playbackState.value.audioSampleRate,
                        audioChannels = if (result != null && result.audioChannels > 0) result.audioChannels else platformPlayer.playbackState.value.audioChannels,
                        hdrMode = if (result != null) result.hdrMode else platformPlayer.playbackState.value.hdrMode,
                        decoderName = if (result != null) result.decoderName else platformPlayer.playbackState.value.decoderName,
                        hardwareDecoding = if (result != null) result.hardwareDecoding else platformPlayer.playbackState.value.hardwareDecoding,
                        demuxerName = if (result != null) result.demuxerName else platformPlayer.playbackState.value.demuxerName,
                        containerFormat = if (result != null) result.containerFormat else platformPlayer.playbackState.value.containerFormat,
                        displayFps = if (result != null) result.displayFps else platformPlayer.playbackState.value.displayFps,
                        chapters = if (result != null && result.chapters.isNotEmpty()) result.chapters else platformPlayer.playbackState.value.chapters,
                        currentCueCount = cueCount,
                    ),
                )
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
        }
    }

    // Lightweight position polling (updates every 1s — doesn't extract tracks, preserving onEvents data)
    LaunchedEffect(exoPlayer) {
        while (isActive) {
            if (exoPlayer.isPlaying || exoPlayer.isLoading) {
                interceptedStateChanged(
                    PlayerPlaybackState(
                        isPlaying = exoPlayer.isPlaying,
                        isLoading = exoPlayer.isLoading,
                        currentPositionMs = exoPlayer.currentPosition,
                        durationMs = exoPlayer.duration.coerceAtLeast(0L),
                        bufferedPositionMs = exoPlayer.bufferedPosition,
                        playbackSpeed = exoPlayer.playbackParameters.speed,
                    ),
                )
            }
            delay(1000.milliseconds)
        }
    }

    // Apply video scale to PlayerView
    LaunchedEffect(videoScale) {
        val view = playerView ?: return@LaunchedEffect
        view.setResizeMode(
            when (videoScale) {
                VideoScale.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                VideoScale.FILL -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                VideoScale.ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                VideoScale.STRETCH -> AspectRatioFrameLayout.RESIZE_MODE_FILL
            },
        )
    }

    // Adjust subtitle bottom padding to avoid overlap with controls
    LaunchedEffect(subtitleBottomMargin) {
        val view = playerView ?: return@LaunchedEffect
        val density = context.resources.displayMetrics.density
        val bottomPx = (subtitleBottomMargin * density).toInt()
        val subView = view.subtitleView
        if (subView != null) {
            subView.setPadding(0, 0, 0, bottomPx)
            subView.setVisibility(android.view.View.VISIBLE)
            subView.invalidate()
        }
    }

    // Apply subtitle styling
    LaunchedEffect(subtitleStyle, playerView) {
        playerView?.subtitleView?.let { sv ->
            applySubtitleStyleToView(sv, subtitleStyle)
        }
    }

    // ExoPlayer reference guard: set to null after release to prevent stale access
    // from PlayerView cleanup or LaunchedEffects that race disposal
    var playerReleased by remember { mutableStateOf(false) }

    // Auto Picture-in-Picture via onUserLeaveHint (fires on Home/Recents, not rotation)
    DisposableEffect(exoPlayer) {
        val pipListener = Runnable {
            if (exoPlayer.playWhenReady && !playerReleased) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    try {
                        val videoFormat = exoPlayer.videoFormat
                        val rational = if (videoFormat != null && videoFormat.width > 0 && videoFormat.height > 0) {
                            val aspect = videoFormat.width.toFloat() / videoFormat.height.toFloat()
                            if (aspect in 0.4184f..2.39f) {
                                android.util.Rational(videoFormat.width, videoFormat.height)
                            } else {
                                null
                            }
                        } else {
                            null
                        }
                        val builder = android.app.PictureInPictureParams.Builder()
                        if (rational != null) builder.setAspectRatio(rational)
                        activity?.enterPictureInPictureMode(builder.build())
                    } catch (_: Exception) {
                    }
                }
            }
        }
        (activity as? androidx.activity.ComponentActivity)?.addOnUserLeaveHintListener(pipListener)
        onDispose {
            (activity as? androidx.activity.ComponentActivity)?.removeOnUserLeaveHintListener(pipListener)
        }
    }

    // Safely manage ExoPlayer release lifecycle to prevent memory leaks
    DisposableEffect(exoPlayer) {
        onDispose {
            playerView?.player = null
            playerView = null
            try {
                exoPlayer.release()
            } catch (_: Exception) {
                // best-effort release
            }
            playerReleased = true
        }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = false
                playerView = this
                // Force subtitle view visible from the start
                subtitleView?.let { sv ->
                    sv.setUserDefaultStyle()
                    sv.setUserDefaultTextSize()
                    sv.setVisibility(android.view.View.VISIBLE)
                    sv.invalidate()
                }
            }
        },
        onRelease = { releasedView ->
            // Release player AFTER PlayerView is detached — prevents crash when
            // PlayerView's onDetachedFromWindow tries to access the released player
            releasedView.player = null
            playerReleased = true
        },
        modifier = modifier,
    )
}

private data class TrackExtractionResult(
    val audioTracks: List<AudioTrack>,
    val subtitleTracks: List<SubtitleTrack>,
    val videoTracks: List<VideoTrack>,
    val selectedAudioTrackIndex: Int,
    val selectedSubtitleTrackIndex: Int,
    val selectedVideoTrackIndex: Int,
    val videoWidth: Int,
    val videoHeight: Int,
    val videoCodec: String?,
    val videoBitrate: Int,
    val audioBitrate: Int,
    val audioCodec: String?,
    val audioSampleRate: Int,
    val audioChannels: Int,
    val hdrMode: String?,
    val decoderName: String?,
    val hardwareDecoding: Boolean,
    val demuxerName: String?,
    val containerFormat: String?,
    val displayFps: Float,
    val chapters: List<ChapterInfo>,
)

private fun extractTracks(player: Player): TrackExtractionResult {
    val tracks = player.currentTracks
    val audioTracks = mutableListOf<AudioTrack>()
    val subtitleTracks = mutableListOf<SubtitleTrack>()
    val videoTracks = mutableListOf<VideoTrack>()

    val trackSelectionParameters = player.trackSelectionParameters
    val hasVideoOverride = trackSelectionParameters.overrides.values.any { it.type == C.TRACK_TYPE_VIDEO }
    val videoOverride = trackSelectionParameters.overrides.values.firstOrNull { it.type == C.TRACK_TYPE_VIDEO }
    var videoWidth = 0
    var videoHeight = 0
    var videoCodec: String? = null
    var videoBitrate = 0
    var audioBitrate = 0
    var audioCodec: String? = null
    var audioSampleRate = 0
    var audioChannels = 0
    var hdrMode: String? = null
    var decoderName: String? = null
    var hardwareDecoding = false
    var displayFps = 0f

    tracks.groups.forEachIndexed { index, group ->
        val type = group.type
        for (i in 0 until group.length) {
            val format = group.getTrackFormat(i)
            val isSelected = group.isTrackSelected(i)
            val label = format.label ?: format.language ?: "Track ${i + 1}"
            if (type == C.TRACK_TYPE_AUDIO) {
                if (isSelected) {
                    if (format.bitrate > 0) audioBitrate = format.bitrate
                    audioCodec = format.codecs
                    audioSampleRate = format.sampleRate
                    audioChannels = format.channelCount
                }
                audioTracks.add(
                    AudioTrack(
                        index = index,
                        id = i.toString(),
                        label = label,
                        language = format.language,
                        isSelected = isSelected,
                        codec = format.codecs,
                        channels = format.channelCount,
                        bitrate = format.bitrate,
                    ),
                )
            } else if (type == C.TRACK_TYPE_TEXT) {
                subtitleTracks.add(
                    SubtitleTrack(
                        index,
                        i.toString(),
                        label,
                        format.language,
                        isSelected,
                    ),
                )
            } else if (type == C.TRACK_TYPE_VIDEO) {
                val isTrackOverridden = if (hasVideoOverride) {
                    videoOverride?.mediaTrackGroup == group.mediaTrackGroup && videoOverride?.trackIndices?.contains(i) == true
                } else {
                    false
                }
                videoTracks.add(
                    VideoTrack(
                        index = index,
                        id = i.toString(),
                        label = "${format.width}x${format.height}" + (if (format.frameRate > 0) " (${format.frameRate.toInt()}fps)" else ""),
                        width = format.width,
                        height = format.height,
                        codec = format.codecs,
                        bitrate = format.bitrate,
                        isSelected = isTrackOverridden,
                    ),
                )
                if (isSelected) {
                    if (format.width > 0 && format.height > 0) {
                        videoWidth = format.width
                        videoHeight = format.height
                    }
                    if (videoCodec == null) {
                        videoCodec = format.codecs
                    }
                    if (format.bitrate > 0) videoBitrate = format.bitrate
                    val colorInfo = format.colorInfo
                    if (colorInfo != null) {
                        hdrMode = if (colorInfo.colorTransfer == C.COLOR_TRANSFER_ST2084 || colorInfo.colorTransfer == C.COLOR_TRANSFER_HLG) "HDR" else "SDR"
                    }
                    val mime = format.sampleMimeType
                    if (mime != null) {
                        decoderName = mime.substringAfter("video/")
                        hardwareDecoding = !(mime.contains("google", ignoreCase = true) || mime.contains("soft", ignoreCase = true) || mime.contains("sw", ignoreCase = true))
                    }
                    displayFps = format.frameRate
                }
            }
        }
    }

    val url = player.currentMediaItem?.localConfiguration?.uri?.toString()?.lowercase() ?: ""
    val (containerFormat, demuxerName) = when {
        url.contains(".m3u8") || url.contains(".m3u") -> Pair("MPEG-TS (HLS)", "HLS Demuxer")
        url.contains(".mpd") -> Pair("DASH", "DASH Demuxer")
        url.contains(".mp4") -> Pair("MP4", "MP4 Demuxer")
        url.contains(".mkv") -> Pair("Matroska (MKV)", "Matroska Demuxer")
        url.contains(".webm") -> Pair("WebM", "WebM Demuxer")
        url.contains(".mp3") -> Pair("MP3", "MP3 Demuxer")
        else -> Pair("MPEG-4", "AAC Demuxer")
    }

    val chapters = mutableListOf<ChapterInfo>()
    try {
        val mediaMeta = player.mediaMetadata
        val chaptersField = mediaMeta::class.members.firstOrNull { it.name == "chapters" }
        val rawChapters = chaptersField?.call(mediaMeta) as? List<*>
        rawChapters?.forEach { raw ->
            if (raw != null) {
                val kclass = raw::class
                val titleMember = kclass.members.firstOrNull { it.name == "title" || it.name == "name" }
                val startMember = kclass.members.firstOrNull { it.name == "startTimeMs" }
                val endMember = kclass.members.firstOrNull { it.name == "endTimeMs" }
                val chTitle = titleMember?.call(raw)?.toString()
                val startMs = (startMember?.call(raw) as? Number)?.toLong() ?: 0L
                val endMs = (endMember?.call(raw) as? Number)?.toLong() ?: 0L
                if (chTitle != null) chapters.add(ChapterInfo(title = chTitle, startMs = startMs, endMs = endMs))
            }
        }
    } catch (_: Exception) {
    }

    val selectedAudioTrackIndex = audioTracks.indexOfFirst { it.isSelected }
    val selectedSubtitleTrackIndex = subtitleTracks.indexOfFirst { it.isSelected }
    val selectedVideoTrackIndex = if (!hasVideoOverride) -1 else videoTracks.indexOfFirst { it.isSelected }

    return TrackExtractionResult(
        audioTracks = audioTracks,
        subtitleTracks = subtitleTracks,
        videoTracks = videoTracks,
        selectedAudioTrackIndex = selectedAudioTrackIndex,
        selectedSubtitleTrackIndex = selectedSubtitleTrackIndex,
        selectedVideoTrackIndex = selectedVideoTrackIndex,
        videoWidth = videoWidth,
        videoHeight = videoHeight,
        videoCodec = videoCodec,
        videoBitrate = videoBitrate,
        audioBitrate = audioBitrate,
        audioCodec = audioCodec,
        audioSampleRate = audioSampleRate,
        audioChannels = audioChannels,
        hdrMode = hdrMode,
        decoderName = decoderName,
        hardwareDecoding = hardwareDecoding,
        demuxerName = demuxerName,
        containerFormat = containerFormat,
        displayFps = displayFps,
        chapters = chapters,
    )
}

private fun applySubtitleStyleToView(
    sv: SubtitleView,
    subtitleStyle: SubtitleStyle,
) {
    // Force Canvas view type to support custom font typefaces (WebView drops custom typefaces)
    sv.setViewType(SubtitleView.VIEW_TYPE_CANVAS)

    // Explicitly bypass embedded font styles/sizes to enforce user custom subtitle selections
    sv.setApplyEmbeddedStyles(false)
    sv.setApplyEmbeddedFontSizes(false)

    val typeface = when (subtitleStyle.fontFamily) {
        "Serif" -> android.graphics.Typeface.SERIF
        "Monospace" -> android.graphics.Typeface.MONOSPACE
        "Cursive" -> android.graphics.Typeface.create("cursive", android.graphics.Typeface.NORMAL)
        else -> android.graphics.Typeface.SANS_SERIF
    }
    val style = when {
        subtitleStyle.isBold && subtitleStyle.isItalic -> android.graphics.Typeface.BOLD_ITALIC
        subtitleStyle.isBold -> android.graphics.Typeface.BOLD
        subtitleStyle.isItalic -> android.graphics.Typeface.ITALIC
        else -> android.graphics.Typeface.NORMAL
    }
    val finalTypeface = android.graphics.Typeface.create(typeface, style)

    val baseBgColor = subtitleStyle.bgColorArgb
    val bgAlpha = (subtitleStyle.bgOpacity * 255).toInt().coerceIn(0, 255)
    val bgColor = if (baseBgColor == 0) {
        android.graphics.Color.argb(bgAlpha, 0, 0, 0)
    } else {
        android.graphics.Color.argb(
            bgAlpha,
            android.graphics.Color.red(baseBgColor),
            android.graphics.Color.green(baseBgColor),
            android.graphics.Color.blue(baseBgColor),
        )
    }

    val edgeType = when (subtitleStyle.edgeStyle) {
        "None" -> CaptionStyleCompat.EDGE_TYPE_NONE
        "Outline" -> CaptionStyleCompat.EDGE_TYPE_OUTLINE
        "Shadow" -> CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW
        else -> CaptionStyleCompat.EDGE_TYPE_NONE
    }

    val edgeColor = when (subtitleStyle.edgeStyle) {
        "Outline" -> subtitleStyle.outlineColorArgb
        "Shadow" -> subtitleStyle.shadowColorArgb
        else -> android.graphics.Color.TRANSPARENT
    }

    sv.setStyle(
        CaptionStyleCompat(
            subtitleStyle.fontColorArgb,
            bgColor,
            android.graphics.Color.TRANSPARENT,
            edgeType,
            edgeColor,
            finalTypeface,
        ),
    )
    sv.setFixedTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, subtitleStyle.fontSizeSp.toFloat())
    sv.invalidate()
}
