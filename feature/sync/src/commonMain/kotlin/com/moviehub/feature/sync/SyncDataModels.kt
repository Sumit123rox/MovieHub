package com.moviehub.feature.sync

import kotlinx.serialization.Serializable

@Serializable
data class SupabaseFavorite(
    val content_id: String,
    val profile_id: String,
    val content_type: String,
    val title: String,
    val poster_url: String?
)

@Serializable
data class SupabaseWatchHistory(
    val media_id: String,
    val profile_id: String,
    val type: String,
    val progress_ms: Long,
    val duration_ms: Long,
    val is_watched: Boolean,
    val updated_at: Long
)

@Serializable
data class SupabaseWatchProgress(
    val media_id: String,
    val profile_id: String,
    val type: String,
    val progress_ms: Long,
    val duration_ms: Long,
    val is_watched: Boolean
)

@Serializable
data class SupabaseAddon(
    val id: String,
    val profile_id: String,
    val name: String,
    val version: String,
    val manifest_url: String
)

@Serializable
data class SupabasePreference(
    val profile_id: String,
    val theme: String,
    val accent_color: String
)
