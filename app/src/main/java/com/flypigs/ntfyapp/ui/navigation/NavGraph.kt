package com.flypigs.ntfyapp.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.flypigs.ntfyapp.ui.screen.attachment.AttachmentViewerScreen
import com.flypigs.ntfyapp.ui.screen.detail.DetailScreen
import com.flypigs.ntfyapp.ui.screen.home.HomeScreen
import com.flypigs.ntfyapp.ui.screen.settings.SettingsScreen
import com.flypigs.ntfyapp.ui.screen.stats.StatsScreen
import com.flypigs.ntfyapp.ui.theme.*

@Composable
fun NtfyNavGraph(
    navController: NavHostController,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = "home",
        modifier = modifier,
        enterTransition = { pageEnterTransition() },
        exitTransition = { pageExitTransition() },
        popEnterTransition = { pagePopEnterTransition() },
        popExitTransition = { pagePopExitTransition() }
    ) {
        composable("home") {
            HomeScreen(
                onOpenDrawer = onOpenDrawer,
                onNavigateToSettings = { navController.navigate("settings") },
                onNavigateToDetail = { id -> navController.navigate("detail/$id") },
                // v6 新增：附件回调
                onNavigateToAttachment = { url, name ->
                    val encodedUrl = Uri.encode(url)
                    val encodedName = Uri.encode(name)
                    navController.navigate("attachment/$encodedUrl/$encodedName")
                }
            )
        }
        composable("stats") {
            StatsScreen()
        }
        composable("settings") {
            SettingsScreen()
        }
        composable(
            route = "detail/{messageId}",
            arguments = listOf(navArgument("messageId") { type = NavType.StringType })
        ) {
            DetailScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAttachment = { url, name ->
                    val encodedUrl = Uri.encode(url)
                    val encodedName = Uri.encode(name)
                    navController.navigate("attachment/$encodedUrl/$encodedName")
                }
            )
        }
        // v6 新增：全屏图片查看器
        composable(
            route = "attachment/{url}/{name}",
            arguments = listOf(
                navArgument("url") { type = NavType.StringType },
                navArgument("name") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val encodedUrl = backStackEntry.arguments?.getString("url").orEmpty()
            val encodedName = backStackEntry.arguments?.getString("name").orEmpty()
            AttachmentViewerScreen(
                imageUrl = Uri.decode(encodedUrl),
                imageName = Uri.decode(encodedName),
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
