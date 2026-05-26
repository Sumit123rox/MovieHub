package com.moviehub.core.network

import com.moviehub.core.database.AddonDao
import com.moviehub.core.database.AddonEntity
import com.moviehub.core.database.ProfileDao
import com.moviehub.core.database.ProfileEntity
import com.moviehub.core.database.ProfileRepository
import com.moviehub.core.model.Profile
import com.moviehub.core.model.StremioManifest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class AddonManagerTest {

    private class FakeAddonDao : AddonDao {
        val addons = MutableStateFlow<List<AddonEntity>>(emptyList())
        override fun getAllAddons(profileId: String): Flow<List<AddonEntity>> = addons
        
        override suspend fun insertAddon(addon: AddonEntity) {
            addons.value = addons.value.filterNot { it.id == addon.id && it.profileId == addon.profileId } + addon
        }
        
        override suspend fun deleteAddon(id: String, profileId: String) {
            addons.value = addons.value.filterNot { it.id == id && it.profileId == profileId }
        }
    }

    private class FakeProfileDao : ProfileDao {
        override fun getAllProfiles(): Flow<List<ProfileEntity>> = flowOf(emptyList())
        override suspend fun getProfileById(id: String): ProfileEntity? = null
        override suspend fun insertProfile(profile: ProfileEntity) {}
        override suspend fun updateProfile(profile: ProfileEntity) {}
        override suspend fun deleteProfile(profile: ProfileEntity) {}
        override suspend fun getProfileCount(): Int = 0
    }

    @Test
    fun testAddonManagerDynamicRegistration() = runTest {
        val mockAddonDao = FakeAddonDao()
        val mockProfileDao = FakeProfileDao()
        val mockProfileRepository = ProfileRepository(mockProfileDao)
        val testProfile = Profile(id = "test-profile", name = "Test User")
        mockProfileRepository.setActiveProfile(testProfile)
        
        // Add Cinemeta to pre-populate the DB
        val cinemetaManifest = StremioManifest(
            id = "org.stremio.cinemeta",
            name = "Cinemeta",
            version = "3.0.0"
        )
        mockAddonDao.insertAddon(
            AddonEntity(
                id = cinemetaManifest.id,
                profileId = "test-profile",
                url = "https://v3-cinemeta.strem.io",
                manifest = Json.encodeToString(cinemetaManifest)
            )
        )
        
        val addonManager = AddonManager(mockAddonDao, mockProfileRepository, UnconfinedTestDispatcher())
        
        val testManifest = StremioManifest(
            id = "new.addon",
            name = "New Addon",
            version = "1.0.0",
            types = listOf("movie"),
            resources = emptyList(),
            catalogs = emptyList()
        )
        
        addonManager.addAddon("https://test.com", testManifest)
        runCurrent() // let flows update
        
        val addons = addonManager.installedAddons.value
        assertEquals(2, addons.size) // Cinemeta + New Addon
        assertEquals("https://test.com", addonManager.getAddonUrl("new.addon"))
    }

    @Test
    fun testAddonManagerDuplicatePrevention() = runTest {
        val mockAddonDao = FakeAddonDao()
        val mockProfileDao = FakeProfileDao()
        val mockProfileRepository = ProfileRepository(mockProfileDao)
        val testProfile = Profile(id = "test-profile", name = "Test User")
        mockProfileRepository.setActiveProfile(testProfile)
        
        // Add Cinemeta to pre-populate the DB
        val cinemetaManifest = StremioManifest(
            id = "org.stremio.cinemeta",
            name = "Cinemeta",
            version = "3.0.0"
        )
        mockAddonDao.insertAddon(
            AddonEntity(
                id = cinemetaManifest.id,
                profileId = "test-profile",
                url = "https://v3-cinemeta.strem.io",
                manifest = Json.encodeToString(cinemetaManifest)
            )
        )
        
        val addonManager = AddonManager(mockAddonDao, mockProfileRepository, UnconfinedTestDispatcher())
        
        val testManifest = StremioManifest(
            id = "org.stremio.cinemeta",
            name = "Cinemeta",
            version = "3.0.0"
        )
        
        addonManager.addAddon("https://duplicate.com", testManifest)
        runCurrent()
        
        val addons = addonManager.installedAddons.value
        assertEquals(1, addons.size) // Still just 1
    }
}
