package com.mangotv.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mangotv.app.ui.home.HomeScreen
import com.mangotv.app.ui.settings.AddAddonScreen
import com.mangotv.app.ui.settings.AddonsScreen
import com.mangotv.app.ui.settings.SettingsScreen

@Composable
fun MangoNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = MangoRoutes.HOME) {
        composable(MangoRoutes.HOME) {
            HomeScreen(
                onNavigate = { route -> navController.navigate(route) }
            )
        }
        composable(MangoRoutes.SETTINGS) {
            SettingsScreen(
                onNavigate = { route -> navController.navigate(route) },
                onOpenAddons = { navController.navigate(MangoRoutes.SETTINGS_ADDONS) }
            )
        }
        composable(MangoRoutes.SETTINGS_ADDONS) {
            AddonsScreen(
                onNavigate = { route -> navController.navigate(route) },
                onAddAddon = { navController.navigate(MangoRoutes.SETTINGS_ADD_ADDON) }
            )
        }
        composable(MangoRoutes.SETTINGS_ADD_ADDON) {
            AddAddonScreen(
                onNavigate = { route -> navController.navigate(route) },
                onInstalled = { navController.popBackStack() }
            )
        }
    }
}
