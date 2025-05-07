package dev.yilliee.iotventure.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TeamSolvesResponse(
    @SerialName("solves")
    val solves: List<TeamSolve>? = null,
    @SerialName("serverTime")
    val serverTime: Long? = null,
    @SerialName("error")
    val error: String? = null
)

@Serializable
data class TeamSolve(
    @SerialName("challengeId")
    val challengeId: Int,
    @SerialName("solvedAt")
    val solvedAt: String
)
