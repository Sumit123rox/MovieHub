package com.moviehub.core.database

import androidx.room3.Dao
import androidx.room3.Entity
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.PrimaryKey
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

@Entity(
    tableName = "watch_progress",
    primaryKeys = ["mediaId", "profileId"]
)
data class WatchProgress(
    val mediaId: String,
    val profileId: String,
    val type: String,
    val progressMs: Long,
    val durationMs: Long,
    val isWatched: Boolean = false
)

@Dao
interface WatchProgressDao {
    @Query("SELECT * FROM watch_progress WHERE mediaId = :mediaId AND profileId = :profileId LIMIT 1")
    fun getProgress(mediaId: String, profileId: String): Flow<WatchProgress?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(progress: WatchProgress)

    @Query("UPDATE watch_progress SET isWatched = 1 WHERE mediaId = :mediaId AND profileId = :profileId")
    suspend fun markAsWatched(mediaId: String, profileId: String)
}
