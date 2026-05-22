package com.moviehub.feature.details.presentation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moviehub.core.model.MediaItem
import com.moviehub.core.model.MediaType
import com.moviehub.core.model.MediaVideo
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource

enum class SeasonViewMode {
    Posters, Text;
    fun toggled() = if (this == Posters) Text else Posters
}

@Composable
fun DetailSeriesContent(
    media: MediaItem,
    modifier: Modifier = Modifier,
    preferredSeasonNumber: Int? = null,
    preferredEpisodeNumber: Int? = null,
    onEpisodeClick: ((MediaVideo) -> Unit)? = null,
) {
    val hasVideos = media.videos.isNotEmpty()
    if (media.type != MediaType.SHOW && !hasVideos) return

    if (media.videos.isEmpty()) {
        Column(modifier = modifier.padding(16.dp)) {
            Text(
                text = "Episodes",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "No episodes available.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    val groupedEpisodes = remember(media.videos) {
        val withSeasonOrEp = media.videos.filter { it.season != null || it.episode != null }
        if (withSeasonOrEp.isNotEmpty()) {
            withSeasonOrEp
                .sortedWith(compareBy({ it.season ?: 0 }, { it.episode ?: 0 }))
                .groupBy { it.season ?: 0 }
        } else if (media.type != MediaType.SHOW && media.videos.isNotEmpty()) {
            mapOf(0 to media.videos)
        } else {
            emptyMap()
        }
    }

    if (groupedEpisodes.isEmpty()) {
        if (media.type == MediaType.SHOW) {
            Column(modifier = modifier.padding(16.dp)) {
                Text(
                    text = "Episodes",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Episodes missing season/episode numbers.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        return
    }

    val seasons = groupedEpisodes.keys.sorted()
    val defaultSeason = preferredSeasonNumber
        ?.takeIf { it in groupedEpisodes }
        ?: seasons.first()
    var selectedSeasonOverride by rememberSaveable(media.id) { mutableStateOf<Int?>(null) }
    val currentSeason = selectedSeasonOverride
        ?.takeIf { it in groupedEpisodes }
        ?: defaultSeason

    var seasonViewMode by remember {
        mutableStateOf(SeasonViewMode.Posters)
    }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val sizing = seriesContentSizing(maxWidth.value)
        val containerWidthDp = maxWidth.value

        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(vertical = 16.dp)
        ) {
            if (seasons.size > 1) {
                val hasSeasonPosters = seasons.any { season ->
                    groupedEpisodes[season]
                        .orEmpty()
                        .any { !it.thumbnail.isNullOrBlank() }
                }
                Column(
                    modifier = Modifier.animateContentSize(animationSpec = tween(280)),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Seasons",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontSize = sizing.seasonHeaderSize,
                                fontWeight = FontWeight.SemiBold,
                            ),
                            color = Color.White,
                        )
                        if (hasSeasonPosters) {
                            SeasonViewModeToggle(
                                mode = seasonViewMode,
                                sizing = sizing,
                                onClick = {
                                    seasonViewMode = seasonViewMode.toggled()
                                },
                            )
                        }
                    }

                    if (hasSeasonPosters) {
                        Crossfade(
                            targetState = seasonViewMode,
                            animationSpec = tween(280),
                            label = "season_selector_layout",
                        ) { mode ->
                            when (mode) {
                                SeasonViewMode.Posters -> SeasonPosterScrollRow(
                                    seasons = seasons,
                                    groupedEpisodes = groupedEpisodes,
                                    media = media,
                                    currentSeason = currentSeason,
                                    sizing = sizing,
                                    onSelect = { selectedSeasonOverride = it },
                                )
                                SeasonViewMode.Text -> SeasonTextChipScrollRow(
                                    seasons = seasons,
                                    currentSeason = currentSeason,
                                    sizing = sizing,
                                    onSelect = { selectedSeasonOverride = it },
                                )
                            }
                        }
                    } else {
                        SeasonTextChipScrollRow(
                            seasons = seasons,
                            currentSeason = currentSeason,
                            sizing = sizing,
                            onSelect = { selectedSeasonOverride = it },
                        )
                    }
                }
            }

            AnimatedContent(
                targetState = currentSeason,
                transitionSpec = {
                    val fromIdx = seasons.indexOf(initialState).takeIf { it >= 0 } ?: 0
                    val toIdx = seasons.indexOf(targetState).takeIf { it >= 0 } ?: 0
                    val dir = if (toIdx >= fromIdx) 1 else -1
                    (fadeIn(tween(220)) + slideInHorizontally(tween(220)) { dir * it / 5 })
                        .togetherWith(
                            fadeOut(tween(170)) + slideOutHorizontally(tween(170)) { -dir * it / 5 },
                        )
                },
                label = "season_episodes",
            ) { seasonForContent ->
                val sectionTitle = if (media.type != MediaType.SHOW && seasons.size == 1 && seasonForContent <= 0) {
                    "Videos"
                } else {
                    if (seasonForContent <= 0) "Specials" else "Season $seasonForContent"
                }
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = sectionTitle,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    val seasonEpisodes = groupedEpisodes.getValue(seasonForContent)
                    
                    EpisodeHorizontalRow(
                        episodes = seasonEpisodes,
                        maxWidthDp = containerWidthDp,
                        fallbackImage = media.backgroundUrl ?: media.posterUrl,
                        preferredEpisodeNumber = preferredEpisodeNumber,
                        onEpisodeClick = onEpisodeClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun SeasonViewModeToggle(
    mode: SeasonViewMode,
    sizing: SeriesContentSizing,
    onClick: () -> Unit,
) {
    val isPosters = mode == SeasonViewMode.Posters
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isPosters) {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                },
            )
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = if (isPosters) 0.2f else 0.3f),
                shape = RoundedCornerShape(8.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (isPosters) "Posters" else "Text",
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = sizing.seasonToggleTextSize,
                fontWeight = FontWeight.SemiBold,
            ),
            color = if (isPosters) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                Color.White
            },
        )
    }
}

