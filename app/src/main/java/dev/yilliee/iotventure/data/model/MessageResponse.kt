package dev.yilliee.iotventure.data.model

import kotlinx.serialization.Serializable

@Serializable
data class MessageResponse(
    val messages: List<Message>? = null,
    val serverTime: Long? = null,
    val error: String? = null
)

@Serializable
data class Message(
    val id: Int,
    val content: String,
    val createdAt: String,
    val senderName: String? = null
)
