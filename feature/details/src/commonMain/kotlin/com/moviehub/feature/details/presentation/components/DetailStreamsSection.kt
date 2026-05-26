package com.moviehub.feature.details.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moviehub.core.model.StreamItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetailStreamsSection(
    streams: List<StreamItem>,
    isSearching: Boolean,
    modifier: Modifier = Modifier,
    showHeader: Boolean = true,
) {
    if (streams.isEmpty() && !isSearching) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (showHeader) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Available Streams",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.width(8.dp))
                if (!isSearching && streams.isNotEmpty()) {
                    Text(
                        text = "(${streams.size})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }

        if (isSearching) {
            Text(
                text = "Searching for streams...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        } else if (streams.isNotEmpty()) {
            val qualityTags = remember(streams) { extractQualityTags(streams) }
            val sourceTypes = remember(streams) { extractSourceTypes(streams) }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Quality badges
                if (qualityTags.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        qualityTags.forEach { tag ->
                            StreamQualityBadge(text = tag)
                        }
                    }
                }

                // Source type indicators (torrent vs direct)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val torrentCount = streams.count { it.isTorrentStream }
                    val directCount = streams.count { !it.isTorrentStream && it.hasPlayableSource }
                    if (torrentCount > 0) {
                        StreamSourceBadge(
                            text = "$torrentCount Torrent",
                            color = Color(0xFFFF9800)
                        )
                    }
                    if (directCount > 0) {
                        StreamSourceBadge(
                            text = "$directCount Direct",
                            color = Color(0xFF4CAF50)
                        )
                    }
                }

                // Top sources
                val topSources = remember(streams) {
                    streams
                        .mapNotNull { it.addonName ?: it.sourceName }
                        .distinct()
                        .take(4)
                }
                if (topSources.isNotEmpty()) {
                    Text(
                        text = "via ${topSources.joinToString(", ")}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                        maxLines = 2,
                    )
                }
            }
        }
    }
}

@Composable
private fun StreamQualityBadge(
    text: String,
    modifier: Modifier = Modifier,
) {
    val bgColor = when {
        text.contains("4K", ignoreCase = true) || text.contains("2160", ignoreCase = true) -> Color(0xFFFFD700).copy(alpha = 0.15f)
        text.contains("1080", ignoreCase = true) || text.contains("FHD", ignoreCase = true) -> Color(0xFF4CAF50).copy(alpha = 0.15f)
        text.contains("720", ignoreCase = true) || text.contains("HD", ignoreCase = true) -> Color(0xFF2196F3).copy(alpha = 0.15f)
        text.contains("HDR", ignoreCase = true) || text.contains("DV", ignoreCase = true) -> Color(0xFF9C27B0).copy(alpha = 0.15f)
        else -> Color.White.copy(alpha = 0.08f)
    }
    val textColor = when {
        text.contains("4K", ignoreCase = true) || text.contains("2160", ignoreCase = true) -> Color(0xFFFFD700)
        text.contains("1080", ignoreCase = true) || text.contains("FHD", ignoreCase = true) -> Color(0xFF4CAF50)
        text.contains("720", ignoreCase = true) || text.contains("HD", ignoreCase = true) -> Color(0xFF2196F3)
        text.contains("HDR", ignoreCase = true) || text.contains("DV", ignoreCase = true) -> Color(0xFFCE93D8)
        else -> Color.White.copy(alpha = 0.7f)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.3.sp
            ),
            color = textColor,
        )
    }
}

@Composable
private fun StreamSourceBadge(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = null,
            modifier = Modifier.size(12.dp),
            tint = color,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.3.sp
            ),
            color = color,
        )
    }
}

/**
 * Extracts unique quality tags from stream names/titles.
 * Parses common resolution and format indicators.
 */
private fun extractQualityTags(streams: List<StreamItem>): List<String> {
    val qualities = linkedSetOf<String>()
    val allText = streams.mapNotNull { it.name ?: it.description }.joinToString(" ")

    // Priority-ordered checks
    if ("4K".toRegex().containsMatchIn(allText) || "2160".toRegex().containsMatchIn(allText)) {
        qualities.add("4K")
    }
    if ("1080".toRegex().containsMatchIn(allText) || "FHD".toRegex().containsMatchIn(allText)) {
        qualities.add("1080p")
    }
    if ("720".toRegex().containsMatchIn(allText)) {
        qualities.add("720p")
    }
    if ("480".toRegex().containsMatchIn(allText)) {
        qualities.add("480p")
    }
    if ("HDR".toRegex().containsMatchIn(allText) || "HDR10".toRegex().containsMatchIn(allText)) {
        qualities.add("HDR")
    }
    if ("Dolby Vision".toRegex().containsMatchIn(allText) || "DV".toRegex().containsMatchIn(allText)) {
        qualities.add("Dolby Vision")
    }

    return qualities.toList()
}

/**
 * Extracts unique source type identifiers from streams.
 */
private fun extractSourceTypes(streams: List<StreamItem>): List<String> {
    val types = linkedSetOf<String>()
    val hasTorrent = streams.any { it.isTorrentStream }
    val hasDirect = streams.any { !it.isTorrentStream && it.url != null }
    val hasExternal = streams.any { it.externalUrl != null }

    if (hasTorrent) types.add("Torrent")
    if (hasDirect) types.add("Direct")
    if (hasExternal) types.add("External")

    return types.toList()
}

