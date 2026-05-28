package com.moviehub.core.database

import androidx.room3.RoomDatabase
import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

expect abstract class PlatformContext

internal val MIGRATION_3_4 = object : Migration(3, 4) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE `watch_progress` ADD COLUMN `audioGroupIndex` INTEGER NOT NULL DEFAULT -2")
        connection.execSQL("ALTER TABLE `watch_progress` ADD COLUMN `audioTrackIndex` INTEGER NOT NULL DEFAULT -2")
        connection.execSQL("ALTER TABLE `watch_progress` ADD COLUMN `subtitleGroupIndex` INTEGER NOT NULL DEFAULT -2")
        connection.execSQL("ALTER TABLE `watch_progress` ADD COLUMN `subtitleTrackIndex` INTEGER NOT NULL DEFAULT -2")
    }
}

internal val MIGRATION_4_5 = object : Migration(4, 5) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE `user_preferences` ADD COLUMN `seekIncrement` INTEGER NOT NULL DEFAULT 10")
    }
}

internal val MIGRATION_5_6 = object : Migration(5, 6) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE `stremio_cache` ADD COLUMN `cachedAt` INTEGER NOT NULL DEFAULT 0")
    }
}

internal val MIGRATION_6_7 = object : Migration(6, 7) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE `user_preferences` ADD COLUMN `debridApiKey` TEXT NOT NULL DEFAULT ''")
    }
}

internal val MIGRATION_7_8 = object : Migration(7, 8) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL("""
            CREATE VIRTUAL TABLE IF NOT EXISTS media_fts USING fts5(
                mediaId, title, overview, tokenize='porter unicode61'
            )
        """)
    }
}

internal val MIGRATION_8_9 = object : Migration(8, 9) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL("DROP TABLE IF EXISTS media_fts")
    }
}

fun RoomDatabase.Builder<MovieDatabase>.configureDatabase(): RoomDatabase.Builder<MovieDatabase> {
    return this
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
        .addCallback(object : RoomDatabase.Callback() {
            override suspend fun onOpen(connection: SQLiteConnection) {
                connection.execSQL("PRAGMA journal_mode=WAL")
                connection.execSQL("PRAGMA synchronous=NORMAL")
                connection.execSQL("PRAGMA busy_timeout=5000")
                connection.execSQL("PRAGMA foreign_keys=ON")
                connection.execSQL("PRAGMA cache_size=-64000")
            }
        })
}

expect class MovieDatabaseFactory(ctx: PlatformContext) {
    fun create(): MovieDatabase
}
