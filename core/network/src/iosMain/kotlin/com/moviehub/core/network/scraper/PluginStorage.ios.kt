package com.moviehub.core.network.scraper

import platform.Foundation.NSDate
import platform.Foundation.NSUserDefaults
import platform.Foundation.timeIntervalSince1970

actual class PluginStorage {
    private val pluginsStateKey = "plugins_state"

    actual fun loadState(profileId: String): String? =
        NSUserDefaults.standardUserDefaults.stringForKey("${pluginsStateKey}_$profileId")

    actual fun saveState(profileId: String, payload: String) {
        NSUserDefaults.standardUserDefaults.setObject(
            payload,
            forKey = "${pluginsStateKey}_$profileId",
        )
    }
}

actual fun currentPluginPlatform(): String = "ios"

actual fun currentEpochMillis(): Long =
    (NSDate().timeIntervalSince1970 * 1000.0).toLong()
