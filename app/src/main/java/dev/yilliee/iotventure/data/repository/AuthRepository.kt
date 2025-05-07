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
    private val chatRepository: ChatRepository,
    private val gameRepository: GameRepository
) {

    companion object {
        private const val TAG = "AuthRepository"
    }

    /**
     * Attempts to log in a user with the provided credentials
     * and preloads all necessary data
     */
    suspend fun login(username: String, password: String): Result<LoginResponse> {
        return withContext(Dispatchers.IO) {
            Log.d(TAG, "Login attempt for user: $username")

            val deviceName = android.os.Build.MODEL
            val result = apiService.login(username, password, deviceName)

            if (result.isSuccess) {
                val response = result.getOrNull()
                if (response != null) {
                    Log.d(TAG, "Login successful, saving credentials")

                    // Save device token
                    response.deviceToken.let { token ->
                        preferencesManager.saveDeviceToken(token)
                        apiService.setDeviceToken(token)
                        Log.d(TAG, "Device token saved: $token")
                    }

                    // Save user credentials
                    preferencesManager.saveUsername(username)
                    preferencesManager.savePassword(password)
                    preferencesManager.saveTeamName(username)

                    // Save challenges if available
                    response.challenges?.let { challenges ->
                        Log.d(TAG, "Received ${challenges.size} challenges")
                        preferencesManager.saveChallenges(challenges)
                    }

                    // Fetch team solves to update solved challenges
                    Log.d(TAG, "Fetching team solves after login")
                    gameRepository.fetchTeamSolves()

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

                    // Set game start time if not already set
                    if (preferencesManager.getGameStartTime() == 0L) {
                        preferencesManager.setGameStartTime(System.currentTimeMillis())
                        Log.d(TAG, "Set initial game start time")
                    }
                } else {
                    Log.e(TAG, "Login response was null")
                    return@withContext Result.failure(Exception("Login response was null"))
                }
            } else {
                Log.e(TAG, "Login failed: ${result.exceptionOrNull()?.message}")
            }

            result
        }
    }

    /**
     * Logs out the current user and clears all local data
     */
    suspend fun logout(): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            Log.d(TAG, "Logout attempt")

            try {
                // Try to logout from server
                val result = apiService.logout()

                // Always clear local data regardless of server response
                Log.d(TAG, "Clearing all local data")
                preferencesManager.clearAllData()

                if (result.isSuccess) {
                    Log.d(TAG, "Logout successful")
                    Result.success(true)
                } else {
                    Log.e(TAG, "Logout from server failed: ${result.exceptionOrNull()?.message}")
                    // Return success anyway since we've cleared local data
                    Result.success(true)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception during logout: ${e.message}")
                // Even if server logout fails, clear local data
                preferencesManager.clearAllData()
                // Return success since we've cleared local data
                Result.success(true)
            }
        }
    }

    /**
     * Checks if the user is currently logged in
     */
    fun isLoggedIn(): Boolean {
        val hasToken = preferencesManager.getDeviceToken() != null
        val hasUsername = preferencesManager.getUsername() != null
        val hasPassword = preferencesManager.getPassword() != null

        val isLoggedIn = hasToken && hasUsername && hasPassword
        Log.d(TAG, "isLoggedIn check: $isLoggedIn (token: $hasToken, username: $hasUsername, password: $hasPassword)")
        return isLoggedIn
    }

    /**
     * Gets the current username
     */
    fun getUsername(): String? {
        return preferencesManager.getUsername()
    }
}
