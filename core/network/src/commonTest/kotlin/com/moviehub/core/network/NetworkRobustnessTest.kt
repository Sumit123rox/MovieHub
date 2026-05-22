package com.moviehub.core.network

import com.moviehub.core.network.mapper.toDomain
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NetworkRobustnessTest {

    @Test
    fun testStremioApiClientGracefulFailure() = runTest {
        val httpClient = MockStremioEngine.create(
            mapOf(
                "success.addon" to MockStremioEngine.successManifest,
                "broken.addon" to MockStremioEngine.malformedJson,
                "error.addon" to "ERROR_500"
            )
        )
        val apiClient = StremioApiClient(httpClient)

        // Success Case
        val manifest = apiClient.getManifest("https://success.addon")
        assertEquals("test.addon", manifest.id)

        // Malformed JSON should throw SerializationException (handled by repo try-catch)
        val resultMalformed = runCatching { apiClient.getManifest("https://broken.addon") }
        assertTrue(resultMalformed.isFailure)

        // 500 Error
        val resultError = runCatching { apiClient.getManifest("https://error.addon") }
        assertTrue(resultError.isFailure)
    }
}
