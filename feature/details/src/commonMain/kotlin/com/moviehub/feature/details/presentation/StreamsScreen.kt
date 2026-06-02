package com.moviehub.feature.details.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.FlowRow
import com.moviehub.core.model.StreamFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TheaterComedy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.moviehub.core.model.DownloadItem
import com.moviehub.core.model.DownloadState
import com.moviehub.core.model.StreamItem
import com.moviehub.core.network.DownloadsRepository
import com.moviehub.core.ui.components.EmptyState
import com.moviehub.core.ui.components.GlassyBox
import com.moviehub.core.ui.components.TechnicalBadge
import com.moviehub.core.ui.components.shimmerEffect
import com.moviehub.core.ui.theme.MovieHubColors
import com.moviehub.core.ui.theme.MovieHubDimens
import com.moviehub.feature.details.data.AddonStreamStatus
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import com.moviehub.core.ui.components.SmartStatusBar
import androidx.compose.ui.graphics.luminance

@Composable
fun StreamDiscoveryIndicator(processed: Int, total: Int) {
    GlassyBox(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = MovieHubDimens.Spacing.xxs),
        blurRadius = MovieHubDimens.Spacing.sm,
    ) {
        Column(modifier = Modifier.padding(MovieHubDimens.Spacing.lg)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(MovieHubDimens.Spacing.xl),
                )
                Spacer(modifier = Modifier.width(MovieHubDimens.Spacing.md))
                Text(
                    text = "Searching Addons",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(modifier = Modifier.height(MovieHubDimens.Spacing.md))
            LinearProgressIndicator(
                progress = { if (total > 0) processed.toFloat() / total else 0f },
                modifier = Modifier.fillMaxWidth().height(MovieHubDimens.Spacing.xxs),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
            )
            Spacer(modifier = Modifier.height(MovieHubDimens.Spacing.sm))
            Text(
                text = "Searching $processed of $total addons...",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            )
        }
    }
}

@Composable
fun AddonStatusPills(statuses: Map<String, AddonStreamStatus>) {
    val totalCount = statuses.size
    val completedCount = statuses.count {
        it.value is AddonStreamStatus.Completed ||
            it.value is AddonStreamStatus.TimedOut ||
            it.value is AddonStreamStatus.Failed
    }

    GlassyBox(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = MovieHubDimens.Spacing.xxs),
        blurRadius = MovieHubDimens.Spacing.sm,
    ) {
        Column(modifier = Modifier.padding(MovieHubDimens.Spacing.md)) {
            // Header row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(MovieHubDimens.Spacing.xl),
                )
                Spacer(modifier = Modifier.width(MovieHubDimens.Spacing.md))
                Text(
                    text = "Searching Addons",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "$completedCount / $totalCount",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
            }
            Spacer(modifier = Modifier.height(MovieHubDimens.Spacing.sm))

            // Progress bar
            LinearProgressIndicator(
                progress = { if (totalCount > 0) completedCount.toFloat() / totalCount else 0f },
                modifier = Modifier.fillMaxWidth().height(MovieHubDimens.Spacing.xxs),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
            )

            Spacer(modifier = Modifier.height(MovieHubDimens.Spacing.sm))

            // Per-addon status chips in a wrapping flow
            val sortedProviders = statuses.entries.sortedBy { (_, status) ->
                when (status) {
                    is AddonStreamStatus.Fetching -> 0 // Show active ones first
                    is AddonStreamStatus.Pending -> 1
                    is AddonStreamStatus.Completed -> 2
                    is AddonStreamStatus.TimedOut -> 3
                    is AddonStreamStatus.Failed -> 4
                }
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.xs),
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(sortedProviders, key = { it.key }) { (name, status) ->
                    AddonStatusChip(name = name, status = status)
                }
            }
        }
    }
}

