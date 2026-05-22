package com.moviehub.core.database

import com.moviehub.core.model.StremioMeta
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// Simple K/V Cache interface for Stremio responses
interface StremioCache {
    fun saveMeta(id: String, meta: StremioMeta)
    fun getMeta(id: String): StremioMeta?
}

// In-memory implementation for now; could be replaced by SQLDelight
class StremioCacheImpl : StremioCache {
    private val metaCache = mutableMapOf<String, String>()

    override fun saveMeta(id: String, meta: StremioMeta) {
        metaCache[id] = Json.encodeToString(meta)
    }

    override fun getMeta(id: String): StremioMeta? {
        return metaCache[id]?.let { Json.decodeFromString<StremioMeta>(it) }
    }
}
