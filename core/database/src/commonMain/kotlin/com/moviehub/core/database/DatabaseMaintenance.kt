package com.moviehub.core.database

import androidx.room3.RoomDatabase
import androidx.room3.util.performSuspending
import androidx.sqlite.execSQL

/**
 * Database maintenance utilities: VACUUM, integrity check, FTS rebuild, size reporting.
 */
class DatabaseMaintenance(private val db: RoomDatabase) {

    suspend fun vacuum() {
        performSuspending(db, false, true) { connection ->
            connection.execSQL("VACUUM")
        }
    }

    suspend fun integrityCheck(): List<String> {
        return performSuspending(db, true, false) { connection ->
            val stmt = connection.prepare("PRAGMA integrity_check")
            try {
                val results = mutableListOf<String>()
                while (stmt.step()) {
                    stmt.getText(0)?.let { results.add(it) }
                }
                results
            } finally {
                stmt.close()
            }
        }
    }

    suspend fun quickCheck(): Boolean {
        return performSuspending(db, true, false) { connection ->
            val stmt = connection.prepare("PRAGMA quick_check")
            try {
                if (stmt.step()) {
                    stmt.getText(0) == "ok"
                } else {
                    false
                }
            } finally {
                stmt.close()
            }
        }
    }

    suspend fun rebuildFts() {
        performSuspending(db, false, true) { connection ->
            connection.execSQL("INSERT INTO media_fts(media_fts) VALUES('rebuild')")
        }
    }

    suspend fun optimizeFts() {
        performSuspending(db, false, true) { connection ->
            connection.execSQL("INSERT INTO media_fts(media_fts) VALUES('optimize')")
        }
    }

    suspend fun pageCount(): Long {
        return performSuspending(db, true, false) { connection ->
            val stmt = connection.prepare("PRAGMA page_count")
            try {
                if (stmt.step()) stmt.getLong(0) else 0L
            } finally {
                stmt.close()
            }
        }
    }

    suspend fun databaseSizeBytes(): Long {
        return performSuspending(db, true, false) { connection ->
            val stmt = connection.prepare("PRAGMA page_size")
            val pageSize: Long = try {
                if (stmt.step()) stmt.getLong(0) else 4096L
            } finally {
                stmt.close()
            }
            val pages = pageCount()
            pages * pageSize
        }
    }

    suspend fun tableRowCounts(): Map<String, Long> {
        return performSuspending(db, true, false) { connection ->
            val stmt = connection.prepare(
                "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' AND name NOT LIKE 'room_%' AND name NOT LIKE '%_fts%' ORDER BY name",
            )
            val tables = mutableListOf<String>()
            try {
                while (stmt.step()) {
                    stmt.getText(0)?.let { tables.add(it) }
                }
            } finally {
                stmt.close()
            }

            val counts = mutableMapOf<String, Long>()
            for (table in tables) {
                val countStmt = connection.prepare("SELECT COUNT(*) FROM \"$table\"")
                try {
                    if (countStmt.step()) {
                        counts[table] = countStmt.getLong(0)
                    }
                } finally {
                    countStmt.close()
                }
            }
            counts
        }
    }

    suspend fun ftsRowCount(): Long {
        return performSuspending(db, true, false) { connection ->
            val stmt = connection.prepare("SELECT COUNT(*) FROM media_fts")
            try {
                if (stmt.step()) stmt.getLong(0) else 0L
            } finally {
                stmt.close()
            }
        }
    }

    /** Full maintenance run: quick check, rebuild FTS, vacuum. */
    suspend fun performMaintenance() {
        if (!quickCheck()) {
            throw IllegalStateException("Database integrity check failed")
        }
        rebuildFts()
        optimizeFts()
        vacuum()
    }
}
