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
import dev.yilliee.iotventure.di.ServiceLocator
import dev.yilliee.iotventure.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TeamDetailsScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val preferencesManager = remember { ServiceLocator.providePreferencesManager(context) }
    val gameRepository = remember { ServiceLocator.provideGameRepository(context) }

    // Get team data from preferences
    val teamName = remember { preferencesManager.getTeamName() ?: "Your Team" }
    val username = remember { preferencesManager.getUsername() ?: "Unknown User" }

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
                username = username,
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

            // Just show the current user since we don't have a list of team members
            MemberCard(
                name = username,
                isCurrentUser = true,
                solvedChallenges = completedChallenges,
                points = totalPoints
            )
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
fun MemberCard(
    name: String,
    isCurrentUser: Boolean,
    solvedChallenges: Int,
    points: Int
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
                text = name + (if (isCurrentUser) " (You)" else ""),
                style = MaterialTheme.typography.titleMedium,
                color = Gold,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Solved Challenges: $solvedChallenges",
                style = MaterialTheme.typography.bodyMedium,
                color = TextWhite
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Points: $points",
                style = MaterialTheme.typography.bodyMedium,
                color = TextWhite
            )
        }
    }
}

// Keep these for backward compatibility
data class TeamMember(
    val id: Int,
    val name: String,
    val cluesFound: Int
)

data class ClueFinding(
    val id: Int,
    val clueNumber: Int,
    val clueTitle: String,
    val foundBy: String,
    val timestamp: String
)
