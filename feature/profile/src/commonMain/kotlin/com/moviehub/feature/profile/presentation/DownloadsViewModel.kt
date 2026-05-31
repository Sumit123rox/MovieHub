package com.moviehub.feature.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moviehub.core.model.DownloadItem
import com.moviehub.core.network.DownloadsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.moviehub.core.network.StorageInfo

class DownloadsViewModel(
    private val downloadsRepository: DownloadsRepository,
) : ViewModel() {
    val downloads: StateFlow<List<DownloadItem>> = downloadsRepository.allDownloads
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _storageInfo = MutableStateFlow<StorageInfo?>(null)
    val storageInfo = _storageInfo.asStateFlow()

    init {
        updateStorageInfo()
    }

    fun updateStorageInfo() {
        viewModelScope.launch {
            _storageInfo.value = downloadsRepository.getStorageInfo()
        }
    }

    fun cancelDownload(item: DownloadItem) {
        viewModelScope.launch {
            downloadsRepository.cancelDownload(item)
            delay(500) // Give the file deletion some time to propagate
            updateStorageInfo()
        }
    }

    fun pauseDownload(item: DownloadItem) {
        viewModelScope.launch {
            downloadsRepository.pauseDownload(item)
            updateStorageInfo()
        }
    }

    fun resumeDownload(item: DownloadItem) {
        viewModelScope.launch {
            downloadsRepository.resumeDownload(item)
            updateStorageInfo()
        }
    }

    override fun onCleared() {
        super.onCleared()
        downloadsRepository.dispose()
    }
}
