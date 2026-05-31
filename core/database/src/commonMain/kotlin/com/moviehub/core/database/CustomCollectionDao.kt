package com.moviehub.core.database

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomCollectionDao {
    @Query("SELECT * FROM custom_collections WHERE profileId = :profileId ORDER BY createdTimestamp DESC")
    fun getCollections(profileId: String): Flow<List<CustomCollectionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollection(collection: CustomCollectionEntity)

    @Delete
    suspend fun deleteCollection(collection: CustomCollectionEntity)

    @Query("DELETE FROM custom_collections WHERE id = :collectionId AND profileId = :profileId")
    suspend fun deleteCollectionById(collectionId: String, profileId: String)

    @Query("DELETE FROM collection_items WHERE collectionId = :collectionId AND profileId = :profileId")
    suspend fun clearCollectionItems(collectionId: String, profileId: String)

    @Query("SELECT * FROM collection_items WHERE collectionId = :collectionId AND profileId = :profileId ORDER BY addedTimestamp DESC")
    fun getCollectionItems(collectionId: String, profileId: String): Flow<List<CollectionItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollectionItem(item: CollectionItemEntity)

    @Query("DELETE FROM collection_items WHERE collectionId = :collectionId AND contentId = :contentId AND profileId = :profileId")
    suspend fun deleteCollectionItem(collectionId: String, contentId: String, profileId: String)

    @Query("SELECT collectionId FROM collection_items WHERE contentId = :contentId AND profileId = :profileId")
    fun getCollectionsForMedia(contentId: String, profileId: String): Flow<List<String>>
}
