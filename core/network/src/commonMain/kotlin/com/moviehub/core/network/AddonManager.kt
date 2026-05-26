package com.moviehub.core.network

import com.moviehub.core.database.AddonDao
import com.moviehub.core.database.AddonEntity
import com.moviehub.core.database.ProfileRepository
import com.moviehub.core.model.StremioManifest
import co.touchlab.kermit.Logger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val addonManagerLogger = Logger.withTag("AddonManager")
private val addonManagerExceptionHandler = CoroutineExceptionHandler { _, e ->
    addonManagerLogger.e(e) { "Unhandled coroutine exception" }
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class AddonManager(
    private val addonDao: AddonDao,
    private val profileRepository: ProfileRepository,
    private val ioDispatcher: kotlinx.coroutines.CoroutineDispatcher = kotlinx.coroutines.Dispatchers.Default
) {
    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher + addonManagerExceptionHandler)

    val installedAddons: StateFlow<List<StremioManifest>> = profileRepository.activeProfile
        .flatMapLatest { profile ->
            if (profile == null) flowOf(emptyList())
            else addonDao.getAllAddons(profile.id).map { entities ->
                entities.map { Json.decodeFromString<StremioManifest>(it.manifest) }
            }
        }
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    private val _addonUrlMap: StateFlow<Map<String, String>> = profileRepository.activeProfile
        .flatMapLatest { profile ->
            if (profile == null) flowOf(emptyMap())
            else addonDao.getAllAddons(profile.id).map { entities ->
                entities.associate { it.id to it.url }
            }
        }
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = emptyMap()
        )

    fun dispose() {
        scope.cancel()
    }

    suspend fun addAddon(url: String, manifest: StremioManifest) {
        val profileId = profileRepository.activeProfile.value?.id ?: return
        val entity = AddonEntity(
            id = manifest.id,
            profileId = profileId,
            url = url,
            manifest = Json.encodeToString(manifest)
        )
        addonDao.insertAddon(entity)
    }

    fun getAddonUrl(addonId: String): String? {
        return _addonUrlMap.value[addonId]
    }

    suspend fun removeAddon(addonId: String) {
        val profileId = profileRepository.activeProfile.value?.id ?: return
        addonDao.deleteAddon(addonId, profileId)
    }

    fun getAddonByUrl(url: String): StremioManifest? {
        val addonId = _addonUrlMap.value.entries.firstOrNull { it.value == url }?.key ?: return null
        return installedAddons.value.firstOrNull { it.id == addonId }
    }

    fun getAddonsProviding(resource: String, type: String): List<Pair<String, StremioManifest>> {
        val typeAliases = when (type.lowercase()) {
            "series" -> listOf("series", "tv", "show")
            "tv" -> listOf("series", "tv", "show")
            else -> listOf(type.lowercase())
        }

        return installedAddons.value
            .filter { manifest ->
                manifest.resources.contains(resource) && 
                (manifest.types.isEmpty() || manifest.types.any { it.lowercase() in typeAliases })
            }
            .mapNotNull { manifest -> 
                _addonUrlMap.value[manifest.id]?.let { url -> url to manifest } 
            }
    }
}

