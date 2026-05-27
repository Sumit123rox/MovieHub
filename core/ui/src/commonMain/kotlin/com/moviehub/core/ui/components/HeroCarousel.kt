package com.moviehub.core.ui.components

import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.draw.blur
import com.moviehub.core.model.MediaItem
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.absoluteValue
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun HeroCarousel(
    items: List<MediaItem>,
    onItemClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return

    // Pager state with offset to allow scrolling backward, but initialized to index 0
    val initialPage = Int.MAX_VALUE / 2
    val offset = initialPage % items.size
    val startPage = initialPage - offset

    val pagerState = rememberPagerState(initialPage = startPage, pageCount = { Int.MAX_VALUE })

    // Auto-scroll logic: 4s delay with smooth spring animation
    LaunchedEffect(pagerState.settledPage) {
        while (isActive) {
            delay(4000.milliseconds)
            if (!pagerState.isScrollInProgress) {
                try {
                    pagerState.animateScrollToPage(
                        pagerState.currentPage + 1,
                        animationSpec = tween(600)
                    )
                } catch (e: CancellationException) {
                    if (!isActive) throw e
                }
            }
        }
    }

    // Snap recovery: if pager is stuck mid-transition (e.g., after background resume)
    LaunchedEffect(Unit) {
        delay(100.milliseconds)
        if (!pagerState.isScrollInProgress && pagerState.currentPageOffsetFraction.absoluteValue > 0.1f) {
            try {
                pagerState.scrollToPage(pagerState.currentPage)
            } catch (_: CancellationException) { }
        }
    }

    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val gradientStartY = with(androidx.compose.ui.platform.LocalDensity.current) { 180.dp.toPx() }

    Box(modifier = modifier.fillMaxWidth().height(650.dp)) {
        // 1. Background Plate (Blurred background that bleeds)
        val currentItem by remember { derivedStateOf { items[pagerState.currentPage % items.size] } }

        Box(modifier = Modifier.fillMaxSize()) {
            // Background plate with blur — Crossfade removed since the gradient
            // overlay and pager animation provide enough visual continuity.
            // Rendering two blurred images simultaneously (Crossfade) doubles GPU work.
            KamelImage(
                resource = {
                    asyncPainterResource(
                        data = currentItem.posterUrl ?: currentItem.backgroundUrl ?: ""
                    )
                },
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(8.dp)
            )

            // Top gradient for status bar and header clarity
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.4f),
                                Color.Transparent,
                                MaterialTheme.colorScheme.background.copy(alpha = 0.5f),
                                MaterialTheme.colorScheme.background
                            )
                        )
                    )
            )
        }

        // 2. Elevated Card Pager
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 48.dp,
                top = statusBarPadding + 24.dp,
                end = 48.dp,
                bottom = 48.dp
            ),
            pageSpacing = 16.dp
        ) { page ->
            val item = items[page % items.size]
            val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val progress = 1f - pageOffset.absoluteValue.coerceIn(0f, 1f)
                        val scale = lerp(0.85f, 1f, progress)
                        scaleX = scale
                        scaleY = scale
                        alpha = lerp(0.5f, 1f, progress)
                        translationY = (1f - progress) * -24f
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { onItemClick(item) }
                ) {
                    KamelImage(
                        resource = {
                            asyncPainterResource(
                                data = item.posterUrl ?: item.backgroundUrl ?: ""
                            )
                        },
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Card Bottom Gradient
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.5f)
                                    ),
                                    startY = gradientStartY
                                )
                            )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Watch Now Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.White,
                                    Color(0xFFD3D3D3)
                                )
                            )
                        )
                        .clickable { onItemClick(item) }
                        .padding(horizontal = 32.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "Details",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.Black
                    )
                }
            }
        }

        // 3. Reactive Indicator
        ReactiveIndicator(
            pagerState = pagerState,
            count = items.size,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        )
    }
}

@Composable
fun ReactiveIndicator(
    pagerState: PagerState,
    count: Int,
    modifier: Modifier = Modifier
) {
    val currentPage by remember { derivedStateOf { pagerState.currentPage % count } }
    val offsetFraction by remember { derivedStateOf { pagerState.currentPageOffsetFraction } }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(count) { index ->
            val isSelected = currentPage == index

            val width = if (isSelected) {
                lerp(24f, 8f, offsetFraction.absoluteValue).dp
            } else if ((pagerState.currentPage + 1) % count == index && offsetFraction > 0) {
                lerp(8f, 24f, offsetFraction).dp
            } else if ((pagerState.currentPage - 1 + count) % count == index && offsetFraction < 0) {
                lerp(8f, 24f, offsetFraction.absoluteValue).dp
            } else {
                8.dp
            }

            val color =
                if (isSelected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onBackground.copy(
                    alpha = 0.4f
                )

            Box(modifier = Modifier.height(6.dp).width(width).clip(CircleShape).background(color))
        }
    }
}
