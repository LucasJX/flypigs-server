package com.flypigs.ntfyapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.flypigs.ntfyapp.ui.screen.detail.DetailScreen
import com.flypigs.ntfyapp.ui.screen.home.HomeScreen
import com.flypigs.ntfyapp.ui.screen.settings.SettingsScreen
import com.flypigs.ntfyapp.ui.screen.stats.StatsScreen

@Composable
fun NtfyNavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onNavigateToSettings = { navController.navigate("settings") },
                onNavigateToDetail = { id -> navController.navigate("detail/$id") }
            )
        }
        composable("stats") {
            StatsScreen()
        }
        composable("settings") {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = "detail/{messageId}",
            arguments = listOf(navArgument("messageId") { type = NavType.StringType })
        ) {
            DetailScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
