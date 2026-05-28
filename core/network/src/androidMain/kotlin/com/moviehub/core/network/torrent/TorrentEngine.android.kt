package com.moviehub.core.network.torrent

import android.content.Context
import com.moviehub.core.database.PlatformContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Android TorrentEngine using anitorrent JNI bindings.
 *
 * Architecture:
 *   TorrentEngine
 *       +-- SessionManager (anitorrent) — manages libtorrent session + DHT
 *            +-- TorrentHandle per magnet — downloads pieces
 *                 +-- Local HTTP server — serves file via range requests to ExoPlayer
 *
 * ## anitorrent status
 *
 * The anitorrent library (org.openani.anitorrent) publishes native .so files to Maven Central
 * but its Kotlin API module (anitorrent-api) is not yet published. When the API ships:
 *
 * 1. Add to settings.gradle.kts versionCatalogs:
 *        create("anitorrentLibs") { from("org.openani.anitorrent:catalog:0.1.0") }
 * 2. Add to core/network/build.gradle.kts androidMain:
 *        implementation(anitorrentLibs.anitorrent.native.android)
 * 3. Remove the `false` guard below and implement using:
 *        import org.openani.anitorrent.SessionManager
 *        import org.openani.anitorrent.TorrentHandle
 *        import org.openani.anitorrent.SessionParams
 * 4. Wire pause/resume/remove → handle.pause() / handle.resume() / session.removeTorrent()
 * 5. Wire progress tracking → handle.status()
 * 6. Wire piece reading → localServer?.register(infoHash, idx) { offset, size -> handle.readPieceAt(idx, offset, size) }
 */
actual class TorrentEngine actual constructor(private val ctx: PlatformContext) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private val _progress = MutableStateFlow(emptyList<TorrentProgress>())
    private val activeTorrents = ConcurrentHashMap<String, TorrentSessionState>()
    private var localServer: LocalStreamServer? = null
    private var engineConfig: TorrentConfig = TorrentConfig()

    actual val progress: StateFlow<List<TorrentProgress>> = _progress.asStateFlow()
    private var _isRunning = false
    actual val isRunning: Boolean
        get() = _isRunning

    private data class TorrentSessionState(
        val magnetUri: String,
        val infoHash: String,
        val streamUrl: String,
        val fileIndex: Int,
    )

    actual suspend fun start(config: TorrentConfig): Int {
        mutex.withLock {
            if (_isRunning) return@withLock
            engineConfig = config

            val port = try {
                ServerSocket(config.httpStreamPort).use { it.localPort }
            } catch (_: Exception) {
                ServerSocket(0).use { it.localPort }
            }
            localServer = LocalStreamServer(port)
            localServer?.start()
            _isRunning = true
        }
        return engineConfig.httpStreamPort
    }

    actual suspend fun addTorrent(magnet: MagnetMetadata, fileIndex: Int): String {
        val isNativeAvailable = try {
            System.loadLibrary("anitorrent")
            false // Set to true when anitorrent-api is published and wired
        } catch (_: UnsatisfiedLinkError) {
            false
        }

        val url = if (isNativeAvailable) {
            addTorrentNative(magnet, fileIndex)
        } else {
            addTorrentPlaceholder(magnet, fileIndex)
        }
        val idx = if (fileIndex >= 0) fileIndex else 0
        activeTorrents[magnet.infoHash] = TorrentSessionState(
            magnetUri = magnet.magnetUri,
            infoHash = magnet.infoHash,
            streamUrl = url,
            fileIndex = idx,
        )
        scope.launch { trackProgress(magnet.infoHash) }
        return url
    }

    actual fun pause(torrentId: String) {
        // TODO: anitorrent — handle.pause()
    }

    actual fun resume(torrentId: String) {
        // TODO: anitorrent — handle.resume()
    }

    actual fun removeTorrent(torrentId: String) {
        activeTorrents.remove(torrentId)?.let { state ->
            localServer?.unregister(state.streamUrl)
        }
        // TODO: anitorrent — session.removeTorrent(handle)
    }

    actual suspend fun stop() {
        mutex.withLock {
            activeTorrents.values.forEach { state ->
                localServer?.unregister(state.streamUrl)
            }
            activeTorrents.clear()
            localServer?.stop()
            localServer = null
            _progress.value = emptyList()
            _isRunning = false
        }
        scope.coroutineContext[Job]?.children?.forEach { it.cancel() }
    }

    private fun addTorrentNative(magnet: MagnetMetadata, fileIndex: Int): String {
        // TODO: Wire anitorrent when API is published.
        // val savePath = (ctx as Context).cacheDir.resolve("torrents").also { it.mkdirs() }.absolutePath
        // val session = SessionManager(SessionParams(savePath, ...))
        // val handle = session.addTorrent(magnet.magnetUri)
        // val info = handle.torrentFile
        // val idx = ...
        // localServer?.register(magnet.infoHash, idx) { offset, size -> handle.readPieceAt(idx, offset, size) }
        // return "http://127.0.0.1:${engineConfig.httpStreamPort}/torrent/${magnet.infoHash}/$idx"
        return addTorrentPlaceholder(magnet, fileIndex)
    }

    private fun addTorrentPlaceholder(magnet: MagnetMetadata, fileIndex: Int): String {
        val idx = if (fileIndex >= 0) fileIndex else 0
        val url = "http://127.0.0.1:${engineConfig.httpStreamPort}/torrent/${magnet.infoHash}/$idx"
        localServer?.registerUnavailable(url)
        return url
    }

    private suspend fun trackProgress(infoHash: String) = coroutineScope {
        var attempts = 0
        while (isActive && activeTorrents.containsKey(infoHash)) {
            val state = activeTorrents[infoHash] ?: break
            if (attempts > 3) {
                _progress.value = _progress.value.map {
                    if (it.torrentId == infoHash) it.copy(status = TorrentStatus.ERROR, progress = 0f)
                    else it
                }
                break
            }
            val progress = TorrentProgress(
                torrentId = infoHash,
                status = TorrentStatus.QUEUED,
                progress = 0f,
                numPeers = 0,
                localStreamUrl = state.streamUrl,
                selectedFile = TorrentFileInfo(
                    fileIndex = state.fileIndex,
                    path = "",
                    size = 0,
                ),
            )
            _progress.value = _progress.value.filter { it.torrentId != infoHash } + progress
            delay(2000)
            attempts++
        }
    }

    protected fun finalize() {
        scope.launch { stop() }
    }
}

