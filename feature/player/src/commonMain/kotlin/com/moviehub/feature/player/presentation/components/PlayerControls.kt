package com.moviehub.feature.player.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.moviehub.core.model.AudioTrack
import com.moviehub.core.model.SubtitleTrack
import com.moviehub.core.ui.components.GlassyBox
import moviehub.core.ui.generated.resources.Res
import org.jetbrains.compose.resources.stringResource

@Composable
fun PlayerControls(
    isVisible: Boolean,
    isPlaying: Boolean,
    title: String,
    onPlayPauseToggle: () -> Unit,
    onBackClick: () -> Unit,
    onSeek: (Float) -> Unit,
    progress: Float,
    duration: Long,
    currentTime: Long,
    onSpeedChange: (Float) -> Unit = {},
    onAudioTrackChange: (Int) -> Unit = {},
    onSubtitleTrackChange: (Int) -> Unit = {},
    audioTracks: List<AudioTrack> = emptyList(),
    subtitleTracks: List<SubtitleTrack> = emptyList(),
    currentSpeed: Float = 1f,
    modifier: Modifier = Modifier
) {
    var showSettings by remember { mutableStateOf(false) }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier.fillMaxSize()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            GlassyBox(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    IconButton(onClick = { showSettings = true }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Bottom Bar
            GlassyBox(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Time Indicators
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatTime(currentTime),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = formatTime(duration),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        IconButton(onClick = onPlayPauseToggle) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        Slider(
                            value = progress,
                            onValueChange = onSeek,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                        )
                    }
                }
            }
        }

        if (showSettings) {
            PlayerSettingsDialog(
                onDismiss = { showSettings = false },
                audioTracks = audioTracks,
                subtitleTracks = subtitleTracks,
                currentSpeed = currentSpeed,
                onSpeedChange = onSpeedChange,
                onAudioTrackChange = onAudioTrackChange,
                onSubtitleTrackChange = onSubtitleTrackChange
            )
        }
    }
}

@Composable
fun PlayerSettingsDialog(
    onDismiss: () -> Unit,
    audioTracks: List<AudioTrack>,
    subtitleTracks: List<SubtitleTrack>,
    currentSpeed: Float,
    onSpeedChange: (Float) -> Unit,
    onAudioTrackChange: (Int) -> Unit,
    onSubtitleTrackChange: (Int) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Playback Settings") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Speed Selection
                Text("Playback Speed", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0.5f, 1.0f, 1.5f, 2.0f).forEach { speed ->
                        FilterChip(
                            selected = currentSpeed == speed,
                            onClick = { onSpeedChange(speed) },
                            label = { Text("${speed}x") }
                        )
                    }
                }

                // Audio Tracks
                if (audioTracks.isNotEmpty()) {
                    Text("Audio Track", style = MaterialTheme.typography.labelLarge)
                    audioTracks.forEach { track ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable { onAudioTrackChange(track.index) }.padding(vertical = 4.dp)
                        ) {
                            RadioButton(selected = track.isSelected, onClick = { onAudioTrackChange(track.index) })
                            Text(track.label, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }

                // Subtitle Tracks
                if (subtitleTracks.isNotEmpty()) {
                    Text("Subtitles", style = MaterialTheme.typography.labelLarge)
                    subtitleTracks.forEach { track ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable { onSubtitleTrackChange(track.index) }.padding(vertical = 4.dp)
                        ) {
                            RadioButton(selected = track.isSelected, onClick = { onSubtitleTrackChange(track.index) })
                            Text(track.label, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
