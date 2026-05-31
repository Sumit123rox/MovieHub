package com.moviehub.core.database

import androidx.room3.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromContentType(value: ContentType): String = value.name

    @TypeConverter
    fun toContentType(value: String): ContentType = ContentType.valueOf(value)

    @TypeConverter
    fun fromStringMap(value: Map<String, String>): String = json.encodeToString(value)

    @TypeConverter
    fun toStringMap(value: String): Map<String, String> = try {
        json.decodeFromString(value)
    } catch (e: Exception) {
        emptyMap()
    }

    @TypeConverter
    fun fromDownloadState(value: com.moviehub.core.model.DownloadState): String = value.name

    @TypeConverter
    fun toDownloadState(value: String): com.moviehub.core.model.DownloadState = com.moviehub.core.model.DownloadState.valueOf(value)
}
