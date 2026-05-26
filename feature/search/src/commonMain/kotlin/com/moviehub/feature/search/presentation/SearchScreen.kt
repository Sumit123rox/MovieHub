package com.moviehub.feature.search.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.moviehub.core.ui.components.ContentCard
import com.moviehub.core.ui.components.Poster
import com.moviehub.core.ui.components.VerticalGrid
import com.moviehub.core.ui.components.shimmerEffect
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.aspectRatio
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
            ContentCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
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
                    singleLine = true,
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                        onDone = { viewModel.onAction(SearchAction.PerformSearch) },
                        onSearch = { viewModel.onAction(SearchAction.PerformSearch) }
                    ),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        imeAction = androidx.compose.ui.text.input.ImeAction.Search
                    )
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                if (state.isLoading) {
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
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = state.error!!, color = MaterialTheme.colorScheme.error)
                    }
                } else if (state.query.isBlank()) {
                    if (state.recentSearches.isNotEmpty()) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp)
                        ) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Recent Searches",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Clear",
                                        color = MaterialTheme.colorScheme.primary,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.clickable {
                                            viewModel.onAction(SearchAction.ClearSearchHistory)
                                        }
                                    )
                                }
                            }
                            items(state.recentSearches, key = { it }) { query ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.onAction(SearchAction.SelectRecentSearch(query))
                                        }
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = query,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { viewModel.onAction(SearchAction.RemoveSearch(query)) }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove",
                                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(Res.string.search_hint),
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }
}