@Composable
private fun AddonStatusChip(name: String, status: AddonStreamStatus) {
    val (bgColor, contentColor, indicator) = when (status) {
        is AddonStreamStatus.Pending -> Triple(
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            "○",
        )
        is AddonStreamStatus.Fetching -> Triple(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            MaterialTheme.colorScheme.primary,
            "◉",
        )
        is AddonStreamStatus.Completed -> Triple(
            MovieHubColors.Success.copy(alpha = 0.12f),
            MovieHubColors.Success,
            "✓",
        )
        is AddonStreamStatus.TimedOut -> Triple(
            Color(0xFFFFA726).copy(alpha = 0.12f),
            Color(0xFFFFA726),
            "⏱",
        )
        is AddonStreamStatus.Failed -> Triple(
            MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
            MaterialTheme.colorScheme.error,
            "✗",
        )
    }

    val alpha = if (status is AddonStreamStatus.Fetching) {
        val transition = rememberInfiniteTransition(label = "fetchingPulse")
        val pulseAlpha by transition.animateFloat(
            initialValue = 0.35f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseAlpha"
        )
        pulseAlpha
    } else {
        1.0f
    }

    Surface(
        shape = RoundedCornerShape(MovieHubDimens.Spacing.lg),
        color = bgColor.copy(alpha = bgColor.alpha * alpha),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = MovieHubDimens.Spacing.ms, vertical = MovieHubDimens.Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.xxs),
        ) {
            Text(
                text = indicator,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = contentColor.alpha * alpha),
            )
            Text(
                text = name,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = contentColor.alpha * alpha),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 120.dp),
            )
            if (status is AddonStreamStatus.Completed && status.streamCount > 0) {
                Text(
                    text = "+${status.streamCount}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamsScreen(
    id: String,
    type: String,
    mediaId: String? = null,
    title: String? = null,
    backdropUrl: String? = null,
    onPlayClick: (stream: StreamItem, streams: List<StreamItem>, title: String?, posterUrl: String?) -> Unit,
    onBackClick: () -> Unit,
    viewModel: DetailsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    val isSystemDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    SmartStatusBar(
        isDark = isSystemDark,
        color = MaterialTheme.colorScheme.background,
    )
    val downloadsRepository: DownloadsRepository = koinInject()
    val profileRepository: com.moviehub.core.database.ProfileRepository = koinInject()
    val scope = rememberCoroutineScope()
    var activeTorrentStream by remember { mutableStateOf<StreamItem?>(null) }
    var noPlayableSource by remember { mutableStateOf<StreamItem?>(null) }
    var selectedProvider by rememberSaveable { mutableStateOf<String?>(null) }
    // Available providers derived from streams (null = "All")
    val providers = remember(state.streams) {
        val names = state.streams.map {
            it.addonName?.takeIf { n -> n.isNotBlank() }
                ?: it.sourceName?.takeIf { s -> s.isNotBlank() }
                ?: "Other"
        }.distinct().sorted()
        listOf(null as String?) + names
    }
    // Filtered streams based on selected provider
    val displayStreams = remember(selectedProvider, state.streams) {
        if (selectedProvider == null) {
            state.streams
        } else {
            state.streams.filter {
                (it.addonName?.takeIf { n -> n.isNotBlank() }
                    ?: it.sourceName?.takeIf { s -> s.isNotBlank() }
                    ?: "Other") == selectedProvider
            }
        }
    }

    val listState = rememberLazyListState()
    var isHeaderVisible by remember { mutableStateOf(true) }
    val headerDebounce = remember { mutableStateOf(0) }

    LaunchedEffect(listState) {
        var lastIndex = 0
        var lastOffset = 0
        while (isActive) {
            val index = listState.firstVisibleItemIndex
            val offset = listState.firstVisibleItemScrollOffset
            val scrolledEnough = index > 0 || offset > 64
            val scrollingDown = index > lastIndex || (index == lastIndex && offset > lastOffset)
            val scrollingUp = index < lastIndex || (index == lastIndex && offset < lastOffset)

            if (scrollingDown && scrolledEnough) {
                headerDebounce.value = (headerDebounce.value + 1).coerceAtMost(5)
                if (headerDebounce.value >= 3 && isHeaderVisible) isHeaderVisible = false
            } else if (scrollingUp) {
                headerDebounce.value = (headerDebounce.value - 1).coerceAtLeast(-3)
                if (headerDebounce.value <= -2 && !isHeaderVisible) isHeaderVisible = true
            } else {
                headerDebounce.value = 0
            }

            lastIndex = index
            lastOffset = offset
            delay(100.milliseconds)
        }
    }

    // Parse season/episode from stream ID (e.g. "tt12345:1:3" → S1 E3, "tmdb:12345:1:3" → S1 E3)
    val streamSeasonEpisode = remember(id) {
        val parts = id.split(":")
        if (parts.size >= 3) {
            val s = parts[parts.size - 2].toIntOrNull()
            val e = parts[parts.size - 1].toIntOrNull()
            if (s != null && e != null) Pair(s, e) else null
        } else {
            null
        }
    }

    // Only load on first composition or when ID changes — not on resume/back-navigation
    var lastLoadedId by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(id, type) {
        if (lastLoadedId != id) {
            lastLoadedId = id
            viewModel.onAction(DetailsAction.LoadDetails(mediaId ?: id, type, null))
            viewModel.onAction(DetailsAction.LoadStreams(id, type))
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { paddingValues ->
        // Try backgroundUrl, fallback to posterUrl, then pass-through backdropUrl
        val activeBackdropUrl = state.mediaItem?.backgroundUrl
            ?: state.mediaItem?.posterUrl
            ?: backdropUrl
        val hasBackdrop = activeBackdropUrl != null
 
        Box(modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
        ) {
            // ===== FULLSCREEN BACKDROP (keyed on URL to avoid unnecessary recomposition) =====
            key(activeBackdropUrl) {
                if (activeBackdropUrl != null) {
                    StreamsBackdrop(backdropUrl = activeBackdropUrl)
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0xFF0F0F12),
                                        Color(0xFF070709)
                                    )
                                )
                            )
                            .shimmerEffect()
                    )
                }
            }
 
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()),
            ) {
                // ===== HEADER (hides on scroll-down, shows on scroll-up) =====
                AnimatedVisibility(
                    visible = isHeaderVisible,
                    modifier = Modifier.fillMaxWidth(),
                    enter = slideInVertically(initialOffsetY = { h -> -h }) + fadeIn(animationSpec = tween(200)),
                    exit = slideOutVertically(targetOffsetY = { h -> -h }) + fadeOut(animationSpec = tween(200)),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = MovieHubDimens.Spacing.sm, vertical = MovieHubDimens.Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier.background(
                                Color.Black.copy(alpha = 0.4f),
                                CircleShape,
                            ),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White,
                            )
                        }
                        Spacer(modifier = Modifier.width(MovieHubDimens.Spacing.md))
                        Column {
                            Text(
                                text = "Choose Stream",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                            )
                            // Season/Episode badge when browsing a specific episode
                            streamSeasonEpisode?.let { (season, episode) ->
                                val label = if (season <= 0) {
                                    "Specials · E$episode"
                                } else {
                                    "S$season E$episode"
                                }
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .background(
                                            Color.Black.copy(alpha = 0.4f),
                                            RoundedCornerShape(MovieHubDimens.Spacing.xxs),
                                        )
                                        .padding(horizontal = MovieHubDimens.Spacing.sm, vertical = MovieHubDimens.Spacing.dp2),
                                )
                            }
                            val displayTitle = state.mediaItem?.title ?: title
                            displayTitle?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.65f),
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }

                // ===== STREAM CONTENT =====
                // Show shimmer when: searching, or haven't started yet (no addon count)
                val isInitiallyLoading = state.streams.isEmpty() && (state.isSearchingStreams || state.totalStreamAddons == 0)
                if (isInitiallyLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(MovieHubDimens.Spacing.lg),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        GlassyBox(
                            modifier = Modifier.fillMaxWidth(),
                            blurRadius = MovieHubDimens.Spacing.md,
                            baseAlpha = 0.55f,
                        ) {
                            Column(
                                modifier = Modifier.padding(MovieHubDimens.Spacing.lg),
                                verticalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.md)
                            ) {
                                Text(
                                    text = "Searching for streams...",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                )
                                repeat(5) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(MovieHubDimens.Player.shimmerHeight)
                                            .clip(RoundedCornerShape(MovieHubDimens.Spacing.md))
                                            .shimmerEffect(),
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Pinned provider filter bar (stays on screen while scrolling)
                    if (displayStreams.isNotEmpty() || state.streams.isNotEmpty()) {
                        ProviderFilterBar(
                            providers = providers,
                            selectedProvider = selectedProvider,
                            onProviderSelected = { selectedProvider = it },
                            modifier = Modifier.padding(horizontal = MovieHubDimens.Spacing.md, vertical = MovieHubDimens.Spacing.xxs),
                        )
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = MovieHubDimens.Spacing.md,
                            end = MovieHubDimens.Spacing.md,
                            top = MovieHubDimens.Spacing.sm,
                            bottom = paddingValues.calculateBottomPadding() + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + MovieHubDimens.Spacing.lg,
                        ),
                        verticalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.ms),
                    ) {
                        val hasActiveProviders = state.addonStreamStatuses.values.any {
                            it is AddonStreamStatus.Pending || it is AddonStreamStatus.Fetching
                        }
                        if (hasActiveProviders && state.addonStreamStatuses.isNotEmpty()) {
                            item {
                                AddonStatusPills(statuses = state.addonStreamStatuses)
                            }
                        }

                        // Provider filter bar — horizontal scrollable chips (removed from LazyColumn, pinned above)

                        items(displayStreams, key = { "${it.addonName ?: ""}_${it.url ?: it.infoHash ?: it.name ?: it.hashCode()}" }) { stream ->
                            StreamItemCard(
                                stream = stream,
                                onClick = {
                                    if (stream.isTorrentStream) {
                                        activeTorrentStream = stream
                                    } else if (stream.hasPlayableSource) {
                                        onPlayClick(stream, state.streams, state.mediaItem?.title, state.mediaItem?.posterUrl)
                                    } else {
                                        noPlayableSource = stream
                                    }
                                },
                                onDownloadClick = if (stream.url != null || stream.externalUrl != null) { {
                                    val url = stream.url ?: stream.externalUrl ?: return@StreamItemCard
                                    val profileId = profileRepository.activeProfile.value?.id ?: return@StreamItemCard
                                    scope.launch {
                                        downloadsRepository.startDownload(
                                            DownloadItem(
                                                id = "${mediaId ?: id}_${url.hashCode().let { if (it < 0) -it else it }}",
                                                profileId = profileId,
                                                mediaId = mediaId ?: id,
                                                title = state.mediaItem?.title ?: stream.name ?: "Unknown",
                                                posterUrl = state.mediaItem?.posterUrl,
                                                url = url,
                                                headers = stream.behaviorHints.proxyHeaders?.request ?: emptyMap(),
                                                state = DownloadState.QUEUED,
                                            ),
                                        )
                                    }
                                } } else { null },
                            )
                        }

                        // Only show "No streams found" after search actually completed
                        // (totalStreamAddons > 0 means we attempted the search)
                        if (state.streams.isEmpty() && !state.isSearchingStreams && state.totalStreamAddons > 0) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = MovieHubDimens.EmptyState.iconSize),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    GlassyBox(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = MovieHubDimens.Spacing.md),
                                        blurRadius = MovieHubDimens.Spacing.md,
                                        baseAlpha = 0.85f,
                                    ) {
                                        Box(modifier = Modifier.padding(vertical = MovieHubDimens.Spacing.xl)) {
                                            EmptyState(
                                                icon = Icons.Default.TheaterComedy,
                                                title = "No streams found",
                                                subtitle = "Try adding more addons or check back later",
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            activeTorrentStream?.let { torrentStream ->
                TorrentStreamDialog(
                    stream = torrentStream,
                    onDismiss = { activeTorrentStream = null },
                )
            }

            noPlayableSource?.let { unplayable ->
                AlertDialog(
                    onDismissRequest = { noPlayableSource = null },
                    containerColor = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(MovieHubDimens.Spacing.xxl),
                    title = {
                        Text(
                            text = "No Playable Source",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    },
                    text = {
                        Text(
                            text = "This stream has no direct playable URL. Try another source or copy the link for external playback.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = { noPlayableSource = null },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                            shape = RoundedCornerShape(MovieHubDimens.Spacing.md),
                        ) {
                            Text("OK", fontWeight = FontWeight.Bold)
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun StreamsBackdrop(backdropUrl: String) {
    Box(modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
    ) {
        KamelImage(
            resource = { asyncPainterResource(data = backdropUrl) },
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            onLoading = { Box(Modifier.fillMaxSize().shimmerEffect()) },
            onFailure = { Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f))) },
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.7f),
                            Color.Black.copy(alpha = 0.25f),
                            Color.Black.copy(alpha = 0.3f),
                            Color.Black.copy(alpha = 0.7f),
                        ),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.35f),
                            Color.Black.copy(alpha = 0.7f),
                        ),
                    ),
                ),
        )
    }
}

