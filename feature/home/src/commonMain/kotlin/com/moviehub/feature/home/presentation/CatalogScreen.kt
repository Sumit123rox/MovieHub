package com.moviehub.feature.home.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.moviehub.core.model.MediaItemStore
import com.moviehub.core.ui.components.Poster
import com.moviehub.core.ui.components.VerticalGrid
import com.moviehub.core.ui.components.shimmerEffect
import moviehub.core.ui.generated.resources.Res
import moviehub.core.ui.generated.resources.back
import moviehub.core.ui.generated.resources.no_items_found
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogScreen(
    title: String,
    type: String,
    catalogId: String,
    addonId: String?,
    onMediaClick: (id: String, type: String, addonUrl: String?) -> Unit,
    onBackClick: () -> Unit,
    viewModel: CatalogViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(type, catalogId, addonId) {
        viewModel.loadCatalog(type, catalogId, addonId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues).background(MaterialTheme.colorScheme.background)) {
            when {
                state.isLoading -> {
                    VerticalGrid(
                        items = List(9) { "" },
                        modifier = Modifier.fillMaxSize()
                    ) { _, _ ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(0.67f)
                                .clip(RoundedCornerShape(8.dp))
                                .shimmerEffect()
                        )
                    }
                }
                state.error != null && state.displayedItems.isEmpty() -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(Res.string.no_items_found),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        state.error?.let { errorMsg ->
                            Text(
                                text = errorMsg,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
                state.displayedItems.isNotEmpty() -> {
                    VerticalGrid(
                        items = state.displayedItems,
                        modifier = Modifier.fillMaxSize()
                    ) { index, movie ->
                        if (index >= state.displayedItems.size - 4 && !state.isPaginating && state.canPaginate) {
                            viewModel.loadMore(type, catalogId, addonId)
                        }

                        Poster(
                            url = movie.posterUrl,
                            contentDescription = movie.title,
                            isWatched = movie.id in state.watchedMediaIds,
                            onClick = {
                                MediaItemStore.put(movie.id, movie)
                                onMediaClick(movie.id, movie.type.stremioType, movie.sourceAddonUrl)
                            }
                        )
                    }

                    if (state.isPaginating) {
                        Box(modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(16.dp)) {
                            CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.Center),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                else -> {
                    Text(
                        text = stringResource(Res.string.no_items_found),
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}
