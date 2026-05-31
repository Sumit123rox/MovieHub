package com.moviehub.feature.home.presentation

import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.moviehub.core.model.MediaType
import com.moviehub.core.ui.components.ContentCard
import com.moviehub.core.ui.components.EmptyState
import com.moviehub.core.ui.components.HeroCarousel
import com.moviehub.core.ui.components.Poster
import com.moviehub.core.ui.components.SmartStatusBar
import com.moviehub.core.ui.components.shimmerEffect
import com.moviehub.core.ui.theme.MovieHubDimens
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
    viewModel: HomeViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var onboardingDismissed by rememberSaveable { mutableStateOf(false) }
    var selectedCategoryType by rememberSaveable { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()

    // Detect when user scrolls near the bottom to trigger lazy loading
    val shouldLoadMore by remember {
        derivedStateOf {
            if (!state.hasMoreSections || state.isLoadingMore) {
                false
            } else {
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

    SmartStatusBar(
        isDark = true,
        color = MaterialTheme.colorScheme.background,
    )

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { viewModel.onAction(HomeAction.Refresh) },
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues),
        ) {
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

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
            ) {
                // Dismissable onboarding banner — only when no addons are installed
                if (state.installedAddons.isEmpty() && !state.isLoading && !onboardingDismissed) {
                    item(key = "onboarding_banner") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(MovieHubDimens.Spacing.lg),
                        ) {
                            ContentCard(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(MovieHubDimens.Spacing.xl),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.md),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(MovieHubDimens.Avatar.lg)
                                            .background(
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                                shape = RoundedCornerShape(MovieHubDimens.Spacing.ml),
                                            ),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Extension,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(MovieHubDimens.Icon.xl),
                                        )
                                    }

                                    Text(
                                        text = "Get Started",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center,
                                    )

                                    Text(
                                        text = "Add an external provider to start browsing movies and series.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                        textAlign = TextAlign.Center,
                                        lineHeight = MovieHubDimens.Font.xxl,
                                    )

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                                shape = RoundedCornerShape(MovieHubDimens.Spacing.xs),
                                            )
                                            .padding(MovieHubDimens.Spacing.ms),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.sm),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                            modifier = Modifier.size(MovieHubDimens.Spacing.ml),
                                        )
                                        Text(
                                            text = "Supports Stremio HTTP Addons & JS Plugins",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                            modifier = Modifier.weight(1f),
                                        )
                                    }

                                    Button(
                                        onClick = onAddonsClick,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary,
                                        ),
                                        shape = RoundedCornerShape(MovieHubDimens.Spacing.sm),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(MovieHubDimens.Player.sideSliderWidth),
                                    ) {
                                        Text(
                                            text = "Configure External Providers",
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.5.sp,
                                        )
                                    }
                                }
                            }

                            // Close (dismiss) button
                            IconButton(
                                onClick = { onboardingDismissed = true },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(MovieHubDimens.Spacing.xxxl)
                                    .padding(MovieHubDimens.Spacing.xxs),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    modifier = Modifier.size(MovieHubDimens.Player.seekBarThumb),
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
                                        movie.sourceAddonUrl,
                                    )
                                },
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
                                    .height(MovieHubDimens.Shimmer.heroHeight)
                                    .padding(bottom = MovieHubDimens.Spacing.xxl)
                                    .shimmerEffect(),
                            )
                        } else {
                            // Category Row Shimmer
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = MovieHubDimens.Spacing.lg, horizontal = MovieHubDimens.Spacing.lg),
                                verticalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.md),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(MovieHubDimens.Shimmer.titleWidth)
                                        .height(MovieHubDimens.Icon.md)
                                        .clip(RoundedCornerShape(MovieHubDimens.Spacing.xxs))
                                        .shimmerEffect(),
                                )
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.md),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    items(5) {
                                        Box(
                                            modifier = Modifier
                                                .width(MovieHubDimens.Shimmer.cardWidth)
                                                .height(MovieHubDimens.Shimmer.cardHeight)
                                                .clip(RoundedCornerShape(MovieHubDimens.Spacing.sm))
                                                .shimmerEffect(),
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
                                },
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
                            sections = state.dynamicSections,
                        )
                    }
                }

                // Dynamic Catalog Sections (filtered by selected category type)
                val displaySections = if (selectedCategoryType != null) {
                    state.dynamicSections.filter { it.type == selectedCategoryType }
                } else {
                    state.dynamicSections
                }
                items(
                    items = displaySections,
                    key = { "${it.addonId}_${it.catalogId}_${it.type}" },
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
                                    section.addonId,
                                )
                            },
                            onItemClick = { id, type, addonUrl ->
                                // Cache item in MediaItemStore before navigating to Details
                                state.dynamicSections
                                    .flatMap { it.items }
                                    .firstOrNull { it.id == id }
                                    ?.let { MediaItemStore.put(it.id, it) }
                                onMediaClick(id, type, addonUrl)
                            },
                            onItemHover = { id, type, addonId ->
                                viewModel.onAction(HomeAction.PrewarmCatalogItem(id, type, addonId))
                            },
                        )
                    }
                }

                // Empty state — always shown when no content is available
                if (state.dynamicSections.isEmpty() && !state.isLoading) {
                    item(key = "empty_home") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = MovieHubDimens.Icon.xxxl),
                            contentAlignment = Alignment.Center,
                        ) {
                            EmptyState(
                                icon = Icons.Default.Extension,
                                title = if (state.installedAddons.isEmpty()) {
                                    "No catalogs available"
                                } else {
                                    "No content available"
                                },
                                subtitle = if (state.installedAddons.isEmpty()) {
                                    "Add an external provider to get started."
                                } else {
                                    "This provider does not support catalog browsing. Add a different addon for catalogs."
                                },
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(MovieHubDimens.Spacing.xxxl))
                }

                // Lazy loading indicator — shown while more sections are being fetched
                if (state.isLoadingMore) {
                    item(key = "loading_more_indicator") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(MovieHubDimens.Spacing.lg),
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.sm),
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(MovieHubDimens.Spacing.lg),
                                    strokeWidth = MovieHubDimens.Spacing.dp2,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Text(
                                    text = "Loading more...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
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
                                .padding(top = MovieHubDimens.Spacing.xxxl, bottom = MovieHubDimens.Avatar.lg),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            // Decorative divider with sparkle
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.md),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(MovieHubDimens.Icon.xxxl)
                                        .height(MovieHubDimens.Spacing.dp1)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                                )
                                Text(
                                    text = "✦",
                                    fontSize = MovieHubDimens.Font.xs,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                )
                                Box(
                                    modifier = Modifier
                                        .width(MovieHubDimens.Icon.xxxl)
                                        .height(MovieHubDimens.Spacing.dp1)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                                )
                            }

                            Spacer(modifier = Modifier.height(MovieHubDimens.Spacing.xxl))

                            // Main closing message
                            Text(
                                text = "You've reached the end 🎬",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                letterSpacing = 0.3.sp,
                            )

                            Spacer(modifier = Modifier.height(MovieHubDimens.Spacing.xs))

                            Text(
                                text = "But the journey never ends ✨",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                                letterSpacing = 0.2.sp,
                            )

                            Spacer(modifier = Modifier.height(MovieHubDimens.Icon.xl))

                            // Premium badge-style footer
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(MovieHubDimens.Spacing.lg))
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.03f),
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
                                            ),
                                        ),
                                    )
                                    .border(
                                        width = MovieHubDimens.Spacing.dp1,
                                        brush = Brush.linearGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.04f),
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                            ),
                                        ),
                                        shape = RoundedCornerShape(MovieHubDimens.Spacing.lg),
                                    )
                                    .padding(horizontal = MovieHubDimens.Icon.xl, vertical = MovieHubDimens.Player.seekBarThumb),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.md),
                                ) {
                                    Text(text = "🇮🇳", fontSize = MovieHubDimens.Font.headline)
                                    Column {
                                        Text(
                                            text = "Made with love",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                            letterSpacing = 0.3.sp,
                                        )
                                        Text(
                                            text = "in India",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                            letterSpacing = 1.sp,
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(MovieHubDimens.Spacing.xl))

                            // Closing quote
                            Text(
                                text = "⌂ MovieHub",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f),
                                letterSpacing = 2.sp,
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(MovieHubDimens.Spacing.lg))
                }
            }
        }
    }
}

