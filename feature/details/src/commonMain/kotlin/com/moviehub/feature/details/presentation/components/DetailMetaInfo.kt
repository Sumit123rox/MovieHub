package com.moviehub.feature.details.presentation.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moviehub.core.model.MediaItem
import com.moviehub.core.model.MediaType

@Composable
fun DetailMetaInfo(
    media: MediaItem,
    modifier: Modifier = Modifier,
) {
    val releaseYear = media.releaseInfo?.take(4)
    val runtimeText = formatRuntime(media.runtime)
    val ageBadge = media.ageRating?.trim()?.takeIf { it.isNotBlank() }
    val rating = media.rating

    // Series summary: number of seasons & episodes
    val seriesSummary = if (media.type == MediaType.SHOW) {
        val seasons = media.videos.mapNotNull { it.season }.distinct().size
        val episodes = media.videos.count { it.episode != null }
        if (seasons > 0) {
            if (episodes > 0) {
                "$seasons Season${if (seasons != 1) "s" else ""}, $episodes Episode${if (episodes != 1) "s" else ""}"
            } else {
                "$seasons Season${if (seasons != 1) "s" else ""}"
            }
        } else {
            null
        }
    } else {
        null
    }

    val hasMetaRow = releaseYear != null ||
        runtimeText != null ||
        ageBadge != null ||
        rating != null ||
        seriesSummary != null

    val noData = !hasMetaRow &&
        media.genres.isEmpty() &&
        media.tagline.isNullOrBlank() &&
        media.directors.isEmpty() &&
        media.writers.isEmpty() &&
        media.country.isNullOrBlank() &&
        media.language.isNullOrBlank() &&
        media.status.isNullOrBlank() &&
        media.description.isNullOrBlank()
    if (noData) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (hasMetaRow) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // Content type badge
                if (media.type == MediaType.MOVIE) {
                    DetailHeroMetaBadge(text = "Movie")
                } else if (media.type == MediaType.SHOW) {
                    DetailHeroMetaBadge(text = "TV Series")
                }

                releaseYear?.let { year ->
                    Text(
                        text = year,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                    )
                }
                runtimeText?.let { rt ->
                    Text(
                        text = rt,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                    )
                }
                ageBadge?.let { badge ->
                    DetailHeroMetaBadge(text = badge)
                }
                rating?.let { r ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = ImdbYellow,
                        ) {
                            Text(
                                text = "IMDb",
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.sp,
                                ),
                                color = ImdbBlack,
                            )
                        }
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = r,
                            style = MaterialTheme.typography.titleMedium,
                            color = ImdbYellow,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                seriesSummary?.let { summary ->
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }

        if (media.genres.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                media.genres.forEach { genre ->
                    DetailGenreBadge(text = genre)
                }
            }
        }

        if (!media.tagline.isNullOrBlank()) {
            Text(
                text = "“${media.tagline}”",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    letterSpacing = 0.2.sp,
                    lineHeight = 22.sp,
                ),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            )
        }

        if (media.directors.isNotEmpty()) {
            MetaLabelValueRow(
                label = "Director",
                value = media.directors.joinToString(", "),
            )
        }

        if (media.writers.isNotEmpty()) {
            MetaLabelValueRow(
                label = "Writer",
                value = media.writers.joinToString(", "),
            )
        }

        val country = media.country
        if (!country.isNullOrBlank()) {
            MetaLabelValueRow(
                label = "Country",
                value = country,
            )
        }

        val language = media.language
        if (!language.isNullOrBlank()) {
            MetaLabelValueRow(
                label = "Language",
                value = language.uppercase(),
            )
        }

        val status = media.status
        if (!status.isNullOrBlank()) {
            MetaLabelValueRow(
                label = "Status",
                value = status,
            )
        }

        if (!media.description.isNullOrBlank()) {
            var expanded by remember { mutableStateOf(false) }
            var canExpand by remember(media.description) { mutableStateOf(false) }
            Column(
                modifier = Modifier.animateContentSize(),
            ) {
                Text(
                    text = media.description!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                    maxLines = if (expanded) Int.MAX_VALUE else 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 22.sp,
                    onTextLayout = { result ->
                        if (!expanded) {
                            canExpand = result.hasVisualOverflow
                        }
                    },
                )
                if (canExpand) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (expanded) "Show Less" else "Show More",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { expanded = !expanded },
                    )
                }
            }
        }
    }
}

@Composable
private fun MetaLabelValueRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.padding(vertical = 1.dp),
    ) {
        Text(
            text = "$label:  ",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.2.sp,
            ),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                lineHeight = 20.sp,
            ),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f),
        )
    }
}

@Composable
private fun DetailHeroMetaBadge(
    text: String,
    contentColor: Color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
) {
    Box(
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f),
                shape = RoundedCornerShape(8.dp),
            )
            .border(
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f)),
                shape = RoundedCornerShape(8.dp),
            )
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp,
            ),
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun DetailGenreBadge(text: String) {
    Box(
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                shape = RoundedCornerShape(10.dp),
            )
            .border(
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                shape = RoundedCornerShape(10.dp),
            )
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.25.sp,
            ),
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

/**
 * Formats a runtime string for display.
 * Handles formats like "124 min", "2h", "1h 30m", or raw numbers.
 */
private fun formatRuntime(runtime: String?): String? {
    if (runtime.isNullOrBlank()) return null
    // Already looks formatted (contains "h" or "hr")
    if (runtime.contains("h", ignoreCase = true) || runtime.contains("hr", ignoreCase = true)) {
        return runtime
    }
    // Extract numeric portion
    val minutes = runtime.filter { it.isDigit() }.toIntOrNull() ?: return runtime
    if (minutes <= 0) return null

    val hours = minutes / 60
    val mins = minutes % 60
    return when {
        hours > 0 && mins > 0 -> "${hours}h ${mins}m"
        hours > 0 -> "${hours}h"
        else -> "${mins}m"
    }
}

private val ImdbYellow = Color(0xFFF5C518)
private val ImdbBlack = Color(0xFF000000)
