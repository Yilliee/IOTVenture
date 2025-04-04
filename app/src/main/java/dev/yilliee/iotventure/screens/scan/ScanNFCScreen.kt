package dev.yilliee.iotventure.screens.scan

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.yilliee.iotventure.ui.theme.*
import kotlinx.coroutines.delay

enum class ScanStatus {
    SCANNING, SUCCESS, ERROR
}

@Composable
fun ScanNfcScreen(
    onBackClick: () -> Unit,
    onScanComplete: () -> Unit
) {
    var scanStatus by remember { mutableStateOf(ScanStatus.SCANNING) }
    var statusMessage by remember { mutableStateOf("Scanning for NFC token...") }

    // Simulate a successful scan after 5 seconds
    LaunchedEffect(key1 = Unit) {
        delay(5000)
        scanStatus = ScanStatus.SUCCESS
        statusMessage = "Token found! Clue unlocked."

        // Navigate back after success
        delay(2000)
        onScanComplete()
    }

    Scaffold(
        topBar = {
            ScanTopBar(onBackClick = onBackClick)
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            when (scanStatus) {
                ScanStatus.SCANNING -> ScanningContent(statusMessage)
                ScanStatus.SUCCESS -> SuccessContent(statusMessage)
                ScanStatus.ERROR -> ErrorContent(
                    statusMessage = statusMessage,
                    onRetry = {
                        scanStatus = ScanStatus.SCANNING
                        statusMessage = "Scanning for NFC token..."
                    }
                )
            }
        }
    }
}

@Composable
fun ScanTopBar(
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
                text = "Scan NFC Token",
                style = MaterialTheme.typography.titleMedium,
                color = TextWhite
            )

            // Empty box for alignment
            Box(modifier = Modifier.size(40.dp))
        }
    }
}

@Composable
fun ScanningContent(statusMessage: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "scanAnimation")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scaleAnimation"
    )

    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alphaAnimation"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            // Animated circle
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .scale(scale)
                    .alpha(alpha)
                    .clip(CircleShape)
                    .background(Gold.copy(alpha = 0.2f))
            )

            // Icon container
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(Gold.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = "Scan",
                    tint = Gold,
                    modifier = Modifier.size(64.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = statusMessage,
            style = MaterialTheme.typography.titleMedium,
            color = Gold,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Hold your device near the NFC token",
            style = MaterialTheme.typography.bodyMedium,
            color = TextGray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
    }
}

@Composable
fun SuccessContent(statusMessage: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "Success",
            tint = SuccessGreen,
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = statusMessage,
            style = MaterialTheme.typography.titleMedium,
            color = SuccessGreen,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ErrorContent(
    statusMessage: String,
    onRetry: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = "Error",
            tint = ErrorRed,
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = statusMessage,
            style = MaterialTheme.typography.titleMedium,
            color = ErrorRed,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = DarkSurfaceLight,
                contentColor = TextWhite
            )
        ) {
            Text("Try Again")
        }
    }
}

