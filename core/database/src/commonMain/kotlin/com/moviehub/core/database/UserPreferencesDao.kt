package com.moviehub.core.database

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UserPreferencesDao {
    @Query("SELECT * FROM user_preferences WHERE profileId = :profileId")
    suspend fun getPreference(profileId: String): UserPreferencesEntity?

    @Query("SELECT * FROM user_preferences WHERE profileId = :profileId")
    fun getPreferenceFlow(profileId: String): Flow<UserPreferencesEntity?>

    @Query("SELECT * FROM user_preferences")
    fun getAllPreferences(): Flow<List<UserPreferencesEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setPreference(preference: UserPreferencesEntity)

    @Query("DELETE FROM user_preferences WHERE profileId = :profileId")
    suspend fun deletePreference(profileId: String)

    @Query("DELETE FROM user_preferences")
    suspend fun clearAllPreferences()
}