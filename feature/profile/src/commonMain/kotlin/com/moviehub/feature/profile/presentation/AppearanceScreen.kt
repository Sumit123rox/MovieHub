package com.moviehub.feature.profile.presentation

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.moviehub.core.database.ProfileRepository
import com.moviehub.core.database.UserPreferencesDao
import com.moviehub.core.database.UserPreferencesEntity
import com.moviehub.core.ui.theme.AccentType
import com.moviehub.core.ui.theme.Accents
import com.moviehub.core.ui.theme.MovieHubDimens
import com.moviehub.core.ui.theme.ThemeType
import com.moviehub.core.ui.theme.Themes
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceScreen(
    onBackClick: () -> Unit,
) {
    val userPreferencesDao: UserPreferencesDao = koinInject()
    val profileRepository: ProfileRepository = koinInject()
    val scope = rememberCoroutineScope()
    val prefs by userPreferencesDao.getPreferenceFlow(
        profileRepository.activeProfile.value?.id ?: "",
    ).collectAsState(null)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Appearance", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = MovieHubDimens.Spacing.xl, vertical = MovieHubDimens.Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.xxl),
        ) {
            val currentTheme = prefs?.theme?.let { safeThemeFromString(it) } ?: ThemeType.NUVIO_DARK
            val currentAccent = prefs?.accentColor?.let { safeAccentFromString(it) } ?: AccentType.BLUE

            // Theme Section
            Column(verticalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.ml)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.sm),
                ) {
                    Icon(
                        Icons.Default.Palette,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(MovieHubDimens.Icon.sm),
                    )
                    Text(
                        text = "Theme",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = MovieHubDimens.Font.trackingWide,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "Pick your vibe",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.md),
                ) {
                    ThemeType.entries.forEach { theme ->
                        val isSelected = theme == currentTheme
                        val preview = Themes.fromType(theme)

                        val interactionSource = remember { MutableInteractionSource() }
                        val isPressed by interactionSource.collectIsPressedAsState()
                        val scale by animateFloatAsState(
                            targetValue = if (isPressed) 0.96f else 1.0f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                            label = "ThemeScale_${theme.name}"
                        )

                        Surface(
                            modifier = Modifier
                                .width(108.dp)
                                .graphicsLayer(scaleX = scale, scaleY = scale)
                                .clip(RoundedCornerShape(MovieHubDimens.Radius.lg))
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = null,
                                    onClick = { savePreference(scope, profileRepository, userPreferencesDao, prefs, theme, null) }
                                ),
                            color = preview.surface,
                            shape = RoundedCornerShape(MovieHubDimens.Radius.lg),
                            border = if (isSelected) {
                                BorderStroke(
                                    MovieHubDimens.Spacing.dp2, MaterialTheme.colorScheme.primary,
                                )
                            } else {
                                BorderStroke(
                                    MovieHubDimens.Spacing.dp1, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                )
                            },
                        ) {
                            Column(
                                modifier = Modifier.padding(MovieHubDimens.Spacing.md),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.ms),
                            ) {
                                // Premium dynamic mini mockup preview of each theme
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                        .clip(RoundedCornerShape(MovieHubDimens.Radius.sm))
                                        .background(preview.background)
                                        .border(
                                            width = MovieHubDimens.Spacing.dp1,
                                            color = preview.onSurface.copy(alpha = 0.08f),
                                            shape = RoundedCornerShape(MovieHubDimens.Radius.sm)
                                        )
                                        .padding(MovieHubDimens.Spacing.xxs)
                                ) {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.xxs),
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        // Mini Hero Banner
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(18.dp)
                                                .clip(RoundedCornerShape(MovieHubDimens.Spacing.xxs))
                                                .background(preview.surfaceVariant)
                                        ) {
                                            // Mini Play Button in the mockup
                                            Box(
                                                modifier = Modifier
                                                    .padding(end = MovieHubDimens.Spacing.xxs)
                                                    .size(MovieHubDimens.Spacing.sm)
                                                    .align(Alignment.BottomEnd)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.primary)
                                            )
                                        }
                                        // Mini Catalog Row
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.xxs),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(MovieHubDimens.Icon.sm)
                                                    .clip(RoundedCornerShape(MovieHubDimens.Spacing.xxs))
                                                    .background(preview.surfaceVariant)
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(MovieHubDimens.Icon.sm)
                                                    .clip(RoundedCornerShape(MovieHubDimens.Spacing.xxs))
                                                    .background(preview.surfaceVariant)
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = theme.label,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = MovieHubDimens.Font.sm,
                                    ),
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        preview.onSurface.copy(alpha = 0.7f)
                                    },
                                    maxLines = 1,
                                )

                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(MovieHubDimens.Spacing.xs)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Divider
            HorizontalDivider(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                thickness = MovieHubDimens.Spacing.dp1,
            )

            // Accent Color Section
            Column(verticalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.ml)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.sm),
                ) {
                    Icon(
                        Icons.Default.Palette,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(MovieHubDimens.Icon.sm),
                    )
                    Text(
                        text = "Accent Color",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = MovieHubDimens.Font.trackingWide,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "Pick your color",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.md)) {
                    AccentType.entries.chunked(5).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            row.forEach { accentType ->
                                val isSelected = accentType == currentAccent
                                AccentColorWidget(
                                    accentType = accentType,
                                    isSelected = isSelected,
                                    onClick = {
                                        savePreference(scope, profileRepository, userPreferencesDao, prefs, null, accentType)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Preview card
            Spacer(modifier = Modifier.weight(1f))

            Surface(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f),
                shape = RoundedCornerShape(MovieHubDimens.Radius.lg),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(MovieHubDimens.Spacing.lg),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.md),
                ) {
                    Box(
                        modifier = Modifier
                            .size(MovieHubDimens.Avatar.sm)
                            .clip(RoundedCornerShape(MovieHubDimens.Radius.sm))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(MovieHubDimens.Icon.sm)
                                .clip(RoundedCornerShape(MovieHubDimens.Spacing.xxs))
                                .background(MaterialTheme.colorScheme.primary),
                        )
                    }
                    Column {
                        Text(
                            text = "Preview",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "${currentTheme.label} · ${currentAccent.label} accent",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(MovieHubDimens.Spacing.sm))
        }
    }
}

