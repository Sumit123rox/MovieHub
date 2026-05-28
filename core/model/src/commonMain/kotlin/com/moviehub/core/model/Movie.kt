package com.moviehub.core.model

import androidx.compose.runtime.Immutable

@Immutable
data class Movie(
    val id: String,
    val title: String,
    val posterUrl: String
)
