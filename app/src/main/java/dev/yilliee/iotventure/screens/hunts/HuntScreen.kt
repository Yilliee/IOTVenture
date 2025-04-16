package dev.yilliee.iotventure.screens.hunts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.yilliee.iotventure.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HuntsScreen(
    onHuntClick: (String) -> Unit,
    onLogoutClick: () -> Unit
) {
    var hunts by remember { mutableStateOf(getMockHunts()) }

    Scaffold(
        topBar = {
            HuntsTopBar(onLogoutClick = onLogoutClick)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Current Hunt Section
            Text(
                text = "Current Hunt",
                style = MaterialTheme.typography.titleLarge,
                color = Gold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Current Hunt Card
            val currentHunt = hunts.first { it.isCurrent }
            CurrentHuntCard(
                hunt = currentHunt,
                onClick = { onHuntClick("current") }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Past Hunts Section
            Text(
                text = "Past Hunts",
                style = MaterialTheme.typography.titleLarge,
                color = Gold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Past Hunts List
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(hunts.filter { !it.isCurrent }) { hunt ->
                    PastHuntCard(
                        hunt = hunt,
                        onClick = { onHuntClick(hunt.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun HuntsTopBar(
    onLogoutClick: () -> Unit
) {
    Surface(
        color = DarkSurface,
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Treasure Hunts",
                style = MaterialTheme.typography.titleLarge,
                color = TextWhite
            )

            IconButton(
                onClick = onLogoutClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Logout,
                    contentDescription = "Logout",
                    tint = TextWhite
                )
            }
        }
    }
}

@Composable
fun CurrentHuntCard(
    hunt: HuntData,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 2.dp,
                color = Gold,
                shape = MaterialTheme.shapes.medium
            ),
        colors = CardDefaults.cardColors(
            containerColor = DarkSurface
        ),
        shape = MaterialTheme.shapes.medium,
        onClick = onClick
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
                        .background(Gold),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = DarkBackground,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = hunt.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = Gold,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Started: ${hunt.startDate}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextGray
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(
                    icon = Icons.Default.Place,
                    value = "${hunt.cluesFound}/${hunt.totalClues}",
                    label = "Clues Found"
                )

                StatItem(
                    icon = Icons.Default.EmojiEvents,
                    value = "#${hunt.currentRank}",
                    label = "Current Rank"
                )

                StatItem(
                    icon = Icons.Default.Timer,
                    value = hunt.timeElapsed,
                    label = "Time"
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Gold,
                    contentColor = DarkBackground
                )
            ) {
                Text("Continue Hunt")
            }
        }
    }
}

@Composable
fun PastHuntCard(
    hunt: HuntData,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = DarkSurface
        ),
        shape = MaterialTheme.shapes.medium,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(DarkSurfaceLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = TextWhite,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = hunt.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextWhite
                )

                Text(
                    text = "Completed: ${hunt.endDate}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextGray
                )
            }

            Column(
                horizontalAlignment = Alignment.End
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = when (hunt.finalRank) {
                            1 -> Gold
                            2 -> SilverMedal
                            3 -> BronzeMedal
                            else -> TextGray
                        },
                        modifier = Modifier.size(16.dp)
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        text = "#${hunt.finalRank}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = when (hunt.finalRank) {
                            1 -> Gold
                            2 -> SilverMedal
                            3 -> BronzeMedal
                            else -> TextWhite
                        },
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "${hunt.cluesFound}/${hunt.totalClues} clues",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextGray
                )
            }
        }
    }
}

@Composable
fun StatItem(
    icon: ImageVector,
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

data class HuntData(
    val id: String,
    val name: String,
    val startDate: String,
    val endDate: String? = null,
    val cluesFound: Int,
    val totalClues: Int,
    val currentRank: Int,
    val finalRank: Int = 0,
    val timeElapsed: String,
    val isCurrent: Boolean
)

private fun getMockHunts(): List<HuntData> {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    
    return listOf(
        HuntData(
            id = "current",
            name = "City Explorer Challenge",
            startDate = dateFormat.format(Date()),
            cluesFound = 7,
            totalClues = 20,
            currentRank = 3,
            timeElapsed = "01:45:22",
            isCurrent = true
        ),
        HuntData(
            id = "1",
            name = "Campus Mystery Hunt",
            startDate = "Oct 15, 2023",
            endDate = "Oct 17, 2023",
            cluesFound = 18,
            totalClues = 20,
            currentRank = 0,
            finalRank = 1,
            timeElapsed = "05:30:45",
            isCurrent = false
        ),
        HuntData(
            id = "2",
            name = "Downtown Treasure Trail",
            startDate = "Aug 5, 2023",
            endDate = "Aug 6, 2023",
            cluesFound = 12,
            totalClues = 15,
            currentRank = 0,
            finalRank = 2,
            timeElapsed = "04:15:30",
            isCurrent = false
        ),
        HuntData(
            id = "3",
            name = "Historical Landmarks Quest",
            startDate = "Jun 20, 2023",
            endDate = "Jun 22, 2023",
            cluesFound = 8,
            totalClues = 10,
            currentRank = 0,
            finalRank = 5,
            timeElapsed = "03:45:10",
            isCurrent = false
        )
    )
}
