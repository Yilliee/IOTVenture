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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.yilliee.iotventure.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TeamChatScreen(
    onBackClick: () -> Unit
) {
    var messages by remember { mutableStateOf(emptyList<MessageData>()) }
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Load mock messages on first composition
    LaunchedEffect(key1 = Unit) {
        messages = getMockMessages()
        // Scroll to bottom after messages load
        scope.launch {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            ChatTopBar(onBackClick = onBackClick)
        },
        bottomBar = {
            ChatInputBar(
                messageText = messageText,
                onMessageChange = { messageText = it },
                onSendClick = {
                    if (messageText.isNotBlank()) {
                        val newMessage = MessageData(
                            id = messages.size + 1,
                            sender = "You",
                            text = messageText,
                            timestamp = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date()),
                            status = MessageStatus.SENT,
                            isMine = true
                        )
                        messages = messages + newMessage
                        messageText = ""

                        // Scroll to bottom after sending
                        scope.launch {
                            listState.animateScrollToItem(messages.size - 1)
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
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

@Composable
fun MessageItem(message: MessageData) {
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
                    MessageStatus.SENT -> Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Sent",
                        tint = TextGray,
                        modifier = Modifier.size(14.dp)
                    )
                    MessageStatus.DELIVERED -> Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Delivered",
                        tint = SuccessGreen,
                        modifier = Modifier.size(14.dp)
                    )
                    MessageStatus.READ -> Icon(
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
            // Attachment button
            IconButton(
                onClick = { /* Handle attachment */ },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AccountBox,
                    contentDescription = "Attach Image",
                    tint = TextGray
                )
            }

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

            // Emoji button
            IconButton(
                onClick = { /* Handle emoji */ },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Face,
                    contentDescription = "Emoji",
                    tint = TextGray
                )
            }

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

private fun getMockMessages(): List<MessageData> {
    return listOf(
        MessageData(
            id = 1,
            sender = "Alex",
            text = "Hey team, I think I found a clue near the library!",
            timestamp = "10:15 AM",
            status = MessageStatus.READ,
            isMine = false
        ),
        MessageData(
            id = 2,
            sender = "You",
            text = "Great! What does it look like?",
            timestamp = "10:16 AM",
            status = MessageStatus.READ,
            isMine = true
        ),
        MessageData(
            id = 3,
            sender = "Alex",
            text = "It's a small QR code hidden behind a book about pirates.",
            timestamp = "10:18 AM",
            status = MessageStatus.READ,
            isMine = false
        ),
        MessageData(
            id = 4,
            sender = "Sarah",
            text = "I'm heading to the science building now. Anyone want to join?",
            timestamp = "10:20 AM",
            status = MessageStatus.READ,
            isMine = false
        ),
        MessageData(
            id = 5,
            sender = "You",
            text = "I'll meet you there in 5 minutes.",
            timestamp = "10:21 AM",
            status = MessageStatus.READ,
            isMine = true
        ),
        MessageData(
            id = 6,
            sender = "Mike",
            text = "I found another clue at the fountain!",
            timestamp = "10:25 AM",
            status = MessageStatus.DELIVERED,
            isMine = false
        )
    )
}

data class MessageData(
    val id: Int,
    val sender: String,
    val text: String,
    val timestamp: String,
    val status: MessageStatus,
    val isMine: Boolean
)

enum class MessageStatus {
    SENT, DELIVERED, READ
}

