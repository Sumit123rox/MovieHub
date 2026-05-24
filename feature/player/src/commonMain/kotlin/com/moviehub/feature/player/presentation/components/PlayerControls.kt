package com.moviehub.feature.player.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.moviehub.core.model.AudioTrack
import com.moviehub.core.model.SubtitleTrack
import com.moviehub.core.model.StreamItem
import com.moviehub.core.ui.components.GlassyBox
import com.moviehub.core.ui.theme.MovieHubColors

enum class PlayerSettingsType {
    SPEED,
    SUBTITLES,
    AUDIO,
    SOURCES
}

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
    currentStream: StreamItem? = null,
    streams: List<StreamItem> = emptyList(),
    onStreamChange: (StreamItem) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var activeSettingsSheet by remember { mutableStateOf<PlayerSettingsType?>(null) }
    val safeDuration = if (duration > 0L) duration else 1L

    Box(modifier = modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Top Bar with vertical gradient shadow
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.8f),
                                    Color.Black.copy(alpha = 0.4f),
                                    Color.Transparent
                                )
                            )
                        )
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                        .padding(vertical = 16.dp, horizontal = 24.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier
                                .size(44.dp)
                                .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        IconButton(
                            onClick = { activeSettingsSheet = PlayerSettingsType.SPEED },
                            modifier = Modifier
                                .size(44.dp)
                                .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                // Bottom Bar with vertical gradient shadow and notch/home-bar support
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.5f),
                                    Color.Black.copy(alpha = 0.9f)
                                )
                            )
                        )
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
                        .padding(vertical = 20.dp, horizontal = 24.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Time Indicators and Slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = formatTime(currentTime),
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.8f),
                                fontWeight = FontWeight.Medium
                            )

                            Slider(
                                value = progress,
                                onValueChange = onSeek,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(
                                    thumbColor = MovieHubColors.Primary,
                                    activeTrackColor = MovieHubColors.Primary,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.24f)
                                )
                            )

                            Text(
                                text = formatTime(duration),
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.8f),
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Bottom Actions Row (Left: Action pills, Right: playback seek/play buttons)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // bottom settings pills
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                PlayerActionPill(
                                    label = "Speed: ${currentSpeed}x",
                                    icon = Icons.Default.Speed,
                                    onClick = { activeSettingsSheet = PlayerSettingsType.SPEED }
                                )

                                val currentSub = subtitleTracks.firstOrNull { it.isSelected }?.label ?: "Off"
                                PlayerActionPill(
                                    label = "Subtitles: $currentSub",
                                    icon = Icons.Default.ClosedCaption,
                                    onClick = { activeSettingsSheet = PlayerSettingsType.SUBTITLES }
                                )

                                val currentAudio = audioTracks.firstOrNull { it.isSelected }?.label ?: "Default"
                                PlayerActionPill(
                                    label = "Audio: $currentAudio",
                                    icon = Icons.Default.VolumeUp,
                                    onClick = { activeSettingsSheet = PlayerSettingsType.AUDIO }
                                )

                                if (streams.isNotEmpty()) {
                                    val currentSrc = currentStream?.addonName ?: currentStream?.name ?: "Alternate"
                                    PlayerActionPill(
                                        label = "Sources: $currentSrc",
                                        icon = Icons.Default.List,
                                        onClick = { activeSettingsSheet = PlayerSettingsType.SOURCES }
                                    )
                                }
                            }

                            // Center playback controls (10s back, play/pause, 10s forward)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(20.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { onSeek(((currentTime - 10000L).coerceAtLeast(0L).toFloat() / safeDuration).coerceIn(0f, 1f)) },
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(Color.White.copy(alpha = 0.08f), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Replay10,
                                        contentDescription = "Rewind 10s",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                IconButton(
                                    onClick = onPlayPauseToggle,
                                    modifier = Modifier
                                        .size(56.dp)
                                        .background(MovieHubColors.Primary, CircleShape)
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = if (isPlaying) "Pause" else "Play",
                                        tint = Color.White,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { onSeek(((currentTime + 10000L).coerceAtMost(duration).toFloat() / safeDuration).coerceIn(0f, 1f)) },
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(Color.White.copy(alpha = 0.08f), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Forward10,
                                        contentDescription = "Forward 10s",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Custom compose glassy sheets (Slide in side panels in landscape mode)
        when (activeSettingsSheet) {
            PlayerSettingsType.SPEED -> {
                PlayerOverlaySheet(
                    title = "Playback Speed",
                    onDismiss = { activeSettingsSheet = null }
                ) {
                    listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                        val isSelected = currentSpeed == speed
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) Color.White.copy(alpha = 0.1f) else Color.Transparent)
                                .clickable {
                                    onSpeedChange(speed)
                                    activeSettingsSheet = null
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    onSpeedChange(speed)
                                    activeSettingsSheet = null
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = MovieHubColors.Primary,
                                    unselectedColor = Color.White.copy(alpha = 0.6f)
                                )
                            )
                            Text(
                                text = "${speed}x",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(start = 12.dp)
                            )
                        }
                    }
                }
            }

            PlayerSettingsType.SUBTITLES -> {
                PlayerOverlaySheet(
                    title = "Subtitles",
                    onDismiss = { activeSettingsSheet = null }
                ) {
                    val isOffSelected = subtitleTracks.none { it.isSelected }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isOffSelected) Color.White.copy(alpha = 0.1f) else Color.Transparent)
                            .clickable {
                                onSubtitleTrackChange(-1)
                                activeSettingsSheet = null
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        RadioButton(
                            selected = isOffSelected,
                            onClick = {
                                onSubtitleTrackChange(-1)
                                activeSettingsSheet = null
                            },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MovieHubColors.Primary,
                                unselectedColor = Color.White.copy(alpha = 0.6f)
                            )
                        )
                        Text(
                            text = "Subtitles Off",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White,
                            fontWeight = if (isOffSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (subtitleTracks.isEmpty()) {
                        Text(
                            text = "No subtitle tracks available",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.weight(1f).fillMaxWidth()
                        ) {
                            items(subtitleTracks) { track ->
                                val isSelected = track.isSelected
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) Color.White.copy(alpha = 0.1f) else Color.Transparent)
                                        .clickable {
                                            onSubtitleTrackChange(track.index)
                                            activeSettingsSheet = null
                                        }
                                        .padding(horizontal = 16.dp, vertical = 12.dp)
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = {
                                            onSubtitleTrackChange(track.index)
                                            activeSettingsSheet = null
                                        },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = MovieHubColors.Primary,
                                            unselectedColor = Color.White.copy(alpha = 0.6f)
                                        )
                                    )
                                    Text(
                                        text = track.label,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = Color.White,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.padding(start = 12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            PlayerSettingsType.AUDIO -> {
                PlayerOverlaySheet(
                    title = "Audio Track",
                    onDismiss = { activeSettingsSheet = null }
                ) {
                    if (audioTracks.isEmpty()) {
                        Text(
                            text = "No audio tracks available",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.weight(1f).fillMaxWidth()
                        ) {
                            items(audioTracks) { track ->
                                val isSelected = track.isSelected
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) Color.White.copy(alpha = 0.1f) else Color.Transparent)
                                        .clickable {
                                            onAudioTrackChange(track.index)
                                            activeSettingsSheet = null
                                        }
                                        .padding(horizontal = 16.dp, vertical = 12.dp)
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = {
                                            onAudioTrackChange(track.index)
                                            activeSettingsSheet = null
                                        },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = MovieHubColors.Primary,
                                            unselectedColor = Color.White.copy(alpha = 0.6f)
                                        )
                                    )
                                    Text(
                                        text = track.label,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = Color.White,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.padding(start = 12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            PlayerSettingsType.SOURCES -> {
                PlayerOverlaySheet(
                    title = "Alternative Sources",
                    onDismiss = { activeSettingsSheet = null }
                ) {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    ) {
                        items(streams) { stream ->
                            val isSelected = currentStream?.url == stream.url || currentStream?.externalUrl == stream.externalUrl
                            Row(
                                verticalAlignment = Alignment.Top,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) MovieHubColors.Primary.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.04f))
                                    .then(
                                        if (isSelected) Modifier.border(1.dp, MovieHubColors.Primary.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                        else Modifier
                                    )
                                    .clickable {
                                        onStreamChange(stream)
                                        activeSettingsSheet = null
                                    }
                                    .padding(14.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = stream.addonName ?: stream.name ?: "Unknown Source",
                                            color = Color.White,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold
                                            ),
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(99.dp))
                                                    .background(MovieHubColors.Primary)
                                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "Playing",
                                                    color = Color.White,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }

                                    val desc = stream.description
                                    if (!desc.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = desc,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White.copy(alpha = 0.6f),
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            null -> { /* Do nothing */ }
        }
    }
}

@Composable
fun PlayerActionPill(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        color = Color.Black.copy(alpha = 0.5f),
        contentColor = Color.White,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(16.dp),
                tint = Color.White
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun PlayerOverlaySheet(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
            .background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.CenterEnd
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(360.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
        ) {
            GlassyBox(
                modifier = Modifier.fillMaxSize(),
                blurRadius = 24.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical + WindowInsetsSides.Right))
                        .padding(horizontal = 24.dp, vertical = 20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f), thickness = 1.dp)

                    Spacer(modifier = Modifier.height(16.dp))

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        content()
                    }
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val minStr = if (minutes < 10) "0$minutes" else "$minutes"
    val secStr = if (seconds < 10) "0$seconds" else "$seconds"
    return "$minStr:$secStr"
}
