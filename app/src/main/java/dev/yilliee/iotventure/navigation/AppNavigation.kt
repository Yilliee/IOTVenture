package dev.yilliee.iotventure.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import dev.yilliee.iotventure.screens.chat.TeamChatScreen
import dev.yilliee.iotventure.screens.dashboard.GameDashboardScreen
import dev.yilliee.iotventure.screens.emergency.EmergencyUnlockScreen
import dev.yilliee.iotventure.screens.leaderboard.LeaderboardScreen
import dev.yilliee.iotventure.screens.login.LoginScreen
import dev.yilliee.iotventure.screens.map.ClueMapScreen
import dev.yilliee.iotventure.screens.scan.ScanNfcScreen
import dev.yilliee.iotventure.screens.settings.DeviceTransferScreen
import dev.yilliee.iotventure.screens.team.TeamDetailsScreen
import dev.yilliee.iotventure.di.ServiceLocator
import android.content.Context
import androidx.compose.runtime.remember

object AppDestinations {
    const val LOGIN_ROUTE = "login"
    const val DASHBOARD_ROUTE = "dashboard"
    const val LEADERBOARD_ROUTE = "leaderboard"
    const val TEAM_CHAT_ROUTE = "team_chat"
    const val SCAN_NFC_ROUTE = "scan_nfc"
    const val EMERGENCY_UNLOCK_ROUTE = "emergency_unlock"
    const val DEVICE_TRANSFER_ROUTE = "device_transfer"
    const val CLUE_MAP_ROUTE = "clue_map/{challengeId}"
    const val TEAM_DETAILS_ROUTE = "team_details"
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    startDestination: String = AppDestinations.LOGIN_ROUTE,
    context: Context
) {
    val apiService = remember { ServiceLocator.provideApiService(context) }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(AppDestinations.LOGIN_ROUTE) {
            LoginScreen(
                onLoginSuccess = {
                    // Go directly to dashboard instead of hunts list
                    navController.navigate(AppDestinations.DASHBOARD_ROUTE) {
                        popUpTo(AppDestinations.LOGIN_ROUTE) { inclusive = true }
                    }
                }
            )
        }

        composable(AppDestinations.DASHBOARD_ROUTE) {
            GameDashboardScreen(
                onNavigateToScreen = { route ->
                    navController.navigate(route)
                },
                onEmergencyClick = { navController.navigate(AppDestinations.EMERGENCY_UNLOCK_ROUTE) }
            )
        }

        composable(AppDestinations.LEADERBOARD_ROUTE) {
            LeaderboardScreen(
                onBackClick = { navController.popBackStack() },
                apiService = apiService,
                context = context
            )
        }

        composable(AppDestinations.TEAM_CHAT_ROUTE) {
            TeamChatScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(AppDestinations.SCAN_NFC_ROUTE) {
            ScanNfcScreen(
                onBackClick = { navController.popBackStack() },
                onScanComplete = { navController.popBackStack() }
            )
        }

        composable(AppDestinations.EMERGENCY_UNLOCK_ROUTE) {
            EmergencyUnlockScreen(
                onBackClick = { navController.popBackStack() },
                onExitGame = {
                    navController.navigate(AppDestinations.LOGIN_ROUTE) {
                        popUpTo(AppDestinations.LOGIN_ROUTE) { inclusive = true }
                    }
                },
                onReturnToGame = { navController.popBackStack() }
            )
        }

        composable(AppDestinations.DEVICE_TRANSFER_ROUTE) {
            DeviceTransferScreen(
                onBackClick = { navController.popBackStack() },
                onTransferComplete = {
                    navController.navigate(AppDestinations.LOGIN_ROUTE) {
                        popUpTo(AppDestinations.LOGIN_ROUTE) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = AppDestinations.CLUE_MAP_ROUTE,
            arguments = listOf(
                navArgument("challengeId") {
                    type = NavType.IntType
                    defaultValue = -1
                }
            )
        ) { backStackEntry ->
            val challengeId = backStackEntry.arguments?.getInt("challengeId") ?: -1
            ClueMapScreen(
                onBackClick = { navController.popBackStack() },
                onScanClick = { challenge ->
                    navController.navigate(AppDestinations.SCAN_NFC_ROUTE)
                },
                initialChallengeId = challengeId
            )
        }

        composable(AppDestinations.TEAM_DETAILS_ROUTE) {
            TeamDetailsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
