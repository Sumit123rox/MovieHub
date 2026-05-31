package com.moviehub.feature.profile.presentation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.moviehub.core.database.ContentType
import com.moviehub.core.database.FavoriteDao
import com.moviehub.core.database.CustomCollectionDao
import com.moviehub.core.database.CustomCollectionEntity
import com.moviehub.core.database.CollectionItemEntity
import com.moviehub.core.database.ProfileRepository
import com.moviehub.core.ui.components.EmptyState
import com.moviehub.core.ui.components.MovieHubTopBar
import com.moviehub.core.ui.components.Poster
import com.moviehub.core.ui.theme.MovieHubDimens
import com.moviehub.core.ui.theme.MovieHubColors
import com.moviehub.core.utils.currentTimeMillis
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onMediaClick: (id: String, type: String) -> Unit,
    onBackClick: () -> Unit = {},
    favoriteDao: FavoriteDao = koinInject(),
    customCollectionDao: CustomCollectionDao = koinInject(),
    profileRepository: ProfileRepository = koinInject(),
) {
    val activeProfile by profileRepository.activeProfile.collectAsState()
    val profileId = activeProfile?.id
    val scope = rememberCoroutineScope()

    // ── Data Loading ────────────────────────────────────────────────
    val favorites by remember(profileId) {
        if (profileId != null) {
            favoriteDao.getAllFavorites(profileId)
        } else {
            flowOf(emptyList())
        }
    }.collectAsState(initial = emptyList())

    val collections by remember(profileId) {
        if (profileId != null) {
            customCollectionDao.getCollections(profileId)
        } else {
            flowOf(emptyList())
        }
    }.collectAsState(initial = emptyList())

    var selectedCollectionId by remember { mutableStateOf<String?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var collectionToDelete by remember { mutableStateOf<CustomCollectionEntity?>(null) }

    val collectionItems by remember(selectedCollectionId, profileId) {
        if (profileId != null && selectedCollectionId != null) {
            customCollectionDao.getCollectionItems(selectedCollectionId!!, profileId)
        } else {
            flowOf(emptyList())
        }
    }.collectAsState(initial = emptyList())

    // ── Mapping items to common UI model ────────────────────────────
    val displayItems = remember(selectedCollectionId, favorites, collectionItems) {
        if (selectedCollectionId == null) {
            favorites.map {
                LibraryDisplayItem(
                    id = it.contentId,
                    title = it.title,
                    posterUrl = it.posterUrl,
                    contentType = it.contentType
                )
            }
        } else {
            collectionItems.map {
                LibraryDisplayItem(
                    id = it.contentId,
                    title = it.title,
                    posterUrl = it.posterUrl,
                    contentType = it.contentType
                )
            }
        }
    }

    Scaffold(
        topBar = {
            MovieHubTopBar(title = "Library")
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.md)
        ) {
            // Horizontal Capsule List Selector
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = MovieHubDimens.Spacing.xs),
                horizontalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.sm),
                contentPadding = PaddingValues(horizontal = MovieHubDimens.Spacing.lg)
            ) {
                item {
                    LibraryFilterCapsule(
                        label = "Favorites",
                        isSelected = selectedCollectionId == null,
                        onClick = { selectedCollectionId = null }
                    )
                }

                items(collections, key = { it.id }) { col ->
                    LibraryFilterCapsule(
                        label = col.name,
                        isSelected = selectedCollectionId == col.id,
                        onClick = { selectedCollectionId = col.id },
                        onLongClick = { collectionToDelete = col }
                    )
                }

                item {
                    LibraryFilterCapsule(
                        label = "+ Create List",
                        isSelected = false,
                        onClick = { showCreateDialog = true },
                        isAccent = true
                    )
                }
            }

            if (displayItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    EmptyState(
                        icon = Icons.Default.Bookmark,
                        title = if (selectedCollectionId == null) "Nothing saved yet" else "Empty Collection",
                        subtitle = if (selectedCollectionId == null) {
                            "Save movies & shows from their detail page to curate your favorites."
                        } else {
                            "Add movies or shows to this collection from their stream pages."
                        },
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 110.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentPadding = PaddingValues(
                        start = MovieHubDimens.Spacing.lg,
                        end = MovieHubDimens.Spacing.lg,
                        bottom = MovieHubDimens.Spacing.lg
                    ),
                    horizontalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.md),
                    verticalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.lg),
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            text = "${if (selectedCollectionId == null) "Favorites" else "Curated items"} (${displayItems.size})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.padding(bottom = MovieHubDimens.Spacing.xxs),
                        )
                    }

                    items(
                        items = displayItems,
                        key = { it.id },
                    ) { item ->
                        val contentType = when (item.contentType) {
                            ContentType.MOVIE -> "movie"
                            ContentType.SHOW -> "series"
                        }
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.xs),
                        ) {
                            Poster(
                                url = item.posterUrl,
                                contentDescription = item.title,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(0.67f),
                                onClick = { onMediaClick(item.id, contentType) },
                            )
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }

        // ── Dialog Overlays ──────────────────────────────────────────

        if (showCreateDialog) {
            var listName by remember { mutableStateOf("") }
            var listDesc by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = { showCreateDialog = false },
                title = { Text("Create New List", fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.md)
                    ) {
                        OutlinedTextField(
                            value = listName,
                            onValueChange = { listName = it },
                            label = { Text("List Name") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = listDesc,
                            onValueChange = { listDesc = it },
                            label = { Text("Description (Optional)") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (listName.isNotBlank() && profileId != null) {
                                scope.launch {
                                    val newCol = CustomCollectionEntity(
                                        id = "col_${currentTimeMillis()}",
                                        profileId = profileId,
                                        name = listName.trim(),
                                        description = listDesc.trim()
                                    )
                                    customCollectionDao.insertCollection(newCol)
                                    showCreateDialog = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Create")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateDialog = false }) {
                        Text("Cancel")
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(MovieHubDimens.Radius.lg)
            )
        }

        val deletingCol = collectionToDelete
        if (deletingCol != null) {
            AlertDialog(
                onDismissRequest = { collectionToDelete = null },
                title = { Text("Delete List", fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        text = "Are you sure you want to delete the list \"${deletingCol.name}\"? This action cannot be undone.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (profileId != null) {
                                scope.launch {
                                    customCollectionDao.deleteCollectionById(deletingCol.id, profileId)
                                    customCollectionDao.clearCollectionItems(deletingCol.id, profileId)
                                    if (selectedCollectionId == deletingCol.id) {
                                        selectedCollectionId = null
                                    }
                                    collectionToDelete = null
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { collectionToDelete = null }) {
                        Text("Cancel")
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(MovieHubDimens.Radius.lg)
            )
        }
    }
}

data class LibraryDisplayItem(
    val id: String,
    val title: String,
    val posterUrl: String?,
    val contentType: ContentType
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryFilterCapsule(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    isAccent: Boolean = false
) {
    val scale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(
                when {
                    isSelected -> MaterialTheme.colorScheme.primary
                    isAccent -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                }
            )
            .combinedClickable(
                onClick = {
                    scope.launch {
                        scale.animateTo(0.95f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
                        scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
                        onClick()
                    }
                },
                onLongClick = onLongClick
            )
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            .padding(
                horizontal = MovieHubDimens.Spacing.lg,
                vertical = MovieHubDimens.Spacing.sm
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = when {
                isSelected -> MaterialTheme.colorScheme.onPrimary
                isAccent -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            }
        )
    }
}
