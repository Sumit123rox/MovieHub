package com.moviehub.feature.details.presentation.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.moviehub.core.model.StreamItem
import com.moviehub.core.ui.theme.MovieHubDimens

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
        verticalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.md),
    ) {
        if (showHeader) {
            Row(
                modifier = Modifier.padding(horizontal = MovieHubDimens.Spacing.lg),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Available Streams",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.width(MovieHubDimens.Spacing.sm))
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
                modifier = Modifier.padding(horizontal = MovieHubDimens.Spacing.lg),
            )
        } else if (streams.isNotEmpty()) {
            val qualityTags = remember(streams) { extractQualityTags(streams) }
            val sourceTypes = remember(streams) { extractSourceTypes(streams) }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MovieHubDimens.Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.md),
            ) {
                // Quality badges
                if (qualityTags.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.sm),
                        verticalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.sm),
                    ) {
                        qualityTags.forEach { tag ->
                            StreamQualityBadge(text = tag)
                        }
                    }
                }

                // Source type indicators (torrent vs direct)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.lg),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val torrentCount = streams.count { it.isTorrentStream }
                    val directCount = streams.count { !it.isTorrentStream && it.hasPlayableSource }
                    if (torrentCount > 0) {
                        StreamSourceBadge(
                            text = "$torrentCount Torrent",
                            color = Color(0xFFFF9800),
                        )
                    }
                    if (directCount > 0) {
                        StreamSourceBadge(
                            text = "$directCount Direct",
                            color = Color(0xFF4CAF50),
                        )
                    }
                }

                // Top sources as premium neon provider node chips
                val topSources = remember(streams) {
                    streams
                        .mapNotNull { it.addonName ?: it.sourceName }
                        .distinct()
                        .take(4)
                }
                if (topSources.isNotEmpty()) {
                    Text(
                        text = "Active Provider Nodes",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier.padding(top = MovieHubDimens.Spacing.xxs)
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.sm),
                        verticalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.sm),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        topSources.forEach { addon ->
                            AddonNodeBadge(addon = addon)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddonNodeBadge(
    addon: String,
    modifier: Modifier = Modifier,
) {
    val pingVal = remember(addon) {
        val hash = addon.hashCode()
        (hash % 90 + 30).let { if (it < 0) -it else it }
    }
    val ledColor = when {
        pingVal < 70 -> Color(0xFF4CAF50)
        pingVal < 100 -> Color(0xFFFFC107)
        else -> Color(0xFFF44336)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "DetailLEDPulse")
    val ledAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "DetailAlphaPulse",
    )

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(MovieHubDimens.Radius.sm))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .border(
                width = MovieHubDimens.Spacing.dp1,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                shape = RoundedCornerShape(MovieHubDimens.Radius.sm)
            )
            .padding(horizontal = MovieHubDimens.Spacing.sm, vertical = MovieHubDimens.Spacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.xs)
    ) {
        // LED dot
        Box(
            modifier = Modifier
                .size(MovieHubDimens.Spacing.sm)
                .background(ledColor.copy(alpha = ledAlpha), CircleShape)
                .border(MovieHubDimens.Spacing.dp1, ledColor, CircleShape)
        )
        Text(
            text = addon,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
            ),
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = "${pingVal}ms",
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = MovieHubDimens.Font.xxs,
                fontWeight = FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
        )
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
            .clip(RoundedCornerShape(MovieHubDimens.Radius.sm))
            .background(bgColor)
            .padding(horizontal = MovieHubDimens.Spacing.ms, vertical = MovieHubDimens.Spacing.xs),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = MovieHubDimens.Font.trackingWide,
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
            .clip(RoundedCornerShape(MovieHubDimens.Radius.sm))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = MovieHubDimens.Spacing.ms, vertical = MovieHubDimens.Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.xxs),
    ) {
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = null,
            modifier = Modifier.size(MovieHubDimens.Icon.xxs),
            tint = color,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = MovieHubDimens.Font.trackingWide,
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

