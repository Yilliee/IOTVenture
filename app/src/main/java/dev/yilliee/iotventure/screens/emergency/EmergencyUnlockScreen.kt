package dev.yilliee.iotventure.screens.emergency

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.yilliee.iotventure.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun EmergencyUnlockScreen(
    onBackClick: () -> Unit,
    onExitGame: () -> Unit,
    onReturnToGame: () -> Unit
) {
    var countdown by remember { mutableIntStateOf(5) }
    var unlocking by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()


    Scaffold(
        topBar = {
            EmergencyTopBar(onBackClick = onBackClick)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Warning Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = ErrorRed.copy(alpha = 0.1f),
                        shape = MaterialTheme.shapes.large
                    )
                    .border(
                        width = 1.dp,
                        color = ErrorRed.copy(alpha = 0.3f),
                        shape = MaterialTheme.shapes.large
                    )
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = ErrorRed,
                        modifier = Modifier.size(64.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Emergency Unlock",
                        style = MaterialTheme.typography.headlineMedium,
                        color = ErrorRed,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "This will unlock the game and allow you to exit. Your progress will be saved, but you may forfeit any ongoing challenges.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextWhite,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Only use this in case of a real emergency.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ErrorRed,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (unlocking) {
                // Countdown
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Unlocking in",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextWhite
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = countdown.toString(),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontSize = 48.sp
                        ),
                        color = ErrorRed,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                // Unlock Button
                Button(
                    onClick = {
                        unlocking = true

                        // Start countdown
                        scope.launch {
                            repeat(5) {
                                delay(1000)
                                countdown--
                            }
                            // Set showDialog to true after countdown
                            showDialog = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ErrorRed,
                        contentColor = DarkBackground
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Unlock",
                        modifier = Modifier.size(24.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "Emergency Unlock",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Cancel Button
            TextButton(
                onClick = onBackClick,
                enabled = !unlocking,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = if (unlocking) TextGray else TextWhite
                )
            ) {
                Text("Cancel")
            }
        }
        if (showDialog) {
            ShowUnlockDialog(
                onExitGame = onExitGame,
                onReturnToGame = onReturnToGame
            )
        }
    }
}

@Composable
fun EmergencyTopBar(
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
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
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
                text = "Emergency Unlock",
                style = MaterialTheme.typography.titleMedium,
                color = TextWhite
            )

            // Empty box for alignment
            Box(modifier = Modifier.size(40.dp))
        }
    }
}

@Composable
private fun ShowUnlockDialog(
    onExitGame: () -> Unit,
    onReturnToGame: () -> Unit
) {
    var showDialog by remember { mutableStateOf(true) }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = {
                Text("Game Unlocked")
            },
            text = {
                Text("Emergency unlock activated. Your game progress has been saved. You can now exit the app.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDialog = false
                        onExitGame()
                    }
                ) {
                    Text("Exit Game", color = ErrorRed)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDialog = false
                        onReturnToGame()
                    }
                ) {
                    Text("Return to Game")
                }
            },
            containerColor = DarkSurface,
            titleContentColor = TextWhite,
            textContentColor = TextWhite
        )
    }
}

