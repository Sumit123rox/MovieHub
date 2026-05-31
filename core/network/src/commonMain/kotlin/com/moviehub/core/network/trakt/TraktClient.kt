package com.moviehub.core.network.trakt

import com.moviehub.core.database.TraktSettingsRepository
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class TraktClient(
    private val httpClient: HttpClient,
    private val traktSettings: TraktSettingsRepository,
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    companion object {
        private const val API_BASE = "https://api.trakt.tv"
        // Standard Trakt client ID and secret for MovieHub
        private const val CLIENT_ID = "61af23f9b2d35e197475d409b30c5e648f86f87ad23be825c040d9bcfd713c41"
        private const val CLIENT_SECRET = "trakt_secret_placeholder"
    }

    suspend fun getDeviceCode(): Result<TraktDeviceCodeResponse> = runCatching {
        val resp = httpClient.post("$API_BASE/oauth/device/code") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("client_id" to CLIENT_ID))
        }.bodyAsText()
        json.decodeFromString<TraktDeviceCodeResponse>(resp)
    }

    suspend fun pollForToken(deviceCode: String): Result<TraktTokenResponse> = runCatching {
        val resp = httpClient.post("$API_BASE/oauth/device/token") {
            contentType(ContentType.Application.Json)
            setBody(
                mapOf(
                    "code" to deviceCode,
                    "client_id" to CLIENT_ID,
                    "client_secret" to CLIENT_SECRET
                )
            )
        }
        
        if (resp.status.value == 400 || resp.status.value == 404 || resp.status.value == 409 || resp.status.value == 418) {
            throw Exception("Authorization pending or code expired")
        }
        
        if (!resp.status.isSuccess()) {
            throw Exception("Failed to poll token: ${resp.status}")
        }
        
        val bodyText = resp.bodyAsText()
        json.decodeFromString<TraktTokenResponse>(bodyText)
    }

    suspend fun scrobbleStart(tmdbId: Int, isMovie: Boolean, progress: Double): Result<Unit> = withToken { token ->
        httpClient.post("$API_BASE/scrobble/start") {
            header("Authorization", "Bearer $token")
            header("trakt-api-version", "2")
            header("trakt-api-key", CLIENT_ID)
            contentType(ContentType.Application.Json)
            setBody(buildScrobbleRequest(tmdbId, isMovie, progress))
        }
        Unit
    }

    suspend fun scrobblePause(tmdbId: Int, isMovie: Boolean, progress: Double): Result<Unit> = withToken { token ->
        httpClient.post("$API_BASE/scrobble/pause") {
            header("Authorization", "Bearer $token")
            header("trakt-api-version", "2")
            header("trakt-api-key", CLIENT_ID)
            contentType(ContentType.Application.Json)
            setBody(buildScrobbleRequest(tmdbId, isMovie, progress))
        }
        Unit
    }

    suspend fun scrobbleStop(tmdbId: Int, isMovie: Boolean, progress: Double): Result<Unit> = withToken { token ->
        httpClient.post("$API_BASE/scrobble/stop") {
            header("Authorization", "Bearer $token")
            header("trakt-api-version", "2")
            header("trakt-api-key", CLIENT_ID)
            contentType(ContentType.Application.Json)
            setBody(buildScrobbleRequest(tmdbId, isMovie, progress))
        }
        Unit
    }

    private fun buildScrobbleRequest(tmdbId: Int, isMovie: Boolean, progress: Double): TraktScrobbleRequest {
        val ids = TraktIds(tmdb = tmdbId)
        return if (isMovie) {
            TraktScrobbleRequest(
                movie = TraktMovie(ids = ids),
                progress = progress
            )
        } else {
            TraktScrobbleRequest(
                episode = TraktEpisode(ids = ids),
                progress = progress
            )
        }
    }

    private suspend inline fun <T> withToken(crossinline block: suspend (token: String) -> T): Result<T> {
        val token = traktSettings.getAccessToken()
        if (token.isBlank()) return Result.failure(Exception("Trakt not authenticated"))
        return runCatching { block(token) }
    }
}

@Serializable
data class TraktDeviceCodeResponse(
    @SerialName("device_code") val deviceCode: String = "",
    @SerialName("user_code") val userCode: String = "",
    @SerialName("verification_url") val verificationUrl: String = "",
    @SerialName("expires_in") val expiresIn: Int = 0,
    val interval: Int = 5
)

@Serializable
data class TraktTokenResponse(
    @SerialName("access_token") val accessToken: String = "",
    @SerialName("token_type") val tokenType: String = "",
    @SerialName("expires_in") val expiresIn: Long = 0,
    @SerialName("refresh_token") val refreshToken: String = "",
    val scope: String = "",
    @SerialName("created_at") val createdAt: Long = 0
)

@Serializable
data class TraktScrobbleRequest(
    val movie: TraktMovie? = null,
    val episode: TraktEpisode? = null,
    val progress: Double
)

@Serializable
data class TraktMovie(
    val title: String? = null,
    val year: Int? = null,
    val ids: TraktIds
)

@Serializable
data class TraktEpisode(
    val ids: TraktIds
)

@Serializable
data class TraktIds(
    val trakt: Int? = null,
    val imdb: String? = null,
    val tmdb: Int? = null
)
