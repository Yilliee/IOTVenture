package dev.yilliee.iotventure.data.repository

import android.util.Log
import dev.yilliee.iotventure.data.model.Challenge
import dev.yilliee.iotventure.data.model.NfcValidationResult
import dev.yilliee.iotventure.data.remote.ApiService
import dev.yilliee.iotventure.data.local.PreferencesManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flow

class GameRepository(
    private val preferencesManager: PreferencesManager,
    private val apiService: ApiService
) {
    companion object {
        private const val TAG = "GameRepository"
    }

    private val _nfcValidationResult = MutableStateFlow<NfcValidationResult?>(null)
    val nfcValidationResult: StateFlow<NfcValidationResult?> = _nfcValidationResult.asStateFlow()

    private val _selectedChallenge = MutableStateFlow<Challenge?>(null)
    val selectedChallenge: StateFlow<Challenge?> = _selectedChallenge.asStateFlow()

    private val _challenges = MutableStateFlow<List<Challenge>>(emptyList())
    val challenges: StateFlow<List<Challenge>> = _challenges.asStateFlow()

    init {
        _challenges.value = preferencesManager.getChallenges()
    }

    fun getChallenges(): Flow<List<Challenge>> = flow {
        emit(preferencesManager.getChallenges())
    }

    suspend fun getChallengeById(id: Int): Challenge? {
        return preferencesManager.getChallenges().find { it.id == id }
    }

    fun selectChallenge(challenge: Challenge) {
        _selectedChallenge.value = challenge
    }

    fun clearSelectedChallenge() {
        _selectedChallenge.value = null
    }

    suspend fun addToSolveQueue(challenge: Challenge) {
        val currentQueue = preferencesManager.getSolveQueue().toMutableList()
        if (!currentQueue.any { it.id == challenge.id }) {
            currentQueue.add(challenge)
            preferencesManager.setSolveQueue(currentQueue)
        }
    }

    suspend fun submitSolves() {
        val solveQueue = preferencesManager.getSolveQueue()
        if (solveQueue.isEmpty()) return

        try {
            val deviceToken = preferencesManager.getDeviceToken()
            if (deviceToken == null) {
                Log.e(TAG, "No device token found")
                return
            }

            for (challenge in solveQueue) {
                try {
                    apiService.submitSolve(deviceToken, challenge.id, challenge.keyHash)
                    // Add to solved challenges and remove from queue
                    val solvedChallenges = preferencesManager.getSolvedChallenges().toMutableList()
                    solvedChallenges.add(challenge.id)
                    preferencesManager.setSolvedChallenges(solvedChallenges)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to submit solve for challenge ${challenge.id}", e)
                }
            }

            // Clear solve queue after successful submission
            preferencesManager.setSolveQueue(emptyList())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to submit solves", e)
        }
    }

    fun setGameStartTime() {
        preferencesManager.setGameStartTime(System.currentTimeMillis())
    }

    fun getGameStartTime(): Long {
        return preferencesManager.getGameStartTime()
    }

    suspend fun emergencyLock() {
        try {
            val deviceToken = preferencesManager.getDeviceToken()
            if (deviceToken != null) {
                apiService.emergencyLock(deviceToken)
                // Clear solve queue and solved challenges
                preferencesManager.setSolveQueue(emptyList())
                preferencesManager.setSolvedChallenges(emptyList())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to emergency lock", e)
        }
    }

    suspend fun validateNfcTag(keyHash: String) {
        val challenges = preferencesManager.getChallenges()
        val challenge = challenges.find { it.keyHash == keyHash }
        if (challenge != null) {
            val solvedChallenges = preferencesManager.getSolvedChallenges()
            if (solvedChallenges.contains(challenge.id)) {
                _nfcValidationResult.value = NfcValidationResult.AlreadySolved
            } else {
                _nfcValidationResult.value = NfcValidationResult.Valid(challenge)
            }
        } else {
            _nfcValidationResult.value = NfcValidationResult.Invalid
        }
    }

    fun clearNfcValidationResult() {
        _nfcValidationResult.value = null
    }
} 