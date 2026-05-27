package com.moviehub.core.network.tmdb

import co.touchlab.kermit.Logger
import com.moviehub.core.model.MediaCompany
import com.moviehub.core.model.MediaItem
import com.moviehub.core.model.MediaPerson
import com.moviehub.core.model.MediaPreview
import com.moviehub.core.model.MediaType
import com.moviehub.core.model.MediaVideo
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * Holds enriched data fetched from TMDB.
 */
data class TmdbEnrichedData(
    val castWithPhotos: List<MediaPerson> = emptyList(),
    val tmdbRating: String? = null,
    val tmdbVoteCount: Int? = null,
    val runtime: String? = null,
    val ageRating: String? = null,
    val awards: String? = null,
    val tagline: String? = null,
)

/**
 * Enriches a MediaItem with data from TMDB (cast photos, ratings, etc.).
 * Returns the original item unchanged if TMDB is not configured or lookup fails.
 */
class TmdbEnrichmentService(
    private val tmdbService: TmdbService,
) {
    private val logger = Logger.withTag("TmdbEnrichment")

    /**
     * Enrich a full MediaItem with TMDB data in a single call.
     * Fetches details, credits, and enriches the item.
     */
    suspend fun enrich(media: MediaItem): MediaItem = coroutineScope {
        if (!tmdbService.hasApiKey()) return@coroutineScope media

        var enriched = media

        val tmdbId = resolveTmdbId(media)
        if (tmdbId != null) {
            enriched = enrichWithTmdbId(enriched, tmdbId)
        } else {
            val imdbId = extractImdbId(media)
            if (imdbId != null) {
                val mediaType = TmdbService.normalizeMediaType(media.type.stremioType)
                val findResult = tmdbService.imdbToTmdb(imdbId, mediaType)
                if (findResult != null) {
                    enriched = enrichWithTmdbId(enriched, findResult.id)
                }
            }
        }

        // Title-based search fallback when no ID could be resolved
        if (enriched == media && media.title.isNotBlank()) {
            val searchResult = searchByTitle(media)
            if (searchResult != null) {
                enriched = enrichWithTmdbId(enriched, searchResult.id)
            }
        }

        enriched
    }

    /**
     * Enrich just the cast with TMDB photos.
     */
    suspend fun enrichCastWithPhotos(media: MediaItem): MediaItem {
        if (!tmdbService.hasApiKey()) return media
        if (media.cast.isEmpty()) return media

        val tmdbId = resolveTmdbId(media) ?: return media
        val mediaType = TmdbService.normalizeMediaType(media.type.stremioType)
        val credits = tmdbService.getCredits(tmdbId, mediaType) ?: return media

        val tmdbCastMap = credits.cast
            .filter { it.profilePath != null }
            .groupBy { it.name.lowercase() }
            .mapValues { (_, members) -> members.first() }

        val enrichedCast = media.cast.map { person ->
            val tmdbPerson = tmdbCastMap[person.name.lowercase()]
            if (tmdbPerson != null) {
                person.copy(photo = TmdbImageUrl.profile(tmdbPerson.profilePath) ?: person.photo)
            } else person
        }

        return media.copy(cast = enrichedCast)
    }

    /**
     * Creates a MediaItem from TMDB when no addon provides metadata.
     * Fetches basic details (title, poster, etc.) then runs full enrichment.
     * Called as a last resort fallback when all addon sources fail.
     */
    suspend fun fetchAsMediaItem(imdbId: String, type: String): MediaItem? {
        if (!tmdbService.hasApiKey()) return null

        val mediaType = TmdbService.normalizeMediaType(type)
        val findResult = tmdbService.imdbToTmdb(imdbId, mediaType) ?: return null
        val tmdbId = findResult.id

        val basic: MediaItem = when (mediaType) {
            "movie" -> {
                val details = tmdbService.getMovieDetails(tmdbId) ?: return null
                MediaItem(
                    id = imdbId,
                    title = details.title ?: return null,
                    imdbId = details.imdbId ?: imdbId,
                    posterUrl = TmdbImageUrl.poster(details.posterPath),
                    backgroundUrl = TmdbImageUrl.backdrop(details.backdropPath),
                    description = details.overview,
                    type = MediaType.MOVIE,
                    releaseInfo = details.releaseDate?.take(4),
                    genres = details.genres?.map { it.name }.orEmpty(),
                )
            }
            "tv" -> {
                val details = tmdbService.getTvDetails(tmdbId) ?: return null
                MediaItem(
                    id = imdbId,
                    title = details.name ?: return null,
                    imdbId = imdbId,
                    posterUrl = TmdbImageUrl.poster(details.posterPath),
                    backgroundUrl = TmdbImageUrl.backdrop(details.backdropPath),
                    description = details.overview,
                    type = MediaType.SHOW,
                    releaseInfo = details.firstAirDate?.take(4),
                    genres = details.genres?.map { it.name }.orEmpty(),
                )
            }
            else -> return null
        }

        return try {
            enrich(basic)
        } catch (e: Exception) {
            logger.w { "TMDB fallback enrichment failed: ${e.message}" }
            basic
        }
    }

    /**
     * Creates a MediaItem from a TMDB numeric ID (from recommendations, etc.)
     * Skips the IMDb-to-TMDB lookup and fetches details directly.
     */
    suspend fun fetchAsMediaItemFromTmdbId(tmdbId: Int, type: String): MediaItem? {
        if (!tmdbService.hasApiKey()) return null

        val mediaType = TmdbService.normalizeMediaType(type)
        val basic: MediaItem = when (mediaType) {
            "movie" -> {
                val details = tmdbService.getMovieDetails(tmdbId) ?: return null
                MediaItem(
                    id = tmdbId.toString(),
                    title = details.title ?: return null,
                    imdbId = details.imdbId,
                    posterUrl = TmdbImageUrl.poster(details.posterPath),
                    backgroundUrl = TmdbImageUrl.backdrop(details.backdropPath),
                    description = details.overview,
                    type = MediaType.MOVIE,
                    releaseInfo = details.releaseDate?.take(4),
                    genres = details.genres?.map { it.name }.orEmpty(),
                )
            }
            "tv" -> {
                val details = tmdbService.getTvDetails(tmdbId) ?: return null
                MediaItem(
                    id = tmdbId.toString(),
                    title = details.name ?: return null,
                    imdbId = null,
                    posterUrl = TmdbImageUrl.poster(details.posterPath),
                    backgroundUrl = TmdbImageUrl.backdrop(details.backdropPath),
                    description = details.overview,
                    type = MediaType.SHOW,
                    releaseInfo = details.firstAirDate?.take(4),
                    genres = details.genres?.map { it.name }.orEmpty(),
                )
            }
            else -> return null
        }

        return try {
            enrich(basic)
        } catch (e: Exception) {
            logger.w { "TMDB fallback enrichment failed for tmdbId=$tmdbId: ${e.message}" }
            basic
        }
    }

    suspend fun fetchPersonDetail(personId: Int): TmdbPersonDetail? {
        if (!tmdbService.hasApiKey()) return null
        return tmdbService.getPersonDetail(personId)
    }

    suspend fun fetchPersonCredits(personId: Int): List<MediaPreview> {
        if (!tmdbService.hasApiKey()) return emptyList()
        val credits = tmdbService.getPersonCombinedCredits(personId) ?: return emptyList()

        val seen = mutableSetOf<Int>()
        return credits.cast
            .sortedByDescending { it.voteAverage ?: 0.0 }
            .mapNotNull { credit ->
                val id = credit.id
                if (id in seen) return@mapNotNull null
                seen.add(id)

                val title = credit.title ?: credit.name ?: return@mapNotNull null
                val type = when (credit.mediaType) {
                    "movie" -> "movie"
                    "tv" -> "series"
                    else -> "other"
                }
                MediaPreview(
                    id = id.toString(),
                    title = title,
                    posterUrl = TmdbImageUrl.poster(credit.posterPath, "w342"),
                    type = type,
                )
            }
            .take(20)
    }

    private suspend fun enrichWithTmdbId(media: MediaItem, tmdbId: Int): MediaItem = coroutineScope {
        val mediaType = TmdbService.normalizeMediaType(media.type.stremioType)

        val detailsDef = async {
            when (mediaType) {
                "movie" -> tmdbService.getMovieDetails(tmdbId)
                "tv" -> tmdbService.getTvDetails(tmdbId)
                else -> null
            }
        }
        val creditsDef = async {
            tmdbService.getCredits(tmdbId, mediaType)
        }
        val ageRatingDef = async {
            when (mediaType) {
                "movie" -> tmdbService.getMovieReleaseDates(tmdbId)
                "tv" -> tmdbService.getTvContentRatings(tmdbId)
                else -> null
            }
        }

        val details = detailsDef.await()
        val credits = creditsDef.await()
        val ageRatingResponse = ageRatingDef.await()

        // Age rating
        val ageRating = when {
            media.ageRating != null -> media.ageRating
            ageRatingResponse is TmdbMovieReleaseDatesResponse -> {
                ageRatingResponse.results
                    .firstOrNull { it.iso3166_1 == "US" }
                    ?.releaseDates
                    ?.firstOrNull { !it.certification.isNullOrBlank() }
                    ?.certification
            }
            ageRatingResponse is TmdbTvContentRatingsResponse -> {
                ageRatingResponse.results
                    .firstOrNull { it.iso3166_1 == "US" }
                    ?.rating
            }
            else -> null
        }

        // Runtime
        val runtime = when {
            media.runtime != null -> media.runtime
            details is TmdbMovieDetails && details.runtime != null -> "${details.runtime} min"
            details is TmdbTvDetails && details.episodeRunTime?.isNotEmpty() == true -> {
                "${details.episodeRunTime.average().toInt()}m avg"
            }
            else -> null
        }

        // Rating
        val rating = when {
            media.rating != null -> media.rating
            details != null -> {
                val avg: Double? = when (details) {
                    is TmdbMovieDetails -> details.voteAverage
                    is TmdbTvDetails -> details.voteAverage
                    else -> null
                }
                if (avg != null && avg > 0) formatOneDecimal(avg) else null
            }
            else -> null
        }

        // Season/episode data for TV shows from TMDB (parallel fetch)
        val enrichedVideos = if (media.videos.isEmpty() && details is TmdbTvDetails) {
            val numSeasons = details.numberOfSeasons ?: 0
            if (numSeasons > 0) {
                val seasonDeferred = (1..numSeasons).map { seasonNum ->
                    async { tmdbService.getSeasonDetails(tmdbId, seasonNum) }
                }
                seasonDeferred.awaitAll().flatMap { seasonDetail: TmdbSeasonDetails? ->
                    seasonDetail?.episodes?.map { episode: TmdbEpisode ->
                        MediaVideo(
                            id = "tmdb:$tmdbId:${episode.seasonNumber}:${episode.episodeNumber}",
                            title = episode.name ?: "Episode ${episode.episodeNumber}",
                            released = episode.airDate,
                            season = episode.seasonNumber,
                            episode = episode.episodeNumber,
                            thumbnail = TmdbImageUrl.still(episode.stillPath),
                            overview = episode.overview,
                        )
                    } ?: emptyList<MediaVideo>()
                }
            } else emptyList()
        } else media.videos

        // Cast photos + TMDB ID enrichment
        val enrichedCast = if (media.cast.isNotEmpty() && credits != null) {
            val tmdbCastMap = credits.cast
                .filter { it.profilePath != null || it.id > 0 }
                .groupBy { it.name.lowercase() }
                .mapValues { (_, members) -> members.first() }

            media.cast.map { person ->
                tmdbCastMap[person.name.lowercase()]?.let { tmdbPerson ->
                    person.copy(
                        photo = TmdbImageUrl.profile(tmdbPerson.profilePath) ?: person.photo,
                        tmdbId = if (tmdbPerson.id > 0) tmdbPerson.id else person.tmdbId,
                    )
                } ?: person
            }
        } else media.cast

        // Directors & Writers from TMDB crew (only if not already present)
        val enrichedDirectors = if (media.directors.isEmpty() && credits != null) {
            credits.crew
                .filter { it.job?.lowercase() == "director" }
                .map { it.name }
        } else media.directors

        val enrichedWriters = if (media.writers.isEmpty() && credits != null) {
            credits.crew
                .filter { it.job?.lowercase() in listOf("writer", "screenplay", "story", "teleplay") }
                .map { it.name }
                .distinct()
        } else media.writers

        // Production companies & networks from TMDB details
        val enrichedProductionCompanies = if (media.productionCompanies.isEmpty()) {
            when (details) {
                is TmdbMovieDetails -> details.productionCompanies?.map { tmdb ->
                    MediaCompany(
                        name = tmdb.name,
                        logo = TmdbImageUrl.logo(tmdb.logoPath, "w154"),
                    )
                }?.filter { it.name.isNotBlank() }.orEmpty()
                is TmdbTvDetails -> details.productionCompanies?.map { tmdb ->
                    MediaCompany(
                        name = tmdb.name,
                        logo = TmdbImageUrl.logo(tmdb.logoPath, "w154"),
                    )
                }?.filter { it.name.isNotBlank() }.orEmpty()
                else -> media.productionCompanies
            }
        } else media.productionCompanies

        val enrichedNetworks = if (media.networks.isEmpty()) {
            when (details) {
                is TmdbTvDetails -> details.networks?.map { tmdb ->
                    MediaCompany(
                        name = tmdb.name,
                        logo = TmdbImageUrl.logo(tmdb.logoPath, "w154"),
                    )
                }?.filter { it.name.isNotBlank() }.orEmpty()
                else -> media.networks
            }
        } else media.networks

        // Country & language fallback from TMDB details
        val enrichedCountry = if (media.country.isNullOrBlank()) {
            when (details) {
                is TmdbMovieDetails -> details.productionCountries?.firstOrNull()?.name
                is TmdbTvDetails -> details.originCountry?.firstOrNull()
                else -> null
            }
        } else media.country

        val enrichedLanguage = if (media.language.isNullOrBlank()) {
            (details as? TmdbMovieDetails)?.originalLanguage?.uppercase()
                ?: (details as? TmdbTvDetails)?.originalLanguage?.uppercase()
        } else media.language

        // Status & tagline from TMDB
        val enrichedStatus = media.status ?: when (details) {
            is TmdbMovieDetails -> details.status
            is TmdbTvDetails -> details.status
            else -> null
        }

        val tagline = when (details) {
            is TmdbMovieDetails -> details.tagline
            is TmdbTvDetails -> null // TV details don't have tagline in TMDB
            else -> null
        }

        // Description fallback — use TMDB overview if Stremio description is empty
        val enrichedDescription = if (media.description.isNullOrBlank() && details != null) {
            when (details) {
                is TmdbMovieDetails -> details.overview
                is TmdbTvDetails -> details.overview
                else -> null
            }
        } else media.description

        // More like this (only if empty)
        val moreLikeThis = if (media.moreLikeThis.isEmpty()) {
            val recommendations = tmdbService.getRecommendations(tmdbId, mediaType)
            recommendations?.results
                ?.sortedByDescending { it.voteAverage ?: 0.0 }
                ?.take(12)
                ?.map { rec ->
                    MediaPreview(
                        id = rec.id.toString(),
                        title = rec.title ?: rec.name ?: "Unknown",
                        posterUrl = TmdbImageUrl.poster(rec.posterPath, "w342"),
                        type = if (rec.mediaType == "tv") "series" else "movie",
                    )
                } ?: media.moreLikeThis
        } else media.moreLikeThis

        // Backfill poster/background from TMDB when addon metadata lacks them
        val enrichedPosterUrl = media.posterUrl ?: TmdbImageUrl.poster(
            when (details) {
                is TmdbMovieDetails -> details.posterPath
                is TmdbTvDetails -> details.posterPath
                else -> null
            }
        )
        val enrichedBackgroundUrl = media.backgroundUrl ?: TmdbImageUrl.backdrop(
            when (details) {
                is TmdbMovieDetails -> details.backdropPath
                is TmdbTvDetails -> details.backdropPath
                else -> null
            }
        )

        media.copy(
            rating = rating,
            runtime = runtime,
            ageRating = ageRating,
            cast = enrichedCast,
            directors = enrichedDirectors,
            writers = enrichedWriters,
            productionCompanies = enrichedProductionCompanies,
            networks = enrichedNetworks,
            country = enrichedCountry,
            language = enrichedLanguage,
            status = enrichedStatus,
            tagline = tagline,
            description = enrichedDescription,
            videos = enrichedVideos,
            moreLikeThis = moreLikeThis,
            posterUrl = enrichedPosterUrl,
            backgroundUrl = enrichedBackgroundUrl,
        )
    }

    private suspend fun searchByTitle(media: MediaItem): TmdbFindResult? {
        val title = media.title.trim()
        val year = media.releaseInfo?.take(4)
        val mediaType = TmdbService.normalizeMediaType(media.type.stremioType)

        // Build multiple search queries in priority order
        val queries = buildList {
            // 1. Title + year (most specific)
            if (year != null) add("$title $year")
            // 2. Title only
            add(title)
            // 3. Remove parenthetical content — "(English dub)", "(2005)", etc.
            val cleaned = title.replace(Regex("\\s*\\([^)]*\\)\\s*"), "").trim()
            if (cleaned != title && cleaned.isNotBlank()) {
                if (year != null) add("$cleaned $year")
                add(cleaned)
            }
            // 4. First part before any separator — "Movie: Subtitle" → "Movie"
            val firstPart = title.split(Regex("[:\\-–—|]")).firstOrNull()?.trim()
            if (firstPart != null && firstPart != title && firstPart.isNotBlank()) {
                if (year != null) add("$firstPart $year")
                add(firstPart)
            }
        }.distinct()

        for (query in queries) {
            val response = tmdbService.searchMulti(query) ?: continue
            val result = response.results.firstOrNull { result ->
                val matchesType = when (mediaType) {
                    "movie" -> result.mediaType == "movie"
                    "tv" -> result.mediaType == "tv"
                    else -> true
                }
                if (!matchesType) return@firstOrNull false
                // Year check: prefer exact year match when available
                if (year != null) {
                    val resultYear = if (mediaType == "movie") result.releaseDate?.take(4) else result.firstAirDate?.take(4)
                    resultYear == null || resultYear == year
                } else true
            }
            if (result != null) {
                return TmdbFindResult(
                    id = result.id,
                    title = result.title ?: result.name,
                    name = result.name ?: result.title,
                    mediaType = mediaType,
                )
            }
        }
        return null
    }

    private fun resolveTmdbId(media: MediaItem): Int? {
        TmdbService.extractTmdbId(media.id)?.let { return it }
        media.imdbId?.let { imdb ->
            if (imdb.startsWith("tt")) return null
            imdb.toIntOrNull()?.let { return it }
        }
        return null
    }

    private fun extractImdbId(media: MediaItem): String? {
        media.imdbId?.let {
            if (it.startsWith("tt") && it.length > 2) return it
        }
        val parts = media.id.split(":", "/", "?", "&")
        return parts.firstOrNull { it.startsWith("tt") && it.length > 2 }
    }
}

private fun formatOneDecimal(value: Double): String {
    val truncated = (value * 10).toInt() / 10.0
    val whole = truncated.toInt()
    val tenth = ((truncated - whole) * 10).toInt()
    return "$whole.$tenth"
}
