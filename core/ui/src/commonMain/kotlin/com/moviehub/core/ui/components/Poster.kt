package com.moviehub.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import com.moviehub.core.ui.theme.MovieHubDimens
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.graphicsLayer

@Composable
fun Poster(
    url: String?,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    title: String? = null,
    shape: Shape = MaterialTheme.shapes.medium,
    aspectRatio: Float = MovieHubDimens.Poster.aspectRatio,
    quality: String? = null,
    isWatched: Boolean = false,
    progressFraction: Float = -1f,
    onClick: (() -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.96f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "PosterPressScale"
    )

    val resolvedUrl = remember(url) {
        if (url.isNullOrBlank()) null
        else if (!url.startsWith("http")) {
            "https://image.tmdb.org/t/p/w342$url"
        } else {
            url
        }
    }

    Surface(
        modifier = modifier
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .aspectRatio(aspectRatio)
            .clip(shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                } else Modifier
            ),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (resolvedUrl != null) {
                KamelImage(
                    resource = { asyncPainterResource(data = resolvedUrl) },
                    contentDescription = contentDescription,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    onLoading = { _ ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .shimmerEffect(),
                        )
                    },
                    onFailure = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(MovieHubDimens.Spacing.sm)
                            ) {
                                androidx.compose.material3.Icon(
                                    imageVector = Icons.Default.Movie,
                                    contentDescription = "Failed to load image",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.size(MovieHubDimens.Icon.xl)
                                )
                                if (!title.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(MovieHubDimens.Spacing.xs))
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        maxLines = 2,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    },
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = title ?: "No URL",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        maxLines = 3,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier.padding(MovieHubDimens.Spacing.sm),
                    )
                }
            }

            if (quality != null) {
                TechnicalBadge(
                    text = quality,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(MovieHubDimens.Spacing.sm),
                )
            }

            // Progress bar at bottom (for continue-watching style)
            if (progressFraction >= 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(MovieHubDimens.Player.seekBarActive)
                        .align(Alignment.BottomCenter)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = progressFraction.coerceIn(0f, 1f))
                            .background(MaterialTheme.colorScheme.primary),
                    )
                }
            }

            // Watched badge (top-left checkmark)
            if (isWatched) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(MovieHubDimens.Spacing.sm)
                        .size(MovieHubDimens.Icon.xl)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "✓",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = MovieHubDimens.Font.lg,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}
