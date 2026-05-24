package com.moviehub.feature.details.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moviehub.core.model.MediaCompany
import com.moviehub.core.model.MediaItem
import com.moviehub.core.model.MediaType
import com.moviehub.core.ui.components.shimmerEffect
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetailProductionSection(
    media: MediaItem,
    modifier: Modifier = Modifier,
    showHeader: Boolean = true,
) {
    val isSeriesLike = media.type == MediaType.SHOW || media.videos.any { it.season != null || it.episode != null }
    val isNetworkSource = isSeriesLike && media.networks.isNotEmpty()
    val sourceItems = if (isSeriesLike) {
        media.networks.ifEmpty { media.productionCompanies }
    } else {
        media.productionCompanies.ifEmpty { media.networks }
    }
    if (sourceItems.isEmpty()) return

    val displayItems = sourceItems.take(6)
    if (displayItems.isEmpty()) return

    DetailSection(
        title = if (isSeriesLike) "Networks" else "Production Companies",
        modifier = modifier,
        showHeader = showHeader,
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val chipHeight = when {
                maxWidth >= 1024.dp -> 44.dp
                maxWidth >= 720.dp -> 40.dp
                else -> 36.dp
            }
            val logoWidth = when {
                maxWidth >= 1024.dp -> 72.dp
                maxWidth >= 720.dp -> 68.dp
                else -> 64.dp
            }
            val logoHeight = when {
                maxWidth >= 1024.dp -> 26.dp
                maxWidth >= 720.dp -> 24.dp
                else -> 22.dp
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                displayItems.forEach { item ->
                    ProductionChip(
                        item = item,
                        chipHeight = chipHeight,
                        logoWidth = logoWidth,
                        logoHeight = logoHeight,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductionChip(
    item: MediaCompany,
    chipHeight: androidx.compose.ui.unit.Dp,
    logoWidth: androidx.compose.ui.unit.Dp,
    logoHeight: androidx.compose.ui.unit.Dp,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color = Color.White)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .height(chipHeight),
        contentAlignment = Alignment.Center,
    ) {
        if (!item.logo.isNullOrBlank()) {
            KamelImage(
                resource = { asyncPainterResource(data = item.logo!!) },
                contentDescription = item.name,
                modifier = Modifier
                    .width(logoWidth)
                    .height(logoHeight),
                contentScale = ContentScale.Fit,
                onLoading = { Box(Modifier.fillMaxSize().shimmerEffect()) }
            )
        } else {
            Text(
                text = item.name,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = Color.Black,
            )
        }
    }
}
