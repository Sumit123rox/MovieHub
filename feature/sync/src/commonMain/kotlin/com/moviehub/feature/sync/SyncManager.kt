package com.moviehub.feature.sync

import co.touchlab.kermit.Logger
import com.moviehub.core.database.AddonDao
import com.moviehub.core.database.AddonEntity
import com.moviehub.core.database.CacheService
import com.moviehub.core.database.FavoriteDao
import com.moviehub.core.database.FavoriteEntity
import com.moviehub.core.database.ProfileRepository
import com.moviehub.core.database.UserPreferencesDao
import com.moviehub.core.database.UserPreferencesEntity
import com.moviehub.core.database.WatchHistoryDao
import com.moviehub.core.database.WatchHistoryEntity
import com.moviehub.core.database.WatchProgress
import com.moviehub.core.database.WatchProgressDao
import com.moviehub.core.model.Profile
import com.moviehub.core.network.CatalogPrefetcher
import com.moviehub.core.network.NetworkConnectivityMonitor
import com.moviehub.core.network.PowerStateManager
import com.moviehub.core.utils.currentTimeMillis
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull

data class SyncState(
    val isSyncing: Boolean = false,
    val lastSyncTime: Long? = null,
    val lastSyncError: String? = null,
    val itemsSynced: Int = 0,
)

