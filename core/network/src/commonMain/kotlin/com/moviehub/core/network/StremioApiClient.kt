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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

class StremioApiClient(
    private val httpClient: HttpClient
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
        return try {
            if (!response.status.isSuccess()) return null
            val body = response.bodyAsText().trim()
            
            // 1. Case-insensitive check for HTML markers to prevent common community addon error pages
            val lowerBody = body.lowercase()
            if (lowerBody.startsWith("<!doctype") || lowerBody.startsWith("<html") || lowerBody.startsWith("<body")) {
                logger.w { "Addon returned HTML instead of JSON from ${response.call.request.url}" }
                return null
            }
            
            // 2. Ensure it actually starts like JSON (either an object or an array)
            if (!body.startsWith("{") && !body.startsWith("[")) {
                logger.w { "Addon returned non-JSON content from ${response.call.request.url}" }
                return null
            }

            // 3. Check for Stremio-specific error payloads
            if (body.contains("\"err\":") || body.contains("\"error\":")) {
                logger.w { "Addon returned a handler error from ${response.call.request.url}" }
                null
            } else {
                body
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getManifest(baseUrl: String): StremioManifest? {
        val url = if (baseUrl.endsWith("/manifest.json")) baseUrl else "${baseUrl.removeSuffix("/")}/manifest.json"
        logger.i { "Fetching manifest: $url" }
        return try {
            val response = httpClient.get(url)
            val body = safeParseBody(response) ?: return null
            json.decodeFromString<StremioManifest>(body).also {
                logger.i { "Manifest fetched successfully: ${it.name}" }
            }
        } catch (e: Exception) {
            logger.e(e) { "Error fetching/decoding manifest from $url" }
            null
        }
    }

    suspend fun getCatalog(
        baseUrl: String,
        type: String,
        id: String,
        extra: Map<String, String> = emptyMap()
    ): CatalogResponse? {
        val sanitizedBase = sanitizeBaseUrl(baseUrl)
        val extraPath = if (extra.isEmpty()) "" else "/${extra.map { "${it.key}=${it.value}" }.joinToString(",")}"
        val url = "$sanitizedBase/catalog/$type/$id$extraPath.json"
        logger.i { "Fetching catalog: $url" }
        return try {
            val response = httpClient.get(url)
            val body = safeParseBody(response) ?: return null
            json.decodeFromString<CatalogResponse>(body).also {
                logger.i { "Catalog fetched successfully: ${it.metas.size} items" }
            }
        } catch (e: Exception) {
            logger.e(e) { "Error fetching/decoding catalog from $url" }
            null
        }
    }

    suspend fun getMeta(
        baseUrl: String,
        type: String,
        id: String
    ): MetaResponse? {
        val sanitizedBase = sanitizeBaseUrl(baseUrl)
        val url = "$sanitizedBase/meta/$type/$id.json"
        logger.i { "Fetching meta: $url" }
        return try {
            val response = httpClient.get(url)
            val body = safeParseBody(response) ?: return null
            json.decodeFromString<MetaResponse>(body).also {
                logger.i { "Meta fetched successfully for: ${it.meta.name}" }
            }
        } catch (e: Exception) {
            logger.e(e) { "Error fetching/decoding meta from $url" }
            null
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
        val url = "$sanitizedBase/stream/$type/$id.json"
        logger.i { "Fetching streams: $url" }
        return try {
            val response = httpClient.get(url)
            val body = safeParseBody(response) ?: return emptyList()
            StreamParser.parse(body, addonName, addonId).also {
                logger.i { "Streams fetched successfully: ${it.size} streams" }
            }
        } catch (e: Exception) {
            logger.e(e) { "Error fetching/parsing streams from $url" }
            emptyList()
        }
    }
}
