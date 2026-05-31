package com.moviehub.core.database

import kotlinx.serialization.KSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal expect fun cacheServiceTimeMillis(): Long

class CacheService(
    private val cacheDao: StremioCacheDao,
    private val json: Json,
    private val currentTimeMillis: () -> Long = ::cacheServiceTimeMillis,
) {
    companion object {
        val CATALOG_TTL_MS: Long = 10 * 60 * 1000L // 10 minutes
        val META_TTL_MS: Long = 60 * 60 * 1000L // 1 hour
        val STREAM_TTL_MS: Long = 20 * 60 * 1000L // 20 minutes
        val MAX_CACHE_ENTRIES = 500

        fun catalogKey(type: String, catalogId: String, addonId: String, skip: Int = 0) =
            "catalog:$type:$catalogId:$addonId:$skip"
        fun metaKey(id: String) = "meta:$id"
        fun streamKey(id: String, type: String) = "stream:$id:$type"
    }

    // ----- Backward-compatible (no TTL) -----

    suspend fun getCached(key: String): String? = cacheDao.getById(key)?.jsonData

    suspend fun putCache(key: String, type: String, jsonData: String) {
        cacheDao.insert(StremioCacheEntity(id = key, type = type, jsonData = jsonData, cachedAt = currentTimeMillis()))
    }

    suspend fun <T> getCachedParsed(key: String, deserializer: KSerializer<T>): T? {
        return getCached(key)?.let { json.decodeFromString(deserializer, it) }
    }

    suspend fun <T> putCacheSerialized(key: String, type: String, value: T, serializer: KSerializer<T>) {
        putCache(key, type, json.encodeToString(serializer, value))
    }

    // ----- TTL-aware overloads -----

    suspend fun getCached(key: String, ttlMs: Long): String? {
        val entity = cacheDao.getById(key) ?: return null
        if (currentTimeMillis() - entity.cachedAt > ttlMs) {
            cacheDao.delete(key)
            return null
        }
        return entity.jsonData
    }

    suspend fun <T> getCachedParsed(key: String, deserializer: KSerializer<T>, ttlMs: Long): T? {
        return getCached(key, ttlMs)?.let { json.decodeFromString(deserializer, it) }
    }

    // ----- Convenience methods with default TTLs -----

    suspend fun getCachedCatalog(key: String): String? = getCached(key, CATALOG_TTL_MS)

    suspend fun getCachedMeta(key: String): String? = getCached(key, META_TTL_MS)

    suspend fun getCachedStream(key: String): String? = getCached(key, STREAM_TTL_MS)

    suspend fun <T> getCachedCatalogParsed(key: String, deserializer: KSerializer<T>): T? =
        getCachedParsed(key, deserializer, CATALOG_TTL_MS)

    suspend fun <T> getCachedMetaParsed(key: String, deserializer: KSerializer<T>): T? =
        getCachedParsed(key, deserializer, META_TTL_MS)

    // ----- Eviction -----

    suspend fun evictIfNeeded() {
        val count = cacheDao.count()
        if (count > MAX_CACHE_ENTRIES) {
            val oldest = cacheDao.getOldestEntries(count - MAX_CACHE_ENTRIES + 50)
            oldest.forEach { cacheDao.delete(it.id) }
        }
    }

    suspend fun evictExpired() {
        val thresholds = mapOf(
            "catalog" to currentTimeMillis() - CATALOG_TTL_MS,
            "meta" to currentTimeMillis() - META_TTL_MS,
            "stream" to currentTimeMillis() - STREAM_TTL_MS,
        )
        val minThreshold = thresholds.values.min()
        cacheDao.deleteOlderThan(minThreshold)
    }

    suspend fun clearAll() = cacheDao.clearAll()

    suspend fun clearByType(type: String) = cacheDao.clearByType(type)
}
