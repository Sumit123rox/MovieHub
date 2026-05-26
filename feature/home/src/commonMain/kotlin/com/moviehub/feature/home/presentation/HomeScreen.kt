package com.moviehub.feature.home.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moviehub.core.model.ContinueWatchingItem
import com.moviehub.core.model.MediaItem
import com.moviehub.core.model.MediaItemStore
import com.moviehub.core.ui.components.HeroCarousel
import com.moviehub.core.ui.components.Poster
import com.moviehub.core.ui.components.ContentCard
import com.moviehub.core.ui.components.shimmerEffect
import com.moviehub.core.ui.theme.MovieHubColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Info
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onMediaClick: (id: String, type: String, addonUrl: String?) -> Unit,
    onAuthClick: () -> Unit,
    onSeeAllClick: (title: String, type: String, catalogId: String, addonId: String?) -> Unit,
    onSearchClick: () -> Unit,
    onAddonsClick: () -> Unit,
    onResumeClick: (mediaId: String, type: String) -> Unit,
    viewModel: HomeViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    var onboardingDismissed by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(paddingValues)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                // Dismissable onboarding banner — only when no addons are installed
                if (state.installedAddons.isEmpty() && !state.isLoading && !onboardingDismissed) {
                    item(key = "onboarding_banner") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp, 80.dp, 16.dp, 16.dp)
                        ) {
                            ContentCard(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .background(
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                                shape = RoundedCornerShape(14.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Extension,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }

                                    Text(
                                        text = "Get Started",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center
                                    )

                                    Text(
                                        text = "Add an external provider to start browsing movies and series.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                        textAlign = TextAlign.Center,
                                        lineHeight = 18.sp
                                    )

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                                shape = RoundedCornerShape(6.dp)
                                            )
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "Supports Stremio HTTP Addons & JS Plugins",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    Button(
                                        onClick = onAddonsClick,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(44.dp)
                                    ) {
                                        Text(
                                            text = "Configure External Providers",
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.5.sp
                                        )
                                    }
                                }
                            }

                            // Close (dismiss) button
                            IconButton(
                                onClick = { onboardingDismissed = true },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(32.dp)
                                    .padding(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // Featured/Hero Section
                if (state.featuredItems.isNotEmpty()) {
                    item(key = "hero_carousel") {
                        HeroCarousel(
                            items = state.featuredItems,
                            onItemClick = { movie ->
                                MediaItemStore.put(movie.id, movie)
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
                }

                // Continue Watching Section
                if (state.continueWatching.isNotEmpty()) {
                    item(key = "continue_watching") {
                        ContinueWatchingSection(
                            items = state.continueWatching,
                            onResumeClick = { item ->
                                onResumeClick(item.mediaId, item.type)
                            },
                            onMediaClick = { item ->
                                onResumeClick(item.mediaId, item.type)
                            }
                        )
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
                        mediaItems = section.items,
                        watchedMediaIds = state.watchedMediaIds,
                        onSeeAllClick = {
                            onSeeAllClick(
                                section.catalogName,
                                section.type,
                                section.catalogId,
                                section.addonId
                            )
                        },
                        onItemClick = { id, type, addonUrl ->
                            // Cache item in MediaItemStore before navigating to Details
                            state.dynamicSections
                                .flatMap { it.items }
                                .firstOrNull { it.id == id }
                                ?.let { MediaItemStore.put(it.id, it) }
                            onMediaClick(id, type, addonUrl)
                        }
                    )
                }

                // Empty state — always shown when no content is available
                if (state.dynamicSections.isEmpty() && !state.isLoading) {
                    item(key = "empty_home") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 32.dp, vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (state.installedAddons.isEmpty())
                                    "No catalogs available. Add an external provider to get started."
                                else
                                    "This provider does not support catalog browsing. Add a different addon for catalogs.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
fun ContinueWatchingSection(
    items: List<ContinueWatchingItem>,
    onResumeClick: (ContinueWatchingItem) -> Unit,
    onMediaClick: (ContinueWatchingItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return

    Column(modifier = modifier.padding(vertical = 12.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Continue Watching",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Pick up where you left off",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items, key = { it.mediaId }) { item ->
                ContinueWatchingCard(
                    item = item,
                    onResumeClick = { onResumeClick(item) },
                    onMediaClick = { onMediaClick(item) },
                    modifier = Modifier.width(160.dp)
                )
            }
        }
    }
}

@Composable
fun ContinueWatchingCard(
    item: ContinueWatchingItem,
    onResumeClick: () -> Unit,
    onMediaClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = if (item.durationMs > 0)
        (item.progressMs.toFloat() / item.durationMs).coerceIn(0f, 1f) else 0f
    val isFullyWatched = item.durationMs > 0 && progress >= 0.9f

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onMediaClick)
        ) {
            Poster(
                url = item.posterUrl,
                contentDescription = item.title,
                modifier = Modifier.fillMaxSize(),
                isWatched = isFullyWatched,
                progressFraction = if (!isFullyWatched) progress else -1f
            )

            // Gradient overlay at bottom for readability
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.85f)
                            )
                        )
                    )
            )

            // Play button overlay
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable(onClick = onResumeClick),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "▶",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = item.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        if (item.durationMs > 0) {
            val remainingMs = item.durationMs - item.progressMs
            val remainingText = when {
                remainingMs < 60_000 -> "<1 min left"
                remainingMs < 3_600_000 -> "${remainingMs / 60_000} min left"
                else -> "${remainingMs / 3_600_000}h ${(remainingMs % 3_600_000) / 60_000}m left"
            }
            Text(
                text = remainingText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                maxLines = 1
            )
        }
    }
}

@Composable
fun HomeSection(
    title: String,
    subtitle: String?,
    mediaItems: List<MediaItem>,
    watchedMediaIds: Set<String> = emptySet(),
    onSeeAllClick: () -> Unit,
    onItemClick: (id: String, type: String, addonUrl: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    if (mediaItems.isEmpty()) return

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
                    color = MaterialTheme.colorScheme.onSurface,
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
            items(mediaItems, key = { "${it.sourceAddonId ?: ""}_${it.id}" }) { item ->
                Poster(
                    url = item.posterUrl,
                    contentDescription = item.title,
                    modifier = Modifier
                        .width(150.dp)
                        .height(225.dp),
                    isWatched = item.id in watchedMediaIds,
                    onClick = { onItemClick(item.id, item.type.stremioType, item.sourceAddonUrl) }
                )
            }
        }
    }
}
