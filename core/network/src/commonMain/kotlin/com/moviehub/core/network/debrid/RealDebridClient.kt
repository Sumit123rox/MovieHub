package com.moviehub.core.network.debrid

import com.moviehub.core.database.DebridSettingsRepository
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json

class RealDebridClient(
    private val httpClient: HttpClient,
    private val settingsRepository: DebridSettingsRepository,
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    // Rate limiting — RD allows 30 req/min; simple cooldown per call

    companion object {
        private const val OAUTH_BASE = "https://api.real-debrid.com/oauth/v2"
        private const val API_BASE = "https://api.real-debrid.com/rest/1.0"
        private const val CLIENT_ID = "X0D3T4C5"
        private const val MAX_RETRIES = 2
    }

    // ── Auth ────────────────────────────────────────────────────────

    suspend fun getDeviceCode(): Result<DeviceCodeResponse> = runCatching {
        json.decodeFromString<DeviceCodeResponse>(
            httpClient.post("$OAUTH_BASE/device/code") {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody("client_id=$CLIENT_ID&new_connection=yes")
            }.bodyAsText(),
        )
    }

    suspend fun pollForToken(deviceCode: String): Result<String> = runCatching {
        val body = httpClient.post("$OAUTH_BASE/device/credentials") {
            contentType(ContentType.Application.FormUrlEncoded)
            setBody("client_id=$CLIENT_ID&code=$deviceCode")
        }.bodyAsText()
        val resp = json.decodeFromString<CredentialResponse>(body)
        resp.error?.let { throw Exception("OAuth error: $it") }
        val token = resp.accessToken.also { if (it.isBlank()) throw Exception("Empty access token") }
        token
    }

    // ── Torrent API ──────────────────────────────────────────────────

    suspend fun addTorrent(magnet: String): Result<AddTorrentResponse> = withToken {
        json.decodeFromString<AddTorrentResponse>(
            httpClient.post("$API_BASE/torrents/addTorrent") {
                header("Authorization", "Bearer $it")
                contentType(ContentType.Application.FormUrlEncoded)
                setBody("magnet=$magnet")
            }.bodyAsText(),
        )
    }

    suspend fun getTorrentInfo(torrentId: String): Result<TorrentInfo> = withToken {
        json.decodeFromString<TorrentInfo>(
            httpClient.get("$API_BASE/torrents/info/$torrentId") {
                header("Authorization", "Bearer $it")
            }.bodyAsText(),
        )
    }

    suspend fun selectFiles(torrentId: String, fileIds: String = "all"): Result<Unit> = withToken {
        httpClient.post("$API_BASE/torrents/selectFiles/$torrentId") {
            header("Authorization", "Bearer $it")
            contentType(ContentType.Application.FormUrlEncoded)
            setBody("files=$fileIds")
        }
        Unit
    }

    suspend fun unrestrictLink(link: String): Result<String> = withToken {
        val resp = json.decodeFromString<UnrestrictResponse>(
            httpClient.post("$API_BASE/unrestrict/link") {
                header("Authorization", "Bearer $it")
                contentType(ContentType.Application.FormUrlEncoded)
                setBody("link=$link")
            }.bodyAsText(),
        )
        resp.download
    }

    suspend fun checkCached(hash: String): Result<List<String>> = withToken {
        val body = httpClient.get("$API_BASE/torrents/instantAvailability/$hash") {
            header("Authorization", "Bearer $it")
        }.bodyAsText()

        @Suppress("UNCHECKED_CAST")
        val map = json.decodeFromString<Map<String, List<List<Map<String, String>>>>>(body)
        val variants = map[hash]
        if (variants.isNullOrEmpty()) {
            emptyList()
        } else {
            variants.flatten().mapNotNull { it["filename"] }
        }
    }

    // ── Full resolution pipeline ────────────────────────────────────

    suspend fun resolveToDirectUrl(magnet: String): Result<String> {
        val addResp = addTorrent(magnet).getOrElse { return Result.failure(it) }

        // Select all files to proceed
        if (addResp.id.isNotBlank()) {
            selectFiles(addResp.id)
        }

        // Poll until torrent enters a terminal state
        var torrentInfo: TorrentInfo
        var attempts = 0
        val maxAttempts = 30

        while (attempts < maxAttempts) {
            torrentInfo = getTorrentInfo(addResp.id).getOrElse { return Result.failure(it) }
            attempts++

            when (torrentInfo.status) {
                "downloaded" -> {
                    // Ready — proceed to unrestrict
                    val link = torrentInfo.links.firstOrNull()
                        ?: return Result.failure(Exception("No links in torrent"))
                    return unrestrictLink(link)
                }
                "magnet_conversion" -> {
                    // Still processing magnet → torrent metadata
                    delay(2000)
                }
                "downloading" -> {
                    // Data is being fetched — check every 5s
                    delay(5000)
                }
                "waiting_files_selection" -> {
                    // File selection needed — re-select and continue
                    selectFiles(addResp.id)
                    delay(3000)
                }
                "queued" -> {
                    // RD is overloaded — wait longer
                    delay(5000)
                }
                "error", "virus", "dead" -> {
                    return Result.failure(Exception("Torrent ${torrentInfo.status}: unable to process"))
                }
                "magnet_error" -> {
                    return Result.failure(Exception("Invalid magnet link"))
                }
                else -> {
                    // Unknown status — give it a few attempts then fail
                    if (attempts > 5) {
                        return Result.failure(Exception("Unknown torrent status: ${torrentInfo.status}"))
                    }
                    delay(3000)
                }
            }
        }

        return Result.failure(Exception("Torrent processing timed out after $maxAttempts attempts"))
    }

    // ── Helpers ─────────────────────────────────────────────────────

    private suspend inline fun <T> withToken(crossinline block: suspend (token: String) -> T): Result<T> {
        val token = settingsRepository.getApiKey("realdebrid")
        if (token.isBlank()) return Result.failure(Exception("Real-Debrid not authenticated"))
        return withRetry { block(token) }
    }

    /** Retry on transient failures with exponential backoff. */
    private suspend fun <T> withRetry(block: suspend () -> T): Result<T> {
        var lastError: Throwable? = null
        for (attempt in 0..MAX_RETRIES) {
            rateLimitCheck()
            val result = try {
                Result.success(block())
            } catch (e: Exception) {
                Result.failure<T>(e)
            }
            if (result.isSuccess) return result
            lastError = result.exceptionOrNull()
            if (attempt < MAX_RETRIES) {
                delay((500L * (attempt + 1)).coerceAtMost(3000L))
            }
        }
        return Result.failure(lastError ?: Exception("Retry exhausted"))
    }

    /**
     * Rate limiter — RD allows 30 requests per minute.
     * Enforce minimum 2.5s gap between successive API calls.
     */
    private suspend fun rateLimitCheck() {
        // Simple cooldown — avoids needing platform-specific clock APIs
        delay(2500)
    }
}
