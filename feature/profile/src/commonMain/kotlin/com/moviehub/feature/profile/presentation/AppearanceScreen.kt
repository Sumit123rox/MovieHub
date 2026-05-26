package com.moviehub.feature.profile.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moviehub.core.database.ProfileRepository
import com.moviehub.core.database.UserPreferencesDao
import com.moviehub.core.database.UserPreferencesEntity
import com.moviehub.core.ui.theme.AccentType
import com.moviehub.core.ui.theme.Accents
import com.moviehub.core.ui.theme.ThemeType
import com.moviehub.core.ui.theme.Themes
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceScreen(
    onBackClick: () -> Unit
) {
    val userPreferencesDao: UserPreferencesDao = koinInject()
    val profileRepository: ProfileRepository = koinInject()
    val scope = rememberCoroutineScope()
    val prefs by userPreferencesDao.getPreferenceFlow(
        profileRepository.activeProfile.value?.id ?: ""
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
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            val currentTheme = prefs?.theme?.let { safeThemeFromString(it) } ?: ThemeType.NUVIO_DARK
            val currentAccent = prefs?.accentColor?.let { safeAccentFromString(it) } ?: AccentType.BLUE

            // Theme Section
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Palette,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Theme",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.3.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "Pick your vibe",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ThemeType.entries.forEach { theme ->
                        val isSelected = theme == currentTheme
                        val preview = Themes.fromType(theme)

                        Surface(
                            onClick = { savePreference(scope, profileRepository, userPreferencesDao, prefs, theme, null) },
                            color = preview.surface,
                            shape = RoundedCornerShape(16.dp),
                            border = if (isSelected) BorderStroke(
                                2.dp, MaterialTheme.colorScheme.primary
                            ) else BorderStroke(
                                1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                            ),
                            modifier = Modifier.width(108.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Preview swatch
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(preview.background),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
                                }

                                Text(
                                    text = theme.label,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 11.sp
                                    ),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    maxLines = 1
                                )

                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary)
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
                thickness = 1.dp
            )

            // Accent Color Section
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Palette,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Accent Color",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.3.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "Pick your color",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    AccentType.entries.chunked(5).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            row.forEach { accentType ->
                                val accentColor = Accents.fromType(accentType)
                                val isSelected = accentType == currentAccent

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.clickable {
                                        savePreference(scope, profileRepository, userPreferencesDao, prefs, null, accentType)
                                    }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(accentColor.primary)
                                            .then(
                                                if (isSelected) Modifier.border(
                                                    3.dp, MaterialTheme.colorScheme.onSurface,
                                                    CircleShape
                                                ) else Modifier.border(
                                                    1.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                                                    CircleShape
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = accentColor.onPrimary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = accentType.label,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 10.sp
                                        ),
                                        color = if (isSelected) MaterialTheme.colorScheme.onSurface
                                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Preview card
            Spacer(modifier = Modifier.weight(1f))

            Surface(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                    Column {
                        Text(
                            text = "Preview",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${currentTheme.label} · ${currentAccent.label} accent",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

private fun savePreference(
    scope: kotlinx.coroutines.CoroutineScope,
    profileRepository: ProfileRepository,
    userPreferencesDao: UserPreferencesDao,
    prefs: UserPreferencesEntity?,
    theme: ThemeType?,
    accent: AccentType?
) {
    scope.launch {
        val profile = profileRepository.activeProfile.value ?: return@launch
        userPreferencesDao.setPreference(
            UserPreferencesEntity(
                profileId = profile.id,
                theme = theme?.name?.lowercase() ?: prefs?.theme ?: "nuvio_dark",
                accentColor = accent?.name?.lowercase() ?: prefs?.accentColor ?: "blue",
                useAmoled = prefs?.useAmoled ?: true,
                language = prefs?.language ?: "en",
                tmdbApiKey = prefs?.tmdbApiKey ?: "",
            )
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
