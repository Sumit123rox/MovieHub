package com.moviehub.core.network.debrid

import com.moviehub.core.model.StreamItem

/**
 * Abstraction over Debrid services (Real-Debrid, AllDebrid, Premiumize, etc).
 * Each provider handles its own auth, torrent resolution, and rate limiting.
 */
interface DebridProvider {
    /** Display name for logging / UI. */
    val name: String

    /** Whether the user has configured API credentials for this provider. */
    suspend fun isConfigured(): Boolean

    /** Resolve a torrent stream to a direct HTTP URL. Returns null if not supported/cached. */
    suspend fun resolveToDirectUrl(stream: StreamItem): Result<String>

    /** Check if a torrent infoHash is already cached on the provider's servers. */
    suspend fun checkCached(infoHash: String): Result<List<String>>
}
