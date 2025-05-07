package dev.yilliee.iotventure.screens.team

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.yilliee.iotventure.data.local.PreferencesManager
import dev.yilliee.iotventure.data.model.Challenge
import dev.yilliee.iotventure.data.model.SolvedChallenge
import dev.yilliee.iotventure.data.model.TeamMember
import dev.yilliee.iotventure.di.ServiceLocator
import dev.yilliee.iotventure.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch

@Composable
fun TeamDetailsScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val preferencesManager = remember { ServiceLocator.providePreferencesManager(context) }
    val gameRepository = remember { ServiceLocator.provideGameRepository(context) }
    val scope = rememberCoroutineScope()

    // Get team data from preferences
    val teamName = remember { preferencesManager.getTeamName() ?: "Your Team" }
    val currentUsername = remember { preferencesManager.getUsername() ?: "Unknown User" }

    // State for team members
    var teamMembers by remember { mutableStateOf<List<TeamMember>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Get solved challenges
    val solvedChallengesIds = remember { preferencesManager.getSolvedChallenges() }

    // Get all challenges
    val challenges = remember { preferencesManager.getChallenges() }

    // Calculate team stats
    val completedChallenges = solvedChallengesIds.size
    val totalPoints = challenges
        .filter { challenge -> solvedChallengesIds.contains(challenge.id) }
        .sumOf { challenge -> challenge.points }
    val completionPercentage = if (challenges.isNotEmpty()) {
        (completedChallenges * 100) / challenges.size
    } else {
        0
    }

    // Fetch team members
    LaunchedEffect(Unit) {
        try {
            val members = gameRepository.getTeamMembers()
            teamMembers = members.map { member ->
                TeamMember(
                    username = member.username,
                    lastActive = member.lastActive,
                    solvedChallenges = member.solvedChallenges,
                    isCurrentUser = member.username == currentUsername
                )
            }
        } catch (e: Exception) {
            // Handle error
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TeamTopBar(onBackClick = onBackClick)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Team Card
            TeamCard(
                teamName = teamName,
                username = currentUsername,
                completedChallenges = completedChallenges,
                totalPoints = totalPoints,
                completionPercentage = completionPercentage
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Team Stats
            Text(
                text = "Team Stats",
                style = MaterialTheme.typography.titleLarge,
                color = Gold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            StatsCard(
                completedChallenges = completedChallenges,
                totalPoints = totalPoints,
                completionPercentage = completionPercentage
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Team Members
            Text(
                text = "Team Members",
                style = MaterialTheme.typography.titleLarge,
                color = Gold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Gold)
                }
            } else {
                teamMembers.forEach { member ->
                    MemberCard(
                        member = member,
                        preferencesManager = preferencesManager
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun TeamTopBar(onBackClick: () -> Unit) {
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

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = "Team Details",
                style = MaterialTheme.typography.titleLarge,
                color = TextWhite
            )
        }
    }
}

@Composable
fun TeamCard(
    teamName: String,
    username: String,
    completedChallenges: Int,
    totalPoints: Int,
    completionPercentage: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = DarkSurface
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = teamName,
                style = MaterialTheme.typography.titleLarge,
                color = Gold,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Username: $username",
                style = MaterialTheme.typography.bodyMedium,
                color = TextWhite
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Completed Challenges: $completedChallenges",
                style = MaterialTheme.typography.bodyMedium,
                color = TextWhite
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Total Points: $totalPoints",
                style = MaterialTheme.typography.bodyMedium,
                color = TextWhite
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Completion: $completionPercentage%",
                style = MaterialTheme.typography.bodyMedium,
                color = TextWhite
            )
        }
    }
}

@Composable
fun StatsCard(
    completedChallenges: Int,
    totalPoints: Int,
    completionPercentage: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = DarkSurface
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            StatItem(label = "Completed Challenges", value = completedChallenges.toString())
            StatItem(label = "Total Points", value = totalPoints.toString())
            StatItem(label = "Completion", value = "$completionPercentage%")
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextWhite
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = Gold
        )
    }
}

@Composable
private fun MemberCard(
    member: TeamMember,
    preferencesManager: PreferencesManager,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (member.isCurrentUser) "${member.username} (You)" else member.username,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (member.isCurrentUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Last Active: ${member.lastActive}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Solved Challenges",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = member.solvedChallenges.size.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Column {
                    Text(
                        text = "Total Points",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = member.solvedChallenges.sumOf { solvedChallenge: SolvedChallenge ->
                            preferencesManager.getChallenges()
                                .find { challenge -> challenge.id == solvedChallenge.challengeId }
                                ?.points ?: 0
                        }.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

// Keep these for backward compatibility

data class ClueFinding(
    val id: Int,
    val clueNumber: Int,
    val clueTitle: String,
    val foundBy: String,
    val timestamp: String
)

private fun formatLastActive(timestamp: Long): String {
    val date = Date(timestamp)
    return SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(date)
}