@Composable
fun StreamItemCard(
    stream: StreamItem,
    onClick: () -> Unit,
    onDownloadClick: (() -> Unit)? = null,
) {
    val flagEmoji = remember(stream) { getFlagEmoji(stream.name ?: stream.description) }
    val addonName = remember(stream.addonName) { stream.addonName }
    val streamDescription = remember(stream.description) { stream.description }
    val meta = remember(stream) { parseEnhancedMetadata(stream) }

    GlassyBox(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        baseAlpha = 0.55f,
        blurRadius = MovieHubDimens.Spacing.sm,
    ) {
        Row(
            modifier = Modifier.padding(MovieHubDimens.Spacing.ml),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // ===== FLAG / SOURCE ICON =====
            Box(
                modifier = Modifier
                    .size(MovieHubDimens.Player.sideSliderWidth)
                    .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(MovieHubDimens.Spacing.md)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = flagEmoji,
                    fontSize = MovieHubDimens.Font.headline,
                )
            }

            Spacer(modifier = Modifier.width(MovieHubDimens.Spacing.ml))

            // ===== DETAILS =====
            Column(modifier = Modifier.weight(1f)) {
                // Source name & file size
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.sm),
                ) {
                    Text(
                        text = stream.name ?: "Unknown Source",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    
                    // File size badge (prioritizes parsed text size, fallbacks to metadata)
                    meta.sizeText?.let { sizeStr ->
                        Text(
                            text = sizeStr,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = MovieHubDimens.Font.xs,
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    RoundedCornerShape(MovieHubDimens.Spacing.xxs),
                                )
                                .padding(horizontal = MovieHubDimens.Spacing.xs, vertical = MovieHubDimens.Spacing.dp2),
                        )
                    }
                }

                // Addon name / Provider label
                val providerLabel = addonName?.takeIf { it.isNotBlank() } ?: stream.sourceName?.takeIf { it.isNotBlank() }
                providerLabel?.let {
                    Spacer(modifier = Modifier.height(MovieHubDimens.Spacing.dp2))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                val combinedLc = remember(stream) {
                    val n = stream.name ?: ""
                    val d = stream.description ?: ""
                    val f = stream.behaviorHints.filename ?: ""
                    "$n $d $f".lowercase()
                }

                // Technical specs & rich badges flow grid
                Spacer(modifier = Modifier.height(MovieHubDimens.Spacing.xs))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.xs),
                    verticalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.xs),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Source Type Badge
                    StreamMetadataBadge(
                        text = meta.sourceType,
                        backgroundColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                        contentColor = MaterialTheme.colorScheme.secondary
                    )

                    // Stream Format badge (HLS, DASH, MP4, WebM)
                    val format = stream.streamFormat
                    if (format != com.moviehub.core.model.StreamFormat.UNKNOWN) {
                        val formatColor = when (format) {
                            com.moviehub.core.model.StreamFormat.HLS -> Color(0xFF00E676)
                            com.moviehub.core.model.StreamFormat.DASH -> Color(0xFF29B6F6)
                            else -> Color(0xFFFFB74D)
                        }
                        StreamMetadataBadge(
                            text = format.name,
                            backgroundColor = formatColor.copy(alpha = 0.15f),
                            contentColor = formatColor
                        )
                    }

                    // Adaptive Streaming & Auto Quality Badges
                    if (format == com.moviehub.core.model.StreamFormat.HLS || format == com.moviehub.core.model.StreamFormat.DASH) {
                        StreamMetadataBadge(
                            text = "Adaptive Streaming",
                            backgroundColor = Color(0xFF00E676).copy(alpha = 0.15f),
                            contentColor = Color(0xFF00E676)
                        )
                        StreamMetadataBadge(
                            text = "Auto Quality",
                            backgroundColor = Color(0xFF00E676).copy(alpha = 0.15f),
                            contentColor = Color(0xFF00E676)
                        )
                    }

                    // Resolution Badge
                    meta.quality?.let { qual ->
                        StreamMetadataBadge(
                            text = qual,
                            backgroundColor = Color(0xFFFF5252).copy(alpha = 0.15f),
                            contentColor = Color(0xFFFF5252)
                        )
                    }

                    // HDR/Dolby Vision Badges
                    meta.hdr.forEach { hdrType ->
                        val hdrBgColor = if (hdrType == "Dolby Vision") Color(0xFFFF4081).copy(alpha = 0.15f) else Color(0xFFE040FB).copy(alpha = 0.15f)
                        val hdrContentColor = if (hdrType == "Dolby Vision") Color(0xFFFF4081) else Color(0xFFE040FB)
                        StreamMetadataBadge(
                            text = hdrType,
                            backgroundColor = hdrBgColor,
                            contentColor = hdrContentColor
                        )
                    }

                    // Audio Badges
                    meta.audio.forEach { aud ->
                        val audText = if (aud == "Atmos") "Dolby Atmos" else aud
                        val audBgColor = if (aud == "Atmos") Color(0xFF7C4DFF).copy(alpha = 0.15f) else Color(0xFF00E5FF).copy(alpha = 0.15f)
                        val audContentColor = if (aud == "Atmos") Color(0xFF7C4DFF) else Color(0xFF00E5FF)
                        StreamMetadataBadge(
                            text = audText,
                            backgroundColor = audBgColor,
                            contentColor = audContentColor
                        )
                    }

                    // Multi Audio Badge
                    val hasMultiAudio = meta.languages.contains("Multi") || combinedLc.contains("multi-audio") || combinedLc.contains("dual-audio") || combinedLc.contains("multi audio") || combinedLc.contains("dual audio") || combinedLc.contains("dubbed")
                    if (hasMultiAudio) {
                        StreamMetadataBadge(
                            text = "Multi Audio",
                            backgroundColor = Color(0xFF00E5FF).copy(alpha = 0.15f),
                            contentColor = Color(0xFF00E5FF)
                        )
                    }

                    // Subtitles Available Badge
                    val hasSubtitles = combinedLc.contains("subtitles") || combinedLc.contains("subbed") || combinedLc.contains("subs") || combinedLc.contains("multi-sub") || combinedLc.contains("multisubs")
                    if (hasSubtitles) {
                        StreamMetadataBadge(
                            text = "Subtitles Available",
                            backgroundColor = Color(0xFFFFD700).copy(alpha = 0.15f),
                            contentColor = Color(0xFFFFD700)
                        )
                    }

                    // Codec Badges
                    meta.codecs.forEach { cod ->
                        StreamMetadataBadge(
                            text = cod,
                            backgroundColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                            contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }

                    // Language Badges
                    meta.languages.forEach { lang ->
                        StreamMetadataBadge(
                            text = lang,
                            backgroundColor = Color(0xFFFFD700).copy(alpha = 0.12f),
                            contentColor = Color(0xFFFFD700)
                        )
                    }

                    // Seeders Badge
                    meta.seeders?.let { seeds ->
                        val seederColor = when (meta.reliability) {
                            StreamReliability.HIGH -> Color(0xFF00E676)
                            StreamReliability.MEDIUM -> Color(0xFFFFB300)
                            else -> Color(0xFFFF1744)
                        }
                        StreamMetadataBadge(
                            text = "👤 $seeds Seeds",
                            backgroundColor = seederColor.copy(alpha = 0.15f),
                            contentColor = seederColor
                        )
                    }

                    // Reliability Badge
                    val reliabilityText = when (meta.reliability) {
                        StreamReliability.HIGH -> "High Reliability"
                        StreamReliability.MEDIUM -> "Medium Reliability"
                        StreamReliability.LOW -> "Low Seeders"
                        StreamReliability.UNKNOWN -> null
                    }
                    val reliabilityColor = when (meta.reliability) {
                        StreamReliability.HIGH -> Color(0xFF00E676)
                        StreamReliability.MEDIUM -> Color(0xFFFFB300)
                        StreamReliability.LOW -> Color(0xFFFF1744)
                        StreamReliability.UNKNOWN -> Color.Gray
                    }
                    if (reliabilityText != null) {
                        StreamMetadataBadge(
                            text = reliabilityText,
                            backgroundColor = reliabilityColor.copy(alpha = 0.1f),
                            contentColor = reliabilityColor
                        )
                    }
                }

                // Raw description / filename for complete transparency
                if (!streamDescription.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(MovieHubDimens.Spacing.xs))
                    Text(
                        text = streamDescription,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = MovieHubDimens.Font.xs
                        ),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(modifier = Modifier.width(MovieHubDimens.Spacing.sm))

            // ===== PLAY BUTTON =====
            Box(
                modifier = Modifier
                    .size(MovieHubDimens.Player.pillHeight)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(MovieHubDimens.Spacing.xl),
                )
            }

            // ===== DOWNLOAD BUTTON =====
            if (onDownloadClick != null) {
                Spacer(modifier = Modifier.width(MovieHubDimens.Spacing.xs))
                Box(
                    modifier = Modifier
                        .size(MovieHubDimens.Player.pillHeight)
                        .clickable(onClick = onDownloadClick)
                        .background(
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                            CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Download",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.size(MovieHubDimens.Player.seekBarThumb),
                    )
                }
            }
        }
    }
}

