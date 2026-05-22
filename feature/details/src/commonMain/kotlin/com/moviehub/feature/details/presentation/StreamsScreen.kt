package com.moviehub.feature.details.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moviehub.core.model.StreamItem
import com.moviehub.core.ui.components.GlassyBox
import com.moviehub.core.ui.components.TechnicalBadge
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun StreamDiscoveryIndicator(processed: Int, total: Int) {
    GlassyBox(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    androidx.compose.material.icons.Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Global Stream Discovery",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { if (total > 0) processed.toFloat() / total else 0f },
                modifier = Modifier.fillMaxWidth().height(4.dp).background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(2.dp)),
                color = MaterialTheme.colorScheme.primary,
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Searching $processed of $total addons...",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun StreamsScreen(
    id: String,
    type: String,
    mediaId: String? = null,
    onPlayClick: (stream: StreamItem) -> Unit,
    onBackClick: () -> Unit,
    viewModel: DetailsViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(id, type, mediaId) {
        viewModel.onAction(DetailsAction.LoadDetails(mediaId ?: id, type, null))
        viewModel.onAction(DetailsAction.LoadStreams(id, type))
    }

    Scaffold(
        containerColor = Color.Black,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            // Background Backdrop
            state.mediaItem?.backgroundUrl?.let { backdrop ->
                KamelImage(
                    resource = { asyncPainterResource(data = backdrop) },
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    onLoading = { Box(Modifier.fillMaxSize()) }
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Black.copy(alpha = 0.7f),
                                    Color.Black
                                )
                            )
                        )
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.background(Color.White.copy(alpha = 0.1f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Choose Stream",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        state.mediaItem?.title?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.6f),
                                maxLines = 1
                            )
                        }
                    }
                }

                if (state.isSearchingStreams && state.streams.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Searching for streams...", color = Color.White.copy(alpha = 0.7f))
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 8.dp,
                            bottom = paddingValues.calculateBottomPadding() + 16.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (state.isSearchingStreams) {
                            item {
                                StreamDiscoveryIndicator(
                                    processed = state.processedStreamAddons,
                                    total = state.totalStreamAddons
                                )
                            }
                        }

                        items(state.streams) { stream ->
                            StreamItemCard(
                                stream = stream,
                                onClick = { onPlayClick(stream) }
                            )
                        }

                        if (state.streams.isEmpty() && !state.isSearchingStreams) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 100.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No streams found.\nTry adding more addons.",
                                        color = Color.White.copy(alpha = 0.5f),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StreamItemCard(
    stream: StreamItem,
    onClick: () -> Unit
) {
    GlassyBox(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Flag/Region Indicator
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = getFlagEmoji(stream.name ?: stream.description),
                    fontSize = 20.sp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stream.name ?: "Unknown Source",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                stream.addonName?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    extractTechnicalSpecs(stream.description).forEach { spec ->
                        TechnicalBadge(spec)
                    }
                }
            }

            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

fun getFlagEmoji(text: String?): String {
    if (text == null) return "🌐"
    val t = text.uppercase()
    return when {
        t.contains("INDIA") || t.contains(" IN ") || t.contains("[IN]") -> "🇮🇳"
        t.contains("USA") || t.contains(" US ") || t.contains("[US]") || t.contains("UNITED STATES") -> "🇺🇸"
        t.contains("UK") || t.contains("UNITED KINGDOM") -> "🇬🇧"
        t.contains("FR") || t.contains("FRANCE") -> "🇫🇷"
        t.contains("ES") || t.contains("SPAIN") -> "🇪🇸"
        t.contains("DE") || t.contains("GERMANY") -> "🇩🇪"
        t.contains("IT") || t.contains("ITALY") -> "🇮🇹"
        t.contains("BR") || t.contains("BRAZIL") -> "🇧🇷"
        else -> "🌐"
    }
}

private fun extractTechnicalSpecs(title: String?): List<String> {
    if (title == null) return emptyList()
    val specs = mutableListOf<String>()
    val lowerTitle = title.lowercase()
    
    if (lowerTitle.contains("4k") || lowerTitle.contains("2160p") || lowerTitle.contains("uhd")) specs.add("4K")
    if (lowerTitle.contains("1080p") || lowerTitle.contains("fhd")) specs.add("1080P")
    if (lowerTitle.contains("720p") || lowerTitle.contains("hd")) if (!specs.contains("4K") && !specs.contains("1080P")) specs.add("720P")
    
    if (lowerTitle.contains("hdr")) specs.add("HDR")
    if (lowerTitle.contains("dv") || lowerTitle.contains("dolby vision")) specs.add("DV")
    
    if (lowerTitle.contains("dts")) specs.add("DTS")
    if (lowerTitle.contains("atmos")) specs.add("ATMOS")
    if (lowerTitle.contains("5.1")) specs.add("5.1")
    if (lowerTitle.contains("7.1")) specs.add("7.1")
    
    if (lowerTitle.contains("hevc") || lowerTitle.contains("x265")) specs.add("HEVC")
    
    return specs
}
