package com.moviehub.core.network

import co.touchlab.kermit.Logger
import com.moviehub.core.model.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

class StremioApiClient(
    private val httpClient: HttpClient,
    private val dispatchers: NetworkDispatchers = NetworkDispatchers(),
    private val dedupCache: DeduplicatingCache = DeduplicatingCache()
) {
    private val logger = Logger.withTag("MovieHubNet")
    private val json = Json { 
        ignoreUnknownKeys = true 
        isLenient = true
        coerceInputValues = true
    }

    private fun sanitizeBaseUrl(url: String): String {
        return url.removeSuffix("/").removeSuffix("/manifest.json")
    }

    /**
     * Safely parses the response body, ensuring it's not HTML and not a Stremio error.
     */
    private suspend fun safeParseBody(response: HttpResponse): String? {
        val url = response.call.request.url
        return try {
            if (!response.status.isSuccess()) {
                logger.w { "safeParseBody: non-success status ${response.status.value} from $url" }
                return null
            }
            val body = response.bodyAsText().trim()

            if (body.isEmpty()) {
                logger.w { "safeParseBody: empty body from $url" }
                return null
            }

            // 1. Case-insensitive check for HTML markers to prevent common community addon error pages
            val lowerBody = body.lowercase()
            if (lowerBody.startsWith("<!doctype") || lowerBody.startsWith("<html") || lowerBody.startsWith("<body")) {
                logger.w { "safeParseBody: addon returned HTML instead of JSON from $url (first 100 chars: ${body.take(100)})" }
                return null
            }

            // 2. Ensure it actually starts like JSON (either an object or an array)
            if (!body.startsWith("{") && !body.startsWith("[")) {
                logger.w { "safeParseBody: addon returned non-JSON content from $url (first 100 chars: ${body.take(100)})" }
                return null
            }

            // 3. Check for Stremio-specific error payloads
            if (body.contains("\"err\":") || body.contains("\"error\":")) {
                logger.w { "safeParseBody: addon returned error payload from $url" }
                null
            } else {
                body
            }
        } catch (e: Exception) {
            logger.e(e) { "safeParseBody: exception reading body from $url: ${e.message}" }
            null
        }
    }

    suspend fun getManifest(baseUrl: String): StremioManifest? {
        val url = if (baseUrl.endsWith("/manifest.json")) baseUrl else "${baseUrl.removeSuffix("/")}/manifest.json"
        logger.i { "Fetching manifest: $url" }
        return withContext(dispatchers.io) {
            dedupCache.get(url) {
                try {
                    val response = httpClient.get(url)
                    val responseStatus = response.status
                    logger.i { "Manifest HTTP response: ${responseStatus.value} for $url" }
                    if (!responseStatus.isSuccess()) {
                        logger.w { "Manifest request returned non-success status ${responseStatus.value} from $url" }
                        null
                    } else {
                        val body = safeParseBody(response)
                        if (body == null) {
                            logger.w { "safeParseBody returned null for manifest from $url" }
                            null
                        } else {
                            json.decodeFromString<StremioManifest>(body).also {
                                logger.i { "Manifest fetched successfully: ${it.name} (id=${it.id}, resources=${it.resources})" }
                            }
                        }
                    }
                } catch (e: Exception) {
                    logger.e(e) { "Error fetching/decoding manifest from $url: ${e.message}" }
                    null
                }
            }
        }
    }

    private fun String.encodeAddonPathSegment(): String = buildString {
        this@encodeAddonPathSegment.encodeToByteArray().forEach { byte ->
            val value = byte.toInt() and 0xFF
            val char = value.toChar()
            if (char in 'a'..'z' || char in 'A'..'Z' || char in '0'..'9' || char == '-' || char == '_' || char == '.' || char == '~' || char == ':') {
                append(char)
            } else {
                append('%')
                append("0123456789ABCDEF"[value shr 4])
                append("0123456789ABCDEF"[value and 0x0F])
            }
        }
    }

    suspend fun getCatalog(
        baseUrl: String,
        type: String,
        id: String,
        extra: Map<String, String> = emptyMap()
    ): CatalogResponse? {
        val sanitizedBase = sanitizeBaseUrl(baseUrl)
        val encodedId = id.encodeAddonPathSegment()
        val extraPath = if (extra.isEmpty()) "" else "/${extra.map { "${it.key.encodeAddonPathSegment()}=${it.value.encodeAddonPathSegment()}" }.joinToString("&")}"
        val url = "$sanitizedBase/catalog/$type/$encodedId$extraPath.json"
        logger.i { "Fetching catalog: $url" }
        return withContext(dispatchers.io) {
            dedupCache.get(url) {
                try {
                    val response = httpClient.get(url)
                    val body = safeParseBody(response)
                    if (body == null) {
                        logger.w { "safeParseBody returned null for catalog from $url" }
                        null
                    } else {
                        json.decodeFromString<CatalogResponse>(body).also {
                            logger.i { "Catalog fetched successfully: ${it.metas.size} items" }
                        }
                    }
                } catch (e: Exception) {
                    logger.e(e) { "Error fetching/decoding catalog from $url" }
                    null
                }
            }
        }
    }

    suspend fun getMeta(
        baseUrl: String,
        type: String,
        id: String
    ): MetaResponse? {
        val sanitizedBase = sanitizeBaseUrl(baseUrl)
        val encodedId = id.encodeAddonPathSegment()
        val url = "$sanitizedBase/meta/$type/$encodedId.json"
        logger.i { "Fetching meta: $url" }
        return withContext(dispatchers.io) {
            dedupCache.get(url) {
                try {
                    val response = httpClient.get(url)
                    val body = safeParseBody(response)
                    if (body == null) {
                        logger.w { "safeParseBody returned null for meta from $url" }
                        null
                    } else {
                        json.decodeFromString<MetaResponse>(body).also {
                            logger.i { "Meta fetched successfully for: ${it.meta.name}" }
                        }
                    }
                } catch (e: Exception) {
                    logger.e(e) { "Error fetching/decoding meta from $url" }
                    null
                }
            }
        }
    }

    suspend fun getStreams(
        baseUrl: String,
        type: String,
        id: String,
        addonName: String = "Unknown",
        addonId: String = ""
    ): List<StreamItem> {
        val sanitizedBase = sanitizeBaseUrl(baseUrl)
        val encodedId = id.encodeAddonPathSegment()
        val url = "$sanitizedBase/stream/$type/$encodedId.json"
        logger.i { "Fetching streams: $url" }
        return withContext(dispatchers.io) {
            dedupCache.get(url) {
                try {
                    val response = httpClient.get(url)
                    val body = safeParseBody(response)
                    if (body == null) {
                        logger.w { "safeParseBody returned null for streams from $url" }
                        emptyList()
                    } else {
                        StreamParser.parse(body, addonName, addonId).also {
                            logger.i { "Streams fetched successfully: ${it.size} streams" }
                        }
                    }
                } catch (e: Exception) {
                    logger.e(e) { "Error fetching/parsing streams from $url" }
                    emptyList()
                }
            }
        }
    }
}
