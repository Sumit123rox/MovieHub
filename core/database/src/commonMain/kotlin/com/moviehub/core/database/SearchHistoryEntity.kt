package com.moviehub.core.database

import androidx.room3.Entity

@Entity(
    tableName = "search_history",
    primaryKeys = ["query", "profileId"]
)
data class SearchHistoryEntity(
    val query: String,
    val profileId: String,
    val timestamp: Long = System.currentTimeMillis()
)
