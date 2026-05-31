package com.moviehub.feature.profile.presentation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.moviehub.core.model.DownloadItem
import com.moviehub.core.model.DownloadState
import com.moviehub.core.ui.components.EmptyState
import com.moviehub.core.ui.components.MovieHubTopBar
import com.moviehub.core.ui.theme.MovieHubDimens
import com.moviehub.core.ui.theme.MovieHubColors
import org.koin.compose.viewmodel.koinViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    onBackClick: () -> Unit,
    viewModel: DownloadsViewModel = koinViewModel(),
) {
    val downloads by viewModel.downloads.collectAsState()
    val storageInfo by viewModel.storageInfo.collectAsState()

    Scaffold(
        topBar = {
            MovieHubTopBar(
                title = "Downloads",
                onBackClick = onBackClick
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Storage Telemetry Dashboard
            storageInfo?.let { info ->
                Box(
                    modifier = Modifier.padding(
                        horizontal = MovieHubDimens.Spacing.lg,
                        vertical = MovieHubDimens.Spacing.md
                    )
                ) {
                    StorageDashboard(storageInfo = info)
                }
            }

            if (downloads.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    EmptyState(
                        icon = Icons.Default.Download,
                        title = "No Downloads Yet",
                        subtitle = "Streams you save for offline playback will appear here.",
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentPadding = PaddingValues(
                        horizontal = MovieHubDimens.Spacing.lg,
                        vertical = MovieHubDimens.Spacing.sm
                    ),
                    verticalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.md),
                ) {
                    items(downloads, key = { it.id }) { item ->
                        DownloadItemCard(
                            item = item,
                            onCancel = { viewModel.cancelDownload(item) },
                            onPause = { viewModel.pauseDownload(item) },
                            onResume = { viewModel.resumeDownload(item) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StorageDashboard(storageInfo: com.moviehub.core.network.StorageInfo) {
    val usedBytes = storageInfo.totalBytes - storageInfo.freeBytes
    val appProgress = if (storageInfo.totalBytes > 0) storageInfo.appBytes.toFloat() / storageInfo.totalBytes else 0f
    val otherProgress = if (storageInfo.totalBytes > 0) (usedBytes - storageInfo.appBytes).toFloat() / storageInfo.totalBytes else 0f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MovieHubDimens.Radius.md))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(MovieHubDimens.Spacing.lg)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.xs)
        ) {
            Text(
                text = "Device Storage Space",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(2.dp))

            // Multi-segment storage bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(MovieHubDimens.Radius.sm))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            ) {
                // System data progress
                Box(
                    modifier = Modifier
                        .fillMaxWidth(otherProgress.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f))
                )
                // App data progress (MovieHub stack overlay)
                Box(
                    modifier = Modifier
                        .fillMaxWidth((appProgress + otherProgress).coerceIn(0f, 1f))
                        .fillMaxHeight()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(appProgress.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    Text(
                        text = "MovieHub: ${formatBytes(storageInfo.appBytes)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f))
                    )
                    Text(
                        text = "System: ${formatBytes(usedBytes - storageInfo.appBytes)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }

                Text(
                    text = "Free: ${formatBytes(storageInfo.freeBytes)}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun DownloadItemCard(
    item: DownloadItem,
    onCancel: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
) {
    val scale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        ),
        shape = RoundedCornerShape(MovieHubDimens.Radius.md),
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                scope.launch {
                    scale.animateTo(0.97f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
                    scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
                }
            }
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MovieHubDimens.Spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when (item.state) {
                            DownloadState.DOWNLOADING -> "Downloading"
                            DownloadState.COMPLETED -> "Completed"
                            DownloadState.FAILED -> "Failed"
                            DownloadState.PAUSED -> "Paused"
                            DownloadState.QUEUED -> "Queued"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = when (item.state) {
                            DownloadState.COMPLETED -> MovieHubColors.Success
                            DownloadState.FAILED -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.primary
                        }
                    )
                    Text(
                        text = "·",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                    Text(
                        text = formatBytes(item.downloadedSize),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
                
                if (item.state == DownloadState.DOWNLOADING || item.state == DownloadState.PAUSED) {
                    Spacer(modifier = Modifier.height(MovieHubDimens.Spacing.sm))
                    LinearProgressIndicator(
                        progress = { item.progress.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(MovieHubDimens.Radius.sm)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${(item.progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                    )
                }
            }
            Spacer(modifier = Modifier.width(MovieHubDimens.Spacing.md))
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.xs)
            ) {
                // Pause / Play Button
                if (item.state == DownloadState.DOWNLOADING) {
                    IconButton(onClick = onPause, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Pause, contentDescription = "Pause", tint = MaterialTheme.colorScheme.primary)
                    }
                } else if (item.state == DownloadState.PAUSED) {
                    IconButton(onClick = onResume, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Resume", tint = MaterialTheme.colorScheme.primary)
                    }
                }

                IconButton(onClick = onCancel, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f))
                }
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val kb = 1024.0
    val mb = kb * 1024.0
    val gb = mb * 1024.0
    val tb = gb * 1024.0
    return when {
        bytes >= tb -> "${(bytes / tb * 10).toInt() / 10.0} TB"
        bytes >= gb -> "${(bytes / gb * 10).toInt() / 10.0} GB"
        bytes >= mb -> "${(bytes / mb * 10).toInt() / 10.0} MB"
        bytes >= kb -> "${(bytes / kb * 10).toInt() / 10.0} KB"
        else -> "$bytes B"
    }
}
