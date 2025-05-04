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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.yilliee.iotventure.navigation.AppDestinations
import dev.yilliee.iotventure.ui.theme.*
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit
import androidx.compose.foundation.BorderStroke

@Composable
fun GameDashboardScreen(
    onNavigateToScreen: (String) -> Unit,
    onEmergencyClick: () -> Unit
) {
    var gameTime by remember { mutableStateOf(0L) }
    val currentHuntId = remember { mutableStateOf("current") } // This would come from your game state
    val currentClue = remember {
        mutableStateOf(
            ClueData(
                id = 1,
                title = "The Ancient Map",
                description = "Find the hidden map in the oldest building on campus.",
                hint = "Look for a building with a clock tower.",
                isCompleted = false
            )
        )
    }

    var selectedTab by remember { mutableStateOf(0) }

    // Timer effect
    LaunchedEffect(key1 = Unit) {
        while (true) {
            delay(1000)
            gameTime += 1
        }
    }

    Scaffold(
        topBar = {
            GameTopBar(
                gameTime = gameTime,
                onEmergencyClick = onEmergencyClick
            )
        },
        bottomBar = {
            BottomNavBar(
                selectedTab = selectedTab,
                onTabSelected = { index, route ->
                    selectedTab = index
                    if (route.isNotEmpty()) {
                        if (route == AppDestinations.TEAM_CHAT_ROUTE) {
                            onNavigateToScreen("${AppDestinations.TEAM_CHAT_ROUTE.replace("{huntId}", currentHuntId.value)}")
                        } else {
                            onNavigateToScreen(route)
                        }
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
            // Current Clue Card
            ClueCard(
                clue = currentClue.value,
                onScanClick = { onNavigateToScreen(AppDestinations.SCAN_NFC_ROUTE) }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Progress Section
            ProgressSection(
                completedClues = 7,
                totalClues = 20
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Device Transfer Button
            DeviceTransferButton(
                onClick = { onNavigateToScreen(AppDestinations.DEVICE_TRANSFER_ROUTE) }
            )

            // Extra space at bottom to account for bottom navigation
            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

@Composable
fun GameTopBar(
    gameTime: Long,
    onEmergencyClick: () -> Unit
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

@Composable
fun BottomNavBar(
    selectedTab: Int,
    onTabSelected: (Int, String) -> Unit
) {
    val items = listOf(
        BottomNavItem("Dashboard", Icons.Default.Home, ""),
        BottomNavItem("Map", Icons.Default.Place, AppDestinations.CLUE_MAP_ROUTE),
        BottomNavItem("Scan", Icons.Default.QrCodeScanner, AppDestinations.SCAN_NFC_ROUTE),
        BottomNavItem("Chat", Icons.Default.Chat, AppDestinations.TEAM_CHAT_ROUTE),
        BottomNavItem("Team", Icons.Default.Group, AppDestinations.TEAM_DETAILS_ROUTE)
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
                        contentDescription = item.title
                    )
                },
                label = { Text(item.title) },
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
fun ProgressSection(
    completedClues: Int,
    totalClues: Int
) {
    val progress = completedClues.toFloat() / totalClues.toFloat()

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
                text = "Your Progress",
                style = MaterialTheme.typography.titleMedium,
                color = TextWhite
            )

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
                text = "$completedClues of $totalClues clues solved",
                style = MaterialTheme.typography.bodyMedium,
                color = TextGray
            )
        }
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
