package com.moviehub.navigation

import com.moviehub.core.model.StreamItem
import kotlinx.serialization.Serializable

sealed interface Screen {
    @Serializable
    data object Home : Screen

    @Serializable
    data object Search : Screen

    @Serializable
    data object Addon : Screen

    @Serializable
    data object Auth : Screen
    
    @Serializable
    data object Sync : Screen

    @Serializable
    data object Profile : Screen
    
    @Serializable
    data object Settings : Screen
    
    @Serializable
    data class Details(val id: String, val type: String, val addonUrl: String? = null) : Screen
    
    @Serializable
    data class Player(val launchId: Long) : Screen

    @Serializable
    data class Catalog(val title: String, val type: String, val catalogId: String, val addonId: String? = null) : Screen

    @Serializable
    data class Streams(val id: String, val type: String, val mediaId: String) : Screen
}