fun getFlagEmoji(text: String?): String {
    if (text == null) return "🌐"
    val t = text.uppercase()
    return when {
        t.contains("INDIA") || t.contains(" IN ") || t.contains("[IN]") -> "🇮🇳"
        t.contains("USA") || t.contains(" US ") || t.contains("[US]") || t.contains("UNITED STATES") -> "🇺🇸"
        t.contains("UK") || t.contains("UNITED KINGDOM") -> "🇬🇧"
        t.contains("FR") || t.contains("FRANCE") -> "🇫🇷"
        t.contains("ES") || t.contains("SPAIN") -> "🇪🇸"
        t.contains("DE") || t.contains("GERMANY") -> "🇩🇪"
        t.contains("IT") || t.contains("ITALY") -> "🇮🇹"
        t.contains("BR") || t.contains("BRAZIL") -> "🇧🇷"
        t.contains("JP") || t.contains("JAPAN") -> "🇯🇵"
        t.contains("KR") || t.contains("SOUTH KOREA") -> "🇰🇷"
        else -> "🌐"
    }
}

private fun extractTechnicalSpecs(title: String?): List<String> {
    if (title == null) return emptyList()
    val specs = mutableListOf<String>()
    val lowerTitle = title.lowercase()

    if (lowerTitle.contains("4k") || lowerTitle.contains("2160p") || lowerTitle.contains("uhd")) specs.add("4K")
    if (lowerTitle.contains("1080p") || lowerTitle.contains("fhd")) specs.add("1080P")
    if (lowerTitle.contains("720p") || lowerTitle.contains("hd")) {
        if (!specs.contains("4K") && !specs.contains("1080P")) specs.add("720P")
    }

    if (lowerTitle.contains("hdr")) specs.add("HDR")
    if (lowerTitle.contains("dv") || lowerTitle.contains("dolby vision")) specs.add("DV")

    if (lowerTitle.contains("dts")) specs.add("DTS")
    if (lowerTitle.contains("atmos")) specs.add("ATMOS")
    if (lowerTitle.contains("5.1")) specs.add("5.1")
    if (lowerTitle.contains("7.1")) specs.add("7.1")

    if (lowerTitle.contains("hevc") || lowerTitle.contains("x265")) specs.add("HEVC")

    return specs
}

