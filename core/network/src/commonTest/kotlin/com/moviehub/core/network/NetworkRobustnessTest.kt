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
        assertEquals("test.addon", manifest?.id)

        // Malformed JSON should return null (handled by client try-catch)
        val manifestMalformed = apiClient.getManifest("https://broken.addon")
        assertTrue(manifestMalformed == null)

        // 500 Error should return null (handled by client try-catch)
        val manifestError = apiClient.getManifest("https://error.addon")
        assertTrue(manifestError == null)
    }
}
