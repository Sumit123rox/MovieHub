package com.moviehub.core.database

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Fts5
import androidx.room3.FtsOptions
import androidx.room3.PrimaryKey

@Entity
@Fts5(tokenizer = FtsOptions.TOKENIZER_PORTER, tokenizerArgs = ["unicode61"])
data class MediaFtsEntity(
    @PrimaryKey
    @ColumnInfo(name = "rowid")
    val rowId: Long = 0,
    val mediaId: String,
    val title: String,
    val overview: String?
)
