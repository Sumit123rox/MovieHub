package com.moviehub.feature.profile.presentation

import androidx.compose.runtime.Immutable

@Immutable
data class SettingsState(
    val currentSubPage: SettingsSubPage? = null,
    val tmdbKeyInput: String = "",
    val showTmdbKey: Boolean = false,
    val activeConfigureProvider: String? = null,
    val activeSelectionDialog: String? = null,
    val feedbackMessage: String? = null,
    val selectedSubtitleTab: Int = 0,
)

sealed interface SettingsAction {
    data class NavigateToSubPage(val page: SettingsSubPage?) : SettingsAction
    data class SetTmdbKeyInput(val value: String) : SettingsAction
    data object ToggleShowTmdbKey : SettingsAction
    data class SetActiveConfigureProvider(val provider: String?) : SettingsAction
    data class SetActiveSelectionDialog(val dialog: String?) : SettingsAction
    data class SetSelectedSubtitleTab(val tab: Int) : SettingsAction
    data class ShowFeedback(val message: String) : SettingsAction
    data object DismissFeedback : SettingsAction

    // Preference mutations (delegated from UI to ViewModel for testability)
    data class UpdateSeekIncrement(val value: Int) : SettingsAction
    data class UpdatePlaybackPreference(val update: (com.moviehub.core.database.PlaybackPreferences) -> com.moviehub.core.database.PlaybackPreferences) : SettingsAction
    data class UpdateUserPreference(val update: (com.moviehub.core.database.UserPreferencesEntity) -> com.moviehub.core.database.UserPreferencesEntity) : SettingsAction
    data class SaveTmdbApiKey(val key: String) : SettingsAction
    data class SaveDebridApiKey(val provider: String, val key: String) : SettingsAction
    data class SaveTraktCode(val code: String) : SettingsAction
    data class ClearDebridKey(val provider: String) : SettingsAction
    data class UpdateGlobalSubtitleStyle(val style: com.moviehub.core.model.SubtitleStyle) : SettingsAction
    data class ExportSubtitlePreset(val style: com.moviehub.core.model.SubtitleStyle) : SettingsAction
}
