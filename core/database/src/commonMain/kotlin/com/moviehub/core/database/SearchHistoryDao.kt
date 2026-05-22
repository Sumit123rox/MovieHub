package com.moviehub.core.database

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchHistoryDao {
    @Query("SELECT * FROM search_history WHERE profileId = :profileId ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentSearches(profileId: String, limit: Int = 20): Flow<List<SearchHistoryEntity>>

    @Query("SELECT * FROM search_history WHERE profileId = :profileId AND query LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchHistory(profileId: String, query: String): Flow<List<SearchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearch(search: SearchHistoryEntity)

    @Query("DELETE FROM search_history WHERE query = :query AND profileId = :profileId")
    suspend fun deleteSearch(query: String, profileId: String)

    @Query("DELETE FROM search_history WHERE profileId = :profileId")
    suspend fun clearSearchHistory(profileId: String)

    @Query("DELETE FROM search_history WHERE profileId = :profileId AND query NOT IN (SELECT query FROM search_history WHERE profileId = :profileId ORDER BY timestamp DESC LIMIT :keep)")
    suspend fun trimOldSearches(profileId: String, keep: Int = 100)
}