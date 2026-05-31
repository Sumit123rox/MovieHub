package com.moviehub.core.database

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json

class TraktSettingsRepository(
    private val userPreferencesDao: UserPreferencesDao,
    private val profileRepository: ProfileRepository,
) {
    private val _isConfigured = MutableStateFlow(false)
    val isConfigured: Flow<Boolean> = _isConfigured.asStateFlow()

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun getAccessToken(): String {
        return getCredential("trakt_access_token")
    }

    suspend fun setAccessToken(token: String) {
        setCredential("trakt_access_token", token)
        _isConfigured.value = token.isNotBlank()
    }

    suspend fun getRefreshToken(): String {
        return getCredential("trakt_refresh_token")
    }

    suspend fun setRefreshToken(token: String) {
        setCredential("trakt_refresh_token", token)
    }

    private suspend fun getCredential(key: String): String {
        val profileId = profileRepository.activeProfile.value?.id ?: return ""
        val prefs = userPreferencesDao.getPreference(profileId)
        val fullKey = prefs?.debridApiKey ?: ""
        if (fullKey.startsWith("{") || fullKey.startsWith("[")) {
            return runCatching {
                val map = json.decodeFromString<Map<String, String>>(fullKey)
                map[key] ?: ""
            }.getOrDefault("")
        }
        return ""
    }

    private suspend fun setCredential(key: String, value: String) {
        val profileId = profileRepository.activeProfile.value?.id ?: return
        val current = userPreferencesDao.getPreference(profileId)
        val fullKey = current?.debridApiKey ?: ""
        val map = if (fullKey.startsWith("{") || fullKey.startsWith("[")) {
            runCatching {
                json.decodeFromString<Map<String, String>>(fullKey).toMutableMap()
            }.getOrElse { mutableMapOf() }
        } else {
            mutableMapOf("realdebrid" to fullKey)
        }
        map[key] = value.trim()
        val jsonStr = json.encodeToString(map)
        val updated = (current ?: UserPreferencesEntity(profileId = profileId)).copy(
            debridApiKey = jsonStr,
        )
        userPreferencesDao.setPreference(updated)
    }

    suspend fun refresh() {
        val token = getAccessToken()
        _isConfigured.value = token.isNotBlank()
    }
}
