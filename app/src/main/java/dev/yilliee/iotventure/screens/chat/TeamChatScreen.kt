package dev.yilliee.iotventure.screens.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.yilliee.iotventure.data.*
import dev.yilliee.iotventure.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamChatScreen(
    huntId: String,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { ChatRepository(context) }
    val messages by repository.getMessagesForHunt(huntId).collectAsState(initial = emptyList())
    var newMessage by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Team Chat") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp),
                reverseLayout = true
            ) {
                items(messages.reversed()) { message ->
                    MessageItem(message)
                }
            }

            ChatInputBar(
                messageText = newMessage,
                onMessageChange = { newMessage = it },
                onSendClick = {
                    if (newMessage.isNotBlank()) {
                        scope.launch {
                            repository.sendMessage(
                                huntId = huntId,
                                sender = "You",
                                message = newMessage,
                                isMine = true
                            )
                            newMessage = ""
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun ChatInputBar(
    messageText: String,
    onMessageChange: (String) -> Unit,
    onSendClick: () -> Unit
) {
    Surface(
        color = DarkSurface,
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = messageText,
                onValueChange = onMessageChange,
                placeholder = { Text("Type a message...") },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = DarkSurfaceLight,
                    unfocusedContainerColor = DarkSurfaceLight,
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite
                )
            )

            IconButton(
                onClick = onSendClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send",
                    tint = Gold
                )
            }
        }
    }
}

@Composable
fun MessageItem(message: ChatMessage) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = if (message.isMine) 40.dp else 0.dp,
                end = if (message.isMine) 0.dp else 40.dp
            ),
        horizontalAlignment = if (message.isMine) Alignment.End else Alignment.Start
    ) {
        // Sender name (only for others' messages)
        if (!message.isMine) {
            Text(
                text = message.sender,
                style = MaterialTheme.typography.labelMedium,
                color = TextGray,
                modifier = Modifier.padding(start = 12.dp, bottom = 4.dp)
            )
        }

        // Message bubble
        Surface(
            color = if (message.isMine) Gold else DarkSurfaceLight,
            shape = RoundedCornerShape(
                topStart = if (message.isMine) 16.dp else 4.dp,
                topEnd = if (message.isMine) 4.dp else 16.dp,
                bottomStart = 16.dp,
                bottomEnd = 16.dp
            )
        ) {
            Box(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = message.message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (message.isMine) DarkBackground else TextWhite
                )
            }
        }

        // Timestamp
        Text(
            text = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(message.timestamp),
            style = MaterialTheme.typography.labelSmall,
            color = TextGray,
            modifier = Modifier.padding(top = 4.dp, start = 4.dp, end = 4.dp)
        )
    }
}

@Composable
fun ChatTopBar(
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

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Team Chat",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextWhite
                )

                Text(
                    text = "4 members online",
                    style = MaterialTheme.typography.bodySmall,
                    color = SuccessGreen
                )
            }
        }
    }
}

