package com.moviehub.feature.addon.presentation

import com.moviehub.core.model.StremioManifest
import androidx.compose.runtime.Immutable

@Immutable
data class AddonState(
    val addonUrl: String = "",
    val isInstalling: Boolean = false,
    val installedAddons: List<StremioManifest> = emptyList(),
    val addonUrls: Map<String, String> = emptyMap(),
    val error: String? = null,
    val successMessage: String? = null
)

sealed interface AddonAction {
    data class UrlChanged(val url: String) : AddonAction
    object InstallAddon : AddonAction
    object LoadInstalledAddons : AddonAction
    data class RemoveAddon(val addonId: String) : AddonAction
    data class RefreshAddon(val addonId: String) : AddonAction
    data class ConfigureAddon(val addonId: String) : AddonAction
}

sealed interface AddonEvent {
    data class OpenUrl(val url: String) : AddonEvent
}
