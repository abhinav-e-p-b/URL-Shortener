package com.ironfleet.urlshortener.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
    const val HOME     = "home"
    const val HISTORY  = "history"
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
 *
 * The global Scaffold owns:
 *  - a [TopAppBar] showing the [AppLogo] composable (surface colour, no elevation)
 *  - a [NavigationBar] at the bottom (surface colour, with a top divider)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UrlShortenerNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        // ── Global TopAppBar — AppLogo centred, no elevation, bottom divider ──
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TopAppBar(
                    title = {
                        AppLogo()
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    // No elevation — rely on the divider for chrome separation
                    scrollBehavior = null
                )
                // Bottom divider below TopAppBar — tactile chrome separation
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline,
                    thickness = 1.dp
                )
            }
        },
        // ── Bottom NavigationBar — surface colour with top divider ─────────────
        bottomBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Top divider above nav bar — tactile chrome separation
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline,
                    thickness = 1.dp
                )
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
                ) {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = item.icon,
                            label = { Text(stringResource(item.labelRes)) },
                            selected = currentRoute == item.route,
                            colors = NavigationBarItemDefaults.colors(
                                // Kraft tactile: muted outline tint for selected indicator,
                                // charcoal for selected text/icon, greyboard for unselected
                                indicatorColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                selectedIconColor = MaterialTheme.colorScheme.onSurface,
                                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
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
