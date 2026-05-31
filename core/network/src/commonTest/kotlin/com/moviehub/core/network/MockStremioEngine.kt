package com.moviehub.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object MockStremioEngine {
    val successManifest = """
        {
            "id": "test.addon",
            "name": "Test Addon",
            "version": "1.0.0",
            "description": "A test addon",
            "resources": [],
            "types": ["movie"],
            "catalogs": []
        }
    """.trimIndent()

    val malformedJson = """
        {
            "id": "broken.addon",
            "name": "Broken Addon",
            "version": "1.0.0"
            "description": missing quote
        }
    """.trimIndent()

    fun create(responses: Map<String, String>): HttpClient {
        val mockEngine = MockEngine { request ->
            val url = request.url.toString()
            val responseBody = responses.entries.find { url.contains(it.key) }?.value

            if (responseBody != null) {
                if (responseBody == "ERROR_500") {
                    respond(
                        content = "Server Error",
                        status = HttpStatusCode.InternalServerError,
                        headers = headersOf(HttpHeaders.ContentType, "text/plain"),
                    )
                } else {
                    respond(
                        content = responseBody,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            } else {
                respond(
                    content = "Not Found",
                    status = HttpStatusCode.NotFound,
                    headers = headersOf(HttpHeaders.ContentType, "text/plain"),
                )
            }
        }

        return HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                    },
                )
            }
        }
    }
}
