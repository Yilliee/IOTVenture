package dev.yilliee.iotventure.data.model

import kotlinx.serialization.Serializable

@Serializable
data class MessageResponse(
    val messages: List<Message> = emptyList(),
    val serverTime: Long = 0
)

@Serializable
data class Message(
    val id: Int,
    val content: String,
    val createdAt: String,
    val senderName: String? = null
)

@Serializable
data class MessageRequest(
    val deviceToken: String,
    val content: String
)
