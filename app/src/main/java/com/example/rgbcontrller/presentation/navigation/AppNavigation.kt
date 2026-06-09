package com.example.rgbcontrller.presentation.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.Icons
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.rgbcontrller.presentation.screens.dashboard.DashboardScreen
import com.example.rgbcontrller.presentation.screens.device.DeviceScreen
import com.example.rgbcontrller.presentation.screens.editor.EditorScreen
import com.example.rgbcontrller.presentation.screens.live.LiveControlScreen
import com.example.rgbcontrller.presentation.screens.settings.SettingsScreen

sealed class AppDestination(val route: String, val label: String, val icon: ImageVector) {
    data object Dashboard : AppDestination("dashboard", "Home", Icons.Filled.Home)
    data object Live : AppDestination("live", "Live", Icons.Filled.GraphicEq)
    data object Editor : AppDestination("editor", "Editor", Icons.Filled.Edit)

    data object Device : AppDestination("device", "Device", Icons.Filled.Bluetooth)
    data object Settings : AppDestination("settings", "Settings", Icons.Filled.Settings)
}

val bottomDestinations = listOf(
    AppDestination.Dashboard,
    AppDestination.Live,
    AppDestination.Editor,
)

@Composable
fun LightDeckApp(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    androidx.compose.material3.Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            LightDeckBottomBar(navController)
        },
    ) { padding ->
        AppNavGraph(navController = navController, modifier = Modifier.padding(padding))
    }
}

@Composable
fun AppNavGraph(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(
        navController = navController,
        startDestination = AppDestination.Dashboard.route,
        modifier = modifier,
        enterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(280))
        },
        exitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(220))
        },
        popEnterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(280))
        },
        popExitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(220))
        },
    ) {
        composable(AppDestination.Dashboard.route) {
            DashboardScreen(onOpenDevice = { navController.navigate(AppDestination.Device.route) })
        }
        composable(AppDestination.Live.route) {
            LiveControlScreen()
        }
        composable(AppDestination.Editor.route) {
            EditorScreen()
        }
        composable(AppDestination.Device.route) {
            DeviceScreen(
                onBack = { navController.popBackStack() },
                onOpenSettings = { navController.navigate(AppDestination.Settings.route) },
            )
        }
        composable(AppDestination.Settings.route) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}

@Composable
private fun LightDeckBottomBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route
    val showBottomBar = bottomDestinations.any { it.route == currentRoute }
    if (!showBottomBar) return

    NavigationBar {
        bottomDestinations.forEach { destination ->
            val selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true
            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(destination.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(destination.icon, contentDescription = destination.label) },
                label = { androidx.compose.material3.Text(destination.label) },
            )
        }
    }
}
