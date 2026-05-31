package com.moviehub.core.database

import androidx.room3.Dao
import androidx.room3.Entity
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

@Entity(
    tableName = "watch_progress",
    primaryKeys = ["mediaId", "profileId"],
)
data class WatchProgress(
    val mediaId: String,
    val profileId: String,
    val type: String,
    val progressMs: Long,
    val durationMs: Long,
    val isWatched: Boolean = false,
    val audioGroupIndex: Int = -2,
    val audioTrackIndex: Int = -2,
    val subtitleGroupIndex: Int = -2,
    val subtitleTrackIndex: Int = -2,
)

@Dao
interface WatchProgressDao {
    @Query("SELECT * FROM watch_progress WHERE mediaId = :mediaId AND profileId = :profileId LIMIT 1")
    fun getProgress(mediaId: String, profileId: String): Flow<WatchProgress?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(progress: WatchProgress)

    @Query("UPDATE watch_progress SET isWatched = 0 WHERE mediaId = :mediaId AND profileId = :profileId")
    suspend fun markAsUnwatched(mediaId: String, profileId: String)

    @Query("SELECT * FROM watch_progress WHERE profileId = :profileId AND isWatched = 0 AND progressMs > 0 ORDER BY progressMs DESC LIMIT :limit")
    fun getInProgress(profileId: String, limit: Int = 20): Flow<List<WatchProgress>>

    @Query("SELECT mediaId FROM watch_progress WHERE profileId = :profileId AND isWatched = 1")
    fun getWatchedMediaIds(profileId: String): Flow<List<String>>

    @Query("DELETE FROM watch_progress WHERE mediaId = :mediaId AND profileId = :profileId")
    suspend fun deleteProgress(mediaId: String, profileId: String)
}