@Composable
private fun SeasonTextChipScrollRow(
    seasons: List<Int>,
    currentSeason: Int,
    sizing: SeriesContentSizing,
    onSelect: (Int) -> Unit,
) {
    val seasonListState = rememberLazyListState()
    var hasPositionedSeasonRow by remember(seasons) { mutableStateOf(false) }

    LaunchedEffect(seasons, currentSeason) {
        val currentIndex = seasons.indexOf(currentSeason)
        if (currentIndex >= 0) {
            if (hasPositionedSeasonRow) {
                seasonListState.animateScrollToItem(currentIndex)
            } else {
                seasonListState.scrollToItem(currentIndex)
                hasPositionedSeasonRow = true
            }
        }
    }

    LazyRow(
        state = seasonListState,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(sizing.seasonChipGap),
    ) {
        items(seasons, key = { season -> season }) { season ->
            val isSelected = season == currentSeason
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(sizing.seasonChipRadius))
                    .background(
                        if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            Color.White.copy(alpha = 0.1f)
                        },
                    )
                    .clickable { onSelect(season) }
                    .padding(
                        horizontal = sizing.seasonChipHorizontalPadding,
                        vertical = sizing.seasonChipVerticalPadding,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (season <= 0) "Specials" else "Season $season",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = sizing.seasonChipTextSize,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    ),
                    color = if (isSelected) {
                        Color.Black
                    } else {
                        Color.White
                    },
                )
            }
        }
    }
}

@Composable
private fun SeasonPosterScrollRow(
    seasons: List<Int>,
    groupedEpisodes: Map<Int, List<MediaVideo>>,
    media: MediaItem,
    currentSeason: Int,
    sizing: SeriesContentSizing,
    onSelect: (Int) -> Unit,
) {
    val seasonListState = rememberLazyListState()
    var hasPositionedSeasonRow by remember(seasons) { mutableStateOf(false) }

    LaunchedEffect(seasons, currentSeason) {
        val currentIndex = seasons.indexOf(currentSeason)
        if (currentIndex >= 0) {
            if (hasPositionedSeasonRow) {
                seasonListState.animateScrollToItem(currentIndex)
            } else {
                seasonListState.scrollToItem(currentIndex)
                hasPositionedSeasonRow = true
            }
        }
    }

    LazyRow(
        state = seasonListState,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(sizing.seasonChipGap),
    ) {
        items(seasons, key = { season -> season }) { season ->
            SeasonPosterButton(
                label = if (season <= 0) "Specials" else "Season $season",
                imageUrl = groupedEpisodes[season]
                    .orEmpty()
                    .firstNotNullOfOrNull { episode -> episode.thumbnail }
                    ?: media.posterUrl
                    ?: media.backgroundUrl,
                isSelected = season == currentSeason,
                sizing = sizing,
                onClick = { onSelect(season) },
            )
        }
    }
}

