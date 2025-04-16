package dev.yilliee.iotventure.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.yilliee.iotventure.screens.chat.TeamChatScreen
import dev.yilliee.iotventure.screens.dashboard.GameDashboardScreen
import dev.yilliee.iotventure.screens.emergency.EmergencyUnlockScreen
import dev.yilliee.iotventure.screens.hunts.HuntsScreen
import dev.yilliee.iotventure.screens.hunts.HuntDetailsScreen
import dev.yilliee.iotventure.screens.leaderboard.LeaderboardScreen
import dev.yilliee.iotventure.screens.login.LoginScreen
import dev.yilliee.iotventure.screens.map.ClueMapScreen
import dev.yilliee.iotventure.screens.scan.ScanNfcScreen
import dev.yilliee.iotventure.screens.settings.DeviceTransferScreen
import dev.yilliee.iotventure.screens.team.TeamLogScreen

object AppDestinations {
    const val LOGIN_ROUTE = "login"
    const val HUNTS_ROUTE = "hunts"
    const val HUNT_DETAILS_ROUTE = "hunt_details/{huntId}"
    const val DASHBOARD_ROUTE = "dashboard"
    const val LEADERBOARD_ROUTE = "leaderboard"
    const val TEAM_CHAT_ROUTE = "team_chat"
    const val SCAN_NFC_ROUTE = "scan_nfc"
    const val EMERGENCY_UNLOCK_ROUTE = "emergency_unlock"
    const val DEVICE_TRANSFER_ROUTE = "device_transfer"
    const val CLUE_MAP_ROUTE = "clue_map"
    const val TEAM_LOG_ROUTE = "team_log"
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    startDestination: String = AppDestinations.LOGIN_ROUTE
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(AppDestinations.LOGIN_ROUTE) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(AppDestinations.HUNTS_ROUTE) {
                        popUpTo(AppDestinations.LOGIN_ROUTE) { inclusive = true }
                    }
                }
            )
        }

        composable(AppDestinations.HUNTS_ROUTE) {
            HuntsScreen(
                onHuntClick = { huntId ->
                    if (huntId == "current") {
                        navController.navigate(AppDestinations.DASHBOARD_ROUTE)
                    } else {
                        navController.navigate("hunt_details/$huntId")
                    }
                },
                onLogoutClick = {
                    navController.navigate(AppDestinations.LOGIN_ROUTE) {
                        popUpTo(AppDestinations.LOGIN_ROUTE) { inclusive = true }
                    }
                }
            )
        }

        composable(AppDestinations.HUNT_DETAILS_ROUTE) { backStackEntry ->
            val huntId = backStackEntry.arguments?.getString("huntId") ?: "0"
            HuntDetailsScreen(
                huntId = huntId,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(AppDestinations.DASHBOARD_ROUTE) {
            GameDashboardScreen(
                onLeaderboardClick = { navController.navigate(AppDestinations.LEADERBOARD_ROUTE) },
                onTeamChatClick = { navController.navigate(AppDestinations.TEAM_CHAT_ROUTE) },
                onScanClick = { navController.navigate(AppDestinations.SCAN_NFC_ROUTE) },
                onEmergencyClick = { navController.navigate(AppDestinations.EMERGENCY_UNLOCK_ROUTE) },
                onClueMapClick = { navController.navigate(AppDestinations.CLUE_MAP_ROUTE) },
                onTeamLogClick = { navController.navigate(AppDestinations.TEAM_LOG_ROUTE) }
            )
        }

        composable(AppDestinations.LEADERBOARD_ROUTE) {
            LeaderboardScreen(
                onBackClick = { navController.popBackStack() }
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
                    navController.navigate(AppDestinations.HUNTS_ROUTE) {
                        popUpTo(AppDestinations.HUNTS_ROUTE) { inclusive = true }
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

        composable(AppDestinations.CLUE_MAP_ROUTE) {
            ClueMapScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(AppDestinations.TEAM_LOG_ROUTE) {
            TeamLogScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
