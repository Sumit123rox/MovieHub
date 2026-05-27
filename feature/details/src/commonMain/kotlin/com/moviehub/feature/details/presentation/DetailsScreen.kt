package com.moviehub.feature.details.presentation

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.moviehub.core.model.MediaType
import com.moviehub.feature.details.presentation.components.*
import com.moviehub.core.ui.components.shimmerEffect
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
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
    viewModel: DetailsViewModel = koinViewModel()
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
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).animateContentSize()) {
            if (state.isLoading) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = paddingValues.calculateBottomPadding())
                ) {
                    // Hero Image Placeholder Shimmer
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(350.dp)
                            .shimmerEffect()
                    )
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Title Placeholder Shimmer
                        Box(
                            modifier = Modifier
                                .width(220.dp)
                                .height(32.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .shimmerEffect()
                        )
                        
                        // Metadata Row Placeholder Shimmer
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(modifier = Modifier.width(60.dp).height(18.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                            Box(modifier = Modifier.width(80.dp).height(18.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                            Box(modifier = Modifier.width(50.dp).height(18.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                        }
                        
                        // Description Lines Placeholder Shimmer
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.fillMaxWidth().height(16.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                            Box(modifier = Modifier.fillMaxWidth().height(16.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                            Box(modifier = Modifier.fillMaxWidth(0.6f).height(16.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                        }
                    }
                }
            } else if (state.mediaItem != null) {
                val media = state.mediaItem ?: return@Scaffold                
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = paddingValues.calculateBottomPadding() + 16.dp)
                ) {
                    item {
                        Box {
                            DetailHero(
                                media = media,
                                onHeightChanged = { /* Handle height if needed */ }
                            )
                            
                            val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                            IconButton(
                                onClick = onBackClick,
                                modifier = Modifier
                                    .padding(top = statusBarPadding + 8.dp, start = 8.dp)
                                    .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White
                                )
                            }
                        }
                    }

                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            val playLabel = when {
                                state.isWatched -> "Watch Again"
                                state.isInProgress -> {
                                    val pct = (state.watchProgressPercent * 100).toInt()
                                    "Continue ${pct}%"
                                }
                                media.type == MediaType.SHOW -> "S1 E1"
                                else -> "Play"
                            }

                            DetailActionButtons(
                                playLabel = playLabel,
                                onPlayClick = {
                                    val streamId = if (media.type == MediaType.SHOW) {
                                        // Default to S1E1 if main play is clicked
                                        "${media.id}:1:1"
                                    } else media.id
                                    onNavigateToStreams(streamId, media.type.stremioType, media.id)
                                },
                                isSaved = state.isFavorite,
                                onSaveClick = { viewModel.onAction(DetailsAction.ToggleFavorite) }
                            )

                            // In-progress mini bar
                            if (state.isInProgress) {
                                val pct = state.watchProgressPercent
                                LinearProgressIndicator(
                                    progress = { pct },
                                    modifier = Modifier.fillMaxWidth().height(2.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                )
                            }

                            // Watched Toggle
                            WatchedToggle(
                                isWatched = state.isWatched,
                                onToggle = { viewModel.onAction(DetailsAction.ToggleWatched) }
                            )

                            DetailMetaInfo(media = media)
                        }
                    }

                    if (media.cast.isNotEmpty()) {
                        item {
                            DetailCastSection(
                                cast = media.cast,
                                modifier = Modifier.padding(vertical = 16.dp),
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
                                modifier = Modifier.padding(vertical = 8.dp)
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
                                modifier = Modifier.padding(16.dp)
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
                                modifier = Modifier.padding(vertical = 16.dp)
                            )
                        }
                    } else {
                        item {
                            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)) {
                                Text(
                                    text = "More Like This",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontWeight = FontWeight.Bold,
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (state.isTmdbConfigured)
                                        "No related content found on TMDB for this title."
                                    else
                                        "Add a TMDB API key in Settings to get personalized recommendations.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(modifier = Modifier.height(12.dp))
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
                                modifier = Modifier.padding(vertical = 16.dp)
                            )
                        }
                    }
                }
            } else if (state.error != null) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = state.error!!, color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(16.dp))
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
                }
            )
        }
    }
}
