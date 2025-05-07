package dev.yilliee.iotventure.data.model

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val teamId: Int,
    val username: String,
    val password: String,
    val device_name: String? = null
)
