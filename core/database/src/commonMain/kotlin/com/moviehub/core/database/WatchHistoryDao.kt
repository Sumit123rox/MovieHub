package com.moviehub.core.database

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchHistoryDao {
    @Query("SELECT * FROM watch_history WHERE profileId = :profileId ORDER BY lastWatchedAt DESC")
    fun getAllWatchHistory(profileId: String): Flow<List<WatchHistoryEntity>>

    @Query("SELECT * FROM watch_history WHERE mediaId = :mediaId AND profileId = :profileId")
    suspend fun getWatchHistory(mediaId: String, profileId: String): WatchHistoryEntity?

    @Query("SELECT * FROM watch_history WHERE mediaId = :mediaId AND profileId = :profileId")
    fun getWatchHistoryFlow(mediaId: String, profileId: String): Flow<WatchHistoryEntity?>

    @Query("SELECT * FROM watch_history WHERE mediaId IN (:mediaIds) AND profileId = :profileId")
    suspend fun getWatchHistoryBatch(mediaIds: List<String>, profileId: String): List<WatchHistoryEntity>

    @Query("SELECT * FROM watch_history WHERE profileId = :profileId ORDER BY lastWatchedAt DESC LIMIT :limit")
    fun getRecentlyWatched(profileId: String, limit: Int = 10): Flow<List<WatchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWatchHistory(watchHistory: WatchHistoryEntity)

    @Query("""
        INSERT OR REPLACE INTO watch_history(mediaId, profileId, title, type, posterPath, lastWatchedAt)
        VALUES (:mediaId, :profileId, :title, :type,
                COALESCE(NULLIF(:posterPath, ''), (SELECT posterPath FROM watch_history WHERE mediaId = :mediaId AND profileId = :profileId)),
                :lastWatchedAt)
    """)
    suspend fun conditionalUpsertWatchHistory(
        mediaId: String,
        profileId: String,
        title: String,
        type: String,
        posterPath: String?,
        lastWatchedAt: Long = com.moviehub.core.utils.currentTimeMillis()
    )

    @Update
    suspend fun updateWatchHistory(watchHistory: WatchHistoryEntity)

    @Query("DELETE FROM watch_history WHERE mediaId = :mediaId AND profileId = :profileId")
    suspend fun deleteWatchHistory(mediaId: String, profileId: String)

    @Query("DELETE FROM watch_history WHERE profileId = :profileId")
    suspend fun clearWatchHistory(profileId: String)
}
