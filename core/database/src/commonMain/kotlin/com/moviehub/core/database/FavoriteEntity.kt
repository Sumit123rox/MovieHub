package com.moviehub.core.database

import androidx.room3.Entity

enum class ContentType {
    MOVIE,
    SHOW
}

@Entity(
    tableName = "favorites",
    primaryKeys = ["contentId", "profileId"]
)
data class FavoriteEntity(
    val contentId: String,
    val profileId: String,
    val contentType: ContentType,
    val title: String,
    val posterUrl: String?,
    val addedTimestamp: Long = System.currentTimeMillis()
)
