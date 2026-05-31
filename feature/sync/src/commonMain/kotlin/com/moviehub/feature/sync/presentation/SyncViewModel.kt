package com.moviehub.feature.sync.presentation

import androidx.lifecycle.ViewModel
import com.moviehub.feature.sync.SyncManager
import com.moviehub.feature.sync.SyncState
import kotlinx.coroutines.flow.StateFlow

class SyncViewModel(
    private val syncManager: SyncManager,
) : ViewModel() {

    val syncState: StateFlow<SyncState> = syncManager.syncState

    fun onSyncNow() {
        syncManager.triggerSync()
    }
}
