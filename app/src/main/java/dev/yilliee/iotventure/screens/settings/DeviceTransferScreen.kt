package dev.yilliee.iotventure.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class TransferStatus {
    IDLE, SEARCHING, TRANSFERRING, SUCCESS, ERROR
}

enum class TransferMethod {
    BLUETOOTH, NEARBY_SHARE
}

@Composable
fun DeviceTransferScreen(
    onBackClick: () -> Unit,
    onTransferComplete: () -> Unit
) {
    var transferMethod by remember { mutableStateOf(TransferMethod.BLUETOOTH) }
    var encryptionEnabled by remember { mutableStateOf(true) }
    var transferStatus by remember { mutableStateOf(TransferStatus.IDLE) }
    var statusMessage by remember { mutableStateOf("") }
    var transferProgress by remember { mutableStateOf(0f) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            DeviceTransferTopBar(
                onBackClick = onBackClick,
                isBackEnabled = transferStatus != TransferStatus.TRANSFERRING
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp)
        ) {
            when (transferStatus) {
                TransferStatus.IDLE -> {
                    TransferOptionsContent(
                        transferMethod = transferMethod,
                        onTransferMethodChange = { transferMethod = it },
                        encryptionEnabled = encryptionEnabled,
                        onEncryptionChange = { encryptionEnabled = it },
                        onStartTransfer = {
                            transferStatus = TransferStatus.SEARCHING
                            statusMessage = "Searching for nearby devices..."

                            // Simulate finding devices
                            scope.launch {
                                delay(2000)
                                transferStatus = TransferStatus.TRANSFERRING
                                statusMessage = "Transferring game data..."

                                // Simulate transfer progress
                                repeat(10) {
                                    delay(300)
                                    transferProgress = (it + 1) / 10f
                                }

                                // Simulate transfer completion
                                transferStatus = TransferStatus.SUCCESS
                                statusMessage = "Transfer complete! You can now continue playing on the new device."
                            }
                        }
                    )
                }
                TransferStatus.SEARCHING -> {
                    SearchingContent(statusMessage)
                }
                TransferStatus.TRANSFERRING -> {
                    TransferringContent(
                        statusMessage = statusMessage,
                        progress = transferProgress
                    )
                }
                TransferStatus.SUCCESS -> {
                    SuccessContent(
                        statusMessage = statusMessage,
                        onLogOut = onTransferComplete
                    )
                }
                TransferStatus.ERROR -> {
                    ErrorContent(
                        statusMessage = statusMessage,
                        onRetry = {
                            transferStatus = TransferStatus.IDLE
                            statusMessage = ""
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DeviceTransferTopBar(
    onBackClick: () -> Unit,
    isBackEnabled: Boolean
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
                enabled = isBackEnabled,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = if (isBackEnabled) TextWhite else TextGray
                )
            }

            Text(
                text = "Device Transfer",
                style = MaterialTheme.typography.titleMedium,
                color = TextWhite
            )

            // Empty box for alignment
            Box(modifier = Modifier.size(40.dp))
        }
    }
}

@Composable
fun TransferOptionsContent(
    transferMethod: TransferMethod,
    onTransferMethodChange: (TransferMethod) -> Unit,
    encryptionEnabled: Boolean,
    onEncryptionChange: (Boolean) -> Unit,
    onStartTransfer: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Transfer Method",
            style = MaterialTheme.typography.labelLarge,
            color = TextGray
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Bluetooth Option
        TransferMethodOption(
            icon = Icons.Default.Bluetooth,
            title = "Bluetooth",
            description = "Transfer directly to a nearby device",
            isSelected = transferMethod == TransferMethod.BLUETOOTH,
            onClick = { onTransferMethodChange(TransferMethod.BLUETOOTH) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Nearby Share Option
        TransferMethodOption(
            icon = Icons.Default.PhoneAndroid,
            title = "Nearby Share",
            description = "Use Android's Nearby Share feature",
            isSelected = transferMethod == TransferMethod.NEARBY_SHARE,
            onClick = { onTransferMethodChange(TransferMethod.NEARBY_SHARE) }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Security Options
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
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Security",
                        tint = Gold
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "Security",
                        style = MaterialTheme.typography.titleMedium,
                        color = Gold,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Encrypt Data",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextWhite
                        )

                        Text(
                            text = "Secure your game data during transfer",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextGray
                        )
                    }

                    Switch(
                        checked = encryptionEnabled,
                        onCheckedChange = onEncryptionChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Gold,
                            checkedTrackColor = Gold.copy(alpha = 0.3f),
                            uncheckedThumbColor = TextGray,
                            uncheckedTrackColor = DarkSurfaceLight
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Start Transfer Button
        Button(
            onClick = onStartTransfer,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Gold,
                contentColor = DarkBackground
            )
        ) {
            Text(
                text = "Start Transfer",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Note
        Text(
            text = "Note: After transfer, this device will be logged out and all game data will be deleted.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextGray,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun TransferMethodOption(
    icon: ImageVector,
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = isSelected,
                onClick = onClick
            )
            .then(
                if (isSelected) {
                    Modifier
                        .border(
                            width = 1.dp,
                            color = Gold,
                            shape = MaterialTheme.shapes.medium
                        )
                        .background(
                            color = Gold.copy(alpha = 0.1f),
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
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isSelected) Gold else TextWhite,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isSelected) Gold else TextWhite,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextGray
                )
            }
        }
    }
}

@Composable
fun SearchingContent(statusMessage: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            color = Gold,
            modifier = Modifier.size(48.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = statusMessage,
            style = MaterialTheme.typography.bodyLarge,
            color = TextWhite,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun TransferringContent(
    statusMessage: String,
    progress: Float
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = Gold,
                trackColor = DarkSurfaceLight
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "${(progress * 100).toInt()}%",
            style = MaterialTheme.typography.bodyLarge,
            color = Gold,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = statusMessage,
            style = MaterialTheme.typography.bodyLarge,
            color = TextWhite,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun SuccessContent(
    statusMessage: String,
    onLogOut: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "Success",
            tint = SuccessGreen,
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = statusMessage,
            style = MaterialTheme.typography.bodyLarge,
            color = SuccessGreen,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onLogOut,
            colors = ButtonDefaults.buttonColors(
                containerColor = SuccessGreen,
                contentColor = DarkBackground
            )
        ) {
            Text("Log Out")
        }
    }
}

@Composable
fun ErrorContent(
    statusMessage: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = "Error",
            tint = ErrorRed,
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = statusMessage,
            style = MaterialTheme.typography.bodyLarge,
            color = ErrorRed,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

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
