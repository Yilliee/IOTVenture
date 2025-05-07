package dev.yilliee.iotventure.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Solve(
    val challengeId: Int,
    val timestamp: Long,
    val solved: Boolean
)

@Serializable
data class TeamSolve(
    val teamId: Int,
    val name: String,
    val solves: List<Solve>,
    val totalPoints: Int
)

@Serializable
data class LeaderboardChallenge(
    val id: Int,
    val name: String,
    val shortName: String,
    val points: Int
)

@Serializable
data class LeaderboardResponse(
    val challenges: List<LeaderboardChallenge>,
    val competitionEnded: Boolean,
    val teamSolves: List<TeamSolve>
)


