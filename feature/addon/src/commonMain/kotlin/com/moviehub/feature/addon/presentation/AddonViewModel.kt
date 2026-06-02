package com.moviehub.feature.addon.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moviehub.core.utils.PerformanceMonitor
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
            PerformanceMonitor.beginSection("VM:Addon:refreshAddon")
            try {
                _state.value = _state.value.copy(
                    refreshingAddonIds = _state.value.refreshingAddonIds + addonId,
                    error = null
                )
                repository.fetchAddonManifest(url)
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = "Refresh failed: ${e.message}")
            } finally {
                _state.value = _state.value.copy(
                    refreshingAddonIds = _state.value.refreshingAddonIds - addonId
                )
                PerformanceMonitor.endSection()
            }
        }
    }

    private fun removeAddon(addonId: String) {
        viewModelScope.launch {
            PerformanceMonitor.beginSection("VM:Addon:removeAddon")
            try {
                val result = repository.removeAddon(addonId)
                if (result.isSuccess) {
                    // repository flow should update automatically
                }
            } finally {
                PerformanceMonitor.endSection()
            }
        }
    }

    private var hasCheckedUpdates = false

    private fun loadInstalledAddons() {
        viewModelScope.launch {
            PerformanceMonitor.beginSection("VM:Addon:loadInstalledAddons")
            try {
                repository.getInstalledAddonsFlow().collect { addons ->
                    val sorted = addons.sortedBy { it.name.lowercase() }
                    val urls = mutableMapOf<String, String>()
                    sorted.forEach { manifest ->
                        repository.getAddonUrl(manifest.id)?.let { url ->
                            urls[manifest.id] = url
                        }
                    }
                    _state.value = _state.value.copy(installedAddons = sorted, addonUrls = urls)
                    
                    if (!hasCheckedUpdates && sorted.isNotEmpty()) {
                        hasCheckedUpdates = true
                        checkForUpdates(sorted)
                    }
                }
            } finally {
                PerformanceMonitor.endSection()
            }
        }
    }

    private fun checkForUpdates(addons: List<com.moviehub.core.model.StremioManifest>) {
        viewModelScope.launch {
            kotlinx.coroutines.delay(2000)
            var updatedCount = 0
            addons.forEach { addon ->
                val url = repository.getAddonUrl(addon.id) ?: return@forEach
                runCatching {
                    val result = repository.fetchAddonManifest(url)
                    if (result.isSuccess) {
                        val newManifest = result.getOrNull()
                        if (newManifest != null && newManifest.version != addon.version) {
                            updatedCount++
                        }
                    }
                }
            }
            if (updatedCount > 0) {
                _state.value = _state.value.copy(
                    successMessage = "$updatedCount providers have been automatically updated."
                )
                viewModelScope.launch {
                    kotlinx.coroutines.delay(5000)
                    if (_state.value.successMessage?.contains("automatically updated") == true) {
                        _state.value = _state.value.copy(successMessage = null)
                    }
                }
            }
        }
    }

    private fun installAddon() {
        val currentUrl = _state.value.addonUrl
        if (currentUrl.isBlank()) return

        viewModelScope.launch {
            PerformanceMonitor.beginSection("VM:Addon:installAddon")
            try {
                _state.value = _state.value.copy(isInstalling = true, error = null, successMessage = null)

                val result = repository.fetchAddonManifest(currentUrl)

                if (result.isSuccess) {
                    val manifest = result.getOrNull()
                    _state.value = _state.value.copy(
                        isInstalling = false,
                        successMessage = "Successfully installed ${manifest?.name}",
                        addonUrl = "",
                    )
                } else {
                    _state.value = _state.value.copy(
                        isInstalling = false,
                        error = "Failed to install addon: ${result.exceptionOrNull()?.message}",
                    )
                }
            } finally {
                PerformanceMonitor.endSection()
            }
        }
    }
}
