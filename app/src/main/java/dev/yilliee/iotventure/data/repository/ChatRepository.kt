package dev.yilliee.iotventure.data.repository

import android.util.Log
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
    companion object {
        private const val TAG = "ChatRepository"
        private const val MESSAGE_FETCH_INTERVAL = 5000L // 5 seconds
    }

    /**
     * Fetches messages from the server and updates local storage
     */
    suspend fun fetchMessages(): Result<List<TeamMessage>> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Fetching messages from server")
                val result = apiService.getMessages()

                if (result.isSuccess) {
                    val response = result.getOrNull()
                    val serverMessages = response?.messages ?: emptyList()
                    Log.d(TAG, "Received ${serverMessages.size} messages from server")

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
                    Log.d(TAG, "No new messages, returning cached messages")
                    return@withContext Result.success(preferencesManager.getTeamMessages())
                } else {
                    Log.e(TAG, "Error fetching messages: ${result.exceptionOrNull()?.message}")
                    return@withContext Result.failure(result.exceptionOrNull() ?: Exception("Unknown error"))
                }
            } catch (e: Exception) {
                // If network error, return cached messages
                Log.e(TAG, "Network error fetching messages", e)
                return@withContext Result.success(preferencesManager.getTeamMessages())
            }
        }
    }

    /**
     * Gets locally stored messages
     */
    fun getLocalMessages(): List<TeamMessage> {
        return preferencesManager.getTeamMessages()
    }

    /**
     * Sends a message to the server and updates local storage
     */
    suspend fun sendMessage(text: String): Result<TeamMessage> {
        return withContext(Dispatchers.IO) {
            try {
                // First add message locally
                val localMessage = addLocalMessage(text)

                // Then try to send to server
                val result = apiService.sendMessage(text)

                if (result.isSuccess) {
                    // Message sent successfully
                    Log.d(TAG, "Message sent successfully")
                    return@withContext Result.success(localMessage)
                } else {
                    // Message failed to send, but we keep it locally
                    Log.e(TAG, "Failed to send message: ${result.exceptionOrNull()?.message}")
                    return@withContext Result.success(localMessage)
                }
            } catch (e: Exception) {
                // Network error, but we keep the message locally
                Log.e(TAG, "Network error sending message", e)
                return@withContext Result.success(addLocalMessage(text))
            }
        }
    }

    /**
     * Adds a new message to local storage
     */
    private fun addLocalMessage(text: String): TeamMessage {
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
        Log.d(TAG, "Added new local message: $text")

        return newMessage
    }

    /**
     * Clears all messages from local storage
     */
    fun clearMessages() {
        preferencesManager.saveTeamMessages(emptyList())
        Log.d(TAG, "Cleared all messages")
    }

    /**
     * Converts server message format to local TeamMessage format
     */
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

    /**
     * Formats server timestamp to a user-friendly format
     */
    private fun formatServerTimestamp(timestamp: String): String {
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
