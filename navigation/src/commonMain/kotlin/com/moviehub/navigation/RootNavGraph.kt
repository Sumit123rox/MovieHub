package com.moviehub.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.moviehub.core.model.PlayerLaunch
import com.moviehub.core.model.PlayerLaunchStore
import com.moviehub.feature.addon.presentation.AddonScreen
import com.moviehub.feature.auth.presentation.AuthScreen
import com.moviehub.feature.details.presentation.DetailsScreen
import com.moviehub.feature.details.presentation.StreamsScreen
import com.moviehub.feature.home.presentation.CatalogScreen
import com.moviehub.feature.home.presentation.HomeScreen
import com.moviehub.feature.player.presentation.PlayerScreen
import com.moviehub.feature.profile.presentation.ProfileScreen
import com.moviehub.feature.profile.presentation.ProfileViewModel
import com.moviehub.feature.profile.presentation.SettingsScreen
import com.moviehub.feature.search.presentation.SearchScreen
import com.moviehub.feature.sync.presentation.SyncScreen
import org.koin.compose.koinInject
import com.moviehub.core.database.ProfileRepository
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun RootNavGraph() {
    val navController = rememberNavController()
    val profileRepository: ProfileRepository = koinInject()
    val activeProfile by profileRepository.activeProfile.collectAsState()

    // Redirect to Profile selection if there's no active profile
    LaunchedEffect(activeProfile) {
        if (activeProfile == null) {
            val currentRoute = navController.currentBackStackEntry?.destination?.route
            if (currentRoute?.contains("Profile") != true) {
                navController.navigate(Screen.Profile) {
                    // Ensure Screen.Home is kept at the root of backstack
                    popUpTo(Screen.Home) {
                        inclusive = false
                    }
                }
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = Color.Black,
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            
            // Only show bottom bar when active profile is selected and not on the profile choose screen itself
            val isBottomBarVisible = activeProfile != null && currentDestination?.hierarchy?.any { 
                (it.route?.contains("Home") == true || 
                it.route?.contains("Search") == true || 
                it.route?.contains("Sync") == true || 
                it.route?.contains("Addon") == true ||
                it.route?.contains("Settings") == true) &&
                it.route?.contains("Profile") != true
            } == true
            
            AnimatedVisibility(
                visible = isBottomBarVisible,
                enter = fadeIn(animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(300))
            ) {
                NavigationBar(
                    containerColor = Color.Black,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    val items = listOf(
                        NavigationItem("Home", Screen.Home, Icons.Default.Home),
                        NavigationItem("Search", Screen.Search, Icons.Default.Search),
                        NavigationItem("Sync", Screen.Sync, Icons.Default.Sync),
                        NavigationItem("Addons", Screen.Addon, Icons.Default.Extension),
                        NavigationItem("Profile", Screen.Settings, Icons.Default.Person)
                    )
                    
                    items.forEach { item ->
                        val isSelected = currentDestination?.hierarchy?.any { it.route?.contains(item.screen::class.simpleName ?: "") == true } == true
                        
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = isSelected,
                            onClick = {
                                navController.navigate(item.screen) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = Color.White.copy(alpha = 0.6f),
                                unselectedTextColor = Color.White.copy(alpha = 0.6f),
                                indicatorColor = Color.White.copy(alpha = 0.1f)
                            )
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home,
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(bottom = paddingValues.calculateBottomPadding()),
            enterTransition = { fadeIn(animationSpec = tween(300)) + slideInHorizontally(initialOffsetX = { it / 10 }) },
            exitTransition = { fadeOut(animationSpec = tween(300)) + slideOutHorizontally(targetOffsetX = { -it / 10 }) },
            popEnterTransition = { fadeIn(animationSpec = tween(300)) + slideInHorizontally(initialOffsetX = { -it / 10 }) },
            popExitTransition = { fadeOut(animationSpec = tween(300)) + slideOutHorizontally(targetOffsetX = { it / 10 }) }
        ) {
            composable<Screen.Home> {
                HomeScreen(
                    onMediaClick = { id, type, addonUrl -> 
                        navController.navigate(Screen.Details(id, type, addonUrl))
                    },
                    onAuthClick = {
                        navController.navigate(Screen.Auth)
                    },
                    onSeeAllClick = { title, type, catalogId, addonId ->
                        navController.navigate(Screen.Catalog(title, type, catalogId, addonId))
                    },
                    onSearchClick = {
                        navController.navigate(Screen.Search)
                    },
                    onAddonsClick = {
                        navController.navigate(Screen.Addon)
                    }
                )
            }
            composable<Screen.Catalog> { backStackEntry ->
                val catalog = backStackEntry.toRoute<Screen.Catalog>()
                CatalogScreen(
                    title = catalog.title,
                    type = catalog.type,
                    catalogId = catalog.catalogId,
                    addonId = catalog.addonId,
                    onMediaClick = { id, type, addonUrl -> 
                        navController.navigate(Screen.Details(id, type, addonUrl))
                    },
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable<Screen.Search> {
                SearchScreen(
                    onMediaClick = { id, type -> 
                        navController.navigate(Screen.Details(id, type))
                    }
                )
            }
            composable<Screen.Addon> {
                AddonScreen()
            }
            composable<Screen.Sync> {
                SyncScreen()
            }
            composable<Screen.Profile> {
                ProfileScreen(
                    viewModel = koinViewModel(),
                    onProfileSelected = {
                        navController.navigate(Screen.Home) {
                            popUpTo(Screen.Profile) { inclusive = true }
                        }
                    }
                )
            }
            composable<Screen.Settings> {
                val scope = rememberCoroutineScope()
                SettingsScreen(
                    onSwitchProfile = {
                        scope.launch {
                            profileRepository.setActiveProfile(null)
                        }
                    },
                    onNavigateToSync = {
                        navController.navigate(Screen.Sync)
                    }
                )
            }
            composable<Screen.Auth> {
                AuthScreen(onDismiss = { navController.popBackStack() })
            }
            composable<Screen.Details> { backStackEntry ->
                val details = backStackEntry.toRoute<Screen.Details>()
                DetailsScreen(
                    id = details.id, 
                    type = details.type,
                    addonUrl = details.addonUrl,
                    onNavigateToStreams = { id, type, mediaId ->
                        navController.navigate(Screen.Streams(id, type, mediaId))
                    },
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable<Screen.Streams> { backStackEntry ->
                val streams = backStackEntry.toRoute<Screen.Streams>()
                StreamsScreen(
                    id = streams.id,
                    type = streams.type,
                    mediaId = streams.mediaId,
                    onPlayClick = { stream, allStreams, title ->
                        val launchId = PlayerLaunchStore.put(
                            PlayerLaunch(
                                stream = stream,
                                streams = allStreams,
                                title = title,
                                mediaId = streams.id,
                                mediaType = streams.type
                            )
                        )
                        navController.navigate(Screen.Player(launchId))
                    },
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable<Screen.Player> { backStackEntry ->
                val player = backStackEntry.toRoute<Screen.Player>()
                val launch = PlayerLaunchStore.get(player.launchId)
                if (launch != null) {
                    PlayerScreen(
                        stream = launch.stream,
                        streams = launch.streams,
                        title = launch.title ?: "Playing...",
                        onBackClick = { navController.popBackStack() }
                    )
                } else {
                    navController.popBackStack()
                }
            }
        }
    }
}

private data class NavigationItem(
    val label: String,
    val screen: Screen,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)
