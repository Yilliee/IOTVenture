package dev.yilliee.iotventure.data.remote

import android.util.Log
import dev.yilliee.iotventure.data.model.LoginRequest
import dev.yilliee.iotventure.data.model.LoginResponse
import dev.yilliee.iotventure.data.model.MessageResponse
import dev.yilliee.iotventure.data.model.LeaderboardResponse
import dev.yilliee.iotventure.data.model.TeamSolvesResponse
import dev.yilliee.iotventure.data.model.TeamMembersResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.serialization.encodeToString

/**
 * Service class for handling all API communications with the server
 */
class ApiService {
    companion object {
        private const val TAG = "ApiService"
        private const val CONNECT_TIMEOUT = 10000
        private const val READ_TIMEOUT = 10000
    }

    // JSON serializer configuration
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    // Server configuration
    private var serverIp = "192.168.100.18"
    private var serverPort = "3000"
    private var baseUrl = "http://$serverIp:$serverPort"

    // Authentication token
    private var deviceToken: String? = null

    /**
     * Updates the server connection settings
     */
    fun updateServerSettings(ip: String, port: String) {
        serverIp = ip
        serverPort = port
        baseUrl = "http://$serverIp:$serverPort"
        Log.d(TAG, "Server settings updated to $serverIp:$serverPort")
    }

    /**
     * Sets the device token for authentication
     */
    fun setDeviceToken(token: String) {
        deviceToken = token
        Log.d(TAG, "Device token set: $token")
    }

    /**
     * Gets the base URL for API requests
     */
    private fun getBaseUrl(): String {
        return "http://$serverIp:$serverPort"
    }

    /**
     * Adds authentication header to requests if a device token is available
     */
    private fun addAuthHeader(connection: HttpURLConnection) {
        deviceToken?.let {
            connection.setRequestProperty("Authorization", "Bearer $it")
        }
    }

    /**
     * Performs login and retrieves device token and challenges
     */
    suspend fun login(username: String, password: String, deviceName: String): Result<LoginResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val baseUrl = getBaseUrl()
                Log.d(TAG, "Attempting login for user: $username to $baseUrl/api/team/login")

                val url = URL("$baseUrl/api/team/login")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Accept", "application/json")
                connection.doOutput = true
                connection.connectTimeout = CONNECT_TIMEOUT
                connection.readTimeout = READ_TIMEOUT

                // Create request body in the format expected by the server
                val requestBody = json.encodeToString(
                    mapOf(
                        "username" to username,
                        "password" to password,
                        "device_name" to deviceName
                    )
                )

                Log.d(TAG, "Request body: $requestBody")

                // Send request
                connection.outputStream.use { os ->
                    os.write(requestBody.toByteArray())
                    os.flush()
                }

                // Process response
                val responseCode = connection.responseCode
                Log.d(TAG, "Login response code: $responseCode")

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
                    Log.d(TAG, "Login success response: $response")

