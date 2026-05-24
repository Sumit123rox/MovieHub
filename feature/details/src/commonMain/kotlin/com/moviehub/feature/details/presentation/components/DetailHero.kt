package com.moviehub.feature.details.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.moviehub.core.model.MediaItem
import com.moviehub.core.ui.components.shimmerEffect
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource

@Composable
fun DetailHero(
    media: MediaItem,
    isTablet: Boolean = false,
    scrollOffset: Int = 0,
    contentMaxWidth: Dp = 560.dp,
    onHeightChanged: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
    ) {
        val heroHeight = detailHeroHeight(maxWidth, isTablet)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(heroHeight)
                .onSizeChanged { onHeightChanged(it.height) }
                .graphicsLayer {
                    clip = true
                },
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.BottomCenter,
            ) {
                val imageUrl = media.backgroundUrl ?: media.posterUrl
                if (imageUrl != null) {
                    KamelImage(
                        resource = { asyncPainterResource(data = imageUrl) },
                        contentDescription = media.title,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                translationY = scrollOffset * 0.5f
                                scaleX = 1.08f
                                scaleY = 1.08f
                            },
                        alignment = if (isTablet) Alignment.TopCenter else Alignment.Center,
                        contentScale = ContentScale.Crop,
                        onLoading = { Box(Modifier.fillMaxSize().shimmerEffect()) }
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surface),
                    )
                }

                // Stronger black gradient at the bottom
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.5f),
                                    Color.Black.copy(alpha = 0.8f),
                                    Color.Black,
                                ),
                            ),
                        ),
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = if (isTablet) 32.dp else 18.dp)
                        .padding(bottom = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (media.logoUrl != null) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth(if (isTablet) 0.56f else 0.6f)
                                .widthIn(max = contentMaxWidth)
                                .height(if (isTablet) 72.dp else 80.dp)
                        ) {
                            // Subtle light-glow behind the logo for visibility
                            Box(
                                modifier = Modifier
                                    .fillMaxSize(0.8f)
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(
                                                Color.White.copy(alpha = 0.15f),
                                                Color.Transparent
                                            )
                                        )
                                    )
                            )
                            
                            KamelImage(
                                resource = { asyncPainterResource(data = media.logoUrl!!) },
                                contentDescription = media.title,
                                modifier = Modifier.fillMaxSize(),
                                alignment = Alignment.Center,
                                contentScale = ContentScale.Fit,
                                onLoading = { Box(Modifier.fillMaxSize().shimmerEffect()) }
                            )
                        }
                    } else {
                        Text(
                            text = media.title,
                            style = if (isTablet) MaterialTheme.typography.displaySmall else MaterialTheme.typography.displayLarge,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                        )
                    }

                    if (media.genres.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = media.genres.take(3).joinToString(" \u2022 "),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

private fun detailHeroHeight(maxWidth: Dp, isTablet: Boolean): Dp =
    if (!isTablet) {
        (maxWidth * 1.33f).coerceIn(420.dp, 760.dp)
    } else {
        (maxWidth * 0.42f).coerceIn(300.dp, 420.dp)
    }
