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
import dev.yilliee.iotventure.data.remote.ApiService
import dev.yilliee.iotventure.ui.theme.*
import dev.yilliee.iotventure.di.ServiceLocator
import android.content.Context
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun LeaderboardScreen(
    onBackClick: () -> Unit,
    apiService: ApiService = remember { ApiService() },
    context: Context
) {
    var teams by remember { mutableStateOf(emptyList<TeamData>()) }
    var isOnline by remember { mutableStateOf(true) }
    var lastUpdated by remember { mutableStateOf("Never") }
    var isRefreshing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val rotationAngle by animateFloatAsState(
        targetValue = if (isRefreshing) 360f else 0f,
        label = "refreshRotation"
    )
    val scope = rememberCoroutineScope()
    val preferencesManager = remember { ServiceLocator.providePreferencesManager(context) }
    val userTeamName = remember { preferencesManager.getTeamName() }

    // Function to load leaderboard data
    fun loadLeaderboardData() {
        scope.launch {
            isRefreshing = true
            try {
                apiService.getLeaderboard().fold(
                    onSuccess = { response ->
                        // Sort teams by points in descending order
                        val sortedTeams = response.teamSolves.sortedByDescending { it.totalPoints }
                        
                        // Calculate ranks with ties
                        var currentRank = 1
                        var currentPoints = -1
                        var skipCount = 0
                        
                        teams = sortedTeams.mapIndexed { index, team ->
                            if (team.totalPoints != currentPoints) {
                                currentRank = index + 1
                                currentPoints = team.totalPoints
                                skipCount = 0
                            } else {
                                skipCount++
                            }
                            
                            TeamData(
                                id = team.teamId,
                                name = team.name,
                                rank = currentRank,
                                points = team.totalPoints,
                                solvedChallenges = team.solves.count { it.solved }
                            )
                        }
                        isOnline = true
                        error = null
                        lastUpdated = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                    },
                    onFailure = { e ->
                        isOnline = false
                        error = e.message
                    }
                )
            } catch (e: Exception) {
                isOnline = false
                error = e.message
            } finally {
                isRefreshing = false
            }
        }
    }

    // Initial load and auto-refresh every minute
    LaunchedEffect(key1 = Unit) {
        loadLeaderboardData()
        while (true) {
            delay(60000) // 1 minute
            if (isOnline) {
                loadLeaderboardData()
            }
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
            // Last updated info and refresh button
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
                    onClick = { loadLeaderboardData() },
                    modifier = Modifier.size(32.dp),
                    enabled = !isRefreshing
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = Gold,
                        modifier = Modifier.rotate(rotationAngle)
                    )
                }
            }

            // Error message if any
            error?.let { errorMessage ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = ErrorRed.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextWhite,
                        modifier = Modifier.padding(16.dp)
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
                    TeamItem(team = team, userTeamName = userTeamName)
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
                text = "Ranking",
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
fun TeamItem(team: TeamData, userTeamName: String?) {
    val isUserTeam = team.name == userTeamName
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
                    text = "${team.solvedChallenges} clues • ${team.points}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextGray
                )
            }

            // Score
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = team.points.toString(),
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

data class TeamData(
    val id: Int,
    val name: String,
    val rank: Int,
    val points: Int,
    val solvedChallenges: Int
)
