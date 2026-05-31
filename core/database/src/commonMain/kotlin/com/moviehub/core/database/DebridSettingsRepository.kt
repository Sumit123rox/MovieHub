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

    private val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun getApiKey(): String {
        val profileId = profileRepository.activeProfile.value?.id ?: return ""
        val prefs = userPreferencesDao.getPreference(profileId)
        return prefs?.debridApiKey ?: ""
    }

    suspend fun getApiKey(provider: String): String {
        val fullKey = getApiKey()
        if (fullKey.startsWith("{") || fullKey.startsWith("[")) {
            return runCatching {
                val map = json.decodeFromString<Map<String, String>>(fullKey)
                map[provider.lowercase()] ?: ""
            }.getOrDefault("")
        } else {
            // Legacy key is assumed to be RealDebrid
            if (provider.lowercase() == "realdebrid") return fullKey
            return ""
        }
    }

    suspend fun setApiKey(key: String) {
        val profileId = profileRepository.activeProfile.value?.id ?: return
        val current = userPreferencesDao.getPreference(profileId)
        val updated = (current ?: UserPreferencesEntity(profileId = profileId)).copy(
            debridApiKey = key.trim(),
        )
        userPreferencesDao.setPreference(updated)
        _isAuthenticated.value = key.isNotBlank()
    }

    suspend fun setApiKey(provider: String, key: String) {
        val fullKey = getApiKey()
        val map = if (fullKey.startsWith("{") || fullKey.startsWith("[")) {
            runCatching {
                json.decodeFromString<Map<String, String>>(fullKey).toMutableMap()
            }.getOrElse { mutableMapOf() }
        } else {
            mutableMapOf("realdebrid" to fullKey)
        }
        map[provider.lowercase()] = key.trim()
        val jsonStr = json.encodeToString(map)
        setApiKey(jsonStr)
    }

    suspend fun isProviderConfigured(provider: String): Boolean {
        return getApiKey(provider).isNotBlank()
    }

    suspend fun refresh() {
        val key = getApiKey()
        _isAuthenticated.value = key.isNotBlank()
    }
}
