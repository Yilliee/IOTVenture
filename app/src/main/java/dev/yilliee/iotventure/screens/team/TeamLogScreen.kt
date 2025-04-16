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
fun TeamLogScreen(
    onBackClick: () -> Unit
) {
    val teamMembers = remember { getMockTeamMembers() }
    val activityLog = remember { getMockActivityLog() }
    var selectedFilter by remember { mutableStateOf("All") }
    val filters = listOf("All", "Clues Found", "Challenges", "Team Updates")

    Scaffold(
        topBar = {
            TeamLogTopBar(
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

            // Filter Chips
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                filters.forEachIndexed { index, filter ->
                    SegmentedButton(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = filters.size
                        ),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = Gold,
                            activeContentColor = DarkBackground,
                            inactiveContainerColor = DarkSurfaceLight,
                            inactiveContentColor = TextWhite
                        )
                    ) {
                        Text(filter)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Activity Log Title
            Text(
                text = "Activity Log",
                style = MaterialTheme.typography.titleLarge,
                color = Gold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Activity Log List
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val filteredLog = when (selectedFilter) {
                    "Clues Found" -> activityLog.filter { it.type == ActivityType.CLUE_FOUND }
                    "Challenges" -> activityLog.filter { it.type == ActivityType.CHALLENGE }
                    "Team Updates" -> activityLog.filter { it.type == ActivityType.TEAM_UPDATE }
                    else -> activityLog
                }

                items(filteredLog) { activity ->
                    ActivityLogItem(activity = activity)
                }
            }
        }
    }
}

@Composable
fun TeamLogTopBar(
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
                text = "Team Log",
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
fun ActivityLogItem(
    activity: ActivityLog
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
            // Activity Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        when (activity.type) {
                            ActivityType.CLUE_FOUND -> SuccessGreen.copy(alpha = 0.2f)
                            ActivityType.CHALLENGE -> Gold.copy(alpha = 0.2f)
                            ActivityType.TEAM_UPDATE -> DarkSurfaceLight
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (activity.type) {
                        ActivityType.CLUE_FOUND -> Icons.Default.Place
                        ActivityType.CHALLENGE -> Icons.Default.EmojiEvents
                        ActivityType.TEAM_UPDATE -> Icons.Default.Group
                    },
                    contentDescription = null,
                    tint = when (activity.type) {
                        ActivityType.CLUE_FOUND -> SuccessGreen
                        ActivityType.CHALLENGE -> Gold
                        ActivityType.TEAM_UPDATE -> TextWhite
                    },
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = activity.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = when (activity.type) {
                        ActivityType.CLUE_FOUND -> SuccessGreen
                        ActivityType.CHALLENGE -> Gold
                        ActivityType.TEAM_UPDATE -> TextWhite
                    },
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = activity.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextWhite
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = activity.memberName,
                        style = MaterialTheme.typography.bodySmall,
                        color = Gold,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = activity.timestamp,
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

enum class ActivityType {
    CLUE_FOUND, CHALLENGE, TEAM_UPDATE
}

data class ActivityLog(
    val id: Int,
    val title: String,
    val description: String,
    val memberName: String,
    val timestamp: String,
    val type: ActivityType
)

private fun getMockTeamMembers(): List<TeamMember> {
    return listOf(
        TeamMember(id = 1, name = "Alex", cluesFound = 3),
        TeamMember(id = 2, name = "Sarah", cluesFound = 2),
        TeamMember(id = 3, name = "You", cluesFound = 1),
        TeamMember(id = 4, name = "Mike", cluesFound = 1)
    )
}

private fun getMockActivityLog(): List<ActivityLog> {
    val dateFormat = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
    val currentDate = Date()
    val calendar = Calendar.getInstance()
    
    // Current time minus 10 minutes
    calendar.time = currentDate
    calendar.add(Calendar.MINUTE, -10)
    val time1 = dateFormat.format(calendar.time)
    
    // Current time minus 30 minutes
    calendar.time = currentDate
    calendar.add(Calendar.MINUTE, -30)
    val time2 = dateFormat.format(calendar.time)
    
    // Current time minus 1 hour
    calendar.time = currentDate
    calendar.add(Calendar.HOUR, -1)
    val time3 = dateFormat.format(calendar.time)
    
    // Current time minus 2 hours
    calendar.time = currentDate
    calendar.add(Calendar.HOUR, -2)
    val time4 = dateFormat.format(calendar.time)
    
    // Current time minus 3 hours
    calendar.time = currentDate
    calendar.add(Calendar.HOUR, -3)
    val time5 = dateFormat.format(calendar.time)
    
    return listOf(
        ActivityLog(
            id = 1,
            title = "Clue #3 Found",
            description = "The Clock Tower clue has been solved!",
            memberName = "Alex",
            timestamp = time1,
            type = ActivityType.CLUE_FOUND
        ),
        ActivityLog(
            id = 2,
            title = "Challenge Completed",
            description = "Decoded the ancient cipher in record time.",
            memberName = "Sarah",
            timestamp = time2,
            type = ActivityType.CHALLENGE
        ),
        ActivityLog(
            id = 3,
            title = "Team Rank Updated",
            description = "Team moved up to 3rd place on the leaderboard.",
            memberName = "System",
            timestamp = time3,
            type = ActivityType.TEAM_UPDATE
        ),
        ActivityLog(
            id = 4,
            title = "Clue #2 Found",
            description = "The Ancient Library clue has been solved!",
            memberName = "Mike",
            timestamp = time4,
            type = ActivityType.CLUE_FOUND
        ),
        ActivityLog(
            id = 5,
            title = "Clue #1 Found",
            description = "The Starting Point clue has been solved!",
            memberName = "You",
            timestamp = time5,
            type = ActivityType.CLUE_FOUND
        )
    )
}
