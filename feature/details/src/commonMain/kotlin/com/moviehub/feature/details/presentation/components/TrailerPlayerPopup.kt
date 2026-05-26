package com.moviehub.feature.details.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.moviehub.core.model.MediaTrailer
import com.moviehub.core.model.PlayerPlaybackState
import com.moviehub.core.model.TrailerPlaybackSource
import com.moviehub.feature.player.presentation.PlayerAction
import com.moviehub.feature.player.presentation.VideoPlayer
import com.moviehub.feature.player.presentation.components.PlayerControls
import kotlinx.coroutines.delay

@Composable
fun TrailerPlayerPopup(
    trailer: MediaTrailer?,
    source: TrailerPlaybackSource?,
    isLoading: Boolean,
    onDismiss: () -> Unit,
) {
    if (trailer == null) return

    var playbackState by remember { mutableStateOf(PlayerPlaybackState()) }
    var requestedAction by remember { mutableStateOf<PlayerAction?>(null) }
    var isControlsVisible by remember { mutableStateOf(true) }
    var restartAfterSeek by remember { mutableStateOf(false) }

    // Auto-hide controls after 3s of playback
    LaunchedEffect(isControlsVisible, playbackState.isPlaying) {
        if (isControlsVisible && playbackState.isPlaying) {
            delay(3000)
            isControlsVisible = false
        }
    }

    // When trailer ends at 100%, pressing play restarts from the beginning
    LaunchedEffect(requestedAction, restartAfterSeek) {
        if (restartAfterSeek && requestedAction == null) {
            restartAfterSeek = false
            requestedAction = PlayerAction.Play
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Layer 1: Video content with tap-to-toggle
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    isControlsVisible = !isControlsVisible
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Video player area (fills available space)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isLoading && source == null) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                    } else if (source != null) {
                        VideoPlayer(
                            url = source.videoUrl,
                            onPlaybackStateChanged = { playbackState = it },
                            requestedAction = requestedAction,
                            onActionConsumed = { requestedAction = null },
                            forceLandscape = false,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Loading overlay during buffering
                        if (playbackState.isLoading) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }

                        // Transparent tap overlay — captures touches that native
                        // video views swallow, enabling controls toggle
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { isControlsVisible = !isControlsVisible }
                        )

                        // Player controls overlay
                        PlayerControls(
                            isVisible = isControlsVisible,
                            isPlaying = playbackState.isPlaying,
                            title = trailer.name ?: "Trailer",
                            onPlayPauseToggle = {
                                if (!playbackState.isPlaying && playbackState.durationMs > 0 && playbackState.currentPositionMs >= playbackState.durationMs) {
                                    // Trailer ended — seek to 0 first, then auto-play
                                    restartAfterSeek = true
                                    requestedAction = PlayerAction.SeekTo(0)
                                } else {
                                    requestedAction = if (playbackState.isPlaying) PlayerAction.Pause else PlayerAction.Play
                                }
                            },
                            onBackClick = onDismiss,
                            onSeek = { progress ->
                                requestedAction = PlayerAction.SeekTo((progress * playbackState.durationMs).toLong())
                            },
                            progress = if (playbackState.durationMs > 0) playbackState.currentPositionMs.toFloat() / playbackState.durationMs else 0f,
                            duration = playbackState.durationMs,
                            currentTime = playbackState.currentPositionMs,
                            onSpeedChange = { requestedAction = PlayerAction.SetSpeed(it) },
                            currentSpeed = playbackState.playbackSpeed,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(
                            text = "Unable to load trailer source.",
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}
