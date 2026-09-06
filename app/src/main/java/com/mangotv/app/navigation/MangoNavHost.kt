package com.mangotv.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mangotv.app.data.model.ContentType
import com.mangotv.app.ui.detail.DetailScreen
import com.mangotv.app.ui.home.HomeScreen
import com.mangotv.app.ui.player.PlayerScreen
import com.mangotv.app.ui.settings.AddAddonScreen
import com.mangotv.app.ui.settings.AddonsScreen
import com.mangotv.app.ui.settings.SettingsScreen
import com.mangotv.app.ui.sources.SourcesScreen
import java.net.URLDecoder

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
        composable(MangoRoutes.DETAIL_PATTERN) {
            DetailScreen(
                onNavigate = { route -> navController.navigate(route) }
            )
        }
        composable(MangoRoutes.SOURCES_PATTERN) {
            SourcesScreen(
                onNavigate = { route -> navController.navigate(route) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(MangoRoutes.PLAYER_PATTERN) { backStackEntry ->
            PlayerScreen(
                onBack = { navController.popBackStack() },
                // Pops the player off the back stack before pushing Sources
                // rather than stacking Sources on top of a dead player
                // instance the user could otherwise navigate back into.
                onChangeSource = {
                    val args = backStackEntry.arguments
                    val providerId = URLDecoder.decode(args?.getString("providerId").orEmpty(), "UTF-8")
                    val type = if (args?.getString("type") == ContentType.TV_SHOW.name) {
                        ContentType.TV_SHOW
                    } else {
                        ContentType.MOVIE
                    }
                    val id = URLDecoder.decode(args?.getString("id").orEmpty(), "UTF-8")
                    val season = args?.getString("season")?.toIntOrNull()?.takeIf { it >= 0 }
                    val episode = args?.getString("episode")?.toIntOrNull()?.takeIf { it >= 0 }
                    navController.navigate(MangoRoutes.sources(providerId, type, id, season, episode)) {
                        popUpTo(MangoRoutes.PLAYER_PATTERN) { inclusive = true }
                    }
                }
            )
        }
    }
}
