package dev.yilliee.iotventure.data.remote

import android.util.Log
import dev.yilliee.iotventure.data.model.LoginResponse
import dev.yilliee.iotventure.data.model.MessageResponse
import dev.yilliee.iotventure.data.model.LeaderboardResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.serialization.encodeToString

class ApiService {
    companion object {
        private const val TAG = "ApiService"
        private const val CONNECT_TIMEOUT = 10000
        private const val READ_TIMEOUT = 10000
    }

    private val json = Json { 
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }
    private var serverIp: String = "127.0.0.1"
    private var serverPort: String = "3000"
    private var deviceToken: String? = null

    // Update server settings
    fun updateServerSettings(ip: String, port: String) {
        serverIp = ip
        serverPort = port
        Log.d(TAG, "Server settings updated to $serverIp:$serverPort")
    }

    fun setDeviceToken(token: String) {
        deviceToken = token
    }

    // Get base URL using current server settings
    private fun getBaseUrl(): String {
        return "http://$serverIp:$serverPort"
    }

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

                val requestBody = json.encodeToString(
                    mapOf(
                        "username" to username,
                        "password" to password,
                        "device_name" to deviceName
                    )
                )

                Log.d(TAG, "Request body: $requestBody")

                connection.outputStream.use { os ->
                    os.write(requestBody.toByteArray())
                    os.flush()
                }

                val responseCode = connection.responseCode
                Log.d(TAG, "Login response code: $responseCode")

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
                    Log.d(TAG, "Login success response: $response")
                    try {
                        val loginResponse = json.decodeFromString<LoginResponse>(response)
                        deviceToken = loginResponse.deviceToken
                        Result.success(loginResponse)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to parse login response: ${e.message}", e)
                        Result.failure(e)
                    }
                } else {
                    val errorResponse = BufferedReader(InputStreamReader(connection.errorStream)).use { it.readText() }
                    Log.e(TAG, "Login error response: $errorResponse")
                    Result.failure(Exception("Login failed: $errorResponse"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Login network error: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    suspend fun logout(username: String, password: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val baseUrl = getBaseUrl()
                val url = URL("$baseUrl/api/team/logout")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Authorization", "Bearer $deviceToken")
                connection.setRequestProperty("Accept", "application/json")

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    deviceToken = null
                    Result.success(true)
                } else {
                    Result.failure(Exception("Logout failed with code: $responseCode"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun submitSolve(deviceToken: String, challengeId: Int, keyHash: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val baseUrl = getBaseUrl()
                val url = URL("$baseUrl/api/team/solve")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Authorization", "Bearer $deviceToken")
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Accept", "application/json")
                connection.doOutput = true
                connection.connectTimeout = CONNECT_TIMEOUT
                connection.readTimeout = READ_TIMEOUT

                val requestBody = """
                    {
                        "challengeId": $challengeId,
                        "keyHash": "$keyHash"
                    }
                """.trimIndent()

                connection.outputStream.use { os ->
                    os.write(requestBody.toByteArray())
                    os.flush()
                }

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    Result.success(true)
                } else {
                    val errorResponse = BufferedReader(InputStreamReader(connection.errorStream)).use { it.readText() }
                    Result.failure(Exception("Solve submission failed: $errorResponse"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun getLeaderboard(deviceToken: String): Result<LeaderboardResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val baseUrl = getBaseUrl()
                val url = URL("$baseUrl/api/leaderboard")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Authorization", "Bearer $deviceToken")
                connection.setRequestProperty("Accept", "application/json")
                connection.connectTimeout = CONNECT_TIMEOUT
                connection.readTimeout = READ_TIMEOUT

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
                    val leaderboardResponse = json.decodeFromString<LeaderboardResponse>(response)
                    Result.success(leaderboardResponse)
                } else {
                    val errorResponse = BufferedReader(InputStreamReader(connection.errorStream)).use { it.readText() }
                    Result.failure(Exception("Failed to get leaderboard: $errorResponse"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun getMessages(deviceToken: String): Result<MessageResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val baseUrl = getBaseUrl()
                val url = URL("$baseUrl/api/messages")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Authorization", "Bearer $deviceToken")
                connection.setRequestProperty("Accept", "application/json")
                connection.connectTimeout = CONNECT_TIMEOUT
                connection.readTimeout = READ_TIMEOUT

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
                    val messageResponse = json.decodeFromString<MessageResponse>(response)
                    Result.success(messageResponse)
                } else {
                    val errorResponse = BufferedReader(InputStreamReader(connection.errorStream)).use { it.readText() }
                    Result.failure(Exception("Failed to get messages: $errorResponse"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun sendMessage(deviceToken: String, content: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val baseUrl = getBaseUrl()
                val url = URL("$baseUrl/api/messages")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Authorization", "Bearer $deviceToken")
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Accept", "application/json")
                connection.doOutput = true
                connection.connectTimeout = CONNECT_TIMEOUT
                connection.readTimeout = READ_TIMEOUT

                val requestBody = """
                    {
                        "content": "$content"
                    }
                """.trimIndent()

                connection.outputStream.use { os ->
                    os.write(requestBody.toByteArray())
                    os.flush()
                }

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    Result.success(true)
                } else {
                    val errorResponse = BufferedReader(InputStreamReader(connection.errorStream)).use { it.readText() }
                    Result.failure(Exception("Failed to send message: $errorResponse"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun emergencyLock(deviceToken: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val baseUrl = getBaseUrl()
                val url = URL("$baseUrl/api/team/emergency-lock")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Authorization", "Bearer $deviceToken")
                connection.setRequestProperty("Accept", "application/json")
                connection.connectTimeout = CONNECT_TIMEOUT
                connection.readTimeout = READ_TIMEOUT

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    Result.success(true)
                } else {
                    val errorResponse = BufferedReader(InputStreamReader(connection.errorStream)).use { it.readText() }
                    Result.failure(Exception("Failed to emergency lock: $errorResponse"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // Add a new method for updating the leaderboard
    suspend fun updateLeaderboard(deviceToken: String, solves: List<Map<String, Any>>, isFinalSubmission: Boolean = false): Result<Boolean> {
        return try {
            Log.d(TAG, "Updating leaderboard with token: $deviceToken")

            val requestBody = json.encodeToString(mapOf(
                "deviceToken" to deviceToken,
                "solves" to solves,
                "isFinalSubmission" to isFinalSubmission
            ))

            val url = URL("${getBaseUrl()}/update-leaderboard")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.doInput = true
            connection.connectTimeout = CONNECT_TIMEOUT
            connection.readTimeout = READ_TIMEOUT
            connection.setRequestProperty("Content-Type", "application/json")

            // Write request body
            connection.outputStream.use { os ->
                os.write(requestBody.toByteArray())
                os.flush()
            }

            val responseCode = connection.responseCode
            Log.d(TAG, "Update leaderboard response code: $responseCode")

            if (responseCode in 200..299) {
                Result.success(true)
            } else {
                // Error response
                val errorResponse = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "{}"
                Log.e(TAG, "Update leaderboard error response: $errorResponse")

                Result.failure(IOException("API error: Error updating leaderboard"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Update leaderboard network error: ${e.message}", e)
            Result.failure(IOException("Network error: ${e.message}", e))
        }
    }
}
