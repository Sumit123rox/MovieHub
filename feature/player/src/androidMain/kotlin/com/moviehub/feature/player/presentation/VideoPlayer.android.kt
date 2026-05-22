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
            .build().apply {
                setMediaItem(MediaItem.fromUri(url))
                prepare()
                playWhenReady = true
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
                    val trackGroup = exoPlayer.currentTracks.groups[action.index]
                    exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                        .buildUpon()
                        .setOverrideForType(TrackSelectionOverride(trackGroup.mediaTrackGroup, 0))
                        .build()
                }
                is PlayerAction.SelectSubtitleTrack -> {
                    val trackGroup = exoPlayer.currentTracks.groups[action.index]
                    exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                        .buildUpon()
                        .setOverrideForType(TrackSelectionOverride(trackGroup.mediaTrackGroup, 0))
                        .build()
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
