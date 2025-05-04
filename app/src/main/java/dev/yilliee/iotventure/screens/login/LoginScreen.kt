package dev.yilliee.iotventure.screens.login

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.yilliee.iotventure.ui.theme.DarkSurface
import dev.yilliee.iotventure.ui.theme.Gold
import dev.yilliee.iotventure.ui.theme.TextGray
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit
) {
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var isLoading by rememberSaveable { mutableStateOf(false) }
    var errorMessage by rememberSaveable { mutableStateOf("") }
    var showServerSettings by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp)
    ) {
        IconButton(
            onClick = { showServerSettings = true },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Server Settings",
                tint = Gold
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
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
                isLoading = isLoading,
                errorMessage = errorMessage,
                onSubmit = {
                    if (username.isBlank() || password.isBlank()) {
                        errorMessage = "Please enter both username and password"
                        return@LoginContent
                    }
                    errorMessage = ""
                    isLoading = true
                    scope.launch {
                        delay(1500)
                        isLoading = false
                        onLoginSuccess()
                    }
                }
            )
        }

        if (showServerSettings) {
            ServerSettingsDialog(onDismiss = { showServerSettings = false })
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
    onSubmit: () -> Unit
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
fun ServerSettingsDialog(onDismiss: () -> Unit) {
    var serverIp by rememberSaveable { mutableStateOf("192.168.1.100") }
    var serverPort by rememberSaveable { mutableStateOf("3000") }

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
                onClick = onDismiss,
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
