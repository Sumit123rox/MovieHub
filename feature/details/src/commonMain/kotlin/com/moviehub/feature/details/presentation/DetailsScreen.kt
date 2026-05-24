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
    onBackClick: () -> Unit = {},
    viewModel: DetailsViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    var selectedTrailer by remember { mutableStateOf<com.moviehub.core.model.MediaTrailer?>(null) }
    
    val listState = rememberLazyListState()

    LaunchedEffect(id, type, addonUrl) {
        viewModel.onAction(DetailsAction.LoadDetails(id, type, addonUrl))
    }

    Scaffold(
        containerColor = Color.Black,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().background(Color.Black).animateContentSize()) {
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
                val media = state.mediaItem!!
                
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = paddingValues.calculateBottomPadding() + 80.dp)
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
                            DetailActionButtons(
                                onPlayClick = { 
                                    val streamId = if (media.type == MediaType.SHOW) {
                                        // Default to S1E1 if main play is clicked
                                        "${media.id}:1:1"
                                    } else media.id
                                    onNavigateToStreams(streamId, media.type.stremioType, media.id)
                                },
                                onSaveClick = { /* Handle Save */ }
                            )

                            DetailMetaInfo(media = media)
                        }
                    }

                    item {
                        DetailCastSection(
                            cast = media.cast,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    }

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

                    if (media.type == MediaType.SHOW) {
                        item {
                            DetailSeriesContent(
                                media = media,
                                onEpisodeClick = { episode ->
                                    val streamId = if (episode.id.isNotBlank() && episode.id.contains(":")) episode.id else "${media.id}:${episode.season}:${episode.episode}"
                                    onNavigateToStreams(streamId, "series", media.id)
                                }
                            )
                        }
                    }

                    item {
                        DetailProductionSection(
                            media = media,
                            modifier = Modifier.padding(16.dp)
                        )
                    }

                    item {
                        DetailAdditionalInfoSection(
                            media = media,
                            modifier = Modifier.padding(16.dp)
                        )
                    }

                    item {
                        DetailPosterRailSection(
                            title = "More Like This",
                            items = media.moreLikeThis,
                            onPosterClick = { preview ->
                                viewModel.onAction(DetailsAction.LoadDetails(preview.id, preview.type, null))
                            },
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    }

                    if (media.collectionItems.isNotEmpty()) {
                        item {
                            DetailPosterRailSection(
                                title = media.collectionName ?: "Collection",
                                items = media.collectionItems,
                                onPosterClick = { preview ->
                                    viewModel.onAction(DetailsAction.LoadDetails(preview.id, preview.type, null))
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
