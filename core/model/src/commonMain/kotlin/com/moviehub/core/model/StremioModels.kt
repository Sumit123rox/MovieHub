package com.moviehub.core.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class StremioManifest(
    val id: String,
    val version: String,
    val name: String,
    val description: String? = null,
    val catalogs: List<StremioCatalog> = emptyList(),
    @Serializable(with = StremioResourceListSerializer::class)
    val resources: List<String> = emptyList(),
    val types: List<String> = emptyList(),
    val behaviorHints: StremioBehaviorHints = StremioBehaviorHints()
)

@Serializable
data class StremioBehaviorHints(
    val configurable: Boolean = false,
    val p2p: Boolean = false
)

object StremioResourceListSerializer : KSerializer<List<String>> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("StremioResourceList", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): List<String> {
        val input = decoder as? JsonDecoder ?: throw SerializationException("Expected JSON")
        val element = input.decodeJsonElement()
        return when (element) {
            is kotlinx.serialization.json.JsonArray -> {
                element.map { item ->
                    when (item) {
                        is JsonPrimitive -> item.content
                        is JsonObject -> item["name"]?.jsonPrimitive?.content ?: "unknown"
                        else -> "unknown"
                    }
                }
            }
            is JsonPrimitive -> {
                element.content.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            }
            else -> {
                emptyList()
            }
        }
    }

    override fun serialize(encoder: Encoder, value: List<String>) {
        encoder.encodeString(value.joinToString(","))
    }
}

@Serializable
data class StremioCatalog(
    val type: String,
    val id: String,
    val name: String? = null,
    val extra: List<StremioExtra> = emptyList()
)

@Serializable
data class StremioExtra(
    val name: String,
    val isRequired: Boolean = false,
    val options: List<String>? = null
)

@Serializable
data class MetaResponse(
    val meta: StremioMeta
)

@Serializable
data class CatalogResponse(
    val metas: List<StremioMeta>
)

@Serializable
data class StremioVideo(
    val id: String,
    val title: String? = null,
    val name: String? = null,
    val released: String? = null,
    val thumbnail: String? = null,
    val season: Int? = null,
    val episode: Int? = null,
    val overview: String? = null
)

@Serializable
data class StremioTrailer(
    val source: String,
    val type: String? = null
)

@Serializable
data class StremioPerson(
    val name: String,
    val role: String? = null,
    val photo: String? = null
)

object StremioPersonListSerializer : KSerializer<List<StremioPerson>> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("StremioPersonList", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): List<StremioPerson> {
        val input = decoder as? JsonDecoder ?: throw SerializationException("Expected JSON")
        val element = input.decodeJsonElement()
        return when (element) {
            is kotlinx.serialization.json.JsonArray -> {
                element.map { item ->
                    if (item is JsonPrimitive) {
                        StremioPerson(name = item.content)
                    } else if (item is JsonObject) {
                        StremioPerson(
                            name = item["name"]?.jsonPrimitive?.content ?: "unknown",
                            role = item["character"]?.jsonPrimitive?.content ?: item["role"]?.jsonPrimitive?.content,
                            photo = item["photo"]?.jsonPrimitive?.content
                        )
                    } else {
                        StremioPerson(name = "unknown")
                    }
                }
            }
            is JsonPrimitive -> {
                element.content.split(",").map { it.trim() }.filter { it.isNotEmpty() }.map { StremioPerson(name = it) }
            }
            else -> {
                emptyList()
            }
        }
    }

    override fun serialize(encoder: Encoder, value: List<StremioPerson>) {
        encoder.encodeString(value.joinToString(",") { it.name })
    }
}

@Serializable
data class StremioCompany(
    val name: String,
    val logo: String? = null
)

@Serializable
data class StremioMeta(
    val id: String,
    val type: String,
    val name: String,
    @SerialName("imdb_id") val imdbId: String? = null,
    val poster: String? = null,
    val background: String? = null,
    val logo: String? = null,
    val description: String? = null,
    val releaseInfo: String? = null,
    val runtime: String? = null,
    val imdbRating: String? = null,
    val status: String? = null,
    val ageRating: String? = null,
    val country: String? = null,
    val language: String? = null,
    val genres: List<String> = emptyList(),
    @Serializable(with = StremioPersonListSerializer::class)
    val cast: List<StremioPerson> = emptyList(),
    val directors: List<String> = emptyList(),
    val writers: List<String> = emptyList(),
    val productionCompanies: List<StremioCompany> = emptyList(),
    val networks: List<StremioCompany> = emptyList(),
    val videos: List<StremioVideo> = emptyList(),
    val trailers: List<StremioTrailer> = emptyList()
)
