package com.moviehub.feature.details.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.moviehub.core.model.MediaItem
import com.moviehub.core.model.MediaType

@Composable
fun DetailAdditionalInfoSection(
    media: MediaItem,
    modifier: Modifier = Modifier,
    showHeader: Boolean = true,
) {
    val isSeriesLike = media.type == MediaType.SHOW || media.videos.any { it.season != null || it.episode != null }
    val title = if (isSeriesLike) "Show Details" else "Movie Details"

    val rows = buildList {
        media.status?.let { add("Status" to it) }
        media.releaseInfo?.let { add("Release Info" to it) }
        media.runtime?.let { add("Runtime" to it) }
        media.ageRating?.let { add("Certification" to it) }
        media.country?.let { add("Origin Country" to it) }
        media.language?.let { add("Original Language" to it.uppercase()) }
    }

    if (rows.isEmpty()) return

    DetailSection(
        title = title,
        modifier = modifier,
        showHeader = showHeader,
    ) {
        rows.forEachIndexed { index, (label, value) ->
            DetailInfoRow(
                label = label,
                value = value,
                showDivider = index < rows.lastIndex,
            )
        }
    }
}

@Composable
private fun DetailInfoRow(
    label: String,
    value: String,
    showDivider: Boolean,
) {
    androidx.compose.foundation.layout.Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = value,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.End,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.10f)),
            )
        }
    }
}
