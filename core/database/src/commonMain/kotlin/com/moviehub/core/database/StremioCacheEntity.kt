package com.moviehub.core.database

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "stremio_cache")
data class StremioCacheEntity(
    @PrimaryKey val id: String,
    val type: String,
    val jsonData: String,
    val cachedAt: Long = 0L
)
