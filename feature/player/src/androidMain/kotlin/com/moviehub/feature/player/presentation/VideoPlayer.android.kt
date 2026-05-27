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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.text.Cue
import androidx.media3.common.text.CueGroup
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.moviehub.core.model.AudioTrack
import com.moviehub.core.model.ChapterInfo
import com.moviehub.core.model.PlayerPlaybackState
import com.moviehub.core.model.SubtitleStyle
import com.moviehub.core.model.SubtitleTrack
import com.moviehub.core.model.VideoScale
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.session.MediaSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
@OptIn(UnstableApi::class)
@Composable
actual fun VideoPlayer(
    url: String,
    headers: Map<String, String>,
    onPlaybackStateChanged: (PlayerPlaybackState) -> Unit,
    requestedAction: PlayerAction?,
    onActionConsumed: () -> Unit,
    forceLandscape: Boolean,
    brightness: Float,
    videoScale: VideoScale,
    subtitleBottomMargin: Int,
    subtitleStyle: SubtitleStyle,
    drmLicenseUrl: String?,
    drmScheme: String?,
    modifier: Modifier
) {
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
        val originalOrientation = activity?.requestedOrientation
            ?: android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        if (forceLandscape) {
            activity?.requestedOrientation =
                android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
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
            if (forceLandscape) {
                activity?.requestedOrientation = originalOrientation
            }
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
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M
                && android.provider.Settings.System.canWrite(context)
            ) {
                android.provider.Settings.System.putInt(
                    context.contentResolver,
                    android.provider.Settings.System.SCREEN_BRIGHTNESS,
                    (brightness.coerceIn(0f, 1f) * 255).toInt().coerceIn(0, 255)
                )
            }
        } catch (_: Exception) { }
    }

    val okHttpClient = remember {
        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .connectionPool(ConnectionPool(5, 5, TimeUnit.MINUTES))
            .build()
    }

    // Rebuild ExoPlayer when headers change (source switch) so the data source factory uses new proxy headers
    val exoPlayer = remember(headers) {
        val httpDataSourceFactory = OkHttpDataSource.Factory(okHttpClient)
            .setDefaultRequestProperties(headers)

        val loadControl = androidx.media3.exoplayer.DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs */ 50_000,
                /* maxBufferMs */ 120_000,
                /* bufferForPlaybackMs */ 2_500,
                /* bufferForPlaybackAfterRebufferMs */ 5_000
            )
            .setPrioritizeTimeOverSizeThresholds(false)
            .setBackBuffer(30_000, false)
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
        if (launchIntent != null) {
            val pendingIntent = android.app.PendingIntent.getActivity(
                context, 0, launchIntent,
                android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
            )
            MediaSession.Builder(context, exoPlayer)
                .setSessionActivity(pendingIntent)
                .build()
        } else {
            MediaSession.Builder(context, exoPlayer).build()
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
                    error = "Failed to load stream: Stream URL is blank"
                )
            )
            return@LaunchedEffect
        }

        try {
            // Reset track selection params on source switch to clear old overrides
            exoPlayer.trackSelectionParameters =
                androidx.media3.common.TrackSelectionParameters.getDefaults(context)
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
                            .build()
                    )
                    .build()
            } else {
                MediaItem.fromUri(url)
            }
            exoPlayer.setMediaItem(mediaItem)
            // Preconnect: warm DNS + TCP + TLS before prepare()
            try {
                withContext(Dispatchers.IO) {
                    val headRequest = okhttp3.Request.Builder()
                        .url(url)
                        .head()
                        .build()
                    okHttpClient.newCall(headRequest).execute().close()
                }
            } catch (_: Exception) {
                // Best-effort preconnect — media will still load via normal prepare()
            }
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
        } catch (e: Exception) {
            onPlaybackStateChanged(
                PlayerPlaybackState(
                    isPlaying = false,
                    isLoading = false,
                    error = "Failed to load stream: ${e.localizedMessage ?: e.message ?: "Invalid Stream URL"}"
                )
            )
        }
    }

    // Track PlayerView for resize mode, subtitle, and action handling
    // Declared early so lambdas inside DisposableEffect can reference it
    var playerView by remember { mutableStateOf<PlayerView?>(null) }
    var lastCueList by remember { mutableStateOf<List<Cue>?>(null) }

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

            override fun onEvents(player: Player, events: Player.Events) {
                val hasTrackChanges = events.contains(Player.EVENT_TRACKS_CHANGED)
                        || events.contains(Player.EVENT_MEDIA_METADATA_CHANGED)
                val result = if (hasTrackChanges) extractTracks(player) else null
                val currentCues = player.currentCues.cues
                val cueCount = currentCues.size

                onPlaybackStateChanged(
                    PlayerPlaybackState(
                        isPlaying = player.isPlaying,
                        isLoading = player.isLoading,
                        error = player.playerError?.message ?: player.playerError?.localizedMessage,
                        currentPositionMs = player.currentPosition,
                        durationMs = player.duration.coerceAtLeast(0L),
                        bufferedPositionMs = player.bufferedPosition,
                        playbackSpeed = player.playbackParameters.speed,
                        audioTracks = result?.audioTracks ?: emptyList(),
                        subtitleTracks = result?.subtitleTracks ?: emptyList(),
                        videoWidth = result?.videoWidth ?: 0,
                        videoHeight = result?.videoHeight ?: 0,
                        videoCodec = result?.videoCodec,
                        videoBitrate = result?.videoBitrate ?: 0,
                        audioBitrate = result?.audioBitrate ?: 0,
                        chapters = result?.chapters ?: emptyList(),
                        currentCueCount = cueCount,
                    )
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
                onPlaybackStateChanged(
                    PlayerPlaybackState(
                        isPlaying = exoPlayer.isPlaying,
                        isLoading = exoPlayer.isLoading,
                        currentPositionMs = exoPlayer.currentPosition,
                        durationMs = exoPlayer.duration.coerceAtLeast(0L),
                        bufferedPositionMs = exoPlayer.bufferedPosition,
                        playbackSpeed = exoPlayer.playbackParameters.speed,
                    )
                )
            }
            delay(1000)
        }
    }

    // Handle actions
    LaunchedEffect(requestedAction) {
        requestedAction?.let { action ->
            when (action) {
                is PlayerAction.Play -> exoPlayer.play()
                is PlayerAction.Pause -> exoPlayer.pause()
                is PlayerAction.SeekTo -> exoPlayer.seekTo(action.positionMs)
                is PlayerAction.SetSpeed -> {
                    exoPlayer.playbackParameters = PlaybackParameters(action.speed)
                }

                is PlayerAction.SetVolume -> {
                    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                    val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                    val newVolume = (action.volume.coerceIn(0f, 1f) * max).toInt().coerceIn(0, max)
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, AudioManager.FLAG_SHOW_UI)
                }

                is PlayerAction.SelectAudioTrack -> {
                    val groups = exoPlayer.currentTracks.groups
                    if (action.groupIndex in 0 until groups.size) {
                        val trackGroup = groups[action.groupIndex]
                        if (trackGroup.type == C.TRACK_TYPE_AUDIO && action.trackIndex in 0 until trackGroup.length) {
                            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                .buildUpon()
                                .setOverrideForType(
                                    TrackSelectionOverride(
                                        trackGroup.mediaTrackGroup,
                                        action.trackIndex
                                    )
                                )
                                .build()
                            // Ensure playback continues after track re-evaluation
                            if (!exoPlayer.isPlaying) exoPlayer.play()
                        }
                    }
                }

                is PlayerAction.SelectSubtitleTrack -> {
                    if (action.groupIndex == -1) {
                        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                            .buildUpon()
                            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                            .build()
                        if (!exoPlayer.isPlaying) exoPlayer.play()
                    } else {
                        val groups = exoPlayer.currentTracks.groups
                        if (action.groupIndex in 0 until groups.size) {
                            val trackGroup = groups[action.groupIndex]
                            if (trackGroup.type == C.TRACK_TYPE_TEXT && action.trackIndex in 0 until trackGroup.length) {
                                // Build fresh params to avoid stale overrides
                                exoPlayer.trackSelectionParameters =
                                    exoPlayer.trackSelectionParameters
                                        .buildUpon()
                                        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                                        .setOverrideForType(
                                            TrackSelectionOverride(
                                                trackGroup.mediaTrackGroup,
                                                action.trackIndex
                                            )
                                        )
                                        .build()
                                // Push existing cues manually to subtitle view
                                lastCueList?.let { cues ->
                                    playerView?.subtitleView?.let { sv ->
                                        sv.setCues(cues)
                                        sv.setVisibility(android.view.View.VISIBLE)
                                        sv.invalidate()
                                    }
                                }
                            }
                        }
                    }
                }

                is PlayerAction.SetScale -> {
                    // Handled via videoScale state in LaunchedEffect(videoScale)
                }

                is PlayerAction.ResetZoom -> {
                    // Handled via freeZoomScale/freeZoomOffset state in PlayerScreen
                }

                is PlayerAction.EnterPip -> {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        activity?.enterPictureInPictureMode(
                            android.app.PictureInPictureParams.Builder().build()
                        )
                    }
                }
            }
            onActionConsumed()
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
            }
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
            sv.setStyle(
                CaptionStyleCompat(
                    subtitleStyle.fontColorArgb,
                    android.graphics.Color.argb(
                        (subtitleStyle.bgOpacity * 255).toInt(), 0, 0, 0
                    ),
                    android.graphics.Color.TRANSPARENT,
                    CaptionStyleCompat.EDGE_TYPE_OUTLINE,
                    subtitleStyle.fontColorArgb,
                    null
                )
            )
            sv.setFixedTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, subtitleStyle.fontSizeSp.toFloat())
            sv.invalidate()
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
                            android.util.Rational(videoFormat.width, videoFormat.height)
                        } else null
                        val builder = android.app.PictureInPictureParams.Builder()
                        if (rational != null) builder.setAspectRatio(rational)
                        activity?.enterPictureInPictureMode(builder.build())
                    } catch (_: Exception) { }
                }
            }
        }
        (activity as? androidx.activity.ComponentActivity)?.addOnUserLeaveHintListener(pipListener)
        onDispose {
            (activity as? androidx.activity.ComponentActivity)?.removeOnUserLeaveHintListener(pipListener)
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
            try {
                exoPlayer.release()
            } catch (_: Exception) {
                // ExoPlayer release is best-effort at this point
            }
            playerReleased = true
        },
        modifier = modifier
    )
}

