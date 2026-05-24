package com.moviehub.core.network

import com.moviehub.core.database.PlatformContext
import com.moviehub.core.model.DownloadItem
import kotlinx.coroutines.flow.Flow

expect class PlatformDownloader(ctx: PlatformContext) {
    fun download(item: DownloadItem): String // Returns task ID
    fun pause(taskId: String)
    fun resume(taskId: String)
    fun cancel(taskId: String)
    fun getProgress(taskId: String): Flow<DownloadProgress>
}

data class DownloadProgress(
    val downloadedSize: Long,
    val totalSize: Long,
    val progress: Float
)
