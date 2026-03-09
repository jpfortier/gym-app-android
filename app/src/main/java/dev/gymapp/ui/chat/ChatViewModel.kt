package dev.gymapp.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.gymapp.api.ErrorBodyParser
import dev.gymapp.api.GymApi
import dev.gymapp.api.models.ApiError
import dev.gymapp.api.models.ChatRequest
import dev.gymapp.api.models.ChatResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ChatMessage(
    val id: String,
    val role: ChatRole,
    val content: String,
    val response: ChatResponse? = null
)

enum class ChatRole {
    USER,
    ASSISTANT
}

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val inputMode: InputMode = InputMode.VOICE,
    val lastError: ApiError? = null
)

enum class InputMode {
    VOICE,
    TEXT
}

class ChatViewModel(private val api: GymApi) : ViewModel() {

    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    private var messageIdCounter = 0
    private fun nextId() = "msg_${++messageIdCounter}"

    fun sendText(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val userMsg = ChatMessage(nextId(), ChatRole.USER, text)
            _state.update { it.copy(messages = it.messages + userMsg) }

            val (chatResponse, apiError) = withContext(Dispatchers.IO) {
                runCatching {
                    val response = api.chat(ChatRequest(text = text.trim()))
                    if (response.isSuccessful) {
                        response.body()!! to null
                    } else {
                        null to ErrorBodyParser.parse(response.errorBody(), response.code())
                    }
                }.getOrElse {
                    null to ApiError(
                        error = it.message ?: "Unknown error",
                        code = "?",
                        errorToken = null
                    )
                }
            }

            _state.update { it.copy(isLoading = false) }
            if (chatResponse != null) {
                val displayText = formatResponse(chatResponse)
                val assistantMsg = ChatMessage(nextId(), ChatRole.ASSISTANT, displayText, chatResponse)
                _state.update { it.copy(messages = it.messages + assistantMsg, lastError = null) }
            } else if (apiError != null) {
                _state.update { it.copy(lastError = apiError) }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(lastError = null) }
    }

    fun setInputMode(mode: InputMode) {
        _state.update { it.copy(inputMode = mode) }
    }

    fun sendAudio(audioBase64: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val userMsg = ChatMessage(nextId(), ChatRole.USER, "[Voice message]")
            _state.update { it.copy(messages = it.messages + userMsg) }

            val (chatResponse, apiError) = withContext(Dispatchers.IO) {
                runCatching {
                    val response = api.chat(
                        ChatRequest(audioBase64 = audioBase64, audioFormat = "m4a")
                    )
                    if (response.isSuccessful) {
                        response.body()!! to null
                    } else {
                        null to ErrorBodyParser.parse(response.errorBody(), response.code())
                    }
                }.getOrElse {
                    null to ApiError(
                        error = it.message ?: "Unknown error",
                        code = "?",
                        errorToken = null
                    )
                }
            }

            _state.update { it.copy(isLoading = false) }
            if (chatResponse != null) {
                val displayText = formatResponse(chatResponse)
                val assistantMsg = ChatMessage(nextId(), ChatRole.ASSISTANT, displayText, chatResponse)
                _state.update { it.copy(messages = it.messages + assistantMsg, lastError = null) }
            } else if (apiError != null) {
                _state.update { it.copy(lastError = apiError) }
            }
        }
    }

    private fun formatResponse(r: ChatResponse): String {
        val msg = r.message ?: ""
        return when (r.intent) {
            "log" -> msg + (r.prs?.takeIf { it.isNotEmpty() }?.let { "\n${it.size} new PR(s)!" } ?: "")
            "query" -> r.history?.let { h ->
                msg + "\n" + h.entries.joinToString("\n") { e ->
                    "${e.sessionDate}: ${e.rawSpeech ?: ""} - ${e.sets.joinToString { "${it.weight ?: ""}x${it.reps}" }}"
                }
            } ?: msg
            else -> msg
        }.trim()
    }
}
