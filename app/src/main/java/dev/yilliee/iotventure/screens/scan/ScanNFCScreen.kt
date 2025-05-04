package dev.yilliee.iotventure.screens.scan

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.os.Build
import android.widget.Toast
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
import kotlinx.coroutines.delay
import java.nio.charset.Charset
import dev.yilliee.iotventure.MainActivity

enum class ScanStatus {
    SCANNING, SUCCESS, ERROR
}

@Composable
fun ScanNfcScreen(
    onBackClick: () -> Unit,
    onScanComplete: () -> Unit
) {
    val context = LocalContext.current
    var scanStatus by remember { mutableStateOf(ScanStatus.SCANNING) }
    var statusMessage by remember { mutableStateOf("Scanning for NFC token...") }
    var nfcContent by remember { mutableStateOf("") }
    var showNfcDialog by remember { mutableStateOf(false) }

    // NFC Adapter
    val nfcAdapter = remember { NfcAdapter.getDefaultAdapter(context) }
    val isNfcAvailable = remember { nfcAdapter != null }
    val isNfcEnabled = remember { nfcAdapter?.isEnabled ?: false }

    // Check NFC availability
    LaunchedEffect(Unit) {
        if (!isNfcAvailable) {
            scanStatus = ScanStatus.ERROR
            statusMessage = "NFC is not available on this device"
        } else if (!isNfcEnabled) {
            scanStatus = ScanStatus.ERROR
            statusMessage = "NFC is disabled. Please enable it in your device settings"
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
            nfcAdapter?.enableForegroundDispatch(
                context.findActivity(),
                pendingIntent,
                intentFilters,
                null
            )

            onDispose {
                // Disable NFC foreground dispatch when leaving this screen
                nfcAdapter?.disableForegroundDispatch(context.findActivity())
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
                val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
                tag?.let {
                    val ndef = Ndef.get(it)
                    if (ndef != null) {
                        try {
                            ndef.connect()
                            val ndefMessage = ndef.cachedNdefMessage
                            if (ndefMessage != null) {
                                val records = ndefMessage.records
                                if (records.isNotEmpty()) {
                                    val record = records[0]
                                    val payload = record.payload
                                    val textEncoding = if ((payload[0].toInt() and 128) == 0) "UTF-8" else "UTF-16"
                                    val languageCodeLength = payload[0].toInt() and 63

                                    nfcContent = String(
                                        payload,
                                        languageCodeLength + 1,
                                        payload.size - languageCodeLength - 1,
                                        Charset.forName(textEncoding)
                                    )

                                    scanStatus = ScanStatus.SUCCESS
                                    statusMessage = "Token found! Clue unlocked."
                                    showNfcDialog = true
                                }
                            } else {
                                // If no NDEF message, try to get raw tag ID
                                val id = tag.id
                                if (id != null && id.isNotEmpty()) {
                                    val hexId = id.joinToString("") { "%02X".format(it) }
                                    nfcContent = "Tag ID: $hexId"
                                    scanStatus = ScanStatus.SUCCESS
                                    statusMessage = "Token found! Clue unlocked."
                                    showNfcDialog = true
                                }
                            }
                        } catch (e: Exception) {
                            scanStatus = ScanStatus.ERROR
                            statusMessage = "Error reading NFC tag: ${e.message}"
                        } finally {
                            try {
                                ndef.close()
                            } catch (e: Exception) {
                                // Ignore close errors
                            }
                        }
                    } else {
                        // Handle non-NDEF tags
                        val id = tag.id
                        if (id != null && id.isNotEmpty()) {
                            val hexId = id.joinToString("") { "%02X".format(it) }
                            nfcContent = "Non-NDEF Tag ID: $hexId"
                            scanStatus = ScanStatus.SUCCESS
                            statusMessage = "Token found! Clue unlocked."
                            showNfcDialog = true
                        }
                    }
                }

                // Clear the NFC intent after processing
                MainActivity.nfcIntent.value = null
            }
        }
    }

    // No simulation, only real NFC scanning
    DisposableEffect(Unit) {
        onDispose {}
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
                        if (isNfcAvailable && isNfcEnabled) {
                            scanStatus = ScanStatus.SCANNING
                            statusMessage = "Scanning for NFC token..."
                        }
                    }
                )
            }
        }

        if (showNfcDialog) {
            NfcContentDialog(
                content = nfcContent,
                onDismiss = {
                    showNfcDialog = false
                    onScanComplete()
                }
            )
        }
    }
}

@Composable
fun NfcContentDialog(
    content: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "NFC Token Content",
                color = Gold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Text(
                text = content,
                style = MaterialTheme.typography.bodyLarge,
                color = TextWhite
            )
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Gold,
                    contentColor = DarkBackground
                )
            ) {
                Text("Continue")
            }
        },
        containerColor = DarkSurface,
        titleContentColor = Gold,
        textContentColor = TextWhite
    )
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
