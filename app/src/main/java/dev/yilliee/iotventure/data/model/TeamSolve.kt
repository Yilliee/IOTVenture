package dev.yilliee.iotventure.data.model

import kotlinx.serialization.Serializable

@Serializable
data class TeamSolve(
    val challengeId: Int,
    val solvedAt: String
)