@Composable
private fun SeasonPosterButton(
    label: String,
    imageUrl: String?,
    isSelected: Boolean,
    sizing: SeriesContentSizing,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(sizing.seasonPosterWidth)
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(sizing.seasonPosterHeight)
                .clip(RoundedCornerShape(sizing.seasonPosterRadius))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .border(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        Color.White.copy(alpha = 0.1f)
                    },
                    shape = RoundedCornerShape(sizing.seasonPosterRadius),
                ),
        ) {
            if (imageUrl != null) {
                KamelImage(
                    resource = { asyncPainterResource(data = imageUrl) },
                    contentDescription = label,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        modifier = Modifier.padding(horizontal = 12.dp),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = sizing.seasonChipTextSize,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
            ),
            color = if (isSelected) {
                Color.White
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EpisodeHorizontalRow(
    episodes: List<MediaVideo>,
    maxWidthDp: Float,
    fallbackImage: String?,
    preferredEpisodeNumber: Int? = null,
    onEpisodeClick: ((MediaVideo) -> Unit)?,
) {
    val rowMetrics = rememberEpisodeHorizontalCardMetrics(maxWidthDp)
    val listState = rememberLazyListState()
    var hasPositioned by remember(episodes) { mutableStateOf(false) }

    LaunchedEffect(episodes, preferredEpisodeNumber) {
        val targetIndex = if (preferredEpisodeNumber != null) {
            episodes.indexOfFirst { it.episode == preferredEpisodeNumber }
        } else {
            -1
        }
        if (targetIndex >= 0) {
            if (hasPositioned) {
                listState.animateScrollToItem(targetIndex)
            } else {
                listState.scrollToItem(targetIndex)
                hasPositioned = true
            }
        }
    }

    LazyRow(
        state = listState,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = rowMetrics.rowVerticalPadding),
        horizontalArrangement = Arrangement.spacedBy(rowMetrics.itemSpacing),
    ) {
        itemsIndexed(
            items = episodes,
            key = { index, episode -> "${episode.season}:${episode.episode}:${episode.id}#$index" },
        ) { _, episode ->
            EpisodeHorizontalCard(
                video = episode,
                fallbackImage = fallbackImage,
                metrics = rowMetrics,
                onClick = { onEpisodeClick?.invoke(episode) },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EpisodeHorizontalCard(
    video: MediaVideo,
    fallbackImage: String?,
    metrics: EpisodeHorizontalCardMetrics,
    onClick: (() -> Unit)? = null,
) {
    val cardShape = RoundedCornerShape(metrics.cornerRadius)
    Box(
        modifier = Modifier
            .width(metrics.cardWidth)
            .height(metrics.cardHeight)
            .clip(cardShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.12f),
                shape = cardShape,
            )
            .combinedClickable(
                enabled = onClick != null,
                onClick = { onClick?.invoke() },
            ),
    ) {
        val imageUrl = video.thumbnail ?: fallbackImage
        if (imageUrl != null) {
            KamelImage(
                resource = { asyncPainterResource(data = imageUrl) },
                contentDescription = video.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.10f),
                            Color.Black.copy(alpha = 0.42f),
                            Color.Black.copy(alpha = 0.78f),
                        ),
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(
                    start = metrics.contentPadding,
                    end = metrics.contentPadding,
                    top = metrics.contentPadding,
                    bottom = metrics.contentBottomPadding,
                ),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            val badgeText = if (video.season != null && video.episode != null) {
                "S${video.season} E${video.episode}"
            } else if (video.episode != null) {
                "E${video.episode}"
            } else {
                "Episode"
            }
            
            EpisodeCodeBadge(
                text = badgeText,
                textSize = metrics.badgeTextSize,
                radius = metrics.badgeRadius,
                horizontalPadding = metrics.badgeHorizontalPadding,
                verticalPadding = metrics.badgeVerticalPadding,
                backgroundAlpha = 0.42f,
            )

            Text(
                text = video.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = metrics.titleTextSize,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = metrics.titleLineHeight,
                ),
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            if (!video.overview.isNullOrBlank()) {
                Text(
                    text = video.overview!!,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = metrics.bodyTextSize,
                        lineHeight = metrics.bodyLineHeight,
                    ),
                    color = Color.White.copy(alpha = 0.86f),
                    maxLines = metrics.overviewMaxLines,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private data class EpisodeHorizontalCardMetrics(
    val rowHorizontalPadding: Dp,
    val rowVerticalPadding: Dp,
    val itemSpacing: Dp,
    val cardWidth: Dp,
    val cardHeight: Dp,
    val cornerRadius: Dp,
    val contentPadding: Dp,
    val contentBottomPadding: Dp,
    val titleTextSize: androidx.compose.ui.unit.TextUnit,
    val titleLineHeight: androidx.compose.ui.unit.TextUnit,
    val bodyTextSize: androidx.compose.ui.unit.TextUnit,
    val bodyLineHeight: androidx.compose.ui.unit.TextUnit,
    val overviewMaxLines: Int,
    val metaTextSize: androidx.compose.ui.unit.TextUnit,
    val badgeTextSize: androidx.compose.ui.unit.TextUnit,
    val badgeRadius: Dp,
    val badgeHorizontalPadding: Dp,
    val badgeVerticalPadding: Dp,
)

@Composable
private fun rememberEpisodeHorizontalCardMetrics(maxWidthDp: Float): EpisodeHorizontalCardMetrics {
    return remember(maxWidthDp) {
        when {
            maxWidthDp >= 1300f -> EpisodeHorizontalCardMetrics(
                rowHorizontalPadding = 0.dp,
                rowVerticalPadding = 0.dp,
                itemSpacing = 18.dp,
                cardWidth = 420.dp,
                cardHeight = 256.dp,
                cornerRadius = 18.dp,
                contentPadding = 16.dp,
                contentBottomPadding = 18.dp,
                titleTextSize = 18.sp,
                titleLineHeight = 24.sp,
                bodyTextSize = 14.sp,
                bodyLineHeight = 20.sp,
                overviewMaxLines = 3,
                metaTextSize = 12.sp,
                badgeTextSize = 11.sp,
                badgeRadius = 8.dp,
                badgeHorizontalPadding = 10.dp,
                badgeVerticalPadding = 5.dp,
            )

            maxWidthDp >= 1000f -> EpisodeHorizontalCardMetrics(
                rowHorizontalPadding = 0.dp,
                rowVerticalPadding = 0.dp,
                itemSpacing = 16.dp,
                cardWidth = 384.dp,
                cardHeight = 236.dp,
                cornerRadius = 16.dp,
                contentPadding = 14.dp,
                contentBottomPadding = 16.dp,
                titleTextSize = 17.sp,
                titleLineHeight = 22.sp,
                bodyTextSize = 13.sp,
                bodyLineHeight = 18.sp,
                overviewMaxLines = 3,
                metaTextSize = 12.sp,
                badgeTextSize = 10.sp,
                badgeRadius = 7.dp,
                badgeHorizontalPadding = 9.dp,
                badgeVerticalPadding = 4.dp,
            )

            maxWidthDp >= 760f -> EpisodeHorizontalCardMetrics(
                rowHorizontalPadding = 0.dp,
                rowVerticalPadding = 0.dp,
                itemSpacing = 14.dp,
                cardWidth = 340.dp,
                cardHeight = 212.dp,
                cornerRadius = 14.dp,
                contentPadding = 12.dp,
                contentBottomPadding = 14.dp,
                titleTextSize = 16.sp,
                titleLineHeight = 21.sp,
                bodyTextSize = 12.sp,
                bodyLineHeight = 17.sp,
                overviewMaxLines = 2,
                metaTextSize = 11.sp,
                badgeTextSize = 10.sp,
                badgeRadius = 6.dp,
                badgeHorizontalPadding = 8.dp,
                badgeVerticalPadding = 4.dp,
            )

            else -> EpisodeHorizontalCardMetrics(
                rowHorizontalPadding = 0.dp,
                rowVerticalPadding = 0.dp,
                itemSpacing = 12.dp,
                cardWidth = 296.dp,
                cardHeight = 184.dp,
                cornerRadius = 14.dp,
                contentPadding = 10.dp,
                contentBottomPadding = 12.dp,
                titleTextSize = 14.sp,
                titleLineHeight = 19.sp,
                bodyTextSize = 11.sp,
                bodyLineHeight = 15.sp,
                overviewMaxLines = 2,
                metaTextSize = 10.sp,
                badgeTextSize = 9.sp,
                badgeRadius = 5.dp,
                badgeHorizontalPadding = 7.dp,
                badgeVerticalPadding = 3.dp,
            )
        }
    }
}

@Composable
private fun EpisodeCodeBadge(
    text: String,
    textSize: androidx.compose.ui.unit.TextUnit,
    radius: Dp,
    horizontalPadding: Dp,
    verticalPadding: Dp,
    backgroundAlpha: Float,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(radius))
            .background(Color.Black.copy(alpha = backgroundAlpha))
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = textSize,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.sp,
            ),
            color = Color.White.copy(alpha = 0.9f),
            maxLines = 1,
        )
    }
}

private data class SeriesContentSizing(
    val seasonHeaderSize: androidx.compose.ui.unit.TextUnit,
    val seasonToggleTextSize: androidx.compose.ui.unit.TextUnit,
    val seasonChipGap: Dp,
    val seasonChipRadius: Dp,
    val seasonChipHorizontalPadding: Dp,
    val seasonChipVerticalPadding: Dp,
    val seasonChipTextSize: androidx.compose.ui.unit.TextUnit,
    val seasonPosterWidth: Dp,
    val seasonPosterHeight: Dp,
    val seasonPosterRadius: Dp,
)

private fun seriesContentSizing(maxWidthDp: Float): SeriesContentSizing =
    when {
        maxWidthDp >= 1440f -> SeriesContentSizing(
            seasonHeaderSize = 28.sp,
            seasonToggleTextSize = 16.sp,
            seasonChipGap = 20.dp,
            seasonChipRadius = 16.dp,
            seasonChipHorizontalPadding = 20.dp,
            seasonChipVerticalPadding = 16.dp,
            seasonChipTextSize = 16.sp,
            seasonPosterWidth = 140.dp,
            seasonPosterHeight = 210.dp,
            seasonPosterRadius = 16.dp,
        )
        maxWidthDp >= 1024f -> SeriesContentSizing(
            seasonHeaderSize = 26.sp,
            seasonToggleTextSize = 15.sp,
            seasonChipGap = 18.dp,
            seasonChipRadius = 14.dp,
            seasonChipHorizontalPadding = 18.dp,
            seasonChipVerticalPadding = 14.dp,
            seasonChipTextSize = 15.sp,
            seasonPosterWidth = 130.dp,
            seasonPosterHeight = 195.dp,
            seasonPosterRadius = 14.dp,
        )
        maxWidthDp >= 768f -> SeriesContentSizing(
            seasonHeaderSize = 24.sp,
            seasonToggleTextSize = 14.sp,
            seasonChipGap = 16.dp,
            seasonChipRadius = 12.dp,
            seasonChipHorizontalPadding = 16.dp,
            seasonChipVerticalPadding = 12.dp,
            seasonChipTextSize = 17.sp,
            seasonPosterWidth = 120.dp,
            seasonPosterHeight = 180.dp,
            seasonPosterRadius = 12.dp,
        )
        else -> SeriesContentSizing(
            seasonHeaderSize = 18.sp,
            seasonToggleTextSize = 12.sp,
            seasonChipGap = 16.dp,
            seasonChipRadius = 12.dp,
            seasonChipHorizontalPadding = 16.dp,
            seasonChipVerticalPadding = 12.dp,
            seasonChipTextSize = 15.sp,
            seasonPosterWidth = 100.dp,
            seasonPosterHeight = 150.dp,
            seasonPosterRadius = 8.dp,
        )
    }
