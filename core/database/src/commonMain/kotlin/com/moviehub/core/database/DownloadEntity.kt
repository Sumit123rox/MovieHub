package com.moviehub.core.database

import androidx.room3.Entity
import com.moviehub.core.model.DownloadItem
import com.moviehub.core.model.DownloadState

@Entity(
    tableName = "downloads",
    primaryKeys = ["id", "profileId"],
)
data class DownloadEntity(
    val id: String,
    val profileId: String,
    val mediaId: String,
    val title: String,
    val posterUrl: String?,
    val url: String,
    val headers: Map<String, String>,
    val filePath: String?,
    val state: DownloadState,
    val progress: Float,
    val totalSize: Long,
    val downloadedSize: Long,
    val createdAt: Long,
)

fun DownloadEntity.toExternalModel() = DownloadItem(
    id = id,
    profileId = profileId,
    mediaId = mediaId,
    title = title,
    posterUrl = posterUrl,
    url = url,
    headers = headers,
    filePath = filePath,
    state = state,
    progress = progress,
    totalSize = totalSize,
    downloadedSize = downloadedSize,
    createdAt = createdAt,
)

fun DownloadItem.toEntity() = DownloadEntity(
    id = id,
    profileId = profileId,
    mediaId = mediaId,
    title = title,
    posterUrl = posterUrl,
    url = url,
    headers = headers,
    filePath = filePath,
    state = state,
    progress = progress,
    totalSize = totalSize,
    downloadedSize = downloadedSize,
    createdAt = createdAt,
)
