package dev.yilliee.iotventure.data.repository

import android.util.Log
import dev.yilliee.iotventure.data.local.PreferencesManager
import dev.yilliee.iotventure.data.model.LoginResponse
import dev.yilliee.iotventure.data.remote.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepository(
    private val apiService: ApiService,
    private val preferencesManager: PreferencesManager,
    private val chatRepository: ChatRepository
) {

    companion object {
        private const val TAG = "AuthRepository"
    }

    suspend fun login(username: String, password: String): Result<LoginResponse> {
        return withContext(Dispatchers.IO) {
            Log.d(TAG, "Login attempt for user: $username")

            val deviceName = android.os.Build.MODEL
            val result = apiService.login(username, password, deviceName)

            if (result.isSuccess) {
                val response = result.getOrNull()
                response?.deviceToken?.let { token ->
                    Log.d(TAG, "Login successful, saving credentials")
                    preferencesManager.saveDeviceToken(token)
                    preferencesManager.saveUsername(username)
                    preferencesManager.savePassword(password) // Store password for reconnection
                    preferencesManager.saveTeamName(username) // Using username as team name for now

                    // Save challenges if available
                    response.challenges?.let { challenges ->
                        Log.d(TAG, "Received ${challenges.size} challenges")
                        preferencesManager.saveChallenges(challenges)
                    }

                    // Fetch and store messages
                    Log.d(TAG, "Fetching messages after login")
                    chatRepository.fetchMessages().fold(
                        onSuccess = { messages ->
                            Log.d(TAG, "Successfully fetched ${messages.size} messages")
                        },
                        onFailure = { error ->
                            Log.e(TAG, "Failed to fetch messages: ${error.message}")
                        }
                    )
                }
            } else {
                Log.e(TAG, "Login failed: ${result.exceptionOrNull()?.message}")
            }

            result
        }
    }

    suspend fun logout(): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            val username = preferencesManager.getUsername()
            val password = preferencesManager.getPassword()

            if (username == null || password == null) {
                Log.e(TAG, "Cannot logout: missing credentials")
                return@withContext Result.failure(Exception("Not logged in"))
            }

            Log.d(TAG, "Logout attempt for user: $username")

            val result = apiService.logout(username, password)

            if (result.isSuccess) {
                Log.d(TAG, "Logout successful, clearing data")
                preferencesManager.clearAllData()
            } else {
                Log.e(TAG, "Logout failed: ${result.exceptionOrNull()?.message}")
                // Even if server logout fails, clear local data
                preferencesManager.clearAllData()
            }

            result
        }
    }

    fun isLoggedIn(): Boolean {
        val hasToken = preferencesManager.getDeviceToken() != null
        val hasUsername = preferencesManager.getUsername() != null
        val hasPassword = preferencesManager.getPassword() != null

        return hasToken && hasUsername && hasPassword
    }

    fun getUsername(): String? {
        return preferencesManager.getUsername()
    }
}
