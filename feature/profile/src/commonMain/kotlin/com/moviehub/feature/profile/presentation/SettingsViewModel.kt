package com.moviehub.feature.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.moviehub.core.database.DebridSettingsRepository
import com.moviehub.core.database.PlaybackPreferencesRepository
import com.moviehub.core.database.ProfileRepository
import com.moviehub.core.database.TmdbSettingsRepository
import com.moviehub.core.database.TraktSettingsRepository
import com.moviehub.core.database.UserPreferencesDao
import com.moviehub.core.database.UserPreferencesEntity
import com.moviehub.core.model.SubtitleStyle
import com.moviehub.core.utils.PerformanceMonitor
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlin.time.Duration.Companion.milliseconds

class SettingsViewModel(
    private val userPreferencesDao: UserPreferencesDao,
    private val tmdbSettingsRepository: TmdbSettingsRepository,
    private val debridSettingsRepository: DebridSettingsRepository,
    private val traktSettingsRepository: TraktSettingsRepository,
    private val playbackPreferencesRepository: PlaybackPreferencesRepository,
    private val profileRepository: ProfileRepository,
    private val json: Json,
) : ViewModel() {

    private val logger = Logger.withTag("SettingsViewModel")
    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    fun onAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.NavigateToSubPage -> {
                _state.value = _state.value.copy(currentSubPage = action.page)
            }
            is SettingsAction.SetTmdbKeyInput -> {
                _state.value = _state.value.copy(tmdbKeyInput = action.value)
            }
            is SettingsAction.ToggleShowTmdbKey -> {
                _state.value = _state.value.copy(showTmdbKey = !_state.value.showTmdbKey)
            }
            is SettingsAction.SetActiveConfigureProvider -> {
                _state.value = _state.value.copy(activeConfigureProvider = action.provider)
            }
            is SettingsAction.SetActiveSelectionDialog -> {
                _state.value = _state.value.copy(activeSelectionDialog = action.dialog)
            }
            is SettingsAction.SetSelectedSubtitleTab -> {
                _state.value = _state.value.copy(selectedSubtitleTab = action.tab)
            }
            is SettingsAction.ShowFeedback -> {
                _state.value = _state.value.copy(feedbackMessage = action.message)
                viewModelScope.launch {
                    delay(2000.milliseconds)
                    if (_state.value.feedbackMessage == action.message) {
                        _state.value = _state.value.copy(feedbackMessage = null)
                    }
                }
            }
            is SettingsAction.DismissFeedback -> {
                _state.value = _state.value.copy(feedbackMessage = null)
            }
            is SettingsAction.UpdateSeekIncrement -> {
                updatePreference { it.copy(seekIncrement = action.value.coerceIn(5, 60)) }
            }
            is SettingsAction.UpdatePlaybackPreference -> {
                viewModelScope.launch {
                    PerformanceMonitor.beginSection("VM:Settings:updatePlaybackPref")
                    try {
                        val current = try {
                            playbackPreferencesRepository.getPreferencesFlow().first()
                        } catch (_: Exception) {
                            com.moviehub.core.database.PlaybackPreferences()
                        }
                        playbackPreferencesRepository.updatePreferences(action.update(current))
                    } catch (e: Exception) {
                        logger.e(e) { "Failed to update playback preference" }
                    } finally {
                        PerformanceMonitor.endSection()
                    }
                }
            }
            is SettingsAction.UpdateUserPreference -> {
                updatePreference { action.update(it) }
            }
            is SettingsAction.SaveTmdbApiKey -> {
                viewModelScope.launch {
                    PerformanceMonitor.beginSection("VM:Settings:saveTmdbKey")
                    try {
                        tmdbSettingsRepository.setApiKey(action.key)
                        _state.value = _state.value.copy(
                            tmdbKeyInput = "",
                            feedbackMessage = "TMDB API key saved!",
                        )
                        dismissFeedbackAfterDelay()
                    } catch (e: Exception) {
                        logger.e(e) { "Failed to save TMDB API key" }
                        _state.value = _state.value.copy(feedbackMessage = "Failed to save key: ${e.message}")
                    } finally {
                        PerformanceMonitor.endSection()
                    }
                }
            }
            is SettingsAction.SaveDebridApiKey -> {
                viewModelScope.launch {
                    PerformanceMonitor.beginSection("VM:Settings:saveDebridKey")
                    try {
                        val profileId = profileRepository.activeProfile.value?.id ?: return@launch
                        val currentPrefs = userPreferencesDao.getPreference(profileId) ?: UserPreferencesEntity(profileId)
                        val fullKey = currentPrefs.debridApiKey
                        val credentialsMap: MutableMap<String, String> = if (fullKey.startsWith("{") || fullKey.startsWith("[")) {
                            runCatching { json.decodeFromString<MutableMap<String, String>>(fullKey) }
                                .getOrElse { mutableMapOf() }
                        } else if (fullKey.isNotBlank()) {
                            mutableMapOf("realdebrid" to fullKey)
                        } else {
                            mutableMapOf()
                        }
                        credentialsMap[action.provider] = action.key
                        val newKey = json.encodeToString(
                            MapSerializer(
                                serializer<String>(),
                                serializer<String>(),
                            ),
                            credentialsMap,
                        )
                        userPreferencesDao.setPreference(currentPrefs.copy(debridApiKey = newKey))
                        _state.value = _state.value.copy(
                            activeConfigureProvider = null,
                            feedbackMessage = "${action.provider.replaceFirstChar { it.uppercase() }} API key saved!",
                        )
                        dismissFeedbackAfterDelay()
                    } catch (e: Exception) {
                        logger.e(e) { "Failed to save debrid key for ${action.provider}" }
                        _state.value = _state.value.copy(feedbackMessage = "Failed to save key: ${e.message}")
                    } finally {
                        PerformanceMonitor.endSection()
                    }
                }
            }
            is SettingsAction.SaveTraktCode -> {
                viewModelScope.launch {
                    PerformanceMonitor.beginSection("VM:Settings:saveTraktCode")
                    try {
                        val profileId = profileRepository.activeProfile.value?.id ?: return@launch
                        val currentPrefs = userPreferencesDao.getPreference(profileId) ?: UserPreferencesEntity(profileId)
                        val fullKey = currentPrefs.debridApiKey
                        val credentialsMap: MutableMap<String, String> = if (fullKey.startsWith("{") || fullKey.startsWith("[")) {
                            runCatching { json.decodeFromString<MutableMap<String, String>>(fullKey) }
                                .getOrElse { mutableMapOf() }
                        } else if (fullKey.isNotBlank()) {
                            mutableMapOf("realdebrid" to fullKey)
                        } else {
                            mutableMapOf()
                        }
                        credentialsMap["trakt_access_token"] = action.code
                        val newKey = json.encodeToString(
                            MapSerializer(
                                serializer<String>(),
                                serializer<String>(),
                            ),
                            credentialsMap,
                        )
                        userPreferencesDao.setPreference(currentPrefs.copy(debridApiKey = newKey))
                        _state.value = _state.value.copy(
                            activeConfigureProvider = null,
                            feedbackMessage = "Trakt code saved!",
                        )
                        dismissFeedbackAfterDelay()
                    } catch (e: Exception) {
                        logger.e(e) { "Failed to save Trakt code" }
                        _state.value = _state.value.copy(feedbackMessage = "Failed to save code: ${e.message}")
                    } finally {
                        PerformanceMonitor.endSection()
                    }
                }
            }
            is SettingsAction.ClearDebridKey -> {
                viewModelScope.launch {
                    PerformanceMonitor.beginSection("VM:Settings:clearDebridKey")
                    try {
                        val profileId = profileRepository.activeProfile.value?.id ?: return@launch
                        val currentPrefs = userPreferencesDao.getPreference(profileId) ?: UserPreferencesEntity(profileId)
                        val fullKey = currentPrefs.debridApiKey
                        val credentialsMap: MutableMap<String, String> = if (fullKey.startsWith("{") || fullKey.startsWith("[")) {
                            runCatching { json.decodeFromString<MutableMap<String, String>>(fullKey) }
                                .getOrElse { mutableMapOf() }
                        } else if (fullKey.isNotBlank()) {
                            mutableMapOf("realdebrid" to fullKey)
                        } else {
                            mutableMapOf()
                        }
                        credentialsMap.remove(action.provider)
                        val newKey = json.encodeToString(
                            MapSerializer(
                                serializer<String>(),
                                serializer<String>(),
                            ),
                            credentialsMap,
                        )
                        userPreferencesDao.setPreference(currentPrefs.copy(debridApiKey = newKey))
                        _state.value = _state.value.copy(
                            feedbackMessage = "${action.provider.replaceFirstChar { it.uppercase() }} key removed.",
                        )
                        dismissFeedbackAfterDelay()
                    } catch (e: Exception) {
                        logger.e(e) { "Failed to clear debrid key for ${action.provider}" }
                    } finally {
                        PerformanceMonitor.endSection()
                    }
                }
            }
            is SettingsAction.UpdateGlobalSubtitleStyle -> {
                viewModelScope.launch {
                    PerformanceMonitor.beginSection("VM:Settings:updateSubtitleStyle")
                    try {
                        val profileId = profileRepository.activeProfile.value?.id ?: return@launch
                        val currentPrefs = userPreferencesDao.getPreference(profileId) ?: UserPreferencesEntity(profileId)
                        val styleJson = json.encodeToString(action.style)
                        userPreferencesDao.setPreference(currentPrefs.copy(subtitleStyleJson = styleJson))
                    } catch (e: Exception) {
                        logger.e(e) { "Failed to save subtitle style" }
                    } finally {
                        PerformanceMonitor.endSection()
                    }
                }
            }
            is SettingsAction.ExportSubtitlePreset -> {
                viewModelScope.launch {
                    val styleJson = json.encodeToString(action.style)
                    _state.value = _state.value.copy(
                        feedbackMessage = "Subtitle preset exported: $styleJson",
                    )
                    dismissFeedbackAfterDelay()
                }
            }
        }
    }

    private fun updatePreference(update: (UserPreferencesEntity) -> UserPreferencesEntity) {
        viewModelScope.launch {
            PerformanceMonitor.beginSection("VM:Settings:updateUserPref")
            try {
                val profileId = profileRepository.activeProfile.value?.id ?: return@launch
                val currentPrefs = userPreferencesDao.getPreference(profileId) ?: UserPreferencesEntity(profileId)
                userPreferencesDao.setPreference(update(currentPrefs))
            } catch (e: Exception) {
                logger.e(e) { "Failed to update user preference" }
            } finally {
                PerformanceMonitor.endSection()
            }
        }
    }

    private fun dismissFeedbackAfterDelay() {
        viewModelScope.launch {
            delay(3000.milliseconds)
            _state.value = _state.value.copy(feedbackMessage = null)
        }
    }
}
