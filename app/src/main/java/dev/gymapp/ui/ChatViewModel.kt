package dev.gymapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.gymapp.api.GymApi
import dev.gymapp.api.models.ChatHistoryMessage
import dev.gymapp.api.models.ChatRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

data class ChatMessage(
    val id: String,
    val role: String,
    val content: String
)

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
    val error: String? = null
)

class ChatViewModel(private val api: GymApi) : ViewModel() {

    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    private var messageIdCounter = 0
    private fun nextId() = "msg_${++messageIdCounter}"

    fun loadHistory(showLoading: Boolean = true) {
        viewModelScope.launch {
            if (showLoading) {
                _state.update { it.copy(isLoading = true, error = null) }
            }
            withContext(Dispatchers.IO) {
                runCatching {
                    val response = api.chatHistory()
                    if (response.isSuccessful) {
                        response.body()?.map { msg ->
                            ChatMessage(
                                id = nextId(),
                                role = msg.role,
                                content = msg.content
                            )
                        } ?: emptyList()
                    } else {
                        emptyList()
                    }
                }.getOrElse {
                    emptyList()
                }
            }.let { history ->
                messageIdCounter = history.size
                _state.update {
                    it.copy(
                        messages = history,
                        isLoading = false,
                        error = null
                    )
                }
            }
        }
    }

    fun sendText(text: String) {
        if (text.isBlank()) return
        sendMessage(ChatRequest(text = text.trim()))
    }

    fun sendVoice(audioBase64: String, audioFormat: String = "audio/wav") {
        sendMessage(
            ChatRequest(
                audioBase64 = audioBase64,
                audioFormat = audioFormat
            )
        )
    }

    private fun sendMessage(request: ChatRequest) {
        viewModelScope.launch {
            _state.update { it.copy(isSending = true, error = null) }

            withContext(Dispatchers.IO) {
                runCatching {
                    val response = api.chat(request)
                    if (response.isSuccessful) {
                        loadHistory(showLoading = false)
                    } else {
                        _state.update {
                            it.copy(
                                isSending = false,
                                error = "HTTP ${response.code()}"
                            )
                        }
                    }
                }.getOrElse { e ->
                    _state.update {
                        it.copy(
                            isSending = false,
                            error = e.message ?: "Request failed"
                        )
                    }
                }
            }
            _state.update { it.copy(isSending = false) }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
