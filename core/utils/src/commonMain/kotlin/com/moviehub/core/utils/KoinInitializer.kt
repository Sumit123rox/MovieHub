package com.moviehub.core.utils

import co.touchlab.kermit.Logger
import co.touchlab.kermit.platformLogWriter
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration

fun initKoin(appDeclaration: KoinAppDeclaration = {}, modules: List<Module> = emptyList()) {
    if (GlobalContext.getOrNull() == null) {
        startKoin {
            appDeclaration()
            modules(modules)
        }
        Logger.setLogWriters(platformLogWriter())
    }
}
