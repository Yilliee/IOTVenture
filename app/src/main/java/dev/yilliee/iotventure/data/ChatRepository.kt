package dev.yilliee.iotventure.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import java.util.Date

class ChatRepository(context: Context) {
    private val database = ChatDatabase(context)
    private val messagesFlow = MutableStateFlow<List<ChatMessage>>(emptyList())

    fun getMessagesForHunt(huntId: String): Flow<List<ChatMessage>> = flow {
        // Initial load
        val initialMessages = database.getMessagesForHunt(huntId).first()
        messagesFlow.value = initialMessages
        emit(initialMessages)

        // Keep emitting updates
        messagesFlow.collect { messages ->
            emit(messages)
        }
    }

    suspend fun sendMessage(huntId: String, sender: String, message: String, isMine: Boolean) {
        val chatMessage = ChatMessage(
            huntId = huntId,
            sender = sender,
            message = message,
            timestamp = Date(),
            isMine = isMine,
            status = MessageStatus.SENT
        )
        database.insertMessage(chatMessage)
        // Update the flow with new messages
        val updatedMessages = database.getMessagesForHunt(huntId).first()
        messagesFlow.value = updatedMessages
    }

    suspend fun clearHuntMessages(huntId: String) {
        database.deleteMessagesForHunt(huntId)
        messagesFlow.value = emptyList()
    }

    suspend fun clearAllMessages() {
        database.deleteAllMessages()
        messagesFlow.value = emptyList()
    }
} 