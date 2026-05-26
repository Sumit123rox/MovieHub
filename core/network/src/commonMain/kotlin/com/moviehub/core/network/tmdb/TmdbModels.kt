package com.moviehub.core.network.tmdb

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TmdbMovieDetails(
    val id: Int,
    val title: String? = null,
    val overview: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("vote_average") val voteAverage: Double? = null,
    @SerialName("vote_count") val voteCount: Int? = null,
    @SerialName("runtime") val runtime: Int? = null,
    val status: String? = null,
    val tagline: String? = null,
    val genres: List<TmdbGenre>? = null,
    @SerialName("production_companies") val productionCompanies: List<TmdbCompany>? = null,
    @SerialName("production_countries") val productionCountries: List<TmdbCountry>? = null,
    @SerialName("original_language") val originalLanguage: String? = null,
    @SerialName("belongs_to_collection") val belongsToCollection: TmdbCollectionRef? = null,
    @SerialName("imdb_id") val imdbId: String? = null,
    @SerialName("vote_average_count") val voteAverageCount: Int? = null,
)

@Serializable
data class TmdbTvDetails(
    val id: Int,
    val name: String? = null,
    val overview: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("first_air_date") val firstAirDate: String? = null,
    @SerialName("last_air_date") val lastAirDate: String? = null,
    @SerialName("vote_average") val voteAverage: Double? = null,
    @SerialName("vote_count") val voteCount: Int? = null,
    @SerialName("episode_run_time") val episodeRunTime: List<Int>? = null,
    val status: String? = null,
    val genres: List<TmdbGenre>? = null,
    @SerialName("production_companies") val productionCompanies: List<TmdbCompany>? = null,
    @SerialName("networks") val networks: List<TmdbCompany>? = null,
    @SerialName("origin_country") val originCountry: List<String>? = null,
    @SerialName("original_language") val originalLanguage: String? = null,
    @SerialName("number_of_seasons") val numberOfSeasons: Int? = null,
    @SerialName("number_of_episodes") val numberOfEpisodes: Int? = null,
    @SerialName("created_by") val createdBy: List<TmdbCreator>? = null,
    @SerialName("vote_average_count") val voteAverageCount: Int? = null,
    @SerialName("last_episode_to_air") val lastEpisodeToAir: TmdbEpisodeRef? = null,
    @SerialName("next_episode_to_air") val nextEpisodeToAir: TmdbEpisodeRef? = null,
    @SerialName("in_production") val inProduction: Boolean? = null,
)

@Serializable
data class TmdbEpisodeRef(
    val id: Int? = null,
    val name: String? = null,
    val overview: String? = null,
    @SerialName("air_date") val airDate: String? = null,
    @SerialName("episode_number") val episodeNumber: Int? = null,
    @SerialName("season_number") val seasonNumber: Int? = null,
    @SerialName("still_path") val stillPath: String? = null,
    @SerialName("vote_average") val voteAverage: Double? = null,
)

@Serializable
data class TmdbGenre(val id: Int, val name: String)

@Serializable
data class TmdbCompany(
    val id: Int,
    val name: String,
    @SerialName("logo_path") val logoPath: String? = null,
    @SerialName("origin_country") val originCountry: String? = null,
)

@Serializable
data class TmdbCountry(
    @SerialName("iso_3166_1") val iso3166_1: String,
    val name: String,
)

@Serializable
data class TmdbCreator(
    val id: Int,
    @SerialName("credit_id") val creditId: String? = null,
    val name: String? = null,
    val gender: Int? = null,
    @SerialName("profile_path") val profilePath: String? = null,
)

@Serializable
data class TmdbCollectionRef(
    val id: Int,
    val name: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
)

@Serializable
data class TmdbCreditsResponse(
    val cast: List<TmdbCastMember> = emptyList(),
    val crew: List<TmdbCrewMember> = emptyList(),
)

@Serializable
data class TmdbCastMember(
    val id: Int,
    val name: String,
    val character: String? = null,
    @SerialName("profile_path") val profilePath: String? = null,
    val order: Int = 0,
    @SerialName("known_for_department") val knownForDepartment: String? = null,
    @SerialName("popularity") val popularity: Double? = null,
)

@Serializable
data class TmdbCrewMember(
    val id: Int,
    val name: String,
    val job: String? = null,
    val department: String? = null,
    @SerialName("profile_path") val profilePath: String? = null,
    @SerialName("known_for_department") val knownForDepartment: String? = null,
)

@Serializable
data class TmdbPersonDetail(
    val id: Int,
    val name: String,
    val biography: String? = null,
    @SerialName("birthday") val birthday: String? = null,
    @SerialName("deathday") val deathday: String? = null,
    @SerialName("place_of_birth") val placeOfBirth: String? = null,
    @SerialName("profile_path") val profilePath: String? = null,
    @SerialName("known_for_department") val knownForDepartment: String? = null,
    @SerialName("also_known_as") val alsoKnownAs: List<String>? = null,
    val gender: Int? = null,
    val popularity: Double? = null,
    @SerialName("adult") val adult: Boolean? = null,
    @SerialName("homepage") val homepage: String? = null,
)

@Serializable
data class TmdbPersonCombinedCredits(
    val cast: List<TmdbPersonCredit> = emptyList(),
    val crew: List<TmdbPersonCredit> = emptyList(),
)

