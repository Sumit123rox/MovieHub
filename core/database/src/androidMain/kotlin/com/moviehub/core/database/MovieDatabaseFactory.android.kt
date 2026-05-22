package com.moviehub.core.database

import android.content.Context
import androidx.room3.Room

actual typealias PlatformContext = Context

actual class MovieDatabaseFactory actual constructor(private val ctx: PlatformContext) {
    actual fun create(): MovieDatabase {
        val dbFile = ctx.getDatabasePath("moviehub.db")
        return Room.databaseBuilder<MovieDatabase>(
            context = ctx,
            name = dbFile.absolutePath,
            factory = MovieDatabaseConstructor::initialize
        )
            .configureDatabase()
            .build()
    }
}
