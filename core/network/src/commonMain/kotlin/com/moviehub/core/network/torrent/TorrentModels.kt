package com.moviehub.core.network.torrent

data class TorrentConfig(
    val listenPort: Int = 6881,
    val enableDht: Boolean = true,
    val enablePex: Boolean = true,
    val maxConnections: Int = 50,
    val downloadRateLimit: Int = 0, // bytes/s, 0 = unlimited
    val uploadRateLimit: Int = 0, // bytes/s, 0 = unlimited
    val httpStreamPort: Int = 8888, // local HTTP server port for ExoPlayer
    val maxActiveDownloads: Int = 3,
)

data class MagnetMetadata(
    val infoHash: String,
    val magnetUri: String,
    val name: String? = null,
    val trackers: List<String> = emptyList(),
)

data class TorrentFileInfo(
    val fileIndex: Int,
    val path: String,
    val size: Long,
    val isVideo: Boolean = path.let {
        val lower = it.lowercase()
        lower.endsWith(".mp4") || lower.endsWith(".mkv") || lower.endsWith(".avi") ||
            lower.endsWith(".mov") || lower.endsWith(".webm") || lower.endsWith(".m4v") ||
            lower.endsWith(".ts") || lower.endsWith(".wmv") || lower.endsWith(".flv")
    },
)

enum class TorrentStatus {
    QUEUED, CHECKING, DOWNLOADING_METADATA, DOWNLOADING, SEEDING, PAUSED, FINISHED, ERROR
}

data class TorrentProgress(
    val torrentId: String,
    val status: TorrentStatus,
    val downloadRate: Long = 0, // bytes/s
    val uploadRate: Long = 0, // bytes/s
    val totalDownloaded: Long = 0,
    val totalSize: Long = 0,
    val progress: Float = 0f, // 0..1
    val numPeers: Int = 0,
    val numSeeds: Int = 0,
    val numPieces: Int = 0,
    val piecesHave: Int = 0,
    val selectedFile: TorrentFileInfo? = null,
    val localStreamUrl: String? = null, // http://localhost:PORT/file
)

/** Callback interface for torrent state changes. */
fun interface TorrentListener {
    fun onProgress(progress: TorrentProgress)
    fun onError(torrentId: String, error: String) {}
    fun onFinished(torrentId: String) {}
}