/**
 * Local HTTP server that serves torrent file data via range requests.
 * Runs on localhost (127.0.0.1) so ExoPlayer can stream pieces as if
 * it were any remote URL — supports HTTP Range headers for seeking.
 */
private class LocalStreamServer(private val port: Int) {
    private val routes = ConcurrentHashMap<String, (Long, Int) -> ByteArray>()
    private val unavailableRoutes = ConcurrentHashMap<String, Boolean>()
    private var server: ServerSocket? = null
    private var executor: ExecutorService? = null
    @Volatile private var running = false

    fun start() {
        if (running) return
        server = ServerSocket(port, 50, InetAddress.getByName("127.0.0.1"))
        running = true
        executor = Executors.newCachedThreadPool()
        val acceptThread = Thread {
            while (running) {
                try {
                    val client = server?.accept() ?: break
                    executor?.submit { handleClient(client) }
                } catch (_: IOException) {
                    if (running) break
                }
            }
        }
        acceptThread.isDaemon = true
        acceptThread.name = "LocalStreamServer-accept"
        acceptThread.start()
    }

    private fun handleClient(client: Socket) {
        client.use { socket ->
            val input = socket.getInputStream().bufferedReader()
            val output = socket.getOutputStream()

            val requestLine = input.readLine() ?: return
            val parts = requestLine.split(' ')
            if (parts.size < 2) return
            val method = parts[0]
            val path = parts[1]
            if (method != "GET") return

            val headers = mutableMapOf<String, String>()
            while (true) {
                val line = input.readLine() ?: break
                if (line.isBlank()) break
                val colon = line.indexOf(':')
                if (colon > 0) {
                    headers[line.substring(0, colon).trim().lowercase()] =
                        line.substring(colon + 1).trim()
                }
            }

            if (unavailableRoutes.containsKey(path)) {
                sendResponse(output, 404, "Not Found")
                return
            }

            val dataReader = routes[path] ?: run {
                sendResponse(output, 404, "Not Found")
                return
            }

            val rangeHeader = headers["range"]
            var startOffset = 0L
            var requestSize = Int.MAX_VALUE

            if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
                val range = rangeHeader.removePrefix("bytes=")
                val dash = range.indexOf('-')
                if (dash > 0) {
                    startOffset = range.substring(0, dash).toLongOrNull() ?: 0L
                    val end = range.substring(dash + 1).toLongOrNull()
                    if (end != null && end >= startOffset) {
                        requestSize = (end - startOffset + 1).toInt().coerceAtMost(Int.MAX_VALUE)
                    }
                }
            }

            val data = try {
                dataReader(startOffset, requestSize)
            } catch (e: Exception) {
                sendResponse(output, 500, "Stream error: ${e.message}")
                return
            }

            if (data.isEmpty()) {
                sendResponse(output, 204, "No Content")
                return
            }

            if (rangeHeader != null) {
                val body = buildString {
                    append("HTTP/1.1 206 Partial Content\r\n")
                    append("Content-Type: application/octet-stream\r\n")
                    append("Content-Length: ${data.size}\r\n")
                    append("Content-Range: bytes $startOffset-${startOffset + data.size - 1}/*\r\n")
                    append("Accept-Ranges: bytes\r\n")
                    append("Connection: close\r\n")
                    append("\r\n")
                }
                output.write(body.toByteArray())
            } else {
                val body = buildString {
                    append("HTTP/1.1 200 OK\r\n")
                    append("Content-Type: application/octet-stream\r\n")
                    append("Content-Length: ${data.size}\r\n")
                    append("Accept-Ranges: bytes\r\n")
                    append("Connection: close\r\n")
                    append("\r\n")
                }
                output.write(body.toByteArray())
            }
            output.write(data)
            output.flush()
        }
    }

    private fun sendResponse(output: java.io.OutputStream, code: Int, message: String) {
        val body = buildString {
            append("HTTP/1.1 $code $message\r\n")
            append("Content-Length: ${message.length}\r\n")
            append("Connection: close\r\n")
            append("\r\n")
            append(message)
        }
        output.write(body.toByteArray())
        output.flush()
    }

    fun register(infoHash: String, fileIndex: Int, reader: (Long, Int) -> ByteArray) {
        routes["/torrent/$infoHash/$fileIndex"] = reader
    }

    fun registerUnavailable(url: String) {
        unavailableRoutes[url] = true
    }

    fun unregister(url: String) {
        routes.remove(url)
        unavailableRoutes.remove(url)
    }

    fun stop() {
        running = false
        executor?.shutdownNow()
        executor = null
        server?.close()
        server = null
        routes.clear()
        unavailableRoutes.clear()
    }
}
