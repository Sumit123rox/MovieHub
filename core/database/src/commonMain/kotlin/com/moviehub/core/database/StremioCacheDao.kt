package com.moviehub.core.database

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query

@Dao
interface StremioCacheDao {
    @Query("SELECT * FROM stremio_cache WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): StremioCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: StremioCacheEntity)

    @Query("DELETE FROM stremio_cache WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM stremio_cache")
    suspend fun clearAll()
}
