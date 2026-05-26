package com.moviehub.core.network.scraper

import co.touchlab.kermit.Logger
import com.moviehub.core.database.ProfileRepository
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.internal.SynchronizedObject
import kotlinx.coroutines.internal.synchronized
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val pluginRepoLogger = Logger.withTag("PluginRepository")
private val pluginRepoExceptionHandler = CoroutineExceptionHandler { _, e ->
    pluginRepoLogger.e(e) { "Unhandled coroutine exception" }
}

/**
 * Repository responsible for managing user-installed JS plugin repositories.
 * Fetches plugin manifests, downloads and caches the JS scraper scripts locally,
 * and handles registration/unregistration with ScraperManager.
 */
@OptIn(InternalCoroutinesApi::class)
class PluginRepository(
    private val httpClient: HttpClient,
    private val profileRepository: ProfileRepository,
    private val pluginStorage: PluginStorage,
    private val scraperManager: ScraperManager,
) {
    private val log = Logger.withTag("PluginRepository")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default + pluginRepoExceptionHandler)

    private val _uiState = MutableStateFlow(PluginsUiState())
    val uiState: StateFlow<PluginsUiState> = _uiState.asStateFlow()

    private val stateLock = SynchronizedObject()
    private var initialized = false
    private var currentProfileId = "default"
    private val activeRefreshJobs = mutableMapOf<String, Job>()

    fun dispose() {
        scope.cancel()
    }

    init {
        // Observe profile changes to reload plugins accordingly
        scope.launch {
            try {
                profileRepository.activeProfile.collect { profile ->
                    try {
                        val nextProfileId = profile?.id ?: "default"
                        val needsInit = synchronized(stateLock) {
                            if (nextProfileId != currentProfileId || !initialized) {
                                cancelActiveRefreshes()
                                currentProfileId = nextProfileId
                                initialized = false
                                true
                            } else false
                        }
                        if (needsInit) loadAndInitializePlugins()
                    } catch (e: Exception) {
                        pluginRepoLogger.e(e) { "Error handling profile change" }
                    }
                }
            } catch (e: Exception) {
                pluginRepoLogger.e(e) { "Failed to collect active profile" }
            }
        }
    }

    /**
     * Initializes and loads the plugins from local storage, registering enabled scrapers to ScraperManager.
     */
    fun initialize() = synchronized(stateLock) {
        if (initialized) return
        loadAndInitializePlugins()
    }

    private fun loadAndInitializePlugins() {
        val profileId = synchronized(stateLock) { currentProfileId }
        val stored = loadStoredState(profileId)
        val state = PluginsUiState(
            pluginsEnabled = stored?.pluginsEnabled ?: true,
            repositories = stored?.repositories?.map {
                PluginRepositoryItem(
                    manifestUrl = it.manifestUrl,
                    name = it.name,
                    description = it.description,
                    version = it.version,
                    scraperCount = it.scraperCount,
                    lastUpdated = it.lastUpdated,
                    isRefreshing = false,
                    errorMessage = null
                )
            } ?: emptyList(),
            scrapers = stored?.scrapers?.map {
                PluginScraper(
                    id = it.id,
                    repositoryUrl = it.repositoryUrl,
                    name = it.name,
                    description = it.description,
                    version = it.version,
                    filename = it.filename,
                    supportedTypes = it.supportedTypes,
                    enabled = it.enabled,
                    manifestEnabled = it.manifestEnabled,
                    logo = it.logo,
                    contentLanguage = it.contentLanguage,
                    formats = it.formats,
                    code = it.code
                )
            } ?: emptyList()
        )

        _uiState.value = state
        synchronized(stateLock) { initialized = true }

        // Register enabled scrapers to ScraperManager
        scraperManager.clearJsPlugins()
        if (state.pluginsEnabled) {
            state.scrapers.filter { it.enabled }.forEach { scraper ->
                scraperManager.registerJsPlugin(
                    id = scraper.id,
                    name = scraper.name,
                    code = scraper.code,
                    supportedTypes = scraper.supportedTypes
                )
            }
        }
        log.i { "Loaded ${state.repositories.size} plugin repositories with ${state.scrapers.size} cached scrapers." }
    }

    /**
     * Installs a new plugin repository from its manifest URL.
     */
    suspend fun addRepository(rawUrl: String): AddPluginRepositoryResult = withContext(Dispatchers.Default) {
        initialize()
        val manifestUrl = try {
            normalizeManifestUrl(rawUrl)
        } catch (error: IllegalArgumentException) {
            return@withContext AddPluginRepositoryResult.Error(error.message ?: "Invalid repository URL")
        }

        if (_uiState.value.repositories.any { it.manifestUrl == manifestUrl }) {
            return@withContext AddPluginRepositoryResult.Error("That plugin repository is already installed.")
        }

        try {
            _uiState.update { it.copy(isInstalling = true, error = null, successMessage = null) }
            val previousById = _uiState.value.scrapers.associateBy { it.id }
            val (repo, scrapers) = fetchRepositoryData(manifestUrl, previousById)

            _uiState.update { state ->
                state.copy(
                    repositories = state.repositories + repo,
                    scrapers = state.scrapers.filterNot { it.repositoryUrl == manifestUrl } + scrapers,
                    isInstalling = false,
                    successMessage = "Successfully installed ${repo.name}"
                )
            }

            persist()
            updateScraperManager()
            AddPluginRepositoryResult.Success(repo)
        } catch (error: Throwable) {
            log.e(error) { "Failed to install plugin repository: $manifestUrl" }
            _uiState.update { it.copy(isInstalling = false, error = error.message ?: "Failed to install plugin repository") }
            AddPluginRepositoryResult.Error(error.message ?: "Unable to install plugin repository")
        }
    }

    /**
     * Uninstalls a plugin repository and removes its scrapers.
     */
    fun removeRepository(manifestUrl: String) {
        initialize()
        _uiState.update { state ->
            state.copy(
                repositories = state.repositories.filterNot { it.manifestUrl == manifestUrl },
                scrapers = state.scrapers.filterNot { it.repositoryUrl == manifestUrl }
            )
        }
        persist()
        updateScraperManager()
    }

    /**
     * Toggles a specific scraper on or off.
     */
    fun toggleScraper(scraperId: String, enabled: Boolean) {
        initialize()
        _uiState.update { state ->
            state.copy(
                scrapers = state.scrapers.map { scraper ->
                    if (scraper.id == scraperId) {
                        scraper.copy(enabled = if (scraper.manifestEnabled) enabled else false)
                    } else {
                        scraper
                    }
                }
            )
        }
        persist()
        updateScraperManager()
    }

    /**
     * Globally enables or disables all JS plugins.
     */
    fun setPluginsEnabled(enabled: Boolean) {
        initialize()
        _uiState.update { it.copy(pluginsEnabled = enabled) }
        persist()
        updateScraperManager()
    }

    /**
     * Refreshes a single repository's manifest and scrapers.
     */
    fun refreshRepository(manifestUrl: String) {
        initialize()
        val existingJob = synchronized(stateLock) { activeRefreshJobs[manifestUrl] }
        if (existingJob?.isActive == true) return

        markRefreshing(manifestUrl)
        val refreshJob = scope.launch {
            try {
                val result = runCatching {
                    val previous = _uiState.value.scrapers.associateBy { it.id }
                    fetchRepositoryData(manifestUrl, previous)
                }

                _uiState.update { state ->
                    result.fold(
                        onSuccess = { (repo, scrapers) ->
                            val updatedRepos = state.repositories.map { existing ->
                                if (existing.manifestUrl == manifestUrl) repo else existing
                            }
                            state.copy(
                                repositories = updatedRepos,
                                scrapers = state.scrapers.filterNot { it.repositoryUrl == manifestUrl } + scrapers
                            )
                        },
                        onFailure = { error ->
                            state.copy(
                                repositories = state.repositories.map { existing ->
                                    if (existing.manifestUrl == manifestUrl) {
                                        existing.copy(
                                            isRefreshing = false,
                                            errorMessage = error.message ?: "Unable to refresh repository"
                                        )
                                    } else {
                                        existing
                                    }
                                }
                            )
                        }
                    )
                }
                persist()
                updateScraperManager()
            } finally {
                synchronized(stateLock) { activeRefreshJobs.remove(manifestUrl) }
            }
        }
        synchronized(stateLock) { activeRefreshJobs[manifestUrl] = refreshJob }
    }

    /**
     * Refreshes all installed plugin repositories.
     */
    fun refreshAll() {
        initialize()
        _uiState.value.repositories.forEach { repo ->
            refreshRepository(repo.manifestUrl)
        }
    }

    private suspend fun fetchRepositoryData(
        manifestUrl: String,
        previousScrapers: Map<String, PluginScraper>,
    ): Pair<PluginRepositoryItem, List<PluginScraper>> = withContext(Dispatchers.Default) {
        val payload = httpGetText(manifestUrl)
        val manifest = PluginManifestParser.parse(payload)
        val baseUrl = manifestUrl.substringBefore("?").removeSuffix("/manifest.json")

        val scrapers = manifest.scrapers
            .filter { scraper -> isSupportedOnCurrentPlatform(scraper) }
            .mapNotNull { info ->
                val codeUrl = if (info.filename.startsWith("http://") || info.filename.startsWith("https://")) {
                    info.filename
                } else {
                    "$baseUrl/${info.filename.trimStart('/')}"
                }
                runCatching {
                    val code = httpGetText(codeUrl)
                    val scraperId = "${manifestUrl.lowercase()}:${info.id}"
                    val previous = previousScrapers[scraperId]
                    val enabled = when {
                        !info.enabled -> false
                        previous != null -> previous.enabled
                        else -> info.enabled
                    }

                    PluginScraper(
                        id = scraperId,
                        repositoryUrl = manifestUrl,
                        name = info.name,
                        description = info.description.orEmpty(),
                        version = info.version,
                        filename = info.filename,
                        supportedTypes = info.supportedTypes,
                        enabled = enabled,
                        manifestEnabled = info.enabled,
                        logo = info.logo,
                        contentLanguage = info.contentLanguage ?: emptyList(),
                        formats = info.formats ?: info.supportedFormats,
                        code = code
                    )
                }.getOrNull()
            }

        val repo = PluginRepositoryItem(
            manifestUrl = manifestUrl,
            name = manifest.name,
            description = manifest.description,
            version = manifest.version,
            scraperCount = scrapers.size,
            lastUpdated = currentEpochMillis(),
            isRefreshing = false,
            errorMessage = null
        )
        repo to scrapers
    }

    private fun isSupportedOnCurrentPlatform(scraper: PluginManifestScraper): Boolean {
        val platform = currentPluginPlatform().lowercase()
        val supported = scraper.supportedPlatforms?.map { it.lowercase() }?.toSet().orEmpty()
        val disabled = scraper.disabledPlatforms?.map { it.lowercase() }?.toSet().orEmpty()
        if (supported.isNotEmpty() && platform !in supported) return false
        if (platform in disabled) return false
        return true
    }

    private fun markRefreshing(manifestUrl: String) {
        _uiState.update { state ->
            state.copy(
                repositories = state.repositories.map { repo ->
                    if (repo.manifestUrl == manifestUrl) {
                        repo.copy(isRefreshing = true, errorMessage = null)
                    } else {
                        repo
                    }
                }
            )
        }
    }

    private fun updateScraperManager() {
        val state = _uiState.value
        scraperManager.clearJsPlugins()
        if (state.pluginsEnabled) {
            state.scrapers.filter { it.enabled }.forEach { scraper ->
                scraperManager.registerJsPlugin(
                    id = scraper.id,
                    name = scraper.name,
                    code = scraper.code,
                    supportedTypes = scraper.supportedTypes
                )
            }
        }
    }

    private suspend fun httpGetText(url: String): String {
        val response = httpClient.get(url) {
            header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
        }
        if (!response.status.isSuccess()) {
            throw Exception("Failed to fetch: HTTP ${response.status.value}")
        }
        return response.bodyAsText()
    }

    private fun persist() {
        val state = _uiState.value
        val payload = StoredPluginsState(
            pluginsEnabled = state.pluginsEnabled,
            repositories = state.repositories.map { repo ->
                StoredPluginRepository(
                    manifestUrl = repo.manifestUrl,
                    name = repo.name,
                    description = repo.description,
                    version = repo.version,
                    scraperCount = repo.scraperCount,
                    lastUpdated = repo.lastUpdated
                )
            },
            scrapers = state.scrapers.map { scraper ->
                StoredPluginScraper(
                    id = scraper.id,
                    repositoryUrl = scraper.repositoryUrl,
                    name = scraper.name,
                    description = scraper.description,
                    version = scraper.version,
                    filename = scraper.filename,
                    supportedTypes = scraper.supportedTypes,
                    enabled = scraper.enabled,
                    manifestEnabled = scraper.manifestEnabled,
                    logo = scraper.logo,
                    contentLanguage = scraper.contentLanguage,
                    formats = scraper.formats,
                    code = scraper.code
                )
            }
        )
        pluginStorage.saveState(currentProfileId, json.encodeToString(payload))
    }

    private fun loadStoredState(profileId: String): StoredPluginsState? {
        val raw = pluginStorage.loadState(profileId)?.trim().orEmpty()
        if (raw.isBlank()) return null
        return runCatching {
            json.decodeFromString<StoredPluginsState>(raw)
        }.getOrNull()
    }

    private fun cancelActiveRefreshes() = synchronized(stateLock) {
        activeRefreshJobs.values.forEach { it.cancel() }
        activeRefreshJobs.clear()
    }

    private fun normalizeManifestUrl(rawUrl: String): String {
        val trimmed = rawUrl.trim()
        require(trimmed.isNotEmpty()) { "Enter a plugin repository URL." }

        val normalizedScheme = when {
            trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
            else -> "https://$trimmed"
        }

        val withoutFragment = normalizedScheme.substringBefore("#")
        val query = withoutFragment.substringAfter("?", "")
        val path = withoutFragment.substringBefore("?").trimEnd('/')
        val manifestPath = if (path.endsWith("/manifest.json")) path else "$path/manifest.json"
        return if (query.isEmpty()) manifestPath else "$manifestPath?$query"
    }
}
