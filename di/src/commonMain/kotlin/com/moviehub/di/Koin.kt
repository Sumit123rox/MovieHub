package com.moviehub.di

import com.moviehub.core.database.MovieDatabase
import com.moviehub.core.database.MovieDatabaseFactory
import com.moviehub.core.database.ProfileRepository
import com.moviehub.core.network.AddonManager
import com.moviehub.core.network.DownloadsRepository
import com.moviehub.core.network.StremioApiClient
import com.moviehub.core.network.scraper.PluginRepository
import com.moviehub.core.network.scraper.ScraperManager
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
import com.moviehub.feature.home.presentation.HomeViewModel
import com.moviehub.feature.profile.di.profileModule
import com.moviehub.feature.search.data.SearchRepository
import com.moviehub.feature.search.data.SearchRepositoryImpl
import com.moviehub.feature.search.presentation.SearchViewModel
import com.moviehub.feature.sync.SyncManager
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val databaseModule = module {
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
    single { ProfileRepository(get()) }
}

expect val platformModule: Module

val networkModule = module {
    single {
        createSupabaseClient(
            supabaseUrl = "https://your-project.supabase.co",
            supabaseKey = "your-anon-key"
        ) {
            install(Postgrest)
            install(Auth)
        }
    }
    single { AddonManager(get(), get()) }
    single { DownloadsRepository(get(), get(), get()) }
    single { SyncManager(get(), get(), get(), get()) }

    single {
        HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    coerceInputValues = true
                })
            }
            install(io.ktor.client.plugins.HttpTimeout) {
                connectTimeoutMillis = 5000
                requestTimeoutMillis = 8000
                socketTimeoutMillis = 8000
            }
        }
    }
    single { StremioApiClient(get()) }
    single { ScraperManager(get()) }
    single { PluginRepository(get(), get(), get(), get()) }
    single { com.moviehub.core.network.YouTubePlaybackResolver(get()) }
}

val homeModule = module {
    single<HomeRepository> { HomeRepositoryImpl(get(), get()) }
    viewModel { HomeViewModel(get(), get()) }
}

val searchModule = module {
    single<SearchRepository> { SearchRepositoryImpl(get(), get()) }
    viewModel { SearchViewModel(get()) }
}

val addonModule = module {
    single<AddonRepository> { AddonRepositoryImpl(get(), get()) }
    viewModel { AddonViewModel(get()) }
    viewModel { PluginsViewModel(get()) }
}

val detailsModule = module {
    single<DetailsRepository> { DetailsRepositoryImpl(get(), get(), get()) }
    viewModel { DetailsViewModel(get(), get()) }
}

val authModule = module {
    single<AuthRepository> { AuthRepositoryImpl() }
    viewModel { AuthViewModel(get()) }
}

fun appModules(): List<Module> = listOf(
    platformModule,
    databaseModule,
    networkModule,
    homeModule,
    searchModule,
    addonModule,
    detailsModule,
    authModule,
    profileModule
)
