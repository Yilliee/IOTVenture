package dev.yilliee.iotventure.data.model

import kotlinx.serialization.Serializable

@Serializable
data class UpdateLeaderboardRequest(
    val deviceToken: String,
    val solves: List<SolveSubmission>,
    val isFinalSubmission: Boolean = false
)
