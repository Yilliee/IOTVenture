package dev.yilliee.iotventure.screens.login

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.yilliee.iotventure.data.model.LoginResponse
import dev.yilliee.iotventure.data.repository.AuthRepository
import dev.yilliee.iotventure.data.repository.GameRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LoginViewModel(
    private val authRepository: AuthRepository,
    private val gameRepository: GameRepository
) : ViewModel() {

    companion object {
        private const val TAG = "LoginViewModel"
    }

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState

    fun login(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            _loginState.value = LoginState.Error("Please enter username and password to connect to server")
            return
        }

        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                Log.d(TAG, "Starting login process for user: $username")
                val result = authRepository.login(username, password)
                result.fold(
                    onSuccess = { response ->
                        Log.d(TAG, "Login successful, device token: ${response.deviceToken}")

                        // Preload challenges data
                        try {
                            gameRepository.getChallenges().collect { challenges ->
                                Log.d(TAG, "Preloaded ${challenges.size} challenges")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error preloading challenges", e)
                        }

                        _loginState.value = LoginState.Success(response)
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Login failed: ${error.message}", error)
                        _loginState.value = LoginState.Error(error.message ?: "Unknown error occurred")
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error during login: ${e.message}", e)
                _loginState.value = LoginState.Error(e.message ?: "An unexpected error occurred")
            }
        }
    }

    fun resetState() {
        _loginState.value = LoginState.Idle
    }

    // Add a method to test server connection
    fun testServerConnection() {
        viewModelScope.launch {
            _loginState.value = LoginState.Testing
            try {
                val result = gameRepository.testServerConnection()

                if (result.isSuccess) {
                    _loginState.value = LoginState.ConnectionSuccess
                    Log.d(TAG, "Server connection test successful")
                } else {
                    _loginState.value = LoginState.ConnectionError(result.exceptionOrNull()?.message ?: "Unknown error")
                    Log.e(TAG, "Server connection test failed: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                _loginState.value = LoginState.ConnectionError(e.message ?: "Unknown error")
                Log.e(TAG, "Server connection test exception: ${e.message}", e)
            }
        }
    }

    // Update the LoginState sealed class to include connection testing states
    sealed class LoginState {
        object Idle : LoginState()
        object Loading : LoginState()
        object Testing : LoginState()
        object ConnectionSuccess : LoginState()
        data class ConnectionError(val message: String) : LoginState()
        data class Success(val response: LoginResponse) : LoginState()
        data class Error(val message: String) : LoginState()
    }

    class Factory(
        private val authRepository: AuthRepository,
        private val gameRepository: GameRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
                return LoginViewModel(authRepository, gameRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
