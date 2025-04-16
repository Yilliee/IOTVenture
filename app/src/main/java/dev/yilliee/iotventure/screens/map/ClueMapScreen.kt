package dev.yilliee.iotventure.screens.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.yilliee.iotventure.ui.theme.*

@Composable
fun ClueMapScreen(
    onBackClick: () -> Unit
) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var selectedClue by remember { mutableStateOf<ClueMapPoint?>(null) }
    val cluePoints = remember { getMockCluePoints() }

    Scaffold(
        topBar = {
            MapTopBar(
                onBackClick = onBackClick
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Map with zoom and pan
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DarkBackground)
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(0.5f, 3f)
                            val newOffset = offset + pan
                            offset = newOffset
                        }
                    }
            ) {
                // Map background
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y
                        )
                ) {
                    // Draw grid lines
                    val gridSize = 50f
                    val strokeWidth = 1f
                    
                    // Vertical lines
                    for (x in 0..(size.width.toInt() / gridSize.toInt())) {
                        val xPos = x * gridSize
                        drawLine(
                            color = Color.DarkGray.copy(alpha = 0.3f),
                            start = Offset(xPos, 0f),
                            end = Offset(xPos, size.height),
                            strokeWidth = strokeWidth
                        )
                    }
                    
                    // Horizontal lines
                    for (y in 0..(size.height.toInt() / gridSize.toInt())) {
                        val yPos = y * gridSize
                        drawLine(
                            color = Color.DarkGray.copy(alpha = 0.3f),
                            start = Offset(0f, yPos),
                            end = Offset(size.width, yPos),
                            strokeWidth = strokeWidth
                        )
                    }

                    // Draw paths between clues
                    val completedClues = cluePoints.filter { it.isFound }
                    if (completedClues.size > 1) {
                        for (i in 0 until completedClues.size - 1) {
                            drawLine(
                                color = Gold.copy(alpha = 0.6f),
                                start = Offset(completedClues[i].x, completedClues[i].y),
                                end = Offset(completedClues[i + 1].x, completedClues[i + 1].y),
                                strokeWidth = 5f
                            )
                        }
                    }

                    // Draw current path if there's a next clue
                    val lastCompletedClue = completedClues.lastOrNull()
                    val nextClue = cluePoints.find { !it.isFound }
                    if (lastCompletedClue != null && nextClue != null) {
                        drawLine(
                            color = Gold.copy(alpha = 0.3f),
                            start = Offset(lastCompletedClue.x, lastCompletedClue.y),
                            end = Offset(nextClue.x, nextClue.y),
                            strokeWidth = 5f,
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                                floatArrayOf(20f, 10f), 0f
                            )
                        )
                    }

                    // Draw clue points
                    cluePoints.forEach { clue ->
                        // Draw circle for each clue point
                        drawCircle(
                            color = if (clue.isFound) SuccessGreen else Gold,
                            radius = if (clue.isFound) 15f else 20f,
                            center = Offset(clue.x, clue.y),
                            style = if (clue.isFound) Stroke(width = 4f) else Stroke(width = 2f)
                        )
                        
                        // Draw filled circle for current clue
                        if (clue.isCurrent) {
                            drawCircle(
                                color = Gold,
                                radius = 10f,
                                center = Offset(clue.x, clue.y)
                            )
                        }
                    }
                }

                // Clue markers (interactive elements)
                cluePoints.forEach { clue ->
                    Box(
                        modifier = Modifier
                            .offset(
                                x = ((clue.x * scale + offset.x) - 20).dp,
                                y = ((clue.y * scale + offset.y) - 20).dp
                            )
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.Transparent)
                            .clickable { selectedClue = clue }
                    )
                }
            }

            // Map controls
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                FloatingActionButton(
                    onClick = { scale = (scale * 1.2f).coerceIn(0.5f, 3f) },
                    containerColor = DarkSurface,
                    contentColor = Gold,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Zoom In",
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                FloatingActionButton(
                    onClick = { scale = (scale / 1.2f).coerceIn(0.5f, 3f) },
                    containerColor = DarkSurface,
                    contentColor = Gold,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Zoom Out",
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                FloatingActionButton(
                    onClick = { 
                        scale = 1f
                        offset = Offset.Zero
                    },
                    containerColor = DarkSurface,
                    contentColor = Gold,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset View",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Clue info card
            selectedClue?.let { clue ->
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(16.dp),
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
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(if (clue.isFound) SuccessGreen else Gold),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = clue.id.toString(),
                                    color = DarkBackground,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = clue.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (clue.isFound) SuccessGreen else Gold,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    text = if (clue.isFound) "Found" else if (clue.isCurrent) "Current" else "Locked",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextGray
                                )
                            }

                            IconButton(
                                onClick = { selectedClue = null }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = TextGray
                                )
                            }
                        }

                        if (clue.isFound || clue.isCurrent) {
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = clue.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextWhite
                            )
                            
                            if (clue.isCurrent) {
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            color = Gold.copy(alpha = 0.1f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = Gold,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = "Hint: ${clue.hint}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Gold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MapTopBar(
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
                text = "Clue Map",
                style = MaterialTheme.typography.titleLarge,
                color = TextWhite
            )
        }
    }
}

data class ClueMapPoint(
    val id: Int,
    val title: String,
    val description: String,
    val hint: String,
    val x: Float,
    val y: Float,
    val isFound: Boolean,
    val isCurrent: Boolean
)

private fun getMockCluePoints(): List<ClueMapPoint> {
    return listOf(
        ClueMapPoint(
            id = 1,
            title = "Starting Point",
            description = "The adventure begins at the campus entrance.",
            hint = "Look for the large stone sign.",
            x = 150f,
            y = 200f,
            isFound = true,
            isCurrent = false
        ),
        ClueMapPoint(
            id = 2,
            title = "The Ancient Library",
            description = "Find the oldest book in the library's special collection.",
            hint = "Check the glass display case on the second floor.",
            x = 250f,
            y = 300f,
            isFound = true,
            isCurrent = false
        ),
        ClueMapPoint(
            id = 3,
            title = "The Clock Tower",
            description = "Discover the secret behind the clock tower's unusual chimes.",
            hint = "Count the number of chimes at noon.",
            x = 400f,
            y = 250f,
            isFound = true,
            isCurrent = false
        ),
        ClueMapPoint(
            id = 4,
            title = "The Hidden Garden",
            description = "Find the rare flower that blooms only at midnight.",
            hint = "Look for a garden entrance behind the science building.",
            x = 500f,
            y = 350f,
            isFound = false,
            isCurrent = true
        ),
        ClueMapPoint(
            id = 5,
            title = "The Professor's Office",
            description = "Locate the professor's secret research notes.",
            hint = "The office is on the top floor of the mathematics building.",
            x = 600f,
            y = 200f,
            isFound = false,
            isCurrent = false
        ),
        ClueMapPoint(
            id = 6,
            title = "The Underground Tunnel",
            description = "Navigate the forgotten tunnels beneath the campus.",
            hint = "The entrance is hidden in the oldest dormitory's basement.",
            x = 700f,
            y = 400f,
            isFound = false,
            isCurrent = false
        ),
        ClueMapPoint(
            id = 7,
            title = "The Final Secret",
            description = "Uncover the ultimate mystery of the treasure hunt.",
            hint = "Return to where it all began, but look up instead of forward.",
            x = 800f,
            y = 300f,
            isFound = false,
            isCurrent = false
        )
    )
}
