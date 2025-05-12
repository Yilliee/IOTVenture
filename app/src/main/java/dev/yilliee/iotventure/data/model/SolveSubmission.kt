package dev.yilliee.iotventure.data.model

import kotlinx.serialization.Serializable

@Serializable
data class SolveSubmission(
    val challengeId: Int,
    val solvedAt: Long
)
