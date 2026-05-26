package com.moviehub.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ManifestDeserializationTest {

    /**
     * Full copy of the WebStreamrMBG manifest as served by
     * https://87d6a6ef6b58-webstreamrmbg.baby-beamup.club/manifest.json
     * Tests that the StremioManifest model in core/model can handle
     * complex manifests with config, stremioAddonsConfig, idPrefixes,
     * and behaviorHints with extra fields.
     */
    private val webStreamrManifest = """
{
  "id": "webstreamr-mbg",
  "version": "0.73.2",
  "name": "WebStreamrMBG",
  "description": "Provides HTTP URLs from streaming websites.",
  "resources": ["stream"],
  "types": ["movie","series"],
  "catalogs": [],
  "idPrefixes": ["tmdb:","tt"],
  "logo": "https://emojiapi.dev/api/v1/spider_web/256.png",
  "behaviorHints": {
    "p2p": false,
    "configurable": true,
    "configurationRequired": false
  },
  "config": [
    {"key":"multi","type":"checkbox","title":"Multi","default":"checked"},
    {"key":"showErrors","type":"checkbox","title":"Show errors"}
  ],
  "stremioAddonsConfig": {
    "issuer": "https://stremio-addons.net",
    "signature": "abc123"
  }
}
    """.trimIndent()

    /**
     * Tests that resources can be both a simple string array AND
     * the object form used by some addons.
     */
    private val manifestWithObjectResources = """
{
  "id": "test.object.resources",
  "version": "1.0.0",
  "name": "Test Object Resources",
  "resources": [
    {"name": "stream", "types": ["movie","series"]},
    {"name": "catalog", "types": ["movie"]}
  ],
  "types": ["movie","series"],
  "catalogs": [
    {"type": "movie", "id": "top", "name": "Top Movies"},
    {"type": "series", "id": "top", "name": "Top Series"}
  ]
}
    """.trimIndent()

    private fun createMockClient(responseBody: String): HttpClient {
        val mockEngine = MockEngine { _ ->
            respond(
                content = responseBody,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        return HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    coerceInputValues = true
                })
            }
        }
    }

    @Test
    fun testWebStreamrMBGManifestDeserialization() = runTest {
        val httpClient = createMockClient(webStreamrManifest)
        val apiClient = StremioApiClient(httpClient)
        val manifest = apiClient.getManifest("https://test.com")

        assertNotNull(manifest, "WebStreamrMBG manifest should deserialize successfully")
        assertEquals("webstreamr-mbg", manifest.id)
        assertEquals("WebStreamrMBG", manifest.name)
        assertEquals("0.73.2", manifest.version)
        assertEquals(listOf("stream"), manifest.resources)
        assertEquals(listOf("movie", "series"), manifest.types)
        assertTrue(manifest.catalogs.isEmpty())
        assertTrue(manifest.behaviorHints.configurable)
    }

    @Test
    fun testManifestWithObjectResourcesDeserialization() = runTest {
        val httpClient = createMockClient(manifestWithObjectResources)
        val apiClient = StremioApiClient(httpClient)
        val manifest = apiClient.getManifest("https://test.com")

        assertNotNull(manifest, "Manifest with object resources should deserialize")
        assertEquals("test.object.resources", manifest.id)
        assertEquals("Test Object Resources", manifest.name)
        // Object-form resources should be converted to their "name" values
        assertEquals(listOf("stream", "catalog"), manifest.resources)
        assertEquals(2, manifest.catalogs.size)
        assertEquals("Top Movies", manifest.catalogs[0].name)
    }
}
