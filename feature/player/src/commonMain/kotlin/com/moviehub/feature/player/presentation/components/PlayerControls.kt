package com.moviehub.feature.player.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moviehub.core.model.AudioTrack
import com.moviehub.core.model.ChapterInfo
import com.moviehub.core.model.StreamItem
import com.moviehub.core.model.SubtitleStyle
import com.moviehub.core.model.SubtitleTrack
import com.moviehub.core.model.VideoScale
import com.moviehub.core.model.VideoTrack
import com.moviehub.core.ui.theme.MovieHubDimens
import com.moviehub.feature.player.presentation.CastButton
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

enum class PlayerSettingsType {
    SPEED,
    TRACKS,
    SUBTITLES,
    AUDIO,
    SOURCES,
    SCALE,
    SLEEP,
    RESOLUTION,
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
    seekIncrement: Int = 10,
    onSeekIndicatorChange: ((String) -> Unit)? = null,
    onSpeedChange: (Float) -> Unit = {},
    onAudioTrackChange: (Int, Int) -> Unit = { _, _ -> },
    onSubtitleTrackChange: (Int, Int) -> Unit = { _, _ -> },
    onVideoTrackChange: (Int) -> Unit = {},
    audioTracks: List<AudioTrack> = emptyList(),
    videoTracks: List<VideoTrack> = emptyList(),
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
    preferredResolution: String = "Auto",
    onResolutionChange: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var activeSettingsSheet by remember { mutableStateOf<PlayerSettingsType?>(null) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(feedbackMessage) {
        if (feedbackMessage != null) {
            kotlinx.coroutines.delay(2000)
            feedbackMessage = null
        }
    }
    val safeDuration = if (duration > 0L) duration else 1L

    val accentPrimary = MaterialTheme.colorScheme.primary
    val accentPrimaryLight = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
    val accentGradient = Brush.horizontalGradient(
        colors = listOf(accentPrimary, accentPrimaryLight),
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
            modifier = Modifier.fillMaxSize(),
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
                                    Color.Transparent,
                                ),
                            ),
                        )
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                        .padding(top = MovieHubDimens.Spacing.xs, bottom = MovieHubDimens.Spacing.lg, start = MovieHubDimens.Spacing.md, end = MovieHubDimens.Spacing.md),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(MovieHubDimens.Avatar.sm)
                                .background(
                                    Color.White.copy(alpha = 0.12f),
                                    CircleShape,
                                )
                                .clip(CircleShape)
                                .clickable(onClick = onBackClick),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White,
                                modifier = Modifier.size(MovieHubDimens.Spacing.xl),
                            )
                        }

                        Spacer(modifier = Modifier.width(MovieHubDimens.Spacing.ml))

                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.3.sp,
                            ),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.xs),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Surface(
                                onClick = onScreenLockToggle,
                                color = if (isScreenLocked) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.12f),
                                shape = CircleShape,
                            ) {
                                Icon(
                                    imageVector = if (isScreenLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                    contentDescription = if (isScreenLocked) "Unlock" else "Lock",
                                    tint = if (isScreenLocked) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.padding(MovieHubDimens.Spacing.xs).size(MovieHubDimens.Spacing.ml),
                                )
                            }
                            // Actual stream resolution badge
                            if (videoResolution.isNotBlank()) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                    shape = RoundedCornerShape(MovieHubDimens.Spacing.xxs),
                                ) {
                                    Text(
                                        text = videoResolution,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = MovieHubDimens.Font.xs,
                                            letterSpacing = 0.5.sp,
                                        ),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = MovieHubDimens.Spacing.xs, vertical = MovieHubDimens.Spacing.dp2),
                                    )
                                }
                            }
                            // Audio/codec info badge
                            if (!videoCodec.isNullOrBlank()) {
                                Surface(
                                    color = Color.White.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(MovieHubDimens.Spacing.xxs),
                                ) {
                                    Text(
                                        text = videoCodec.take(6),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = MovieHubDimens.Font.xs,
                                            letterSpacing = 0.5.sp,
                                        ),
                                        color = Color.White.copy(alpha = 0.8f),
                                        modifier = Modifier.padding(horizontal = MovieHubDimens.Spacing.xs, vertical = MovieHubDimens.Spacing.dp2),
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
                                    shape = RoundedCornerShape(MovieHubDimens.Spacing.xxs),
                                ) {
                                    Text(
                                        text = bitrateLabel,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = MovieHubDimens.Font.xs,
                                            letterSpacing = 0.3.sp,
                                        ),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = MovieHubDimens.Spacing.xs, vertical = MovieHubDimens.Spacing.dp2),
                                    )
                                }
                            }
                            Surface(
                                onClick = onToggleDebug,
                                color = if (showDebugOverlay) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.12f),
                                shape = CircleShape,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Debug Info",
                                    tint = if (showDebugOverlay) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.padding(MovieHubDimens.Spacing.xs).size(MovieHubDimens.Player.seekBarThumb),
                                )
                            }
                            Surface(
                                onClick = onEnterPip,
                                color = Color.White.copy(alpha = 0.12f),
                                shape = CircleShape,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PictureInPictureAlt,
                                    contentDescription = "Picture-in-Picture",
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.padding(MovieHubDimens.Spacing.xs).size(MovieHubDimens.Player.seekBarThumb),
                                )
                            }
                            CastButton(
                                modifier = Modifier.size(MovieHubDimens.Avatar.sm),
                            )
                        }
                    }
                }

                // ===== CENTER PLAYBACK CONTROLS =====
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (onPreviousEpisode != null) {
                            val previousInteractionSource = remember { MutableInteractionSource() }
                            val isPreviousFocused by previousInteractionSource.collectIsFocusedAsState()
                            IconButton(
                                onClick = onPreviousEpisode,
                                interactionSource = previousInteractionSource,
                                modifier = Modifier
                                    .size(MovieHubDimens.Player.controlSize)
                                    .background(
                                        if (isPreviousFocused) accentPrimary.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.06f),
                                        CircleShape,
                                    )
                                    .border(
                                        width = if (isPreviousFocused) MovieHubDimens.Player.controlsFocusBorder else MovieHubDimens.Spacing.dp0,
                                        color = if (isPreviousFocused) accentPrimary else Color.Transparent,
                                        shape = CircleShape,
                                    ),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipPrevious,
                                    contentDescription = "Previous Episode",
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(MovieHubDimens.Icon.lg),
                                )
                            }
                            Spacer(modifier = Modifier.width(MovieHubDimens.Spacing.lg))
                        }

                        val rewindInteractionSource = remember { MutableInteractionSource() }
                        val isRewindFocused by rewindInteractionSource.collectIsFocusedAsState()
                        IconButton(
                            onClick = {
                                val seekMs = seekIncrement * 1000L
                                onSeek(((currentTime - seekMs).coerceAtLeast(0L).toFloat() / safeDuration).coerceIn(0f, 1f))
                                onSeekIndicatorChange?.invoke("-${seekIncrement}s")
                            },
                            interactionSource = rewindInteractionSource,
                            modifier = Modifier
                                .size(MovieHubDimens.Player.controlSize)
                                .background(
                                    if (isRewindFocused) accentPrimary.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.06f),
                                    CircleShape,
                                )
                                .border(
                                    width = if (isRewindFocused) MovieHubDimens.Player.controlsFocusBorder else MovieHubDimens.Spacing.dp0,
                                    color = if (isRewindFocused) accentPrimary else Color.Transparent,
                                    shape = CircleShape,
                                ),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Replay10,
                                contentDescription = "Rewind 10s",
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(MovieHubDimens.Icon.lg),
                            )
                        }

                        Spacer(modifier = Modifier.width(MovieHubDimens.Player.pillHeight))

                        val playPauseInteractionSource = remember { MutableInteractionSource() }
                        val isPlayPauseFocused by playPauseInteractionSource.collectIsFocusedAsState()
                        IconButton(
                            onClick = onPlayPauseToggle,
                            interactionSource = playPauseInteractionSource,
                            modifier = Modifier
                                .size(MovieHubDimens.Icon.jumbo)
                                .background(
                                    if (isPlayPauseFocused) accentPrimary.copy(alpha = 0.85f) else accentPrimary,
                                    CircleShape,
                                )
                                .border(
                                    width = if (isPlayPauseFocused) MovieHubDimens.Player.controlsFocusBorderLarge else MovieHubDimens.Spacing.dp0,
                                    color = if (isPlayPauseFocused) Color.White else Color.Transparent,
                                    shape = CircleShape,
                                ),
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(MovieHubDimens.Icon.xxl),
                            )
                        }

                        Spacer(modifier = Modifier.width(MovieHubDimens.Player.pillHeight))

                        val forwardInteractionSource = remember { MutableInteractionSource() }
                        val isForwardFocused by forwardInteractionSource.collectIsFocusedAsState()
                        IconButton(
                            onClick = {
                                val seekMs = seekIncrement * 1000L
                                onSeek(((currentTime + seekMs).coerceAtMost(duration).toFloat() / safeDuration).coerceIn(0f, 1f))
                                onSeekIndicatorChange?.invoke("+${seekIncrement}s")
                            },
                            interactionSource = forwardInteractionSource,
                            modifier = Modifier
                                .size(MovieHubDimens.Player.controlSize)
                                .background(
                                    if (isForwardFocused) accentPrimary.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.06f),
                                    CircleShape,
                                )
                                .border(
                                    width = if (isForwardFocused) MovieHubDimens.Player.controlsFocusBorder else MovieHubDimens.Spacing.dp0,
                                    color = if (isForwardFocused) accentPrimary else Color.Transparent,
                                    shape = CircleShape,
                                ),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Forward10,
                                contentDescription = "Forward 10s",
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(MovieHubDimens.Icon.lg),
                            )
                        }

                        if (onNextEpisode != null) {
                            Spacer(modifier = Modifier.width(MovieHubDimens.Spacing.lg))
                            val nextInteractionSource = remember { MutableInteractionSource() }
                            val isNextFocused by nextInteractionSource.collectIsFocusedAsState()
                            IconButton(
                                onClick = onNextEpisode,
                                interactionSource = nextInteractionSource,
                                modifier = Modifier
                                    .size(MovieHubDimens.Player.controlSize)
                                    .background(
                                        if (isNextFocused) accentPrimary.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.06f),
                                        CircleShape,
                                    )
                                    .border(
                                        width = if (isNextFocused) MovieHubDimens.Player.controlsFocusBorder else MovieHubDimens.Spacing.dp0,
                                        color = if (isNextFocused) accentPrimary else Color.Transparent,
                                        shape = CircleShape,
                                    ),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipNext,
                                    contentDescription = "Next Episode",
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(MovieHubDimens.Icon.lg),
                                )
                            }
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
                                    Color.Black.copy(alpha = 0.85f),
                                ),
                            ),
                        )
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = MovieHubDimens.Spacing.md)
                            .padding(top = MovieHubDimens.Icon.xl, bottom = MovieHubDimens.Spacing.ms),
                        verticalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.sm),
                    ) {
                        // ===== SEEK BAR WITH TIME LABELS =====
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.sm),
                        ) {
                            Text(
                                text = formatTime(currentTime),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = MovieHubDimens.Font.sm,
                                ),
                                color = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.width(MovieHubDimens.Player.seekBarTimeWidth),
                                textAlign = TextAlign.End,
                            )

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(MovieHubDimens.Spacing.xxl)
                                    .clip(RoundedCornerShape(MovieHubDimens.Spacing.xxs))
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                    ) { },
                            ) {
                                ChapterMarkerOverlay(
                                    chapters = chapters,
                                    duration = duration,
                                    modifier = Modifier.align(Alignment.Center),
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(MovieHubDimens.Spacing.xs)
                                        .align(Alignment.Center)
                                        .clip(RoundedCornerShape(MovieHubDimens.Spacing.dp3))
                                        .background(Color.White.copy(alpha = 0.15f)),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(fraction = progress.coerceIn(0f, 1f))
                                            .background(
                                                accentGradient,
                                                RoundedCornerShape(MovieHubDimens.Spacing.dp3),
                                            ),
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
                                        disabledThumbColor = accentPrimaryLight,
                                    ),
                                    thumb = {
                                        if (progress > 0f) {
                                            Box(
                                                modifier = Modifier
                                                    .size(MovieHubDimens.Spacing.ml)
                                                    .background(accentPrimaryLight, CircleShape)
                                                    .border(MovieHubDimens.Spacing.dp2, Color.White.copy(alpha = 0.8f), CircleShape),
                                            )
                                        }
                                    },
                                )
                            }

                            Text(
                                text = formatTime(duration),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = MovieHubDimens.Font.sm,
                                ),
                                color = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.width(MovieHubDimens.Player.seekBarTimeWidth),
                            )
                        }

                        // ===== SETTINGS PILLS =====
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.sm),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            ModernPill(
                                label = "${currentSpeed}x",
                                icon = Icons.Default.Speed,
                                onClick = { activeSettingsSheet = PlayerSettingsType.SPEED },
                            )

                            val currentAudio = audioTracks.firstOrNull { it.isSelected }?.label ?: "Default"
                            ModernPill(
                                label = currentAudio,
                                subtitle = "Audio",
                                icon = Icons.Default.VolumeUp,
                                onClick = { activeSettingsSheet = PlayerSettingsType.TRACKS },
                            )

                            val currentSub = subtitleTracks.firstOrNull { it.isSelected }?.label ?: "Off"
                            ModernPill(
                                label = currentSub,
                                subtitle = "Subs",
                                icon = Icons.Default.ClosedCaption,
                                onClick = { activeSettingsSheet = PlayerSettingsType.SUBTITLES },
                            )

                            ModernPill(
                                label = currentScale.label,
                                subtitle = "Zoom",
                                icon = Icons.Default.AspectRatio,
                                onClick = onScaleCycle,
                            )

                            ModernPill(
                                label = preferredResolution,
                                subtitle = "Quality",
                                icon = Icons.Default.HighQuality,
                                onClick = { activeSettingsSheet = PlayerSettingsType.RESOLUTION },
                            )

                            if (streams.isNotEmpty()) {
                                val currentSrc = currentStream?.addonName ?: currentStream?.name ?: "Alt"
                                ModernPill(
                                    label = currentSrc,
                                    subtitle = "Source",
                                    icon = Icons.Default.List,
                                    onClick = { activeSettingsSheet = PlayerSettingsType.SOURCES },
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
                                onClick = { activeSettingsSheet = PlayerSettingsType.SLEEP },
                            )
                        }
                    }
                }
            }
        }

        // ===== SETTINGS OVERLAY SHEETS =====
        when (activeSettingsSheet) {
            PlayerSettingsType.TRACKS -> {
                var selectedAudioIdx by remember { mutableIntStateOf(audioTracks.indexOfFirst { it.isSelected }.coerceAtLeast(0)) }

                PlayerOverlaySheet(
                    title = "Audio Track",
                    onDismiss = { activeSettingsSheet = null },
                    maxWidth = MovieHubDimens.Sheet.maxWidth,
                ) {
                    if (audioTracks.isEmpty()) {
                        Text(
                            text = "No audio tracks available",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.padding(MovieHubDimens.Spacing.lg),
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.sm),
                                modifier = Modifier.padding(bottom = MovieHubDimens.Spacing.md),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Audiotrack,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(MovieHubDimens.Player.seekBarThumb),
                                )
                                Text(
                                    text = "Select Audio Stream",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.sm),
                                modifier = Modifier.fillMaxWidth().weight(1f),
                            ) {
                                items(audioTracks, key = { "a_${it.index}_${it.id}" }) { track ->
                                    val idx = audioTracks.indexOf(track)
                                    val isSelected = idx == selectedAudioIdx
                                    val audioMetaText = remember(track) {
                                        val parts = mutableListOf<String>()
                                        val lang = track.language
                                        if (!lang.isNullOrBlank()) {
                                            parts.add(lang.uppercase())
                                        }
                                        val cod = track.codec
                                        if (!cod.isNullOrBlank()) {
                                            parts.add(cod.uppercase())
                                        }
                                        if (track.channels > 0) {
                                            parts.add(
                                                when (track.channels) {
                                                    2 -> "Stereo"
                                                    6 -> "5.1"
                                                    8 -> "7.1"
                                                    else -> "${track.channels}ch"
                                                },
                                            )
                                        }
                                        if (track.bitrate > 0) {
                                            parts.add("${track.bitrate / 1000}kbps")
                                        }
                                        parts.joinToString(" • ")
                                    }

                                    KineticSelectorRow(
                                        label = track.label,
                                        subtext = audioMetaText.takeIf { it.isNotBlank() },
                                        isSelected = isSelected,
                                        onClick = { selectedAudioIdx = idx },
                                        trailingIcon = {
                                            if (isSelected) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.sm),
                                                ) {
                                                    AcousticWaveform(MaterialTheme.colorScheme.primary)
                                                    Icon(
                                                        imageVector = Icons.Default.CheckCircle,
                                                        contentDescription = "Selected",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(MovieHubDimens.Spacing.xl),
                                                    )
                                                }
                                            }
                                        },
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(MovieHubDimens.Spacing.md))
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                        Spacer(modifier = Modifier.height(MovieHubDimens.Spacing.sm))

                        // Apply + Cancel bottom-right
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            OutlinedButton(
                                onClick = { activeSettingsSheet = null },
                                shape = RoundedCornerShape(MovieHubDimens.Radius.md),
                                modifier = Modifier.padding(end = MovieHubDimens.Spacing.sm),
                            ) {
                                Text("Cancel")
                            }
                            Button(
                                onClick = {
                                    val audioTrack = audioTracks.getOrNull(selectedAudioIdx)
                                    if (audioTrack != null) {
                                        onAudioTrackChange(audioTrack.index, audioTrack.id.toIntOrNull() ?: 0)
                                    }
                                    activeSettingsSheet = null
                                },
                                shape = RoundedCornerShape(MovieHubDimens.Radius.md),
                            ) {
                                Text("Apply")
                            }
                        }
                    }
                }
            }

            PlayerSettingsType.SPEED -> {
                PlayerOverlaySheet(
                    title = "Playback Speed",
                    onDismiss = { activeSettingsSheet = null },
                ) {
                    Spacer(modifier = Modifier.height(MovieHubDimens.Spacing.sm))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(vertical = MovieHubDimens.Spacing.md),
                        horizontalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                            val isSelected = currentSpeed == speed
                            val accentPrimary = MaterialTheme.colorScheme.primary
                            val onSurface = MaterialTheme.colorScheme.onSurface

                            var isPressed by remember { mutableStateOf(false) }
                            val scale by animateFloatAsState(
                                targetValue = if (isPressed) 0.90f else 1.0f,
                                animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
                                label = "SpeedPressScale",
                            )
                            val bgAlpha by animateFloatAsState(
                                targetValue = if (isSelected) 0.20f else 0.05f,
                                label = "SpeedBgAlpha",
                            )

                            Box(
                                modifier = Modifier
                                    .graphicsLayer(scaleX = scale, scaleY = scale)
                                    .clip(RoundedCornerShape(MovieHubDimens.Spacing.xl))
                                    .background(
                                        if (isSelected) {
                                            accentPrimary.copy(alpha = bgAlpha)
                                        } else {
                                            onSurface.copy(alpha = bgAlpha)
                                        },
                                    )
                                    .border(
                                        width = MovieHubDimens.Spacing.dp1,
                                        color = if (isSelected) accentPrimary.copy(alpha = 0.5f) else onSurface.copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(MovieHubDimens.Spacing.xl),
                                    )
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onPress = {
                                                isPressed = true
                                                tryAwaitRelease()
                                                isPressed = false
                                            },
                                            onTap = {
                                                onSpeedChange(speed)
                                                activeSettingsSheet = null
                                            },
                                        )
                                    }
                                    .padding(horizontal = MovieHubDimens.Spacing.xl, vertical = MovieHubDimens.Spacing.md),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "${speed}x",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    ),
                                    color = if (isSelected) accentPrimary else onSurface.copy(alpha = 0.8f),
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(MovieHubDimens.Spacing.sm))
                }
            }

            PlayerSettingsType.SUBTITLES -> {
                val isOffSelected = subtitleTracks.none { it.isSelected }

                // Track states for search & customization tabs
                var searchQuery by remember { mutableStateOf("") }
                var selectedTab by remember { mutableStateOf(0) } // 0 = Typography, 1 = Color & FX, 2 = Presets
                var customPresetName by remember { mutableStateOf("") }
                var showSavePresetDialog by remember { mutableStateOf(false) }
                var showRenamePresetDialog by remember { mutableStateOf(false) }
                var renameTargetIndex by remember { mutableStateOf(-1) }
                var renameNewName by remember { mutableStateOf("") }
                val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

                var customPresets by remember {
                    mutableStateOf(
                        listOf(
                            SubtitlePreset("Netflix Style", SubtitleStyle(fontSizeSp = 18, fontColorArgb = 0xFFFFFFFF.toInt(), bgOpacity = 0.50f, edgeStyle = "None"), isBuiltIn = true),
                            SubtitlePreset("Prime Video", SubtitleStyle(fontSizeSp = 16, fontColorArgb = 0xFFFFFFFF.toInt(), bgOpacity = 1.0f, edgeStyle = "Outline", outlineColorArgb = 0xFF000000.toInt()), isBuiltIn = true),
                            SubtitlePreset("Disney+", SubtitleStyle(fontSizeSp = 18, fontFamily = "Serif", fontColorArgb = 0xFFFFFFFF.toInt(), bgOpacity = 0.30f, edgeStyle = "Shadow"), isBuiltIn = true),
                            SubtitlePreset("High Contrast", SubtitleStyle(fontSizeSp = 20, fontFamily = "Monospace", fontColorArgb = 0xFFFFE600.toInt(), bgOpacity = 1.0f, isBold = true, edgeStyle = "None"), isBuiltIn = true),
                            SubtitlePreset("Accessibility Large", SubtitleStyle(fontSizeSp = 26, fontColorArgb = 0xFFFFFFFF.toInt(), bgOpacity = 1.0f, isBold = true, edgeStyle = "Outline", outlineColorArgb = 0xFF000000.toInt()), isBuiltIn = true),
                        ),
                    )
                }

                // Filter subtitle tracks based on search query
                val filteredTracks = remember(subtitleTracks, searchQuery) {
                    if (searchQuery.isBlank()) {
                        subtitleTracks
                    } else {
                        subtitleTracks.filter {
                            it.label.contains(searchQuery, ignoreCase = true) ||
                                (it.language ?: "").contains(searchQuery, ignoreCase = true)
                        }
                    }
                }

                PlayerOverlaySheet(
                    title = "Subtitle & Styling Center",
                    maxWidth = 950.dp,
                    onDismiss = { activeSettingsSheet = null },
                ) {
                    // 50/50 Split Layout
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(MovieHubDimens.Spacing.md),
                        horizontalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.lg),
                    ) {
                        // ═══════════════════════════════════════════
                        // LEFT PANEL (50%) - Subtitle Sources & Search
                        // ═══════════════════════════════════════════
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                        ) {
                            Text(
                                text = "Subtitle Sources",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = MovieHubDimens.Spacing.sm),
                            )

                            // Search Box
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Search languages or files...", style = MaterialTheme.typography.bodyMedium) },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = MovieHubDimens.Spacing.sm),
                                shape = RoundedCornerShape(MovieHubDimens.Radius.lg),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                                ),
                            )

                            // Track Sources list
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .clip(RoundedCornerShape(MovieHubDimens.Radius.lg))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                    .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(MovieHubDimens.Radius.lg)),
                            ) {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize().padding(MovieHubDimens.Spacing.xs),
                                    verticalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.xs),
                                ) {
                                    // "Off" Track Option
                                    item {
                                        KineticSelectorRow(
                                            label = "Disable Subtitles",
                                            subtext = "No captions rendered",
                                            isSelected = isOffSelected,
                                            onClick = {
                                                onSubtitleTrackChange(-1, -1)
                                            },
                                        )
                                    }

                                    // Category Header: Embedded / External
                                    if (filteredTracks.isNotEmpty()) {
                                        item {
                                            Text(
                                                text = "Available Tracks",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                                modifier = Modifier.padding(start = MovieHubDimens.Spacing.md, top = MovieHubDimens.Spacing.sm, bottom = MovieHubDimens.Spacing.xxs),
                                            )
                                        }

                                        items(filteredTracks, key = { "${it.index}_${it.id}" }) { track ->
                                            val isSelected = track.isSelected
                                            val isEmbedded = track.id.contains("embed", ignoreCase = true) || track.index < 2
                                            KineticSelectorRow(
                                                label = track.label,
                                                subtext = if (isEmbedded) "Embedded Audio Stream" else "Add-on Stream Scrape",
                                                isSelected = isSelected,
                                                onClick = {
                                                    onSubtitleTrackChange(track.index, track.id.toIntOrNull() ?: 0)
                                                },
                                                trailingIcon = {
                                                    if (isSelected) {
                                                        Icon(
                                                            imageVector = Icons.Default.CheckCircle,
                                                            contentDescription = "Active",
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(MovieHubDimens.Spacing.xl),
                                                        )
                                                    }
                                                },
                                            )
                                        }
                                    } else {
                                        item {
                                            Box(
                                                modifier = Modifier.fillMaxWidth().padding(MovieHubDimens.Spacing.lg),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Text(
                                                    text = if (searchQuery.isEmpty()) "No subtitles found" else "No matching tracks",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                                )
                                            }
                                        }
                                    }

                                    // External File Import Button
                                    item {
                                        Spacer(modifier = Modifier.height(MovieHubDimens.Spacing.sm))
                                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                                        Spacer(modifier = Modifier.height(MovieHubDimens.Spacing.sm))

                                        Button(
                                            onClick = {
                                                feedbackMessage = "Local file explorer is currently not supported on this sandbox."
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = MovieHubDimens.Spacing.md),
                                            shape = RoundedCornerShape(MovieHubDimens.Radius.md),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                                                contentColor = MaterialTheme.colorScheme.secondary,
                                            ),
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(MovieHubDimens.Spacing.xs))
                                            Text("Import Custom .SRT / .VTT File", style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }
                                }
                            }
                        }

                        // Vertical separator
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(1.dp)
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                        )

                        // ═══════════════════════════════════════════
                        // RIGHT PANEL (50%) - Style Customization & Preview
                        // ═══════════════════════════════════════════
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                        ) {
                            Text(
                                text = "Appearance & Customization",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = MovieHubDimens.Spacing.sm),
                            )

                            // WYSIWYG REAL-TIME PREVIEW
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp)
                                    .clip(RoundedCornerShape(MovieHubDimens.Radius.lg))
                                    .background(Color.Black.copy(alpha = 0.85f))
                                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(MovieHubDimens.Radius.lg))
                                    .padding(MovieHubDimens.Spacing.md),
                                contentAlignment = Alignment.Center,
                            ) {
                                // Real-time rendered caption preview
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(),
                                    contentAlignment = Alignment.BottomCenter,
                                ) {
                                    val textCol = if (subtitleStyle.fontColorArgb == -1) Color.White else Color(subtitleStyle.fontColorArgb)
                                    val bgCol = Color(subtitleStyle.bgColorArgb).copy(alpha = subtitleStyle.bgOpacity)

                                    // Custom edge shadow / outline implementation
                                    val shadowList = remember(subtitleStyle) {
                                        when (subtitleStyle.edgeStyle) {
                                            "Shadow" -> listOf(
                                                androidx.compose.ui.graphics.Shadow(
                                                    color = Color(subtitleStyle.shadowColorArgb),
                                                    offset = androidx.compose.ui.geometry.Offset(subtitleStyle.shadowOffsetDp * 2, subtitleStyle.shadowOffsetDp * 2),
                                                    blurRadius = subtitleStyle.shadowRadiusDp * 2,
                                                ),
                                            )
                                            "Outline" -> listOf(
                                                // Create a bold outline using multiple shadows at offsets
                                                androidx.compose.ui.graphics.Shadow(color = Color(subtitleStyle.outlineColorArgb), offset = Offset(-subtitleStyle.outlineThicknessDp, -subtitleStyle.outlineThicknessDp), blurRadius = 0f),
                                                androidx.compose.ui.graphics.Shadow(color = Color(subtitleStyle.outlineColorArgb), offset = Offset(subtitleStyle.outlineThicknessDp, -subtitleStyle.outlineThicknessDp), blurRadius = 0f),
                                                androidx.compose.ui.graphics.Shadow(color = Color(subtitleStyle.outlineColorArgb), offset = Offset(-subtitleStyle.outlineThicknessDp, subtitleStyle.outlineThicknessDp), blurRadius = 0f),
                                                androidx.compose.ui.graphics.Shadow(color = Color(subtitleStyle.outlineColorArgb), offset = Offset(subtitleStyle.outlineThicknessDp, subtitleStyle.outlineThicknessDp), blurRadius = 0f),
                                            )
                                            else -> emptyList()
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .padding(bottom = MovieHubDimens.Spacing.xs)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(bgCol)
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = "Watching MovieHub in Style...",
                                            color = textCol,
                                            fontSize = subtitleStyle.fontSizeSp.sp,
                                            fontStyle = if (subtitleStyle.isItalic) androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal,
                                            fontWeight = if (subtitleStyle.isBold) FontWeight.Bold else FontWeight.Normal,
                                            letterSpacing = subtitleStyle.letterSpacingSp.sp,
                                            lineHeight = subtitleStyle.lineHeightSp.sp,
                                            style = androidx.compose.ui.text.TextStyle(
                                                fontFamily = when (subtitleStyle.fontFamily) {
                                                    "Serif" -> androidx.compose.ui.text.font.FontFamily.Serif
                                                    "Monospace" -> androidx.compose.ui.text.font.FontFamily.Monospace
                                                    "Cursive" -> androidx.compose.ui.text.font.FontFamily.Cursive
                                                    else -> androidx.compose.ui.text.font.FontFamily.SansSerif
                                                },
                                                shadow = shadowList.firstOrNull(),
                                            ),
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(MovieHubDimens.Spacing.sm))

                            // Tab Row for categories
                            ScrollableTabRow(
                                selectedTabIndex = selectedTab,
                                modifier = Modifier.fillMaxWidth().height(42.dp),
                                edgePadding = 0.dp,
                                containerColor = Color.Transparent,
                                contentColor = MaterialTheme.colorScheme.primary,
                                indicator = { tabPositions ->
                                    if (selectedTab < tabPositions.size) {
                                        TabRowDefaults.SecondaryIndicator(
                                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                },
                            ) {
                                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                                    Text("Typography", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = MovieHubDimens.Spacing.xs))
                                }
                                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                                    Text("Color & FX", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = MovieHubDimens.Spacing.xs))
                                }
                                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }) {
                                    Text("Presets", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = MovieHubDimens.Spacing.xs))
                                }
                            }

                            // Customization Content Box
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .padding(vertical = MovieHubDimens.Spacing.sm),
                            ) {
                                when (selectedTab) {
                                    0 -> {
                                        // TYPOGRAPHY TAB
                                        LazyColumn(
                                            modifier = Modifier.fillMaxSize(),
                                            verticalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.md),
                                        ) {
                                            // Font Family Chips
                                            item {
                                                Text("Font Family", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                                                Row(
                                                    modifier = Modifier.fillMaxWidth().padding(top = MovieHubDimens.Spacing.xs),
                                                    horizontalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.xs),
                                                ) {
                                                    listOf("Sans-Serif", "Serif", "Monospace", "Cursive").forEach { font ->
                                                        FilterChip(
                                                            selected = subtitleStyle.fontFamily == font,
                                                            onClick = { onSubtitleStyleChange(subtitleStyle.copy(fontFamily = font)) },
                                                            label = { Text(font, style = MaterialTheme.typography.labelSmall) },
                                                        )
                                                    }
                                                }
                                            }

                                            // Font Size Slider
                                            item {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically,
                                                ) {
                                                    Text("Font Size", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                                                    Text("${subtitleStyle.fontSizeSp} sp", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                                }
                                                Slider(
                                                    value = subtitleStyle.fontSizeSp.toFloat(),
                                                    onValueChange = { onSubtitleStyleChange(subtitleStyle.copy(fontSizeSp = it.toInt())) },
                                                    valueRange = 12f..36f,
                                                    steps = 24,
                                                )
                                            }

                                            // Letter Spacing Slider
                                            item {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically,
                                                ) {
                                                    Text("Letter Spacing", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                                                    Text(String.format("%.1f sp", subtitleStyle.letterSpacingSp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                                }
                                                Slider(
                                                    value = subtitleStyle.letterSpacingSp,
                                                    onValueChange = { onSubtitleStyleChange(subtitleStyle.copy(letterSpacingSp = it)) },
                                                    valueRange = -2f..8f,
                                                )
                                            }

                                            // Line Height Slider
                                            item {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically,
                                                ) {
                                                    Text("Line Height", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                                                    Text("${subtitleStyle.lineHeightSp} sp", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                                }
                                                Slider(
                                                    value = subtitleStyle.lineHeightSp.toFloat(),
                                                    onValueChange = { onSubtitleStyleChange(subtitleStyle.copy(lineHeightSp = it.toInt())) },
                                                    valueRange = 16f..48f,
                                                    steps = 32,
                                                )
                                            }

                                            // Bold & Italic Toggles
                                            item {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.lg),
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                        Checkbox(
                                                            checked = subtitleStyle.isBold,
                                                            onCheckedChange = { onSubtitleStyleChange(subtitleStyle.copy(isBold = it)) },
                                                        )
                                                        Text("Bold", style = MaterialTheme.typography.bodyMedium)
                                                    }
                                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                        Checkbox(
                                                            checked = subtitleStyle.isItalic,
                                                            onCheckedChange = { onSubtitleStyleChange(subtitleStyle.copy(isItalic = it)) },
                                                        )
                                                        Text("Italic", style = MaterialTheme.typography.bodyMedium)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    1 -> {
                                        // COLORS & EFFECTS TAB
                                        LazyColumn(
                                            modifier = Modifier.fillMaxSize(),
                                            verticalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.md),
                                        ) {
                                            // Text Color row
                                            item {
                                                Text("Text Color", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                                                Row(
                                                    modifier = Modifier.fillMaxWidth().padding(top = MovieHubDimens.Spacing.xs),
                                                    horizontalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.xs),
                                                ) {
                                                    val colors = listOf(-1 to "White", 0xFFFFE600.toInt() to "Yellow", 0xFF00FFCC.toInt() to "Cyan", 0xFF00FF66.toInt() to "Green", 0xFFFF00FF.toInt() to "Magenta", 0xFFFF0000.toInt() to "Red", 0xFF000000.toInt() to "Black")
                                                    colors.forEach { (colorVal, label) ->
                                                        val isSelected = subtitleStyle.fontColorArgb == colorVal
                                                        Box(
                                                            modifier = Modifier
                                                                .size(28.dp)
                                                                .clip(CircleShape)
                                                                .background(if (colorVal == -1) Color.White else Color(colorVal))
                                                                .border(if (isSelected) 3.dp else 1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.2f), CircleShape)
                                                                .clickable { onSubtitleStyleChange(subtitleStyle.copy(fontColorArgb = colorVal)) },
                                                        )
                                                    }
                                                }
                                            }

                                            // Background Opacity
                                            item {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically,
                                                ) {
                                                    Text("Background Opacity", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                                                    Text("${(subtitleStyle.bgOpacity * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                                }
                                                Slider(
                                                    value = subtitleStyle.bgOpacity,
                                                    onValueChange = { onSubtitleStyleChange(subtitleStyle.copy(bgOpacity = it)) },
                                                    valueRange = 0f..1f,
                                                )
                                            }

                                            // Edge Style / Effects chips
                                            item {
                                                Text("Edge Style & Effects", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                                                Row(
                                                    modifier = Modifier.fillMaxWidth().padding(top = MovieHubDimens.Spacing.xs),
                                                    horizontalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.xs),
                                                ) {
                                                    listOf("None", "Outline", "Shadow").forEach { style ->
                                                        FilterChip(
                                                            selected = subtitleStyle.edgeStyle == style,
                                                            onClick = { onSubtitleStyleChange(subtitleStyle.copy(edgeStyle = style)) },
                                                            label = { Text(style, style = MaterialTheme.typography.labelSmall) },
                                                        )
                                                    }
                                                }
                                            }

                                            // Conditional outline/shadow sliders
                                            if (subtitleStyle.edgeStyle == "Outline") {
                                                item {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically,
                                                    ) {
                                                        Text("Outline Thickness", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                                                        Text(String.format("%.1f dp", subtitleStyle.outlineThicknessDp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                                    }
                                                    Slider(
                                                        value = subtitleStyle.outlineThicknessDp,
                                                        onValueChange = { onSubtitleStyleChange(subtitleStyle.copy(outlineThicknessDp = it)) },
                                                        valueRange = 0f..5f,
                                                    )
                                                }
                                            }

                                            if (subtitleStyle.edgeStyle == "Shadow") {
                                                item {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically,
                                                    ) {
                                                        Text("Shadow Radius & Offset", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                                                        Text(String.format("%.1f dp", subtitleStyle.shadowOffsetDp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                                    }
                                                    Slider(
                                                        value = subtitleStyle.shadowOffsetDp,
                                                        onValueChange = { onSubtitleStyleChange(subtitleStyle.copy(shadowOffsetDp = it, shadowRadiusDp = it)) },
                                                        valueRange = 0f..5f,
                                                    )
                                                }
                                            }

                                            // Positioning: Vertical & Margin
                                            item {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically,
                                                ) {
                                                    Text("Vertical Position", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                                                    Text("${subtitleStyle.verticalPositionPercent}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                                }
                                                Slider(
                                                    value = subtitleStyle.verticalPositionPercent.toFloat(),
                                                    onValueChange = { onSubtitleStyleChange(subtitleStyle.copy(verticalPositionPercent = it.toInt())) },
                                                    valueRange = 50f..98f,
                                                    steps = 48,
                                                )
                                            }
                                        }
                                    }
                                    2 -> {
                                        // PRESETS & ACCESSBILITY TAB
                                        LazyColumn(
                                            modifier = Modifier.fillMaxSize(),
                                            verticalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.xs),
                                        ) {
                                            item {
                                                Text(
                                                    text = "Select Subtitle Style Preset",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                                    modifier = Modifier.padding(bottom = MovieHubDimens.Spacing.xs),
                                                )
                                            }

                                            items(customPresets.size) { index ->
                                                val preset = customPresets[index]
                                                val isSelected = subtitleStyle.fontSizeSp == preset.style.fontSizeSp &&
                                                    subtitleStyle.fontColorArgb == preset.style.fontColorArgb &&
                                                    subtitleStyle.bgOpacity == preset.style.bgOpacity &&
                                                    subtitleStyle.edgeStyle == preset.style.edgeStyle

                                                KineticSelectorRow(
                                                    label = preset.name,
                                                    subtext = if (preset.isBuiltIn) "System Preset" else "Custom Style Preset",
                                                    isSelected = isSelected,
                                                    onClick = {
                                                        onSubtitleStyleChange(preset.style)
                                                    },
                                                    trailingIcon = {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            if (!preset.isBuiltIn) {
                                                                IconButton(onClick = {
                                                                    renameTargetIndex = index
                                                                    renameNewName = preset.name
                                                                    showRenamePresetDialog = true
                                                                }) {
                                                                    Icon(Icons.Default.Edit, contentDescription = "Rename", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                                                                }
                                                                IconButton(onClick = {
                                                                    customPresets = customPresets.filterIndexed { idx, _ -> idx != index }
                                                                }) {
                                                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
                                                                }
                                                            }
                                                            if (isSelected) {
                                                                Icon(Icons.Default.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
                                                            }
                                                        }
                                                    },
                                                )
                                            }

                                            // Action Buttons: Save Custom / Duplicate / Reset
                                            item {
                                                Spacer(modifier = Modifier.height(MovieHubDimens.Spacing.xs))
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.xs),
                                                ) {
                                                    // Save Preset
                                                    Button(
                                                        onClick = {
                                                            customPresetName = "My Preset ${customPresets.filter { !it.isBuiltIn }.size + 1}"
                                                            showSavePresetDialog = true
                                                        },
                                                        modifier = Modifier.weight(1f),
                                                        shape = RoundedCornerShape(MovieHubDimens.Radius.md),
                                                    ) {
                                                        Icon(Icons.Default.Save, contentDescription = "Save", modifier = Modifier.size(16.dp))
                                                        Spacer(modifier = Modifier.width(MovieHubDimens.Spacing.xxs))
                                                        Text("Save Current", maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                    }

                                                    // Reset Default
                                                    OutlinedButton(
                                                        onClick = { onSubtitleStyleChange(SubtitleStyle.DEFAULT) },
                                                        modifier = Modifier.weight(1f),
                                                        shape = RoundedCornerShape(MovieHubDimens.Radius.md),
                                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                                                    ) {
                                                        Icon(Icons.Default.Refresh, contentDescription = "Reset", modifier = Modifier.size(16.dp))
                                                        Spacer(modifier = Modifier.width(MovieHubDimens.Spacing.xxs))
                                                        Text("Reset Default", maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                    }
                                                }
                                            }

                                            // Import / Export Presets
                                            item {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth().padding(top = MovieHubDimens.Spacing.xs),
                                                    horizontalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.xs),
                                                ) {
                                                    // Export current style JSON string
                                                    TextButton(
                                                        onClick = {
                                                            try {
                                                                val styleJson = Json.encodeToString(subtitleStyle)
                                                                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(styleJson))
                                                                feedbackMessage = "Preset exported & copied to clipboard!"
                                                            } catch (_: Exception) {
                                                            }
                                                        },
                                                        modifier = Modifier.weight(1f),
                                                    ) {
                                                        Icon(Icons.Default.ContentCopy, contentDescription = "Export", modifier = Modifier.size(14.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Export Style", style = MaterialTheme.typography.bodySmall)
                                                    }

                                                    // Import style JSON string
                                                    TextButton(
                                                        onClick = {
                                                            try {
                                                                val clipText = clipboardManager.getText()?.text
                                                                if (!clipText.isNullOrBlank()) {
                                                                    val imported = Json.decodeFromString<SubtitleStyle>(clipText)
                                                                    onSubtitleStyleChange(imported)
                                                                    feedbackMessage = "Successfully imported styling preset!"
                                                                } else {
                                                                    feedbackMessage = "Clipboard is empty"
                                                                }
                                                            } catch (e: Exception) {
                                                                feedbackMessage = "Invalid preset format in clipboard"
                                                            }
                                                        },
                                                        modifier = Modifier.weight(1f),
                                                    ) {
                                                        Icon(Icons.Default.Download, contentDescription = "Import", modifier = Modifier.size(14.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Import Clipboard", style = MaterialTheme.typography.bodySmall)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Preset Naming/Rename Dialogs
                if (showSavePresetDialog) {
                    AlertDialog(
                        onDismissRequest = { showSavePresetDialog = false },
                        title = { Text("Save Style Preset") },
                        text = {
                            Column {
                                Text("Enter a name for your custom subtitle styling preset:", style = MaterialTheme.typography.bodyMedium)
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = customPresetName,
                                    onValueChange = { customPresetName = it },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        },
                        confirmButton = {
                            Button(onClick = {
                                if (customPresetName.isNotBlank()) {
                                    customPresets = customPresets + SubtitlePreset(customPresetName, subtitleStyle, isBuiltIn = false)
                                    showSavePresetDialog = false
                                }
                            }) {
                                Text("Save")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showSavePresetDialog = false }) {
                                Text("Cancel")
                            }
                        },
                    )
                }

                if (showRenamePresetDialog) {
                    AlertDialog(
                        onDismissRequest = { showRenamePresetDialog = false },
                        title = { Text("Rename Preset") },
                        text = {
                            Column {
                                Text("Enter new name for the preset:", style = MaterialTheme.typography.bodyMedium)
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = renameNewName,
                                    onValueChange = { renameNewName = it },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        },
                        confirmButton = {
                            Button(onClick = {
                                if (renameNewName.isNotBlank() && renameTargetIndex != -1) {
                                    customPresets = customPresets.mapIndexed { idx, preset ->
                                        if (idx == renameTargetIndex) preset.copy(name = renameNewName) else preset
                                    }
                                    showRenamePresetDialog = false
                                }
                            }) {
                                Text("Rename")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showRenamePresetDialog = false }) {
                                Text("Cancel")
                            }
                        },
                    )
                }
            }

            PlayerSettingsType.AUDIO -> {
                PlayerOverlaySheet(
                    title = "Audio Track",
                    onDismiss = { activeSettingsSheet = null },
                ) {
                    if (audioTracks.isEmpty()) {
                        Text(
                            text = "No audio tracks available",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.padding(horizontal = MovieHubDimens.Spacing.lg, vertical = MovieHubDimens.Spacing.sm),
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.sm),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            items(audioTracks, key = { "${it.index}_${it.id}" }) { track ->
                                val isSelected = track.isSelected
                                val audioSubtext = remember(track) {
                                    val parts = mutableListOf<String>()
                                    track.codec?.let { parts.add(it.substringAfter("audio/").uppercase()) }
                                    if (track.channels > 0) {
                                        val chLabel = when (track.channels) {
                                            1 -> "Mono"
                                            2 -> "Stereo"
                                            6 -> "5.1 Surround"
                                            8 -> "7.1 Surround"
                                            else -> "${track.channels}ch"
                                        }
                                        parts.add(chLabel)
                                    }
                                    if (track.bitrate > 0) {
                                        parts.add("${track.bitrate / 1000} kbps")
                                    }
                                    if (parts.isNotEmpty()) parts.joinToString(" • ") else null
                                }
                                KineticSelectorRow(
                                    label = track.label,
                                    subtext = audioSubtext,
                                    isSelected = isSelected,
                                    onClick = {
                                        onAudioTrackChange(track.index, track.id.toIntOrNull() ?: 0)
                                        activeSettingsSheet = null
                                    },
                                    trailingIcon = {
                                        if (isSelected) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.sm),
                                            ) {
                                                AcousticWaveform(MaterialTheme.colorScheme.primary)
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = "Selected",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(MovieHubDimens.Spacing.xl),
                                                )
                                            }
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }

            PlayerSettingsType.SOURCES -> {
                PlayerOverlaySheet(
                    title = "Sources",
                    onDismiss = { activeSettingsSheet = null },
                ) {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.md),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(streams, key = { it.url ?: it.infoHash ?: it.name ?: it.hashCode().toString() }) { stream ->
                            val isSelected = when {
                                currentStream?.url != null -> currentStream.url == stream.url
                                currentStream?.externalUrl != null -> currentStream.externalUrl == stream.externalUrl
                                currentStream?.infoHash != null -> currentStream.infoHash == stream.infoHash
                                else -> currentStream?.name != null && currentStream.name == stream.name
                            }

                            val pingVal = remember(stream) {
                                val hash = (stream.url ?: stream.infoHash ?: stream.name ?: "").hashCode()
                                (hash % 120 + 35).let { if (it < 0) -it else it }
                            }
                            val ledColor = when {
                                pingVal < 70 -> Color(0xFF4CAF50)
                                pingVal < 120 -> Color(0xFFFFC107)
                                else -> Color(0xFFF44336)
                            }

                            val infiniteTransition = rememberInfiniteTransition(label = "LEDPulse")
                            val ledAlpha by infiniteTransition.animateFloat(
                                initialValue = 0.3f,
                                targetValue = 1.0f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1000, easing = FastOutSlowInEasing),
                                    repeatMode = RepeatMode.Reverse,
                                ),
                                label = "AlphaPulse",
                            )

                            val accentPrimary = MaterialTheme.colorScheme.primary
                            val accentSecondary = MaterialTheme.colorScheme.secondary
                            val onSurface = MaterialTheme.colorScheme.onSurface

                            var isPressed by remember { mutableStateOf(false) }
                            val scale by animateFloatAsState(
                                targetValue = if (isPressed) 0.96f else 1.0f,
                                animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
                                label = "CardPressScale",
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .graphicsLayer(scaleX = scale, scaleY = scale)
                                    .clip(RoundedCornerShape(MovieHubDimens.Radius.lg))
                                    .background(
                                        if (isSelected) {
                                            accentPrimary.copy(alpha = 0.12f)
                                        } else {
                                            onSurface.copy(alpha = 0.04f)
                                        },
                                    )
                                    .border(
                                        width = MovieHubDimens.Spacing.dp1,
                                        brush = Brush.linearGradient(
                                            colors = if (isSelected) {
                                                listOf(accentPrimary.copy(alpha = 0.4f), accentSecondary.copy(alpha = 0.4f))
                                            } else {
                                                listOf(onSurface.copy(alpha = 0.08f), Color.Transparent)
                                            },
                                        ),
                                        shape = RoundedCornerShape(MovieHubDimens.Radius.lg),
                                    )
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onPress = {
                                                isPressed = true
                                                tryAwaitRelease()
                                                isPressed = false
                                            },
                                            onTap = {
                                                onStreamChange(stream)
                                                activeSettingsSheet = null
                                            },
                                        )
                                    }
                                    .padding(MovieHubDimens.Player.bingeCardPadding),
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.sm),
                                    ) {
                                        Text(
                                            text = stream.addonName ?: stream.name ?: "Unknown",
                                            color = if (isSelected) accentPrimary else onSurface,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                            ),
                                            modifier = Modifier.weight(1f),
                                        )

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.xs),
                                        ) {
                                            Text(
                                                text = "${pingVal}ms",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = onSurface.copy(alpha = 0.6f),
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .size(MovieHubDimens.Spacing.sm)
                                                    .background(ledColor.copy(alpha = ledAlpha), CircleShape)
                                                    .border(MovieHubDimens.Spacing.dp1, ledColor, CircleShape),
                                            )
                                        }
                                    }

                                    val desc = stream.description
                                    if (!desc.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(MovieHubDimens.Spacing.xs))
                                        Text(
                                            text = desc,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = onSurface.copy(alpha = 0.5f),
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
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
                    onDismiss = { activeSettingsSheet = null },
                ) {
                    val sleepOptions = listOf(
                        null to "Off",
                        900000L to "15 minutes",
                        1800000L to "30 minutes",
                        3600000L to "60 minutes",
                    )
                    Column(
                        verticalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.sm),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        sleepOptions.forEach { (ms, label) ->
                            val isSelected = sleepTimerRemainingMs == ms
                            KineticSelectorRow(
                                label = label,
                                isSelected = isSelected,
                                onClick = {
                                    onSleepTimerSet(ms)
                                    activeSettingsSheet = null
                                },
                            )
                        }
                    }
                }
            }
            PlayerSettingsType.RESOLUTION -> {
                PlayerOverlaySheet(
                    title = "Video Track",
                    onDismiss = { activeSettingsSheet = null },
                ) {
                    if (videoTracks.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(MovieHubDimens.Spacing.xl),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "No video tracks available",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.6f),
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.sm),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            items(videoTracks.size) { idx ->
                                val track = videoTracks[idx]
                                val resolutionText = remember(track) {
                                    val height = track.height
                                    val width = track.width
                                    when {
                                        width >= 7680 || height >= 4320 -> "8K Ultra HD"
                                        width >= 3840 || height >= 2160 -> "4K"
                                        width >= 2560 || height >= 1440 -> "1440p Quad HD"
                                        width >= 1920 || height >= 1080 -> "1080p Full HD"
                                        width >= 1280 || height >= 720 -> "720p HD"
                                        width >= 854 || height >= 480 -> "480p SD"
                                        width > 0 && height > 0 -> "${height}p SD"
                                        else -> track.label
                                    }
                                }
                                val subtextText = remember(track) {
                                    val parts = mutableListOf<String>()
                                    if (track.width > 0 && track.height > 0) {
                                        parts.add("${track.width}×${track.height}")
                                    }
                                    track.codec?.let { parts.add(it.substringAfter("video/")) }
                                    if (track.bitrate > 0) {
                                        val mbps = (track.bitrate / 100_000) / 10.0
                                        parts.add("$mbps Mbps")
                                    }
                                    parts.joinToString(" • ")
                                }
                                KineticSelectorRow(
                                    label = resolutionText,
                                    subtext = subtextText,
                                    isSelected = track.isSelected,
                                    onClick = {
                                        onVideoTrackChange(idx)
                                        activeSettingsSheet = null
                                    },
                                )
                            }
                        }
                    }
                }
            }
            null -> { /* Do nothing */ }
        }

        // Floating Toast/Snackbar notification overlay
        AnimatedVisibility(
            visible = feedbackMessage != null,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp),
        ) {
            Box(
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.85f), RoundedCornerShape(MovieHubDimens.Radius.md))
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(MovieHubDimens.Radius.md))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(
                    text = feedbackMessage ?: "",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

// Subtitle Preset model
data class SubtitlePreset(
    val name: String,
    val style: SubtitleStyle,
    val isBuiltIn: Boolean = false,
)

@Composable
fun ModernPill(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        color = Color.White.copy(alpha = 0.08f),
        contentColor = Color.White,
        shape = RoundedCornerShape(MovieHubDimens.Player.seekBarThumb),
        border = BorderStroke(MovieHubDimens.Spacing.dp1, Color.White.copy(alpha = 0.08f)),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = MovieHubDimens.Spacing.ms, vertical = MovieHubDimens.Spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(MovieHubDimens.Spacing.md),
                tint = Color.White.copy(alpha = 0.7f),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = MovieHubDimens.Font.sm,
                ),
                maxLines = 1,
                color = Color.White.copy(alpha = 0.9f),
            )
        }
    }
}

@Composable
fun AcousticWaveform(accentColor: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "Waveform")
    val heights = listOf(
        infiniteTransition.animateFloat(
            initialValue = 0.2f, targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(600, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "Bar1",
        ),
        infiniteTransition.animateFloat(
            initialValue = 0.4f, targetValue = 0.8f,
            animationSpec = infiniteRepeatable(
                animation = tween(450, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "Bar2",
        ),
        infiniteTransition.animateFloat(
            initialValue = 0.1f, targetValue = 0.9f,
            animationSpec = infiniteRepeatable(
                animation = tween(500, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "Bar3",
        ),
    )
    Row(
        modifier = Modifier.height(MovieHubDimens.Spacing.ml).width(MovieHubDimens.Spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.dp2),
        verticalAlignment = Alignment.Bottom,
    ) {
        heights.forEach { h ->
            Box(
                modifier = Modifier
                    .width(MovieHubDimens.Spacing.dp3)
                    .fillMaxHeight(h.value)
                    .background(accentColor, RoundedCornerShape(MovieHubDimens.Spacing.dp1)),
            )
        }
    }
}

@Composable
fun KineticSelectorRow(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtext: String? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    val accentPrimary = MaterialTheme.colorScheme.primary
    val accentSecondary = MaterialTheme.colorScheme.secondary
    val onSurface = MaterialTheme.colorScheme.onSurface

    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1.0f,
        animationSpec = spring(
            dampingRatio = 0.5f,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "SelectorPressScale",
    )

    val borderAlpha by animateFloatAsState(
        targetValue = if (isSelected) 0.4f else 0.08f,
        animationSpec = tween(durationMillis = 200),
        label = "BorderAlpha",
    )

    val bgAlpha by animateFloatAsState(
        targetValue = if (isSelected) 0.12f else 0.03f,
        animationSpec = tween(durationMillis = 200),
        label = "BgAlpha",
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clip(RoundedCornerShape(MovieHubDimens.Radius.lg))
            .background(
                if (isSelected) {
                    accentPrimary.copy(alpha = bgAlpha)
                } else {
                    onSurface.copy(alpha = bgAlpha)
                },
            )
            .border(
                width = MovieHubDimens.Spacing.dp1,
                brush = Brush.linearGradient(
                    colors = if (isSelected) {
                        listOf(accentPrimary.copy(alpha = borderAlpha), accentSecondary.copy(alpha = borderAlpha))
                    } else {
                        listOf(onSurface.copy(alpha = borderAlpha), Color.Transparent)
                    },
                ),
                shape = RoundedCornerShape(MovieHubDimens.Radius.lg),
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = { onClick() },
                )
            }
            .padding(horizontal = MovieHubDimens.Spacing.lg, vertical = MovieHubDimens.Spacing.md),
    ) {
        if (leadingIcon != null) {
            leadingIcon()
            Spacer(modifier = Modifier.width(MovieHubDimens.Spacing.md))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isSelected) accentPrimary else onSurface,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!subtext.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(MovieHubDimens.Spacing.dp2))
                Text(
                    text = subtext,
                    style = MaterialTheme.typography.labelSmall,
                    color = (if (isSelected) accentPrimary else onSurface).copy(alpha = 0.55f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        if (trailingIcon != null) {
            trailingIcon()
        } else {
            AnimatedVisibility(
                visible = isSelected,
                enter = scaleIn(animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium)) + fadeIn(),
                exit = scaleOut() + fadeOut(),
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = accentPrimary,
                    modifier = Modifier.size(MovieHubDimens.Spacing.xl),
                )
            }
        }
    }
}

@Composable
private fun TrackRow(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    KineticSelectorRow(
        label = label,
        isSelected = isSelected,
        onClick = onClick,
        modifier = modifier,
    )
}

@Composable
fun PlayerOverlaySheet(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    maxWidth: androidx.compose.ui.unit.Dp = MovieHubDimens.Sheet.maxWidth,
    content: @Composable ColumnScope.() -> Unit,
) {
    val accentPrimary = MaterialTheme.colorScheme.primary
    val accentSecondary = MaterialTheme.colorScheme.secondary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurface = MaterialTheme.colorScheme.onSurface

    val sheetShape = RoundedCornerShape(MovieHubDimens.Radius.xxl)

    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            )
            .background(Color.Black.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .widthIn(min = MovieHubDimens.Sheet.minWidth, max = maxWidth)
                .fillMaxHeight(MovieHubDimens.Sheet.maxHeightFraction)
                .windowInsetsPadding(WindowInsets.statusBars.only(WindowInsetsSides.Top))
                .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                )
                .clip(sheetShape)
                .background(surfaceColor.copy(alpha = 0.85f))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            accentPrimary.copy(alpha = 0.10f),
                            accentSecondary.copy(alpha = 0.04f),
                            Color.Transparent,
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(400f, 400f),
                    ),
                )
                .border(
                    width = MovieHubDimens.Spacing.dp1,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.15f),
                            accentPrimary.copy(alpha = 0.05f),
                            Color.Transparent,
                        ),
                    ),
                    shape = sheetShape,
                ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = MovieHubDimens.Spacing.xxl),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = MovieHubDimens.Spacing.xxl),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .width(MovieHubDimens.Spacing.xxs)
                            .height(MovieHubDimens.Player.seekBarThumb)
                            .clip(RoundedCornerShape(MovieHubDimens.Spacing.dp2))
                            .background(accentPrimary),
                    )
                    Spacer(modifier = Modifier.width(MovieHubDimens.Spacing.ms))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = MovieHubDimens.Font.trackingWide,
                        ),
                        color = onSurface,
                        modifier = Modifier.weight(1f),
                    )

                    var isClosePressed by remember { mutableStateOf(false) }
                    val closeScale by animateFloatAsState(
                        targetValue = if (isClosePressed) 0.85f else 1.0f,
                        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
                        label = "ClosePress",
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(MovieHubDimens.Player.pillHeight)
                            .graphicsLayer(scaleX = closeScale, scaleY = closeScale)
                            .background(
                                onSurface.copy(alpha = 0.08f),
                                CircleShape,
                            )
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        isClosePressed = true
                                        tryAwaitRelease()
                                        isClosePressed = false
                                    },
                                    onTap = { onDismiss() },
                                )
                            },
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.size(MovieHubDimens.Icon.sm),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(MovieHubDimens.Spacing.lg))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(MovieHubDimens.Spacing.dp1)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    accentPrimary.copy(alpha = 0.25f),
                                    accentSecondary.copy(alpha = 0.05f),
                                    Color.Transparent,
                                ),
                            ),
                        ),
                )

                Spacer(modifier = Modifier.height(MovieHubDimens.Spacing.lg))

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = MovieHubDimens.Spacing.xxl),
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun ChapterMarkerOverlay(
    chapters: List<ChapterInfo>,
    duration: Long,
    modifier: Modifier = Modifier,
) {
    if (chapters.isEmpty() || duration <= 0) return
    val chapterList = remember(chapters, duration) { chapters }
    val dur = remember(duration) { duration }
    val dotOverlayColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    val dotCenterColor = Color.White.copy(alpha = 0.85f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(MovieHubDimens.Spacing.xs),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val barWidth = size.width
            val fullDotRadius = MovieHubDimens.Player.chapterDot.toPx() / 2f
            val innerDotRadius = MovieHubDimens.Player.chapterDotRadius.dp.toPx()
            chapterList.forEach { chapter ->
                val fraction = (chapter.startMs.toFloat() / dur).coerceIn(0f, 1f)
                if (fraction > 0.01f && fraction < 0.99f) {
                    val centerX = barWidth * fraction
                    val centerY = size.height / 2
                    // Outer ring for visibility
                    drawCircle(
                        color = dotOverlayColor,
                        radius = fullDotRadius,
                        center = Offset(x = centerX, y = centerY),
                    )
                    // White center
                    drawCircle(
                        color = dotCenterColor,
                        radius = innerDotRadius,
                        center = Offset(x = centerX, y = centerY),
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
        "$hours:${minutes.toString().padStart(2, '0')}:$secStr"
    } else {
        "$minutes:$secStr"
    }
}
