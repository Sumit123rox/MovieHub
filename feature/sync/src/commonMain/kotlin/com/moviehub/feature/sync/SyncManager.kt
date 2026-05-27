package com.moviehub.feature.sync

import co.touchlab.kermit.Logger
import com.moviehub.core.database.AddonDao
import com.moviehub.core.database.CacheService
import com.moviehub.core.database.ProfileRepository
import com.moviehub.core.database.WatchHistoryDao
import com.moviehub.core.model.Profile
import com.moviehub.core.network.CatalogPrefetcher
import io.github.jan.supabase.SupabaseClient
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SyncManager(
    private val supabase: SupabaseClient,
    private val profileRepository: ProfileRepository,
    private val addonDao: AddonDao,
    private val watchHistoryDao: WatchHistoryDao,
    private val cacheService: CacheService,
    private val catalogPrefetcher: CatalogPrefetcher,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default + CoroutineExceptionHandler { _, e ->
        Logger.withTag("SyncManager").e(e) { "Unhandled coroutine exception" }
    })

    init {
        // Profile data sync
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

        // Periodic cache eviction (30s after startup, then every 15 minutes)
        scope.launch {
            delay(30_000L)
            while (isActive) {
                try {
                    cacheService.evictExpired()
                    cacheService.evictIfNeeded()
                } catch (e: Exception) {
                    Logger.withTag("SyncManager").e(e) { "Cache eviction failed" }
                }
                delay(15 * 60 * 1000L)
            }
        }

        // Background catalog prefetch (5s after startup, then every 10 minutes)
        scope.launch {
            delay(5_000L)
            while (isActive) {
                try {
                    catalogPrefetcher.prefetchFirstPage()
                } catch (e: Exception) {
                    Logger.withTag("SyncManager").e(e) { "Catalog prefetch failed" }
                }
                delay(10 * 60 * 1000L)
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
