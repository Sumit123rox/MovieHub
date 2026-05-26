package com.moviehub.feature.details.presentation.person

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moviehub.core.model.MediaPreview
import com.moviehub.core.network.tmdb.TmdbEnrichmentService
import com.moviehub.core.network.tmdb.TmdbImageUrl
import com.moviehub.core.ui.components.Poster
import com.moviehub.core.ui.components.shimmerEffect
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import org.koin.compose.koinInject

@Composable
fun PersonDetailScreen(
    personId: Int,
    personName: String?,
    onBackClick: () -> Unit,
    onMediaClick: (id: String, type: String) -> Unit,
    enrichmentService: TmdbEnrichmentService = koinInject(),
) {
    var uiState by remember { mutableStateOf(PersonDetailUiState()) }

    LaunchedEffect(personId) {
        uiState = uiState.copy(isLoading = true, error = null)
        try {
            val detail = enrichmentService.fetchPersonDetail(personId)
            val credits = enrichmentService.fetchPersonCredits(personId)
            if (detail != null) {
                val personDetail = PersonDetail(
                    id = detail.id,
                    name = detail.name,
                    photoUrl = TmdbImageUrl.profile(detail.profilePath, "w500"),
                    biography = detail.biography,
                    birthday = detail.birthday,
                    deathday = detail.deathday,
                    placeOfBirth = detail.placeOfBirth,
                    knownForDepartment = detail.knownForDepartment,
                    alsoKnownAs = detail.alsoKnownAs,
                )
                uiState = PersonDetailUiState(
                    isLoading = false,
                    person = personDetail,
                    credits = credits,
                )
            } else {
                uiState = PersonDetailUiState(
                    isLoading = false,
                    error = "Could not load person details",
                )
            }
        } catch (e: Exception) {
            uiState = PersonDetailUiState(
                isLoading = false,
                error = "Error loading person: ${e.message}",
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when {
            uiState.isLoading -> PersonDetailSkeleton()
            uiState.error != null -> PersonDetailError(
                error = uiState.error!!,
                onRetry = {
                    uiState = uiState.copy(isLoading = true, error = null)
                }
            )
            uiState.person != null -> PersonDetailContent(
                person = uiState.person!!,
                credits = uiState.credits,
                onMediaClick = onMediaClick,
            )
        }

        // Back button
        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 8.dp, start = 8.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun PersonDetailContent(
    person: PersonDetail,
    credits: List<MediaPreview>,
    onMediaClick: (id: String, type: String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            PersonHeroSection(person = person)
        }

        // Biography
        if (!person.biography.isNullOrBlank()) {
            item {
                var expanded by remember { mutableStateOf(false) }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "Biography",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = person.biography!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                        maxLines = if (expanded) Int.MAX_VALUE else 5,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 22.sp,
                    )
                    if (person.biography.length > 300) {
                        Text(
                            text = if (expanded) "Show Less" else "Show More",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable { expanded = !expanded }
                                .padding(top = 4.dp),
                        )
                    }
                }
            }
        }

        // Filmography
        if (credits.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 8.dp)
                ) {
                    Text(
                        text = "Known For",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(credits) { preview ->
                            Poster(
                                url = preview.posterUrl,
                                contentDescription = preview.title,
                                modifier = Modifier
                                    .width(120.dp)
                                    .height(180.dp),
                                onClick = {
                                    onMediaClick(preview.id, preview.type)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PersonHeroSection(
    person: PersonDetail,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 80.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Profile photo
        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (person.photoUrl != null) {
                KamelImage(
                    resource = { asyncPainterResource(data = person.photoUrl) },
                    contentDescription = person.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    onLoading = { Box(Modifier.fillMaxSize().shimmerEffect()) },
                    onFailure = { Box(Modifier.fillMaxSize().shimmerEffect()) },
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Name
        Text(
            text = person.name,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp),
        )

        // Known for department
        if (person.knownForDepartment != null) {
            Text(
                text = person.knownForDepartment,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        // Personal info badges
        Row(
            modifier = Modifier.padding(top = 12.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (person.birthday != null) {
                InfoChip(
                    label = "Born",
                    value = formatDateForDisplay(person.birthday)
                )
            }
            if (person.deathday != null) {
                InfoChip(
                    label = "Died",
                    value = formatDateForDisplay(person.deathday)
                )
            }
            if (person.placeOfBirth != null) {
                InfoChip(
                    label = "From",
                    value = person.placeOfBirth
                )
            }
        }
    }
}

@Composable
private fun InfoChip(
    label: String,
    value: String,
) {
    Column(
        modifier = Modifier.padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun PersonDetailSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(CircleShape)
                .shimmerEffect()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Box(modifier = Modifier.width(200.dp).height(24.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
        Spacer(modifier = Modifier.height(8.dp))
        Box(modifier = Modifier.width(120.dp).height(18.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
        Spacer(modifier = Modifier.height(24.dp))
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(16.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
        Box(modifier = Modifier.fillMaxWidth(0.9f).padding(horizontal = 16.dp).height(16.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
        Box(modifier = Modifier.fillMaxWidth(0.7f).padding(horizontal = 16.dp).height(16.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
    }
}

@Composable
private fun PersonDetailError(
    error: String,
    onRetry: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 32.dp),
            )
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

private fun formatDateForDisplay(dateStr: String): String {
    val parts = dateStr.split("-")
    if (parts.size != 3) return dateStr
    val monthNames = listOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )
    val year = parts[0]
    val month = parts[1].toIntOrNull()?.let { monthNames.getOrNull(it - 1) } ?: parts[1]
    val day = parts[2].removePrefix("0")
    return "$month $day, $year"
}
