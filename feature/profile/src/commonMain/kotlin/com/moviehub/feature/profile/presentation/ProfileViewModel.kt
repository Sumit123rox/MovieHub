package com.moviehub.feature.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moviehub.core.database.ProfileRepository
import com.moviehub.core.model.Profile
import com.moviehub.core.network.AddonManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProfileUiState(
    val profiles: List<Profile> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class ProfileViewModel(
    private val profileRepository: ProfileRepository,
    private val addonManager: AddonManager
) : ViewModel() {

    val uiState: StateFlow<ProfileUiState> = profileRepository.profiles
        .map { profiles ->
            ProfileUiState(profiles = profiles, isLoading = false)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ProfileUiState(isLoading = true)
        )

    val activeProfile: StateFlow<Profile?> = profileRepository.activeProfile

    fun selectProfile(profile: Profile) {
        viewModelScope.launch {
            profileRepository.setActiveProfile(profile)
        }
    }

    fun createProfile(name: String, cloneFromActive: Boolean = false) {
        viewModelScope.launch {
            val currentActive = profileRepository.activeProfile.value
            val newProfile = profileRepository.createProfile(name)
            
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
            } else if (profileRepository.activeProfile.value == null) {
                profileRepository.setActiveProfile(newProfile)
            }
        }
    }
}
