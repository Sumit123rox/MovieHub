package com.moviehub.core.network.debrid

import com.moviehub.core.database.DebridSettingsRepository
import com.moviehub.core.model.StreamItem
import io.ktor.client.*
import kotlinx.serialization.json.Json

/**
 * AllDebrid (alldebrid.com) client — same pattern as RealDebridClient.
 *
 * API differs from RD primarily in:
 * - Auth uses API key passed as query param (?agent=MovieHub&apikey=...)
 * - Torrent upload endpoint: /v4/magnet/upload
 * - Status endpoint: /v4/magnet/status?id=...
 * - Unrestrict endpoint: /v4/link/unlock
 */
class AllDebridClient(
    private val httpClient: HttpClient,
    private val settingsRepository: DebridSettingsRepository,
) : DebridProvider {
    override val name = "AllDebrid"
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    override suspend fun isConfigured(): Boolean =
        settingsRepository.getApiKey().isNotBlank()

    override suspend fun resolveToDirectUrl(stream: StreamItem): Result<String> {
        val magnet = buildMagnet(stream) ?: return Result.failure(Exception("No magnet/hash for stream"))
        val apiKey = settingsRepository.getApiKey()
        if (apiKey.isBlank()) return Result.failure(Exception("AllDebrid not configured"))

        // Stub implementation — mirrors RealDebridClient.resolveToDirectUrl
        // Full implementation: POST /v4/magnet/upload → poll status → unlock link
        return Result.failure(Exception("AllDebrid full resolution not yet implemented"))
    }

    override suspend fun checkCached(infoHash: String): Result<List<String>> {
        // Stub — /v4/magnet/instant?magnets[]=... endpoint
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
