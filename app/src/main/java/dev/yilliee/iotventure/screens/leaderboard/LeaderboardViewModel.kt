package dev.yilliee.iotventure.screens.leaderboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.yilliee.iotventure.data.model.LeaderboardResponse
import dev.yilliee.iotventure.data.repository.GameRepository
import dev.yilliee.iotventure.data.remote.ApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LeaderboardViewModel(
    private val apiService: ApiService,
    private val gameRepository: GameRepository
) : ViewModel() {

    private val _leaderboardData = MutableStateFlow<LeaderboardResponse?>(null)
    val leaderboardData: StateFlow<LeaderboardResponse?> = _leaderboardData.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadLeaderboard()

        // Observe leaderboard refresh triggers
        viewModelScope.launch {
            gameRepository.leaderboardRefreshTrigger.collect {
                if (it > 0) {
                    loadLeaderboard()
                }
            }
        }
    }

    fun loadLeaderboard() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val result = apiService.getLeaderboard()
                if (result.isSuccess) {
                    _leaderboardData.value = result.getOrNull()
                } else {
                    _error.value = result.exceptionOrNull()?.message ?: "Failed to load leaderboard"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "An error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refreshLeaderboard() {
        loadLeaderboard()
    }
}
