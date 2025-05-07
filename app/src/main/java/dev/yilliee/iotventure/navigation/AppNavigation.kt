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
import dev.yilliee.iotventure.screens.transfer.DeviceTransferScreen
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
    const val DEVICE_TRANSFER_RECEIVER_ROUTE = "device_transfer/receiver"
    const val CLUE_MAP_ROUTE = "clue_map/{challengeId}"
}

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Dashboard : Screen("dashboard")
    object DeviceTransfer : Screen("device_transfer")
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Login.route,
    context: Context
) {
    val apiService = remember { ServiceLocator.provideApiService(context) }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onDeviceTransferClick = {
                    navController.navigate(Screen.DeviceTransfer.route)
                },
                onNavigateToScreen = { route ->
                    navController.navigate(route)
                }
            )
        }

        composable(Screen.Dashboard.route) {
            GameDashboardScreen(
                onNavigateToScreen = { route ->
                    navController.navigate(route)
                },
                onEmergencyClick = {
                    navController.navigate(AppDestinations.EMERGENCY_UNLOCK_ROUTE)
                }
            )
        }

        composable(route = AppDestinations.DEVICE_TRANSFER_ROUTE) {
            DeviceTransferScreen(
                onBackClick = { navController.popBackStack() },
                onTransferComplete = {
                    navController.navigate(AppDestinations.LOGIN_ROUTE) {
                        popUpTo(AppDestinations.DASHBOARD_ROUTE) { inclusive = true }
                    }
                }
            )
        }

        composable(route = AppDestinations.DEVICE_TRANSFER_RECEIVER_ROUTE) {
            DeviceTransferScreen(
                onBackClick = { navController.popBackStack() },
                onTransferComplete = {
                    navController.navigate(AppDestinations.DASHBOARD_ROUTE) {
                        popUpTo(AppDestinations.LOGIN_ROUTE) { inclusive = true }
                    }
                },
                isReceiver = true
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
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onReturnToGame = { navController.popBackStack() }
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
    }
}
