package dev.yilliee.iotventure.data.repository

import android.util.Log
import dev.yilliee.iotventure.data.model.Challenge
import dev.yilliee.iotventure.data.model.NfcValidationResult
import dev.yilliee.iotventure.data.model.SolveSubmission
import dev.yilliee.iotventure.data.model.TeamSolve
import dev.yilliee.iotventure.data.model.UpdateLeaderboardRequest
import dev.yilliee.iotventure.data.remote.ApiService
import dev.yilliee.iotventure.data.local.PreferencesManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay

class GameRepository(
    private val preferencesManager: PreferencesManager,
    private val apiService: ApiService
) {
    companion object {
        private const val TAG = "GameRepository"
    }

    // State flows for reactive UI updates
    private val _nfcValidationResult = MutableStateFlow<NfcValidationResult?>(null)
    val nfcValidationResult: StateFlow<NfcValidationResult?> = _nfcValidationResult.asStateFlow()

    private val _selectedChallenge = MutableStateFlow<Challenge?>(null)
    val selectedChallenge: StateFlow<Challenge?> = _selectedChallenge.asStateFlow()

    private val _challenges = MutableStateFlow<List<Challenge>>(emptyList())
    val challenges: StateFlow<List<Challenge>> = _challenges.asStateFlow()

    private val _solvedChallenges = MutableStateFlow<Set<Int>>(emptySet())
    val solvedChallenges: StateFlow<Set<Int>> = _solvedChallenges.asStateFlow()

    private val _isEmergencyLocked = MutableStateFlow(false)
    val isEmergencyLocked: StateFlow<Boolean> = _isEmergencyLocked.asStateFlow()

    // New state flow for leaderboard refresh
    private val _leaderboardRefreshTrigger = MutableStateFlow(0)
    val leaderboardRefreshTrigger: StateFlow<Int> = _leaderboardRefreshTrigger.asStateFlow()

    init {
        // Initialize state from preferences
        _challenges.value = preferencesManager.getChallenges()
        _solvedChallenges.value = preferencesManager.getSolvedChallenges()
        _isEmergencyLocked.value = preferencesManager.isEmergencyLocked()
        Log.d(TAG, "Initialized with ${_challenges.value.size} challenges and ${_solvedChallenges.value.size} solved challenges")
    }

    // Add a new function to explicitly load challenges from preferences
    fun loadChallengesFromPreferences() {
        val currentSolvedChallenges = _solvedChallenges.value
        _challenges.value = preferencesManager.getChallenges()
        _solvedChallenges.value = preferencesManager.getSolvedChallenges()

        // Only log if there's a change in solved challenges
        if (currentSolvedChallenges != _solvedChallenges.value) {
            Log.d(TAG, "Solved challenges changed from $currentSolvedChallenges to ${_solvedChallenges.value}")
        }
    }

    /**
     * Gets all challenges
     */
    fun getChallenges(): Flow<List<Challenge>> = flow {
        val challenges = preferencesManager.getChallenges()
        _challenges.value = challenges // Update the StateFlow
        emit(challenges)
    }

    /**
     * Gets only unsolved challenges
     */
    fun getAvailableChallenges(): Flow<List<Challenge>> = flow {
        val allChallenges = preferencesManager.getChallenges()
        val solvedIds = preferencesManager.getSolvedChallenges()
        emit(allChallenges.filter { !solvedIds.contains(it.id) })
    }

    /**
     * Gets solved challenge IDs
     */
    fun getSolvedChallengesFlow(): Flow<Set<Int>> = flow {
        emit(preferencesManager.getSolvedChallenges())
    }

    /**
     * Gets a challenge by ID
     */
    suspend fun getChallengeById(id: Int): Challenge? {
        return preferencesManager.getChallenges().find { it.id == id }
    }

    /**
     * Selects a challenge for display
     */
    fun selectChallenge(challenge: Challenge) {
        _selectedChallenge.value = challenge
        Log.d(TAG, "Selected challenge: ${challenge.id} - ${challenge.name}")
    }

    /**
     * Clears the selected challenge
     */
    fun clearSelectedChallenge() {
        _selectedChallenge.value = null
        Log.d(TAG, "Cleared selected challenge")
    }

    /**
     * Adds a solved challenge to the queue for server submission
     */
    suspend fun addToSolveQueue(challenge: Challenge) {
        try {
            val currentQueue = preferencesManager.getSolveQueue().toMutableList()
            if (!currentQueue.any { it.id == challenge.id }) {
                currentQueue.add(challenge)
                preferencesManager.setSolveQueue(currentQueue)
                Log.d(TAG, "Added challenge ${challenge.id} to solve queue")
            }

            // Mark as solved locally
            val solvedChallenges = preferencesManager.getSolvedChallenges().toMutableSet()
            solvedChallenges.add(challenge.id)
            preferencesManager.setSolvedChallenges(solvedChallenges.toList())
            _solvedChallenges.value = solvedChallenges
            Log.d(TAG, "Marked challenge ${challenge.id} as solved locally")

            // Update challenges list to reflect solved status
            _challenges.value = preferencesManager.getChallenges()

            // Submit the solve to the server immediately
            try {
                submitSolveToServer(challenge)
            } catch (e: Exception) {
                Log.e(TAG, "Error submitting solve to server, will retry later: ${e.message}")
                // Don't rethrow - we'll retry later during submitSolves()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in addToSolveQueue: ${e.message}")
            // Don't rethrow to prevent app crashes
        }
    }

    /**
     * Submits a single solve to the server using the update-leaderboard endpoint
     */
    private suspend fun submitSolveToServer(challenge: Challenge) {
        try {
            val deviceToken = apiService.getDeviceToken()
            if (deviceToken == null) {
                Log.e(TAG, "Cannot submit solve: No device token available")
                return
            }

            val solveSubmission = SolveSubmission(
                challengeId = challenge.id,
                solvedAt = System.currentTimeMillis()
            )

            val request = UpdateLeaderboardRequest(
                deviceToken = deviceToken,
                solves = listOf(solveSubmission),
                isFinalSubmission = false
            )

            Log.d(TAG, "Submitting solve for challenge ${challenge.id} to update-leaderboard endpoint")
            val result = apiService.updateLeaderboard(request)

            if (result.isSuccess) {
                Log.d(TAG, "Successfully submitted solve for challenge ${challenge.id}")

                // Trigger leaderboard refresh
                try {
                    refreshLeaderboard()
                } catch (e: Exception) {
                    Log.e(TAG, "Error refreshing leaderboard: ${e.message}")
                }
            } else {
                Log.e(TAG, "Failed to submit solve for challenge ${challenge.id}: ${result.exceptionOrNull()?.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception submitting solve for challenge ${challenge.id}", e)
            // Don't rethrow to prevent app crashes
        }
    }

    /**
     * Submits all queued solves to the server
     */
    suspend fun submitSolves() {
        val solveQueue = preferencesManager.getSolveQueue()
        if (solveQueue.isEmpty()) {
            Log.d(TAG, "No solves in queue to submit")
            return
        }

        Log.d(TAG, "Submitting ${solveQueue.size} solves from queue")

        try {
            val deviceToken = apiService.getDeviceToken()
            if (deviceToken == null) {
                Log.e(TAG, "Cannot submit solves: No device token available")
                return
            }

            val solveSubmissions = solveQueue.map { challenge ->
                SolveSubmission(
                    challengeId = challenge.id,
                    solvedAt = System.currentTimeMillis()
                )
            }

            val request = UpdateLeaderboardRequest(
                deviceToken = deviceToken,
                solves = solveSubmissions,
                isFinalSubmission = false
            )

            val result = apiService.updateLeaderboard(request)

            if (result.isSuccess) {
                Log.d(TAG, "Successfully submitted all solves to server")
                preferencesManager.setSolveQueue(emptyList())

                // Trigger leaderboard refresh
                refreshLeaderboard()
            } else {
                Log.e(TAG, "Failed to submit solves: ${result.exceptionOrNull()?.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to submit solves", e)
            // Don't throw, just log the error
        }
    }

    /**
     * Triggers a leaderboard refresh
     */
    fun refreshLeaderboard() {
        try {
            _leaderboardRefreshTrigger.value = _leaderboardRefreshTrigger.value + 1
            Log.d(TAG, "Triggered leaderboard refresh: ${_leaderboardRefreshTrigger.value}")
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing leaderboard", e)
            // Don't throw, just log the error
        }
    }

    /**
     * Gets the current solve queue
     */
    fun getSolveQueue(): List<Challenge> {
        return preferencesManager.getSolveQueue()
    }

    /**
     * Sets the game start time
     */
    fun setGameStartTime() {
        preferencesManager.setGameStartTime(System.currentTimeMillis())
        Log.d(TAG, "Set game start time to ${System.currentTimeMillis()}")
    }

    /**
     * Gets the game start time
     */
    fun getGameStartTime(): Long {
        return preferencesManager.getGameStartTime()
    }

    /**
     * Triggers emergency lock
     */
    suspend fun emergencyLock() {
        try {
            Log.d(TAG, "Triggering emergency lock")
            // Save emergency lock state locally without server request
            preferencesManager.setEmergencyLocked(true)
            _isEmergencyLocked.value = true
            Log.d(TAG, "Emergency lock state saved locally")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to emergency lock", e)
            throw e
        }
    }

    /**
     * Checks if the game is in emergency lock state
     */
    fun isEmergencyLocked(): Boolean {
        return _isEmergencyLocked.value
    }

    /**
     * Clears emergency lock state
     */
    fun clearEmergencyLock() {
        preferencesManager.setEmergencyLocked(false)
        _isEmergencyLocked.value = false
        // Clear solve queue and solved challenges when unlocking
        preferencesManager.setSolveQueue(emptyList())
        preferencesManager.setSolvedChallenges(emptyList())
        _solvedChallenges.value = emptySet()
        Log.d(TAG, "Cleared emergency lock and game data")
    }

    /**
     * Validates an NFC tag against available challenges
     */
    suspend fun validateNfcTag(tagId: String) {
        Log.d(TAG, "Validating NFC tag: $tagId")

        // Set initial state to Processing
        _nfcValidationResult.value = NfcValidationResult.Processing

        try {
            val challenges = preferencesManager.getChallenges()

            // First check if it's already solved to avoid unnecessary server calls
            val solvedChallenges = preferencesManager.getSolvedChallenges()
            val matchingChallenge = challenges.find { it.keyHash == tagId }

            if (matchingChallenge != null) {
                if (solvedChallenges.contains(matchingChallenge.id)) {
                    Log.d(TAG, "Challenge ${matchingChallenge.id} already solved")
                    _nfcValidationResult.value = NfcValidationResult.AlreadySolved
                    return
                }

                Log.d(TAG, "Valid challenge found: ${matchingChallenge.id} - ${matchingChallenge.name}")
                _nfcValidationResult.value = NfcValidationResult.Valid(matchingChallenge)
            } else {
                // If not found locally, we could optionally check with server here
                // For now, just return invalid
                Log.d(TAG, "No matching challenge found for tag")
                _nfcValidationResult.value = NfcValidationResult.Invalid
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error validating NFC tag", e)
            // Keep the Processing state to let the timeout handle it
            // or set to Invalid if you want immediate feedback
            _nfcValidationResult.value = NfcValidationResult.Invalid
        }
    }

    /**
     * Clears the NFC validation result
     */
    fun clearNfcValidationResult() {
        _nfcValidationResult.value = null
        Log.d(TAG, "Cleared NFC validation result")
    }

    /**
     * Calculates the completion percentage
     */
    fun getCompletionPercentage(): Int {
        val total = preferencesManager.getChallenges().size
        if (total == 0) return 0

        val solved = preferencesManager.getSolvedChallenges().size
        val percentage = if (total == solved) 100 else ((solved.toDouble() * 100) / total).toInt()
        Log.d(TAG, "Completion percentage: $percentage% ($solved/$total)")
        return percentage
    }

    /**
     * Tests server connection
     */
    suspend fun testServerConnection(): Result<Boolean> {
        return try {
            Log.d(TAG, "Testing server connection")
            apiService.testServerConnection()
        } catch (e: Exception) {
            Log.e(TAG, "Server connection test failed", e)
            Result.failure(e)
        }
    }
}
