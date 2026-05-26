package com.moviehub.core.model

import androidx.compose.runtime.Immutable

@Immutable
data class MediaItem(
    val id: String,
    val title: String,
    val imdbId: String? = null,
    val posterUrl: String?,
    val backgroundUrl: String? = null,
    val logoUrl: String? = null,
    val tagline: String? = null,
    val description: String? = null,
    val type: MediaType,
    val rating: String? = null,
    val releaseInfo: String? = null,
    val quality: String? = null,
    val runtime: String? = null,
    val status: String? = null,
    val ageRating: String? = null,
    val country: String? = null,
    val language: String? = null,
    val genres: List<String> = emptyList(),
    val cast: List<MediaPerson> = emptyList(),
    val directors: List<String> = emptyList(),
    val writers: List<String> = emptyList(),
    val productionCompanies: List<MediaCompany> = emptyList(),
    val networks: List<MediaCompany> = emptyList(),
    val sourceAddonId: String? = null,
    val sourceAddonUrl: String? = null,
    val videos: List<MediaVideo> = emptyList(),
    val trailers: List<MediaTrailer> = emptyList(),
    val moreLikeThis: List<MediaPreview> = emptyList(),
    val collectionName: String? = null,
    val collectionItems: List<MediaPreview> = emptyList()
)

@Immutable
data class MediaPerson(
    val name: String,
    val role: String? = null,
    val photo: String? = null,
    val tmdbId: Int? = null,
)

@Immutable
data class MediaCompany(
    val name: String,
    val logo: String? = null
)

@Immutable
data class MediaVideo(
    val id: String,
    val title: String,
    val released: String? = null,
    val season: Int? = null,
    val episode: Int? = null,
    val thumbnail: String? = null,
    val overview: String? = null
)

@Immutable
data class MediaTrailer(
    val id: String,
    val url: String,
    val name: String? = null,
    val type: String? = null
)

@Immutable
data class ContinueWatchingItem(
    val mediaId: String,
    val title: String,
    val type: String,
    val posterUrl: String?,
    val progressMs: Long,
    val durationMs: Long,
    val lastWatchedAt: Long
)

@Immutable
data class MediaPreview(
    val id: String,
    val title: String,
    val posterUrl: String?,
    val type: String
)

enum class MediaType {
    MOVIE, SHOW, OTHER;

    val stremioType: String
        get() = when (this) {
            MOVIE -> "movie"
            SHOW -> "series"
            OTHER -> "other"
        }

    companion object {
        fun fromString(type: String): MediaType = when (type.lowercase()) {
            "movie" -> MOVIE
            "series", "show", "tv" -> SHOW
            else -> OTHER
        }
    }
}
