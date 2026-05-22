package com.moviehub.core.network

import com.moviehub.core.network.model.StremioManifest
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AddonManagerTest {

    @Test
    fun testAddonManagerDynamicRegistration() = runTest {
        val addonManager = AddonManager()
        val testManifest = StremioManifest(
            id = "new.addon",
            name = "New Addon",
            version = "1.0.0",
            types = listOf("movie"),
            resources = emptyList(),
            catalogs = emptyList()
        )
        
        addonManager.addAddon("https://test.com", testManifest)
        
        val addons = addonManager.installedAddons.value
        assertEquals(2, addons.size) // Cinemeta + New Addon
        assertEquals("https://test.com", addonManager.getAddonUrl("new.addon"))
    }

    @Test
    fun testAddonManagerDuplicatePrevention() = runTest {
        val addonManager = AddonManager()
        val testManifest = StremioManifest(
            id = "org.stremio.cinemeta",
            name = "Cinemeta",
            version = "3.0.0"
        )
        
        addonManager.addAddon("https://duplicate.com", testManifest)
        
        val addons = addonManager.installedAddons.value
        assertEquals(1, addons.size) // Still just 1
    }
}
