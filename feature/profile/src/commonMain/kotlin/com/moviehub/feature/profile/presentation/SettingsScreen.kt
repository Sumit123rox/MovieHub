package com.moviehub.feature.profile.presentation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.moviehub.core.database.ProfileRepository
import com.moviehub.core.database.TmdbSettingsRepository
import com.moviehub.core.database.UserPreferencesDao
import com.moviehub.core.database.UserPreferencesEntity
import com.moviehub.core.ui.components.MovieHubTopBar
import com.moviehub.core.ui.components.SectionHeader
import com.moviehub.core.ui.components.SettingsRow
import com.moviehub.core.ui.components.SmartStatusBar
import com.moviehub.core.ui.text.nativeTextFieldImeOptions
import com.moviehub.core.ui.theme.MovieHubDimens
import com.moviehub.core.ui.theme.MovieHubColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun SettingsScreen(
    onSwitchProfile: () -> Unit,
    onNavigateToSync: () -> Unit,
    onNavigateToAppearance: () -> Unit,
    onNavigateToDownloads: () -> Unit = {},
    onNavigateToAddons: () -> Unit = {},
    onNavigateToAuth: () -> Unit = {},
    viewModel: ProfileViewModel = koinViewModel(),
) {
    val activeProfile by viewModel.activeProfile.collectAsState()
    val userPreferencesDao: UserPreferencesDao = koinInject()
    val profileRepository: ProfileRepository = koinInject()
    val tmdbSettingsRepository: TmdbSettingsRepository = koinInject()
    val debridSettings: com.moviehub.core.database.DebridSettingsRepository = koinInject()
    val traktSettings: com.moviehub.core.database.TraktSettingsRepository = koinInject()
    val scope = rememberCoroutineScope()

    val prefs by userPreferencesDao.getPreferenceFlow(activeProfile?.id ?: "").collectAsState(null)

    var tmdbKeyInput by remember { mutableStateOf("") }
    var showTmdbKey by remember { mutableStateOf(false) }

    var activeConfigureProvider by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(activeProfile) {
        tmdbKeyInput = tmdbSettingsRepository.getApiKey()
    }

    SmartStatusBar(
        isDark = true,
        color = MaterialTheme.colorScheme.background,
    )

    Scaffold(
        topBar = {
            MovieHubTopBar(title = "Settings")
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(
                horizontal = MovieHubDimens.Spacing.lg,
                vertical = MovieHubDimens.Spacing.xl,
            ),
            verticalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.md),
        ) {
            // ═══ Profile Card ═══
            item(key = "profile_card") {
                AnimatedSettingsEntry(index = 0) {
                    activeProfile?.let { profile ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(MovieHubDimens.Radius.lg))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(MovieHubDimens.Spacing.lg),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(MovieHubDimens.Avatar.md)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = profile.name.take(1).uppercase(),
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }

                                Spacer(modifier = Modifier.width(MovieHubDimens.Spacing.lg))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = profile.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        text = "Active Profile",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    )
                                }

                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                    modifier = Modifier.size(MovieHubDimens.Spacing.xl),
                                )
                            }
                        }
                    }
                }
            }

            // ═══ Profile Management ═══
            item(key = "profile_header") {
                SectionHeader("Profile Management")
            }
            item(key = "switch_profile") {
                AnimatedSettingsEntry(index = 1) {
                    SettingsRow(
                        icon = Icons.Default.Person,
                        title = "Switch Profile",
                        subtitle = "Change active user profile",
                        onClick = onSwitchProfile,
                    )
                }
            }

            // ═══ Appearance ═══
            item(key = "appearance_header") {
                Spacer(modifier = Modifier.height(MovieHubDimens.Spacing.sm))
                SectionHeader("Appearance")
            }
            item(key = "theme_accent") {
                AnimatedSettingsEntry(index = 2) {
                    SettingsRow(
                        icon = Icons.Default.Palette,
                        title = "Theme & Accent",
                        subtitle = prefs?.let {
                            "${safeThemeFromString(it.theme).label} · ${safeAccentFromString(it.accentColor).label}"
                        },
                        onClick = onNavigateToAppearance,
                    )
                }
            }

            // ═══ App Settings ═══
            item(key = "app_header") {
                Spacer(modifier = Modifier.height(MovieHubDimens.Spacing.sm))
                SectionHeader("App Settings")
            }
            item(key = "cloud_sync") {
                AnimatedSettingsEntry(index = 3) {
                    SettingsRow(
                        icon = Icons.Default.Sync,
                        title = "Cloud Sync",
                        subtitle = "Sync data across devices",
                        onClick = onNavigateToSync,
                    )
                }
            }
            item(key = "downloads") {
                AnimatedSettingsEntry(index = 4) {
                    SettingsRow(
                        icon = Icons.Default.Download,
                        title = "Downloads",
                        subtitle = "Manage offline downloads",
                        onClick = onNavigateToDownloads,
                    )
                }
            }
            item(key = "addons") {
                AnimatedSettingsEntry(index = 5) {
                    SettingsRow(
                        icon = Icons.Default.Extension,
                        title = "External Providers",
                        subtitle = "Manage Stremio addons & plugins",
                        onClick = onNavigateToAddons,
                    )
                }
            }

            // Seek Forward/Backward slider
            item(key = "seek_increment") {
                AnimatedSettingsEntry(index = 6) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(MovieHubDimens.Radius.md))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    ) {
                        val seekValue = prefs?.seekIncrement ?: 10
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = MovieHubDimens.Spacing.lg, vertical = MovieHubDimens.Spacing.ml),
                            verticalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.sm),
                        ) {
                            Text(
                                text = "Seek Forward/Backward",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "${seekValue}s jump on seek controls",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                            )
                            Slider(
                                value = seekValue.toFloat(),
                                onValueChange = {
                                    scope.launch {
                                        val pid = activeProfile?.id ?: return@launch
                                        userPreferencesDao.setPreference(
                                            (prefs ?: UserPreferencesEntity(profileId = pid)).copy(
                                                seekIncrement = it.toInt().coerceIn(5, 60),
                                            ),
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
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text("5s", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                                Text("60s", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                            }
                        }
                    }
                }
            }

            // ═══ Debrid & Sync Providers ═══
            item(key = "debrid_sync_header") {
                Spacer(modifier = Modifier.height(MovieHubDimens.Spacing.sm))
                SectionHeader("Debrid & Sync Providers")
            }
            item(key = "debrid_sync_card") {
                AnimatedSettingsEntry(index = 7) {
                    val credentialsMap = remember(prefs?.debridApiKey) {
                        val fullKey = prefs?.debridApiKey ?: ""
                        if (fullKey.startsWith("{") || fullKey.startsWith("[")) {
                            runCatching<Map<String, String>> {
                                kotlinx.serialization.json.Json.Default.decodeFromString<Map<String, String>>(fullKey)
                            }.getOrElse { emptyMap() }
                        } else {
                            if (fullKey.isNotBlank()) mapOf("realdebrid" to fullKey) else emptyMap()
                        }
                    }
                    val isRdConnected = credentialsMap["realdebrid"]?.isNotBlank() ?: false
                    val isAdConnected = credentialsMap["alldebrid"]?.isNotBlank() ?: false
                    val isPmConnected = credentialsMap["premiumize"]?.isNotBlank() ?: false
                    val isTraktConnected = credentialsMap["trakt_access_token"]?.isNotBlank() ?: false

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.md)
                    ) {
                        ProviderCard(
                            name = "Trakt.tv Sync",
                            isConnected = isTraktConnected,
                            onConfigureClick = { activeConfigureProvider = "trakt" },
                            onLinkClick = onNavigateToAuth
                        )
                        ProviderCard(
                            name = "Real-Debrid",
                            isConnected = isRdConnected,
                            onConfigureClick = { activeConfigureProvider = "realdebrid" },
                            onLinkClick = onNavigateToAuth
                        )
                        ProviderCard(
                            name = "AllDebrid",
                            isConnected = isAdConnected,
                            onConfigureClick = { activeConfigureProvider = "alldebrid" }
                        )
                        ProviderCard(
                            name = "Premiumize.me",
                            isConnected = isPmConnected,
                            onConfigureClick = { activeConfigureProvider = "premiumize" }
                        )
                    }
                }
            }

            // ═══ Metadata Enrichment ═══
            item(key = "tmdb_header") {
                Spacer(modifier = Modifier.height(MovieHubDimens.Spacing.sm))
                SectionHeader("Metadata Enrichment")
            }
            item(key = "tmdb_card") {
                AnimatedSettingsEntry(index = 7) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(MovieHubDimens.Radius.md))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = MovieHubDimens.Spacing.lg, vertical = MovieHubDimens.Spacing.ml),
                            verticalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.ms),
                        ) {
                            Text(
                                text = "TMDB API Key",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "Get a free API key at themoviedb.org to enable cast photos, ratings, and person details.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                            )
                            OutlinedTextField(
                                value = tmdbKeyInput,
                                onValueChange = { tmdbKeyInput = it },
                                placeholder = {
                                    Text(
                                        "Enter TMDB API Key",
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                    )
                                },
                                visualTransformation = if (showTmdbKey) VisualTransformation.None else PasswordVisualTransformation(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                    cursorColor = MaterialTheme.colorScheme.onSurface,
                                ),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    platformImeOptions = nativeTextFieldImeOptions(),
                                ),
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.md),
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
                                    contentPadding = PaddingValues(
                                        horizontal = MovieHubDimens.Spacing.xxl,
                                        vertical = MovieHubDimens.Spacing.sm,
                                    ),
                                ) {
                                    Text("Save Key", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // ═══ Logout ═══
            item(key = "logout") {
                AnimatedSettingsEntry(index = 8) {
                    Button(
                        onClick = { },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                        ),
                        contentPadding = PaddingValues(horizontal = MovieHubDimens.Spacing.lg, vertical = MovieHubDimens.Spacing.lg),
                        shape = RoundedCornerShape(MovieHubDimens.Radius.md),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Logout,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                        )
                        Spacer(modifier = Modifier.width(MovieHubDimens.Spacing.sm))
                        Text("Logout", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium)
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(MovieHubDimens.Spacing.lg))
            }
        }

        val providerToConfigure = activeConfigureProvider
        if (providerToConfigure != null) {
            val currentKey = remember(providerToConfigure, prefs?.debridApiKey) {
                val fullKey = prefs?.debridApiKey ?: ""
                val map = if (fullKey.startsWith("{") || fullKey.startsWith("[")) {
                    runCatching<Map<String, String>> {
                        kotlinx.serialization.json.Json.Default.decodeFromString<Map<String, String>>(fullKey)
                    }.getOrElse { emptyMap() }
                } else {
                    if (fullKey.isNotBlank()) mapOf("realdebrid" to fullKey) else emptyMap()
                }
                val credKey = if (providerToConfigure == "trakt") "trakt_access_token" else providerToConfigure
                map[credKey] ?: ""
            }

            ConfigureProviderDialog(
                providerName = when (providerToConfigure) {
                    "realdebrid" -> "Real-Debrid"
                    "alldebrid" -> "AllDebrid"
                    "premiumize" -> "Premiumize"
                    "trakt" -> "Trakt"
                    else -> providerToConfigure
                },
                currentKey = currentKey,
                onSave = { newKey ->
                    scope.launch {
                        if (providerToConfigure == "trakt") {
                            traktSettings.setAccessToken(newKey)
                        } else {
                            debridSettings.setApiKey(providerToConfigure, newKey)
                        }
                    }
                },
                onDelete = {
                    scope.launch {
                        if (providerToConfigure == "trakt") {
                            traktSettings.setAccessToken("")
                            traktSettings.setRefreshToken("")
                        } else {
                            debridSettings.setApiKey(providerToConfigure, "")
                        }
                    }
                },
                onDismiss = { activeConfigureProvider = null }
            )
        }
    }
}

