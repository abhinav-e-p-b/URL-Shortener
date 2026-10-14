package com.ironfleet.urlshortener.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ironfleet.urlshortener.R
import com.ironfleet.urlshortener.ui.screens.HistoryScreen
import com.ironfleet.urlshortener.ui.screens.HomeScreen
import com.ironfleet.urlshortener.ui.screens.SettingsScreen

/** Navigation route constants */
object Routes {
    const val HOME = "home"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
}

/** Data class representing a bottom navigation tab */
private data class BottomNavItem(
    val route: String,
    val labelRes: Int,
    val icon: @Composable () -> Unit
)

private val bottomNavItems = listOf(
    BottomNavItem(Routes.HOME, R.string.nav_home) {
        Icon(Icons.Filled.Home, contentDescription = null)
    },
    BottomNavItem(Routes.HISTORY, R.string.nav_history) {
        Icon(Icons.Filled.History, contentDescription = null)
    },
    BottomNavItem(Routes.SETTINGS, R.string.nav_settings) {
        Icon(Icons.Filled.Settings, contentDescription = null)
    }
)

/**
 * Top-level navigation host with a Material 3 bottom navigation bar.
 * Each tab maintains its own back stack.
 */
@Composable
fun UrlShortenerNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomNavItems.forEach { item ->
                    NavigationBarItem(
                        icon = item.icon,
                        label = { Text(stringResource(item.labelRes)) },
                        selected = currentRoute == item.route,
                        onClick = {
                            navController.navigate(item.route) {
                                // Pop up to start destination so back-stack doesn't grow
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            // Pass scaffold padding so content isn't hidden behind nav bar
        ) {
            composable(Routes.HOME) {
                HomeScreen(innerPadding = innerPadding)
            }
            composable(Routes.HISTORY) {
                HistoryScreen(innerPadding = innerPadding)
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(innerPadding = innerPadding)
            }
        }
    }
}
