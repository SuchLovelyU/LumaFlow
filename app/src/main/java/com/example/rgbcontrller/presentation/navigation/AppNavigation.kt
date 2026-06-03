package com.example.rgbcontrller.presentation.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
import com.example.rgbcontrller.presentation.screens.effects.EffectDetailScreen
import com.example.rgbcontrller.presentation.screens.effects.EffectsScreen
import com.example.rgbcontrller.presentation.screens.live.LiveControlScreen
import com.example.rgbcontrller.presentation.screens.sensors.SensorModeDetailScreen
import com.example.rgbcontrller.presentation.screens.sensors.SensorModesScreen
import com.example.rgbcontrller.presentation.screens.settings.SettingsScreen

sealed class AppDestination(val route: String, val label: String, val icon: String) {
    data object Dashboard : AppDestination("dashboard", "Home", "H")
    data object Effects : AppDestination("effects", "Effects", "FX")
    data object Live : AppDestination("live", "Live", "L")
    data object Sensors : AppDestination("sensors", "Sensors", "S")
    data object Editor : AppDestination("editor", "Editor", "E")
    data object Device : AppDestination("device", "Device", "D")
    data object Settings : AppDestination("settings", "Settings", "ST")

    data object EffectDetail : AppDestination("effect/{effectId}", "Effect", "FX") {
        fun createRoute(effectId: String) = "effect/$effectId"
    }

    data object SensorDetail : AppDestination("sensor/{modeId}", "Sensor", "S") {
        fun createRoute(modeId: String) = "sensor/$modeId"
    }
}

val bottomDestinations = listOf(
    AppDestination.Dashboard,
    AppDestination.Effects,
    AppDestination.Live,
    AppDestination.Sensors,
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
    ) { _ ->
        AppNavGraph(navController = navController)
    }
}

@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = AppDestination.Dashboard.route,
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
        composable(AppDestination.Effects.route) {
            EffectsScreen(onOpenEffect = { navController.navigate(AppDestination.EffectDetail.createRoute(it)) })
        }
        composable(AppDestination.Live.route) {
            LiveControlScreen()
        }
        composable(AppDestination.Sensors.route) {
            SensorModesScreen(onOpenMode = { navController.navigate(AppDestination.SensorDetail.createRoute(it)) })
        }
        composable(AppDestination.Editor.route) {
            EditorScreen()
        }
        composable(AppDestination.Device.route) {
            DeviceScreen(onOpenSettings = { navController.navigate(AppDestination.Settings.route) })
        }
        composable(AppDestination.Settings.route) {
            SettingsScreen()
        }
        composable(AppDestination.EffectDetail.route) { entry ->
            val effectId = entry.arguments?.getString("effectId").orEmpty()
            EffectDetailScreen(effectId = effectId, onBack = { navController.popBackStack() })
        }
        composable(AppDestination.SensorDetail.route) { entry ->
            val modeId = entry.arguments?.getString("modeId").orEmpty()
            SensorModeDetailScreen(modeId = modeId)
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
                icon = { Text(destination.icon) },
                label = { Text(destination.label) },
            )
        }
    }
}
