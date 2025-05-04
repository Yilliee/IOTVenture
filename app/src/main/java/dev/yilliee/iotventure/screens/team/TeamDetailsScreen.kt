package dev.yilliee.iotventure.screens.team

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.yilliee.iotventure.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TeamDetailsScreen(
    onBackClick: () -> Unit
) {
    val teamMembers = remember { getMockTeamMembers() }
    val clueFindings = remember { getMockClueFindings() }

    Scaffold(
        topBar = {
            TeamDetailsTopBar(
                onBackClick = onBackClick
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Team Members Section
            Text(
                text = "Team Members",
                style = MaterialTheme.typography.titleLarge,
                color = Gold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Team Members Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                teamMembers.forEach { member ->
                    TeamMemberAvatar(member = member)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Clue Findings Title
            Text(
                text = "Clue Findings",
                style = MaterialTheme.typography.titleLarge,
                color = Gold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Clue Findings List
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(clueFindings) { finding ->
                    ClueFoundItem(finding = finding)
                }
            }
        }
    }
}

@Composable
fun TeamDetailsTopBar(
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
                text = "Team Details",
                style = MaterialTheme.typography.titleLarge,
                color = TextWhite
            )
        }
    }
}

@Composable
fun TeamMemberAvatar(
    member: TeamMember
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(72.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(DarkSurfaceLight),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = member.name.first().toString(),
                style = MaterialTheme.typography.headlineSmall,
                color = Gold,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = member.name,
            style = MaterialTheme.typography.bodySmall,
            color = TextWhite,
            textAlign = TextAlign.Center,
            maxLines = 1
        )

        Text(
            text = "${member.cluesFound} clues",
            style = MaterialTheme.typography.labelSmall,
            color = TextGray,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ClueFoundItem(
    finding: ClueFinding
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
            // Clue Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(SuccessGreen.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = null,
                    tint = SuccessGreen,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Clue #${finding.clueNumber}: ${finding.clueTitle}",
                    style = MaterialTheme.typography.titleMedium,
                    color = SuccessGreen,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Found by ${finding.foundBy}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Gold,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = finding.timestamp,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextGray
                    )
                }
            }
        }
    }
}

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

private fun getMockTeamMembers(): List<TeamMember> {
    return listOf(
        TeamMember(id = 1, name = "Alex", cluesFound = 3),
        TeamMember(id = 2, name = "Sarah", cluesFound = 2),
        TeamMember(id = 3, name = "You", cluesFound = 1),
        TeamMember(id = 4, name = "Mike", cluesFound = 1)
    )
}

private fun getMockClueFindings(): List<ClueFinding> {
    val dateFormat = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
    val calendar = Calendar.getInstance()

    // Current time minus 10 minutes
    calendar.time = Date()
    calendar.add(Calendar.MINUTE, -10)
    val time1 = dateFormat.format(calendar.time)

    // Current time minus 30 minutes
    calendar.time = Date()
    calendar.add(Calendar.MINUTE, -30)
    val time2 = dateFormat.format(calendar.time)

    // Current time minus 1 hour
    calendar.time = Date()
    calendar.add(Calendar.HOUR, -1)
    val time3 = dateFormat.format(calendar.time)

    // Current time minus 2 hours
    calendar.time = Date()
    calendar.add(Calendar.HOUR, -2)
    val time4 = dateFormat.format(calendar.time)

    // Current time minus 3 hours
    calendar.time = Date()
    calendar.add(Calendar.HOUR, -3)
    val time5 = dateFormat.format(calendar.time)

    return listOf(
        ClueFinding(
            id = 1,
            clueNumber = 3,
            clueTitle = "The Clock Tower",
            foundBy = "Alex",
            timestamp = time1
        ),
        ClueFinding(
            id = 2,
            clueNumber = 2,
            clueTitle = "The Ancient Library",
            foundBy = "Mike",
            timestamp = time4
        ),
        ClueFinding(
            id = 3,
            clueNumber = 1,
            clueTitle = "The Starting Point",
            foundBy = "You",
            timestamp = time5
        )
    )
}
