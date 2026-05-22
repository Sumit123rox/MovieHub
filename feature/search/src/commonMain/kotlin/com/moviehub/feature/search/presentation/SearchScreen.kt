package com.moviehub.feature.search.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.moviehub.core.ui.components.GlassyBox
import com.moviehub.core.ui.components.Poster
import com.moviehub.core.ui.components.VerticalGrid
import moviehub.core.ui.generated.resources.Res
import moviehub.core.ui.generated.resources.search_hint
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SearchScreen(
    onMediaClick: (id: String, type: String) -> Unit,
    viewModel: SearchViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Glassy Search Bar
            GlassyBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                blurRadius = 8.dp
            ) {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = { viewModel.onAction(SearchAction.QueryChanged(it)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    placeholder = { 
                        Text(
                            text = stringResource(Res.string.search_hint),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) 
                        ) 
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    singleLine = true
                )
            }

            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (state.isLoading) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                } else if (state.results.isNotEmpty()) {
                    VerticalGrid(
                        items = state.results,
                        modifier = Modifier.fillMaxSize()
                    ) { index, movie ->
                        Poster(
                            url = movie.posterUrl,
                            contentDescription = movie.title,
                            quality = when {
                                index % 3 == 0 -> "4K"
                                index % 5 == 0 -> "HD"
                                else -> null
                            },
                            onClick = { onMediaClick(movie.id, movie.type.name.lowercase()) }
                        )
                    }
                } else if (state.error != null) {
                    Text(text = state.error!!, color = MaterialTheme.colorScheme.error)
                } else if (state.query.isBlank()) {
                    Text(
                        text = stringResource(Res.string.search_hint),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}