private data class TrackExtractionResult(
    val audioTracks: List<AudioTrack>,
    val subtitleTracks: List<SubtitleTrack>,
    val videoWidth: Int,
    val videoHeight: Int,
    val videoCodec: String?,
    val videoBitrate: Int,
    val audioBitrate: Int,
    val chapters: List<ChapterInfo>
)

private fun extractTracks(player: Player): TrackExtractionResult {
    val tracks = player.currentTracks
    val audioTracks = mutableListOf<AudioTrack>()
    val subtitleTracks = mutableListOf<SubtitleTrack>()
    var videoWidth = 0
    var videoHeight = 0
    var videoCodec: String? = null
    // Track format's bitrate (Media3 updates this on ABR switches for adaptive streams)
    var videoBitrate = 0
    var audioBitrate = 0
    tracks.groups.forEachIndexed { index, group ->
        val type = group.type
        for (i in 0 until group.length) {
            val format = group.getTrackFormat(i)
            val isSelected = group.isTrackSelected(i)
            val label = format.label ?: format.language ?: "Track ${i + 1}"
            if (type == C.TRACK_TYPE_AUDIO) {
                if (isSelected && format.bitrate > 0) audioBitrate = format.bitrate
                audioTracks.add(
                    AudioTrack(
                        index,
                        i.toString(),
                        label,
                        format.language,
                        isSelected
                    )
                )
            } else if (type == C.TRACK_TYPE_TEXT) {
                subtitleTracks.add(
                    SubtitleTrack(
                        index,
                        i.toString(),
                        label,
                        format.language,
                        isSelected
                    )
                )
            } else if (type == C.TRACK_TYPE_VIDEO && isSelected) {
                if (format.width > 0 && format.height > 0) {
                    videoWidth = format.width
                    videoHeight = format.height
                }
                if (videoCodec == null) {
                    videoCodec = format.codecs
                }
                if (format.bitrate > 0) videoBitrate = format.bitrate
            }
        }
    }
    // Extract chapter info from media metadata
    val chapters = mutableListOf<ChapterInfo>()
    try {
        val mediaMeta = player.mediaMetadata
        // Access chapters via reflection-safe cast to avoid API mismatch
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
    } catch (_: Exception) { /* media metadata chapters not available */ }
    return TrackExtractionResult(audioTracks, subtitleTracks, videoWidth, videoHeight, videoCodec, videoBitrate, audioBitrate, chapters)
}
