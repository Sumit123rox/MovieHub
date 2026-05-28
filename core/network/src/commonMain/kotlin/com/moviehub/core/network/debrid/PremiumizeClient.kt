package com.moviehub.core.network.debrid

import com.moviehub.core.database.DebridSettingsRepository
import com.moviehub.core.model.StreamItem
import io.ktor.client.*
import kotlinx.serialization.json.Json

/**
 * Premiumize.me client — same DebridProvider interface.
 *
 * API differs from RD/AD:
 * - Auth uses ?apikey=... query param
 * - Torrent endpoint: /transfer/create (POST, multipart: file or src=magnet)
 * - Status: /transfer/list
 * - Cache check: /cache/check?items[]=hash
 * - Folder link: /transfer/directdl?id=...
 */
class PremiumizeClient(
    private val httpClient: HttpClient,
    private val settingsRepository: DebridSettingsRepository,
) : DebridProvider {
    override val name = "Premiumize"
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    override suspend fun isConfigured(): Boolean =
        settingsRepository.getApiKey().isNotBlank()

    override suspend fun resolveToDirectUrl(stream: StreamItem): Result<String> {
        val magnet = buildMagnet(stream) ?: return Result.failure(Exception("No magnet/hash for stream"))
        val apiKey = settingsRepository.getApiKey()
        if (apiKey.isBlank()) return Result.failure(Exception("Premiumize not configured"))

        // Stub — mirrors RealDebridClient.resolveToDirectUrl
        // Full: POST /transfer/create → poll /transfer/list → /transfer/directdl
        return Result.failure(Exception("Premiumize full resolution not yet implemented"))
    }

    override suspend fun checkCached(infoHash: String): Result<List<String>> {
        // Stub — /cache/check?items[]=hash
        return Result.success(emptyList())
    }

    private fun buildMagnet(stream: StreamItem): String? {
        val url = stream.url ?: stream.externalUrl
        if (url != null && (url.startsWith("magnet:") || url.contains(".torrent"))) return url
        val hash = stream.infoHash ?: return null
        val name = stream.name?.let { "&dn=${it.replace(" ", "+")}" } ?: ""
        return "magnet:?xt=urn:btih:$hash$name"
    }
}
