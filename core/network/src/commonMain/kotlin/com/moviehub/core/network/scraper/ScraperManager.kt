package com.moviehub.core.network.scraper

import co.touchlab.kermit.Logger
import com.moviehub.core.model.StreamBehaviorHints
import com.moviehub.core.model.StreamItem
import com.moviehub.core.model.StreamProxyHeaders
import io.ktor.client.HttpClient
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.internal.SynchronizedObject
import kotlinx.coroutines.internal.synchronized
import kotlinx.coroutines.supervisorScope

/**
 * Central orchestrator for user-installed JS plugin scrapers.
 * Plugins are registered dynamically at runtime — the app ships with zero built-in scrapers.
 */
@OptIn(InternalCoroutinesApi::class)
class ScraperManager(
    private val httpClient: HttpClient,
) {
    private val log = Logger.withTag("ScraperManager")

    private val lock = SynchronizedObject()

    // Dynamic JS plugin scrapers loaded at runtime from user-installed addons
    private val jsPluginScrapers = mutableListOf<JsPluginScraper>()

    /**
     * Register a JS plugin scraper to be executed alongside built-in scrapers.
     */
    fun registerJsPlugin(
        id: String,
        name: String,
        code: String,
        supportedTypes: List<String> = listOf("movie", "tv"),
    ) = synchronized(lock) {
        jsPluginScrapers.removeAll { it.id == id }
        jsPluginScrapers.add(JsPluginScraper(id, name, code, supportedTypes))
        log.i { "Registered JS plugin scraper: $name ($id)" }
    }

    /**
     * Unregister a JS plugin scraper.
     */
    fun unregisterJsPlugin(id: String) = synchronized(lock) {
        jsPluginScrapers.removeAll { it.id == id }
    }

    /**
     * Clear all registered JS plugins.
     */
    fun clearJsPlugins() = synchronized(lock) {
        jsPluginScrapers.clear()
    }

    /**
     * Returns a snapshot of all currently registered scrapers.
     */
    fun getRegisteredScrapers(): List<JsPluginScraper> = synchronized(lock) {
        jsPluginScrapers.toList()
    }

    /**
     * Run a single scraper by ID and return its results.
     */
    suspend fun getStreamsFromScraper(
        scraperId: String,
        imdbId: String,
        type: String,
        season: Int? = null,
        episode: Int? = null,
    ): List<StreamItem> {
        val normalizedType = normalizeMediaType(type)
        val plugin = synchronized(lock) {
            jsPluginScrapers.firstOrNull { it.id == scraperId }
        } ?: return emptyList()

        if (!plugin.supportsType(normalizedType)) return emptyList()

        return try {
            log.d { "Running JS plugin: ${plugin.name}" }
            val results = PluginRuntime.executePlugin(
                httpClient = httpClient,
                code = plugin.code,
                tmdbId = imdbId,
                mediaType = normalizedType,
                season = season,
                episode = episode,
                scraperId = plugin.id,
            )
            results.map { it.toStreamItem(plugin.name, plugin.id) }
        } catch (e: Exception) {
            log.e(e) { "JS plugin ${plugin.name} failed" }
            emptyList()
        }
    }

    /**
     * Query all enabled scrapers concurrently for direct-play streams.
     *
     * @param imdbId The IMDb ID (e.g., "tt33265765").
     * @param type "movie" or "series".
     * @param season Season number (null for movies).
     * @param episode Episode number (null for movies).
     * @return Combined list of StreamItems from all scrapers.
     */
    suspend fun getStreams(
        imdbId: String,
        type: String,
        season: Int? = null,
        episode: Int? = null,
    ): List<StreamItem> = supervisorScope {
        val normalizedType = normalizeMediaType(type)

        // Snapshot plugins under lock for thread-safe iteration
        val plugins = synchronized(lock) { jsPluginScrapers.toList() }
        val jsJobs = plugins
            .filter { it.supportsType(normalizedType) }
            .map { plugin ->
                async {
                    try {
                        log.d { "Running JS plugin: ${plugin.name}" }
                        val results = PluginRuntime.executePlugin(
                            httpClient = httpClient,
                            code = plugin.code,
                            tmdbId = imdbId,
                            mediaType = normalizedType,
                            season = season,
                            episode = episode,
                            scraperId = plugin.id,
                        )
                        // Convert PluginRuntimeResult to StreamItem
                        results.map { it.toStreamItem(plugin.name, plugin.id) }
                    } catch (e: Exception) {
                        log.e(e) { "JS plugin ${plugin.name} failed" }
                        emptyList()
                    }
                }
            }

        val allResults = jsJobs.awaitAll().flatten()
        log.i { "ScraperManager resolved ${allResults.size} total direct-play streams for $imdbId" }
        allResults
    }

    private fun normalizeMediaType(type: String): String = when (type.lowercase()) {
        "series", "show", "tv" -> "series"
        else -> type.lowercase()
    }

    /**
     * Convert a JS plugin result into a standard StreamItem.
     */
    fun PluginRuntimeResult.toStreamItem(pluginName: String, pluginId: String): StreamItem {
        return StreamItem(
            name = title,
            description = buildString {
                if (!quality.isNullOrBlank()) append("$quality • ")
                if (!provider.isNullOrBlank()) append("$provider • ")
                if (!size.isNullOrBlank()) append("$size • ")
                append(pluginName)
            }.trimEnd(' ', '•'),
            url = if (infoHash.isNullOrBlank()) url else null,
            infoHash = infoHash,
            sourceName = provider ?: pluginName,
            addonName = "$pluginName (Plugin)",
            addonId = pluginId,
            behaviorHints = StreamBehaviorHints(
                proxyHeaders = headers?.takeIf { it.isNotEmpty() }?.let { h ->
                    StreamProxyHeaders(request = h)
                },
            ),
        )
    }

    /**
     * Model for a registered JS plugin scraper.
     */
    data class JsPluginScraper(
        val id: String,
        val name: String,
        val code: String,
        val supportedTypes: List<String>,
    ) {
        fun supportsType(type: String): Boolean {
            val normalized = when (type.lowercase()) {
                "series", "show" -> "tv"
                else -> type.lowercase()
            }
            return supportedTypes.map { it.lowercase() }.let { types ->
                types.contains(normalized) || types.contains(type.lowercase())
            }
        }
    }
}
