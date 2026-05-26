package com.moviehub.feature.addon.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moviehub.core.network.scraper.AddPluginRepositoryResult
import com.moviehub.core.network.scraper.PluginRepository
import com.moviehub.core.network.scraper.PluginsUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PluginsUiStateWrapper(
    val repoUrlInput: String = "",
    val coreState: PluginsUiState = PluginsUiState()
)

sealed interface PluginsAction {
    data class UrlInputChanged(val url: String) : PluginsAction
    object InstallRepository : PluginsAction
    data class RemoveRepository(val manifestUrl: String) : PluginsAction
    data class ToggleScraper(val id: String, val enabled: Boolean) : PluginsAction
    data class TogglePluginsEnabled(val enabled: Boolean) : PluginsAction
    data class RefreshRepository(val manifestUrl: String) : PluginsAction
    object RefreshAll : PluginsAction
}

class PluginsViewModel(
    private val pluginRepository: PluginRepository
) : ViewModel() {

    private val _repoUrlInput = MutableStateFlow("")

    val state: StateFlow<PluginsUiStateWrapper> = combine(
        _repoUrlInput,
        pluginRepository.uiState
    ) { input, core ->
        PluginsUiStateWrapper(
            repoUrlInput = input,
            coreState = core
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PluginsUiStateWrapper()
    )

    init {
        pluginRepository.initialize()
    }

    fun onAction(action: PluginsAction) {
        when (action) {
            is PluginsAction.UrlInputChanged -> {
                _repoUrlInput.value = action.url
            }
            is PluginsAction.InstallRepository -> {
                installRepository()
            }
            is PluginsAction.RemoveRepository -> {
                pluginRepository.removeRepository(action.manifestUrl)
            }
            is PluginsAction.ToggleScraper -> {
                pluginRepository.toggleScraper(action.id, action.enabled)
            }
            is PluginsAction.TogglePluginsEnabled -> {
                pluginRepository.setPluginsEnabled(action.enabled)
            }
            is PluginsAction.RefreshRepository -> {
                pluginRepository.refreshRepository(action.manifestUrl)
            }
            is PluginsAction.RefreshAll -> {
                pluginRepository.refreshAll()
            }
        }
    }

    private fun installRepository() {
        val url = _repoUrlInput.value.trim()
        if (url.isEmpty()) return

        viewModelScope.launch {
            val result = pluginRepository.addRepository(url)
            if (result is AddPluginRepositoryResult.Success) {
                _repoUrlInput.value = ""
            }
        }
    }
}
