package dev.yilliee.iotventure.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val huntId: String,
    val sender: String,
    val message: String,
    val timestamp: Date,
    val isMine: Boolean,
    val status: MessageStatus
)

enum class MessageStatus {
    SENT, DELIVERED, READ
} 