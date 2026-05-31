package com.moviehub.feature.addon.data

import com.moviehub.core.model.StremioManifest
import com.moviehub.core.network.AddonManager
import com.moviehub.core.network.StremioApiClient
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
    private val manager: AddonManager,
) : AddonRepository {

    override fun getAddonUrl(addonId: String): String? {
        return manager.getAddonUrl(addonId)
    }

    private fun normalizeManifestUrl(rawUrl: String): String {
        val trimmed = rawUrl.trim()
        if (trimmed.isEmpty()) return ""
        val normalizedScheme = when {
            trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
            trimmed.startsWith("stremio://") -> "https://${trimmed.removePrefix("stremio://")}"
            else -> "https://$trimmed"
        }
        val withoutFragment = normalizedScheme.substringBefore("#")
        val query = withoutFragment.substringAfter("?", "")
        val path = withoutFragment.substringBefore("?").trimEnd('/')
        val manifestPath = if (path.endsWith("/manifest.json")) path else "$path/manifest.json"
        return if (query.isEmpty()) manifestPath else "$manifestPath?$query"
    }

    override suspend fun fetchAddonManifest(url: String): Result<StremioManifest> {
        return try {
            val sanitizedUrl = normalizeManifestUrl(url)
            if (sanitizedUrl.isEmpty()) {
                return Result.failure(Exception("Addon URL cannot be empty"))
            }

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