@Serializable
data class TmdbPersonCredit(
    val id: Int,
    val title: String? = null,
    val name: String? = null,
    @SerialName("media_type") val mediaType: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    val character: String? = null,
    val job: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("first_air_date") val firstAirDate: String? = null,
    @SerialName("vote_average") val voteAverage: Double? = null,
    @SerialName("vote_count") val voteCount: Int? = null,
    val overview: String? = null,
    @SerialName("episode_count") val episodeCount: Int? = null,
    @SerialName("popularity") val popularity: Double? = null,
)

@Serializable
data class TmdbSeasonDetails(
    val id: Int,
    @SerialName("air_date") val airDate: String? = null,
    val name: String? = null,
    val overview: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("season_number") val seasonNumber: Int? = null,
    @SerialName("vote_average") val voteAverage: Double? = null,
    val episodes: List<TmdbEpisode>? = null,
)

@Serializable
data class TmdbEpisode(
    val id: Int,
    val name: String? = null,
    val overview: String? = null,
    @SerialName("air_date") val airDate: String? = null,
    @SerialName("episode_number") val episodeNumber: Int? = null,
    @SerialName("season_number") val seasonNumber: Int? = null,
    @SerialName("still_path") val stillPath: String? = null,
    @SerialName("vote_average") val voteAverage: Double? = null,
    @SerialName("vote_count") val voteCount: Int? = null,
    @SerialName("runtime") val runtime: Int? = null,
)

@Serializable
data class TmdbMovieReleaseDatesResponse(
    val results: List<TmdbReleaseDateResult> = emptyList(),
)

@Serializable
data class TmdbReleaseDateResult(
    @SerialName("iso_3166_1") val iso3166_1: String,
    @SerialName("release_dates") val releaseDates: List<TmdbReleaseDate> = emptyList(),
)

@Serializable
data class TmdbReleaseDate(
    val certification: String? = null,
    val type: Int? = null,
    val note: String? = null,
)

@Serializable
data class TmdbTvContentRatingsResponse(
    val results: List<TmdbTvRating> = emptyList(),
)

@Serializable
data class TmdbTvRating(
    @SerialName("iso_3166_1") val iso3166_1: String,
    val rating: String? = null,
)

@Serializable
data class TmdbImagesResponse(
    val backdrops: List<TmdbImage> = emptyList(),
    val posters: List<TmdbImage> = emptyList(),
    val logos: List<TmdbImage> = emptyList(),
)

@Serializable
data class TmdbImage(
    @SerialName("file_path") val filePath: String,
    @SerialName("aspect_ratio") val aspectRatio: Double? = null,
    @SerialName("vote_average") val voteAverage: Double? = null,
    @SerialName("vote_count") val voteCount: Int? = null,
    val width: Int? = null,
    val height: Int? = null,
    @SerialName("iso_639_1") val iso639_1: String? = null,
)

@Serializable
data class TmdbVideosResponse(
    val results: List<TmdbVideo> = emptyList(),
)

@Serializable
data class TmdbVideo(
    val id: String,
    val key: String,
    val name: String? = null,
    val site: String? = null,
    val type: String? = null,
    val size: Int? = null,
    val official: Boolean? = null,
    @SerialName("published_at") val publishedAt: String? = null,
)

@Serializable
data class TmdbRecommendationsResponse(
    val results: List<TmdbRecommendationItem> = emptyList(),
)

@Serializable
data class TmdbRecommendationItem(
    val id: Int,
    val title: String? = null,
    val name: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("vote_average") val voteAverage: Double? = null,
    @SerialName("media_type") val mediaType: String? = null,
)

@Serializable
data class TmdbCollectionResponse(
    val id: Int,
    val name: String? = null,
    val overview: String? = null,
    val parts: List<TmdbCollectionPart> = emptyList(),
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
)

@Serializable
data class TmdbCollectionPart(
    val id: Int,
    val title: String? = null,
    val name: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("vote_average") val voteAverage: Double? = null,
    @SerialName("media_type") val mediaType: String? = null,
)

@Serializable
data class TmdbFindResponse(
    @SerialName("movie_results") val movieResults: List<TmdbFindResult> = emptyList(),
    @SerialName("tv_results") val tvResults: List<TmdbFindResult> = emptyList(),
    @SerialName("person_results") val personResults: List<TmdbFindResult> = emptyList(),
)

@Serializable
data class TmdbFindResult(
    val id: Int,
    val title: String? = null,
    val name: String? = null,
    @SerialName("media_type") val mediaType: String? = null,
)

@Serializable
data class TmdbExternalIdsResponse(
    @SerialName("imdb_id") val imdbId: String? = null,
    val id: Int? = null,
)

object TmdbImageUrl {
    private const val BASE = "https://image.tmdb.org/t/p"

    fun poster(path: String?, size: String = "w500"): String? =
        path?.let { "$BASE/$size$it" }

    fun backdrop(path: String?, size: String = "w1280"): String? =
        path?.let { "$BASE/$size$it" }

    fun profile(path: String?, size: String = "w185"): String? =
        path?.let { "$BASE/$size$it" }

    fun logo(path: String?, size: String = "w300"): String? =
        path?.let { "$BASE/$size$it" }

    fun still(path: String?, size: String = "w300"): String? =
        path?.let { "$BASE/$size$it" }
}
