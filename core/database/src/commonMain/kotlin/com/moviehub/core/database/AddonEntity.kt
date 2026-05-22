package com.moviehub.core.database

import androidx.room3.Entity

@Entity(
    tableName = "addon",
    primaryKeys = ["id", "profileId"]
)
data class AddonEntity(
    val id: String,
    val profileId: String,
    val url: String,
    val manifest: String
)
