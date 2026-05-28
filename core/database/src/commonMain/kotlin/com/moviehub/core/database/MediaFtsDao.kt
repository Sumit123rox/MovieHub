package com.moviehub.core.database

import androidx.room3.RoomDatabase
import androidx.room3.util.getColumnIndexOrThrow
import androidx.room3.util.performSuspending

interface MediaFtsDao {
    suspend fun search(query: String, limit: Int = 50): List<MediaFtsEntity>
    suspend fun insert(entity: MediaFtsEntity)
    suspend fun deleteByMediaId(mediaId: String)
    suspend fun clearAll()
    suspend fun rebuild()
}

class MediaFtsDaoImpl(private val db: RoomDatabase) : MediaFtsDao {

    override suspend fun search(query: String, limit: Int): List<MediaFtsEntity> =
        performSuspending(db, true, false) { connection ->
            val stmt = connection.prepare(
                "SELECT rowid, mediaId, title, overview FROM media_fts WHERE media_fts MATCH ? || '*' ORDER BY rank LIMIT ?"
            )
            try {
                stmt.bindText(1, query)
                stmt.bindLong(2, limit.toLong())
                val colRowId = getColumnIndexOrThrow(stmt, "rowid")
                val colMediaId = getColumnIndexOrThrow(stmt, "mediaId")
                val colTitle = getColumnIndexOrThrow(stmt, "title")
                val colOverview = getColumnIndexOrThrow(stmt, "overview")
                val result = mutableListOf<MediaFtsEntity>()
                while (stmt.step()) {
                    result.add(
                        MediaFtsEntity(
                            rowId = stmt.getLong(colRowId),
                            mediaId = stmt.getText(colMediaId) ?: "",
                            title = stmt.getText(colTitle) ?: "",
                            overview = stmt.getText(colOverview)
                        )
                    )
                }
                result
            } finally {
                stmt.close()
            }
        }

    override suspend fun insert(entity: MediaFtsEntity) {
        performSuspending(db, false, true) { connection ->
            val stmt = connection.prepare(
                "INSERT OR REPLACE INTO media_fts(rowid, mediaId, title, overview) VALUES (?, ?, ?, ?)"
            )
            try {
                stmt.bindLong(1, entity.rowId)
                stmt.bindText(2, entity.mediaId)
                stmt.bindText(3, entity.title)
                if (entity.overview == null) stmt.bindNull(4) else stmt.bindText(4, entity.overview)
                stmt.step()
            } finally {
                stmt.close()
            }
        }
    }

    override suspend fun deleteByMediaId(mediaId: String) {
        performSuspending(db, false, true) { connection ->
            val stmt = connection.prepare("DELETE FROM media_fts WHERE mediaId = ?")
            try {
                stmt.bindText(1, mediaId)
                stmt.step()
            } finally {
                stmt.close()
            }
        }
    }

    override suspend fun clearAll() {
        performSuspending(db, false, true) { connection ->
            val stmt = connection.prepare("DELETE FROM media_fts")
            try {
                stmt.step()
            } finally {
                stmt.close()
            }
        }
    }

    override suspend fun rebuild() {
        performSuspending(db, false, true) { connection ->
            val stmt = connection.prepare("INSERT INTO media_fts(media_fts) VALUES('rebuild')")
            try {
                stmt.step()
            } finally {
                stmt.close()
            }
        }
    }
}
