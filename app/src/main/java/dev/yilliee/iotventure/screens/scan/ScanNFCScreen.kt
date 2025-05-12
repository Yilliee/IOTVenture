package dev.yilliee.iotventure.screens.scan

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.util.Log
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.yilliee.iotventure.ui.theme.*
import kotlinx.coroutines.launch
import dev.yilliee.iotventure.MainActivity
import dev.yilliee.iotventure.data.model.Challenge
import dev.yilliee.iotventure.data.model.NfcValidationResult
import dev.yilliee.iotventure.di.ServiceLocator

enum class ScanStatus {
    SCANNING, SUCCESS, ERROR, ALREADY_SOLVED
}

// Change the NfcResultDialog function to not automatically close the screen after dismissal
@Composable
fun NfcResultDialog(
    status: ScanStatus,
    message: String,
    challenge: Challenge?,
    onDismiss: () -> Unit,
    onScanComplete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = when(status) {
                    ScanStatus.SUCCESS -> "Challenge Found!"
                    ScanStatus.ALREADY_SOLVED -> "Already Solved"
                    else -> "Scan Result"
                },
                color = when(status) {
                    ScanStatus.SUCCESS -> SuccessGreen
                    ScanStatus.ALREADY_SOLVED -> Gold
                    else -> ErrorRed
                },
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextWhite
                )

                if (challenge != null && status == ScanStatus.SUCCESS) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Points: ${challenge.points}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Gold,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onDismiss()
                    // Call onScanComplete directly here instead of relying on shouldNavigateBack
                    onScanComplete()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = when(status) {
                        ScanStatus.SUCCESS -> SuccessGreen
                        ScanStatus.ALREADY_SOLVED -> Gold
                        else -> DarkSurfaceLight
                    },
                    contentColor = when(status) {
                        ScanStatus.SUCCESS, ScanStatus.ALREADY_SOLVED -> DarkBackground
                        else -> TextWhite
                    }
                )
            ) {
                Text("Continue")
            }
        },
        containerColor = DarkSurface,
        titleContentColor = when(status) {
            ScanStatus.SUCCESS -> SuccessGreen
            ScanStatus.ALREADY_SOLVED -> Gold
            else -> ErrorRed
        },
        textContentColor = TextWhite
    )
}

