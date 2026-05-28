package com.moviehub.core.network.debrid

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Response from Real-Debrid OAuth device code endpoint. */
@Serializable
data class DeviceCodeResponse(
    @SerialName("device_code") val deviceCode: String = "",
    @SerialName("user_code") val userCode: String = "",
    @SerialName("verification_url") val verificationUrl: String = "",
    @SerialName("expires_in") val expiresIn: Int = 0,
    val interval: Int = 5,
)

/** Response from Real-Debrid OAuth credential polling. */
@Serializable
data class CredentialResponse(
    @SerialName("access_token") val accessToken: String = "",
    @SerialName("expires_in") val expiresIn: Int = 0,
    val error: String? = null,
)

/** Torrent file info from Real-Debrid. */
@Serializable
data class TorrentFile(
    val id: Int = 0,
    val path: String = "",
    val bytes: Long = 0,
    val selected: Int = 0,
)

/** Torrent info response. */
@Serializable
data class TorrentInfo(
    val id: String = "",
    val filename: String = "",
    @SerialName("original_filename") val originalFilename: String = "",
    val hash: String = "",
    val bytes: Long = 0,
    val files: List<TorrentFile> = emptyList(),
    val status: String = "",
    val links: List<String> = emptyFilesList,
    /** Direct download URLs (after unrestrict). Populated after processing. */
    @SerialName("downloads") val downloads: List<String> = emptyList(),
) {
    /** Pick the largest playable video file. */
    val selectedDownload: String?
        get() = if (downloads.isNotEmpty()) downloads.first() else null
}

/** Response from unrestrict link endpoint. */
@Serializable
data class UnrestrictResponse(
    val download: String = "",
    val filename: String = "",
    val filesize: Long = 0,
)

@Serializable
data class AddTorrentResponse(
    val id: String = "",
    val uri: String = "",
)

private val emptyFilesList: List<String> = emptyList()