@Composable
fun TorrentStreamDialog(
    stream: StreamItem,
    onDismiss: () -> Unit,
) {
    val clipboardManager = LocalClipboardManager.current
    val uriHandler = LocalUriHandler.current
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    val magnetUrl = remember(stream) {
        when {
            stream.url?.trimStart()?.startsWith("magnet:", ignoreCase = true) == true -> stream.url
            stream.externalUrl?.trimStart()
                ?.startsWith("magnet:", ignoreCase = true) == true -> stream.externalUrl

            !stream.infoHash.isNullOrBlank() -> {
                var link = "magnet:?xt=urn:btih:${stream.infoHash}"
                val nameOrFilename = stream.behaviorHints.filename ?: stream.name
                if (!nameOrFilename.isNullOrBlank()) {
                    val encodedName = nameOrFilename
                        .replace(" ", "%20")
                        .replace("[", "%5B")
                        .replace("]", "%5D")
                    link += "&dn=$encodedName"
                }
                val trackers = listOf(
                    "udp://tracker.coppersurfer.tk:6969/announce",
                    "udp://tracker.openbittorrent.com:6969/announce",
                    "udp://tracker.opentrackr.org:1337/announce",
                    "udp://tracker.leechers-paradise.org:6969/announce",
                    "udp://tracker.internetwarriors.net:1337/announce",
                    "udp://open.demonii.com:1337/announce",
                    "udp://tracker.torrent.eu.org:451/announce",
                    "udp://tracker.cyberia.is:6969/announce",
                    "wss://tracker.openwebtorrent.com",
                    "wss://tracker.btorrent.xyz",
                    "wss://tracker.fastcast.nz",
                )
                trackers.forEach { tracker ->
                    val encodedTracker = tracker
                        .replace(":", "%3A")
                        .replace("/", "%2F")
                    link += "&tr=$encodedTracker"
                }
                link
            }

            else -> null
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(MovieHubDimens.Spacing.xxl),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.md),
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(MovieHubDimens.Icon.md),
                    )
                }
                Text(
                    text = "P2P Torrent Stream",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.lg)) {
                Text(
                    text = "Standard media players cannot play peer-to-peer torrent or magnet links directly.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f), RoundedCornerShape(MovieHubDimens.Spacing.lg))
                        .padding(MovieHubDimens.Spacing.lg),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.sm)) {
                        val fileName =
                            stream.behaviorHints.filename ?: stream.name ?: "Unknown File"
                        Text(
                            text = fileName,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 3,
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.sm),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            val sizeStr = formatBytes(stream.behaviorHints.videoSize)
                            TechnicalBadge(sizeStr)

                            stream.addonName?.let {
                                TechnicalBadge(it)
                            }
                        }

                        stream.infoHash?.let { hash ->
                            val displayHash = if (hash.length > 12) {
                                "${hash.take(6)}...${hash.takeLast(6)}"
                            } else {
                                hash
                            }
                            Text(
                                text = "Hash: $displayHash",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                            )
                        }
                    }
                }

                Text(
                    text = "Open this stream in an external player like VLC, Stremio, or TorrServe, or copy the magnet link for a Debrid / Torrent manager.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                )

                AnimatedVisibility(visible = !errorMessage.isNullOrBlank(), enter = fadeIn(), exit = fadeOut()) {
                    Text(
                        text = errorMessage!!,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = MovieHubDimens.Spacing.xxs),
                    )
                }

                AnimatedVisibility(visible = !successMessage.isNullOrBlank(), enter = fadeIn(), exit = fadeOut()) {
                    Text(
                        text = successMessage!!,
                        style = MaterialTheme.typography.labelSmall,
                        color = MovieHubColors.Success,
                        modifier = Modifier.padding(top = MovieHubDimens.Spacing.xxs),
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (magnetUrl != null) {
                        try {
                            errorMessage = null
                            uriHandler.openUri(magnetUrl)
                        } catch (e: Exception) {
                            errorMessage =
                                "No compatible external player found. Try VLC or copy the magnet link."
                        }
                    } else {
                        errorMessage = "Failed to construct magnet URL."
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                shape = RoundedCornerShape(MovieHubDimens.Spacing.md),
            ) {
                Text("Play Externally", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.sm)) {
                OutlinedButton(
                    onClick = {
                        if (magnetUrl != null) {
                            clipboardManager.setText(AnnotatedString(magnetUrl))
                            successMessage = "Magnet link copied!"
                            errorMessage = null
                        } else {
                            errorMessage = "Failed to construct magnet URL."
                        }
                    },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                    ),
                    shape = RoundedCornerShape(MovieHubDimens.Spacing.md),
                    border = androidx.compose.foundation.BorderStroke(
                        MovieHubDimens.Spacing.dp1,
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                    ),
                ) {
                    Text("Copy Magnet")
                }

                TextButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                    ),
                ) {
                    Text("Cancel")
                }
            }
        },
    )
}

