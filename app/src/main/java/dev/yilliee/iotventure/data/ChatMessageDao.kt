package dev.yilliee.iotventure.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages WHERE huntId = :huntId ORDER BY timestamp ASC")
    fun getMessagesForHunt(huntId: String): Flow<List<ChatMessage>>

    @Insert
    suspend fun insertMessage(message: ChatMessage)

    @Query("DELETE FROM chat_messages WHERE huntId = :huntId")
    suspend fun deleteMessagesForHunt(huntId: String)

    @Query("DELETE FROM chat_messages")
    suspend fun deleteAllMessages()
} 