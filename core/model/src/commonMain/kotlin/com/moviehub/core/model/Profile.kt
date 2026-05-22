package com.moviehub.core.model

import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    val id: String,
    val name: String,
    val avatarUrl: String? = null,
    val pin: String? = null,
    val isChild: Boolean = false,
    val createdAt: Long = 0L
)
