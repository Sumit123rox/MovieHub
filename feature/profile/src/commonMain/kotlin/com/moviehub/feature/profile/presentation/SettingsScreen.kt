package com.moviehub.feature.profile.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moviehub.core.database.ProfileRepository
import com.moviehub.core.database.TmdbSettingsRepository
import com.moviehub.core.database.UserPreferencesDao
import com.moviehub.core.database.UserPreferencesEntity
import com.moviehub.core.database.PlaybackPreferences
import com.moviehub.core.database.PlaybackPreferencesRepository
import com.moviehub.core.model.SubtitleStyle
import com.moviehub.core.ui.components.MovieHubTopBar
import com.moviehub.core.ui.components.SectionHeader
import com.moviehub.core.ui.components.SettingsRow
import com.moviehub.core.ui.components.SmartStatusBar
import com.moviehub.core.ui.text.nativeTextFieldImeOptions
import com.moviehub.core.ui.theme.MovieHubDimens
import androidx.compose.ui.graphics.luminance
import com.moviehub.core.ui.theme.MovieHubColors
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

enum class SettingsSubPage(val title: String, val subtitle: String) {
    GENERAL("General", "Language, Region, Theme, Profiles"),
    PLAYBACK("Playback", "Auto Play, Resume, Preferred Quality"),
    SUBTITLES("Subtitles", "Font, Size, Colors, FX, Presets"),
    STREAMING("Streaming", "Source Preferences, Timeout, Add-ons"),
    SYNC("Sync & Accounts", "Providers, Accounts, Sync Status"),
    DOWNLOADS("Downloads", "Storage, Cache, Download Preferences"),
    ADVANCED("Advanced", "Logs, Debug Tools, TMDB Key")
}

