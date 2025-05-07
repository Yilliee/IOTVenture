package dev.yilliee.iotventure.data.repository

import android.util.Log
import dev.yilliee.iotventure.data.model.Challenge
import dev.yilliee.iotventure.data.model.NfcValidationResult
import dev.yilliee.iotventure.data.model.TeamSolve
import dev.yilliee.iotventure.data.remote.ApiService
import dev.yilliee.iotventure.data.local.PreferencesManager
import dev.yilliee.iotventure.data.model.TeamMember
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

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

    init {
        // Initialize state from preferences
        _challenges.value = preferencesManager.getChallenges()
        _solvedChallenges.value = preferencesManager.getSolvedChallenges()
        Log.d(TAG, "Initialized with ${_challenges.value.size} challenges and ${_solvedChallenges.value.size} solved challenges")
    }

    // Add a new function to explicitly load challenges from preferences
    fun loadChallengesFromPreferences() {
        _challenges.value = preferencesManager.getChallenges()
        _solvedChallenges.value = preferencesManager.getSolvedChallenges()
        Log.d(TAG, "Explicitly loaded ${_challenges.value.size} challenges and ${_solvedChallenges.value.size} solved challenges")
    }

    /**
     * Fetches team solves from the server and updates local state
     */
    suspend fun fetchTeamSolves() {
        try {
            Log.d(TAG, "Fetching team solves from server")
            val result = apiService.getTeamSolves()

            if (result.isSuccess) {
                val response = result.getOrNull()
                val teamSolves = response?.solves ?: emptyList()
                Log.d(TAG, "Received ${teamSolves.size} team solves from server")

                if (teamSolves.isNotEmpty()) {
                    // Extract challenge IDs from team solves
                    val solvedChallengeIds = teamSolves.map { it.challengeId }.toSet()

                    // Update solved challenges in preferences
                    preferencesManager.setSolvedChallenges(solvedChallengeIds.toList())

                    // Update state flow
                    _solvedChallenges.value = solvedChallengeIds

                    // Reload challenges to ensure UI is updated
                    loadChallengesFromPreferences()

                    Log.d(TAG, "Updated solved challenges with team solves: $solvedChallengeIds")
                }
            } else {
                Log.e(TAG, "Error fetching team solves: ${result.exceptionOrNull()?.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception fetching team solves", e)
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
            var successCount = 0
            val failedSolves = mutableListOf<Challenge>()

            for (challenge in solveQueue) {
                try {
                    Log.d(TAG, "Submitting solve for challenge ${challenge.id} with key hash ${challenge.keyHash}")
                    val result = apiService.submitSolve(challenge.id, challenge.keyHash)
                    if (result.isSuccess) {
                        successCount++
                        Log.d(TAG, "Successfully submitted solve for challenge ${challenge.id}")
                    } else {
                        Log.e(TAG, "Failed to submit solve for challenge ${challenge.id}: ${result.exceptionOrNull()?.message}")
                        failedSolves.add(challenge)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Exception submitting solve for challenge ${challenge.id}", e)
                    failedSolves.add(challenge)
                }
            }

            // Only keep failed solves in the queue
            if (failedSolves.isNotEmpty()) {
                Log.d(TAG, "Keeping ${failedSolves.size} failed solves in queue for retry")
                preferencesManager.setSolveQueue(failedSolves)
            } else {
                Log.d(TAG, "All solves submitted successfully, clearing queue")
                preferencesManager.setSolveQueue(emptyList())
            }

            Log.d(TAG, "Successfully submitted $successCount solves to server")

            // After submitting solves, fetch team solves to ensure we have the latest data
            fetchTeamSolves()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to submit solves", e)
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
            val result = apiService.emergencyLock()
            if (result.isSuccess) {
                Log.d(TAG, "Emergency lock successful")
            } else {
                Log.e(TAG, "Emergency lock failed: ${result.exceptionOrNull()?.message}")
            }

            // Clear solve queue and solved challenges regardless of server response
            preferencesManager.setSolveQueue(emptyList())
            preferencesManager.setSolvedChallenges(emptyList())
            _solvedChallenges.value = emptySet()
            Log.d(TAG, "Cleared solve queue and solved challenges")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to emergency lock", e)
        }
    }

    /**
     * Validates an NFC tag against available challenges
     */
    suspend fun validateNfcTag(tagId: String) {
        Log.d(TAG, "Validating NFC tag: $tagId")
        val challenges = preferencesManager.getChallenges()

        // Find a challenge with matching key hash
        val challenge = challenges.find { it.keyHash == tagId }

        if (challenge != null) {
            val solvedChallenges = preferencesManager.getSolvedChallenges()

            if (solvedChallenges.contains(challenge.id)) {
                Log.d(TAG, "Challenge ${challenge.id} already solved")
                _nfcValidationResult.value = NfcValidationResult.AlreadySolved
            } else {
                Log.d(TAG, "Valid challenge found: ${challenge.id} - ${challenge.name}")
                _nfcValidationResult.value = NfcValidationResult.Valid(challenge)
            }
        } else {
            Log.d(TAG, "No matching challenge found for tag")
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
        val percentage = (solved * 100) / total
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

    suspend fun getTeamMembers(): List<TeamMember> {
        return withContext(Dispatchers.IO) {
            try {
                val result = apiService.getTeamMembers()
                if (result.isSuccess) {
                    result.getOrNull()?.members?.map { member ->
                        TeamMember(
                            username = member.username,
                            lastActive = member.lastActive,
                            solvedChallenges = member.solvedChallenges,
                            isCurrentUser = false
                        )
                    } ?: emptyList()
                } else {
                    Log.e(TAG, "Error fetching team members: ${result.exceptionOrNull()?.message}")
                    emptyList()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching team members", e)
                emptyList()
            }
        }
    }
}
