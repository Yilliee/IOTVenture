package dev.yilliee.iotventure.screens.login

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.yilliee.iotventure.di.ServiceLocator
import dev.yilliee.iotventure.navigation.AppDestinations
import dev.yilliee.iotventure.ui.theme.*
import kotlinx.coroutines.launch
import android.util.Log
import android.widget.Toast
import androidx.compose.ui.text.font.FontWeight

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onDeviceTransferClick: () -> Unit,
    onNavigateToScreen: (String) -> Unit
) {
    val context = LocalContext.current
    val preferencesManager = remember { ServiceLocator.providePreferencesManager(context) }
    val apiService = remember { ServiceLocator.provideApiService(context) }
    val authRepository = remember { ServiceLocator.provideAuthRepository(context) }
    val gameRepository = remember { ServiceLocator.provideGameRepository(context) }
    val viewModel = remember {
        LoginViewModel.Factory(authRepository, gameRepository).create(LoginViewModel::class.java)
    }

    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var showServerSettings by rememberSaveable { mutableStateOf(false) }
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT
    val scrollState = rememberScrollState()

    val loginState by viewModel.loginState.collectAsState()
    val scope = rememberCoroutineScope()

    // Handle login state changes
    LaunchedEffect(loginState) {
        when (loginState) {
            is LoginViewModel.LoginState.Success -> {
                onLoginSuccess()
                viewModel.resetState()
            }
            is LoginViewModel.LoginState.ConnectionSuccess -> {
                Toast.makeText(context, "Server connection successful!", Toast.LENGTH_SHORT).show()
            }
            is LoginViewModel.LoginState.ConnectionError -> {
                val errorMessage = (loginState as LoginViewModel.LoginState.ConnectionError).message
                Toast.makeText(context, "Connection error: $errorMessage", Toast.LENGTH_LONG).show()
            }
            else -> { /* No action needed */ }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isPortrait) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(Gold.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "TH",
                        style = MaterialTheme.typography.headlineLarge,
                        color = Gold
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Treasure Hunt",
                    style = MaterialTheme.typography.headlineLarge,
                    color = Gold
                )

                Text(
                    text = "Begin your adventure",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextGray
                )

                Spacer(modifier = Modifier.height(40.dp))
            }

            LoginContent(
                username = username,
                onUsernameChange = { username = it },
                password = password,
                onPasswordChange = { password = it },
                isLoading = loginState is LoginViewModel.LoginState.Loading || loginState is LoginViewModel.LoginState.Testing,
                errorMessage = when (loginState) {
                    is LoginViewModel.LoginState.Error -> (loginState as LoginViewModel.LoginState.Error).message
                    else -> ""
                },
                onSubmit = {
                    scope.launch {
                        viewModel.login(username, password)
                    }
                },
                onTestConnection = {
                    viewModel.testServerConnection()
                },
                onNavigateToScreen = onNavigateToScreen
            )

            // Display current server settings
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Server: ${preferencesManager.getServerIp()}:${preferencesManager.getServerPort()}",
                style = MaterialTheme.typography.bodySmall,
                color = TextGray,
                textAlign = TextAlign.Center
            )
        }

        Button(
            onClick = { showServerSettings = true },
            colors = ButtonDefaults.buttonColors(
                containerColor = DarkSurface,
                contentColor = Gold
            ),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .size(48.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Server Settings",
                tint = Gold,
                modifier = Modifier.size(24.dp)
            )
        }

        // Server settings dialog
        if (showServerSettings) {
            ServerSettingsDialog(
                onDismiss = { showServerSettings = false },
                onSave = { ip, port ->
                    preferencesManager.saveServerSettings(ip, port)
                    apiService.updateServerSettings(ip, port)
                    scope.launch {
                        viewModel.testServerConnection()
                    }
                }
            )
        }
    }
}

@Composable
private fun LoginContent(
    username: String,
    onUsernameChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    isLoading: Boolean,
    errorMessage: String,
    onSubmit: () -> Unit,
    onTestConnection: () -> Unit,
    onNavigateToScreen: (String) -> Unit
) {
    OutlinedTextField(
        value = username,
        onValueChange = onUsernameChange,
        label = { Text("Username") },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Username",
                tint = Gold
            )
        },
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = DarkSurface,
            focusedBorderColor = Gold,
            unfocusedBorderColor = Color.DarkGray
        ),
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(16.dp))
    OutlinedTextField(
        value = password,
        onValueChange = onPasswordChange,
        label = { Text("Password") },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Password",
                tint = Gold
            )
        },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = DarkSurface,
            focusedBorderColor = Gold,
            unfocusedBorderColor = Color.DarkGray
        ),
        modifier = Modifier.fillMaxWidth()
    )
    if (errorMessage.isNotEmpty()) {
        Spacer(Modifier.height(8.dp))
        Text(
            text = errorMessage,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium
        )
    }
    Spacer(Modifier.height(24.dp))
    Button(
        onClick = onSubmit,
        enabled = !isLoading,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Gold,
            contentColor = MaterialTheme.colorScheme.background
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.background,
                strokeWidth = 2.dp
            )
        } else {
            Text("Login", style = MaterialTheme.typography.titleMedium)
        }
    }

    // Add test connection button
    Spacer(Modifier.height(16.dp))
    OutlinedButton(
        onClick = onTestConnection,
        enabled = !isLoading,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = Gold
        )
    ) {
        Icon(
            imageVector = Icons.Default.Wifi,
            contentDescription = "Test Connection",
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("Test Server Connection")
    }

    // Device transfer button
    Spacer(Modifier.height(16.dp))
    OutlinedButton(
        onClick = { onNavigateToScreen(AppDestinations.DEVICE_TRANSFER_RECEIVER_ROUTE) },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = Gold
        ),
        border = BorderStroke(1.dp, Gold)
    ) {
        Icon(
            imageVector = Icons.Default.PhoneAndroid,
            contentDescription = "Receive",
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Receive Progress from Another Device",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
    }

    Spacer(Modifier.height(24.dp))
    Text(
        text = "Game works offline after initial login",
        style = MaterialTheme.typography.bodyMedium,
        color = TextGray,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun ServerSettingsDialog(
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    val context = LocalContext.current
    val preferencesManager = remember { ServiceLocator.providePreferencesManager(context) }

    var serverIp by rememberSaveable { mutableStateOf(preferencesManager.getServerIp()) }
    var serverPort by rememberSaveable { mutableStateOf(preferencesManager.getServerPort()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Server Settings", color = Gold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                OutlinedTextField(
                    value = serverIp,
                    onValueChange = { serverIp = it },
                    label = { Text("Server IP") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = DarkSurface,
                        focusedBorderColor = Gold,
                        unfocusedBorderColor = Color.DarkGray
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = serverPort,
                    onValueChange = { serverPort = it },
                    label = { Text("Server Port") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = DarkSurface,
                        focusedBorderColor = Gold,
                        unfocusedBorderColor = Color.DarkGray
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // Save settings and call the callback
                    onSave(serverIp, serverPort)
                    Log.d("ServerSettings", "Saved server settings: $serverIp:$serverPort")
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Gold,
                    contentColor = MaterialTheme.colorScheme.background
                )
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = TextGray
                )
            ) {
                Text("Cancel")
            }
        },
        containerColor = DarkSurface,
        titleContentColor = Gold,
        textContentColor = TextGray
    )
}
