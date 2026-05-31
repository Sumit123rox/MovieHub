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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.moviehub.core.ui.components.Poster
import com.moviehub.core.ui.components.SmartStatusBar
import com.moviehub.core.ui.components.VerticalGrid
import com.moviehub.core.ui.components.shimmerEffect
import com.moviehub.core.ui.text.nativeTextFieldImeOptions
import com.moviehub.core.ui.theme.MovieHubDimens
import moviehub.core.ui.generated.resources.Res
import moviehub.core.ui.generated.resources.search_hint
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalComposeUiApi::class, ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    onMediaClick: (id: String, type: String) -> Unit,
    viewModel: SearchViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var isFocused by remember { mutableStateOf(false) }

    val borderGlowAlpha by animateFloatAsState(
        targetValue = if (isFocused) 0.8f else 0.15f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "SearchBorderGlowAlpha"
    )
    val borderGlowThickness by animateDpAsState(
        targetValue = if (isFocused) MovieHubDimens.Spacing.dp2 else MovieHubDimens.Spacing.dp1,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "SearchBorderGlowThickness"
    )

    SmartStatusBar(
        isDark = true,
        color = MaterialTheme.colorScheme.background,
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background),
        ) {
            // ═══ Glowing Gen-Z focused search bar ═══
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = MovieHubDimens.Spacing.lg,
                        end = MovieHubDimens.Spacing.lg,
                        top = MovieHubDimens.Spacing.md,
                        bottom = MovieHubDimens.Spacing.sm,
                    )
                    .clip(RoundedCornerShape(MovieHubDimens.Radius.md))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    .border(
                        width = borderGlowThickness,
                        color = if (isFocused) {
                            MaterialTheme.colorScheme.primary.copy(alpha = borderGlowAlpha)
                        } else {
                            MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f)
                        },
                        shape = RoundedCornerShape(MovieHubDimens.Radius.md)
                    ),
            ) {
                TextField(
                    value = state.query,
                    onValueChange = { viewModel.onAction(SearchAction.QueryChanged(it)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { isFocused = it.isFocused },
                    placeholder = {
                        Text(
                            text = stringResource(Res.string.search_hint),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = if (isFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            modifier = Modifier.size(MovieHubDimens.Icon.md),
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
                // ═══ Loading shimmer ═══
                if (state.isLoading) {
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
                }

                // ═══ Results grid ═══
                if (!state.isLoading && state.results.isNotEmpty()) {
                    VerticalGrid(
                        items = state.results,
                        modifier = Modifier.fillMaxSize(),
                    ) { index, movie ->
                        Poster(
                            url = movie.posterUrl,
                            contentDescription = movie.title,
                            quality = when {
                                index % 3 == 0 -> "4K"
                                index % 5 == 0 -> "HD"
                                else -> null
                            },
                            onClick = { onMediaClick(movie.id, movie.type.name.lowercase()) },
                        )
                    }
                }

                // ═══ Error state ═══
                if (!state.isLoading && state.error != null && state.results.isEmpty()) {
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
                }

                // ═══ Recent searches / idle state ═══
                if (!state.isLoading && state.results.isEmpty() && state.error == null) {
                    if (state.recentSearches.isNotEmpty()) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = MovieHubDimens.Spacing.lg),
                        ) {
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
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
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
                            item(key = "recent_flow") {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.sm),
                                    verticalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.sm),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = MovieHubDimens.Spacing.xs)
                                ) {
                                    state.recentSearches.forEach { query ->
                                        RecentSearchCapsule(
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
                            }
                        }
                    } else {
                        // Idle state — no query, no recent searches
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
                                    text = stringResource(Res.string.search_hint),
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
}

@Composable
private fun RecentSearchCapsule(
    query: String,
    onSelect: () -> Unit,
    onRemove: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "RecentSearchCapsuleScale"
    )

    val removeInteractionSource = remember { MutableInteractionSource() }
    val isRemovePressed by removeInteractionSource.collectIsPressedAsState()
    val removeScale by animateFloatAsState(
        targetValue = if (isRemovePressed) 0.8f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "RecentSearchRemoveScale"
    )

    Row(
        modifier = Modifier
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clip(RoundedCornerShape(MovieHubDimens.Radius.xl))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .border(
                width = MovieHubDimens.Spacing.dp1,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                shape = RoundedCornerShape(MovieHubDimens.Radius.xl)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onSelect
            )
            .padding(start = MovieHubDimens.Spacing.md, end = MovieHubDimens.Spacing.sm, top = MovieHubDimens.Spacing.xs, bottom = MovieHubDimens.Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.xxs)
    ) {
        Text(
            text = query,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Box(
            modifier = Modifier
                .graphicsLayer(scaleX = removeScale, scaleY = removeScale)
                .clip(CircleShape)
                .clickable(
                    interactionSource = removeInteractionSource,
                    indication = null,
                    onClick = onRemove
                )
                .padding(MovieHubDimens.Spacing.xxs)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove recent search",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(MovieHubDimens.Icon.xs)
            )
        }
    }
}

