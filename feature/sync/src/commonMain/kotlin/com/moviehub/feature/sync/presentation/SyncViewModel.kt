package com.moviehub.feature.sync.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.moviehub.core.database.ProfileRepository
import com.moviehub.core.database.UserPreferencesDao
import com.moviehub.feature.sync.SyncManager
import com.moviehub.feature.sync.SyncState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.json.Json

class SyncViewModel(
    private val syncManager: SyncManager,
    private val userPreferencesDao: UserPreferencesDao,
    private val profileRepository: ProfileRepository,
    private val json: Json,
) : ViewModel() {

    private val logger = Logger.withTag("SyncViewModel")

    val syncState: StateFlow<SyncState> = syncManager.syncState

    val isAccountConnected: StateFlow<Boolean> = combine(
        profileRepository.activeProfile,
        userPreferencesDao.getAllPreferences()
    ) { activeProfile, allPrefsList ->
        val profileId = activeProfile?.id ?: return@combine false
        val prefs = allPrefsList.find { it.profileId == profileId }
        val credentialsMap = if (prefs != null && prefs.debridApiKey.isNotBlank()) {
            val fullKey = prefs.debridApiKey
            if (fullKey.startsWith("{") || fullKey.startsWith("[")) {
                runCatching {
                    json.decodeFromString<Map<String, String>>(fullKey)
                }.getOrElse {
                    logger.w { "Failed to parse debrid API key JSON: ${it.message}" }
                    emptyMap()
                }
            } else {
                mapOf("realdebrid" to fullKey)
            }
        } else {
            emptyMap()
        }
        val isRdConnected = credentialsMap["realdebrid"]?.isNotBlank() ?: false
        val isAdConnected = credentialsMap["alldebrid"]?.isNotBlank() ?: false
        val isPmConnected = credentialsMap["premiumize"]?.isNotBlank() ?: false
        val isTraktConnected = credentialsMap["trakt_access_token"]?.isNotBlank() ?: false
        isRdConnected || isAdConnected || isPmConnected || isTraktConnected
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    fun onSyncNow() {
        syncManager.triggerSync()
    }
}
