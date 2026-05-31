package com.moviehub.core.network

import android.app.DownloadManager
import android.net.Uri
import com.moviehub.core.database.PlatformContext
import com.moviehub.core.model.DownloadItem
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlin.time.Duration.Companion.milliseconds

actual class PlatformDownloader actual constructor(private val ctx: PlatformContext) {
    private val downloadManager = ctx.getSystemService(android.content.Context.DOWNLOAD_SERVICE) as DownloadManager

    actual fun download(item: DownloadItem): String {
        val request = DownloadManager.Request(Uri.parse(item.url))
            .setTitle(item.title)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        item.headers.forEach { (key, value) ->
            request.addRequestHeader(key, value)
        }

        val id = downloadManager.enqueue(request)
        return id.toString()
    }

    actual fun pause(taskId: String) {
        // DownloadManager doesn't support pause directly via API easily
    }

    actual fun resume(taskId: String) {
        // DownloadManager doesn't support resume directly via API easily
    }

    actual fun cancel(taskId: String) {
        downloadManager.remove(taskId.toLong())
    }

    actual fun getProgress(taskId: String): Flow<DownloadProgress> = flow {
        val query = DownloadManager.Query().setFilterById(taskId.toLong())
        while (currentCoroutineContext().isActive) {
            val cursor = downloadManager.query(query)
            if (cursor.moveToFirst()) {
                val downloaded = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                val total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))

                emit(
                    DownloadProgress(
                        downloadedSize = downloaded,
                        totalSize = total,
                        progress = if (total > 0) downloaded.toFloat() / total else 0f,
                    ),
                )

                if (status == DownloadManager.STATUS_SUCCESSFUL || status == DownloadManager.STATUS_FAILED) {
                    cursor.close()
                    break
                }
            }
            cursor.close()
            delay(1000.milliseconds)
        }
    }

    actual fun getStorageInfo(): StorageInfo {
        val path = ctx.filesDir
        val total = path.totalSpace
        val free = path.usableSpace
        val appSize = getFolderSize(ctx.filesDir) + getFolderSize(ctx.cacheDir)
        
        return StorageInfo(
            totalBytes = total,
            freeBytes = free,
            appBytes = appSize
        )
    }

    private fun getFolderSize(file: java.io.File): Long {
        if (file.isFile) return file.length()
        var size = 0L
        file.listFiles()?.forEach {
            size += getFolderSize(it)
        }
        return size
    }
}
