package com.moviehub.feature.player.presentation

import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import com.moviehub.core.model.AudioTrack
import com.moviehub.core.model.PlayerPlaybackState
import com.moviehub.core.model.SubtitleTrack
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@OptIn(UnstableApi::class)
@Composable
actual fun VideoPlayer(
    url: String,
    headers: Map<String, String>,
    onPlaybackStateChanged: (PlayerPlaybackState) -> Unit,
    requestedAction: PlayerAction?,
    onActionConsumed: () -> Unit,
    modifier: Modifier
) {
    val context = LocalContext.current
    DisposableEffect(context) {
        var currentContext = context
        var activity: android.app.Activity? = null
        while (currentContext is android.content.ContextWrapper) {
            if (currentContext is android.app.Activity) {
                activity = currentContext
                break
            }
            currentContext = currentContext.baseContext
        }
        if (currentContext is android.app.Activity) {
            activity = currentContext
        }
        val originalOrientation = activity?.requestedOrientation ?: android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

        // Immersive Fullscreen Mode
        val window = activity?.window
        var previousBehavior = 0
        var controller: androidx.core.view.WindowInsetsControllerCompat? = null
        if (window != null) {
            controller = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
            previousBehavior = controller.systemBarsBehavior
            controller.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        onDispose {
            activity?.requestedOrientation = originalOrientation
            if (window != null && controller != null) {
                controller.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior = previousBehavior
            }
        }
    }

    val exoPlayer = remember {
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setDefaultRequestProperties(headers)
            .setAllowCrossProtocolRedirects(true)
        
        val mediaSourceFactory = DefaultMediaSourceFactory(context)
            .setDataSourceFactory(httpDataSourceFactory)

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()

        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .setAudioAttributes(audioAttributes, true)
            .build()
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
            val mediaItem = MediaItem.fromUri(url)
            exoPlayer.setMediaItem(mediaItem)
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

    // Efficient state updates using Listener instead of polling
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                val tracks = player.currentTracks
                val audioTracks = mutableListOf<AudioTrack>()
                val subtitleTracks = mutableListOf<SubtitleTrack>()

                tracks.groups.forEachIndexed { index, group ->
                    val type = group.type
                    for (i in 0 until group.length) {
                        val format = group.getTrackFormat(i)
                        val isSelected = group.isTrackSelected(i)
                        val label = format.label ?: format.language ?: "Track ${i + 1}"

                        if (type == C.TRACK_TYPE_AUDIO) {
                            audioTracks.add(AudioTrack(index, i.toString(), label, format.language, isSelected))
                        } else if (type == C.TRACK_TYPE_TEXT) {
                            subtitleTracks.add(SubtitleTrack(index, i.toString(), label, format.language, isSelected))
                        }
                    }
                }

                onPlaybackStateChanged(
                    PlayerPlaybackState(
                        isPlaying = player.isPlaying,
                        isLoading = player.isLoading,
                        error = player.playerError?.message ?: player.playerError?.localizedMessage,
                        currentPositionMs = player.currentPosition,
                        durationMs = player.duration.coerceAtLeast(0L),
                        bufferedPositionMs = player.bufferedPosition,
                        playbackSpeed = player.playbackParameters.speed,
                        audioTracks = audioTracks,
                        subtitleTracks = subtitleTracks
                    )
                )
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
        }
    }

    // Progress polling (only for position updates while playing)
    LaunchedEffect(exoPlayer) {
        while (isActive) {
            if (exoPlayer.isPlaying) {
                onPlaybackStateChanged(
                    PlayerPlaybackState(
                        isPlaying = exoPlayer.isPlaying,
                        isLoading = exoPlayer.isLoading,
                        error = exoPlayer.playerError?.message,
                        currentPositionMs = exoPlayer.currentPosition,
                        durationMs = exoPlayer.duration.coerceAtLeast(0L),
                        bufferedPositionMs = exoPlayer.bufferedPosition,
                        playbackSpeed = exoPlayer.playbackParameters.speed,
                        audioTracks = mutableListOf<AudioTrack>().apply {
                            val tracks = exoPlayer.currentTracks
                            tracks.groups.forEachIndexed { index, group ->
                                if (group.type == C.TRACK_TYPE_AUDIO) {
                                    for (i in 0 until group.length) {
                                        val format = group.getTrackFormat(i)
                                        add(AudioTrack(index, i.toString(), format.label ?: format.language ?: "Track ${i + 1}", format.language, group.isTrackSelected(i)))
                                    }
                                }
                            }
                        },
                        subtitleTracks = mutableListOf<SubtitleTrack>().apply {
                            val tracks = exoPlayer.currentTracks
                            tracks.groups.forEachIndexed { index, group ->
                                if (group.type == C.TRACK_TYPE_TEXT) {
                                    for (i in 0 until group.length) {
                                        val format = group.getTrackFormat(i)
                                        add(SubtitleTrack(index, i.toString(), format.label ?: format.language ?: "Track ${i + 1}", format.language, group.isTrackSelected(i)))
                                    }
                                }
                            }
                        }
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
                is PlayerAction.SelectAudioTrack -> {
                    if (action.index in 0 until exoPlayer.currentTracks.groups.size) {
                        val trackGroup = exoPlayer.currentTracks.groups[action.index]
                        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                            .buildUpon()
                            .setOverrideForType(TrackSelectionOverride(trackGroup.mediaTrackGroup, 0))
                            .build()
                    }
                }
                is PlayerAction.SelectSubtitleTrack -> {
                    if (action.index == -1) {
                        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                            .buildUpon()
                            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                            .build()
                    } else if (action.index in 0 until exoPlayer.currentTracks.groups.size) {
                        val trackGroup = exoPlayer.currentTracks.groups[action.index]
                        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                            .buildUpon()
                            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                            .setOverrideForType(TrackSelectionOverride(trackGroup.mediaTrackGroup, 0))
                            .build()
                    }
                }
            }
            onActionConsumed()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = false // We use our own controls
            }
        },
        modifier = modifier
    )
}
