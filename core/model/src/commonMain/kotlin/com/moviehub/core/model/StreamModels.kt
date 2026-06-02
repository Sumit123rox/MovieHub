package com.moviehub.core.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
enum class StreamFormat(val priority: Int) {
    HLS(3),
    DASH(2),
    WEBM(1),
    MP4(0),
    UNKNOWN(-1);

    companion object {
        fun fromUrl(url: String?): StreamFormat {
            val u = url?.lowercase()?.trim() ?: return UNKNOWN
            return when {
                u.contains(".m3u8") || u.contains(".m3u") -> HLS
                u.contains(".mpd") -> DASH
                u.contains(".webm") -> WEBM
                u.contains(".mp4") -> MP4
                else -> UNKNOWN
            }
        }
    }
}

@Immutable
@Serializable
data class StreamItem(
    val name: String? = null,
    val description: String? = null,
    val url: String? = null,
    val infoHash: String? = null,
    val fileIdx: Int? = null,
    val externalUrl: String? = null,
    val sourceName: String? = null,
    val addonName: String? = null,
    val addonId: String? = null,
    val drmLicenseUrl: String? = null,
    val drmScheme: String? = null,
    val behaviorHints: StreamBehaviorHints = StreamBehaviorHints()
) {
    val isTorrentStream: Boolean
        get() = !infoHash.isNullOrBlank() ||
            url?.trimStart()?.startsWith("magnet:", ignoreCase = true) == true ||
            url?.trimStart()?.startsWith("torrent:", ignoreCase = true) == true ||
            url?.trim()?.contains(".torrent", ignoreCase = true) == true ||
            externalUrl?.trimStart()?.startsWith("magnet:", ignoreCase = true) == true ||
            externalUrl?.trimStart()?.startsWith("torrent:", ignoreCase = true) == true ||
            externalUrl?.trim()?.contains(".torrent", ignoreCase = true) == true

    val hasPlayableSource: Boolean
        get() = url != null || infoHash != null || externalUrl != null

    val streamFormat: StreamFormat
        get() = StreamFormat.fromUrl(url ?: externalUrl)

    /** Combined sort score: quality tier first, then stream format as tiebreaker. */
    val playbackPriority: Int
        get() {
            val nameLc = name?.lowercase() ?: ""
            val descLc = description?.lowercase() ?: ""
            val urlLc = url?.lowercase() ?: ""

            val qualityScore = when {
                nameLc.contains("4k") || nameLc.contains("2160") -> 1000
                nameLc.contains("1080") || nameLc.contains("fhd") -> 750
                nameLc.contains("720") || nameLc.contains("hd") -> 500
                nameLc.contains("480") || nameLc.contains("sd") -> 250
                else -> 0
            }

            // Give HLS (.m3u8) and DASH (.mpd) a significant boost to prioritize adaptive playback
            val formatBoost = when (streamFormat) {
                StreamFormat.HLS -> 2000
                StreamFormat.DASH -> 1500
                else -> 0
            }

            // Penalize non-webready or raw mp4 files slightly if we have alternatives
            val penalty = if (behaviorHints.notWebReady) -50 else 0

            return qualityScore + formatBoost + streamFormat.priority + penalty
        }
}

@Immutable
@Serializable
data class StreamBehaviorHints(
    val bingeGroup: String? = null,
    val notWebReady: Boolean = false,
    val videoSize: Long? = null,
    val filename: String? = null,
    val proxyHeaders: StreamProxyHeaders? = null
)

@Immutable
@Serializable
data class StreamProxyHeaders(
    val request: Map<String, String>? = null,
    val response: Map<String, String>? = null
)

@Immutable
@Serializable
data class StremioStreamResponse(
    val streams: List<StreamItem> = emptyList()
)
