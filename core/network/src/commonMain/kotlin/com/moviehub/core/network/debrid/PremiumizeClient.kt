package com.moviehub.core.network.debrid

import com.moviehub.core.database.DebridSettingsRepository
import com.moviehub.core.model.StreamItem
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

/**
 * Premiumize.me client — same DebridProvider interface.
 */
class PremiumizeClient(
    private val httpClient: HttpClient,
    private val settingsRepository: DebridSettingsRepository,
) : DebridProvider {
    override val name = "Premiumize"
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    override suspend fun isConfigured(): Boolean =
        settingsRepository.getApiKey("premiumize").isNotBlank()

    override suspend fun resolveToDirectUrl(stream: StreamItem): Result<String> {
        val magnet = buildMagnet(stream) ?: return Result.failure(Exception("No magnet/hash for stream"))
        val apiKey = settingsRepository.getApiKey("premiumize")
        if (apiKey.isBlank()) return Result.failure(Exception("Premiumize not configured"))

        return runCatching {
            // First, attempt directdl in case it's already cached (instant playback)
            val directDlText = httpClient.post("https://www.premiumize.me/api/transfer/directdl") {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody("apikey=$apiKey&src=$magnet")
            }.bodyAsText()

            val directDlResponse = json.decodeFromString<PremiumizeDirectDlResponse>(directDlText)
            if (directDlResponse.status == "success" && directDlResponse.content.isNotEmpty()) {
                val bestFile = directDlResponse.content.maxByOrNull { it.size }
                    ?: directDlResponse.content.first()
                return@runCatching bestFile.link
            }

            // Not instantly available. Create transfer (download to cloud)
            val createText = httpClient.post("https://www.premiumize.me/api/transfer/create") {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody("apikey=$apiKey&src=$magnet")
            }.bodyAsText()

            val createResponse = json.decodeFromString<PremiumizeCreateResponse>(createText)
            if (createResponse.status != "success") {
                throw Exception("Create transfer failed: ${createResponse.message ?: "Unknown error"}")
            }

            val transferId = createResponse.id
                ?: throw Exception("No transfer ID returned")

            // Poll transfer list
            var attempts = 0
            val maxAttempts = 30
            var completed = false

            while (attempts < maxAttempts) {
                attempts++
                delay(3000)

                val listText = httpClient.get("https://www.premiumize.me/api/transfer/list") {
                    parameter("apikey", apiKey)
                }.bodyAsText()

                val listResponse = json.decodeFromString<PremiumizeTransferListResponse>(listText)
                if (listResponse.status == "success") {
                    val matchingTransfer = listResponse.transfers.firstOrNull { it.id == transferId }
                    if (matchingTransfer == null) {
                        // Might have finished quickly and been removed from list to cloud folder
                        completed = true
                        break
                    }

                    if (matchingTransfer.status == "finished") {
                        completed = true
                        break
                    } else if (matchingTransfer.status == "error") {
                        throw Exception("Premiumize transfer failed with error")
                    }
                }
            }

            if (!completed) {
                throw Exception("Premiumize transfer processing timed out")
            }

            // Resolve directdl again now that it is completed
            val finalDlText = httpClient.post("https://www.premiumize.me/api/transfer/directdl") {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody("apikey=$apiKey&src=$magnet")
            }.bodyAsText()

            val finalDlResponse = json.decodeFromString<PremiumizeDirectDlResponse>(finalDlText)
            if (finalDlResponse.status == "success" && finalDlResponse.content.isNotEmpty()) {
                val bestFile = finalDlResponse.content.maxByOrNull { it.size }
                    ?: finalDlResponse.content.first()
                bestFile.link
            } else {
                throw Exception("Failed to resolve playable link from finished transfer: ${finalDlResponse.message}")
            }
        }
    }

    override suspend fun checkCached(infoHash: String): Result<List<String>> {
        val apiKey = settingsRepository.getApiKey("premiumize")
        if (apiKey.isBlank()) return Result.success(emptyList())

        return runCatching {
            val responseText = httpClient.post("https://www.premiumize.me/api/cache/check") {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody("apikey=$apiKey&items[]=$infoHash")
            }.bodyAsText()

            val cacheResponse = json.decodeFromString<PremiumizeCacheCheckResponse>(responseText)
            if (cacheResponse.status == "success" && cacheResponse.response.firstOrNull() == true) {
                val fileName = cacheResponse.filename.firstOrNull()
                if (fileName != null) listOf(fileName) else listOf("Instant Cached")
            } else {
                emptyList()
            }
        }
    }

    private fun buildMagnet(stream: StreamItem): String? {
        val url = stream.url ?: stream.externalUrl
        if (url != null && (url.startsWith("magnet:") || url.contains(".torrent"))) return url
        val hash = stream.infoHash ?: return null
        val name = stream.name?.let { "&dn=${it.replace(" ", "+")}" } ?: ""
        return "magnet:?xt=urn:btih:$hash$name"
    }
}

@Serializable
data class PremiumizeCacheCheckResponse(
    val status: String,
    val response: List<Boolean> = emptyList(),
    val filename: List<String?> = emptyList(),
    val filesize: List<Long?> = emptyList()
)

@Serializable
data class PremiumizeDirectDlResponse(
    val status: String,
    val message: String? = null,
    val content: List<PremiumizeFile> = emptyList()
)

@Serializable
data class PremiumizeFile(
    val link: String = "",
    val name: String = "",
    val size: Long = 0
)

@Serializable
data class PremiumizeCreateResponse(
    val status: String,
    val id: String? = null,
    val message: String? = null
)

@Serializable
data class PremiumizeTransferListResponse(
    val status: String,
    val transfers: List<PremiumizeTransfer> = emptyList()
)

@Serializable
data class PremiumizeTransfer(
    val id: String = "",
    val name: String = "",
    val status: String = "",
    val progress: Float = 0f
)
