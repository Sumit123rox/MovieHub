package com.moviehub.feature.home.presentation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moviehub.core.model.ContinueWatchingItem
import com.moviehub.core.model.MediaItem
import com.moviehub.core.model.MediaItemStore
import com.moviehub.core.ui.components.ContentCard
import com.moviehub.core.ui.components.HeroCarousel
import com.moviehub.core.ui.components.Poster
import com.moviehub.core.ui.components.shimmerEffect
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onMediaClick: (id: String, type: String, addonUrl: String?) -> Unit,
    onSeeAllClick: (title: String, type: String, catalogId: String, addonId: String?) -> Unit,
    onAddonsClick: () -> Unit,
    onResumeClick: (mediaId: String, type: String) -> Unit,
    viewModel: HomeViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    var onboardingDismissed by rememberSaveable { mutableStateOf(false) }
    var selectedCategoryType by rememberSaveable { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()

    // Detect when user scrolls near the bottom to trigger lazy loading
    val shouldLoadMore by remember {
        derivedStateOf {
            if (!state.hasMoreSections || state.isLoadingMore) false
            else {
                val layoutInfo = listState.layoutInfo
                val totalItems = layoutInfo.totalItemsCount
                val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                totalItems > 0 && lastVisible >= totalItems - 4
            }
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            delay(200.milliseconds)
            viewModel.onAction(HomeAction.LoadMore)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = state.isLoading && state.installedAddons.isNotEmpty(),
            onRefresh = { viewModel.onAction(HomeAction.Refresh) },
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState
            ) {
                // Dismissable onboarding banner — only when no addons are installed
                if (state.installedAddons.isEmpty() && !state.isLoading && !onboardingDismissed) {
                    item(key = "onboarding_banner") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
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
                        AnimatedEntry(index = 0) {
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
                        AnimatedEntry(index = 1) {
                            ContinueWatchingSection(
                                items = state.continueWatching,
                                onResumeClick = { item ->
                                    onResumeClick(item.mediaId, item.type)
                                },
                                onMediaClick = { item ->
                                    onResumeClick(item.mediaId, item.type)
                                },
                                onDetailsClick = { item ->
                                    onMediaClick(item.mediaId, item.type, null)
                                },
                                onMarkAsWatched = { item ->
                                    viewModel.onAction(HomeAction.MarkAsWatched(item.mediaId))
                                },
                                onRemoveFromContinue = { item ->
                                    viewModel.onAction(HomeAction.RemoveFromContinue(item.mediaId))
                                }
                            )
                        }
                    }
                }

                // Category filter chips — placed here so filtering has visible effect below
                if (state.dynamicSections.isNotEmpty()) {
                    item(key = "category_filter") {
                        CategoryFilterBar(
                            selectedType = selectedCategoryType,
                            onTypeSelected = { selectedCategoryType = it },
                            sections = state.dynamicSections
                        )
                    }
                }

                // Dynamic Catalog Sections (filtered by selected category type)
                val displaySections = if (selectedCategoryType != null) {
                    state.dynamicSections.filter { it.type == selectedCategoryType }
                } else state.dynamicSections
                items(
                    items = displaySections,
                    key = { "${it.addonId}_${it.catalogId}_${it.type}" }
                ) { section ->
                    AnimatedEntry(index = 2) {
                        HomeSection(
                            title = section.catalogName,
                            subtitle = null,
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

                // Lazy loading indicator — shown while more sections are being fetched
                if (state.isLoadingMore) {
                    item(key = "loading_more_indicator") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Loading more...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }

                if (!state.hasMoreSections && !state.isLoadingMore && state.dynamicSections.isNotEmpty()) {
                    item(key = "end_credit") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 32.dp, bottom = 56.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Decorative divider with sparkle
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(48.dp)
                                        .height(1.dp)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                                )
                                Text(
                                    text = "✦",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                )
                                Box(
                                    modifier = Modifier
                                        .width(48.dp)
                                        .height(1.dp)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // Main closing message
                            Text(
                                text = "You've reached the end 🎬",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                letterSpacing = 0.3.sp
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "But the journey never ends ✨",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                                letterSpacing = 0.2.sp
                            )

                            Spacer(modifier = Modifier.height(28.dp))

                            // Premium badge-style footer
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.03f),
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
                                            )
                                        )
                                    )
                                    .border(
                                        width = 1.dp,
                                        brush = Brush.linearGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.04f),
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                            )
                                        ),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .padding(horizontal = 28.dp, vertical = 18.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(text = "🇮🇳", fontSize = 22.sp)
                                    Column {
                                        Text(
                                            text = "Made with love",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                            letterSpacing = 0.3.sp
                                        )
                                        Text(
                                            text = "in India",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                            letterSpacing = 1.sp
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // Closing quote
                            Text(
                                text = "⌂ MovieHub",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f),
                                letterSpacing = 2.sp
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun AnimatedEntry(
    index: Int = 0,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay((index * 40L).coerceAtMost(300))
        alpha.animateTo(1f, tween(350))
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                this.alpha = alpha.value
                this.scaleX = 0.94f + alpha.value * 0.06f
                this.scaleY = 0.94f + alpha.value * 0.06f
            },
        content = content
    )
}

@Composable
private fun CategoryFilterBar(
    selectedType: String?,
    onTypeSelected: (String?) -> Unit,
    sections: List<CatalogSection>,
    modifier: Modifier = Modifier
) {
    val types = remember(sections) {
        sections.map { it.type }.distinct().sorted()
    }
    if (types.isEmpty()) return

    val typeLabels = mapOf(
        "movie" to "🎬 Movies",
        "series" to "📺 Series",
        "tv" to "📡 TV",
        "anime" to "🎌 Anime",
        "channel" to "📡 Channels"
    )

    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item(key = "all") {
            CategoryChip(
                selected = selectedType == null,
                label = "✨ All",
                onClick = { onTypeSelected(null) }
            )
        }
        items(types, key = { it }) { type ->
            CategoryChip(
                selected = selectedType == type,
                label = typeLabels[type] ?: type.replaceFirstChar { it.uppercase() },
                onClick = { onTypeSelected(type) }
            )
        }
    }
}

@Composable
private fun CategoryChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        )
    }
}

