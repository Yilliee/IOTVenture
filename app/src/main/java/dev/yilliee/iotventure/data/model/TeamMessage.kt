package dev.yilliee.iotventure.data.model

import kotlinx.serialization.Serializable

@Serializable
data class TeamMessage(
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
