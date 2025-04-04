package dev.yilliee.iotventure.screens.leaderboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.yilliee.iotventure.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LeaderboardScreen(
    onBackClick: () -> Unit
) {
    var teams by remember { mutableStateOf(emptyList<TeamData>()) }
    var isOnline by remember { mutableStateOf(true) }
    var lastUpdated by remember { mutableStateOf("2 min ago") }
    var isRefreshing by remember { mutableStateOf(false) }
    val rotationAngle by animateFloatAsState(
        targetValue = if (isRefreshing) 360f else 0f,
        label = "refreshRotation"
    )
    val scope = rememberCoroutineScope()

    // Load mock data on first composition
    LaunchedEffect(key1 = Unit) {
        loadLeaderboardData(teams) { newTeams ->
            teams = newTeams
        }
    }

    Scaffold(
        topBar = {
            LeaderboardTopBar(
                onBackClick = onBackClick,
                isOnline = isOnline
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Last updated info
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface)
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Last updated: $lastUpdated",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextGray
                )

                IconButton(
                    onClick = {
                        if (!isRefreshing) {
                            isRefreshing = true
                            scope.launch {
                                delay(1500)
                                loadLeaderboardData(teams) { newTeams ->
                                    teams = newTeams
                                }
                                lastUpdated = "Just now"
                                isRefreshing = false
                            }
                        }
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = Gold,
                        modifier = Modifier.rotate(rotationAngle)
                    )
                }
            }

            // Leaderboard list
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                items(teams) { team ->
                    TeamItem(team = team)
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            // Offline notice if applicable
            if (!isOnline) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = ErrorRed.copy(alpha = 0.2f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.WifiOff,
                            contentDescription = "Offline",
                            tint = TextWhite,
                            modifier = Modifier.size(16.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = "You're offline. Leaderboard will update when connected.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextWhite
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LeaderboardTopBar(
    onBackClick: () -> Unit,
    isOnline: Boolean
) {
    Surface(
        color = DarkSurface,
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = TextWhite
                )
            }

            Text(
                text = "Leaderboard",
                style = MaterialTheme.typography.titleLarge,
                color = TextWhite,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )

            // Online status indicator
            Icon(
                imageVector = if (isOnline) Icons.Default.Wifi else Icons.Default.WifiOff,
                contentDescription = if (isOnline) "Online" else "Offline",
                tint = if (isOnline) SuccessGreen else TextGray,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun TeamItem(team: TeamData) {
    val isUserTeam = team.id == 3 // Assuming user's team is Map Masters
    val isTopThree = team.rank <= 3

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isUserTeam) {
                    Modifier
                        .border(
                            width = 1.dp,
                            color = Gold,
                            shape = MaterialTheme.shapes.medium
                        )
                        .background(
                            color = Gold.copy(alpha = 0.1f),
                            shape = MaterialTheme.shapes.medium
                        )
                } else {
                    Modifier
                }
            ),
        colors = CardDefaults.cardColors(
            containerColor = DarkSurface
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank Badge
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        when (team.rank) {
                            1 -> Gold
                            2 -> SilverMedal
                            3 -> BronzeMedal
                            else -> DarkSurfaceLight
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = team.rank.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (team.rank <= 3) DarkBackground else TextWhite,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Team Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = if (isUserTeam) "${team.name} (You)" else team.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isUserTeam) Gold else TextWhite,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "${team.cluesSolved} clues • ${team.time}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextGray
                )
            }

            // Score
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = team.score.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isUserTeam) Gold else TextWhite,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "PTS",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextGray
                )
            }
        }
    }
}

private fun loadLeaderboardData(
    currentTeams: List<TeamData>,
    onDataLoaded: (List<TeamData>) -> Unit
) {
    // Mock data
    val mockTeams = listOf(
        TeamData(id = 1, name = "Treasure Hunters", score = 1250, cluesSolved = 15, time = "01:45:22", rank = 1),
        TeamData(id = 2, name = "Gold Diggers", score = 1180, cluesSolved = 14, time = "01:50:15", rank = 2),
        TeamData(id = 3, name = "Map Masters", score = 1050, cluesSolved = 13, time = "01:55:30", rank = 3),
        TeamData(id = 4, name = "Puzzle Pirates", score = 980, cluesSolved = 12, time = "02:05:10", rank = 4),
        TeamData(id = 5, name = "Clue Crew", score = 920, cluesSolved = 11, time = "02:10:45", rank = 5),
        TeamData(id = 6, name = "Adventure Squad", score = 850, cluesSolved = 10, time = "02:15:20", rank = 6),
        TeamData(id = 7, name = "Riddle Solvers", score = 780, cluesSolved = 9, time = "02:20:05", rank = 7),
        TeamData(id = 8, name = "Code Breakers", score = 720, cluesSolved = 8, time = "02:25:30", rank = 8),
        TeamData(id = 9, name = "Mystery Team", score = 650, cluesSolved = 7, time = "02:30:15", rank = 9),
        TeamData(id = 10, name = "Treasure Seekers", score = 600, cluesSolved = 6, time = "02:35:40", rank = 10)
    )

    onDataLoaded(mockTeams)
}

data class TeamData(
    val id: Int,
    val name: String,
    val score: Int,
    val cluesSolved: Int,
    val time: String,
    val rank: Int
)

