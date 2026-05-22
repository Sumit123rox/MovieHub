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
import com.moviehub.core.ui.components.Poster
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onMediaClick: (id: String, type: String, addonUrl: String?) -> Unit,
    onAuthClick: () -> Unit,
    onSeeAllClick: (title: String, type: String, catalogId: String, addonId: String?) -> Unit,
    onSearchClick: () -> Unit,
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
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(650.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
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
            items(items, key = { it.id }) { item ->
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