// Modify the ScanNfcScreen function to properly reset state after scanning
@Composable
fun ScanNfcScreen(
    onBackClick: () -> Unit,
    onScanComplete: () -> Unit
) {
    val context = LocalContext.current
    val gameRepository = remember { ServiceLocator.provideGameRepository(context) }
    val scope = rememberCoroutineScope()

    // Get MainActivity instance to set NFC scan flag
    val activity = remember { context.findActivity() as? MainActivity }

    var scanStatus by remember { mutableStateOf(ScanStatus.SCANNING) }
    var statusMessage by remember { mutableStateOf("Scanning for NFC token...") }
    var nfcContent by remember { mutableStateOf("") }
    var validatedChallenge by remember { mutableStateOf<Challenge?>(null) }
    var showResultDialog by remember { mutableStateOf(false) }

    // NFC Adapter
    val nfcAdapter = remember { NfcAdapter.getDefaultAdapter(context) }
    val isNfcAvailable = remember { nfcAdapter != null }
    val isNfcEnabled = remember { nfcAdapter?.isEnabled ?: false }

    // Set NFC scan flag when entering and leaving this screen
    LaunchedEffect(Unit) {
        activity?.setFromNfcScan(true)
    }

    DisposableEffect(Unit) {
        onDispose {
            // Reset NFC scan flag when leaving the screen
            activity?.setFromNfcScan(false)
        }
    }

    // Check NFC availability
    LaunchedEffect(Unit) {
        if (!isNfcAvailable) {
            scanStatus = ScanStatus.ERROR
            statusMessage = "NFC is not available on this device"
        } else if (!isNfcEnabled) {
            scanStatus = ScanStatus.ERROR
            statusMessage = "NFC is disabled. Please enable it in your device settings"
        }

        // Clear any previous validation result when screen is shown
        gameRepository.clearNfcValidationResult()
    }

    // Observe NFC validation results from the repository
    LaunchedEffect(Unit) {
        try {
            gameRepository.nfcValidationResult.collect { result ->
                result?.let {
                    when (result) {
                        is NfcValidationResult.Valid -> {
                            scanStatus = ScanStatus.SUCCESS
                            statusMessage = "Challenge found! ${result.challenge.name}"
                            validatedChallenge = result.challenge
                            showResultDialog = true

                            // Add to solve queue and submit to server
                            try {
                                gameRepository.addToSolveQueue(result.challenge)
                            } catch (e: Exception) {
                                Log.e("ScanNFCScreen", "Error adding challenge to solve queue", e)
                                // Don't crash the app, just log the error
                            }
                        }
                        is NfcValidationResult.Invalid -> {
                            scanStatus = ScanStatus.ERROR
                            statusMessage = "Invalid NFC token. This doesn't match any challenge."
                            showResultDialog = true
                        }
                        is NfcValidationResult.AlreadySolved -> {
                            scanStatus = ScanStatus.ALREADY_SOLVED
                            statusMessage = "This challenge has already been solved!"
                            showResultDialog = true
                        }
                        is NfcValidationResult.Processing -> {
                            scanStatus = ScanStatus.SCANNING
                            statusMessage = "Processing NFC tag..."
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ScanNFCScreen", "Error collecting NFC validation results", e)
        }
    }

    // NFC Intent handling
    DisposableEffect(Unit) {
        if (isNfcAvailable && isNfcEnabled) {
            val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.getActivity(
                    context,
                    0,
                    Intent(context, context.javaClass).apply {
                        addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    },
                    PendingIntent.FLAG_MUTABLE
                )
            } else {
                PendingIntent.getActivity(
                    context,
                    0,
                    Intent(context, context.javaClass).apply {
                        addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
            }

            val intentFilters = arrayOf(
                IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED).apply {
                    try {
                        addDataType("*/*")
                    } catch (e: IntentFilter.MalformedMimeTypeException) {
                        // Handle exception
                    }
                },
                IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED),
                IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED)
            )

            // Enable NFC foreground dispatch only when on this screen
            try {
                nfcAdapter?.enableForegroundDispatch(
                    context.findActivity(),
                    pendingIntent,
                    intentFilters,
                    null
                )
            } catch (e: Exception) {
                Log.e("ScanNFCScreen", "Error enabling NFC foreground dispatch", e)
            }

            onDispose {
                // Disable NFC foreground dispatch when leaving this screen
                try {
                    nfcAdapter?.disableForegroundDispatch(context.findActivity())
                } catch (e: Exception) {
                    Log.e("ScanNFCScreen", "Error disabling NFC foreground dispatch", e)
                }
                // Clear validation result
                gameRepository.clearNfcValidationResult()
            }
        }

        onDispose { }
    }

    // Observe the NFC intent state from MainActivity
    val currentNfcIntent = MainActivity.nfcIntent.value

    // Process NFC intent when it changes
    LaunchedEffect(currentNfcIntent) {
        currentNfcIntent?.let { intent ->
            // Only process if we're in scanning state
            if (scanStatus == ScanStatus.SCANNING) {
                try {
                    val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
                    tag?.let {
                        try {
                            // Always use the tag ID as the primary identifier
                            val id = tag.id
                            if (id != null && id.isNotEmpty()) {
                                val hexId = id.joinToString("") { "%02X".format(it) }
                                nfcContent = hexId

                                // Validate the NFC tag ID against challenges
                                scope.launch {
                                    try {
                                        gameRepository.validateNfcTag(hexId)
                                    } catch (e: Exception) {
                                        Log.e("ScanNFCScreen", "Error validating NFC tag", e)
                                        scanStatus = ScanStatus.ERROR
                                        statusMessage = "Error validating tag: ${e.message ?: "Unknown error"}"
                                        showResultDialog = true
                                    }
                                }
                            } else {
                                scanStatus = ScanStatus.ERROR
                                statusMessage = "Could not read NFC tag ID"
                                showResultDialog = true
                            }
                        } catch (e: Exception) {
                            Log.e("ScanNFCScreen", "Error reading NFC tag", e)
                            scanStatus = ScanStatus.ERROR
                            statusMessage = "Error reading NFC tag: ${e.message ?: "Unknown error"}"
                            showResultDialog = true
                        }
                    }
                } catch (e: Exception) {
                    Log.e("ScanNFCScreen", "Error processing NFC intent", e)
                    scanStatus = ScanStatus.ERROR
                    statusMessage = "Error processing NFC: ${e.message ?: "Unknown error"}"
                    showResultDialog = true
                }

                // Clear the NFC intent after processing
                MainActivity.nfcIntent.value = null
            }
        }
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
                ScanStatus.ALREADY_SOLVED -> AlreadySolvedContent(statusMessage)
                ScanStatus.ERROR -> ErrorContent(
                    statusMessage = statusMessage,
                    onRetry = {
                        if (isNfcAvailable && isNfcEnabled) {
                            scanStatus = ScanStatus.SCANNING
                            statusMessage = "Scanning for NFC token..."
                            gameRepository.clearNfcValidationResult()
                        }
                    }
                )
            }
        }

        if (showResultDialog) {
            NfcResultDialog(
                status = scanStatus,
                message = statusMessage,
                challenge = validatedChallenge,
                onDismiss = {
                    showResultDialog = false
                    // Reset scan status to allow scanning another tag
                    scanStatus = ScanStatus.SCANNING
                    statusMessage = "Scanning for NFC token..."
                    validatedChallenge = null
                    gameRepository.clearNfcValidationResult()
                },
                onScanComplete = {
                    onScanComplete()
                }
            )
        }
    }
}

@Composable
fun AlreadySolvedContent(statusMessage: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "Already Solved",
            tint = Gold,
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = statusMessage,
            style = MaterialTheme.typography.titleMedium,
            color = Gold,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
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
                    imageVector = Icons.Default.Nfc,
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

// Extension function to find the activity from context
fun android.content.Context.findActivity(): android.app.Activity {
    var context = this
    while (context is android.content.ContextWrapper) {
        if (context is android.app.Activity) return context
        context = context.baseContext
    }
    throw IllegalStateException("No activity found")
}
