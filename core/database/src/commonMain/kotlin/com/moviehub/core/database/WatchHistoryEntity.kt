package com.moviehub.core.database

import androidx.room3.Entity
import com.moviehub.core.utils.currentTimeMillis

@Entity(
    tableName = "watch_history",
    primaryKeys = ["mediaId", "profileId"]
)
data class WatchHistoryEntity(
    val mediaId: String,
    val profileId: String,
    val title: String,
    val type: String,
    val posterPath: String?,
    val lastWatchedAt: Long = currentTimeMillis()
)
