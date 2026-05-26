package com.moviehub.core.network.mapper

import com.moviehub.core.model.*

fun StremioMeta.toDomain(addonId: String? = null, addonUrl: String? = null): MediaItem {
    // Determine the most reliable background/backdrop URL
    val resolvedBackground = background ?: poster
    
    return MediaItem(
        id = id,
        title = name,
        imdbId = imdbId ?: if (id.startsWith("tt")) id else null,
        posterUrl = poster,
        backgroundUrl = resolvedBackground,
        logoUrl = logo,
        description = description,
        type = MediaType.fromString(type),
        rating = imdbRating,
        releaseInfo = releaseInfo,
        runtime = runtime,
        status = status,
        ageRating = ageRating,
        country = country,
        language = language,
        genres = genres,
        cast = cast.map { MediaPerson(it.name, it.role, it.photo) },
        directors = directors,
        writers = writers,
        productionCompanies = productionCompanies.map { MediaCompany(it.name, it.logo) },
        networks = networks.map { MediaCompany(it.name, it.logo) },
        sourceAddonId = addonId,
        sourceAddonUrl = addonUrl,
        videos = videos.map { 
            MediaVideo(
                id = it.id, 
                title = it.title ?: it.name ?: "Unknown Episode", 
                released = it.released, 
                season = it.season, 
                episode = it.episode, 
                thumbnail = it.thumbnail ?: resolvedBackground, // Use series background if episode thumb is missing
                overview = it.overview
            ) 
        },
        trailers = trailers.map { MediaTrailer(id = it.source, url = it.source, type = it.type ?: "Trailer") }
    )
}

fun List<StremioMeta>.toDomain(addonId: String? = null, addonUrl: String? = null): List<MediaItem> =
    map { it.toDomain(addonId, addonUrl) }
