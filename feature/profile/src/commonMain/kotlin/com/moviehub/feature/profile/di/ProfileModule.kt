package com.moviehub.feature.profile.di

import com.moviehub.core.database.ProfileRepository
import com.moviehub.core.network.AddonManager
import com.moviehub.core.network.DownloadsRepository
import com.moviehub.feature.profile.presentation.DownloadsViewModel
import com.moviehub.feature.profile.presentation.ProfileViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val profileModule = module {
    viewModel { ProfileViewModel(get<ProfileRepository>(), get<AddonManager>()) }
    viewModel { DownloadsViewModel(get<DownloadsRepository>()) }
}
