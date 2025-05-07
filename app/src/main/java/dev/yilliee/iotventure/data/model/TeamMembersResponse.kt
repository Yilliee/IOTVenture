package dev.yilliee.iotventure.data.model

import kotlinx.serialization.Serializable

@Serializable
data class TeamMembersResponse(
    val members: List<TeamMember>,
    val serverTime: Long
)

@Serializable
data class TeamMember(
    val username: String,
    val lastActive: Long,
    val solvedChallenges: List<SolvedChallenge>,
    val isCurrentUser: Boolean = false
)

@Serializable
data class SolvedChallenge(
    val challengeId: Int,
    val challengeName: String,
    val solvedAt: Long
) 