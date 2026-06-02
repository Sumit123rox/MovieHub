package com.moviehub.feature.profile.di

import com.moviehub.core.database.DebridSettingsRepository
import com.moviehub.core.database.PlaybackPreferencesRepository
import com.moviehub.core.database.ProfileRepository
import com.moviehub.core.database.TmdbSettingsRepository
import com.moviehub.core.database.TraktSettingsRepository
import com.moviehub.core.database.UserPreferencesDao
import com.moviehub.core.network.AddonManager
import com.moviehub.core.network.DownloadsRepository
import com.moviehub.feature.profile.presentation.DownloadsViewModel
import com.moviehub.feature.profile.presentation.ProfileViewModel
import com.moviehub.feature.profile.presentation.SettingsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val profileModule = module {
    viewModel { ProfileViewModel(get<ProfileRepository>(), get<AddonManager>()) }
    viewModel { DownloadsViewModel(get<DownloadsRepository>()) }
    viewModel {
        SettingsViewModel(
            userPreferencesDao = get(),
            tmdbSettingsRepository = get(),
            debridSettingsRepository = get(),
            traktSettingsRepository = get(),
            playbackPreferencesRepository = get(),
            profileRepository = get(),
            json = get(),
        )
    }
}
