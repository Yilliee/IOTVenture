package dev.yilliee.iotventure.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginResponse(
    @SerialName("deviceToken")
    val deviceToken: String,
    @SerialName("challenges")
    val challenges: List<Challenge>,
    @SerialName("serverTime")
    val serverTime: Long,
    @SerialName("error")
    val error: String? = null
)

@Serializable
data class Challenge(
    @SerialName("id")
    val id: Int,
    @SerialName("name")
    val name: String,
    @SerialName("shortName")
    val shortName: String,
    @SerialName("points")
    val points: Int,
    @SerialName("location")
    val location: Location,
    @SerialName("keyHash")
    val keyHash: String
)

@Serializable
data class Location(
    @SerialName("topLeft")
    val topLeft: Coordinates,
    @SerialName("bottomRight")
    val bottomRight: Coordinates
)

@Serializable
data class Coordinates(
    @SerialName("lat")
    val lat: Double,
    @SerialName("lng")
    val lng: Double
)
