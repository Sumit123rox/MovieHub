package com.moviehub.core.database

import androidx.room3.Room
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

actual abstract class PlatformContext

actual class MovieDatabaseFactory actual constructor(private val ctx: PlatformContext) {
    @OptIn(ExperimentalForeignApi::class)
    actual fun create(): MovieDatabase {
        val documentDirectory = NSFileManager.defaultManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = false,
            error = null
        )
        val dbPath = documentDirectory?.path?.let { "$it/moviehub.db" } ?: "moviehub.db"
        return Room.databaseBuilder<MovieDatabase>(
            name = dbPath,
            factory = MovieDatabaseConstructor::initialize
        )
            .configureDatabase()
            .build()
    }
}
