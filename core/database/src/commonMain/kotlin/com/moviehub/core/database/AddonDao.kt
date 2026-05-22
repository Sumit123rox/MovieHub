package com.moviehub.core.database

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AddonDao {
    @Query("SELECT * FROM addon WHERE profileId = :profileId")
    fun getAllAddons(profileId: String): Flow<List<AddonEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAddon(addon: AddonEntity)

    @Query("DELETE FROM addon WHERE id = :id AND profileId = :profileId")
    suspend fun deleteAddon(id: String, profileId: String)
}
