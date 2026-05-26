package com.moviehub.feature.sync

import co.touchlab.kermit.Logger
import com.moviehub.core.database.ProfileRepository
import com.moviehub.core.database.AddonDao
import com.moviehub.core.database.WatchHistoryDao
import com.moviehub.core.model.Profile
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SyncManager(
    private val supabase: SupabaseClient,
    private val profileRepository: ProfileRepository,
    private val addonDao: AddonDao,
    private val watchHistoryDao: WatchHistoryDao
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default + CoroutineExceptionHandler { _, e ->
        Logger.withTag("SyncManager").e(e) { "Unhandled coroutine exception" }
    })

    init {
        scope.launch {
            try {
                profileRepository.activeProfile.collectLatest { profile ->
                    if (profile != null) {
                        try {
                            syncProfileData(profile)
                        } catch (e: Exception) {
                            Logger.withTag("SyncManager").e(e) { "Error syncing profile data" }
                        }
                    }
                }
            } catch (e: Exception) {
                Logger.withTag("SyncManager").e(e) { "Failed to collect active profile" }
            }
        }
    }

    private suspend fun syncProfileData(profile: Profile) {
        // Implementation for pulling and pushing data to Supabase
        try {
            // Logic to fetch from supabase and update local Room DB
        } catch (e: Exception) {
            // Handle sync error
        }
    }

    fun triggerSync() {
        val profile = profileRepository.activeProfile.value ?: return
        scope.launch {
            syncProfileData(profile)
        }
    }
}