@Composable
fun ProviderCard(
    name: String,
    isConnected: Boolean,
    onConfigureClick: () -> Unit,
    onLinkClick: (() -> Unit)? = null
) {
    val scale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MovieHubDimens.Radius.md))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .clickable {
                scope.launch {
                    scale.animateTo(0.96f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
                    scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
                    onConfigureClick()
                }
            }
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            .padding(MovieHubDimens.Spacing.lg)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.md)
        ) {
            Box(
                modifier = Modifier
                    .size(MovieHubDimens.Spacing.md)
                    .clip(CircleShape)
                    .background(
                        if (isConnected) {
                            MovieHubColors.Success
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                        }
                    )
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (isConnected) "Connected" else "Not configured",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isConnected) {
                        MovieHubColors.Success.copy(alpha = 0.8f)
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                    }
                )
            }

            if (onLinkClick != null && !isConnected) {
                Button(
                    onClick = onLinkClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(MovieHubDimens.Radius.sm),
                    contentPadding = PaddingValues(horizontal = MovieHubDimens.Spacing.md, vertical = MovieHubDimens.Spacing.xs)
                ) {
                    Text("Link Account", style = MaterialTheme.typography.labelMedium)
                }
            } else {
                IconButton(onClick = onConfigureClick) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Configure",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                }
            }
        }
    }
}

@Composable
fun ConfigureProviderDialog(
    providerName: String,
    currentKey: String,
    onSave: (String) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    var keyInput by remember { mutableStateOf(currentKey) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Configure $providerName",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.md)
            ) {
                Text(
                    text = "Enter your API key or token to connect $providerName with MovieHub.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                OutlinedTextField(
                    value = keyInput,
                    onValueChange = { keyInput = it },
                    placeholder = {
                        Text(
                            text = "Paste API Key here...",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                        cursorColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.sm)
            ) {
                if (currentKey.isNotBlank()) {
                    TextButton(
                        onClick = {
                            onDelete()
                            onDismiss()
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Disconnect")
                    }
                }

                Button(
                    onClick = {
                        onSave(keyInput)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Save")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(MovieHubDimens.Radius.lg)
    )
}

@Composable
private fun AnimatedSettingsEntry(
    index: Int = 0,
    content: @Composable BoxScope.() -> Unit,
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
        content = content,
    )
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
