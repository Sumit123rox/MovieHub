package com.moviehub.core.network.debrid

import com.moviehub.core.database.DebridSettingsRepository
import com.moviehub.core.model.StreamItem

/**
 * Resolves torrent-based StreamItems (those with infoHash) to direct HTTP URLs
 * via Real-Debrid. If Debrid is not configured or the stream isn't a torrent,
 * returns the original stream unchanged.
 */
class TorrentResolver(
    private val debridClient: RealDebridClient,
    private val settingsRepository: DebridSettingsRepository,
) {
    /**
     * Resolve a torrent stream to a playable HTTP stream.
     * Returns the original stream if it's not a torrent or if Debrid is unavailable.
     */
    suspend fun resolve(stream: StreamItem): StreamItem {
        if (!stream.isTorrentStream) return stream
        val apiKey = settingsRepository.getApiKey()
        if (apiKey.isBlank()) return stream

        val magnet = buildMagnet(stream) ?: return stream
        val result = debridClient.resolveToDirectUrl(magnet)
        return result.fold(
            onSuccess = { url -> stream.copy(url = url) },
            onFailure = { stream }
        )
    }

    private fun buildMagnet(stream: StreamItem): String? {
        val url = stream.url ?: stream.externalUrl
        if (url != null && (url.startsWith("magnet:") || url.contains(".torrent"))) return url

        val hash = stream.infoHash ?: return null
        // Some Stremio addons encode both infoHash and tracker in the URL
        val name = stream.name?.let { "&dn=${it.encodeForMagnet()}" } ?: ""
        return "magnet:?xt=urn:btih:$hash$name"
    }

    private fun String.encodeForMagnet(): String = replace(" ", "+")
}
