package com.moviehub.core.network

import com.moviehub.core.database.PlatformContext
import com.moviehub.core.model.DownloadItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

actual class PlatformDownloader actual constructor(private val ctx: PlatformContext) {
    actual fun download(item: DownloadItem): String {
        // iOS implementation using NSURLSessionDownloadTask would go here
        // For now, return a placeholder
        return "ios_task_${item.id}"
    }

    actual fun pause(taskId: String) {}
    actual fun resume(taskId: String) {}
    actual fun cancel(taskId: String) {}

    actual fun getProgress(taskId: String): Flow<DownloadProgress> = flow {
        // iOS progress tracking would go here
    }

    actual fun getStorageInfo(): StorageInfo {
        return StorageInfo(
            totalBytes = 256L * 1024L * 1024L * 1024L,
            freeBytes = 100L * 1024L * 1024L * 1024L,
            appBytes = 120L * 1024L * 1024L
        )
    }
}
