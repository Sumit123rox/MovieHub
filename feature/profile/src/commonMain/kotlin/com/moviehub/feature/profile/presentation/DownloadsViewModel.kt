package com.moviehub.feature.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moviehub.core.model.DownloadItem
import com.moviehub.core.network.DownloadsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DownloadsViewModel(
    private val downloadsRepository: DownloadsRepository
) : ViewModel() {
    val downloads: StateFlow<List<DownloadItem>> = downloadsRepository.allDownloads
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun cancelDownload(item: DownloadItem) {
        viewModelScope.launch { downloadsRepository.cancelDownload(item) }
    }

    fun pauseDownload(item: DownloadItem) {
        viewModelScope.launch { downloadsRepository.pauseDownload(item) }
    }

    fun resumeDownload(item: DownloadItem) {
        viewModelScope.launch { downloadsRepository.resumeDownload(item) }
    }

    override fun onCleared() {
        super.onCleared()
        downloadsRepository.dispose()
    }
}
