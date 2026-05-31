package com.moviehub.core.database

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class TmdbSettingsRepository(
    private val userPreferencesDao: UserPreferencesDao,
    private val profileRepository: ProfileRepository,
) {
    private val _isConfigured = MutableStateFlow(false)
    val isConfigured: Flow<Boolean> = _isConfigured.asStateFlow()

    suspend fun getApiKey(): String {
        val profileId = profileRepository.activeProfile.value?.id ?: return ""
        val prefs = userPreferencesDao.getPreference(profileId)
        return prefs?.tmdbApiKey ?: ""
    }

    suspend fun setApiKey(key: String) {
        val profileId = profileRepository.activeProfile.value?.id ?: return
        val current = userPreferencesDao.getPreference(profileId)
        val updated = (current ?: UserPreferencesEntity(profileId = profileId)).copy(
            tmdbApiKey = key.trim(),
        )
        userPreferencesDao.setPreference(updated)
        _isConfigured.value = key.isNotBlank()
    }

    suspend fun refresh() {
        val key = getApiKey()
        _isConfigured.value = key.isNotBlank()
    }
}
