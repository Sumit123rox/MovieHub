package com.moviehub.core.database

import androidx.room3.RoomDatabase
import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

expect abstract class PlatformContext

private val MIGRATION_3_4 = object : Migration(3, 4) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE `watch_progress` ADD COLUMN `audioGroupIndex` INTEGER NOT NULL DEFAULT -2")
        connection.execSQL("ALTER TABLE `watch_progress` ADD COLUMN `audioTrackIndex` INTEGER NOT NULL DEFAULT -2")
        connection.execSQL("ALTER TABLE `watch_progress` ADD COLUMN `subtitleGroupIndex` INTEGER NOT NULL DEFAULT -2")
        connection.execSQL("ALTER TABLE `watch_progress` ADD COLUMN `subtitleTrackIndex` INTEGER NOT NULL DEFAULT -2")
    }
}

private val MIGRATION_4_5 = object : Migration(4, 5) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE `user_preferences` ADD COLUMN `seekIncrement` INTEGER NOT NULL DEFAULT 10")
    }
}

private val MIGRATION_5_6 = object : Migration(5, 6) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE `stremio_cache` ADD COLUMN `cachedAt` INTEGER NOT NULL DEFAULT 0")
    }
}

fun RoomDatabase.Builder<MovieDatabase>.configureDatabase(): RoomDatabase.Builder<MovieDatabase> {
    return this
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
}

expect class MovieDatabaseFactory(ctx: PlatformContext) {
    fun create(): MovieDatabase
}
