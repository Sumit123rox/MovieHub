package com.moviehub.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StremioManifest(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("version") val version: String,
    @SerialName("description") val description: String? = null,
    @SerialName("logo") val logo: String? = null,
    @SerialName("background") val background: String? = null,
    @SerialName("resources") val resources: List<StremioResource>? = emptyList(),
    @SerialName("types") val types: List<String>? = emptyList(),
    @SerialName("catalogs") val catalogs: List<StremioCatalog>? = emptyList(),
    @SerialName("idPrefixes") val idPrefixes: List<String>? = emptyList(),
    @SerialName("contactEmail") val contactEmail: String? = null,
)

@Serializable
data class StremioResource(
    @SerialName("name") val name: String,
    @SerialName("types") val types: List<String>? = emptyList(),
    @SerialName("idPrefixes") val idPrefixes: List<String>? = emptyList(),
)

@Serializable
data class StremioCatalog(
    @SerialName("type") val type: String,
    @SerialName("id") val id: String,
    @SerialName("name") val name: String? = null,
    @SerialName("extra") val extra: List<StremioExtra>? = emptyList(),
)

@Serializable
data class StremioExtra(
    @SerialName("name") val name: String,
    @SerialName("isRequired") val isRequired: Boolean? = false,
    @SerialName("options") val options: List<String>? = emptyList(),
)

@Serializable
data class StremioCatalogResponse(
    @SerialName("metas") val metas: List<StremioMeta> = emptyList(),
)

@Serializable
data class StremioMeta(
    @SerialName("id") val id: String,
    @SerialName("type") val type: String = "",
    @SerialName("name") val name: String,
    @SerialName("imdb_id") val imdbId: String? = null,
    @SerialName("poster") val poster: String? = null,
    @SerialName("background") val background: String? = null,
    @SerialName("logo") val logo: String? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("releaseInfo") val releaseInfo: String? = null,
    @SerialName("runtime") val runtime: String? = null,
    @SerialName("genres") val genres: List<String>? = emptyList(),
    @SerialName("imdbRating") val imdbRating: String? = null,
    @SerialName("director") val director: List<String>? = emptyList(),
    @SerialName("cast") val cast: List<String>? = emptyList(),
    @SerialName("videos") val videos: List<StremioVideo>? = null,
)

@Serializable
data class StremioVideo(
    @SerialName("id") val id: String,
    @SerialName("title") val title: String,
    @SerialName("released") val released: String? = null,
    @SerialName("season") val season: Int? = null,
    @SerialName("episode") val episode: Int? = null,
    @SerialName("thumbnail") val thumbnail: String? = null,
    @SerialName("overview") val overview: String? = null,
)

@Serializable
data class StremioStreamResponse(
    @SerialName("streams") val streams: List<StremioStream> = emptyList(),
)

@Serializable
data class StremioStream(
    @SerialName("name") val name: String? = null,
    @SerialName("title") val title: String? = null,
    @SerialName("url") val url: String? = null,
    @SerialName("ytId") val ytId: String? = null,
    @SerialName("infoHash") val infoHash: String? = null,
    @SerialName("fileIdx") val fileIdx: Int? = null,
    @SerialName("externalUrl") val externalUrl: String? = null,
    @SerialName("behaviorHints") val behaviorHints: StremioBehaviorHints? = null,
)

@Serializable
data class StremioBehaviorHints(
    @SerialName("notWebReady") val notWebReady: Boolean? = null,
    @SerialName("bingeGroup") val bingeGroup: String? = null,
    @SerialName("proxyHeaders") val proxyHeaders: Map<String, String>? = null,
)
