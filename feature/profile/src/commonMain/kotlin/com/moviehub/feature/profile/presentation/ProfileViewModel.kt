package com.moviehub.feature.profile.presentation

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.moviehub.core.database.ProfileRepository
import com.moviehub.core.model.Profile
import com.moviehub.core.network.AddonManager
import com.moviehub.core.utils.PerformanceMonitor
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
data class ProfileUiState(
    val profiles: List<Profile> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

class ProfileViewModel(
    private val profileRepository: ProfileRepository,
    private val addonManager: AddonManager,
) : ViewModel() {

    private val logger = Logger.withTag("ProfileViewModel")

    val uiState: StateFlow<ProfileUiState> = profileRepository.profiles
        .map { profiles ->
            ProfileUiState(profiles = profiles, isLoading = false)
        }
        .catch { e ->
            emit(ProfileUiState(profiles = emptyList(), isLoading = false, error = e.message))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ProfileUiState(isLoading = true),
        )

    val activeProfile: StateFlow<Profile?> = profileRepository.activeProfile

    fun selectProfile(profile: Profile) {
        viewModelScope.launch {
            PerformanceMonitor.beginSection("VM:Profile:selectProfile")
            try {
                profileRepository.setActiveProfile(profile)
            } catch (e: Exception) {
                logger.e(e) { "Failed to select profile ${profile.id}" }
            } finally {
                PerformanceMonitor.endSection()
            }
        }
    }

    fun createProfile(name: String, cloneFromActive: Boolean = false) {
        viewModelScope.launch {
            PerformanceMonitor.beginSection("VM:Profile:createProfile")
            try {
                val currentActive = profileRepository.activeProfile.value
                val newProfile = profileRepository.createProfile(name)

                val cinemetaManifest = com.moviehub.core.model.StremioManifest(
                    id = "org.stremio.cinemeta",
                    version = "3.0.4",
                    name = "Cinemeta",
                    description = "Official Stremio metadata addon for movies and series.",
                    types = listOf("movie", "series"),
                    resources = listOf("catalog", "meta"),
                    catalogs = listOf(
                        com.moviehub.core.model.StremioCatalog(
                            type = "movie",
                            id = "top",
                            name = "Movies",
                            extra = listOf(
                                com.moviehub.core.model.StremioExtra(name = "search", isRequired = false),
                                com.moviehub.core.model.StremioExtra(name = "genre", isRequired = false)
                            )
                        ),
                        com.moviehub.core.model.StremioCatalog(
                            type = "series",
                            id = "top",
                            name = "Series",
                            extra = listOf(
                                com.moviehub.core.model.StremioExtra(name = "search", isRequired = false),
                                com.moviehub.core.model.StremioExtra(name = "genre", isRequired = false)
                            )
                        )
                    )
                )

                if (cloneFromActive && currentActive != null) {
                    // Addon Inheritance Twist: Clone addons from active profile
                    val activeAddons = addonManager.installedAddons.value
                    activeAddons.forEach { manifest ->
                        val url = addonManager.getAddonUrl(manifest.id)
                        if (url != null) {
                            // Temporarily set active profile to new one to add addons
                            profileRepository.setActiveProfile(newProfile)
                            addonManager.addAddon(url, manifest)
                        }
                    }
                    // Switch back to new profile
                    profileRepository.setActiveProfile(newProfile)
                } else {
                    // Seed standard keyless Cinemeta addon so new profiles have instant content and search working out of the box
                    profileRepository.setActiveProfile(newProfile)
                    try {
                        addonManager.addAddon("https://v3-cinemeta.strem.io", cinemetaManifest)
                    } catch (e: Exception) {
                        logger.e(e) { "Failed to seed Cinemeta addon for new profile ${newProfile.id}" }
                    }
                }
            } finally {
                PerformanceMonitor.endSection()
            }
        }
    }
}