@Composable
private fun AccentColorWidget(
    accentType: AccentType,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val accentColor = remember(accentType) { Accents.fromType(accentType) }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "AccentScale_${accentType.name}"
    )

    val glowAlpha by animateFloatAsState(
        targetValue = if (isSelected) 0.45f else 0.08f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "AccentGlow_${accentType.name}"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.xs),
        modifier = Modifier
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clip(MaterialTheme.shapes.medium)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .width(MovieHubDimens.Icon.jumbo)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(accentColor.primary)
                .border(
                    width = if (isSelected) MovieHubDimens.Spacing.dp3 else MovieHubDimens.Spacing.dp1,
                    color = if (isSelected) accentColor.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                    shape = CircleShape
                )
                // Neon glow ring outline when selected
                .then(
                    if (isSelected) {
                        Modifier.border(
                            width = MovieHubDimens.Spacing.dp2,
                            color = accentColor.primary.copy(alpha = glowAlpha),
                            shape = CircleShape
                        )
                    } else Modifier
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = accentColor.onPrimary,
                    modifier = Modifier.size(MovieHubDimens.Icon.sm),
                )
            }
        }
        Text(
            text = accentType.label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                fontSize = MovieHubDimens.Font.xs,
            ),
            color = if (isSelected) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
            },
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
    }
}

private fun savePreference(
    scope: kotlinx.coroutines.CoroutineScope,
    profileRepository: ProfileRepository,
    userPreferencesDao: UserPreferencesDao,
    prefs: UserPreferencesEntity?,
    theme: ThemeType?,
    accent: AccentType?,
) {
    scope.launch {
        val profile = profileRepository.activeProfile.value ?: return@launch
        userPreferencesDao.setPreference(
            UserPreferencesEntity(
                profileId = profile.id,
                theme = theme?.name?.lowercase() ?: prefs?.theme ?: "nuvio_dark",
                accentColor = accent?.name?.lowercase() ?: prefs?.accentColor ?: "blue",
                useAmoled = true,
                language = prefs?.language ?: "en",
                tmdbApiKey = prefs?.tmdbApiKey ?: "",
            ),
        )
    }
}

private fun safeThemeFromString(value: String): ThemeType {
    return try {
        ThemeType.valueOf(value.uppercase())
    } catch (_: Exception) {
        ThemeType.NUVIO_DARK
    }
}

private fun safeAccentFromString(value: String): AccentType {
    return try {
        AccentType.valueOf(value.uppercase())
    } catch (_: Exception) {
        AccentType.BLUE
    }
}


