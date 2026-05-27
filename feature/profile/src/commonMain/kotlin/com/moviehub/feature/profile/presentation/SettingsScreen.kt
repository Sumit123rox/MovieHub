package com.moviehub.feature.profile.presentation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.moviehub.core.database.ProfileRepository
import com.moviehub.core.database.TmdbSettingsRepository
import com.moviehub.core.database.UserPreferencesDao
import com.moviehub.core.database.UserPreferencesEntity
import com.moviehub.core.ui.components.ContentCard
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onSwitchProfile: () -> Unit,
    onNavigateToSync: () -> Unit,
    onNavigateToAppearance: () -> Unit,
    onNavigateToDownloads: () -> Unit = {},
    viewModel: ProfileViewModel = koinViewModel()
) {
    val activeProfile by viewModel.activeProfile.collectAsState()
    val userPreferencesDao: UserPreferencesDao = koinInject()
    val profileRepository: ProfileRepository = koinInject()
    val tmdbSettingsRepository: TmdbSettingsRepository = koinInject()
    val scope = rememberCoroutineScope()

    val prefs by userPreferencesDao.getPreferenceFlow(activeProfile?.id ?: "").collectAsState(null)

    var tmdbKeyInput by remember { mutableStateOf("") }
    var showTmdbKey by remember { mutableStateOf(false) }

    LaunchedEffect(activeProfile) {
        tmdbKeyInput = tmdbSettingsRepository.getApiKey()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Profile Card
            item(key = "profile_card") {
                AnimatedSettingsEntry(index = 0) {
                    activeProfile?.let { profile ->
                        ContentCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = profile.name.take(1).uppercase(),
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column {
                                    Text(
                                        text = profile.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Active Profile",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Profile Management
            item(key = "profile_header") {
                SectionHeader("Profile Management")
            }
            item(key = "switch_profile") {
                AnimatedSettingsEntry(index = 1) {
                    SettingsCardItem(
                        icon = Icons.Default.Person,
                        title = "Switch Profile",
                        onClick = onSwitchProfile
                    )
                }
            }

            // Appearance
            item(key = "appearance_header") {
                SectionHeader("Appearance")
            }
            item(key = "theme_accent") {
                AnimatedSettingsEntry(index = 2) {
                    SettingsCardItem(
                        icon = Icons.Default.Palette,
                        title = "Theme & Accent",
                        subtitle = prefs?.let {
                            "${safeThemeFromString(it.theme).label} · ${safeAccentFromString(it.accentColor).label}"
                        },
                        onClick = onNavigateToAppearance
                    )
                }
            }

            // App Settings
            item(key = "app_header") {
                SectionHeader("App Settings")
            }
            item(key = "cloud_sync") {
                AnimatedSettingsEntry(index = 3) {
                    SettingsCardItem(
                        icon = Icons.Default.Sync,
                        title = "Cloud Sync",
                        onClick = onNavigateToSync
                    )
                }
            }
            item(key = "downloads") {
                AnimatedSettingsEntry(index = 4) {
                    SettingsCardItem(
                        icon = Icons.Default.Download,
                        title = "Downloads",
                        subtitle = "Manage offline downloads",
                        onClick = onNavigateToDownloads
                    )
                }
            }

        item(key = "seek_increment") {
                AnimatedSettingsEntry(index = 5) {
                    ContentCard(modifier = Modifier.fillMaxWidth()) {
                        val seekValue = prefs?.seekIncrement ?: 10
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Seek Forward/Backward",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "$seekValue seconds",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                )
                            }
                            Slider(
                                value = seekValue.toFloat(),
                                onValueChange = {
                                    scope.launch {
                                        val pid = activeProfile?.id ?: return@launch
                                        userPreferencesDao.setPreference(
                                            (prefs ?: UserPreferencesEntity(profileId = pid)).copy(seekIncrement = it.toInt().coerceIn(5, 60))
                                        )
                                    }
                                },
                                valueRange = 5f..60f,
                                steps = 10,
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary,
                                    inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("5s", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                                Text("60s", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                            }
                        }
                    }
                }
            }

            // TMDB API Key
            item(key = "tmdb_header") {
                SectionHeader("Metadata Enrichment")
            }
            item(key = "tmdb_card") {
                AnimatedSettingsEntry(index = 6) {
                    ContentCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "TMDB API Key",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = "Get a free API key at themoviedb.org to enable cast photos, ratings, and person details.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                            )
                            OutlinedTextField(
                                value = tmdbKeyInput,
                                onValueChange = { tmdbKeyInput = it },
                                placeholder = { Text("Enter TMDB API Key", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)) },
                                visualTransformation = if (showTmdbKey) VisualTransformation.None else PasswordVisualTransformation(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                    cursorColor = MaterialTheme.colorScheme.onSurface,
                                ),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                TextButton(onClick = { showTmdbKey = !showTmdbKey }) {
                                    Text(
                                        text = if (showTmdbKey) "Hide" else "Show",
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                Button(
                                    onClick = {
                                        scope.launch {
                                            tmdbSettingsRepository.setApiKey(tmdbKeyInput)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                    ),
                                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                                ) {
                                    Text(
                                        text = "Save Key",
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Logout
            item(key = "logout") {
                AnimatedSettingsEntry(index = 7) {
                    Button(
                        onClick = { },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                        ),
                        contentPadding = PaddingValues(16.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Logout", color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun AnimatedSettingsEntry(
    index: Int = 0,
    content: @Composable BoxScope.() -> Unit
) {
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay((index * 50L).coerceAtMost(300))
        alpha.animateTo(1f, tween(350))
    }

    Box(
        modifier = Modifier.graphicsLayer {
            this.alpha = alpha.value
            this.translationY = (1f - alpha.value) * 20f
        },
        content = content
    )
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
    )
}

@Composable
fun SettingsCardItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    ContentCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }
            if (subtitle == null) {
                Spacer(modifier = Modifier.weight(1f))
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

private fun safeThemeFromString(value: String): com.moviehub.core.ui.theme.ThemeType {
    return try {
        com.moviehub.core.ui.theme.ThemeType.valueOf(value.uppercase())
    } catch (_: Exception) {
        com.moviehub.core.ui.theme.ThemeType.NUVIO_DARK
    }
}

private fun safeAccentFromString(value: String): com.moviehub.core.ui.theme.AccentType {
    return try {
        com.moviehub.core.ui.theme.AccentType.valueOf(value.uppercase())
    } catch (_: Exception) {
        com.moviehub.core.ui.theme.AccentType.BLUE
    }
}
