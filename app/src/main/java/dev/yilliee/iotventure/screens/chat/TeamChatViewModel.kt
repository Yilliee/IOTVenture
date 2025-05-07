package dev.yilliee.iotventure.screens.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.yilliee.iotventure.data.model.TeamMessage
import dev.yilliee.iotventure.data.repository.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TeamChatViewModel(private val chatRepository: ChatRepository) : ViewModel() {

    companion object {
        private const val TAG = "TeamChatViewModel"
    }

    private val _chatState = MutableStateFlow<ChatState>(ChatState.Loading)
    val chatState: StateFlow<ChatState> = _chatState

    init {
        // Load local messages immediately
        val localMessages = chatRepository.getLocalMessages()
        _chatState.value = ChatState.Success(localMessages)
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        viewModelScope.launch {
            Log.d(TAG, "Sending message: $text")
            val newMessage = chatRepository.addLocalMessage(text)

            // Update the UI with the new message
            val currentMessages = when (val state = _chatState.value) {
                is ChatState.Success -> state.messages
                is ChatState.NetworkError -> state.cachedMessages
                else -> emptyList()
            }

            _chatState.value = ChatState.Success(currentMessages + newMessage)
        }
    }

    sealed class ChatState {
        object Loading : ChatState()
        data class Success(val messages: List<TeamMessage>) : ChatState()
        data class NetworkError(val cachedMessages: List<TeamMessage>) : ChatState()
    }

    class Factory(private val chatRepository: ChatRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TeamChatViewModel::class.java)) {
                return TeamChatViewModel(chatRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
