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
import dev.yilliee.iotventure.ui.theme.*
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

@Composable
fun GameDashboardScreen(
    onLeaderboardClick: () -> Unit,
    onTeamChatClick: () -> Unit,
    onScanClick: () -> Unit,
    onEmergencyClick: () -> Unit,
    onClueMapClick: () -> Unit,
    onTeamLogClick: () -> Unit
) {
    var gameTime by remember { mutableStateOf(0L) }
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
            GameBottomBar(
                onScanClick = onScanClick
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
                onScanClick = onScanClick
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons
            ActionButtons(
                onLeaderboardClick = onLeaderboardClick,
                onTeamChatClick = onTeamChatClick,
                onClueMapClick = onClueMapClick,
                onTeamLogClick = onTeamLogClick
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Progress Section
            ProgressSection(
                completedClues = 7,
                totalClues = 20
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
fun ActionButtons(
    onLeaderboardClick: () -> Unit,
    onTeamChatClick: () -> Unit,
    onClueMapClick: () -> Unit,
    onTeamLogClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Leaderboard Button
        ActionButton(
            icon = Icons.Default.EmojiEvents,
            text = "Leaderboard",
            onClick = onLeaderboardClick,
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Team Chat Button
        ActionButton(
            icon = Icons.Default.Chat,
            text = "Team Chat",
            onClick = onTeamChatClick,
            modifier = Modifier.weight(1f)
        )
    }

    Spacer(modifier = Modifier.height(12.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Map Button
        ActionButton(
            icon = Icons.Default.Place,
            text = "Clue Map",
            onClick = onClueMapClick,
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Team Button
        ActionButton(
            icon = Icons.Default.Group,
            text = "Team Log",
            onClick = onTeamLogClick,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun ActionButton(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(100.dp),
        colors = CardDefaults.cardColors(
            containerColor = DarkSurface
        ),
        shape = MaterialTheme.shapes.medium,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = Gold,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = TextWhite,
                textAlign = TextAlign.Center
            )
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

@Composable
fun GameBottomBar(
    onScanClick: () -> Unit
) {
    Surface(
        color = DarkSurface,
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { /* Navigate to leaderboard */ }) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = "Leaderboard",
                    tint = TextGray
                )
            }

            // Scan FAB
            FloatingActionButton(
                onClick = onScanClick,
                containerColor = Gold,
                contentColor = DarkBackground,
                shape = CircleShape,
                modifier = Modifier
                    .size(56.dp)
                    .offset(y = (-16).dp)
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = "Scan NFC",
                    modifier = Modifier.size(24.dp)
                )
            }

            IconButton(onClick = { /* Navigate to settings */ }) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = TextGray
                )
            }
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
