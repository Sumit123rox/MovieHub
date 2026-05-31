package com.moviehub.core.database

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class PlaybackPreferences(
    val defaultSpeed: Float = 1.0f,
    val autoPlayNext: Boolean = true,
    val resumePlayback: Boolean = true,
    val preferredResolution: String = "Auto" // "Auto", "4K", "1080p", "720p", "SD"
)

class PlaybackPreferencesRepository(
    private val userPreferencesDao: UserPreferencesDao,
    private val profileRepository: ProfileRepository,
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun getPreferencesFlow(): Flow<PlaybackPreferences> {
        return profileRepository.activeProfile.map { profile ->
            if (profile == null) {
                PlaybackPreferences()
            } else {
                val prefs = userPreferencesDao.getPreference(profile.id)
                parsePreferences(prefs?.playbackPreferencesJson)
            }
        }
    }

    suspend fun getPreferences(): PlaybackPreferences {
        val profileId = profileRepository.activeProfile.value?.id ?: return PlaybackPreferences()
        val prefs = userPreferencesDao.getPreference(profileId)
        return parsePreferences(prefs?.playbackPreferencesJson)
    }

    suspend fun updatePreferences(updated: PlaybackPreferences) {
        val profileId = profileRepository.activeProfile.value?.id ?: return
        val current = userPreferencesDao.getPreference(profileId)
        val jsonStr = json.encodeToString(PlaybackPreferences.serializer(), updated)
        val updatedEntity = (current ?: UserPreferencesEntity(profileId = profileId)).copy(
            playbackPreferencesJson = jsonStr
        )
        userPreferencesDao.setPreference(updatedEntity)
    }

    private fun parsePreferences(jsonStr: String?): PlaybackPreferences {
        if (jsonStr.isNullOrBlank()) return PlaybackPreferences()
        return runCatching {
            json.decodeFromString(PlaybackPreferences.serializer(), jsonStr)
        }.getOrElse { PlaybackPreferences() }
    }
}
