package com.moviehub.core.database

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "user_preferences")
data class UserPreferencesEntity(
    @PrimaryKey val profileId: String,
    val theme: String = "nuvio_dark",
    val accentColor: String = "blue",
    val useAmoled: Boolean = true,
    val language: String = "en",
    val tmdbApiKey: String = "",
)
