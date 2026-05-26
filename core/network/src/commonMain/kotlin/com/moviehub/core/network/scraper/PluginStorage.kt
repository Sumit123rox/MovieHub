package com.moviehub.core.network.scraper

expect class PluginStorage {
    fun loadState(profileId: String): String?
    fun saveState(profileId: String, payload: String)
}

expect fun currentPluginPlatform(): String

expect fun currentEpochMillis(): Long
