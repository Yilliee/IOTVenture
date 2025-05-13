package dev.yilliee.iotventure.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.yilliee.iotventure.data.model.TeamMessage
import dev.yilliee.iotventure.di.ServiceLocator
import dev.yilliee.iotventure.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun TeamChatScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val chatRepository = remember { ServiceLocator.provideChatRepository(context) }
    val viewModel = remember { TeamChatViewModel.Factory(chatRepository).create(TeamChatViewModel::class.java) }

    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val chatState by viewModel.chatState.collectAsState()

    // Extract messages from state
    val messages = when (chatState) {
        is TeamChatViewModel.ChatState.Success -> (chatState as TeamChatViewModel.ChatState.Success).messages
        is TeamChatViewModel.ChatState.NetworkError -> (chatState as TeamChatViewModel.ChatState.NetworkError).cachedMessages
        else -> emptyList()
    }

    // Scroll to bottom when messages change
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            ChatTopBar(
                onBackClick = onBackClick,
                isOnline = chatState !is TeamChatViewModel.ChatState.NetworkError
            )
        },
        bottomBar = {
            ChatInputBar(
                messageText = messageText,
                onMessageChange = { messageText = it },
                onSendClick = {
                    if (messageText.isNotBlank()) {
                        val message = messageText.trim()
                        messageText = "" // Clear immediately for better UX
                        viewModel.sendMessage(message)
                    }
                }
            )
        },
        modifier = Modifier.imePadding() // Add this to handle keyboard
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (chatState is TeamChatViewModel.ChatState.Loading && messages.isEmpty()) {
                // Show loading indicator only if we have no messages
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Gold
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(messages) { message ->
                        MessageItem(message = message)
                    }
                }
            }
        }
    }
}

@Composable
fun ChatTopBar(
    onBackClick: () -> Unit,
    isOnline: Boolean
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

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isOnline) Icons.Default.Wifi else Icons.Default.WifiOff,
                        contentDescription = if (isOnline) "Online" else "Offline",
                        tint = if (isOnline) SuccessGreen else ErrorRed,
                        modifier = Modifier.size(16.dp)
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        text = if (isOnline) "Online" else "Offline",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isOnline) SuccessGreen else ErrorRed
                    )
                }
            }
        }
    }
}

@Composable
fun MessageItem(message: TeamMessage) {
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
            color = if (message.isMine) DarkGold else DarkSurfaceLight,
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
                    text = message.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextWhite
                )
            }
        }

        // Timestamp and status
        Row(
            modifier = Modifier.padding(top = 4.dp, start = 4.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message.timestamp,
                style = MaterialTheme.typography.labelSmall,
                color = TextGray
            )

            if (message.isMine) {
                Spacer(modifier = Modifier.width(4.dp))

                when (message.status) {
                    dev.yilliee.iotventure.data.model.MessageStatus.SENT -> Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Sent",
                        tint = TextGray,
                        modifier = Modifier.size(14.dp)
                    )
                    dev.yilliee.iotventure.data.model.MessageStatus.DELIVERED -> Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Delivered",
                        tint = SuccessGreen,
                        modifier = Modifier.size(14.dp)
                    )
                    dev.yilliee.iotventure.data.model.MessageStatus.READ -> Icon(
                        imageVector = Icons.Default.Done,
                        contentDescription = "Read",
                        tint = SuccessGreen,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
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
            // Message input
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
                    disabledContainerColor = DarkSurfaceLight,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(20.dp),
                maxLines = 3
            )

            // Send button
            IconButton(
                onClick = onSendClick,
                enabled = messageText.isNotBlank(),
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (messageText.isNotBlank()) Gold else DarkSurfaceLight
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send",
                    tint = if (messageText.isNotBlank()) DarkBackground else TextGray
                )
            }
        }
    }
}
