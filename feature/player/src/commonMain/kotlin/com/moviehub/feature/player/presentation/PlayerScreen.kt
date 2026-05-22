package com.moviehub.feature.player.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.moviehub.core.model.PlayerPlaybackState
import com.moviehub.core.model.StreamItem
import com.moviehub.feature.player.presentation.components.PlayerControls
import kotlinx.coroutines.delay

@Composable
expect fun VideoPlayer(
    url: String,
    headers: Map<String, String> = emptyMap(),
    onPlaybackStateChanged: (PlayerPlaybackState) -> Unit = {},
    requestedAction: PlayerAction? = null,
    onActionConsumed: () -> Unit = {},
    modifier: Modifier = Modifier
)

sealed interface PlayerAction {
    data class SeekTo(val positionMs: Long) : PlayerAction
    data class SetSpeed(val speed: Float) : PlayerAction
    data class SelectAudioTrack(val index: Int) : PlayerAction
    data class SelectSubtitleTrack(val index: Int) : PlayerAction
    data object Play : PlayerAction
    data object Pause : PlayerAction
}

@Composable
fun PlayerScreen(
    stream: StreamItem,
    onBackClick: () -> Unit,
    title: String = "Playing..."
) {
    var playbackState by remember { mutableStateOf(PlayerPlaybackState()) }
    var requestedAction by remember { mutableStateOf<PlayerAction?>(null) }
    var isControlsVisible by remember { mutableStateOf(true) }

    val streamUrl = stream.url ?: stream.externalUrl ?: ""
    val headers = stream.behaviorHints.proxyHeaders?.request ?: emptyMap()

    // Auto-hide controls
    LaunchedEffect(isControlsVisible, playbackState.isPlaying) {
        if (isControlsVisible && playbackState.isPlaying) {
            delay(3000)
            isControlsVisible = false
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
                isControlsVisible = !isControlsVisible
            },
        contentAlignment = Alignment.Center
    ) {
        if (streamUrl.isNotBlank()) {
            VideoPlayer(
                url = streamUrl,
                headers = headers,
                onPlaybackStateChanged = { playbackState = it },
                requestedAction = requestedAction,
                onActionConsumed = { requestedAction = null },
                modifier = Modifier.fillMaxSize()
            )
            
            PlayerControls(
                isVisible = isControlsVisible,
                isPlaying = playbackState.isPlaying,
                title = stream.name ?: title,
                onPlayPauseToggle = { 
                    requestedAction = if (playbackState.isPlaying) PlayerAction.Pause else PlayerAction.Play 
                },
                onBackClick = onBackClick,
                onSeek = { requestedAction = PlayerAction.SeekTo((it * playbackState.durationMs).toLong()) },
                progress = if (playbackState.durationMs > 0) playbackState.currentPositionMs.toFloat() / playbackState.durationMs else 0f,
                duration = playbackState.durationMs,
                currentTime = playbackState.currentPositionMs,
                onSpeedChange = { requestedAction = PlayerAction.SetSpeed(it) },
                onAudioTrackChange = { requestedAction = PlayerAction.SelectAudioTrack(it) },
                onSubtitleTrackChange = { requestedAction = PlayerAction.SelectSubtitleTrack(it) },
                audioTracks = playbackState.audioTracks,
                subtitleTracks = playbackState.subtitleTracks,
                currentSpeed = playbackState.playbackSpeed,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = "Invalid Stream URL",
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}
