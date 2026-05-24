package com.moviehub.feature.home.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moviehub.core.model.MediaItem
import com.moviehub.core.ui.components.HeroCarousel
import com.moviehub.core.ui.components.shimmerEffect
import androidx.compose.ui.draw.clip
import com.moviehub.core.ui.components.Poster
import com.moviehub.core.ui.components.GlassyBox
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Info
import androidx.compose.foundation.shape.RoundedCornerShape
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onMediaClick: (id: String, type: String, addonUrl: String?) -> Unit,
    onAuthClick: () -> Unit,
    onSeeAllClick: (title: String, type: String, catalogId: String, addonId: String?) -> Unit,
    onSearchClick: () -> Unit,
    onAddonsClick: () -> Unit,
    viewModel: HomeViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = Color.Black
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().background(Color.Black).padding(paddingValues)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                // Featured/Hero Section
                if (state.featuredItems.isNotEmpty()) {
                    item(key = "hero_carousel") {
                        HeroCarousel(
                            items = state.featuredItems,
                            onItemClick = { movie ->
                                onMediaClick(
                                    movie.id,
                                    movie.type.stremioType,
                                    movie.sourceAddonUrl
                                )
                            }
                        )
                    }
                } else if (state.isLoading && state.dynamicSections.isEmpty()) {
                    // OLED-friendly high-fidelity skeleton shimmers instead of CircularProgressIndicator
                    items(3) { index ->
                        if (index == 0) {
                            // Hero Carousel Shimmer
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(450.dp)
                                    .padding(bottom = 24.dp)
                                    .shimmerEffect()
                            )
                        } else {
                            // Category Row Shimmer
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp, horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(140.dp)
                                        .height(22.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .shimmerEffect()
                                )
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(5) {
                                        Box(
                                            modifier = Modifier
                                                .width(130.dp)
                                                .height(190.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .shimmerEffect()
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else if (state.dynamicSections.isEmpty()) {
                    // Premium, Neutral-Platform Onboarding Layout
                    item {
                        Box(
                            modifier = Modifier
                                .fillParentMaxHeight(0.85f)
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            GlassyBox(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .wrapContentHeight()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(28.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(20.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .background(
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                                shape = RoundedCornerShape(16.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Extension,
                                            contentDescription = "External Addons",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }

                                    Text(
                                        text = "Neutral Streaming Shell",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White,
                                        textAlign = TextAlign.Center
                                    )

                                    Text(
                                        text = "MovieHub does not provide, host, or catalog any media content, channels, or streams by default. To start browsing movies, series, or video streams, you can add your own external providers.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.LightGray.copy(alpha = 0.8f),
                                        textAlign = TextAlign.Center,
                                        lineHeight = 22.sp
                                    )

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                Color.White.copy(alpha = 0.05f),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = "Info",
                                            tint = Color.White.copy(alpha = 0.6f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "Supports Stremio HTTP Addons and SkyStreams JS Plugins.",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.6f),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    Button(
                                        onClick = onAddonsClick,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = Color.Black
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                    ) {
                                        Text(
                                            text = "Configure External Providers",
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.5.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Dynamic Catalog Sections
                items(
                    items = state.dynamicSections,
                    key = { "${it.addonId}_${it.catalogId}_${it.type}" }
                ) { section ->
                    HomeSection(
                        title = section.catalogName,
                        subtitle = section.addonName,
                        items = section.items,
                        onSeeAllClick = {
                            onSeeAllClick(
                                section.catalogName,
                                section.type,
                                section.catalogId,
                                section.addonId
                            )
                        },
                        onItemClick = onMediaClick
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
fun HomeSection(
    title: String,
    subtitle: String?,
    items: List<MediaItem>,
    onSeeAllClick: () -> Unit,
    onItemClick: (id: String, type: String, addonUrl: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return

    Column(modifier = modifier.padding(vertical = 16.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Text(
                text = "SEE ALL",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onSeeAllClick() }.padding(bottom = 4.dp),
                maxLines = 1
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items, key = { "${it.sourceAddonId ?: ""}_${it.id}" }) { item ->
                Poster(
                    url = item.posterUrl,
                    contentDescription = item.title,
                    modifier = Modifier
                        .width(150.dp)
                        .height(225.dp),
                    onClick = { onItemClick(item.id, item.type.stremioType, item.sourceAddonUrl) }
                )
            }
        }
    }
}
