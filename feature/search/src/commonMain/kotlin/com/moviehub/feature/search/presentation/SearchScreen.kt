package com.moviehub.feature.search.presentation

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moviehub.core.model.MediaItem
import com.moviehub.core.model.MediaType
import com.moviehub.core.ui.components.MovieHubTopBar
import com.moviehub.core.ui.components.Poster
import com.moviehub.core.ui.components.SmartStatusBar
import com.moviehub.core.ui.components.VerticalGrid
import com.moviehub.core.ui.components.shimmerEffect
import com.moviehub.core.ui.text.nativeTextFieldImeOptions
import com.moviehub.core.ui.theme.MovieHubDimens
import androidx.compose.ui.graphics.luminance
import moviehub.core.ui.generated.resources.Res
import moviehub.core.ui.generated.resources.search_hint
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalComposeUiApi::class, ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    onMediaClick: (id: String, type: String) -> Unit,
    onBackClick: (() -> Unit)? = null,
    viewModel: SearchViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var isFocused by remember { mutableStateOf(false) }

    var selectedType by rememberSaveable { mutableStateOf("Movies") }
    var selectedSort by rememberSaveable { mutableStateOf("Trending") }
    var selectedGenre by rememberSaveable { mutableStateOf("All Genres") }

    val discoverMovies = remember {
        listOf(
            MediaItem(
                id = "movie:kara",
                title = "Kara",
                posterUrl = "https://image.tmdb.org/t/p/w500/1s6bXfXm0bV8z0J81H9Dlz8sN1p.jpg",
                type = MediaType.MOVIE,
                releaseInfo = "2026",
                genres = listOf("Action", "Drama")
            ),
            MediaItem(
                id = "movie:365days",
                title = "365 dni",
                posterUrl = "https://image.tmdb.org/t/p/w500/6bs56ZCoZ6u729n9Q4Y7tC64g6k.jpg",
                type = MediaType.MOVIE,
                releaseInfo = "2020",
                genres = listOf("Romance", "Drama")
            ),
            MediaItem(
                id = "movie:dhurandhar",
                title = "Dhurandhar",
                posterUrl = "https://image.tmdb.org/t/p/w500/oK6vO9UvF6t4H7dC4M4N2P3O5k8.jpg",
                type = MediaType.MOVIE,
                releaseInfo = "2025",
                genres = listOf("Action", "Thriller")
            ),
            MediaItem(
                id = "movie:dune2",
                title = "Dune: Part Two",
                posterUrl = "https://image.tmdb.org/t/p/w500/czemqn022PndRjUi5xf5YHTHgAB.jpg",
                type = MediaType.MOVIE,
                releaseInfo = "2024",
                genres = listOf("Sci-Fi", "Adventure")
            ),
            MediaItem(
                id = "movie:interstellar",
                title = "Interstellar",
                posterUrl = "https://image.tmdb.org/t/p/w500/gEU2Qv4w3Fg7vTT95mR23Z92qy1.jpg",
                type = MediaType.MOVIE,
                releaseInfo = "2014",
                genres = listOf("Sci-Fi", "Drama")
            ),
            MediaItem(
                id = "movie:batman2022",
                title = "The Batman",
                posterUrl = "https://image.tmdb.org/t/p/w500/74xTEgt7R36Fpo52JbqNaQjqKbq.jpg",
                type = MediaType.MOVIE,
                releaseInfo = "2022",
                genres = listOf("Action", "Crime")
            ),
            MediaItem(
                id = "series:strangerthings",
                title = "Stranger Things",
                posterUrl = "https://image.tmdb.org/t/p/w500/49WJfeN0mHMqj9R7YJ7Bh67m1IB.jpg",
                type = MediaType.SHOW,
                releaseInfo = "2016",
                genres = listOf("Sci-Fi", "Drama", "Thriller")
            ),
            MediaItem(
                id = "series:onepiece",
                title = "One Piece",
                posterUrl = "https://image.tmdb.org/t/p/w500/c54X135UIBMziS9ki2FhRQCgD7x.jpg",
                type = MediaType.SHOW,
                releaseInfo = "2023",
                genres = listOf("Anime", "Action", "Adventure")
            ),
            MediaItem(
                id = "series:breakingbad",
                title = "Breaking Bad",
                posterUrl = "https://image.tmdb.org/t/p/w500/ztkUQvmg167tB4g94PjJ09T4v6s.jpg",
                type = MediaType.SHOW,
                releaseInfo = "2008",
                genres = listOf("Drama", "Crime")
            )
        )
    }

    val displayedItems = remember(state.results, state.query, selectedType, selectedGenre, selectedSort) {
        val baseList = if (state.query.isEmpty()) {
            discoverMovies
        } else {
            state.results
        }

        baseList.filter { item ->
            val matchesType = when (selectedType) {
                "All" -> true
                "Movies" -> item.type == MediaType.MOVIE
                "Shows" -> item.type == MediaType.SHOW
                "Anime" -> item.genres.contains("Anime") || item.title.lowercase().contains("anime") || item.type == MediaType.SHOW
                else -> true
            }

            val matchesGenre = when (selectedGenre) {
                "All Genres" -> true
                else -> item.genres.contains(selectedGenre)
            }

            matchesType && matchesGenre
        }
    }

    val isSystemDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    SmartStatusBar(
        isDark = isSystemDark,
        color = MaterialTheme.colorScheme.background,
    )

    Scaffold(
        topBar = {
            MovieHubTopBar(
                title = "Search",
                onBackClick = onBackClick
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background),
        ) {

            // ═══ Minimalist Borderless Search Bar (inspired by Nuvio) ═══
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = MovieHubDimens.Spacing.lg,
                        end = MovieHubDimens.Spacing.lg,
                        top = MovieHubDimens.Spacing.sm,
                        bottom = MovieHubDimens.Spacing.md,
                    )
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                TextField(
                    value = state.query,
                    onValueChange = { viewModel.onAction(SearchAction.QueryChanged(it)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { isFocused = it.isFocused },
                    placeholder = {
                        Text(
                            text = "Search movies, shows...",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    trailingIcon = {
                        if (state.query.isNotEmpty()) {
                            val clearInteractionSource = remember { MutableInteractionSource() }
                            val isClearPressed by clearInteractionSource.collectIsPressedAsState()
                            val clearScale by animateFloatAsState(
                                targetValue = if (isClearPressed) 0.8f else 1.0f,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                                label = "SearchClearScale"
                            )

                            Box(
                                modifier = Modifier
                                    .graphicsLayer(scaleX = clearScale, scaleY = clearScale)
                                    .clip(CircleShape)
                                    .clickable(
                                        interactionSource = clearInteractionSource,
                                        indication = null,
                                        onClick = { viewModel.onAction(SearchAction.QueryChanged("")) }
                                    )
                                    .padding(MovieHubDimens.Spacing.sm),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    modifier = Modifier.size(MovieHubDimens.Icon.sm),
                                )
                            }
                        }
                    },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    ),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                        onSearch = { viewModel.onAction(SearchAction.PerformSearch) },
                    ),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        imeAction = androidx.compose.ui.text.input.ImeAction.Search,
                        platformImeOptions = nativeTextFieldImeOptions(),
                    ),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                if (state.query.isEmpty()) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = MovieHubDimens.Spacing.lg),
                    ) {
                        // 1. Recent Searches (vertical rows)
                        if (state.recentSearches.isNotEmpty()) {
                            item(key = "recent_header") {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = MovieHubDimens.Spacing.md),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = "Recent Searches",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        text = "Clear all",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.clickable {
                                            viewModel.onAction(SearchAction.ClearSearchHistory)
                                        },
                                    )
                                }
                            }

                            items(state.recentSearches, key = { it }) { query ->
                                RecentSearchRow(
                                    query = query,
                                    onSelect = {
                                        viewModel.onAction(SearchAction.SelectRecentSearch(query))
                                    },
                                    onRemove = {
                                        viewModel.onAction(SearchAction.RemoveSearch(query))
                                    }
                                )
                            }
                        }

                        // 2. Discover Section
                        item(key = "discover_header") {
                            Text(
                                text = "Discover",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 28.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(top = MovieHubDimens.Spacing.lg, bottom = MovieHubDimens.Spacing.sm)
                            )
                        }

                        item(key = "discover_dropdowns") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(vertical = MovieHubDimens.Spacing.xs),
                                horizontalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.sm)
                            ) {
                                DiscoverDropdownPill(
                                    selectedOption = selectedType,
                                    options = listOf("All", "Movies", "Shows", "Anime"),
                                    onOptionSelected = { selectedType = it }
                                )

                                DiscoverDropdownPill(
                                    selectedOption = selectedSort,
                                    options = listOf("Trending", "Popular", "Newest", "Top Rated"),
                                    onOptionSelected = { selectedSort = it }
                                )

                                DiscoverDropdownPill(
                                    selectedOption = selectedGenre,
                                    options = listOf("All Genres", "Action", "Drama", "Romance", "Sci-Fi", "Crime", "Comedy", "Thriller"),
                                    onOptionSelected = { selectedGenre = it }
                                )
                            }
                        }

                        item(key = "discover_source_label") {
                            Text(
                                text = "IndiaStreams • $selectedType",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                modifier = Modifier.padding(top = MovieHubDimens.Spacing.xs, bottom = MovieHubDimens.Spacing.md)
                            )
                        }

                        // Chunked discover items grid
                        items(displayedItems.chunked(3), key = { row -> row.firstOrNull()?.id ?: row.hashCode() }) { rowItems ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = MovieHubDimens.Spacing.xs),
                                horizontalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.sm)
                            ) {
                                rowItems.forEach { movie ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(bottom = MovieHubDimens.Spacing.md)
                                    ) {
                                        Column {
                                            Poster(
                                                url = movie.posterUrl,
                                                contentDescription = movie.title,
                                                onClick = { onMediaClick(movie.id, movie.type.name.lowercase()) },
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = movie.title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = movie.releaseInfo ?: "",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                                repeat(3 - rowItems.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                } else if (state.isLoading) {
                    // ═══ Loading shimmer ═══
                    VerticalGrid(
                        items = List(9) { "" },
                        modifier = Modifier.fillMaxSize(),
                    ) { _, _ ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(MovieHubDimens.Poster.aspectRatio)
                                .clip(RoundedCornerShape(MovieHubDimens.Radius.sm))
                                .shimmerEffect(),
                        )
                    }
                } else if (state.suggestions.isNotEmpty() && state.results.isEmpty()) {
                    // ═══ Suggestions list ═══
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                start = MovieHubDimens.Spacing.lg,
                                end = MovieHubDimens.Spacing.lg,
                                top = MovieHubDimens.Spacing.sm,
                                bottom = MovieHubDimens.Spacing.sm
                            ),
                        verticalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.sm)
                    ) {
                        items(state.suggestions, key = { it }) { suggestion ->
                            SuggestionItem(
                                suggestion = suggestion,
                                onClick = {
                                    viewModel.onAction(SearchAction.SelectSuggestion(suggestion))
                                },
                                onFillQuery = {
                                    viewModel.onAction(SearchAction.QueryChanged(suggestion))
                                }
                            )
                        }
                    }
                } else if (state.results.isNotEmpty()) {
                    // ═══ Results grid with Nuvio stacked style ═══
                    VerticalGrid(
                        items = displayedItems,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = MovieHubDimens.Spacing.lg, vertical = MovieHubDimens.Spacing.md)
                    ) { index, movie ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = MovieHubDimens.Spacing.sm)
                        ) {
                            Poster(
                                url = movie.posterUrl,
                                contentDescription = movie.title,
                                onClick = { onMediaClick(movie.id, movie.type.name.lowercase()) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = movie.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            Text(
                                text = movie.releaseInfo ?: movie.releaseDate?.take(4) ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                maxLines = 1
                            )
                        }
                    }
                } else if (state.error != null) {
                    // ═══ Error state ═══
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                                modifier = Modifier.size(MovieHubDimens.Icon.jumbo),
                            )
                            Spacer(modifier = Modifier.height(MovieHubDimens.Spacing.lg))
                            Text(
                                text = state.error ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = MovieHubDimens.Spacing.xxxl),
                            )
                        }
                    }
                } else {
                    // Search was performed but returned empty
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                                modifier = Modifier.size(MovieHubDimens.Icon.jumbo),
                            )
                            Spacer(modifier = Modifier.height(MovieHubDimens.Spacing.lg))
                            Text(
                                text = "No results found for \"" + state.query + "\"",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentSearchRow(
    query: String,
    onSelect: () -> Unit,
    onRemove: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "RecentSearchRowScale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onSelect
            )
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(MovieHubDimens.Spacing.md))
        Text(
            text = query,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Remove recent search",
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            modifier = Modifier
                .size(MovieHubDimens.Icon.sm)
                .clickable { onRemove() }
                .padding(MovieHubDimens.Spacing.xs)
        )
    }
}

