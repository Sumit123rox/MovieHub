package com.moviehub.core.database

data class MediaFtsEntity(
    val rowId: Long = 0,
    val mediaId: String,
    val title: String,
    val overview: String?,
)
