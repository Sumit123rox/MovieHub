package com.moviehub.core.network.scraper

import android.content.Context

actual class PluginStorage(private val context: Context) {
    private val preferencesName = "moviehub_plugins"
    private val pluginsStateKey = "plugins_state"

    private val preferences by lazy {
        context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
    }

    actual fun loadState(profileId: String): String? =
        preferences.getString("${pluginsStateKey}_$profileId", null)

    actual fun saveState(profileId: String, payload: String) {
        preferences.edit()
            .putString("${pluginsStateKey}_$profileId", payload)
            .apply()
    }
}

actual fun currentPluginPlatform(): String = "android"

actual fun currentEpochMillis(): Long = System.currentTimeMillis()
