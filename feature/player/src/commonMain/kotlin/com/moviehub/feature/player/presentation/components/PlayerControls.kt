package com.moviehub.feature.player.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moviehub.core.model.AudioTrack
import com.moviehub.core.model.ChapterInfo
import androidx.compose.ui.geometry.Offset
import com.moviehub.core.model.SubtitleStyle
import com.moviehub.core.model.SubtitleTrack
import com.moviehub.core.model.StreamItem
import com.moviehub.core.model.VideoScale
import com.moviehub.core.ui.components.ContentCard
import com.moviehub.core.ui.theme.MovieHubColors
import com.moviehub.feature.player.presentation.CastButton

enum class PlayerSettingsType {
    SPEED,
    SUBTITLES,
    AUDIO,
    SOURCES,
    SCALE,
    SLEEP
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
    onAudioTrackChange: (Int, Int) -> Unit = { _, _ -> },
    onSubtitleTrackChange: (Int, Int) -> Unit = { _, _ -> },
    audioTracks: List<AudioTrack> = emptyList(),
    subtitleTracks: List<SubtitleTrack> = emptyList(),
    currentSpeed: Float = 1f,
    currentStream: StreamItem? = null,
    streams: List<StreamItem> = emptyList(),
    onStreamChange: (StreamItem) -> Unit = {},
    onScaleChange: (VideoScale) -> Unit = {},
    onScaleCycle: () -> Unit = {},
    onResetZoom: () -> Unit = {},
    currentScale: VideoScale = VideoScale.FIT,
    freeZoomScale: Float = 1f,
    currentVolume: Float = 1f,
    videoResolution: String = "",
    videoCodec: String? = null,
    videoBitrate: Int = 0,
    audioBitrate: Int = 0,
    chapters: List<ChapterInfo> = emptyList(),
    showDebugOverlay: Boolean = false,
    onToggleDebug: () -> Unit = {},
    onVolumeChange: (Float) -> Unit = {},
    isScreenLocked: Boolean = false,
    onScreenLockToggle: () -> Unit = {},
    isSettingsSheetOpen: Boolean = false,
    onSettingsSheetOpenChange: (Boolean) -> Unit = {},
    onEnterPip: () -> Unit = {},
    onNextEpisode: (() -> Unit)? = null,
    onPreviousEpisode: (() -> Unit)? = null,
    sleepTimerRemainingMs: Long? = null,
    onSleepTimerSet: (Long?) -> Unit = {},
    subtitleStyle: SubtitleStyle = SubtitleStyle(),
    onSubtitleStyleChange: (SubtitleStyle) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var activeSettingsSheet by remember { mutableStateOf<PlayerSettingsType?>(null) }
    val safeDuration = if (duration > 0L) duration else 1L

    val accentPrimary = MaterialTheme.colorScheme.primary
    val accentPrimaryLight = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
    val accentGradient = Brush.horizontalGradient(
        colors = listOf(accentPrimary, accentPrimaryLight)
    )

    // Notify parent when settings sheet opens/closes
    LaunchedEffect(activeSettingsSheet) {
        onSettingsSheetOpenChange(activeSettingsSheet != null)
    }