@Composable
fun ContinueWatchingSection(
    items: List<ContinueWatchingItem>,
    onResumeClick: (ContinueWatchingItem) -> Unit,
    onMediaClick: (ContinueWatchingItem) -> Unit,
    onDetailsClick: (ContinueWatchingItem) -> Unit,
    onMarkAsWatched: (ContinueWatchingItem) -> Unit,
    onRemoveFromContinue: (ContinueWatchingItem) -> Unit,
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

        val continueListState = rememberLazyListState()
        LazyRow(
            state = continueListState,
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items, key = { item -> item.mediaId }) { item ->
                ContinueWatchingCard(
                    item = item,
                    onResumeClick = { onResumeClick(item) },
                    onMediaClick = { onMediaClick(item) },
                    onDetailsClick = { onDetailsClick(item) },
                    onMarkAsWatched = { onMarkAsWatched(item) },
                    onRemoveFromContinue = { onRemoveFromContinue(item) },
                    modifier = Modifier.width(160.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ContinueWatchingCard(
    item: ContinueWatchingItem,
    onResumeClick: () -> Unit,
    onMediaClick: () -> Unit,
    onDetailsClick: () -> Unit,
    onMarkAsWatched: () -> Unit,
    onRemoveFromContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = if (item.durationMs > 0)
        (item.progressMs.toFloat() / item.durationMs).coerceIn(0f, 1f) else 0f
    val isFullyWatched = item.durationMs > 0 && progress >= 0.9f
    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(12.dp))
                .combinedClickable(
                    onClick = onMediaClick,
                    onLongClick = { showSheet = true }
                )
        ) {
            Poster(
                url = item.posterUrl,
                contentDescription = item.title,
                title = item.title,
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

        // BottomSheet on long press
        if (showSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSheet = false },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp)
                        .padding(horizontal = 8.dp),
                ) {
                    // Poster thumbnail + title header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .width(60.dp)
                                .height(90.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            if (item.posterUrl != null) {
                                Poster(
                                    url = item.posterUrl,
                                    contentDescription = item.title,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )

                    // Details
                    ContinueWatchingSheetButton(
                        icon = Icons.Default.Info,
                        label = "Details",
                        onClick = {
                            showSheet = false
                            onDetailsClick()
                        }
                    )

                    // Mark as Watched
                    ContinueWatchingSheetButton(
                        icon = Icons.Default.CheckCircle,
                        label = "Mark as Watched",
                        onClick = {
                            showSheet = false
                            onMarkAsWatched()
                        }
                    )

                    // Remove
                    ContinueWatchingSheetButton(
                        icon = Icons.Default.Close,
                        label = "Remove from Continue Watching",
                        onClick = {
                            showSheet = false
                            onRemoveFromContinue()
                        },
                        isDestructive = true
                    )
                }
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
private fun ContinueWatchingSheetButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    val contentColor = if (isDestructive)
        MaterialTheme.colorScheme.error
    else
        MaterialTheme.colorScheme.onSurface

    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor.copy(alpha = 0.7f),
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = contentColor,
                modifier = Modifier.weight(1f)
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
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
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

        val rowListState = rememberLazyListState()
        LazyRow(
            state = rowListState,
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(mediaItems, key = { item -> "${item.sourceAddonId ?: ""}_${item.id}" }) { item ->
                Column(
                    modifier = Modifier.width(150.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Poster(
                        url = item.posterUrl,
                        contentDescription = item.title,
                        modifier = Modifier.fillMaxWidth().height(225.dp),
                        isWatched = item.id in watchedMediaIds,
                        onClick = { onItemClick(item.id, item.type.stremioType, item.sourceAddonUrl) }
                    )
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
