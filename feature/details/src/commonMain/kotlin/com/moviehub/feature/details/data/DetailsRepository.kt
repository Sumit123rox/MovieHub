package com.moviehub.feature.details.data

import co.touchlab.kermit.Logger
import com.moviehub.core.model.MediaItem
import com.moviehub.core.network.StremioApiClient
import com.moviehub.core.network.mapper.toDomain
import com.moviehub.core.model.StreamItem

import com.moviehub.core.network.AddonManager
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope

interface DetailsRepository {
    suspend fun getMediaDetails(id: String, type: String, addonUrl: String? = null): MediaItem?
    suspend fun getStreams(id: String, type: String): List<StreamItem>
}

class DetailsRepositoryImpl(
    private val apiClient: StremioApiClient,
    private val addonManager: AddonManager
) : DetailsRepository {

    private val logger = Logger.withTag("DetailsRepository")
    private val CINEMETA_URL = "https://v3-cinemeta.strem.io"

    override suspend fun getMediaDetails(id: String, type: String, addonUrl: String?): MediaItem? {
        // 1. Try Primary Source (from card click)
        if (addonUrl != null) {
            try {
                val response = apiClient.getMeta(addonUrl, type, id)
                if (response != null) {
                    return response.meta.toDomain(addonUrl = addonUrl)
                }
            } catch (e: Exception) {
                logger.w { "Primary meta fetch failed for $id at $addonUrl" }
            }
        }

        // 2. Try Installed Fallbacks
        val metaAddons = addonManager.getAddonsProviding("meta", type)
        for ((url, manifest) in metaAddons) {
            if (url == addonUrl) continue 
            try {
                val response = apiClient.getMeta(url, type, id)
                if (response != null) {
                    logger.i { "Metadata found via fallback addon: ${manifest.name}" }
                    return response.meta.toDomain(addonId = manifest.id, addonUrl = url)
                }
            } catch (e: Exception) {
                // Continue
            }
        }

        // 3. Global Hardcoded Fallback: Cinemeta (The gold standard)
        try {
            logger.i { "All installed addons failed for $id. Attempting Cinemeta fallback..." }
            val response = apiClient.getMeta(CINEMETA_URL, type, id)
            if (response != null) {
                logger.i { "Metadata recovered via Cinemeta global fallback" }
                return response.meta.toDomain(addonId = "cinemeta", addonUrl = CINEMETA_URL)
            }
        } catch (e: Exception) {
            logger.e(e) { "Cinemeta fallback also failed for $id" }
        }
        
        return null
    }

    override suspend fun getStreams(id: String, type: String): List<StreamItem> = supervisorScope {
        val streamAddons = addonManager.getAddonsProviding("stream", type)
        val deferredStreams = streamAddons.map { (url, manifest) ->
            async {
                try {
                    apiClient.getStreams(
                        baseUrl = url,
                        type = type,
                        id = id,
                        addonName = manifest.name,
                        addonId = manifest.id
                    )
                } catch (e: Exception) {
                    emptyList()
                }
            }
        }
        deferredStreams.awaitAll().flatten()
    }
}
