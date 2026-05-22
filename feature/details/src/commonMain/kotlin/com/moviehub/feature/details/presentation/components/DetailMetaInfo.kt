package com.moviehub.feature.details.presentation.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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

@Composable
fun DetailMetaInfo(
    media: MediaItem,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val releaseYear = media.releaseInfo?.take(4)
        val runtimeText = media.runtime
        val ageBadge = media.ageRating?.trim()?.takeIf { it.isNotBlank() }
        val rating = media.rating

        val hasMetaRow = releaseYear != null ||
            runtimeText != null ||
            ageBadge != null ||
            rating != null

        if (hasMetaRow) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                releaseYear?.let { year ->
                    Text(
                        text = year,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    )
                }
                runtimeText?.let { rt ->
                    Text(
                        text = rt,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
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
            }
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

        if (!media.description.isNullOrBlank()) {
            var expanded by remember { mutableStateOf(false) }
            var canExpand by remember(media.description) { mutableStateOf(false) }
            Column(
                modifier = Modifier.animateContentSize(),
            ) {
                Text(
                    text = media.description!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f),
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
                        color = Color.White,
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
    Row {
        Text(
            text = "$label:  ",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.6f),
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
        )
    }
}

@Composable
private fun DetailHeroMetaBadge(
    text: String,
    contentColor: Color = Color.White.copy(alpha = 0.7f),
) {
    Box(
        modifier = Modifier
            .border(
                border = BorderStroke(1.dp, contentColor.copy(alpha = 0.55f)),
                shape = RoundedCornerShape(6.dp),
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private val ImdbYellow = Color(0xFFF5C518)
private val ImdbBlack = Color(0xFF000000)
