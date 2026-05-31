package com.moviehub.core.database

import androidx.room3.Entity
import androidx.room3.PrimaryKey
import com.moviehub.core.utils.currentTimeMillis

@Entity(tableName = "custom_collections")
data class CustomCollectionEntity(
    @PrimaryKey val id: String,
    val profileId: String,
    val name: String,
    val description: String = "",
    val createdTimestamp: Long = currentTimeMillis()
)

@Entity(
    tableName = "collection_items",
    primaryKeys = ["collectionId", "contentId", "profileId"]
)
data class CollectionItemEntity(
    val collectionId: String,
    val contentId: String,
    val profileId: String,
    val contentType: ContentType,
    val title: String,
    val posterUrl: String?,
    val addedTimestamp: Long = currentTimeMillis()
)
