package com.moviehub.core.database

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class DebridSettingsRepository(
    private val userPreferencesDao: UserPreferencesDao,
    private val profileRepository: ProfileRepository,
) {
    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: Flow<Boolean> = _isAuthenticated.asStateFlow()

    suspend fun getApiKey(): String {
        val profileId = profileRepository.activeProfile.value?.id ?: return ""
        val prefs = userPreferencesDao.getPreference(profileId)
        return prefs?.debridApiKey ?: ""
    }

    suspend fun setApiKey(key: String) {
        val profileId = profileRepository.activeProfile.value?.id ?: return
        val current = userPreferencesDao.getPreference(profileId)
        val updated = (current ?: UserPreferencesEntity(profileId = profileId)).copy(
            debridApiKey = key.trim()
        )
        userPreferencesDao.setPreference(updated)
        _isAuthenticated.value = key.isNotBlank()
    }

    suspend fun refresh() {
        val key = getApiKey()
        _isAuthenticated.value = key.isNotBlank()
    }
}
