package com.moviehub.core.network

import com.moviehub.core.database.AddonDao
import com.moviehub.core.database.AddonEntity
import com.moviehub.core.database.ProfileRepository
import com.moviehub.core.model.StremioManifest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class AddonManager(
    private val addonDao: AddonDao,
    private val profileRepository: ProfileRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

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

    private val _addonUrlMap = MutableStateFlow<Map<String, String>>(emptyMap())

    init {
        scope.launch {
            profileRepository.activeProfile.collectLatest { profile ->
                if (profile != null) {
                    addonDao.getAllAddons(profile.id).collect { entities ->
                        _addonUrlMap.value = entities.associate { it.id to it.url }
                    }
                } else {
                    _addonUrlMap.value = emptyMap()
                }
            }
        }
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
        _addonUrlMap.value = _addonUrlMap.value + (manifest.id to url)
    }

    fun getAddonUrl(addonId: String): String? {
        return _addonUrlMap.value[addonId]
    }

    suspend fun removeAddon(addonId: String) {
        val profileId = profileRepository.activeProfile.value?.id ?: return
        addonDao.deleteAddon(addonId, profileId)
        _addonUrlMap.value = _addonUrlMap.value - addonId
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
                manifest.types.any { it.lowercase() in typeAliases }
            }
            .mapNotNull { manifest -> 
                _addonUrlMap.value[manifest.id]?.let { url -> url to manifest } 
            }
    }
}