                    try {
                        val loginResponse = json.decodeFromString<LoginResponse>(response)
                        // Store token for future requests
                        setDeviceToken(loginResponse.deviceToken)
                        Result.success(loginResponse)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to parse login response", e)
                        Result.failure(e)
                    }
                } else {
                    val errorResponse = BufferedReader(InputStreamReader(connection.errorStream)).use { it.readText() }
                    Log.e(TAG, "Login error response: $errorResponse")
                    Result.failure(Exception("Login failed: $errorResponse"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Login network error", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Fetches team solves from the server
     */
    suspend fun getTeamSolves(): Result<TeamSolvesResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val baseUrl = getBaseUrl()
                Log.d(TAG, "Fetching team solves")

                val url = URL("$baseUrl/api/team/solves")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                addAuthHeader(connection)
                connection.setRequestProperty("Accept", "application/json")
                connection.connectTimeout = CONNECT_TIMEOUT
                connection.readTimeout = READ_TIMEOUT

                // Process response
                val responseCode = connection.responseCode
                Log.d(TAG, "Team solves response code: $responseCode")

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
                    val solvesResponse = json.decodeFromString<TeamSolvesResponse>(response)
                    Result.success(solvesResponse)
                } else {
                    val errorResponse = BufferedReader(InputStreamReader(connection.errorStream)).use { it.readText() }
                    Log.e(TAG, "Team solves error: $errorResponse")
                    Result.failure(Exception("Failed to get team solves: $errorResponse"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Team solves network error", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Logs out the current user
     */
    suspend fun logout(): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val baseUrl = getBaseUrl()
                val url = URL("$baseUrl/api/team/logout")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                addAuthHeader(connection)
                connection.setRequestProperty("Accept", "application/json")
                connection.connectTimeout = CONNECT_TIMEOUT
                connection.readTimeout = READ_TIMEOUT

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // Clear token on successful logout
                    deviceToken = null
                    Result.success(true)
                } else {
                    Result.failure(Exception("Logout failed with code: $responseCode"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Logout error", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Submits a challenge solve to the server
     */
    suspend fun submitSolve(challengeId: Int, keyHash: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val baseUrl = getBaseUrl()
                Log.d(TAG, "Submitting solve for challenge $challengeId")

                val url = URL("$baseUrl/api/team/solve")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                addAuthHeader(connection)
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Accept", "application/json")
                connection.doOutput = true
                connection.connectTimeout = CONNECT_TIMEOUT
                connection.readTimeout = READ_TIMEOUT

                // Create request body
                val requestBody = """
                    {
                        "challengeId": $challengeId,
                        "keyHash": "$keyHash"
                    }
                """.trimIndent()

                // Send request
                connection.outputStream.use { os ->
                    os.write(requestBody.toByteArray())
                    os.flush()
                }

                // Process response
                val responseCode = connection.responseCode
                Log.d(TAG, "Solve submission response code: $responseCode")

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    Result.success(true)
                } else {
                    val errorResponse = BufferedReader(InputStreamReader(connection.errorStream)).use { it.readText() }
                    Log.e(TAG, "Solve submission error: $errorResponse")
                    Result.failure(Exception("Solve submission failed: $errorResponse"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Solve submission network error", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Retrieves the current leaderboard
     */
    suspend fun getLeaderboard(): Result<LeaderboardResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val baseUrl = getBaseUrl()
                Log.d(TAG, "Fetching leaderboard data")

                val url = URL("$baseUrl/api/leaderboard")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                addAuthHeader(connection)
                connection.setRequestProperty("Accept", "application/json")
                connection.connectTimeout = CONNECT_TIMEOUT
                connection.readTimeout = READ_TIMEOUT

                // Process response
                val responseCode = connection.responseCode
                Log.d(TAG, "Leaderboard response code: $responseCode")

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
                    val leaderboardResponse = json.decodeFromString<LeaderboardResponse>(response)
                    Result.success(leaderboardResponse)
                } else {
                    val errorResponse = BufferedReader(InputStreamReader(connection.errorStream)).use { it.readText() }
                    Log.e(TAG, "Leaderboard error: $errorResponse")
                    Result.failure(Exception("Failed to get leaderboard: $errorResponse"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Leaderboard network error", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Retrieves team messages
     */
    suspend fun getMessages(): Result<MessageResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val baseUrl = getBaseUrl()
                Log.d(TAG, "Fetching messages")

                val url = URL("$baseUrl/api/messages")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                addAuthHeader(connection)
                connection.setRequestProperty("Accept", "application/json")
                connection.connectTimeout = CONNECT_TIMEOUT
                connection.readTimeout = READ_TIMEOUT

                // Process response
                val responseCode = connection.responseCode
                Log.d(TAG, "Messages response code: $responseCode")

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
                    val messageResponse = json.decodeFromString<MessageResponse>(response)
                    Result.success(messageResponse)
                } else {
                    val errorResponse = BufferedReader(InputStreamReader(connection.errorStream)).use { it.readText() }
                    Log.e(TAG, "Messages error: $errorResponse")
                    Result.failure(Exception("Failed to get messages: $errorResponse"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Messages network error", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Sends a team message
     */
    suspend fun sendMessage(content: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val baseUrl = getBaseUrl()
                Log.d(TAG, "Sending message")

                val url = URL("$baseUrl/api/messages")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                addAuthHeader(connection)
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Accept", "application/json")
                connection.doOutput = true
                connection.connectTimeout = CONNECT_TIMEOUT
                connection.readTimeout = READ_TIMEOUT

                // Create request body
                val requestBody = """
                    {
                        "content": "$content"
                    }
                """.trimIndent()

                // Send request
                connection.outputStream.use { os ->
                    os.write(requestBody.toByteArray())
                    os.flush()
                }

                // Process response
                val responseCode = connection.responseCode
                Log.d(TAG, "Send message response code: $responseCode")

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    Result.success(true)
                } else {
                    val errorResponse = BufferedReader(InputStreamReader(connection.errorStream)).use { it.readText() }
                    Log.e(TAG, "Send message error: $errorResponse")
                    Result.failure(Exception("Failed to send message: $errorResponse"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Send message network error", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Triggers emergency lock
     */
    suspend fun emergencyLock(): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val baseUrl = getBaseUrl()
                Log.d(TAG, "Emergency lock request")

                val url = URL("$baseUrl/api/team/emergency-lock")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                addAuthHeader(connection)
                connection.setRequestProperty("Accept", "application/json")
                connection.connectTimeout = CONNECT_TIMEOUT
                connection.readTimeout = READ_TIMEOUT

                // Process response
                val responseCode = connection.responseCode
                Log.d(TAG, "Emergency lock response code: $responseCode")

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    Result.success(true)
                } else {
                    val errorResponse = BufferedReader(InputStreamReader(connection.errorStream)).use { it.readText() }
                    Log.e(TAG, "Emergency lock error: $errorResponse")
                    Result.failure(Exception("Failed to emergency lock: $errorResponse"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Emergency lock network error", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Tests server connection
     */
    suspend fun testServerConnection(): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val baseUrl = getBaseUrl()
                Log.d(TAG, "Testing connection to $baseUrl")

                val url = URL(baseUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = CONNECT_TIMEOUT
                connection.readTimeout = READ_TIMEOUT

                // Any response means the server is reachable
                val responseCode = connection.responseCode
                Log.d(TAG, "Server connection test response code: $responseCode")
                Result.success(true)
            } catch (e: Exception) {
                Log.e(TAG, "Server connection test failed", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Retrieves team members
     */
    suspend fun getTeamMembers(): Result<TeamMembersResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val baseUrl = getBaseUrl()
                Log.d(TAG, "Fetching team members")

                val url = URL("$baseUrl/api/team/members")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                addAuthHeader(connection)
                connection.setRequestProperty("Accept", "application/json")
                connection.connectTimeout = CONNECT_TIMEOUT
                connection.readTimeout = READ_TIMEOUT

                val responseCode = connection.responseCode
                Log.d(TAG, "Team members response code: $responseCode")

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
                    val teamMembersResponse = json.decodeFromString<TeamMembersResponse>(response)
                    Result.success(teamMembersResponse)
                } else {
                    val errorResponse = BufferedReader(InputStreamReader(connection.errorStream)).use { it.readText() }
                    Log.e(TAG, "Team members error: $errorResponse")
                    Result.failure(Exception("Failed to get team members: $errorResponse"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Team members network error", e)
                Result.failure(e)
            }
        }
    }
}
