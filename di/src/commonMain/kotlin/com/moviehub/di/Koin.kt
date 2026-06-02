package com.moviehub.di

import com.moviehub.core.database.CacheService
import com.moviehub.core.database.MediaFtsDao
import com.moviehub.core.database.MediaFtsDaoImpl
import com.moviehub.core.database.MovieDatabase
import com.moviehub.core.database.MovieDatabaseFactory
import com.moviehub.core.database.ProfileRepository
import com.moviehub.core.database.TmdbSettingsRepository
import com.moviehub.core.network.AddonManager
import com.moviehub.core.network.DownloadsRepository
import com.moviehub.core.network.NetworkConnectivityMonitor
import com.moviehub.core.network.StremioApiClient
import com.moviehub.core.network.YouTubePlaybackResolver
import com.moviehub.core.network.scraper.PluginRepository
import com.moviehub.core.network.scraper.ScraperManager
import com.moviehub.core.network.tmdb.TmdbEnrichmentService
import com.moviehub.core.network.tmdb.TmdbService
import com.moviehub.core.player.MoviePlayerController
import com.moviehub.feature.addon.data.AddonRepository
import com.moviehub.feature.addon.data.AddonRepositoryImpl
import com.moviehub.feature.addon.presentation.AddonViewModel
import com.moviehub.feature.addon.presentation.PluginsViewModel
import com.moviehub.feature.auth.data.AuthRepository
import com.moviehub.feature.auth.data.AuthRepositoryImpl
import com.moviehub.feature.auth.presentation.AuthViewModel
import com.moviehub.feature.details.data.DetailsRepository
import com.moviehub.feature.details.data.DetailsRepositoryImpl
import com.moviehub.feature.details.presentation.DetailsViewModel
import com.moviehub.feature.home.data.HomeRepository
import com.moviehub.feature.home.data.HomeRepositoryImpl
import com.moviehub.feature.home.presentation.CatalogViewModel
import com.moviehub.feature.home.presentation.HomeViewModel
import com.moviehub.feature.profile.di.profileModule
import com.moviehub.feature.search.data.SearchRepository
import com.moviehub.feature.search.data.SearchRepositoryImpl
import com.moviehub.feature.search.presentation.SearchViewModel
import com.moviehub.feature.sync.SyncManager
import com.moviehub.feature.sync.presentation.SyncViewModel
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appJson =
    Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

val databaseModule =
    module {
        single { MovieDatabaseFactory(get()).create() }
        single { get<MovieDatabase>().favoriteDao() }
        single { get<MovieDatabase>().searchHistoryDao() }
        single { get<MovieDatabase>().userPreferencesDao() }
        single { get<MovieDatabase>().watchHistoryDao() }
        single { get<MovieDatabase>().watchProgressDao() }
        single { get<MovieDatabase>().stremioCacheDao() }
        single { get<MovieDatabase>().profileDao() }
        single { get<MovieDatabase>().downloadDao() }
        single { get<MovieDatabase>().addonDao() }
        single { get<MovieDatabase>().customCollectionDao() }
        single { ProfileRepository(get()) }
        single { TmdbSettingsRepository(get(), get()) }
        single<Json> { appJson }
        single { com.moviehub.core.database.DebridSettingsRepository(get(), get()) }
        single { com.moviehub.core.database.TraktSettingsRepository(get(), get()) }
        single { com.moviehub.core.database.PlaybackPreferencesRepository(get(), get()) }
        single<MediaFtsDao> { MediaFtsDaoImpl(get()) }
        single { CacheService(get(), appJson) }
    }

expect val platformModule: Module

