package dev.yilliee.iotventure.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.yilliee.iotventure.data.model.Challenge
import dev.yilliee.iotventure.data.repository.GameRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MapViewModel(
    private val gameRepository: GameRepository
) : ViewModel() {

    private val _challenges = MutableStateFlow<List<Challenge>>(emptyList())
    val challenges: StateFlow<List<Challenge>> = _challenges.asStateFlow()

    val selectedChallenge: StateFlow<Challenge?> = gameRepository.selectedChallenge

    init {
        viewModelScope.launch {
            gameRepository.getChallenges().collect { challenges ->
                _challenges.value = challenges
            }
        }
    }

    fun loadChallenges() {
        viewModelScope.launch {
            gameRepository.getChallenges().collect { challenges ->
                _challenges.value = challenges
            }
        }
    }

    fun selectChallenge(challenge: Challenge) {
        gameRepository.selectChallenge(challenge)
    }

    fun clearSelectedChallenge() {
        gameRepository.clearSelectedChallenge()
    }
} 