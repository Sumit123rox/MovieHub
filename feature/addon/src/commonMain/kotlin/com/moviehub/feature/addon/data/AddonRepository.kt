package com.moviehub.feature.addon.data

import com.moviehub.core.network.StremioApiClient
import com.moviehub.core.network.AddonManager
import com.moviehub.core.model.StremioManifest
import kotlinx.coroutines.flow.Flow

interface AddonRepository {
    suspend fun fetchAddonManifest(url: String): Result<StremioManifest>
    suspend fun getInstalledAddons(): List<StremioManifest>
    fun getInstalledAddonsFlow(): Flow<List<StremioManifest>>
    suspend fun removeAddon(addonId: String): Result<Unit>
    fun getAddonUrl(addonId: String): String?
}

class AddonRepositoryImpl(
    private val apiClient: StremioApiClient,
    private val manager: AddonManager
) : AddonRepository {

    override fun getAddonUrl(addonId: String): String? {
        return manager.getAddonUrl(addonId)
    }

    override suspend fun fetchAddonManifest(url: String): Result<StremioManifest> {
        return try {
            val sanitizedUrl = if (!url.endsWith("/manifest.json") && !url.contains("/manifest.json?")) {
                val base = url.substringBefore("?").trimEnd('/')
                if (url.contains("?")) {
                    "$base/manifest.json?${url.substringAfter("?")}"
                } else {
                    "$base/manifest.json"
                }
            } else url

            val manifest = apiClient.getManifest(sanitizedUrl)
            if (manifest != null) {
                manager.addAddon(sanitizedUrl, manifest)
                Result.success(manifest)
            } else {
                Result.failure(Exception("Failed to fetch manifest or invalid response"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getInstalledAddons(): List<StremioManifest> {
        return manager.installedAddons.value
    }

    override fun getInstalledAddonsFlow(): Flow<List<StremioManifest>> {
        return manager.installedAddons
    }

    override suspend fun removeAddon(addonId: String): Result<Unit> {
        return try {
            manager.removeAddon(addonId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