class SyncManager(
    private val supabase: SupabaseClient,
    private val profileRepository: ProfileRepository,
    private val addonDao: AddonDao,
    private val watchHistoryDao: WatchHistoryDao,
    private val cacheService: CacheService,
    private val catalogPrefetcher: CatalogPrefetcher,
    private val powerStateManager: PowerStateManager,
    private val networkMonitor: NetworkConnectivityMonitor,
    private val favoriteDao: FavoriteDao,
    private val userPreferencesDao: UserPreferencesDao,
    private val watchProgressDao: WatchProgressDao,
) {
    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.Default + CoroutineExceptionHandler { _, e ->
            Logger.withTag("SyncManager").e(e) { "Unhandled coroutine exception" }
        },
    )

    private val _syncState = MutableStateFlow(SyncState())
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

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

        // Periodic cache eviction
        scope.launch {
            delay(30_000L)
            while (isActive) {
                try {
                    if (!powerStateManager.state.value.isPowerSaveMode) {
                        cacheService.evictExpired()
                        cacheService.evictIfNeeded()
                    }
                } catch (e: Exception) {
                    Logger.withTag("SyncManager").e(e) { "Cache eviction failed" }
                }
                delay(if (powerStateManager.state.value.isPowerSaveMode) 30 * 60 * 1000L else 15 * 60 * 1000L)
            }
        }

        // Background catalog prefetch
        scope.launch {
            delay(5_000L)
            while (isActive) {
                try {
                    if (!powerStateManager.state.value.isPowerSaveMode && networkMonitor.isOnlineNow) {
                        catalogPrefetcher.prefetchFirstPage()
                    }
                } catch (e: Exception) {
                    Logger.withTag("SyncManager").e(e) { "Catalog prefetch failed" }
                }
                delay(if (powerStateManager.state.value.isPowerSaveMode) 30 * 60 * 1000L else 10 * 60 * 1000L)
            }
        }
    }

    fun triggerSync() {
        val profile = profileRepository.activeProfile.value ?: return
        scope.launch {
            syncProfileData(profile)
        }
    }

    private suspend fun syncProfileData(profile: Profile) {
        if (!networkMonitor.isOnlineNow) return

        _syncState.value = _syncState.value.copy(isSyncing = true, lastSyncError = null)
        val log = Logger.withTag("SyncManager")
        var synced = 0

        try {
            synced += pullFavorites(profile)
            synced += pullWatchHistory(profile)
            synced += pullWatchProgress(profile)
            synced += pullAddons(profile)
            synced += pullPreferences(profile)

            synced += pushFavorites(profile)
            synced += pushWatchHistory(profile)
            synced += pushWatchProgress(profile)
            synced += pushAddons(profile)
            synced += pushPreferences(profile)

            _syncState.value = _syncState.value.copy(
                isSyncing = false,
                lastSyncTime = currentTimeMillis(),
                itemsSynced = synced,
            )
            log.i { "Sync complete for profile ${profile.id}: $synced items" }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.e(e) { "Sync failed for profile ${profile.id}" }
            _syncState.value = _syncState.value.copy(
                isSyncing = false,
                lastSyncError = e.message ?: "Unknown sync error",
            )
        }
    }

    // ── Pull (Supabase → Room) ────────────────────────────────────────

    private suspend fun pullFavorites(profile: Profile): Int {
        return try {
            val remote: List<SupabaseFavorite> = supabase.from("favorites").select {
                filter { eq("profile_id", profile.id) }
            }.decodeList()
            remote.forEach { fav ->
                val contentType = when (fav.content_type.uppercase()) {
                    "MOVIE" -> com.moviehub.core.database.ContentType.MOVIE
                    "SHOW", "SERIES", "TV" -> com.moviehub.core.database.ContentType.SHOW
                    else -> com.moviehub.core.database.ContentType.MOVIE
                }
                favoriteDao.insertFavorite(
                    FavoriteEntity(
                        contentId = fav.content_id,
                        profileId = fav.profile_id,
                        contentType = contentType,
                        title = fav.title,
                        posterUrl = fav.poster_url,
                    ),
                )
            }
            remote.size
        } catch (e: Exception) {
            0
        }
    }

    private suspend fun pullWatchHistory(profile: Profile): Int {
        return try {
            val remote: List<SupabaseWatchHistory> = supabase.from("watch_history").select {
                filter { eq("profile_id", profile.id) }
            }.decodeList()
            remote.forEach { wh ->
                watchHistoryDao.insertWatchHistory(
                    WatchHistoryEntity(
                        mediaId = wh.media_id,
                        profileId = wh.profile_id,
                        title = "",
                        type = wh.type,
                        posterPath = null,
                        lastWatchedAt = wh.updated_at,
                    ),
                )
            }
            remote.size
        } catch (e: Exception) {
            0
        }
    }

    private suspend fun pullWatchProgress(profile: Profile): Int {
        return try {
            val remote: List<SupabaseWatchProgress> = supabase.from("watch_progress").select {
                filter { eq("profile_id", profile.id) }
            }.decodeList()
            remote.forEach { wp ->
                watchProgressDao.insertOrUpdate(
                    WatchProgress(
                        mediaId = wp.media_id,
                        profileId = wp.profile_id,
                        type = wp.type,
                        progressMs = wp.progress_ms,
                        durationMs = wp.duration_ms,
                        isWatched = wp.is_watched,
                    ),
                )
            }
            remote.size
        } catch (e: Exception) {
            0
        }
    }

    private suspend fun pullAddons(profile: Profile): Int {
        return try {
            val remote: List<SupabaseAddon> = supabase.from("addons").select {
                filter { eq("profile_id", profile.id) }
            }.decodeList()
            val existing = addonDao.getAllAddons(profile.id).firstOrNull() ?: emptyList()
            remote.forEach { remoteAddon ->
                if (existing.none { it.id == remoteAddon.id }) {
                    addonDao.insertAddon(
                        AddonEntity(
                            id = remoteAddon.id,
                            profileId = remoteAddon.profile_id,
                            url = remoteAddon.manifest_url,
                            manifest = "",
                        ),
                    )
                }
            }
            remote.size
        } catch (e: Exception) {
            0
        }
    }

    private suspend fun pullPreferences(profile: Profile): Int {
        return try {
            val remote: List<SupabasePreference> = supabase.from("user_preferences").select {
                filter { eq("profile_id", profile.id) }
            }.decodeList()
            remote.forEach { pref ->
                userPreferencesDao.setPreference(
                    UserPreferencesEntity(
                        profileId = pref.profile_id,
                        theme = pref.theme,
                        accentColor = pref.accent_color,
                    ),
                )
            }
            remote.size
        } catch (e: Exception) {
            0
        }
    }

    // ── Push (Room → Supabase) ────────────────────────────────────────

    private suspend fun pushFavorites(profile: Profile): Int {
        return try {
            val local = favoriteDao.getAllFavorites(profile.id).firstOrNull() ?: return 0
            if (local.isEmpty()) return 0
            val payload = local.map { fav ->
                SupabaseFavorite(
                    content_id = fav.contentId,
                    profile_id = fav.profileId,
                    content_type = fav.contentType.name,
                    title = fav.title,
                    poster_url = fav.posterUrl,
                )
            }
            supabase.from("favorites").upsert(payload)
            payload.size
        } catch (e: Exception) {
            0
        }
    }

    private suspend fun pushWatchHistory(profile: Profile): Int {
        return try {
            val local = watchHistoryDao.getAllWatchHistory(profile.id).firstOrNull() ?: return 0
            if (local.isEmpty()) return 0
            val payload = local.map { wh ->
                SupabaseWatchHistory(
                    media_id = wh.mediaId,
                    profile_id = wh.profileId,
                    type = wh.type,
                    progress_ms = 0,
                    duration_ms = 0,
                    is_watched = true,
                    updated_at = wh.lastWatchedAt,
                )
            }
            supabase.from("watch_history").upsert(payload)
            payload.size
        } catch (e: Exception) {
            0
        }
    }

    private suspend fun pushWatchProgress(profile: Profile): Int {
        return try {
            val local = watchProgressDao.getInProgress(profile.id, limit = 200).firstOrNull() ?: return 0
            if (local.isEmpty()) return 0
            val payload = local.map { wp ->
                SupabaseWatchProgress(
                    media_id = wp.mediaId,
                    profile_id = wp.profileId,
                    type = wp.type,
                    progress_ms = wp.progressMs,
                    duration_ms = wp.durationMs,
                    is_watched = wp.isWatched,
                )
            }
            supabase.from("watch_progress").upsert(payload)
            payload.size
        } catch (e: Exception) {
            0
        }
    }

    private suspend fun pushAddons(profile: Profile): Int {
        return try {
            val local = addonDao.getAllAddons(profile.id).firstOrNull() ?: return 0
            if (local.isEmpty()) return 0
            val payload = local.map { addon ->
                SupabaseAddon(
                    id = addon.id,
                    profile_id = addon.profileId,
                    name = addon.id,
                    version = "1.0",
                    manifest_url = addon.url,
                )
            }
            supabase.from("addons").upsert(payload)
            payload.size
        } catch (e: Exception) {
            0
        }
    }

    private suspend fun pushPreferences(profile: Profile): Int {
        return try {
            val local = userPreferencesDao.getPreference(profile.id) ?: return 0
            val payload = SupabasePreference(
                profile_id = local.profileId,
                theme = local.theme,
                accent_color = local.accentColor,
            )
            supabase.from("user_preferences").upsert(payload)
            1
        } catch (e: Exception) {
            0
        }
    }
}