@Composable
private fun DiscoverDropdownPill(
    selectedOption: String,
    options: List<String>,
    onOptionSelected: (String) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    Box {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                .clickable { isExpanded = true }
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = when (selectedOption) {
                    "All Genres" -> "All Genres"
                    "Trending" -> "Trending movies in India"
                    else -> selectedOption
                },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp)
            )
        }

        DropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false },
            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = if (option == "Trending") "Trending movies in India" else option,
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    onClick = {
                        onOptionSelected(option)
                        isExpanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun SuggestionItem(
    suggestion: String,
    onClick: () -> Unit,
    onFillQuery: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "SuggestionItemScale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clip(RoundedCornerShape(MovieHubDimens.Radius.md))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = if (isPressed) 0.12f else 0.04f))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(
                horizontal = MovieHubDimens.Spacing.lg,
                vertical = MovieHubDimens.Spacing.ml
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                modifier = Modifier.size(MovieHubDimens.Icon.sm)
            )
            Spacer(modifier = Modifier.width(MovieHubDimens.Spacing.md))
            Text(
                text = suggestion,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
        }

        val arrowInteractionSource = remember { MutableInteractionSource() }
        val isArrowPressed by arrowInteractionSource.collectIsPressedAsState()
        val arrowScale by animateFloatAsState(
            targetValue = if (isArrowPressed) 0.8f else 1.0f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
            label = "SuggestionArrowScale"
        )

        Box(
            modifier = Modifier
                .graphicsLayer(scaleX = arrowScale, scaleY = arrowScale)
                .clip(CircleShape)
                .clickable(
                    interactionSource = arrowInteractionSource,
                    indication = null,
                    onClick = onFillQuery
                )
                .padding(MovieHubDimens.Spacing.sm)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Fill Query",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(MovieHubDimens.Icon.sm)
            )
        }
    }
}


