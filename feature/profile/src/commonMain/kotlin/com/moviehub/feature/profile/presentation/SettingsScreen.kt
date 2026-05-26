package com.moviehub.feature.profile.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import com.moviehub.core.database.ProfileRepository
import com.moviehub.core.database.TmdbSettingsRepository
import com.moviehub.core.database.UserPreferencesDao
import com.moviehub.core.database.UserPreferencesEntity
import com.moviehub.core.ui.components.ContentCard
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

    // TMDB key state
    var tmdbKeyInput by remember { mutableStateOf("") }
    var showTmdbKey by remember { mutableStateOf(false) }

    // Load existing TMDB key on profile change
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
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Active Profile Section
            item {
                activeProfile?.let { profile ->
                    ContentCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), MaterialTheme.shapes.small),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = profile.name.take(1).uppercase(),
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
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }

            // Profile Management
            item {
                SectionLabel("Profile Management")

                SettingsItem(
                    icon = Icons.Default.Person,
                    title = "Switch Profile",
                    onClick = onSwitchProfile
                )
            }

            // Appearance
            item {
                SectionLabel("Appearance")

                SettingsItem(
                    icon = Icons.Default.Palette,
                    title = "Theme & Accent",
                    subtitle = prefs?.let {
                        "${safeThemeFromString(it.theme).label} · ${safeAccentFromString(it.accentColor).label}"
                    },
                    onClick = onNavigateToAppearance
                )
            }

            // AMOLED Toggle
            item {
                ContentCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.DarkMode,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "AMOLED Dark Mode",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = prefs?.useAmoled ?: true,
                            onCheckedChange = { enabled ->
                                scope.launch {
                                    val profile = profileRepository.activeProfile.value ?: return@launch
                                    userPreferencesDao.setPreference(
                                        UserPreferencesEntity(
                                            profileId = profile.id,
                                            theme = prefs?.theme ?: "nuvio_dark",
                                            accentColor = prefs?.accentColor ?: "blue",
                                            useAmoled = enabled,
                                            language = prefs?.language ?: "en",
                                            tmdbApiKey = prefs?.tmdbApiKey ?: "",
                                        )
                                    )
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                            )
                        )
                    }
                }
            }

            // Cloud Sync
            item {
                SectionLabel("App Settings")

                SettingsItem(
                    icon = Icons.Default.Sync,
                    title = "Cloud Sync",
                    onClick = onNavigateToSync
                )

                SettingsItem(
                    icon = Icons.Default.Download,
                    title = "Downloads",
                    subtitle = "Manage offline downloads",
                    onClick = onNavigateToDownloads
                )
            }

            // TMDB API Key
            item {
                Text(
                    text = "Metadata Enrichment",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                )
                ContentCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
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
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
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

            // Logout
            item {
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = { /* Handle actual logout from app? */ },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f)),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Icon(Icons.Default.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Logout", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    ContentCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f), modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
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
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
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