data class SubtitlePreset(
    val name: String,
    val style: SubtitleStyle,
    val isBuiltIn: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun SettingsScreen(
    onSwitchProfile: () -> Unit,
    onNavigateToSync: () -> Unit,
    onNavigateToAppearance: () -> Unit,
    onNavigateToDownloads: () -> Unit = {},
    onNavigateToAddons: () -> Unit = {},
    onNavigateToAuth: () -> Unit = {},
    onBackClick: () -> Unit = {},
    viewModel: ProfileViewModel = koinViewModel(),
    settingsViewModel: SettingsViewModel = koinViewModel(),
) {
    val activeProfile by viewModel.activeProfile.collectAsState()
    val state by settingsViewModel.state.collectAsState()

    // Room flows read directly for auto-updating reactive UI
    val userPreferencesDao: UserPreferencesDao = koinInject()
    val playbackPreferencesRepository: PlaybackPreferencesRepository = koinInject()
    val tmdbSettingsRepository: TmdbSettingsRepository = koinInject()
    val traktSettings: com.moviehub.core.database.TraktSettingsRepository = koinInject()
    val debridSettings: com.moviehub.core.database.DebridSettingsRepository = koinInject()

    val prefs by userPreferencesDao.getPreferenceFlow(activeProfile?.id ?: "").collectAsState(null)
    val playbackPrefs by playbackPreferencesRepository.getPreferencesFlow().collectAsState(PlaybackPreferences())
    val scope = rememberCoroutineScope()

    val currentSubPage = state.currentSubPage
    PlatformBackHandler(enabled = currentSubPage != null) {
        settingsViewModel.onAction(SettingsAction.NavigateToSubPage(null))
    }
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

    // Subtitle style read from persisted prefs
    var globalSubtitleStyle by remember { mutableStateOf(SubtitleStyle()) }
    LaunchedEffect(prefs?.subtitleStyleJson) {
        prefs?.subtitleStyleJson?.let { styleJson ->
            if (styleJson.isNotBlank()) {
                runCatching {
                    globalSubtitleStyle = kotlinx.serialization.json.Json.decodeFromString<SubtitleStyle>(styleJson)
                }
            }
        }
    }

    fun updateGlobalSubtitleStyle(newStyle: SubtitleStyle) {
        globalSubtitleStyle = newStyle
        settingsViewModel.onAction(SettingsAction.UpdateGlobalSubtitleStyle(newStyle))
    }

    LaunchedEffect(activeProfile) {
        settingsViewModel.onAction(SettingsAction.SetTmdbKeyInput(tmdbSettingsRepository.getApiKey()))
    }

    val isSystemDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    SmartStatusBar(
        isDark = isSystemDark,
        color = MaterialTheme.colorScheme.background,
    )

    Scaffold(
        topBar = {
            MovieHubTopBar(
                title = currentSubPage?.title ?: "Settings",
                onBackClick = if (currentSubPage != null) {
                    { settingsViewModel.onAction(SettingsAction.NavigateToSubPage(null)) }
                } else null
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (currentSubPage == null) {
                // Main Settings Menu
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        horizontal = MovieHubDimens.Spacing.lg,
                        vertical = MovieHubDimens.Spacing.xl,
                    ),
                    verticalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.md),
                ) {
                    // Profile Card Preview
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
                                    }
                                }
                            }
                        }
                    }

                    // Settings Sub-Pages Rows
                    item { SectionHeader("Settings Categories") }

                    item {
                        SettingsRow(
                            icon = Icons.Default.Settings,
                            title = SettingsSubPage.GENERAL.title,
                            subtitle = SettingsSubPage.GENERAL.subtitle,
                            onClick = { settingsViewModel.onAction(SettingsAction.NavigateToSubPage(SettingsSubPage.GENERAL)) }
                        )
                    }

                    item {
                        SettingsRow(
                            icon = Icons.Default.PlayCircle,
                            title = SettingsSubPage.PLAYBACK.title,
                            subtitle = SettingsSubPage.PLAYBACK.subtitle,
                            onClick = { settingsViewModel.onAction(SettingsAction.NavigateToSubPage(SettingsSubPage.PLAYBACK)) }
                        )
                    }

                    item {
                        SettingsRow(
                            icon = Icons.Default.Subtitles,
                            title = SettingsSubPage.SUBTITLES.title,
                            subtitle = SettingsSubPage.SUBTITLES.subtitle,
                            onClick = { settingsViewModel.onAction(SettingsAction.NavigateToSubPage(SettingsSubPage.SUBTITLES)) }
                        )
                    }

                    item {
                        SettingsRow(
                            icon = Icons.Default.Router,
                            title = SettingsSubPage.STREAMING.title,
                            subtitle = SettingsSubPage.STREAMING.subtitle,
                            onClick = { settingsViewModel.onAction(SettingsAction.NavigateToSubPage(SettingsSubPage.STREAMING)) }
                        )
                    }

                    item {
                        SettingsRow(
                            icon = Icons.Default.Sync,
                            title = SettingsSubPage.SYNC.title,
                            subtitle = SettingsSubPage.SYNC.subtitle,
                            onClick = { settingsViewModel.onAction(SettingsAction.NavigateToSubPage(SettingsSubPage.SYNC)) }
                        )
                    }

                    item {
                        SettingsRow(
                            icon = Icons.Default.Download,
                            title = SettingsSubPage.DOWNLOADS.title,
                            subtitle = SettingsSubPage.DOWNLOADS.subtitle,
                            onClick = { settingsViewModel.onAction(SettingsAction.NavigateToSubPage(SettingsSubPage.DOWNLOADS)) }
                        )
                    }

                    item {
                        SettingsRow(
                            icon = Icons.Default.Code,
                            title = SettingsSubPage.ADVANCED.title,
                            subtitle = SettingsSubPage.ADVANCED.subtitle,
                            onClick = { settingsViewModel.onAction(SettingsAction.NavigateToSubPage(SettingsSubPage.ADVANCED)) }
                        )
                    }
                }
            } else {
                // Render Active Sub-Page
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        horizontal = MovieHubDimens.Spacing.lg,
                        vertical = MovieHubDimens.Spacing.xl,
                    ),
                    verticalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.md),
                ) {
                    when (currentSubPage) {
                        SettingsSubPage.GENERAL -> {
                            item { SectionHeader("Profiles & Theme") }
                            item {
                                SettingsRow(
                                    icon = Icons.Default.Person,
                                    title = "Switch Profile",
                                    subtitle = "Change active user profile",
                                    onClick = onSwitchProfile,
                                )
                            }
                            item {
                                SettingsRow(
                                    icon = Icons.Default.Palette,
                                    title = "Theme & Accent",
                                    subtitle = prefs?.let {
                                        "${safeThemeFromString(it.theme).label} · ${safeAccentFromString(it.accentColor).label}"
                                    },
                                    onClick = onNavigateToAppearance,
                                )
                            }
                            item { SectionHeader("Locale Preferences") }
                            item {
                                SettingsRow(
                                    icon = Icons.Default.Language,
                                    title = "Language",
                                    subtitle = "English (US)",
                                    onClick = { settingsViewModel.onAction(SettingsAction.ShowFeedback("Language selection is coming soon!")) }
                                )
                            }
                            item {
                                SettingsRow(
                                    icon = Icons.Default.Public,
                                    title = "Region / Country",
                                    subtitle = "United States (US)",
                                    onClick = { settingsViewModel.onAction(SettingsAction.ShowFeedback("Country selection is coming soon!")) }
                                )
                            }
                        }

                        SettingsSubPage.PLAYBACK -> {
                            item { SectionHeader("Video Seek Controls") }
                            item {
                                val seekValue = prefs?.seekIncrement ?: 10
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

                            item { SectionHeader("Playback Behaviors") }
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = MovieHubDimens.Spacing.xxs),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Auto Play Next Episode", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                        Text("Automatically start next episode in a series", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                    }
                                    Switch(
                                        checked = playbackPrefs.autoPlayNext,
                                        onCheckedChange = { value ->
                                            scope.launch {
                                                playbackPreferencesRepository.updatePreferences(
                                                    playbackPrefs.copy(autoPlayNext = value)
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = MovieHubDimens.Spacing.xxs),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Resume Playback", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                        Text("Resume videos from last watched position", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                    }
                                    Switch(
                                        checked = playbackPrefs.resumePlayback,
                                        onCheckedChange = { value ->
                                            scope.launch {
                                                playbackPreferencesRepository.updatePreferences(
                                                    playbackPrefs.copy(resumePlayback = value)
                                                )
                                            }
                                        }
                                    )
                                }
                            }

                            item { SectionHeader("Advanced Playback Controls") }
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = MovieHubDimens.Spacing.xs)
                                        .clickable { settingsViewModel.onAction(SettingsAction.SetActiveSelectionDialog("resolution")) },
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Preferred Video Quality", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                        Text("Default stream resolution", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                    }
                                    Text(playbackPrefs.preferredResolution, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                }
                            }
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = MovieHubDimens.Spacing.xs)
                                        .clickable { settingsViewModel.onAction(SettingsAction.SetActiveSelectionDialog("codec")) },
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Preferred Video Codec", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                        Text("Codec compatibility for playback", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                    }
                                    Text(playbackPrefs.preferredCodec, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                }
                            }
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = MovieHubDimens.Spacing.xxs),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Auto Frame Rate", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                        Text("Match display refresh rate to content (HDMI)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                    }
                                    Switch(
                                        checked = playbackPrefs.autoFrameRate,
                                        onCheckedChange = { value ->
                                            scope.launch {
                                                playbackPreferencesRepository.updatePreferences(
                                                    playbackPrefs.copy(autoFrameRate = value)
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = MovieHubDimens.Spacing.xs)
                                        .clickable { settingsViewModel.onAction(SettingsAction.SetActiveSelectionDialog("speed")) },
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Default Playback Speed", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                        Text("Playback rate when starting a stream", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                    }
                                    Text("${playbackPrefs.defaultSpeed}x", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                }
                            }
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(MovieHubDimens.Radius.md))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                        .padding(MovieHubDimens.Spacing.md)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.sm)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Resume Threshold (%)", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                            Text("${playbackPrefs.resumeThresholdPercent}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                        }
                                        Text("Mark as fully watched above this percentage", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                        Slider(
                                            value = playbackPrefs.resumeThresholdPercent.toFloat(),
                                            onValueChange = { value ->
                                                scope.launch {
                                                    playbackPreferencesRepository.updatePreferences(
                                                        playbackPrefs.copy(resumeThresholdPercent = value.toInt())
                                                    )
                                                }
                                            },
                                            valueRange = 50f..95f,
                                            steps = 9
                                        )
                                    }
                                }
                            }
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(MovieHubDimens.Radius.md))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                        .padding(MovieHubDimens.Spacing.md)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.sm)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Skip Intro Duration", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                            Text("${playbackPrefs.skipIntroSeconds}s", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                        }
                                        Text("Seconds to skip when skip intro trigger occurs", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                        Slider(
                                            value = playbackPrefs.skipIntroSeconds.toFloat(),
                                            onValueChange = { value ->
                                                scope.launch {
                                                    playbackPreferencesRepository.updatePreferences(
                                                        playbackPrefs.copy(skipIntroSeconds = value.toInt())
                                                    )
                                                }
                                            },
                                            valueRange = 0f..120f,
                                            steps = 12
                                        )
                                    }
                                }
                            }
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(MovieHubDimens.Radius.md))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                        .padding(MovieHubDimens.Spacing.md)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.sm)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Skip Credits Duration", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                            Text("${playbackPrefs.skipCreditsSeconds}s", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                        }
                                        Text("Seconds to skip before content ending", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                        Slider(
                                            value = playbackPrefs.skipCreditsSeconds.toFloat(),
                                            onValueChange = { value ->
                                                scope.launch {
                                                    playbackPreferencesRepository.updatePreferences(
                                                        playbackPrefs.copy(skipCreditsSeconds = value.toInt())
                                                    )
                                                }
                                            },
                                            valueRange = 0f..120f,
                                            steps = 12
                                        )
                                    }
                                }
                            }
                        }

                        SettingsSubPage.SUBTITLES -> {
                            item { SectionHeader("Subtitle Customizer Preview") }
                            item {
                                // Subtitle Customizer Preview Box
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(72.dp)
                                        .clip(RoundedCornerShape(MovieHubDimens.Radius.lg))
                                        .background(Color.Black.copy(alpha = 0.85f))
                                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(MovieHubDimens.Radius.lg))
                                        .padding(MovieHubDimens.Spacing.xs),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.BottomCenter
                                    ) {
                                        val textCol = if (globalSubtitleStyle.fontColorArgb == -1) Color.White else Color(globalSubtitleStyle.fontColorArgb)
                                        val bgCol = Color(globalSubtitleStyle.bgColorArgb).copy(alpha = globalSubtitleStyle.bgOpacity)

                                        val shadowList = remember(globalSubtitleStyle) {
                                            when (globalSubtitleStyle.edgeStyle) {
                                                "Shadow" -> listOf(
                                                    androidx.compose.ui.graphics.Shadow(
                                                        color = Color(globalSubtitleStyle.shadowColorArgb),
                                                        offset = androidx.compose.ui.geometry.Offset(globalSubtitleStyle.shadowOffsetDp * 2, globalSubtitleStyle.shadowOffsetDp * 2),
                                                        blurRadius = globalSubtitleStyle.shadowRadiusDp * 2,
                                                    ),
                                                )
                                                "Outline" -> listOf(
                                                    androidx.compose.ui.graphics.Shadow(color = Color(globalSubtitleStyle.outlineColorArgb), offset = androidx.compose.ui.geometry.Offset(-globalSubtitleStyle.outlineThicknessDp, -globalSubtitleStyle.outlineThicknessDp), blurRadius = 0f),
                                                    androidx.compose.ui.graphics.Shadow(color = Color(globalSubtitleStyle.outlineColorArgb), offset = androidx.compose.ui.geometry.Offset(globalSubtitleStyle.outlineThicknessDp, -globalSubtitleStyle.outlineThicknessDp), blurRadius = 0f),
                                                    androidx.compose.ui.graphics.Shadow(color = Color(globalSubtitleStyle.outlineColorArgb), offset = androidx.compose.ui.geometry.Offset(-globalSubtitleStyle.outlineThicknessDp, globalSubtitleStyle.outlineThicknessDp), blurRadius = 0f),
                                                    androidx.compose.ui.graphics.Shadow(color = Color(globalSubtitleStyle.outlineColorArgb), offset = androidx.compose.ui.geometry.Offset(globalSubtitleStyle.outlineThicknessDp, globalSubtitleStyle.outlineThicknessDp), blurRadius = 0f),
                                                )
                                                else -> emptyList()
                                            }
                                        }

                                        Box(
                                            modifier = Modifier
                                                .padding(bottom = MovieHubDimens.Spacing.xxs)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(bgCol)
                                                .padding(horizontal = 6.dp, vertical = 2.dp),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(
                                                text = "Global Subtitle Preview Style...",
                                                color = textCol,
                                                fontSize = (globalSubtitleStyle.fontSizeSp * 0.65f).coerceIn(10f, 16f).sp,
                                                fontStyle = if (globalSubtitleStyle.isItalic) androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal,
                                                fontWeight = if (globalSubtitleStyle.isBold) FontWeight.Bold else FontWeight.Normal,
                                                letterSpacing = globalSubtitleStyle.letterSpacingSp.sp,
                                                lineHeight = globalSubtitleStyle.lineHeightSp.sp,
                                                style = androidx.compose.ui.text.TextStyle(
                                                    fontFamily = when (globalSubtitleStyle.fontFamily) {
                                                        "Serif" -> androidx.compose.ui.text.font.FontFamily.Serif
                                                        "Monospace" -> androidx.compose.ui.text.font.FontFamily.Monospace
                                                        "Cursive" -> androidx.compose.ui.text.font.FontFamily.Cursive
                                                        else -> androidx.compose.ui.text.font.FontFamily.SansSerif
                                                    },
                                                    shadow = shadowList.firstOrNull(),
                                                ),
                                            )
                                        }
                                    }
                                }
                            }

                            item {
                                ScrollableTabRow(
                                    selectedTabIndex = state.selectedSubtitleTab,
                                    modifier = Modifier.fillMaxWidth().height(36.dp),
                                    edgePadding = 0.dp,
                                    containerColor = Color.Transparent,
                                    contentColor = MaterialTheme.colorScheme.primary,
                                    indicator = { tabPositions ->
                                        if (state.selectedSubtitleTab < tabPositions.size) {
                                            TabRowDefaults.SecondaryIndicator(
                                                modifier = Modifier.tabIndicatorOffset(tabPositions[state.selectedSubtitleTab]),
                                                color = MaterialTheme.colorScheme.primary,
                                            )
                                        }
                                    },
                                ) {
                                    Tab(selected = state.selectedSubtitleTab == 0, onClick = { settingsViewModel.onAction(SettingsAction.SetSelectedSubtitleTab(0)) }) {
                                        Text("Typography", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = MovieHubDimens.Spacing.xxs))
                                    }
                                    Tab(selected = state.selectedSubtitleTab == 1, onClick = { settingsViewModel.onAction(SettingsAction.SetSelectedSubtitleTab(1)) }) {
                                        Text("Color & FX", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = MovieHubDimens.Spacing.xxs))
                                    }
                                    Tab(selected = state.selectedSubtitleTab == 2, onClick = { settingsViewModel.onAction(SettingsAction.SetSelectedSubtitleTab(2)) }) {
                                        Text("Presets", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = MovieHubDimens.Spacing.xxs))
                                    }
                                }
                            }

                            // Subtitle Editor Panels
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(280.dp)
                                        .padding(vertical = MovieHubDimens.Spacing.xs)
                                ) {
                                    when (state.selectedSubtitleTab) {
                                        0 -> {
                                            LazyColumn(
                                                modifier = Modifier.fillMaxSize(),
                                                verticalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.xs),
                                            ) {
                                                // Font Family
                                                item {
                                                    Text("Font Family", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth().padding(top = MovieHubDimens.Spacing.xs),
                                                        horizontalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.xs),
                                                    ) {
                                                        listOf("Sans-Serif", "Serif", "Monospace", "Cursive").forEach { font ->
                                                            FilterChip(
                                                                selected = globalSubtitleStyle.fontFamily == font,
                                                                onClick = { updateGlobalSubtitleStyle(globalSubtitleStyle.copy(fontFamily = font)) },
                                                                label = { Text(font, style = MaterialTheme.typography.labelSmall) },
                                                            )
                                                        }
                                                    }
                                                }

                                                // Font Size
                                                item {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically,
                                                    ) {
                                                        Text("Font Size", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                                                        Text("${globalSubtitleStyle.fontSizeSp} sp", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                                    }
                                                    Slider(
                                                        value = globalSubtitleStyle.fontSizeSp.toFloat(),
                                                        onValueChange = { updateGlobalSubtitleStyle(globalSubtitleStyle.copy(fontSizeSp = it.toInt())) },
                                                        valueRange = 12f..36f,
                                                        steps = 24,
                                                    )
                                                }

                                                // Bold & Italic
                                                item {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.lg),
                                                    ) {
                                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                            Checkbox(
                                                                checked = globalSubtitleStyle.isBold,
                                                                onCheckedChange = { updateGlobalSubtitleStyle(globalSubtitleStyle.copy(isBold = it)) },
                                                            )
                                                            Text("Bold", style = MaterialTheme.typography.bodyMedium)
                                                        }
                                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                            Checkbox(
                                                                checked = globalSubtitleStyle.isItalic,
                                                                onCheckedChange = { updateGlobalSubtitleStyle(globalSubtitleStyle.copy(isItalic = it)) },
                                                            )
                                                            Text("Italic", style = MaterialTheme.typography.bodyMedium)
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        1 -> {
                                            LazyColumn(
                                                modifier = Modifier.fillMaxSize(),
                                                verticalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.xs),
                                            ) {
                                                // Text Color
                                                item {
                                                    Text("Text Color", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth().padding(top = MovieHubDimens.Spacing.xs),
                                                        horizontalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.xs),
                                                    ) {
                                                        val colors = listOf(-1 to "White", 0xFFFFE600.toInt() to "Yellow", 0xFF00FFCC.toInt() to "Cyan", 0xFF00FF66.toInt() to "Green", 0xFFFF00FF.toInt() to "Magenta", 0xFFFF0000.toInt() to "Red", 0xFF000000.toInt() to "Black")
                                                        colors.forEach { (colorVal, _) ->
                                                            val isSelected = globalSubtitleStyle.fontColorArgb == colorVal
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(28.dp)
                                                                    .clip(CircleShape)
                                                                    .background(if (colorVal == -1) Color.White else Color(colorVal))
                                                                    .border(if (isSelected) 3.dp else 1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.2f), CircleShape)
                                                                    .clickable { updateGlobalSubtitleStyle(globalSubtitleStyle.copy(fontColorArgb = colorVal)) },
                                                            )
                                                        }
                                                    }
                                                }

                                                // Background Opacity
                                                item {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically,
                                                    ) {
                                                        Text("Background Opacity", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                                                        Text("${(globalSubtitleStyle.bgOpacity * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                                    }
                                                    Slider(
                                                        value = globalSubtitleStyle.bgOpacity,
                                                        onValueChange = { updateGlobalSubtitleStyle(globalSubtitleStyle.copy(bgOpacity = it)) },
                                                        valueRange = 0f..1f,
                                                    )
                                                }

                                                // Edge Style
                                                item {
                                                    Text("Edge Style & Effects", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth().padding(top = MovieHubDimens.Spacing.xs),
                                                        horizontalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.xs),
                                                    ) {
                                                        listOf("None", "Outline", "Shadow").forEach { style ->
                                                            FilterChip(
                                                                selected = globalSubtitleStyle.edgeStyle == style,
                                                                onClick = { updateGlobalSubtitleStyle(globalSubtitleStyle.copy(edgeStyle = style)) },
                                                                label = { Text(style, style = MaterialTheme.typography.labelSmall) },
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        2 -> {
                                            val presets = listOf(
                                                SubtitlePreset("Netflix Style", SubtitleStyle(fontSizeSp = 18, fontColorArgb = 0xFFFFFFFF.toInt(), bgOpacity = 0.50f, edgeStyle = "None")),
                                                SubtitlePreset("Prime Video", SubtitleStyle(fontSizeSp = 16, fontColorArgb = 0xFFFFFFFF.toInt(), bgOpacity = 1.0f, edgeStyle = "Outline", outlineColorArgb = 0xFF000000.toInt())),
                                                SubtitlePreset("Disney+", SubtitleStyle(fontSizeSp = 18, fontFamily = "Serif", fontColorArgb = 0xFFFFFFFF.toInt(), bgOpacity = 0.30f, edgeStyle = "Shadow")),
                                                SubtitlePreset("High Contrast", SubtitleStyle(fontSizeSp = 20, fontFamily = "Monospace", fontColorArgb = 0xFFFFE600.toInt(), bgOpacity = 1.0f, isBold = true, edgeStyle = "None"))
                                            )
                                            LazyColumn(
                                                modifier = Modifier.fillMaxSize(),
                                                verticalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.xs),
                                            ) {
                                                presets.forEach { preset ->
                                                    item {
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .clip(RoundedCornerShape(MovieHubDimens.Radius.md))
                                                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                                                .clickable { updateGlobalSubtitleStyle(preset.style) }
                                                                .padding(MovieHubDimens.Spacing.md),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.SpaceBetween
                                                        ) {
                                                            Column {
                                                                Text(preset.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                                                                Text("System Subtitle Preset", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                                            }
                                                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        SettingsSubPage.STREAMING -> {
                            item { SectionHeader("Add-on scrape engines") }
                            item {
                                SettingsRow(
                                    icon = Icons.Default.Extension,
                                    title = "External Providers",
                                    subtitle = "Manage Stremio addons & plugins",
                                    onClick = onNavigateToAddons,
                                )
                            }
                            item { SectionHeader("Timing Preferences") }
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(MovieHubDimens.Radius.md))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                        .padding(MovieHubDimens.Spacing.md)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.sm)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Stream Timeout Limit", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                            Text("${playbackPrefs.streamTimeoutSeconds}s", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                        }
                                        Slider(
                                            value = playbackPrefs.streamTimeoutSeconds.toFloat(),
                                            onValueChange = { value ->
                                                scope.launch {
                                                    playbackPreferencesRepository.updatePreferences(
                                                        playbackPrefs.copy(streamTimeoutSeconds = value.toInt())
                                                    )
                                                }
                                            },
                                            valueRange = 5f..30f,
                                            steps = 25
                                        )
                                    }
                                }
                            }
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(MovieHubDimens.Radius.md))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                        .padding(MovieHubDimens.Spacing.md)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.sm)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Add-on Search Timeout", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                            Text("${playbackPrefs.addonTimeoutSeconds}s", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                        }
                                        Slider(
                                            value = playbackPrefs.addonTimeoutSeconds.toFloat(),
                                            onValueChange = { value ->
                                                scope.launch {
                                                    playbackPreferencesRepository.updatePreferences(
                                                        playbackPrefs.copy(addonTimeoutSeconds = value.toInt())
                                                    )
                                                }
                                            },
                                            valueRange = 5f..30f,
                                            steps = 25
                                        )
                                    }
                                }
                            }
                            item { SectionHeader("Streaming Preferences") }
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = MovieHubDimens.Spacing.xs)
                                        .clickable { settingsViewModel.onAction(SettingsAction.SetActiveSelectionDialog("providers")) },
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Preferred Providers", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                        Text("Restrict source results by provider", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                    }
                                    Text(playbackPrefs.preferredProviders, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                }
                            }
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = MovieHubDimens.Spacing.xs)
                                        .clickable { settingsViewModel.onAction(SettingsAction.SetActiveSelectionDialog("hls")) },
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("HLS Streaming Mode", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                        Text("Adaptive HLS preferences", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                    }
                                    Text(playbackPrefs.hlsPreferences, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                }
                            }
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = MovieHubDimens.Spacing.xs)
                                        .clickable { settingsViewModel.onAction(SettingsAction.SetActiveSelectionDialog("dash")) },
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("DASH Streaming Mode", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                        Text("Adaptive DASH preferences", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                    }
                                    Text(playbackPrefs.dashPreferences, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                }
                            }
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = MovieHubDimens.Spacing.xs)
                                        .clickable { settingsViewModel.onAction(SettingsAction.SetActiveSelectionDialog("fallback")) },
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Fallback Source Behavior", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                        Text("What to do if selected stream fails", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                    }
                                    Text(playbackPrefs.fallbackSourceBehavior, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        SettingsSubPage.SYNC -> {
                            item { SectionHeader("Cloud Sync Status") }
                            item {
                                SettingsRow(
                                    icon = Icons.Default.Sync,
                                    title = "Cloud Sync",
                                    subtitle = "Sync history & watchlists via Supabase",
                                    onClick = onNavigateToSync,
                                )
                            }
                            item { SectionHeader("Accounts Configuration") }
                            item {
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
                                        onConfigureClick = { settingsViewModel.onAction(SettingsAction.SetActiveConfigureProvider("trakt")) },
                                        onLinkClick = onNavigateToAuth
                                    )
                                    ProviderCard(
                                        name = "Real-Debrid",
                                        isConnected = isRdConnected,
                                        onConfigureClick = { settingsViewModel.onAction(SettingsAction.SetActiveConfigureProvider("realdebrid")) },
                                        onLinkClick = onNavigateToAuth
                                    )
                                    ProviderCard(
                                        name = "AllDebrid",
                                        isConnected = isAdConnected,
                                        onConfigureClick = { settingsViewModel.onAction(SettingsAction.SetActiveConfigureProvider("alldebrid")) }
                                    )
                                    ProviderCard(
                                        name = "Premiumize.me",
                                        isConnected = isPmConnected,
                                        onConfigureClick = { settingsViewModel.onAction(SettingsAction.SetActiveConfigureProvider("premiumize")) }
                                    )
                                }
                            }
                        }

                        SettingsSubPage.DOWNLOADS -> {
                            item { SectionHeader("Offline Downloads") }
                            item {
                                SettingsRow(
                                    icon = Icons.Default.Download,
                                    title = "Downloads",
                                    subtitle = "Manage offline downloads",
                                    onClick = onNavigateToDownloads,
                                )
                            }
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = MovieHubDimens.Spacing.xs)
                                        .clickable { settingsViewModel.onAction(SettingsAction.SetActiveSelectionDialog("downloadQuality")) },
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Download Resolution", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                        Text("Target quality when downloading streams", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                    }
                                    Text(playbackPrefs.downloadQuality, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                }
                            }
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = MovieHubDimens.Spacing.xs)
                                        .clickable { settingsViewModel.onAction(SettingsAction.SetActiveSelectionDialog("storage")) },
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Storage Location", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                        Text("Where offline video data is saved", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                    }
                                    Text(playbackPrefs.storageLocation, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                }
                            }
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = MovieHubDimens.Spacing.xxs),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Auto Cleanup Completed", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                        Text("Automatically delete finished movies/series from storage", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                    }
                                    Switch(
                                        checked = playbackPrefs.autoCleanup,
                                        onCheckedChange = { value ->
                                            scope.launch {
                                                playbackPreferencesRepository.updatePreferences(
                                                    playbackPrefs.copy(autoCleanup = value)
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(MovieHubDimens.Radius.md))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                        .padding(MovieHubDimens.Spacing.md)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.sm)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Max Concurrent Downloads", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                            Text("${playbackPrefs.concurrentDownloads}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                        }
                                        Slider(
                                            value = playbackPrefs.concurrentDownloads.toFloat(),
                                            onValueChange = { value ->
                                                scope.launch {
                                                    playbackPreferencesRepository.updatePreferences(
                                                        playbackPrefs.copy(concurrentDownloads = value.toInt())
                                                    )
                                                }
                                            },
                                            valueRange = 1f..5f,
                                            steps = 4
                                        )
                                    }
                                }
                            }
                            item { SectionHeader("Cache Optimization") }
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(MovieHubDimens.Radius.md))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                        .padding(MovieHubDimens.Spacing.md)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.sm)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Video Cache Size Limit", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                            Text("${playbackPrefs.cacheLimitMb} MB", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                        }
                                        Slider(
                                            value = playbackPrefs.cacheLimitMb.toFloat(),
                                            onValueChange = { value ->
                                                scope.launch {
                                                    playbackPreferencesRepository.updatePreferences(
                                                        playbackPrefs.copy(cacheLimitMb = value.toInt())
                                                    )
                                                }
                                            },
                                            valueRange = 128f..2048f,
                                            steps = 15
                                        )
                                    }
                                }
                            }
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(MovieHubDimens.Radius.md))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                        .padding(MovieHubDimens.Spacing.md),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Media Image Cache", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                                        Text("Active Size: 184.2 MB", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                    }
                                    Button(
                                        onClick = { settingsViewModel.onAction(SettingsAction.ShowFeedback("Image cache cleared successfully!")) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                            contentColor = MaterialTheme.colorScheme.primary
                                        ),
                                        shape = RoundedCornerShape(MovieHubDimens.Radius.sm)
                                    ) {
                                        Text("Clear Cache", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }

                        SettingsSubPage.ADVANCED -> {
                            item { SectionHeader("Metadata Enrichment") }
                            item {
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
                                            value = state.tmdbKeyInput,
                                            onValueChange = { settingsViewModel.onAction(SettingsAction.SetTmdbKeyInput(it)) },
                                            placeholder = {
                                                Text(
                                                    "Enter TMDB API Key",
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                                )
                                            },
                                            visualTransformation = if (state.showTmdbKey) VisualTransformation.None else PasswordVisualTransformation(),
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
                                            TextButton(onClick = { settingsViewModel.onAction(SettingsAction.ToggleShowTmdbKey) }) {
                                                Text(
                                                    text = if (state.showTmdbKey) "Hide" else "Show",
                                                    color = MaterialTheme.colorScheme.primary,
                                                )
                                            }
                                            Spacer(modifier = Modifier.weight(1f))
                                            Button(
                                                onClick = {
                                                    settingsViewModel.onAction(SettingsAction.SaveTmdbApiKey(state.tmdbKeyInput))
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

                            item { SectionHeader("Account Operations") }
                            item {
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
                        else -> {}
                    }
                }
            }
        }

        // Overlay Feedback SnackBar-like banner
        AnimatedVisibility(
            visible = state.feedbackMessage != null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(MovieHubDimens.Spacing.lg)
                    .clip(RoundedCornerShape(MovieHubDimens.Radius.md))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(MovieHubDimens.Radius.md))
                    .padding(MovieHubDimens.Spacing.md),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = state.feedbackMessage ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Provider Dialog Handler
        val providerToConfigure = state.activeConfigureProvider
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
                        settingsViewModel.onAction(SettingsAction.ShowFeedback("$providerToConfigure updated successfully!"))
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
                        settingsViewModel.onAction(SettingsAction.ShowFeedback("$providerToConfigure disconnected."))
                    }
                },
                onDismiss = { settingsViewModel.onAction(SettingsAction.SetActiveConfigureProvider(null)) }
            )
        }

        // Active Option Selection Dialog
        state.activeSelectionDialog?.let { dialogType ->
            val options = when (dialogType) {
                "resolution" -> listOf("Auto", "4K", "1080p", "720p", "SD")
                "codec" -> listOf("Auto", "HEVC/H.265", "AVC/H.264", "AV1")
                "speed" -> listOf("0.5", "0.75", "1.0", "1.25", "1.5", "1.75", "2.0")
                "providers" -> listOf("All", "RealDebrid Only", "AllDebrid Only")
                "hls" -> listOf("Auto", "HLS Only", "DASH Only")
                "dash" -> listOf("Auto", "HLS Only", "DASH Only")
                "fallback" -> listOf("Ask", "Auto-Select Next", "Secondary Only")
                "downloadQuality" -> listOf("1080p", "720p", "SD", "4K")
                "storage" -> listOf("Internal", "External SD Card")
                else -> emptyList()
            }
            val title = when (dialogType) {
                "resolution" -> "Select Preferred Quality"
                "codec" -> "Select Preferred Codec"
                "speed" -> "Select Playback Speed"
                "providers" -> "Select Preferred Providers"
                "hls" -> "Select HLS Preference"
                "dash" -> "Select DASH Preference"
                "fallback" -> "Select Fallback Behavior"
                "downloadQuality" -> "Select Download Quality"
                "storage" -> "Select Storage Location"
                else -> ""
            }
            val currentSelected = when (dialogType) {
                "resolution" -> playbackPrefs.preferredResolution
                "codec" -> playbackPrefs.preferredCodec
                "speed" -> playbackPrefs.defaultSpeed.toString()
                "providers" -> playbackPrefs.preferredProviders
                "hls" -> playbackPrefs.hlsPreferences
                "dash" -> playbackPrefs.dashPreferences
                "fallback" -> playbackPrefs.fallbackSourceBehavior
                "downloadQuality" -> playbackPrefs.downloadQuality
                "storage" -> playbackPrefs.storageLocation
                else -> ""
            }

            AlertDialog(
                onDismissRequest = { settingsViewModel.onAction(SettingsAction.SetActiveSelectionDialog(null)) },
                title = { Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
                text = {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.xs)) {
                        items(options.size) { index ->
                            val option = options[index]
                            val isSelected = option == currentSelected
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(MovieHubDimens.Radius.sm))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent)
                                    .clickable {
                                        scope.launch {
                                            val updated = when (dialogType) {
                                                "resolution" -> playbackPrefs.copy(preferredResolution = option)
                                                "codec" -> playbackPrefs.copy(preferredCodec = option)
                                                "speed" -> playbackPrefs.copy(defaultSpeed = option.toFloat())
                                                "providers" -> playbackPrefs.copy(preferredProviders = option)
                                                "hls" -> playbackPrefs.copy(hlsPreferences = option)
                                                "dash" -> playbackPrefs.copy(dashPreferences = option)
                                                "fallback" -> playbackPrefs.copy(fallbackSourceBehavior = option)
                                                "downloadQuality" -> playbackPrefs.copy(downloadQuality = option)
                                                "storage" -> playbackPrefs.copy(storageLocation = option)
                                                else -> playbackPrefs
                                            }
                                            playbackPreferencesRepository.updatePreferences(updated)
                                            settingsViewModel.onAction(SettingsAction.SetActiveSelectionDialog(null))
                                        }
                                    }
                                    .padding(horizontal = MovieHubDimens.Spacing.md, vertical = MovieHubDimens.Spacing.md),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(option, style = MaterialTheme.typography.bodyLarge, color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { settingsViewModel.onAction(SettingsAction.SetActiveSelectionDialog(null)) }) {
                        Text("Cancel")
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                textContentColor = MaterialTheme.colorScheme.onSurface,
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
        delay(((index * 50L).coerceAtMost(300)).milliseconds)
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
