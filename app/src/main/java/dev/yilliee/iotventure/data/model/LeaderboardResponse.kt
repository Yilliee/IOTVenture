package dev.yilliee.iotventure.data.model

import kotlinx.serialization.Serializable

@Serializable
data class LeaderboardResponse(
    val leaderboard: List<LeaderboardEntry>,
    val serverTime: Long
)

@Serializable
data class LeaderboardEntry(
    val teamName: String,
    val solvedChallenges: Int,
    val totalPoints: Int
) 