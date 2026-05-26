package com.moviehub.core.model

import kotlinx.serialization.Serializable

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
}

@Serializable
data class StreamBehaviorHints(
    val bingeGroup: String? = null,
    val notWebReady: Boolean = false,
    val videoSize: Long? = null,
    val filename: String? = null,
    val proxyHeaders: StreamProxyHeaders? = null
)

@Serializable
data class StreamProxyHeaders(
    val request: Map<String, String>? = null,
    val response: Map<String, String>? = null
)

@Serializable
data class StremioStreamResponse(
    val streams: List<StreamItem> = emptyList()
)
