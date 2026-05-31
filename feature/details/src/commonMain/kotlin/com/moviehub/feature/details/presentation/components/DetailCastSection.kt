package com.moviehub.feature.details.presentation.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.moviehub.core.model.MediaPerson
import com.moviehub.core.ui.components.shimmerEffect
import com.moviehub.core.ui.theme.MovieHubDimens
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource

@Composable
fun DetailCastSection(
    cast: List<MediaPerson>,
    modifier: Modifier = Modifier,
    showHeader: Boolean = true,
    onCastClick: ((MediaPerson) -> Unit)? = null,
) {
    if (cast.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.md),
    ) {
        if (showHeader) {
            Text(
                text = "Cast",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = MovieHubDimens.Spacing.lg),
            )
        }

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = MovieHubDimens.Spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.lg),
        ) {
            items(cast, key = { "${it.name}_${it.role}" }) { person ->
                CastItem(
                    person = person,
                    onClick = onCastClick?.let { { it(person) } },
                )
            }
        }
    }
}

@Composable
private fun CastItem(
    person: MediaPerson,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val hasPhoto = person.photo != null && person.photo!!.startsWith("http")
    val avatarUrl = if (hasPhoto) {
        person.photo
    } else {
        "https://ui-avatars.com/api/?name=${person.name.replace(" ", "+")}&background=1a1a1a&color=ffffff&size=128"
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.96f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "CastItemPressScale"
    )

    Column(
        modifier = modifier
            .width(MovieHubDimens.Avatar.md + MovieHubDimens.Spacing.xxl)
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clip(MaterialTheme.shapes.medium)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = onClick != null,
                onClick = { onClick?.invoke() }
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.sm),
    ) {
        Box(
            modifier = Modifier
                .size(MovieHubDimens.Avatar.md)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(
                    width = MovieHubDimens.Spacing.dp1,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                    shape = CircleShape
                ),
        ) {
            if (avatarUrl != null) {
                KamelImage(
                    resource = { asyncPainterResource(data = avatarUrl) },
                    contentDescription = person.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    onLoading = { _ -> Box(Modifier.fillMaxSize().shimmerEffect()) },
                    onFailure = { _ ->
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = person.name.take(1),
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                            )
                        }
                    },
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = person.name.take(1),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    )
                }
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.dp2),
        ) {
            Text(
                text = person.name,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = MovieHubDimens.Font.md,
                ),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (person.role != null) {
                Text(
                    text = person.role!!,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = MovieHubDimens.Font.xs,
                    ),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

