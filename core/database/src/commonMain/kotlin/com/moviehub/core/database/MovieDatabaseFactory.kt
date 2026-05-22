package com.moviehub.core.database

import androidx.room3.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

expect abstract class PlatformContext

fun RoomDatabase.Builder<MovieDatabase>.configureDatabase(): RoomDatabase.Builder<MovieDatabase> {
    return this
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .fallbackToDestructiveMigration()
}

expect class MovieDatabaseFactory(ctx: PlatformContext) {
    fun create(): MovieDatabase
}