fun formatBytes(bytes: Long?): String {
    if (bytes == null || bytes <= 0) return "Unknown Size"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> "${(gb * 10.0).toInt() / 10.0} GB"
        mb >= 1.0 -> "${(mb * 10.0).toInt() / 10.0} MB"
        else -> "$bytes B"
    }
}

@Composable
private fun ProviderFilterBar(
    providers: List<String?>,
    selectedProvider: String?,
    onProviderSelected: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(MovieHubDimens.Spacing.ml),
        color = Color.Black.copy(alpha = 0.35f),
    ) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.sm),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MovieHubDimens.Spacing.sm, vertical = MovieHubDimens.Spacing.xs),
        ) {
            items(providers, key = { it ?: "all" }) { provider ->
                val isSelected = provider == selectedProvider
                val label = provider ?: "All"
                Surface(
                    onClick = { onProviderSelected(provider) },
                    shape = RoundedCornerShape(MovieHubDimens.Spacing.xl),
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        Color.White.copy(alpha = 0.18f)
                    },
                    contentColor = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        Color.White.copy(alpha = 0.85f)
                    },
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = MovieHubDimens.Spacing.lg, vertical = MovieHubDimens.Spacing.sm),
                    )
                }
            }
        }
    }
}

@Composable
fun StreamMetadataBadge(
    text: String,
    backgroundColor: Color,
    contentColor: Color
) {
    Surface(
        shape = RoundedCornerShape(MovieHubDimens.Spacing.xs),
        color = backgroundColor,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = MovieHubDimens.Font.xs
            ),
            color = contentColor,
            modifier = Modifier.padding(horizontal = MovieHubDimens.Spacing.xs, vertical = MovieHubDimens.Spacing.dp2)
        )
    }
}

