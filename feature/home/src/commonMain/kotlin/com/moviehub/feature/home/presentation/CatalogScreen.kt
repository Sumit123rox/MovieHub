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
    var items by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isPaginating by remember { mutableStateOf(false) }
    var skip by remember { mutableStateOf(0) }
    var canPaginate by remember { mutableStateOf(true) }
    
    val scope = rememberCoroutineScope()

    LaunchedEffect(type, catalogId, addonId) {
        isLoading = true
        skip = 0
        items = repository.getCatalog(type, catalogId, addonId, skip = 0)
        isLoading = false
        canPaginate = items.size >= 20 
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
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.primary)
            } else if (items.isNotEmpty()) {
                VerticalGrid(
                    items = items,
                    modifier = Modifier.fillMaxSize()
                ) { index, movie ->
                    // Simple pagination trigger
                    if (index >= items.size - 4 && !isPaginating && canPaginate) {
                        isPaginating = true
                        scope.launch {
                            val nextSkip = skip + 20 
                            val newItems = repository.getCatalog(type, catalogId, addonId, skip = nextSkip)
                            
                            // Check if we actually got new unique items
                            val existingIds = items.map { it.id }.toSet()
                            val uniqueNewItems = newItems.filter { it.id !in existingIds }
                            
                            if (uniqueNewItems.isNotEmpty()) {
                                items = items + uniqueNewItems
                                skip = nextSkip
                            } else {
                                canPaginate = false
                            }
                            isPaginating = false
                        }
                    }
                    
                    Poster(
                        url = movie.posterUrl,
                        contentDescription = movie.title,
                        onClick = { onMediaClick(movie.id, movie.type.stremioType, movie.sourceAddonUrl) }
                    )
                }
                
                if (isPaginating) {
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
