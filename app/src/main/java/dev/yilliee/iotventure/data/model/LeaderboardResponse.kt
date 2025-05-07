package dev.yilliee.iotventure.data.model

import kotlinx.serialization.Serializable

@Serializable
data class LeaderboardResponse(
    val leaderboard: List<LeaderboardTeam>,
    val serverTime: Long
)

@Serializable
data class LeaderboardTeam(
    val teamName: String,
    val solvedChallenges: Int,
    val totalPoints: Int
) 