data class EnhancedStreamMetadata(
    val quality: String? = null,
    val resolution: String? = null,
    val sizeText: String? = null,
    val seeders: Int? = null,
    val codecs: List<String> = emptyList(),
    val audio: List<String> = emptyList(),
    val hdr: List<String> = emptyList(),
    val languages: List<String> = emptyList(),
    val sourceType: String = "Stream",
    val reliability: StreamReliability = StreamReliability.UNKNOWN
)

enum class StreamReliability {
    HIGH, MEDIUM, LOW, UNKNOWN
}

fun parseEnhancedMetadata(stream: StreamItem): EnhancedStreamMetadata {
    val nameText = stream.name ?: ""
    val descText = stream.description ?: ""
    val combinedText = "$nameText $descText".lowercase()

    // 1. Resolution / Quality
    val resolution = when {
        combinedText.contains("2160p") || combinedText.contains("4k") || combinedText.contains("uhd") -> "4K UHD"
        combinedText.contains("1080p") || combinedText.contains("fhd") -> "1080p FHD"
        combinedText.contains("720p") || combinedText.contains("hd") -> "720p HD"
        combinedText.contains("480p") || combinedText.contains("sd") || combinedText.contains("360p") -> "SD"
        else -> null
    }

    // 2. File Size
    var sizeText: String? = null
    val videoSize = stream.behaviorHints.videoSize
    if (videoSize != null && videoSize > 0) {
        sizeText = formatBytes(videoSize)
    } else {
        // Regex search for e.g. "1.5 gb" or "850 mb"
        val sizeRegex = Regex("""(\d+(?:\.\d+)?)\s*(gb|mb|gib|mib)""")
        val match = sizeRegex.find(combinedText)
        if (match != null) {
            val value = match.groupValues[1]
            val unit = match.groupValues[2].uppercase()
            sizeText = "$value $unit"
        }
    }

    // 3. Seeders
    var seeders: Int? = null
    if (stream.isTorrentStream) {
        val seederRegex = Regex("""(?:👤|s:|seeders:|seeds:|peers:)\s*(\d+)""")
        val match = seederRegex.find(combinedText)
        if (match != null) {
            seeders = match.groupValues[1].toIntOrNull()
        }
    }

    // 4. Codecs
    val codecs = mutableListOf<String>()
    if (combinedText.contains("hevc") || combinedText.contains("x265") || combinedText.contains("h265")) codecs.add("HEVC/x265")
    else if (combinedText.contains("x264") || combinedText.contains("h264") || combinedText.contains("avc")) codecs.add("AVC/x264")
    if (combinedText.contains("av1")) codecs.add("AV1")
    if (combinedText.contains("vp9")) codecs.add("VP9")

    // 5. HDR/DV
    val hdr = mutableListOf<String>()
    if (combinedText.contains("dv") || combinedText.contains("dolby vision") || combinedText.contains("dovi")) hdr.add("Dolby Vision")
    if (combinedText.contains("hdr10+")) hdr.add("HDR10+")
    else if (combinedText.contains("hdr")) hdr.add("HDR")

    // 6. Audio
    val audio = mutableListOf<String>()
    if (combinedText.contains("atmos")) audio.add("Atmos")
    if (combinedText.contains("truehd")) audio.add("TrueHD")
    if (combinedText.contains("dts-hd") || combinedText.contains("dtshd") || combinedText.contains("dts-x") || combinedText.contains("dtsx")) audio.add("DTS-HD")
    else if (combinedText.contains("dts")) audio.add("DTS")
    if (combinedText.contains("dd+") || combinedText.contains("eac3") || combinedText.contains("dolby digital plus") || combinedText.contains("ddp")) audio.add("DD+/E-AC3")
    else if (combinedText.contains("dd5.1") || combinedText.contains("dd 5.1") || combinedText.contains("ac3") || combinedText.contains("dolby digital")) audio.add("Dolby 5.1")
    else if (combinedText.contains("aac")) audio.add("AAC")
    else if (combinedText.contains("flac")) audio.add("FLAC")
    
    if (combinedText.contains("5.1")) audio.add("5.1 CH")
    if (combinedText.contains("7.1")) audio.add("7.1 CH")

    // 7. Languages
    val languages = mutableListOf<String>()
    if (combinedText.contains("multi") || combinedText.contains("dual")) {
        languages.add("Multi")
    }
    if (combinedText.contains("hin") || combinedText.contains("hindi")) languages.add("Hindi")
    if (combinedText.contains("eng") || combinedText.contains("english")) languages.add("English")
    if (combinedText.contains("tel") || combinedText.contains("telugu")) languages.add("Telugu")
    if (combinedText.contains("tam") || combinedText.contains("tamil")) languages.add("Tamil")
    if (combinedText.contains("spa") || combinedText.contains("spanish") || combinedText.contains("esp")) languages.add("Spanish")
    if (combinedText.contains("fre") || combinedText.contains("french") || combinedText.contains("fra")) languages.add("French")
    if (combinedText.contains("ger") || combinedText.contains("german") || combinedText.contains("deu")) languages.add("German")

    // 8. Source Type
    val sourceType = when {
        stream.isTorrentStream -> "Torrent"
        stream.streamFormat == StreamFormat.HLS -> "HLS"
        stream.streamFormat == StreamFormat.DASH -> "DASH"
        stream.url?.contains(".mp4") == true || stream.externalUrl?.contains(".mp4") == true -> "Direct MP4"
        stream.url?.startsWith("http") == true || stream.externalUrl?.startsWith("http") == true -> "Direct Link"
        else -> "Stream Link"
    }

    // 9. Reliability
    val reliability = when {
        !stream.isTorrentStream -> StreamReliability.HIGH
        seeders != null -> when {
            seeders > 50 -> StreamReliability.HIGH
            seeders > 10 -> StreamReliability.MEDIUM
            else -> StreamReliability.LOW
        }
        else -> StreamReliability.UNKNOWN
    }

    return EnhancedStreamMetadata(
        quality = resolution,
        resolution = resolution,
        sizeText = sizeText,
        seeders = seeders,
        codecs = codecs,
        audio = audio,
        hdr = hdr,
        languages = languages,
        sourceType = sourceType,
        reliability = reliability
    )
}