@Composable
private fun AnimatedEntry(
    index: Int = 0,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
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
        content = content,
    )
}

@Composable
private fun CategoryFilterBar(
    selectedType: String?,
    onTypeSelected: (String?) -> Unit,
    sections: List<CatalogSection>,
    modifier: Modifier = Modifier,
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
        "channel" to "📡 Channels",
    )

    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = MovieHubDimens.Spacing.lg, vertical = MovieHubDimens.Spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.sm),
    ) {
        item(key = "all") {
            CategoryChip(
                selected = selectedType == null,
                label = "✨ All",
                onClick = { onTypeSelected(null) },
            )
        }
        items(types, key = { it }) { type ->
            CategoryChip(
                selected = selectedType == type,
                label = typeLabels[type] ?: type.replaceFirstChar { it.uppercase() },
                onClick = { onTypeSelected(type) },
            )
        }
    }
}

@Composable
private fun CategoryChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "ChipPressScale"
    )

    Surface(
        shape = RoundedCornerShape(MovieHubDimens.Spacing.xl),
        color = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        contentColor = if (selected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        modifier = modifier
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clip(RoundedCornerShape(MovieHubDimens.Spacing.xl))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .border(
                width = MovieHubDimens.Spacing.dp1,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                shape = RoundedCornerShape(MovieHubDimens.Spacing.xl)
            )
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.padding(horizontal = MovieHubDimens.Spacing.lg, vertical = MovieHubDimens.Spacing.ms),
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
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return

    Column(modifier = modifier.padding(vertical = MovieHubDimens.Spacing.md)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MovieHubDimens.Spacing.lg),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Continue Watching",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "Pick up where you left off",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Spacer(modifier = Modifier.height(MovieHubDimens.Spacing.md))

        val continueListState = rememberLazyListState()
        LazyRow(
            state = continueListState,
            contentPadding = PaddingValues(horizontal = MovieHubDimens.Spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.md),
        ) {
            items(items, key = { item -> item.mediaId }) { item ->
                ContinueWatchingCard(
                    item = item,
                    onResumeClick = { onResumeClick(item) },
                    onMediaClick = { onMediaClick(item) },
                    onDetailsClick = { onDetailsClick(item) },
                    onMarkAsWatched = { onMarkAsWatched(item) },
                    onRemoveFromContinue = { onRemoveFromContinue(item) },
                    modifier = Modifier.width(MovieHubDimens.Poster.continueWatchingWidth),
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
    modifier: Modifier = Modifier,
) {
    val progress = if (item.durationMs > 0) {
        (item.progressMs.toFloat() / item.durationMs).coerceIn(0f, 1f)
    } else {
        0f
    }
    val isFullyWatched = item.durationMs > 0 && progress >= 0.9f
    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "CWPressScale"
    )

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(MovieHubDimens.Poster.continueWatchingHeight)
                .graphicsLayer(scaleX = scale, scaleY = scale)
                .clip(RoundedCornerShape(MovieHubDimens.Spacing.md))
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onMediaClick,
                    onLongClick = { showSheet = true },
                ),
        ) {
            Poster(
                url = item.posterUrl,
                contentDescription = item.title,
                title = item.title,
                modifier = Modifier.fillMaxSize(),
                isWatched = isFullyWatched,
                progressFraction = if (!isFullyWatched) progress else -1f,
            )

            // Gradient overlay at bottom for readability
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(MovieHubDimens.Player.shimmerHeight)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.85f),
                            ),
                        ),
                    ),
            )

            // Play button overlay
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(MovieHubDimens.Icon.xxxl)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable(onClick = onResumeClick),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "▶",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
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
                        .padding(bottom = MovieHubDimens.Spacing.xxxl)
                        .padding(horizontal = MovieHubDimens.Spacing.sm),
                ) {
                    // Poster thumbnail + title header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = MovieHubDimens.Spacing.sm, vertical = MovieHubDimens.Spacing.md),
                        horizontalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.lg),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .width(MovieHubDimens.Poster.bottomSheetThumbnailWidth)
                                .height(MovieHubDimens.Poster.bottomSheetThumbnailHeight)
                                .clip(RoundedCornerShape(MovieHubDimens.Spacing.sm))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
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
                        modifier = Modifier.padding(horizontal = MovieHubDimens.Spacing.lg, vertical = MovieHubDimens.Spacing.xxs),
                    )

                    // Details
                    ContinueWatchingSheetButton(
                        icon = Icons.Default.Info,
                        label = "Details",
                        onClick = {
                            showSheet = false
                            onDetailsClick()
                        },
                    )

                    // Mark as Watched
                    ContinueWatchingSheetButton(
                        icon = Icons.Default.CheckCircle,
                        label = "Mark as Watched",
                        onClick = {
                            showSheet = false
                            onMarkAsWatched()
                        },
                    )

                    // Remove
                    ContinueWatchingSheetButton(
                        icon = Icons.Default.Close,
                        label = "Remove from Continue Watching",
                        onClick = {
                            showSheet = false
                            onRemoveFromContinue()
                        },
                        isDestructive = true,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(MovieHubDimens.Spacing.sm))

        Text(
            text = item.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
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
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun ContinueWatchingSheetButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false,
) {
    val contentColor = if (isDestructive) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(MovieHubDimens.Spacing.md),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.lg),
            modifier = Modifier.padding(horizontal = MovieHubDimens.Spacing.lg, vertical = MovieHubDimens.Spacing.ml),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor.copy(alpha = 0.7f),
                modifier = Modifier.size(MovieHubDimens.Icon.md),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = contentColor,
                modifier = Modifier.weight(1f),
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
    onItemHover: (id: String, type: String, addonId: String?) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier,
) {
    if (mediaItems.isEmpty()) return

    Column(modifier = modifier.padding(vertical = MovieHubDimens.Spacing.lg)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MovieHubDimens.Spacing.lg),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(modifier = Modifier.width(MovieHubDimens.Spacing.md))
            Text(
                text = "SEE ALL",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onSeeAllClick() }.padding(bottom = MovieHubDimens.Spacing.xxs),
                maxLines = 1,
            )
        }

        Spacer(modifier = Modifier.height(MovieHubDimens.Spacing.md))

        val rowListState = rememberLazyListState()
        LazyRow(
            state = rowListState,
            contentPadding = PaddingValues(horizontal = MovieHubDimens.Spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.md),
        ) {
            items(mediaItems, key = { item -> "${item.sourceAddonId ?: ""}_${item.id}" }) { item ->
                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()

                // Fire prewarm after 800ms of continuous press
                LaunchedEffect(isPressed) {
                    if (isPressed) {
                        delay(MovieHubDimens.PrefetchTiming.catalogItemHoverMs)
                        onItemHover(item.id, item.type.stremioType, item.sourceAddonId)
                    }
                }

                Column(
                    modifier = Modifier
                        .width(MovieHubDimens.Poster.homeWidth)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = { onItemClick(item.id, item.type.stremioType, item.sourceAddonUrl) },
                        ),
                    verticalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.xs),
                ) {
                    Poster(
                        url = item.posterUrl,
                        contentDescription = item.title,
                        modifier = Modifier.fillMaxWidth().height(MovieHubDimens.Poster.homeHeight),
                        isWatched = item.id in watchedMediaIds,
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
