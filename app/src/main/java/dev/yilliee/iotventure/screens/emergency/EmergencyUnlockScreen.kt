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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import dev.yilliee.iotventure.di.ServiceLocator
import dev.yilliee.iotventure.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun EmergencyUnlockScreen(
    onBackClick: () -> Unit,
    onExitGame: () -> Unit,
    onUnlockComplete: () -> Unit = {}
) {
    var countdown by remember { mutableIntStateOf(5) }
    var unlocking by remember { mutableStateOf(false) }
    var locking by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Get repositories
    val context = LocalContext.current
    val gameRepository = remember { ServiceLocator.provideGameRepository(context) }
    var isLoggingOut by remember { mutableStateOf(false) }

    // Observe emergency lock state
    val isEmergencyLocked by gameRepository.isEmergencyLocked.collectAsState()

    // Handle back press
    BackHandler(enabled = isEmergencyLocked) {
        // Do nothing when emergency lock is active
    }

    Scaffold(
        topBar = {
            if (!isEmergencyLocked) {
                EmergencyTopBar(onBackClick = onUnlockComplete) // Navigate to dashboard when back is pressed in unlocked state
            }
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
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = ErrorRed,
                        modifier = Modifier.size(48.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = if (isEmergencyLocked) "Emergency Lock Active" else "Emergency Lock",
                        style = MaterialTheme.typography.titleLarge,
                        color = ErrorRed,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (isEmergencyLocked) 
                            "Your game progress has been saved. You can now safely exit the app."
                        else 
                            "Activate emergency lock to prevent data loss when minimizing or closing the app.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextWhite,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (isEmergencyLocked) {
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
                                // Clear emergency lock after countdown
                                isLoggingOut = true
                                try {
                                    gameRepository.clearEmergencyLock()
                                    // Navigate to dashboard after successful unlock
                                    onUnlockComplete()
                                } finally {
                                    isLoggingOut = false
                                    unlocking = false
                                    countdown = 5
                                }
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
            } else {
                if (locking) {
                    // Lock countdown
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Activating lock in",
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
                    // Activate Lock Button
                    Button(
                        onClick = {
                            locking = true
                            countdown = 5

                            // Start countdown
                            scope.launch {
                                repeat(5) {
                                    delay(1000)
                                    countdown--
                                }
                                // Activate emergency lock after countdown
                                isLoggingOut = true
                                try {
                                    gameRepository.emergencyLock()
                                } finally {
                                    isLoggingOut = false
                                    locking = false
                                    countdown = 5
                                }
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
                            contentDescription = "Lock",
                            modifier = Modifier.size(24.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = "Activate Emergency Lock",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyTopBar(
    onBackClick: () -> Unit
) {
    TopAppBar(
        title = { Text("Emergency Lock") },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back"
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = DarkSurface,
            titleContentColor = TextWhite,
            navigationIconContentColor = TextWhite
        )
    )
}
