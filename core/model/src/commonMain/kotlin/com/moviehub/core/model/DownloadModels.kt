package com.moviehub.core.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
enum class DownloadState {
    QUEUED,
    DOWNLOADING,
    COMPLETED,
    FAILED,
    PAUSED
}

@Immutable
@Serializable
data class DownloadItem(
    val id: String,
    val profileId: String,
    val mediaId: String,
    val title: String,
    val posterUrl: String?,
    val url: String,
    val headers: Map<String, String> = emptyMap(),
    val filePath: String? = null,
    val state: DownloadState = DownloadState.QUEUED,
    val progress: Float = 0f,
    val totalSize: Long = 0L,
    val downloadedSize: Long = 0L,
    val createdAt: Long = 0L
)
