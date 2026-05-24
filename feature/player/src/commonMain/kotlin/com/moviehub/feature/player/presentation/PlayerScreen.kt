package com.moviehub.feature.player.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.moviehub.core.model.PlayerPlaybackState
import com.moviehub.core.model.StreamItem
import com.moviehub.core.ui.theme.MovieHubColors
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
    streams: List<StreamItem> = emptyList(),
    onBackClick: () -> Unit,
    title: String = "Playing..."
) {
    var activeStream by remember { mutableStateOf(stream) }
    var playbackState by remember { mutableStateOf(PlayerPlaybackState()) }
    var requestedAction by remember { mutableStateOf<PlayerAction?>(null) }
    var isControlsVisible by remember { mutableStateOf(true) }
    var showRecoveryOverlay by remember { mutableStateOf(false) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }

    val clipboardManager = LocalClipboardManager.current
    val uriHandler = LocalUriHandler.current

    val streamUrl = activeStream.url ?: activeStream.externalUrl ?: ""
    val headers = activeStream.behaviorHints.proxyHeaders?.request ?: emptyMap()

    // Auto-hide controls
    LaunchedEffect(isControlsVisible, playbackState.isPlaying) {
        if (isControlsVisible && playbackState.isPlaying) {
            delay(3000)
            isControlsVisible = false
        }
    }

    // Stuck stream recovery timer (7 seconds buffer threshold)
    LaunchedEffect(playbackState.isLoading, playbackState.isPlaying) {
        if (playbackState.isLoading && !playbackState.isPlaying) {
            delay(7000)
            showRecoveryOverlay = true
        } else {
            showRecoveryOverlay = false
        }
    }

    // Auto-clear feedback messages after 2 seconds
    LaunchedEffect(feedbackMessage) {
        if (feedbackMessage != null) {
            delay(2000)
            feedbackMessage = null
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
            
            // Premium Semi-translucent Loading Spinner Overlay
            if (playbackState.isLoading && playbackState.error == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(
                            color = MovieHubColors.Primary,
                            strokeWidth = 4.dp,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "Loading stream...",
                            color = Color.White.copy(alpha = 0.85f),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            PlayerControls(
                isVisible = isControlsVisible,
                isPlaying = playbackState.isPlaying,
                title = title,
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
                currentStream = activeStream,
                streams = streams,
                onStreamChange = { activeStream = it },
                modifier = Modifier.fillMaxSize()
            )

            // Premium Translucent Recovery/Error Overlay
            if (showRecoveryOverlay || playbackState.error != null) {
                val isActualError = playbackState.error != null
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
                                        if (isActualError) Color.Red.copy(alpha = 0.15f) else MovieHubColors.Primary.copy(alpha = 0.15f), 
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = if (isActualError) "Playback Failed" else "Slow Load Warning",
                                    tint = if (isActualError) Color.Red else MovieHubColors.Primary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            Text(
                                text = if (isActualError) "Playback Failed" else "Playback Load Timeout",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text = if (isActualError) {
                                    "An error occurred: ${playbackState.error}. You can try playing this stream in an external player, copy the direct link, or choose another source."
                                } else {
                                    "This stream is taking longer than usual to load. You can play it externally, copy the direct stream link, or go back."
                                },
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
                                        containerColor = MovieHubColors.Primary,
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
    }
}
