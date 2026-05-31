package com.moviehub.feature.details.presentation

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.moviehub.core.model.MediaType
import com.moviehub.core.ui.components.shimmerEffect
import com.moviehub.core.ui.theme.MovieHubDimens
import com.moviehub.feature.details.presentation.components.*
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DetailsScreen(
    id: String,
    type: String,
    addonUrl: String? = null,
    onNavigateToStreams: (id: String, type: String, mediaId: String) -> Unit = { _, _, _ -> },
    onNavigateToDetails: (id: String, type: String) -> Unit = { _, _ -> },
    onBackClick: () -> Unit = {},
    onCastClick: ((com.moviehub.core.model.MediaPerson) -> Unit)? = null,
    onNavigateToSettings: () -> Unit = {},
    viewModel: DetailsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var selectedTrailer by remember { mutableStateOf<com.moviehub.core.model.MediaTrailer?>(null) }

    val listState = rememberLazyListState()

    LaunchedEffect(id, type, addonUrl) {
        viewModel.onAction(DetailsAction.LoadDetails(id, type, addonUrl))
    }

    // Intercept system back when trailer popup is showing — dismiss trailer instead of navigating away
    PlatformBackHandler(enabled = selectedTrailer != null) {
        selectedTrailer = null
        viewModel.onAction(DetailsAction.ClearTrailer)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).animateContentSize()) {
            // Dynamic Aurora-gradient backdrop overlay for premium Gen-Z visual weight
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(MovieHubDimens.Shimmer.heroHeight)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.04f),
                                Color.Transparent,
                            )
                        )
                    )
            )

            if (state.isLoading) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = paddingValues.calculateBottomPadding()),
                ) {
                    // Hero Image Placeholder Shimmer
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(350.dp)
                            .shimmerEffect(),
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(MovieHubDimens.Spacing.xxl),
                        verticalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.lg),
                    ) {
                        // Title Placeholder Shimmer
                        Box(
                            modifier = Modifier
                                .width(220.dp)
                                .height(MovieHubDimens.Spacing.xxxl)
                                .clip(RoundedCornerShape(MovieHubDimens.Spacing.xs))
                                .shimmerEffect(),
                        )

                        // Metadata Row Placeholder Shimmer
                        Row(horizontalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.md)) {
                            Box(modifier = Modifier.width(MovieHubDimens.EmptyState.iconSize).height(MovieHubDimens.Player.seekBarThumb).clip(RoundedCornerShape(MovieHubDimens.Spacing.xxs)).shimmerEffect())
                            Box(modifier = Modifier.width(MovieHubDimens.Player.shimmerHeight).height(MovieHubDimens.Player.seekBarThumb).clip(RoundedCornerShape(MovieHubDimens.Spacing.xxs)).shimmerEffect())
                            Box(modifier = Modifier.width(50.dp).height(MovieHubDimens.Player.seekBarThumb).clip(RoundedCornerShape(MovieHubDimens.Spacing.xxs)).shimmerEffect())
                        }

                        // Description Lines Placeholder Shimmer
                        Column(verticalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.sm)) {
                            Box(modifier = Modifier.fillMaxWidth().height(MovieHubDimens.Spacing.lg).clip(RoundedCornerShape(MovieHubDimens.Spacing.xxs)).shimmerEffect())
                            Box(modifier = Modifier.fillMaxWidth().height(MovieHubDimens.Spacing.lg).clip(RoundedCornerShape(MovieHubDimens.Spacing.xxs)).shimmerEffect())
                            Box(modifier = Modifier.fillMaxWidth(0.6f).height(MovieHubDimens.Spacing.lg).clip(RoundedCornerShape(MovieHubDimens.Spacing.xxs)).shimmerEffect())
                        }
                    }
                }
            } else if (state.mediaItem != null) {
                val media = state.mediaItem ?: return@Scaffold
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = paddingValues.calculateBottomPadding() + MovieHubDimens.Spacing.lg),
                ) {
                    item {
                        Box {
                            DetailHero(
                                media = media,
                                onHeightChanged = { /* Handle height if needed */ },
                            )

                            val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                            IconButton(
                                onClick = onBackClick,
                                modifier = Modifier
                                    .padding(top = statusBarPadding + MovieHubDimens.Spacing.sm, start = MovieHubDimens.Spacing.sm)
                                    .background(Color.Black.copy(alpha = 0.3f), CircleShape),
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White,
                                )
                            }
                        }
                    }

                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = MovieHubDimens.Spacing.lg),
                            verticalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.lg),
                        ) {
                            // Derive default play target from actual first video (not hardcoded S1E1)
                            val firstVideo = remember(media.videos) {
                                media.videos
                                    .filter { it.season != null || it.episode != null }
                                    .minWithOrNull(compareBy({ it.season ?: 0 }, { it.episode ?: 0 }))
                            }
                            val defaultSeason = firstVideo?.season ?: 1
                            val defaultEpisode = firstVideo?.episode ?: 1
                            val defaultStreamId = if (media.type == MediaType.SHOW) {
                                if (firstVideo?.id?.contains(":") == true) {
                                    firstVideo.id
                                } else {
                                    "${media.id}:$defaultSeason:$defaultEpisode"
                                }
                            } else {
                                media.id
                            }

                            val playLabel = when {
                                state.isWatched -> "Watch Again"
                                state.isInProgress -> {
                                    val pct = (state.watchProgressPercent * 100).toInt()
                                    "Continue $pct%"
                                }
                                media.type == MediaType.SHOW -> "S$defaultSeason E$defaultEpisode"
                                else -> "Play"
                            }

                            DetailActionButtons(
                                playLabel = playLabel,
                                onPlayClick = {
                                    onNavigateToStreams(defaultStreamId, media.type.stremioType, media.id)
                                },
                                isSaved = state.isFavorite,
                                onSaveClick = { viewModel.onAction(DetailsAction.ToggleFavorite) },
                            )

                            // In-progress mini bar
                            if (state.isInProgress) {
                                val pct = state.watchProgressPercent
                                LinearProgressIndicator(
                                    progress = { pct },
                                    modifier = Modifier.fillMaxWidth().height(MovieHubDimens.Spacing.dp2),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                )
                            }

                            // Watched Toggle
                            WatchedToggle(
                                isWatched = state.isWatched,
                                onToggle = { viewModel.onAction(DetailsAction.ToggleWatched) },
                            )

                            DetailMetaInfo(media = media)
                        }
                    }

                    if (media.cast.isNotEmpty()) {
                        item {
                            DetailCastSection(
                                cast = media.cast,
                                modifier = Modifier.padding(vertical = MovieHubDimens.Spacing.lg),
                                onCastClick = onCastClick,
                            )
                        }
                    }

                    if (media.trailers.isNotEmpty()) {
                        item {
                            DetailTrailersSection(
                                trailers = media.trailers,
                                onTrailerClick = { trailer ->
                                    selectedTrailer = trailer
                                    viewModel.onAction(DetailsAction.LoadTrailer(trailer.url))
                                },
                                modifier = Modifier.padding(vertical = MovieHubDimens.Spacing.sm),
                            )
                        }
                    }

                    if (media.type == MediaType.SHOW) {
                        item {
                            DetailSeriesContent(
                                media = media,
                                onEpisodeClick = { episode ->
                                    val streamId = if (episode.id.isNotBlank() && episode.id.contains(":")) episode.id else "${media.id}:${episode.season}:${episode.episode}"
                                    onNavigateToStreams(streamId, "series", media.id)
                                },
                            )
                        }
                    }

                    if (media.productionCompanies.isNotEmpty() || media.networks.isNotEmpty()) {
                        item {
                            DetailProductionSection(
                                media = media,
                                modifier = Modifier.padding(MovieHubDimens.Spacing.lg),
                            )
                        }
                    }

                    if (media.moreLikeThis.isNotEmpty()) {
                        item {
                            DetailPosterRailSection(
                                title = "More Like This",
                                items = media.moreLikeThis,
                                onPosterClick = { preview ->
                                    onNavigateToDetails(preview.id, preview.type)
                                },
                                modifier = Modifier.padding(vertical = MovieHubDimens.Spacing.lg),
                            )
                        }
                    } else {
                        item {
                            Column(modifier = Modifier.padding(start = MovieHubDimens.Spacing.lg, end = MovieHubDimens.Spacing.lg, top = MovieHubDimens.Spacing.sm, bottom = MovieHubDimens.Spacing.sm)) {
                                Text(
                                    text = "More Like This",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontWeight = FontWeight.Bold,
                                )
                                Spacer(modifier = Modifier.height(MovieHubDimens.Spacing.sm))
                                Text(
                                    text = if (state.isTmdbConfigured) {
                                        "No related content found on TMDB for this title."
                                    } else {
                                        "Add a TMDB API key in Settings to get personalized recommendations."
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(modifier = Modifier.height(MovieHubDimens.Spacing.md))
                                Button(onClick = onNavigateToSettings) {
                                    Text("Go to Settings")
                                }
                            }
                        }
                    }

                    if (media.collectionItems.isNotEmpty()) {
                        item {
                            DetailPosterRailSection(
                                title = media.collectionName ?: "Collection",
                                items = media.collectionItems,
                                onPosterClick = { preview ->
                                    onNavigateToDetails(preview.id, preview.type)
                                },
                                modifier = Modifier.padding(vertical = MovieHubDimens.Spacing.lg),
                            )
                        }
                    }
                }
            } else if (state.error != null) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(text = state.error!!, color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(MovieHubDimens.Spacing.lg))
                    Button(onClick = { viewModel.onAction(DetailsAction.LoadDetails(id, type, addonUrl)) }) {
                        Text("Retry")
                    }
                }
            }
        }

        // Trailer Player Popup - Now using the Universal Player
        if (selectedTrailer != null) {
            TrailerPlayerPopup(
                trailer = selectedTrailer,
                source = state.selectedTrailerSource,
                isLoading = state.isResolvingTrailer,
                onDismiss = {
                    selectedTrailer = null
                    viewModel.onAction(DetailsAction.ClearTrailer)
                },
            )
        }
    }
}
