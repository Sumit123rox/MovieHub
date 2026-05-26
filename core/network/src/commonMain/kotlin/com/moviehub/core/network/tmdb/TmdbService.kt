package com.moviehub.core.network.tmdb

import co.touchlab.kermit.Logger
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json

class TmdbService(
    private val httpClient: HttpClient,
) {
    private val logger = Logger.withTag("TmdbService")
    private val json = Json { ignoreUnknownKeys = true }

    private val apiKeyMutex = Mutex()
    private var apiKey: String = ""

    fun setApiKey(key: String) {
        apiKey = key.trim()
    }

    fun hasApiKey(): Boolean = apiKey.isNotBlank()

    private fun requireKey() {
        require(hasApiKey()) { "TMDB API key not set" }
    }

    // ============ MOVIE ============

    suspend fun getMovieDetails(tmdbId: Int, language: String = "en"): TmdbMovieDetails? {
        requireKey()
        return fetch("movie/$tmdbId", language)
    }

    // ============ TV ============

    suspend fun getTvDetails(tmdbId: Int, language: String = "en"): TmdbTvDetails? {
        requireKey()
        return fetch("tv/$tmdbId", language)
    }

    // ============ CREDITS ============

    suspend fun getCredits(tmdbId: Int, mediaType: String, language: String = "en"): TmdbCreditsResponse? {
        requireKey()
        return fetch("$mediaType/$tmdbId/credits", language)
    }

    // ============ PERSON ============

    suspend fun getPersonDetail(personId: Int, language: String = "en"): TmdbPersonDetail? {
        requireKey()
        return fetch("person/$personId", language)
    }

    suspend fun getPersonCombinedCredits(personId: Int, language: String = "en"): TmdbPersonCombinedCredits? {
        requireKey()
        return fetch("person/$personId/combined_credits", language)
    }

    // ============ IMAGE / AGE RATING / VIDEOS ============

    suspend fun getImages(tmdbId: Int, mediaType: String): TmdbImagesResponse? {
        requireKey()
        return fetch("$mediaType/$tmdbId/images") {
            parameter("include_image_language", "en,null")
        }
    }

    suspend fun getMovieReleaseDates(tmdbId: Int): TmdbMovieReleaseDatesResponse? {
        requireKey()
        return fetch("movie/$tmdbId/release_dates")
    }

    suspend fun getTvContentRatings(tmdbId: Int): TmdbTvContentRatingsResponse? {
        requireKey()
        return fetch("tv/$tmdbId/content_ratings")
    }

    suspend fun getVideos(tmdbId: Int, mediaType: String, language: String = "en"): TmdbVideosResponse? {
        requireKey()
        return fetch("$mediaType/$tmdbId/videos", language)
    }

    suspend fun getSeasonDetails(tmdbId: Int, seasonNumber: Int, language: String = "en"): TmdbSeasonDetails? {
        requireKey()
        return fetch("tv/$tmdbId/season/$seasonNumber", language)
    }

    // ============ RECOMMENDATIONS / COLLECTIONS ============

    suspend fun getRecommendations(tmdbId: Int, mediaType: String, page: Int = 1): TmdbRecommendationsResponse? {
        requireKey()
        return fetch("$mediaType/$tmdbId/recommendations") {
            parameter("page", page)
        }
    }

    suspend fun getCollection(collectionId: Int, language: String = "en"): TmdbCollectionResponse? {
        requireKey()
        return fetch("collection/$collectionId", language)
    }

    // ============ DISCOVER ============

    suspend fun discover(
        mediaType: String,
        withCompanies: String? = null,
        withNetworks: String? = null,
        sortBy: String = "popularity.desc",
        page: Int = 1,
        voteCountGte: Int? = null,
    ): TmdbDiscoverResponse? {
        requireKey()
        return fetch("discover/$mediaType") {
            parameter("sort_by", sortBy)
            parameter("page", page)
            if (withCompanies != null) parameter("with_companies", withCompanies)
            if (withNetworks != null) parameter("with_networks", withNetworks)
            if (voteCountGte != null) parameter("vote_count.gte", voteCountGte)
        }
    }

    // ============ ID LOOKUP ============

    suspend fun imdbToTmdb(imdbId: String, mediaType: String): TmdbFindResult? {
        requireKey()
        val response = fetch<TmdbFindResponse>("find/$imdbId") {
            parameter("external_source", "imdb_id")
        } ?: return null

        val type = normalizeMediaType(mediaType)
        return when (type) {
            "movie" -> response.movieResults.firstOrNull()
            "tv" -> response.tvResults.firstOrNull()
            else -> response.movieResults.firstOrNull() ?: response.tvResults.firstOrNull()
        }
    }

    suspend fun getExternalIds(tmdbId: Int, mediaType: String): TmdbExternalIdsResponse? {
        requireKey()
        return fetch("$mediaType/$tmdbId/external_ids")
    }

    // ============ PRIVATE HELPERS ============

    private suspend inline fun <reified T> fetch(
        endpoint: String,
        noinline block: suspend io.ktor.client.request.HttpRequestBuilder.() -> Unit = {},
    ): T? {
        val key = apiKeyMutex.withLock { apiKey }
        val url = "https://api.themoviedb.org/3/$endpoint"
        return try {
            httpClient.get(url) {
                parameter("api_key", key)
                block()
            }.let { response ->
                json.decodeFromString<T>(response.bodyAsText())
            }
        } catch (e: Exception) {
            logger.w { "TMDB API error for $endpoint: ${e.message}" }
            null
        }
    }

    private suspend inline fun <reified T> fetch(endpoint: String, language: String): T? {
        return fetch(endpoint) {
            parameter("language", language)
        }
    }

    companion object {
        fun normalizeMediaType(type: String): String = when (type.lowercase()) {
            "film", "movie" -> "movie"
            "tv", "show", "series", "tvshow", "tv_show" -> "tv"
            else -> type.lowercase()
        }

        fun extractTmdbId(id: String): Int? {
            val cleaned = id.removePrefix("tmdb:")
                .removePrefix("movie:")
                .removePrefix("series:")
                .removePrefix("tv:")
                .trim()
            return cleaned.toIntOrNull()
        }
    }
}

@Serializable
data class TmdbDiscoverResponse(
    val results: List<TmdbDiscoverResult> = emptyList(),
    val page: Int = 1,
    @SerialName("total_pages") val totalPages: Int = 1,
    @SerialName("total_results") val totalResults: Int = 0,
)

@Serializable
data class TmdbDiscoverResult(
    val id: Int,
    val title: String? = null,
    val name: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("vote_average") val voteAverage: Double? = null,
    @SerialName("vote_count") val voteCount: Int? = null,
    @SerialName("media_type") val mediaType: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("first_air_date") val firstAirDate: String? = null,
)
