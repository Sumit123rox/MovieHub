package com.moviehub.core.utils

import co.touchlab.kermit.Logger
import co.touchlab.kermit.platformLogWriter
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration
import org.koin.mp.KoinPlatform

fun initKoin(appDeclaration: KoinAppDeclaration = {}, modules: List<Module> = emptyList()) {
    if (KoinPlatform.getKoinOrNull() == null) {
        startKoin {
            appDeclaration()
            modules(modules)
        }
        Logger.setLogWriters(platformLogWriter())
    }
}
