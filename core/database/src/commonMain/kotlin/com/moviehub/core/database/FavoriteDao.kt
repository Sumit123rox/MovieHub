package com.moviehub.core.database

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites WHERE profileId = :profileId ORDER BY addedTimestamp DESC")
    fun getAllFavorites(profileId: String): Flow<List<FavoriteEntity>>

    @Query("SELECT * FROM favorites WHERE contentId = :contentId AND profileId = :profileId")
    suspend fun getFavoriteById(contentId: String, profileId: String): FavoriteEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE contentId = :contentId AND profileId = :profileId)")
    fun isFavorite(contentId: String, profileId: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteEntity)

    @Delete
    suspend fun deleteFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE contentId = :contentId AND profileId = :profileId")
    suspend fun deleteFavoriteById(contentId: String, profileId: String)

    @Query("DELETE FROM favorites WHERE profileId = :profileId")
    suspend fun clearAllFavorites(profileId: String)
}
