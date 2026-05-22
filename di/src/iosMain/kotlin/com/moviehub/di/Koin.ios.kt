package com.moviehub.di

import com.moviehub.core.database.PlatformContext
import com.moviehub.core.network.PlatformDownloader
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single<PlatformContext> { object : PlatformContext() {} }
    single { PlatformDownloader(get()) }
}
