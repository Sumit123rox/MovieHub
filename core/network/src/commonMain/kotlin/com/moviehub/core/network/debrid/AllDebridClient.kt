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
 * AllDebrid (alldebrid.com) client — same pattern as RealDebridClient.
 */
class AllDebridClient(
    private val httpClient: HttpClient,
    private val settingsRepository: DebridSettingsRepository,
) : DebridProvider {
    override val name = "AllDebrid"
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    override suspend fun isConfigured(): Boolean =
        settingsRepository.getApiKey("alldebrid").isNotBlank()

    override suspend fun resolveToDirectUrl(stream: StreamItem): Result<String> {
        val magnet = buildMagnet(stream) ?: return Result.failure(Exception("No magnet/hash for stream"))
        val apiKey = settingsRepository.getApiKey("alldebrid")
        if (apiKey.isBlank()) return Result.failure(Exception("AllDebrid not configured"))

        return runCatching {
            // Step 1: Upload magnet
            val uploadRespText = httpClient.post("https://api.alldebrid.com/v4/magnet/upload") {
                parameter("agent", "MovieHub")
                parameter("apikey", apiKey)
                contentType(ContentType.Application.FormUrlEncoded)
                setBody("magnets[]=$magnet")
            }.bodyAsText()

            val uploadJson = json.parseToJsonElement(uploadRespText).jsonObject
            val status = uploadJson["status"]?.jsonPrimitive?.content
            if (status != "success") {
                val errorMsg = uploadJson["error"]?.jsonObject?.get("message")?.jsonPrimitive?.content ?: "Unknown error"
                throw Exception("Upload failed: $errorMsg")
            }

            val uploadData = uploadJson["data"]?.jsonObject
            val magnetsArray = uploadData?.get("magnets")?.jsonArray
            val uploadedMagnet = magnetsArray?.firstOrNull()?.jsonObject
                ?: throw Exception("No magnet returned in upload response")

            val error = uploadedMagnet["error"]?.jsonObject
            if (error != null) {
                val errorMsg = error["message"]?.jsonPrimitive?.content ?: "Unknown magnet error"
                throw Exception("Magnet error: $errorMsg")
            }

            val magnetId = uploadedMagnet["id"]?.jsonPrimitive?.long
                ?: throw Exception("No magnet ID returned")

            // Step 2: Poll status
            var attempts = 0
            val maxAttempts = 30
            var finalLink: String? = null

            while (attempts < maxAttempts) {
                attempts++
                delay(2000)

                val statusRespText = httpClient.get("https://api.alldebrid.com/v4.1/magnet/status") {
                    parameter("agent", "MovieHub")
                    parameter("apikey", apiKey)
                    parameter("id", magnetId)
                }.bodyAsText()

                val statusJson = json.parseToJsonElement(statusRespText).jsonObject
                if (statusJson["status"]?.jsonPrimitive?.content != "success") continue

                val dataObj = statusJson["data"]?.jsonObject ?: continue
                val magnetsElem = dataObj["magnets"]

                val magnetStatusList = if (magnetsElem is JsonArray) {
                    json.decodeFromJsonElement<List<AllDebridStatusMagnet>>(magnetsElem)
                } else if (magnetsElem is JsonObject) {
                    listOf(json.decodeFromJsonElement<AllDebridStatusMagnet>(magnetsElem))
                } else {
                    emptyList()
                }

                val currentMagnet = magnetStatusList.firstOrNull() ?: continue
                
                // AllDebrid statusCode mapping:
                // 0: Processing
                // 1: Downloading
                // 2: Compress / Uploading
                // 3: Saving
                // 4: Ready
                // 70+: Error states
                if (currentMagnet.statusCode >= 70) {
                    throw Exception("Magnet download failed on AllDebrid (code: ${currentMagnet.statusCode})")
                }

                if (currentMagnet.statusCode == 4 || currentMagnet.status == "ready") {
                    // Ready! Find the largest playable link or the first one
                    val linkObj = currentMagnet.links.maxByOrNull { it.size }
                        ?: currentMagnet.links.firstOrNull()
                        ?: throw Exception("No links found in ready magnet")
                    finalLink = linkObj.link
                    break
                }
            }

            val linkToUnlock = finalLink ?: throw Exception("Magnet processing timed out")

            // Step 3: Unlock link
            val unlockRespText = httpClient.post("https://api.alldebrid.com/v4/link/unlock") {
                parameter("agent", "MovieHub")
                parameter("apikey", apiKey)
                parameter("link", linkToUnlock)
            }.bodyAsText()

            val unlockJson = json.parseToJsonElement(unlockRespText).jsonObject
            if (unlockJson["status"]?.jsonPrimitive?.content != "success") {
                val errorMsg = unlockJson["error"]?.jsonObject?.get("message")?.jsonPrimitive?.content ?: "Unlock failed"
                throw Exception(errorMsg)
            }

            val unlockData = unlockJson["data"]?.jsonObject
                ?: throw Exception("No data in unlock response")
            val directUrl = unlockData["link"]?.jsonPrimitive?.content
                ?: throw Exception("No direct link in unlock response")

            directUrl
        }
    }

    override suspend fun checkCached(infoHash: String): Result<List<String>> {
        val apiKey = settingsRepository.getApiKey("alldebrid")
        if (apiKey.isBlank()) return Result.success(emptyList())

        return runCatching {
            val respText = httpClient.get("https://api.alldebrid.com/v4/magnet/instant") {
                parameter("agent", "MovieHub")
                parameter("apikey", apiKey)
                parameter("magnets[]", infoHash)
            }.bodyAsText()

            val response = json.decodeFromString<AllDebridResponse<AllDebridInstantData>>(respText)
            if (response.status != "success") return Result.success(emptyList())

            val instantMagnet = response.data?.magnets?.firstOrNull()
            if (instantMagnet?.instant == true && instantMagnet.files != null) {
                instantMagnet.files.map { it.n }
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
data class AllDebridResponse<T>(
    val status: String,
    val data: T? = null,
    val error: AllDebridError? = null
)

@Serializable
data class AllDebridError(
    val code: String = "",
    val message: String = ""
)

@Serializable
data class AllDebridStatusMagnet(
    val id: Long = 0,
    val filename: String? = null,
    val status: String = "",
    val statusCode: Int = 0,
    val links: List<AllDebridLink> = emptyList()
)

@Serializable
data class AllDebridLink(
    val link: String = "",
    val filename: String = "",
    val size: Long = 0
)

@Serializable
data class AllDebridInstantData(
    val magnets: List<AllDebridInstantMagnet> = emptyList()
)

@Serializable
data class AllDebridInstantMagnet(
    val magnet: String = "",
    val instant: Boolean = false,
    val files: List<AllDebridInstantFile>? = null
)

@Serializable
data class AllDebridInstantFile(
    val n: String = "",
    val s: Long = 0
)