    Box(modifier = modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(animationSpec = androidx.compose.animation.core.tween(200)),
            exit = fadeOut(animationSpec = androidx.compose.animation.core.tween(500)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // ===== TOP BAR =====
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.80f),
                                    Color.Black.copy(alpha = 0.30f),
                                    Color.Transparent
                                )
                            )
                        )
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                        .padding(top = 6.dp, bottom = 16.dp, start = 12.dp, end = 12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(
                                    Color.White.copy(alpha = 0.12f),
                                    CircleShape
                                )
                                .clip(CircleShape)
                                .clickable(onClick = onBackClick),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.3.sp
                            ),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                onClick = onScreenLockToggle,
                                color = if (isScreenLocked) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.12f),
                                shape = CircleShape
                            ) {
                                Icon(
                                    imageVector = if (isScreenLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                    contentDescription = if (isScreenLocked) "Unlock" else "Lock",
                                    tint = if (isScreenLocked) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.padding(5.dp).size(14.dp)
                                )
                            }
                            // Actual stream resolution badge
                            if (videoResolution.isNotBlank()) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = videoResolution,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            letterSpacing = 0.5.sp
                                        ),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            // Audio/codec info badge
                            if (!videoCodec.isNullOrBlank()) {
                                Surface(
                                    color = Color.White.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = videoCodec.take(6),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            letterSpacing = 0.5.sp
                                        ),
                                        color = Color.White.copy(alpha = 0.8f),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            // Bitrate badge
                            val bitrateLabel = when {
                                videoBitrate >= 1_000_000 -> "${videoBitrate / 1_000_000}.${(videoBitrate % 1_000_000) / 100_000} Mbps"
                                videoBitrate > 0 -> "${videoBitrate / 1000} kbps"
                                else -> null
                            }
                            if (bitrateLabel != null) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = bitrateLabel,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            letterSpacing = 0.3.sp
                                        ),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Surface(
                                onClick = onToggleDebug,
                                color = if (showDebugOverlay) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.12f),
                                shape = CircleShape
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Debug Info",
                                    tint = if (showDebugOverlay) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.padding(6.dp).size(18.dp)
                                )
                            }
                            Surface(
                                onClick = onEnterPip,
                                color = Color.White.copy(alpha = 0.12f),
                                shape = CircleShape
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PictureInPictureAlt,
                                    contentDescription = "Picture-in-Picture",
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.padding(6.dp).size(18.dp)
                                )
                            }
                            CastButton(
                                modifier = Modifier.size(38.dp)
                            )
                        }
                    }
                }

                // ===== BOTTOM CONTROLS =====
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.35f),
                                    Color.Black.copy(alpha = 0.85f)
                                )
                            )
                        )
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .padding(top = 28.dp, bottom = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // ===== SEEK BAR WITH TIME LABELS =====
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = formatTime(currentTime),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 11.sp
                                ),
                                color = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.width(42.dp),
                                textAlign = TextAlign.End
                            )

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(24.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { }
                            ) {
                                ChapterMarkerOverlay(chapters, duration)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .align(Alignment.Center)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(Color.White.copy(alpha = 0.15f))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(fraction = progress.coerceIn(0f, 1f))
                                            .background(
                                                accentGradient,
                                                RoundedCornerShape(3.dp)
                                            )
                                    )
                                }

                                Slider(
                                    value = progress,
                                    onValueChange = onSeek,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .offset(y = (-1).dp),
                                    colors = SliderDefaults.colors(
                                        thumbColor = accentPrimaryLight,
                                        activeTrackColor = Color.Transparent,
                                        inactiveTrackColor = Color.Transparent,
                                        disabledThumbColor = accentPrimaryLight
                                    ),
                                    thumb = {
                                        if (progress > 0f) {
                                            Box(
                                                modifier = Modifier
                                                    .size(14.dp)
                                                    .background(accentPrimaryLight, CircleShape)
                                                    .border(2.dp, Color.White.copy(alpha = 0.8f), CircleShape)
                                            )
                                        }
                                    }
                                )
                            }

                            Text(
                                text = formatTime(duration),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 11.sp
                                ),
                                color = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.width(42.dp)
                            )
                        }

                        // ===== PLAYBACK CONTROLS ROW =====
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (onPreviousEpisode != null) {
                                IconButton(
                                    onClick = onPreviousEpisode!!,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(Color.White.copy(alpha = 0.06f), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SkipPrevious,
                                        contentDescription = "Previous Episode",
                                        tint = Color.White.copy(alpha = 0.8f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                            }

                            IconButton(
                                onClick = { onSeek(((currentTime - 10000L).coerceAtLeast(0L).toFloat() / safeDuration).coerceIn(0f, 1f)) },
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color.White.copy(alpha = 0.06f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Replay10,
                                    contentDescription = "Rewind 10s",
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(36.dp))

                            IconButton(
                                onClick = onPlayPauseToggle,
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(accentPrimary, CircleShape)
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(36.dp))

                            IconButton(
                                onClick = { onSeek(((currentTime + 10000L).coerceAtMost(duration).toFloat() / safeDuration).coerceIn(0f, 1f)) },
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color.White.copy(alpha = 0.06f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Forward10,
                                    contentDescription = "Forward 10s",
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            if (onNextEpisode != null) {
                                Spacer(modifier = Modifier.width(16.dp))
                                IconButton(
                                    onClick = onNextEpisode!!,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(Color.White.copy(alpha = 0.06f), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SkipNext,
                                        contentDescription = "Next Episode",
                                        tint = Color.White.copy(alpha = 0.8f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        // ===== SETTINGS PILLS =====
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ModernPill(
                                label = "${currentSpeed}x",
                                icon = Icons.Default.Speed,
                                onClick = { activeSettingsSheet = PlayerSettingsType.SPEED }
                            )

                            val currentSub = subtitleTracks.firstOrNull { it.isSelected }?.label ?: "Off"
                            ModernPill(
                                label = currentSub,
                                subtitle = "Subs",
                                icon = Icons.Default.ClosedCaption,
                                onClick = { activeSettingsSheet = PlayerSettingsType.SUBTITLES }
                            )

                            val currentAudio = audioTracks.firstOrNull { it.isSelected }?.label ?: "Default"
                            ModernPill(
                                label = currentAudio,
                                subtitle = "Audio",
                                icon = Icons.Default.VolumeUp,
                                onClick = { activeSettingsSheet = PlayerSettingsType.AUDIO }
                            )

                            ModernPill(
                                label = currentScale.label,
                                subtitle = "Zoom",
                                icon = Icons.Default.AspectRatio,
                                onClick = onScaleCycle
                            )

                            if (streams.isNotEmpty()) {
                                val currentSrc = currentStream?.addonName ?: currentStream?.name ?: "Alt"
                                ModernPill(
                                    label = currentSrc,
                                    subtitle = "Source",
                                    icon = Icons.Default.List,
                                    onClick = { activeSettingsSheet = PlayerSettingsType.SOURCES }
                                )
                            }

                            // Sleep timer pill
                            val sleepMs = sleepTimerRemainingMs
                            val sleepLabel = when {
                                sleepMs == null -> "Timer"
                                sleepMs >= 3600000L -> "${sleepMs / 3600000}h"
                                sleepMs >= 60000L -> "${sleepMs / 60000}m"
                                else -> "<1m"
                            }
                            ModernPill(
                                label = sleepLabel,
                                subtitle = "Sleep",
                                icon = Icons.Default.Timer,
                                onClick = { activeSettingsSheet = PlayerSettingsType.SLEEP }
                            )
                        }
                    }
                }
            }
        }

        // ===== SETTINGS OVERLAY SHEETS =====
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
                                .background(if (isSelected) accentPrimary.copy(alpha = 0.15f) else Color.Transparent)
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
                                    selectedColor = accentPrimary,
                                    unselectedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            )
                            Text(
                                text = "${speed}x",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
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
                            .background(if (isOffSelected) accentPrimary.copy(alpha = 0.15f) else Color.Transparent)
                            .clickable {
                                onSubtitleTrackChange(-1, -1)
                                activeSettingsSheet = null
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        RadioButton(
                            selected = isOffSelected,
                            onClick = {
                                onSubtitleTrackChange(-1, -1)
                                activeSettingsSheet = null
                            },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = accentPrimary,
                                unselectedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        )
                        Text(
                            text = "Off",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (isOffSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (subtitleTracks.isEmpty()) {
                        Text(
                            text = "No subtitle tracks available",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(subtitleTracks, key = { "${it.index}_${it.id}" }) { track ->
                                val isSelected = track.isSelected
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) accentPrimary.copy(alpha = 0.15f) else Color.Transparent)
                                        .clickable {
                                            onSubtitleTrackChange(track.index, track.id.toIntOrNull() ?: 0)
                                            activeSettingsSheet = null
                                        }
                                        .padding(horizontal = 16.dp, vertical = 12.dp)
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = {
                                            onSubtitleTrackChange(track.index, track.id.toIntOrNull() ?: 0)
                                            activeSettingsSheet = null
                                        },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = accentPrimary,
                                            unselectedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    )
                                    Text(
                                        text = track.label,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.padding(start = 12.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Font Size",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(12 to "Small", 16 to "Med", 20 to "Large", 28 to "XL").forEach { (size, label) ->
                            FilterChip(
                                selected = subtitleStyle.fontSizeSp == size,
                                onClick = { onSubtitleStyleChange(subtitleStyle.copy(fontSizeSp = size)) },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Opacity",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(0.0f to "None", 0.15f to "Light", 0.30f to "Med", 0.50f to "Heavy").forEach { (opacity, label) ->
                            FilterChip(
                                selected = subtitleStyle.bgOpacity == opacity,
                                onClick = { onSubtitleStyleChange(subtitleStyle.copy(bgOpacity = opacity)) },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                            )
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
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(audioTracks, key = { "${it.index}_${it.id}" }) { track ->
                                val isSelected = track.isSelected
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) accentPrimary.copy(alpha = 0.15f) else Color.Transparent)
                                        .clickable {
                                            onAudioTrackChange(track.index, track.id.toIntOrNull() ?: 0)
                                            activeSettingsSheet = null
                                        }
                                        .padding(horizontal = 16.dp, vertical = 12.dp)
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = {
                                            onAudioTrackChange(track.index, track.id.toIntOrNull() ?: 0)
                                            activeSettingsSheet = null
                                        },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = accentPrimary,
                                            unselectedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    )
                                    Text(
                                        text = track.label,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
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
                    title = "Sources",
                    onDismiss = { activeSettingsSheet = null }
                ) {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(streams, key = { it.url ?: it.infoHash ?: it.name ?: it.hashCode().toString() }) { stream ->
                            val isSelected = when {
                                currentStream?.url != null -> currentStream.url == stream.url
                                currentStream?.externalUrl != null -> currentStream.externalUrl == stream.externalUrl
                                currentStream?.infoHash != null -> currentStream.infoHash == stream.infoHash
                                else -> currentStream?.name != null && currentStream.name == stream.name
                            }
                            Row(
                                verticalAlignment = Alignment.Top,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) accentPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
                                    .then(
                                        if (isSelected) Modifier.border(1.dp, accentPrimary.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
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
                                            text = stream.addonName ?: stream.name ?: "Unknown",
                                            color = MaterialTheme.colorScheme.onSurface,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold
                                            ),
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(99.dp))
                                                    .background(accentPrimary)
                                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "Playing",
                                                    color = MaterialTheme.colorScheme.onPrimary,
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
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
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

            PlayerSettingsType.SCALE -> { /* Cycling via pill click — no overlay sheet */ }
            PlayerSettingsType.SLEEP -> {
                PlayerOverlaySheet(
                    title = "Sleep Timer",
                    onDismiss = { activeSettingsSheet = null }
                ) {
                    val sleepOptions = listOf(
                        null to "Off",
                        900000L to "15 minutes",
                        1800000L to "30 minutes",
                        3600000L to "60 minutes",
                    )
                    sleepOptions.forEach { (ms, label) ->
                        val isSelected = sleepTimerRemainingMs == ms
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) accentPrimary.copy(alpha = 0.15f) else Color.Transparent)
                                .clickable {
                                    onSleepTimerSet(ms)
                                    activeSettingsSheet = null
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    onSleepTimerSet(ms)
                                    activeSettingsSheet = null
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = accentPrimary,
                                    unselectedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            )
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(start = 12.dp)
                            )
                        }
                    }
                }
            }
            null -> { /* Do nothing */ }
        }
    }
}

@Composable
fun ModernPill(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        color = Color.White.copy(alpha = 0.08f),
        contentColor = Color.White,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(13.dp),
                tint = Color.White.copy(alpha = 0.7f)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp
                ),
                maxLines = 1,
                color = Color.White.copy(alpha = 0.9f)
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
                .width(340.dp)
                .windowInsetsPadding(WindowInsets.statusBars.only(WindowInsetsSides.Top))
                .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
        ) {
            ContentCard(
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
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
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), thickness = 1.dp)

                    Spacer(modifier = Modifier.height(12.dp))

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

@Composable
private fun ChapterMarkerOverlay(
    chapters: List<ChapterInfo>,
    duration: Long,
) {
    if (chapters.isEmpty() || duration <= 0) return
    val chapterList = remember(chapters, duration) { chapters }
    val dur = remember(duration) { duration }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
        val barWidth = size.width
        val dotRadius = 2.dp.toPx()
        chapterList.forEach { chapter ->
            val fraction = (chapter.startMs.toFloat() / dur).coerceIn(0f, 1f)
            if (fraction > 0.01f && fraction < 0.99f) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.6f),
                    radius = dotRadius,
                    center = Offset(
                        x = barWidth * fraction,
                        y = size.height / 2
                    )
                )
            }
        }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    val secStr = seconds.toString().padStart(2, '0')
    return if (hours > 0) {
        "${hours}:${minutes.toString().padStart(2, '0')}:$secStr"
    } else {
        "${minutes}:$secStr"
    }
}
