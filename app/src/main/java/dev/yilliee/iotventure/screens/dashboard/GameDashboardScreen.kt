package dev.yilliee.iotventure.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.yilliee.iotventure.di.ServiceLocator
import dev.yilliee.iotventure.navigation.AppDestinations
import dev.yilliee.iotventure.ui.theme.*
import dev.yilliee.iotventure.data.model.Challenge
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable

@Composable
fun GameDashboardScreen(
    onNavigateToScreen: (String) -> Unit,
    onEmergencyClick: () -> Unit
) {
    val context = LocalContext.current
    val gameRepository = remember { ServiceLocator.provideGameRepository(context) }
    val authRepository = remember { ServiceLocator.provideAuthRepository(context) }
    val preferencesManager = remember { ServiceLocator.providePreferencesManager(context) }
    val scope = rememberCoroutineScope()

    // Check authentication state when app resumes
    LaunchedEffect(Unit) {
        try {
            val isAuthenticated = authRepository.isLoggedIn()
            if (!isAuthenticated) {
                // Navigate to login screen if not authenticated
                onNavigateToScreen(AppDestinations.LOGIN_ROUTE)
            }
        } catch (e: Exception) {
            // If there's any error checking auth state, log out to be safe
            authRepository.logout()
            onNavigateToScreen(AppDestinations.LOGIN_ROUTE)
        }
    }

    var challenges by remember { mutableStateOf<List<Challenge>>(emptyList()) }
    var solvedChallengeIds by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var completionPercentage by remember { mutableStateOf(0) }

    // Collect challenges and solved challenges
    LaunchedEffect(Unit) {
        gameRepository.challenges.collectLatest {
            challenges = it
            println("Challenges received: $it")
        }
    }

    LaunchedEffect(Unit) {
        gameRepository.solvedChallenges.collectLatest {
            solvedChallengeIds = it
            completionPercentage = gameRepository.getCompletionPercentage()
        }
    }

    // Add this LaunchedEffect to explicitly load challenges when the dashboard appears
    LaunchedEffect(Unit) {
        // Explicitly load challenges from preferences
        gameRepository.loadChallengesFromPreferences()

        // Try to submit any pending solves when dashboard is shown
        scope.launch {
            try {
                gameRepository.submitSolves()
            } catch (e: Exception) {
                // Ignore errors, will try again later
            }
        }
    }

    // Add this LaunchedEffect to fetch team solves periodically
    LaunchedEffect(Unit) {
        // Initial fetch
        scope.launch {
            try {
                gameRepository.fetchTeamSolves()
            } catch (e: Exception) {
                // Ignore errors, will try again later
            }
        }

        // Periodic fetch every 30 seconds
        while (true) {
            delay(30000) // 30 seconds
            try {
                gameRepository.fetchTeamSolves()
            } catch (e: Exception) {
                // Ignore errors, will try again
            }
        }
    }

    val completedChallenges = solvedChallengeIds.size
    val totalChallenges = challenges.size

    // Get game start time from preferences
    val gameStartTime = remember { mutableStateOf(preferencesManager.getGameStartTime()) }
    var gameTime by remember { mutableStateOf(0L) }

    // Timer effect
    LaunchedEffect(key1 = Unit) {
        // If no game start time is set, set it now
        if (gameStartTime.value == 0L) {
            preferencesManager.setGameStartTime(System.currentTimeMillis())
            gameStartTime.value = preferencesManager.getGameStartTime()
        }

        while (true) {
            delay(1000)
            // Calculate elapsed time since game start
            gameTime = (System.currentTimeMillis() - gameStartTime.value) / 1000
        }
    }

    Scaffold(
        topBar = {
            GameTopBar(
                gameTime = gameTime,
                onEmergencyClick = onEmergencyClick,
                onLogoutClick = {
                    scope.launch {
                        authRepository.logout()
                        // Navigate to login screen
                        onNavigateToScreen(AppDestinations.LOGIN_ROUTE)
                    }
                }
            )
        },
        bottomBar = {
            BottomNavBar(
                selectedTab = 0,
                onTabSelected = { index, route ->
                    if (route.isNotEmpty()) {
                        onNavigateToScreen(route)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Progress Card
            ProgressCard(
                completedClues = completedChallenges,
                totalClues = totalChallenges,
                percentage = completionPercentage
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Challenges List
            Text(
                text = "Available Challenges",
                style = MaterialTheme.typography.titleLarge,
                color = Gold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (challenges.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Gold)
                }
            } else if (challenges.all { solvedChallengeIds.contains(it.id) }) {
                // All challenges completed
                AllChallengesCompletedCard()
            } else {
                // Filter to show only unsolved challenges
                val availableChallenges = challenges.filter { !solvedChallengeIds.contains(it.id) }

                if (availableChallenges.isEmpty()) {
                    AllChallengesCompletedCard()
                } else {
                    availableChallenges.forEach { challenge ->
                        ChallengeCard(
                            challenge = challenge,
                            isCompleted = solvedChallengeIds.contains(challenge.id),
                            onClick = {
                                // Navigate to map screen with the selected challenge ID
                                val route = "clue_map/${challenge.id}"
                                onNavigateToScreen(route)
                            }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Solved Challenges Section
            if (solvedChallengeIds.isNotEmpty()) {
                Text(
                    text = "Solved Challenges",
                    style = MaterialTheme.typography.titleLarge,
                    color = SuccessGreen,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                val solvedChallenges = challenges.filter { solvedChallengeIds.contains(it.id) }
                solvedChallenges.forEach { challenge ->
                    SolvedChallengeCard(
                        challenge = challenge,
                        onClick = {
                            // Navigate to map screen with the selected challenge ID
                            val route = "clue_map/${challenge.id}"
                            onNavigateToScreen(route)
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Leaderboard Button
            LeaderboardButton(
                onClick = { onNavigateToScreen(AppDestinations.LEADERBOARD_ROUTE) }
            )

            // Solve Queue Status
            val solveQueue = gameRepository.getSolveQueue()
            if (solveQueue.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                SolveQueueCard(
                    queueSize = solveQueue.size,
                    onSyncClick = {
                        scope.launch {
                            gameRepository.submitSolves()
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun SolveQueueCard(
    queueSize: Int,
    onSyncClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = DarkSurface
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Pending Sync",
                    style = MaterialTheme.typography.titleMedium,
                    color = Gold
                )

                Text(
                    text = "$queueSize ${if (queueSize == 1) "challenge" else "challenges"} pending",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextGray
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onSyncClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Gold,
                    contentColor = DarkBackground
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = "Sync",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sync with Server")
            }
        }
    }
}

@Composable
fun AllChallengesCompletedCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = DarkSurface
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = "Trophy",
                tint = Gold,
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "All Challenges Completed!",
                style = MaterialTheme.typography.titleLarge,
                color = Gold,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Congratulations! You've solved all available challenges.",
                style = MaterialTheme.typography.bodyLarge,
                color = TextWhite,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun SolvedChallengeCard(
    challenge: Challenge,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = DarkSurface
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = challenge.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextWhite
                )
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Completed",
                    tint = SuccessGreen
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = challenge.shortName,
                style = MaterialTheme.typography.bodyMedium,
                color = TextGray
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${challenge.points} points",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SuccessGreen
                )

                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "View Challenge",
                    tint = SuccessGreen
                )
            }
        }
    }
}

@Composable
fun ProgressCard(
    completedClues: Int,
    totalClues: Int,
    percentage: Int
) {
    val progress = if (totalClues > 0) completedClues.toFloat() / totalClues.toFloat() else 0f

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = DarkSurface
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Your Progress",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextWhite
                )
                Text(
                    text = "$percentage%",
                    style = MaterialTheme.typography.titleMedium,
                    color = Gold,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = Gold,
                trackColor = DarkSurfaceLight
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "$completedClues of $totalClues challenges completed",
                style = MaterialTheme.typography.bodyMedium,
                color = TextGray
            )
        }
    }
}

@Composable
fun GameTopBar(
    gameTime: Long,
    onEmergencyClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    val formattedTime = remember(gameTime) {
        val hours = TimeUnit.SECONDS.toHours(gameTime)
        val minutes = TimeUnit.SECONDS.toMinutes(gameTime) % 60
        val seconds = gameTime % 60
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    Surface(
        color = DarkSurface,
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Timer
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkSurfaceLight)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = "Timer",
                    tint = Gold,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = formattedTime,
                    color = Gold,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            Text(
                text = "City Explorer Challenge",
                style = MaterialTheme.typography.titleMedium,
                color = TextWhite
            )

            Row {
                // Logout Button
                IconButton(onClick = onLogoutClick) {
                    Icon(
                        imageVector = Icons.Default.Logout,
                        contentDescription = "Logout",
                        tint = TextGray
                    )
                }

                // Emergency Button
                IconButton(onClick = onEmergencyClick) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Emergency Unlock",
                        tint = ErrorRed
                    )
                }
            }
        }
    }
}

@Composable
fun BottomNavBar(
    selectedTab: Int,
    onTabSelected: (Int, String) -> Unit
) {
    val items = listOf(
        BottomNavItem("", Icons.Default.Home, ""), // Removed "Dashboard" text
        BottomNavItem("Map", Icons.Default.Place, "clue_map/-1"), // Changed to use -1 for default map view
        BottomNavItem("Scan", Icons.Default.QrCodeScanner, AppDestinations.SCAN_NFC_ROUTE),
        BottomNavItem("Chat", Icons.Default.Chat, AppDestinations.TEAM_CHAT_ROUTE),
        BottomNavItem("Team", Icons.Default.Group, AppDestinations.TEAM_DETAILS_ROUTE),
        BottomNavItem("Ranking", Icons.Default.EmojiEvents, AppDestinations.LEADERBOARD_ROUTE)
    )

    NavigationBar(
        containerColor = DarkSurface,
        contentColor = TextWhite
    ) {
        items.forEachIndexed { index, item ->
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title.ifEmpty { "Home" }
                    )
                },
                label = {
                    if (item.title.isNotEmpty()) {
                        Text(item.title)
                    }
                },
                selected = selectedTab == index,
                onClick = { onTabSelected(index, item.route) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Gold,
                    selectedTextColor = Gold,
                    indicatorColor = DarkSurfaceLight,
                    unselectedIconColor = TextGray,
                    unselectedTextColor = TextGray
                )
            )
        }
    }
}

data class BottomNavItem(
    val title: String,
    val icon: ImageVector,
    val route: String
)

@Composable
fun ClueCard(
    clue: ClueData,
    onScanClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = DarkSurface
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Current Clue",
                style = MaterialTheme.typography.labelLarge,
                color = TextGray
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = clue.title,
                style = MaterialTheme.typography.headlineMedium,
                color = Gold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = clue.description,
                style = MaterialTheme.typography.bodyLarge,
                color = TextWhite
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Hint Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = Gold.copy(alpha = 0.1f),
                        shape = MaterialTheme.shapes.small
                    )
                    .border(
                        width = 3.dp,
                        color = Gold,
                        shape = MaterialTheme.shapes.small
                    )
                    .padding(12.dp)
            ) {
                Column {
                    Text(
                        text = "HINT:",
                        style = MaterialTheme.typography.labelLarge,
                        color = Gold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = clue.hint,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextWhite
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Scan Button
            Button(
                onClick = onScanClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Gold,
                    contentColor = DarkBackground
                )
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = "Scan",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Scan NFC Token",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun LeaderboardButton(
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = Gold,
            contentColor = DarkBackground
        )
    ) {
        Icon(
            imageVector = Icons.Default.EmojiEvents,
            contentDescription = "Ranking",
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "View Ranking",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
    }
}

data class ClueData(
    val id: Int,
    val title: String,
    val description: String,
    val hint: String,
    val isCompleted: Boolean
)

@Composable
fun DeviceTransferButton(
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = Gold
        ),
        border = BorderStroke(1.dp, Gold)
    ) {
        Icon(
            imageVector = Icons.Default.BluetoothSearching,
            contentDescription = "Transfer",
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Transfer Progress to Another Device",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun ChallengeCard(
    challenge: Challenge,
    isCompleted: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = DarkSurface
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = challenge.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextWhite
                )
                if (isCompleted) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Completed",
                        tint = Gold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = challenge.shortName,
                style = MaterialTheme.typography.bodyMedium,
                color = TextGray
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${challenge.points} points",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Gold
                )

                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "View Challenge",
                    tint = Gold
                )
            }
        }
    }
}
