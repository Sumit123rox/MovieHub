package com.moviehub.core.network.torrent

import com.moviehub.core.database.PlatformContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * iOS TorrentEngine stub.
 *
 * iOS P2P requires compiling libtorrent for iOS (arm64) and exposing via C interop.
 * This is significantly more complex than Android due to:
 * 1. App Store policies on P2P may require justification
 * 2. libtorrent must be built as an XCFramework for iOS + simulator
 * 3. Kotlin/Native cinterop bindings needed
 *
 * For now, this stub reports torrents as unavailable — the HybridStreamResolver
 * will fall back gracefully to what works (Debrid or direct HTTP).
 *
 * When ready to implement:
 * 1. Build libtorrent for iOS targets
 * 2. Create .def file for cinterop
 * 3. Wrap in a Kotlin class similar to Android anitorrent bridge
 * 4. Replace this actual with the real implementation
 */
actual class TorrentEngine actual constructor(private val ctx: PlatformContext) {
    private val _progress = MutableStateFlow(emptyList<TorrentProgress>())
    actual val progress: StateFlow<List<TorrentProgress>> = _progress.asStateFlow()
    private var _isRunning = false
    actual val isRunning: Boolean get() = _isRunning

    actual suspend fun start(config: TorrentConfig): Int {
        // iOS: initialize libtorrent session, start embedded HTTP server
        // For stub: report engine as available but no-op
        _isRunning = true
        return config.httpStreamPort
    }

    actual suspend fun addTorrent(magnet: MagnetMetadata, fileIndex: Int): String {
        // iOS: add magnet to libtorrent session, serve via local HTTP
        // For stub: return a URL that the resolver will treat as unavailable
        return "http://127.0.0.1:${engineConfig.httpStreamPort}/torrent/${magnet.infoHash}/$fileIndex"
    }

    private var engineConfig = TorrentConfig()

    actual fun pause(torrentId: String) {}
    actual fun resume(torrentId: String) {}
    actual fun removeTorrent(torrentId: String) {}

    actual suspend fun stop() {
        _isRunning = false
        _progress.value = emptyList()
    }
}
