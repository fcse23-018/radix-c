package com.terrochroma.shared.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.terrochroma.shared.ui.navigation.Screen
import com.terrochroma.shared.ui.screens.home.HomeScreen
import com.terrochroma.shared.ui.screens.capture.CaptureScreen
import com.terrochroma.shared.ui.screens.map.MapScreen
import com.terrochroma.shared.ui.screens.library.LibraryScreen
import com.terrochroma.shared.ui.screens.settings.SettingsScreen
import com.terrochroma.shared.ui.screens.subscription.SubscriptionScreen
import com.terrochroma.shared.ui.screens.export.ExportScreen
import com.terrochroma.shared.ui.screens.sync.SyncSettingsScreen
import com.terrochroma.shared.ui.theme.DeepSoil
import com.terrochroma.shared.ui.theme.LeafGreen
import com.terrochroma.shared.ui.theme.SoilBrown

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerroChromaApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val bottomNavScreens = listOf(
        Screen.Home,
        Screen.Capture,
        Screen.Map,
        Screen.Library,
        Screen.Settings
    )

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = SoilBrown,
                contentColor = DeepSoil
            ) {
                bottomNavScreens.forEach { screen ->
                    val selected = currentDestination?.hierarchy?.any {
                        it.route == screen.route
                    } == true

                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            when (screen) {
                                Screen.Home -> {
                                    if (selected) {
                                        Icon(Icons.Filled.Home, contentDescription = "Home")
                                    } else {
                                        Icon(Icons.Outlined.Home, contentDescription = "Home")
                                    }
                                }
                                Screen.Capture -> {
                                    // Center FAB for capture
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = {
                                            navController.navigate(Screen.Capture.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        },
                                        icon = {
                                            Icon(Icons.Filled.CameraAlt, contentDescription = "Capture")
                                        },
                                        label = { Text("Capture") }
                                    )
                                }
                                Screen.Map -> {
                                    if (selected) {
                                        Icon(Icons.Filled.Map, contentDescription = "Map")
                                    } else {
                                        Icon(Icons.Outlined.Map, contentDescription = "Map")
                                    }
                                }
                                Screen.Library -> {
                                    if (selected) {
                                        Icon(Icons.Filled.Folder, contentDescription = "Library")
                                    } else {
                                        Icon(Icons.Outlined.Folder, contentDescription = "Library")
                                    }
                                }
                                Screen.Settings -> {
                                    if (selected) {
                                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                                    } else {
                                        Icon(Icons.Outlined.Settings, contentDescription = "Settings")
                                    }
                                }
                                else -> {}
                            }
                        },
                        label = {
                            when (screen) {
                                Screen.Home -> Text("Home")
                                Screen.Capture -> Text("Capture")
                                Screen.Map -> Text("Map")
                                Screen.Library -> Text("Library")
                                Screen.Settings -> Text("Settings")
                                else -> {}
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = LeafGreen,
                            selectedTextColor = LeafGreen,
                            unselectedIconColor = DeepSoil,
                            unselectedTextColor = DeepSoil,
                            indicatorColor = SoilBrown
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(navController)
            }
            composable(Screen.Capture.route) {
                CaptureScreen(navController)
            }
            composable(Screen.Map.route) {
                MapScreen(navController)
            }
            composable(Screen.Library.route) {
                LibraryScreen(navController)
            }
            composable(Screen.Settings.route) {
                SettingsScreen(navController)
            }
            composable("subscription") {
                SubscriptionScreen(navController)
            }
            composable("export") {
                ExportScreen(navController)
            }
            composable("sync_settings") {
                SyncSettingsScreen(navController)
            }
        }
    }
}
