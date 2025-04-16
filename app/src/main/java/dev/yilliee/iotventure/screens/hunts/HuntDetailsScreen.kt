package dev.yilliee.iotventure.screens.hunts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.yilliee.iotventure.screens.leaderboard.TeamData
import dev.yilliee.iotventure.ui.theme.*

@Composable
fun HuntDetailsScreen(
    huntId: String,
    onBackClick: () -> Unit
) {
    val hunt = remember { getMockHuntDetails(huntId) }
    val teams = remember { getMockTeams() }

    Scaffold(
        topBar = {
            HuntDetailsTopBar(
                title = hunt.name,
                onBackClick = onBackClick
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hunt Summary
            item {
                HuntSummaryCard(hunt = hunt)
            }

            // Hunt Stats
            item {
                HuntStatsCard(hunt = hunt)
            }

            // Leaderboard Title
            item {
                Text(
                    text = "Leaderboard",
                    style = MaterialTheme.typography.titleLarge,
                    color = Gold,
                    modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                )
            }

            // Leaderboard
            items(teams) { team ->
                TeamItem(team = team)
            }
        }
    }
}

@Composable
fun HuntDetailsTopBar(
    title: String,
    onBackClick: () -> Unit
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

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = TextWhite
            )
        }
    }
}

@Composable
fun HuntSummaryCard(hunt: HuntDetailData) {
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
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (hunt.isCompleted) DarkSurfaceLight else Gold),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (hunt.isCompleted) Icons.Default.CheckCircle else Icons.Default.Star,
                        contentDescription = null,
                        tint = if (hunt.isCompleted) TextWhite else DarkBackground,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = hunt.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = if (hunt.isCompleted) TextWhite else Gold,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = if (hunt.isCompleted) "Completed: ${hunt.endDate}" else "Started: ${hunt.startDate}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextGray
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = hunt.description,
                style = MaterialTheme.typography.bodyLarge,
                color = TextWhite
            )
        }
    }
}

@Composable
fun HuntStatsCard(hunt: HuntDetailData) {
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
                text = "Hunt Statistics",
                style = MaterialTheme.typography.titleMedium,
                color = Gold,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatColumn(
                    icon = Icons.Default.Place,
                    value = "${hunt.cluesFound}/${hunt.totalClues}",
                    label = "Clues Found"
                )

                StatColumn(
                    icon = Icons.Default.EmojiEvents,
                    value = "#${if (hunt.isCompleted) hunt.finalRank else hunt.currentRank}",
                    label = if (hunt.isCompleted) "Final Rank" else "Current Rank"
                )

                StatColumn(
                    icon = Icons.Default.Timer,
                    value = hunt.timeElapsed,
                    label = "Time"
                )
            }

            if (hunt.isCompleted) {
                Spacer(modifier = Modifier.height(16.dp))
                
                // Progress bar
                LinearProgressIndicator(
                    progress = { hunt.cluesFound.toFloat() / hunt.totalClues },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = Gold,
                    trackColor = DarkSurfaceLight
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "${(hunt.cluesFound.toFloat() / hunt.totalClues * 100).toInt()}% completed",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun StatColumn(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Gold,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = TextWhite,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextGray,
            textAlign = TextAlign.Center
        )
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

data class HuntDetailData(
    val id: String,
    val name: String,
    val description: String,
    val startDate: String,
    val endDate: String? = null,
    val cluesFound: Int,
    val totalClues: Int,
    val currentRank: Int,
    val finalRank: Int = 0,
    val timeElapsed: String,
    val isCompleted: Boolean
)

private fun getMockHuntDetails(huntId: String): HuntDetailData {
    return when (huntId) {
        "1" -> HuntDetailData(
            id = "1",
            name = "Campus Mystery Hunt",
            description = "A thrilling adventure across the university campus, uncovering hidden secrets and solving puzzles left by the mysterious Professor X.",
            startDate = "Oct 15, 2023",
            endDate = "Oct 17, 2023",
            cluesFound = 18,
            totalClues = 20,
            currentRank = 0,
            finalRank = 1,
            timeElapsed = "05:30:45",
            isCompleted = true
        )
        "2" -> HuntDetailData(
            id = "2",
            name = "Downtown Treasure Trail",
            description = "Explore the historic downtown area and discover the city's hidden treasures and forgotten stories.",
            startDate = "Aug 5, 2023",
            endDate = "Aug 6, 2023",
            cluesFound = 12,
            totalClues = 15,
            currentRank = 0,
            finalRank = 2,
            timeElapsed = "04:15:30",
            isCompleted = true
        )
        "3" -> HuntDetailData(
            id = "3",
            name = "Historical Landmarks Quest",
            description = "A journey through time as you visit and learn about the most significant historical landmarks in the region.",
            startDate = "Jun 20, 2023",
            endDate = "Jun 22, 2023",
            cluesFound = 8,
            totalClues = 10,
            currentRank = 0,
            finalRank = 5,
            timeElapsed = "03:45:10",
            isCompleted = true
        )
        else -> HuntDetailData(
            id = "current",
            name = "City Explorer Challenge",
            description = "Navigate through the urban jungle, solving puzzles and finding hidden spots that even locals might not know about.",
            startDate = "Nov 1, 2023",
            cluesFound = 7,
            totalClues = 20,
            currentRank = 3,
            timeElapsed = "01:45:22",
            isCompleted = false
        )
    }
}

private fun getMockTeams(): List<TeamData> {
    return listOf(
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
}
