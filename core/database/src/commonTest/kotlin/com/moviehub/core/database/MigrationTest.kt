package com.moviehub.core.database

import androidx.room3.Room
import androidx.room3.util.performSuspending
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Migration tests for MovieDatabase.
 * Uses an in-memory SQLite database to verify schema after all migrations.
 */
class MigrationTest {

    private fun createTestDatabase(): MovieDatabase {
        return Room.inMemoryDatabaseBuilder<MovieDatabase>(
            factory = MovieDatabaseConstructor::initialize,
        )
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .addMigrations(
                MIGRATION_3_4,
                MIGRATION_4_5,
                MIGRATION_5_6,
                MIGRATION_6_7,
                MIGRATION_7_8,
                MIGRATION_8_9,
            )
            .build()
    }

    @Test
    fun allTablesCreatedAfterFullMigration() = runTest {
        val db = createTestDatabase()
        try {
            val tables = performSuspending(db, true, false) { conn ->
                val stmt = conn.prepare(
                    "SELECT name FROM sqlite_master WHERE type='table' ORDER BY name",
                )
                val names = mutableListOf<String>()
                try {
                    while (stmt.step()) {
                        stmt.getText(0)?.let { names.add(it) }
                    }
                } finally {
                    stmt.close()
                }
                names
            }

            assertTrue(tables.contains("addon"), "Missing table: addon")
            assertTrue(tables.contains("favorites"), "Missing table: favorites")
            assertTrue(tables.contains("search_history"), "Missing table: search_history")
            assertTrue(tables.contains("user_preferences"), "Missing table: user_preferences")
            assertTrue(tables.contains("watch_history"), "Missing table: watch_history")
            assertTrue(tables.contains("watch_progress"), "Missing table: watch_progress")
            assertTrue(tables.contains("stremio_cache"), "Missing table: stremio_cache")
            assertTrue(tables.contains("profiles"), "Missing table: profiles")
            assertTrue(tables.contains("downloads"), "Missing table: downloads")
            assertTrue(tables.contains("media_fts"), "Missing table: media_fts")
        } finally {
            db.close()
        }
    }

    @Test
    fun fts5TableCreatedByMigration7to8() = runTest {
        val db = createTestDatabase()
        try {
            val exists = performSuspending(db, true, false) { conn ->
                val stmt = conn.prepare(
                    "SELECT name FROM sqlite_master WHERE type='table' AND name='media_fts'",
                )
                try {
                    stmt.step()
                } finally {
                    stmt.close()
                }
            }
            assertTrue(exists, "FTS5 table media_fts should exist after full migration")
        } finally {
            db.close()
        }
    }

    @Test
    fun fts5DaoInsertAndSearch() = runTest {
        val db = createTestDatabase()
        try {
            val dao = MediaFtsDaoImpl(db)
            dao.insert(MediaFtsEntity(mediaId = "tt001", title = "Test Movie", overview = "A test overview"))
            dao.insert(MediaFtsEntity(mediaId = "tt002", title = "Another Film", overview = "Another overview"))

            val results = dao.search("test")
            assertEquals(1, results.size, "Should find 'Test Movie'")
            assertEquals("tt001", results[0].mediaId)
            assertEquals("Test Movie", results[0].title)

            val results2 = dao.search("film")
            assertEquals(1, results2.size, "Should find 'Another Film'")
            assertEquals("tt002", results2[0].mediaId)

            dao.deleteByMediaId("tt001")
            val results3 = dao.search("test")
            assertTrue(results3.isEmpty(), "Should be empty after delete")

            dao.clearAll()
            val results4 = dao.search("another")
            assertTrue(results4.isEmpty(), "Should be empty after clearAll")
        } finally {
            db.close()
        }
    }

    @Test
    fun migration7to8AddsDebridApiKeyColumn() = runTest {
        val db = createTestDatabase()
        try {
            val columns = performSuspending(db, true, false) { conn ->
                val stmt = conn.prepare("PRAGMA table_info('user_preferences')")
                val cols = mutableListOf<String>()
                try {
                    while (stmt.step()) {
                        stmt.getText(1)?.let { cols.add(it) }
                    }
                } finally {
                    stmt.close()
                }
                cols
            }
            assertTrue(columns.contains("debridApiKey"), "debridApiKey column should exist")
            assertTrue(columns.contains("seekIncrement"), "seekIncrement column should exist")
        } finally {
            db.close()
        }
    }

    @Test
    fun migration6to7AddsCachedAtColumn() = runTest {
        val db = createTestDatabase()
        try {
            val columns = performSuspending(db, true, false) { conn ->
                val stmt = conn.prepare("PRAGMA table_info('stremio_cache')")
                val cols = mutableListOf<String>()
                try {
                    while (stmt.step()) {
                        stmt.getText(1)?.let { cols.add(it) }
                    }
                } finally {
                    stmt.close()
                }
                cols
            }
            assertTrue(columns.contains("cachedAt"), "cachedAt column should exist")
        } finally {
            db.close()
        }
    }
}
