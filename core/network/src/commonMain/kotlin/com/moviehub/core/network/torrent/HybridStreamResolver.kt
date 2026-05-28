package com.moviehub.core.network.torrent

import com.moviehub.core.database.DebridSettingsRepository
import com.moviehub.core.model.StreamItem
import com.moviehub.core.network.debrid.DebridProvider
import com.moviehub.core.network.debrid.RealDebridClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

/**
 * Resolves torrent streams using a multi-layered strategy:
 * 1. Debrid providers (Real-Debrid → AllDebrid → Premiumize) — instant HTTP if cached
 * 2. P2P (TorrentEngine) — native P2P fallback if no Debrid provider succeeds
 *
 * Debrid providers are tried in order; the first configured provider that resolves wins.
 */
class HybridStreamResolver(
    private val debridClient: RealDebridClient,
    private val debridSettings: DebridSettingsRepository,
    private val torrentEngine: TorrentEngine,
    private val debridProviders: List<DebridProvider> = emptyList(),
) {
    suspend fun resolve(stream: StreamItem): ResolveResult = withContext(Dispatchers.IO) {
        if (!stream.isTorrentStream) {
            return@withContext ResolveResult.Direct(stream)
        }

        // Layer 1: Debrid providers (Real-Debrid first, then extra providers)
        val debridResult = tryDebrid(stream)
        if (debridResult != null) return@withContext debridResult

        for (provider in debridProviders) {
            val result = tryDebridProvider(provider, stream)
            if (result != null) return@withContext result
        }

        // Layer 2: P2P
        val p2pResult = tryP2p(stream)
        if (p2pResult != null) return@withContext p2pResult

        ResolveResult.Unavailable(stream, "No Debrid or P2P resolution available")
    }

    /** Resolve via Real-Debrid (primary provider). */
    private suspend fun tryDebrid(stream: StreamItem): ResolveResult? {
        val apiKey = debridSettings.getApiKey()
        if (apiKey.isBlank()) return null

        val magnet = buildMagnet(stream) ?: return null
        return runCatching {
            val url = debridClient.resolveToDirectUrl(magnet).getOrNull()
            if (url != null) ResolveResult.Direct(stream.copy(url = url)) else null
        }.getOrNull()
    }

    /** Resolve via a generic DebridProvider. */
    private suspend fun tryDebridProvider(provider: DebridProvider, stream: StreamItem): ResolveResult? {
        if (!provider.isConfigured()) return null
        return runCatching {
            val url = provider.resolveToDirectUrl(stream).getOrNull()
            if (url != null) ResolveResult.Direct(stream.copy(url = url)) else null
        }.getOrNull()
    }

    private suspend fun tryP2p(stream: StreamItem): ResolveResult? {
        if (!torrentEngine.isRunning) {
            runCatching { torrentEngine.start() }
        }
        if (!torrentEngine.isRunning) return null

        return runCatching {
            val magnet = buildMagnet(stream) ?: return@runCatching null
            val infoHash = stream.infoHash ?: return@runCatching null
            val metadata = MagnetMetadata(
                infoHash = infoHash,
                magnetUri = magnet,
                name = stream.name,
            )
            val localUrl = torrentEngine.addTorrent(metadata)
            ResolveResult.P2p(stream.copy(url = localUrl), infoHash)
        }.getOrNull()
    }

    private fun buildMagnet(stream: StreamItem): String? {
        val url = stream.url ?: stream.externalUrl
        if (url != null && (url.startsWith("magnet:") || url.contains(".torrent"))) return url
        val hash = stream.infoHash ?: return null
        val name = stream.name?.let { "&dn=${it.replace(" ", "+")}" } ?: ""
        return "magnet:?xt=urn:btih:$hash$name"
    }
}

sealed class ResolveResult {
    /** Stream can play directly (either original non-torrent, or Debrid-resolved). */
    data class Direct(val stream: StreamItem) : ResolveResult()
    /** Stream is being downloaded via P2P at the given local URL. */
    data class P2p(val stream: StreamItem, val infoHash: String) : ResolveResult()
    /** Stream could not be resolved. */
    data class Unavailable(val stream: StreamItem, val reason: String) : ResolveResult()
}
