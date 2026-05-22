package com.moviehub.core.database

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads WHERE profileId = :profileId ORDER BY createdAt DESC")
    fun getAllDownloads(profileId: String): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE id = :id AND profileId = :profileId")
    suspend fun getDownloadById(id: String, profileId: String): DownloadEntity?

    @Query("SELECT * FROM downloads WHERE mediaId = :mediaId AND profileId = :profileId")
    suspend fun getDownloadByMediaId(mediaId: String, profileId: String): DownloadEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(download: DownloadEntity)

    @Update
    suspend fun updateDownload(download: DownloadEntity)

    @Delete
    suspend fun deleteDownload(download: DownloadEntity)

    @Query("UPDATE downloads SET state = :state, progress = :progress, downloadedSize = :downloadedSize WHERE id = :id AND profileId = :profileId")
    suspend fun updateProgress(id: String, profileId: String, state: com.moviehub.core.model.DownloadState, progress: Float, downloadedSize: Long)
}
