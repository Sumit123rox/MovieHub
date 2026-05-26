package com.moviehub.feature.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.moviehub.feature.auth.data.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthState(
    val traktCode: String? = null,
    val debridCode: String? = null,
    val isLoading: Boolean = false,
    val authSuccessMessage: String? = null,
    val error: String? = null
)

sealed interface AuthAction {
    object StartTraktAuth : AuthAction
    object StartDebridAuth : AuthAction
}

class AuthViewModel(private val repository: AuthRepository) : ViewModel() {
    private val logger = Logger.withTag("AuthViewModel")
    private val _state = MutableStateFlow(AuthState())
    val state: StateFlow<AuthState> = _state.asStateFlow()

    fun onAction(action: AuthAction) {
        when (action) {
            is AuthAction.StartTraktAuth -> startTraktAuth()
            is AuthAction.StartDebridAuth -> startDebridAuth()
        }
    }

    private fun startTraktAuth() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val result = repository.getTraktDeviceCode()
            if (result.isSuccess) {
                val code = result.getOrThrow()
                _state.value = _state.value.copy(isLoading = false, traktCode = code)
                pollTrakt(code)
            } else {
                _state.value = _state.value.copy(isLoading = false, error = "Failed to get Trakt code")
            }
        }
    }

    private fun pollTrakt(code: String) {
        viewModelScope.launch {
            val result = repository.pollTraktAuth(code)
            if (result.isSuccess) {
                _state.value = _state.value.copy(traktCode = null, authSuccessMessage = "Trakt Authenticated Successfully!")
            } else {
                _state.value = _state.value.copy(
                    traktCode = null,
                    error = result.exceptionOrNull()?.message ?: "Trakt authentication failed"
                )
                logger.w { "Trakt polling failed: ${result.exceptionOrNull()?.message}" }
            }
        }
    }

    private fun startDebridAuth() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val result = repository.getDebridDeviceCode()
            if (result.isSuccess) {
                val code = result.getOrThrow()
                _state.value = _state.value.copy(isLoading = false, debridCode = code)
                pollDebrid(code)
            } else {
                _state.value = _state.value.copy(isLoading = false, error = "Failed to get Debrid code")
            }
        }
    }

    private fun pollDebrid(code: String) {
        viewModelScope.launch {
            val result = repository.pollDebridAuth(code)
            if (result.isSuccess) {
                _state.value = _state.value.copy(debridCode = null, authSuccessMessage = "Real-Debrid Authenticated Successfully!")
            } else {
                _state.value = _state.value.copy(
                    debridCode = null,
                    error = result.exceptionOrNull()?.message ?: "Real-Debrid authentication failed"
                )
                logger.w { "Debrid polling failed: ${result.exceptionOrNull()?.message}" }
            }
        }
    }
}