val networkModule =
    module {
        single {
            createSupabaseClient(
                supabaseUrl = "https://your-project.supabase.co",
                supabaseKey = "your-anon-key",
            ) {
                install(Postgrest)
                install(Auth)
            }
        }
        single { AddonManager(get(), get()) }
        single { DownloadsRepository(get(), get(), get()) }
        single { NetworkConnectivityMonitor(get()) }
        single {
            SyncManager(
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
            )
        }
        single { com.moviehub.core.network.NetworkDispatchers() }
        single { com.moviehub.core.network.CatalogPrefetcher(get(), get(), get(), get(), appJson) }
        single { com.moviehub.core.network.DeduplicatingCache() }
        single { com.moviehub.core.network.debrid.RealDebridClient(get(), get()) }
        single { com.moviehub.core.network.debrid.AllDebridClient(get(), get()) }
        single { com.moviehub.core.network.debrid.PremiumizeClient(get(), get()) }
        single { com.moviehub.core.network.trakt.TraktClient(get(), get()) }
        single<List<com.moviehub.core.network.debrid.DebridProvider>> {
            listOf(
                get<com.moviehub.core.network.debrid.AllDebridClient>(),
                get<com.moviehub.core.network.debrid.PremiumizeClient>()
            )
        }
        single {
            com.moviehub.core.network.torrent.HybridStreamResolver(
                debridClient = get(),
                debridSettings = get(),
                torrentEngine = get(),
                debridProviders = get()
            )
        }

        single {
            HttpClient {
                install(ContentNegotiation) {
                    json(
                        Json {
                            ignoreUnknownKeys = true
                            isLenient = true
                            coerceInputValues = true
                        },
                    )
                }
                install(HttpRequestRetry) {
                    retryOnServerErrors(maxRetries = 2)
                    retryOnException(maxRetries = 3, retryOnTimeout = true)
                    exponentialDelay(base = 500.0, maxDelayMs = 8000L)
                }
                install(ContentEncoding) {
                    gzip()
                    deflate()
                }
                install(io.ktor.client.plugins.HttpTimeout) {
                    connectTimeoutMillis = 8000
                    requestTimeoutMillis = 20000
                    socketTimeoutMillis = 15000
                }
                defaultRequest {
                    headers { append(io.ktor.http.HttpHeaders.UserAgent, "MovieHub/1.0") }
                }
            }
        }
        single { StremioApiClient(get(), get(), get()) }
        single { ScraperManager(get()) }
        single { PluginRepository(get(), get(), get(), get()) }
        single { YouTubePlaybackResolver(get()) }
        single { TmdbService(get()) }
        single { TmdbEnrichmentService(get()) }
    }

val homeModule =
    module {
        single<HomeRepository> { HomeRepositoryImpl(get(), get(), get(), appJson) }
        viewModel { HomeViewModel(get(), get(), get(), get(), get(), get()) }
        viewModel { CatalogViewModel(get(), get(), get()) }
    }

val searchModule =
    module {
        single<SearchRepository> { SearchRepositoryImpl(get(), get(), get(), get(), get()) }
        viewModel { SearchViewModel(get(), get(), get()) }
    }

val addonModule =
    module {
        single<AddonRepository> { AddonRepositoryImpl(get(), get()) }
        viewModel { AddonViewModel(get()) }
        viewModel { PluginsViewModel(get()) }
    }

val detailsModule =
    module {
        single<DetailsRepository> { DetailsRepositoryImpl(get(), get(), get(), get(), get(), get(), appJson) }
        viewModel { DetailsViewModel(get(), get(), get(), get(), get(), get(), get()) }
    }

val syncModule =
    module {
        viewModel { SyncViewModel(get(), get(), get(), get()) }
    }

val authModule =
    module {
        single<AuthRepository> { AuthRepositoryImpl(get(), get(), get(), get()) }
        viewModel { AuthViewModel(get()) }
    }

val playerModule =
    module {
        single { MoviePlayerController() }
        viewModel {
            com.moviehub.feature.player.presentation.PlayerViewModel(
                playerController = get(),
                watchProgressDao = get(),
                watchHistoryDao = get(),
                profileRepository = get(),
                userPreferencesDao = get(),
                torrentResolver = get(),
                playbackPrefsRepository = get(),
                addonManager = get(),
                stremioApiClient = get(),
            )
        }
    }

fun appModules(): List<Module> =
    listOf(
        platformModule,
        databaseModule,
        networkModule,
        playerModule,
        homeModule,
        searchModule,
        addonModule,
        detailsModule,
        syncModule,
        authModule,
        profileModule,
    )
