package com.moviehub.core.network.torrent

import com.moviehub.core.database.PlatformContext
import kotlinx.coroutines.flow.StateFlow

/**
 * Cross-platform torrent engine wrapping anitorrent (Android) / libtorrent (iOS).
 *
 * Architecture:
 * 1. User selects a torrent stream with infoHash
 * 2. [addTorrent] starts the download with magnet URI
 * 3. Engine downloads pieces via P2P (DHT/PEX/uTP)
 * 4. Local HTTP server streams pieces to ExoPlayer via range requests
 * 5. [removeTorrent] cleans up when done
 */
expect class TorrentEngine(ctx: PlatformContext) {
    val progress: StateFlow<List<TorrentProgress>>

    /** Start the engine and local HTTP server. Returns port the server is listening on. */
    suspend fun start(config: TorrentConfig = TorrentConfig()): Int

    /** Add a magnet link. Returns a local HTTP URL for the selected video file. */
    suspend fun addTorrent(magnet: MagnetMetadata, fileIndex: Int = -1): String

    /** Pause a torrent. */
    fun pause(torrentId: String)

    /** Resume a paused torrent. */
    fun resume(torrentId: String)

    /** Remove torrent and free resources. */
    fun removeTorrent(torrentId: String)

    /** Stop the engine and all sessions. */
    suspend fun stop()

    /** Check if the engine is running. */
    val isRunning: Boolean
}
