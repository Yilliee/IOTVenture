package dev.yilliee.iotventure.data.repository

import dev.yilliee.iotventure.data.local.PreferencesManager
import dev.yilliee.iotventure.data.model.Message
import dev.yilliee.iotventure.data.model.MessageStatus
import dev.yilliee.iotventure.data.model.TeamMessage
import dev.yilliee.iotventure.data.remote.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatRepository(
    private val apiService: ApiService,
    private val preferencesManager: PreferencesManager
) {

    suspend fun fetchMessages(): Result<List<TeamMessage>> {
        return withContext(Dispatchers.IO) {
            val deviceToken = preferencesManager.getDeviceToken()
            if (deviceToken == null) {
                return@withContext Result.failure(Exception("Not logged in"))
            }

            try {
                val result = apiService.getMessages(deviceToken)

                if (result.isSuccess) {
                    val response = result.getOrNull()
                    val serverMessages = response?.messages ?: emptyList()

                    if (serverMessages.isNotEmpty()) {
                        // Convert server messages to TeamMessage format
                        val newMessages = convertServerMessages(serverMessages)

                        // Get existing messages and add new ones
                        val existingMessages = preferencesManager.getTeamMessages()
                        val allMessages = existingMessages + newMessages

                        // Save all messages
                        preferencesManager.saveTeamMessages(allMessages)

                        // Update last fetch time
                        response?.serverTime?.let { preferencesManager.saveLastMessageFetchTime(it) }

                        return@withContext Result.success(allMessages)
                    }

                    // If no new messages, return existing ones
                    return@withContext Result.success(preferencesManager.getTeamMessages())
                } else {
                    return@withContext Result.failure(result.exceptionOrNull() ?: Exception("Unknown error"))
                }
            } catch (e: Exception) {
                // If network error, return cached messages
                return@withContext Result.success(preferencesManager.getTeamMessages())
            }
        }
    }

    fun getLocalMessages(): List<TeamMessage> {
        return preferencesManager.getTeamMessages()
    }

    fun addLocalMessage(text: String): TeamMessage {
        val existingMessages = preferencesManager.getTeamMessages()
        val newId = if (existingMessages.isEmpty()) 1 else existingMessages.maxOf { it.id } + 1

        val timestamp = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())

        val newMessage = TeamMessage(
            id = newId,
            sender = "You",
            text = text,
            timestamp = timestamp,
            status = MessageStatus.SENT,
            isMine = true
        )

        val updatedMessages = existingMessages + newMessage
        preferencesManager.saveTeamMessages(updatedMessages)

        return newMessage
    }

    fun clearMessages() {
        preferencesManager.saveTeamMessages(emptyList())
    }

    private fun convertServerMessages(serverMessages: List<Message>): List<TeamMessage> {
        val username = preferencesManager.getUsername() ?: "You"

        return serverMessages.map { message ->
            TeamMessage(
                id = message.id,
                sender = message.senderName ?: "Team", // Use sender name from server if available
                text = message.content,
                timestamp = formatServerTimestamp(message.createdAt),
                status = MessageStatus.DELIVERED,
                isMine = message.senderName == username // Check if message is from current user
            )
        }
    }

    private fun formatServerTimestamp(timestamp: String): String {
        // Simple format for now, can be improved later
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            val date = inputFormat.parse(timestamp)
            SimpleDateFormat("hh:mm a", Locale.getDefault()).format(date ?: Date())
        } catch (e: Exception) {
            // Fallback to original timestamp if parsing fails
            timestamp
        }
    }
}
