package com.moviehub.core.database

import androidx.room3.Entity
import com.moviehub.core.utils.currentTimeMillis

enum class ContentType {
    MOVIE,
    SHOW,
}

@Entity(
    tableName = "favorites",
    primaryKeys = ["contentId", "profileId"],
)
data class FavoriteEntity(
    val contentId: String,
    val profileId: String,
    val contentType: ContentType,
    val title: String,
    val posterUrl: String?,
    val addedTimestamp: Long = currentTimeMillis(),
)
