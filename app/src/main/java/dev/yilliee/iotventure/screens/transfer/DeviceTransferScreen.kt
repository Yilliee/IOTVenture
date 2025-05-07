package dev.yilliee.iotventure.screens.transfer

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import dev.yilliee.iotventure.data.bluetooth.BluetoothTransferService
import dev.yilliee.iotventure.data.local.PreferencesManager
import dev.yilliee.iotventure.di.ServiceLocator
import dev.yilliee.iotventure.navigation.AppDestinations
import dev.yilliee.iotventure.ui.theme.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import android.util.Log

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceTransferScreen(
    onBackClick: () -> Unit,
    onTransferComplete: () -> Unit,
    isReceiver: Boolean = false
) {
    val context = LocalContext.current
    val preferencesManager = remember { ServiceLocator.providePreferencesManager(context) }
    val bluetoothService = remember { BluetoothTransferService(context) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var isReceiving by remember { mutableStateOf(isReceiver) }
    var pairedDevices by remember { mutableStateOf<List<BluetoothDevice>>(emptyList()) }
    var isTransferring by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedDevice by remember { mutableStateOf<BluetoothDevice?>(null) }

    // Request Bluetooth permissions
    val bluetoothPermissions = arrayOf(
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_ADMIN,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            scope.launch {
                if (!bluetoothService.isBluetoothEnabled()) {
                    withContext(Dispatchers.Main) {
                        errorMessage = "Please enable Bluetooth to continue"
                    }
                    return@launch
                }
                val devices = bluetoothService.getPairedDevices()
                withContext(Dispatchers.Main) {
                    pairedDevices = devices
                }
            }
        } else {
            errorMessage = "Bluetooth permissions are required for device transfer"
        }
    }

    // Check and request permissions
    LaunchedEffect(Unit) {
        val hasPermissions = bluetoothPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        
        if (hasPermissions) {
            if (!bluetoothService.isBluetoothEnabled()) {
                errorMessage = "Please enable Bluetooth to continue"
                return@LaunchedEffect
            }
            pairedDevices = bluetoothService.getPairedDevices()
        } else {
            permissionLauncher.launch(bluetoothPermissions)
        }
    }

    // Server start logic
    LaunchedEffect(isReceiving) {
        if (isReceiving) {
            try {
                Log.d("DeviceTransfer", "Starting server in receiver mode...")
                bluetoothService.startServer { transferData ->
                    scope.launch(Dispatchers.Main) {
                        try {
                            Log.d("DeviceTransfer", "Data received, saving to preferences...")
                            
                            // Save device token and update ApiService
                            Log.d("DeviceTransfer", "Saving device token: ${transferData.deviceToken}")
                            preferencesManager.saveDeviceToken(transferData.deviceToken)
                            val apiService = ServiceLocator.provideApiService(context)
                            apiService.setDeviceToken(transferData.deviceToken)
                            
                            // Save username
                            Log.d("DeviceTransfer", "Saving username...")
                            preferencesManager.saveUsername(transferData.username)
                            
                            // Save team name
                            Log.d("DeviceTransfer", "Saving team name...")
                            preferencesManager.saveTeamName(transferData.teamName)
                            
                            // Save password
                            Log.d("DeviceTransfer", "Saving password...")
                            preferencesManager.savePassword(transferData.password)
                            
                            // Save solved challenges
                            Log.d("DeviceTransfer", "Saving solved challenges...")
                            preferencesManager.setSolvedChallenges(transferData.solvedChallenges)
                            
                            // Save all challenges
                            Log.d("DeviceTransfer", "Saving all challenges...")
                            preferencesManager.saveChallenges(transferData.challenges)
                            
                            // Save team messages
                            Log.d("DeviceTransfer", "Saving team messages...")
                            preferencesManager.saveTeamMessages(transferData.teamMessages)
                            
                            // Save server settings and update ApiService
                            Log.d("DeviceTransfer", "Saving server settings...")
                            preferencesManager.saveServerSettings(transferData.serverIp, transferData.serverPort)
                            apiService.updateServerSettings(transferData.serverIp, transferData.serverPort)
                            
                            // Set login state
                            Log.d("DeviceTransfer", "Setting login state...")
                            preferencesManager.setLoggedIn(true)
                            
                            // Verify token was saved
                            val savedToken = preferencesManager.getDeviceToken()
                            Log.d("DeviceTransfer", "Verified saved token: $savedToken")
                            
                            Log.d("DeviceTransfer", "All data saved successfully")
                            onTransferComplete()
                        } catch (e: Exception) {
                            Log.e("DeviceTransfer", "Error saving data: ${e.message}")
                            errorMessage = e.message ?: "Failed to save data"
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("DeviceTransfer", "Error starting server: ${e.message}")
                errorMessage = e.message ?: "Failed to start server"
            }
        }
    }

    // Client transfer logic
    LaunchedEffect(selectedDevice) {
        selectedDevice?.let { device ->
            scope.launch {
                try {
                    Log.d("DeviceTransfer", "Starting client transfer...")
                    val deviceToken = preferencesManager.getDeviceToken()
                    Log.d("DeviceTransfer", "Current device token: $deviceToken")
                    
                    val transferData = BluetoothTransferService.TransferData(
                        deviceToken = deviceToken ?: "",
                        username = preferencesManager.getUsername() ?: "",
                        teamName = preferencesManager.getTeamName() ?: "",
                        password = preferencesManager.getPassword() ?: "",
                        solvedChallenges = preferencesManager.getSolvedChallenges().toList(),
                        challenges = preferencesManager.getChallenges(),
                        teamMessages = preferencesManager.getTeamMessages(),
                        serverIp = preferencesManager.getServerIp() ?: "",
                        serverPort = preferencesManager.getServerPort() ?: ""
                    )
                    
                    val success = bluetoothService.startClient(device, transferData)
                    if (success) {
                        Log.d("DeviceTransfer", "Transfer completed successfully")
                        onTransferComplete()
                    } else {
                        Log.e("DeviceTransfer", "Transfer failed")
                        errorMessage = "Failed to transfer data"
                    }
                } catch (e: Exception) {
                    Log.e("DeviceTransfer", "Error during transfer: ${e.message}")
                    errorMessage = e.message ?: "Failed to transfer data"
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isReceiving) "Receive Progress" else "Send Progress") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isReceiving) {
                // Receiver UI
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = Gold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Waiting for data transfer...",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextWhite
                )
                Text(
                    text = "Make sure Bluetooth is enabled and the sender device is nearby",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextGray
                )
            } else {
                // Sender UI
                if (pairedDevices.isEmpty()) {
                    Text(
                        text = "No paired devices found",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextWhite
                    )
                    Text(
                        text = "Please pair with the receiver device first",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextGray
                    )
                } else {
                    Text(
                        text = "Select a device to transfer to:",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextWhite,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    LazyColumn {
                        items(pairedDevices) { device ->
                            DeviceItem(
                                device = device,
                                isTransferring = isTransferring,
                                onTransferClick = {
                                    selectedDevice = device
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Show error message if any
    errorMessage?.let { message ->
        LaunchedEffect(message) {
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            errorMessage = null
        }
    }
}

@Composable
fun DeviceItem(
    device: BluetoothDevice,
    isTransferring: Boolean,
    onTransferClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = DarkSurface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = device.name ?: "Unknown Device",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextWhite
                )
                Text(
                    text = device.address,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextGray
                )
            }
            
            if (isTransferring) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Gold
                )
            } else {
                IconButton(onClick = onTransferClick) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Transfer",
                        tint = Gold
                    )
                }
            }
        }
    }
} 