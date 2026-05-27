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

    @Query("DELETE FROM stremio_cache WHERE type = :type")
    suspend fun clearByType(type: String)

    @Query("DELETE FROM stremio_cache")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM stremio_cache")
    suspend fun count(): Int

    @Query("SELECT * FROM stremio_cache ORDER BY cachedAt ASC LIMIT :limit")
    suspend fun getOldestEntries(limit: Int): List<StremioCacheEntity>

    @Query("DELETE FROM stremio_cache WHERE cachedAt < :threshold")
    suspend fun deleteOlderThan(threshold: Long)
}
