package com.moviehub.feature.home.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.moviehub.core.model.MediaItem
import com.moviehub.core.ui.components.Poster
import com.moviehub.core.ui.components.VerticalGrid
import com.moviehub.core.ui.components.shimmerEffect
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.aspectRatio
import com.moviehub.feature.home.data.HomeRepository
import kotlinx.coroutines.launch
import moviehub.core.ui.generated.resources.Res
import moviehub.core.ui.generated.resources.back
import moviehub.core.ui.generated.resources.no_items_found
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogScreen(
    title: String,
    type: String,
    catalogId: String,
    addonId: String?,
    onMediaClick: (id: String, type: String, addonUrl: String?) -> Unit,
    onBackClick: () -> Unit,
    repository: HomeRepository = koinInject()
) {
    var allFetchedItems by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var displayedItems by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isPaginating by remember { mutableStateOf(false) }
    var canPaginate by remember { mutableStateOf(true) }
    
    val pageSize = 15
    val scope = rememberCoroutineScope()

    LaunchedEffect(type, catalogId, addonId) {
        isLoading = true
        val fetched = repository.getCatalog(type, catalogId, addonId, skip = 0)
        allFetchedItems = fetched
        displayedItems = fetched.take(pageSize)
        isLoading = false
        canPaginate = fetched.isNotEmpty()
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
                    containerColor = Color.Black,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color.Black
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues).background(Color.Black)) {
            if (isLoading) {
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
            } else if (displayedItems.isNotEmpty()) {
                VerticalGrid(
                    items = displayedItems,
                    modifier = Modifier.fillMaxSize()
                ) { index, movie ->
                    // Trigger pagination when getting close to the end of currently displayed items
                    if (index >= displayedItems.size - 4 && !isPaginating && canPaginate) {
                        isPaginating = true
                        scope.launch {
                            if (displayedItems.size < allFetchedItems.size) {
                                // Local Pagination: load next chunk of 15 from cache
                                val nextSize = (displayedItems.size + pageSize).coerceAtMost(allFetchedItems.size)
                                displayedItems = allFetchedItems.take(nextSize)
                                isPaginating = false
                            } else {
                                // Network Pagination: load next page from API
                                val nextSkip = allFetchedItems.size
                                val newItems = repository.getCatalog(type, catalogId, addonId, skip = nextSkip)
                                
                                val existingIds = allFetchedItems.map { it.id }.toSet()
                                val uniqueNewItems = newItems.filter { it.id !in existingIds }
                                
                                if (uniqueNewItems.isNotEmpty()) {
                                    allFetchedItems = allFetchedItems + uniqueNewItems
                                    val nextSize = displayedItems.size + pageSize
                                    displayedItems = allFetchedItems.take(nextSize)
                                } else {
                                    canPaginate = false
                                }
                                isPaginating = false
                            }
                        }
                    }
                    
                    Poster(
                        url = movie.posterUrl,
                        contentDescription = movie.title,
                        onClick = { onMediaClick(movie.id, movie.type.stremioType, movie.sourceAddonUrl) }
                    )
                }
                
                if (isPaginating && displayedItems.size == allFetchedItems.size) {
                    Box(modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(16.dp)) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.primary)
                    }
                }
            } else {
                Text(
                    text = stringResource(Res.string.no_items_found),
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }
    }
}
