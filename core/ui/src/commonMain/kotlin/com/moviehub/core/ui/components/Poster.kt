package com.moviehub.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.touchlab.kermit.Logger
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource

private val posterLogger = Logger.withTag("Poster")

@Composable
fun Poster(
    url: String?,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    shape: Shape = MaterialTheme.shapes.medium,
    aspectRatio: Float = 2f / 3f,
    quality: String? = null,
    isWatched: Boolean = false,
    progressFraction: Float = -1f,
    onClick: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier
            .aspectRatio(aspectRatio)
            .clip(shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (url != null) {
                KamelImage(
                    resource = { asyncPainterResource(data = url) },
                    contentDescription = contentDescription,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    onLoading = { _ ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .shimmerEffect()
                        )
                    },
                    onFailure = { error ->
                        posterLogger.e(error) { "Failed to load poster image" }
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("!", color = MaterialTheme.colorScheme.error)
                        }
                    }
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No URL", style = MaterialTheme.typography.labelSmall)
                }
            }

            if (quality != null) {
                TechnicalBadge(
                    text = quality,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                )
            }

            // Progress bar at bottom (for continue-watching style)
            if (progressFraction >= 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .align(Alignment.BottomCenter)
                        .background(Color.White.copy(alpha = 0.2f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = progressFraction.coerceIn(0f, 1f))
                            .background(Color(0xFF4CAF50))
                    )
                }
            }

            // Watched badge (top-left checkmark)
            if (isWatched) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .size(26.dp)
                        .background(Color(0xFF4CAF50), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "✓",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
