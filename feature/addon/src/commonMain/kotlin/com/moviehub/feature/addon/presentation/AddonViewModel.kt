package com.moviehub.feature.addon.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moviehub.feature.addon.data.AddonRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AddonViewModel(private val repository: AddonRepository) : ViewModel() {
    private val _state = MutableStateFlow(AddonState())
    val state: StateFlow<AddonState> = _state.asStateFlow()

    private val _event = MutableSharedFlow<AddonEvent>()
    val event: SharedFlow<AddonEvent> = _event.asSharedFlow()

    init {
        onAction(AddonAction.LoadInstalledAddons)
    }

    fun onAction(action: AddonAction) {
        when (action) {
            is AddonAction.UrlChanged -> {
                _state.value = _state.value.copy(addonUrl = action.url)
            }
            is AddonAction.InstallAddon -> {
                installAddon()
            }
            is AddonAction.LoadInstalledAddons -> {
                loadInstalledAddons()
            }
            is AddonAction.RemoveAddon -> {
                removeAddon(action.addonId)
            }
            is AddonAction.RefreshAddon -> {
                refreshAddon(action.addonId)
            }
            is AddonAction.ConfigureAddon -> {
                configureAddon(action.addonId)
            }
        }
    }

    private fun configureAddon(addonId: String) {
        val url = repository.getAddonUrl(addonId) ?: return
        val base = url.substringBefore("?").trimEnd('/')
        val configUrl = if (base.endsWith("/manifest.json")) {
            base.removeSuffix("/manifest.json") + "/configure"
        } else {
            "$base/configure"
        }
        
        viewModelScope.launch {
            _event.emit(AddonEvent.OpenUrl(configUrl))
        }
    }

    private fun refreshAddon(addonId: String) {
        val url = repository.getAddonUrl(addonId) ?: return
        viewModelScope.launch {
            repository.fetchAddonManifest(url)
            // loadInstalledAddons is already observing or manually triggered
        }
    }

    private fun removeAddon(addonId: String) {
        viewModelScope.launch {
            val result = repository.removeAddon(addonId)
            if (result.isSuccess) {
                // repository flow should update automatically
            }
        }
    }

    private fun loadInstalledAddons() {
        viewModelScope.launch {
            repository.getInstalledAddonsFlow().collect { addons ->
                _state.value = _state.value.copy(installedAddons = addons)
            }
        }
    }

    private fun installAddon() {
        val currentUrl = _state.value.addonUrl
        if (currentUrl.isBlank()) return

        viewModelScope.launch {
            _state.value = _state.value.copy(isInstalling = true, error = null, successMessage = null)
            
            val result = repository.fetchAddonManifest(currentUrl)
            
            if (result.isSuccess) {
                val manifest = result.getOrNull()
                _state.value = _state.value.copy(
                    isInstalling = false,
                    successMessage = "Successfully installed ${manifest?.name}",
                    addonUrl = ""
                )
            } else {
                _state.value = _state.value.copy(
                    isInstalling = false,
                    error = "Failed to install addon: ${result.exceptionOrNull()?.message}"
                )
            }
        }
    }
}
