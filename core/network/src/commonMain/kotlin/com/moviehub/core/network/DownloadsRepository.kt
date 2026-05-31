package com.moviehub.core.network

import co.touchlab.kermit.Logger
import com.moviehub.core.database.DownloadDao
import com.moviehub.core.database.ProfileRepository
import com.moviehub.core.database.toEntity
import com.moviehub.core.database.toExternalModel
import com.moviehub.core.model.DownloadItem
import com.moviehub.core.model.DownloadState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val downloadsLogger = Logger.withTag("DownloadsRepository")
private val downloadsExceptionHandler = CoroutineExceptionHandler { _, e ->
    downloadsLogger.e(e) { "Unhandled coroutine exception" }
}

class DownloadsRepository(
    private val downloadDao: DownloadDao,
    private val profileRepository: ProfileRepository,
    private val platformDownloader: PlatformDownloader,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default + downloadsExceptionHandler)

    fun dispose() {
        scope.cancel()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val allDownloads: Flow<List<DownloadItem>> = profileRepository.activeProfile
        .flatMapLatest { profile ->
            if (profile == null) {
                flowOf(emptyList())
            } else {
                downloadDao.getAllDownloads(profile.id).map { entities ->
                    entities.map { it.toExternalModel() }
                }
            }
        }

    fun startDownload(item: DownloadItem) {
        scope.launch {
            val taskId = platformDownloader.download(item)
            val updatedItem = item.copy(id = taskId, state = DownloadState.DOWNLOADING)
            downloadDao.insertDownload(updatedItem.toEntity())

            platformDownloader.getProgress(taskId).collect { progress ->
                if (!isActive) return@collect
                downloadDao.updateProgress(
                    id = taskId,
                    profileId = item.profileId,
                    state = DownloadState.DOWNLOADING,
                    progress = progress.progress,
                    downloadedSize = progress.downloadedSize,
                )

                if (progress.progress >= 1f) {
                    downloadDao.updateProgress(
                        id = taskId,
                        profileId = item.profileId,
                        state = DownloadState.COMPLETED,
                        progress = 1f,
                        downloadedSize = progress.totalSize,
                    )
                }
            }
        }
    }

    fun pauseDownload(item: DownloadItem) {
        scope.launch {
            platformDownloader.pause(item.id)
            downloadDao.updateDownload(item.copy(state = DownloadState.PAUSED).toEntity())
        }
    }

    fun resumeDownload(item: DownloadItem) {
        scope.launch {
            platformDownloader.resume(item.id)
            downloadDao.updateDownload(item.copy(state = DownloadState.DOWNLOADING).toEntity())
        }
    }

    fun cancelDownload(item: DownloadItem) {
        scope.launch {
            platformDownloader.cancel(item.id)
            downloadDao.deleteDownload(item.toEntity())
        }
    }

    fun getStorageInfo(): StorageInfo {
        return platformDownloader.getStorageInfo()
    }
}
