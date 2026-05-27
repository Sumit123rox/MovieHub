package com.moviehub.core.network

import co.touchlab.kermit.Logger
import com.moviehub.core.model.TrailerPlaybackSource
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*

/**
 * A robust YouTube extractor for playing trailers in the app's universal player.
 * Ported and optimized from the Nuvio reference app.
 */
class YouTubePlaybackResolver(
    private val httpClient: HttpClient
) {
    private val logger = Logger.withTag("YTResolver")
    private val json = Json { ignoreUnknownKeys = true }

    private val videoIdRegex = Regex("^[a-zA-Z0-9_-]{11}$")
    private val apiKeyRegex = Regex("\"INNERTUBE_API_KEY\":\"([^\"]+)\"")
    private val visitorDataRegex = Regex("\"VISITOR_DATA\":\"([^\"]+)\"")

    suspend fun resolveFromYouTubeId(videoId: String): TrailerPlaybackSource? = withContext(Dispatchers.Default) {
        if (!videoIdRegex.matches(videoId)) return@withContext null

        try {
            // 1. Fetch the watch page to extract InnerTube API keys and Visitor data
            val watchUrl = "https://www.youtube.com/watch?v=$videoId&hl=en&bpctr=9999999999&has_verified=1"
            val watchResponse = httpClient.get(watchUrl) {
                header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                header("Accept-Language", "en-US,en;q=0.9")
            }
            
            if (!watchResponse.status.isSuccess()) return@withContext null
            val html = watchResponse.bodyAsText()

            val apiKey = apiKeyRegex.find(html)?.groupValues?.getOrNull(1) ?: return@withContext null
            val visitorData = visitorDataRegex.find(html)?.groupValues?.getOrNull(1)

            // 2. Query InnerTube Player API using multiple client profiles for better compatibility
            val clients = listOf(
                "ANDROID_VR" to "1.56.21",
                "ANDROID" to "17.31.35",
                "TVHTML5" to "7.20230405.08.01"
            )

            for ((clientName, clientVersion) in clients) {
                val source = fetchFromInnerTube(videoId, apiKey, visitorData, clientName, clientVersion)
                if (source != null) return@withContext source
            }

            null
        } catch (e: Exception) {
            logger.e(e) { "Failed to resolve YouTube trailer: $videoId" }
            null
        }
    }

    private suspend fun fetchFromInnerTube(
        videoId: String,
        apiKey: String,
        visitorData: String?,
        clientName: String,
        clientVersion: String
    ): TrailerPlaybackSource? {
        return try {
            val endpoint = "https://www.youtube.com/youtubei/v1/player?key=$apiKey"
            val playerResponse = httpClient.post(endpoint) {
                contentType(ContentType.Application.Json)
                if (visitorData != null) header("X-Goog-Visitor-Id", visitorData)
                
                setBody(buildJsonObject {
                    put("videoId", videoId)
                    put("contentCheckOk", true)
                    put("racyCheckOk", true)
                    putJsonObject("context") {
                        putJsonObject("client") {
                            put("clientName", clientName)
                            put("clientVersion", clientVersion)
                            put("platform", "MOBILE")
                            put("osName", "Android")
                            put("osVersion", "12")
                        }
                    }
                    putJsonObject("playbackContext") {
                        putJsonObject("contentPlaybackContext") {
                            put("html5Preference", "HTML5_PREF_WANTS")
                        }
                    }
                })
            }

            if (!playerResponse.status.isSuccess()) return null
            val responseBody = playerResponse.bodyAsText()
            val responseJson = json.parseToJsonElement(responseBody).jsonObject

            val playabilityStatus = responseJson["playabilityStatus"]?.jsonObject
            val status = playabilityStatus?.get("status")?.jsonPrimitive?.content
            if (status != "OK") {
                logger.w { "InnerTube $clientName status: $status" }
                return null
            }

            val streamingData = responseJson["streamingData"]?.jsonObject ?: return null
            
            // 1. Prefer HLS Manifest for adaptive bitrate and performance
            val hlsUrl = streamingData["hlsManifestUrl"]?.jsonPrimitive?.content
            if (!hlsUrl.isNullOrBlank()) {
                return TrailerPlaybackSource(videoUrl = hlsUrl)
            }

            // 2. Fallback to high-quality progressive MP4
            val formats = streamingData["formats"]?.jsonArray
            val bestProgressive = formats?.mapNotNull { it.jsonObject }
                ?.filter { 
                    val mime = it["mimeType"]?.jsonPrimitive?.content ?: ""
                    mime.contains("video/mp4")
                }
                ?.maxByOrNull { it["height"]?.jsonPrimitive?.intOrNull ?: 0 }
                ?.get("url")?.jsonPrimitive?.content

            if (!bestProgressive.isNullOrBlank()) {
                return TrailerPlaybackSource(videoUrl = bestProgressive)
            }

            null
        } catch (e: Exception) {
            logger.w { "InnerTube $clientName failed: ${e.message}" }
            null
        }
    }

    suspend fun searchTrailer(query: String): String? = withContext(Dispatchers.Default) {
        try {
            val queryEncoded = query.replace(" ", "+")
            val searchUrl = "https://www.youtube.com/results?search_query=$queryEncoded"
            val response = httpClient.get(searchUrl) {
                header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                header("Accept-Language", "en-US,en;q=0.9")
            }
            if (!response.status.isSuccess()) return@withContext null
            val html = response.bodyAsText()
            val match = Regex("\"videoId\":\"([a-zA-Z0-9_-]{11})\"").find(html)
            match?.groupValues?.getOrNull(1)
        } catch (e: Exception) {
            logger.e(e) { "Failed to search YouTube trailer for query: $query" }
            null
        }
    }
}

