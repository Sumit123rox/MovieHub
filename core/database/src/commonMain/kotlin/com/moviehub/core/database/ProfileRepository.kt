package com.moviehub.core.database

import com.moviehub.core.model.Profile
import com.moviehub.core.utils.currentTimeMillis
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

class ProfileRepository(private val profileDao: ProfileDao) {
    private val _activeProfile = MutableStateFlow<Profile?>(null)
    val activeProfile: StateFlow<Profile?> = _activeProfile.asStateFlow()

    val profiles: Flow<List<Profile>> = profileDao.getAllProfiles()
        .map { entities -> entities.map { it.toExternalModel() } }

    suspend fun setActiveProfile(profile: Profile?) {
        _activeProfile.value = profile
    }

    suspend fun createProfile(name: String, avatarUrl: String? = null, pin: String? = null, isChild: Boolean = false): Profile {
        val now = currentTimeMillis()
        val profile = Profile(
            id = now.toString(), // Simple ID generation
            name = name,
            avatarUrl = avatarUrl,
            pin = pin,
            isChild = isChild,
            createdAt = now,
        )
        profileDao.insertProfile(profile.toEntity())
        return profile
    }

    suspend fun deleteProfile(profile: Profile) {
        profileDao.deleteProfile(profile.toEntity())
        if (_activeProfile.value?.id == profile.id) {
            _activeProfile.value = null
        }
    }
}
