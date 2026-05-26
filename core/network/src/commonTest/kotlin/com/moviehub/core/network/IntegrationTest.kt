package com.moviehub.core.network

import io.ktor.client.HttpClient
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class IntegrationTest {

    @Test
    fun testRealCinemetaCatalogFetch() = runTest {
        val apiClient = StremioApiClient(HttpClient())
        val result = runCatching {
            apiClient.getCatalog(
                baseUrl = "https://v3-cinemeta.strem.io",
                type = "movie",
                id = "top"
            )
        }
        
        assertTrue(result.isSuccess, "Failed to fetch from Cinemeta: ${result.exceptionOrNull()}")
        assertTrue(result.getOrNull()?.metas?.isNotEmpty() == true, "Cinemeta returned empty catalog")
    }
}
