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
    val preferredResolution: String = "Auto", // "Auto", "4K", "1080p", "720p", "SD"
    val preferredAudioLanguage: String? = null,
    val preferredSubtitleLanguage: String? = null,
    val subtitlesEnabled: Boolean = true,
    val preferredVideoHeight: Int = 0,

    // Expanded Playback Settings
    val preferredCodec: String = "Auto", // "Auto", "HEVC/H.265", "AVC/H.264", "AV1"
    val autoFrameRate: Boolean = false,
    val resumeThresholdPercent: Int = 90,
    val skipIntroSeconds: Int = 0, // 0 = ask/disabled, or auto skip
    val skipCreditsSeconds: Int = 0,

    // Expanded Streaming Settings
    val streamTimeoutSeconds: Int = 15,
    val addonTimeoutSeconds: Int = 10,
    val hlsPreferences: String = "Auto", // "Auto", "HLS Only", "DASH Only"
    val dashPreferences: String = "Auto",
    val fallbackSourceBehavior: String = "Ask", // "Ask", "Auto-Select Next", "Secondary Only"
    val preferredProviders: String = "All", // "All", "RealDebrid Only", "AllDebrid Only"

    // Expanded Download Settings
    val concurrentDownloads: Int = 2,
    val downloadQuality: String = "1080p",
    val storageLocation: String = "Internal",
    val autoCleanup: Boolean = true,
    val cacheLimitMb: Int = 512,

    // Expanded General Settings
    val themeBehavior: String = "System",
    val dynamicColors: Boolean = true,
    val appAppearance: String = "Nuvio Dark",
    val regionalSettings: String = "Default",
    val performanceSettings: String = "High"
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
