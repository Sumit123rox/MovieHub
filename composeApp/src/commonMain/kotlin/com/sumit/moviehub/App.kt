package com.sumit.moviehub

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.moviehub.core.database.ProfileRepository
import com.moviehub.core.database.UserPreferencesDao
import com.moviehub.core.database.UserPreferencesEntity
import com.moviehub.core.ui.theme.AccentType
import com.moviehub.core.ui.theme.MovieHubTheme
import com.moviehub.core.ui.theme.ThemeType
import com.moviehub.navigation.RootNavGraph
import io.kamel.core.config.KamelConfig
import io.kamel.core.config.httpUrlFetcher
import io.kamel.core.config.takeFrom
import io.kamel.image.config.Default
import io.kamel.image.config.LocalKamelConfig
import kotlinx.coroutines.flow.catch
import org.koin.compose.koinInject

val movieHubKamelConfig = KamelConfig {
    takeFrom(KamelConfig.Default)
    imageBitmapCacheSize = 500
    imageVectorCacheSize = 100
}

@Composable
fun App() {
    val userPreferencesDao: UserPreferencesDao = koinInject()
    val profileRepository: ProfileRepository = koinInject()
    val activeProfile by profileRepository.activeProfile.collectAsState()

    // Read all preferences — use active profile's prefs when logged in,
    // or fall back to any stored preference (for profile selection screen)
    val allPrefs by userPreferencesDao.getAllPreferences()
        .catch { emit(emptyList()) }
        .collectAsState(initial = emptyList())

    val prefs: UserPreferencesEntity? = if (activeProfile != null) {
        allPrefs.find { it.profileId == activeProfile?.id }
    } else {
        allPrefs.firstOrNull()
    }

    val themeType = try {
        ThemeType.valueOf(prefs?.theme?.uppercase() ?: "NUVIO_DARK")
    } catch (_: Exception) {
        ThemeType.NUVIO_DARK
    }

    val accentType = try {
        AccentType.valueOf(prefs?.accentColor?.uppercase() ?: "BLUE")
    } catch (_: Exception) {
        AccentType.BLUE
    }

    CompositionLocalProvider(LocalKamelConfig provides movieHubKamelConfig) {
        MovieHubTheme(themeType = themeType, accentType = accentType) {
            RootNavGraph()
        }
    }
}
