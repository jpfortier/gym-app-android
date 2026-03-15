package dev.gymapp.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.gymapp.api.ChatPostResult
import dev.gymapp.api.ChatRepository
import dev.gymapp.api.GymApi
import dev.gymapp.api.PrImageLoader
import dev.gymapp.api.models.ApiError
import dev.gymapp.api.models.ChatRequest
import dev.gymapp.api.models.ChatResponse
import dev.gymapp.api.models.PersonalRecord
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
    val response: ChatResponse? = null,
    val isPlaceholder: Boolean = false
)

enum class ChatRole {
    USER,
    ASSISTANT
}

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val lastError: ApiError? = null,
    val pendingPrModal: List<PrWithImage>? = null
)

class ChatViewModel(
    private val api: GymApi,
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val imageLoader = PrImageLoader(api)
    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    private var messageIdCounter = 0
    private fun nextId() = "msg_${++messageIdCounter}"

    init {
        loadChatHistory()
    }

    fun loadChatHistory() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val response = api.chatMessages(limit = 50)
                    if (response.isSuccessful) {
                        response.body()?.messages?.map { dto ->
                            ChatMessage(
                                id = dto.id,
                                role = if (dto.role == "user") ChatRole.USER else ChatRole.ASSISTANT,
                                content = dto.content
                            )
                        } ?: emptyList()
                    } else {
                        null
                    }
                }.getOrNull()
            }
            _state.update {
                it.copy(
                    messages = result ?: it.messages,
                    isLoading = false
                )
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(lastError = null) }
    }

    fun dismissPrModal() {
        _state.update { it.copy(pendingPrModal = null) }
    }

    fun sendAudio(audioBase64: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val request = ChatRequest(audioBase64 = audioBase64, audioFormat = "m4a")

            when (val result = withContext(Dispatchers.IO) {
                chatRepository.postChat(request)
            }) {
                is kotlin.Result.Success -> when (val postResult = result.getOrThrow()) {
                    is ChatPostResult.Sync -> handleSyncResponse(postResult.response)
                    is ChatPostResult.Async -> handleAsyncResponse(postResult.job)
                }
                is kotlin.Result.Failure -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            lastError = ApiError(
                                error = result.exceptionOrNull()?.message ?: "Unknown error",
                                code = "?",
                                errorToken = null
                            )
                        )
                    }
                }
            }
        }
    }

    private suspend fun handleSyncResponse(chatResponse: ChatResponse) {
        val userMsg = ChatMessage(nextId(), ChatRole.USER, "[Voice message]")
        val assistantMsg = ChatMessage(
            id = nextId(),
            role = ChatRole.ASSISTANT,
            content = formatResponse(chatResponse),
            response = chatResponse
        )
        _state.update {
            it.copy(
                messages = it.messages + userMsg + assistantMsg,
                isLoading = false,
                lastError = null
            )
        }
        loadChatHistory()
        showPrModalIfNeeded(chatResponse.prs)
    }

    private suspend fun handleAsyncResponse(job: dev.gymapp.api.models.JobResponse) {
        val userMsg = ChatMessage(nextId(), ChatRole.USER, job.text)
        val placeholderId = nextId()
        val placeholderMsg = ChatMessage(
            id = placeholderId,
            role = ChatRole.ASSISTANT,
            content = "Thinking...",
            isPlaceholder = true
        )
        _state.update {
            it.copy(
                messages = it.messages + userMsg + placeholderMsg,
                isLoading = true,
                lastError = null
            )
        }

        when (val pollResult = withContext(Dispatchers.IO) {
            chatRepository.pollUntilComplete(job.jobId)
        }) {
            is kotlin.Result.Success -> {
                val chatResponse = pollResult.getOrThrow().result
                if (chatResponse != null) {
                    val assistantMsg = ChatMessage(
                        id = placeholderId,
                        role = ChatRole.ASSISTANT,
                        content = formatResponse(chatResponse),
                        response = chatResponse,
                        isPlaceholder = false
                    )
                    _state.update { state ->
                        state.copy(
                            messages = state.messages.map {
                                if (it.id == placeholderId) assistantMsg else it
                            },
                            isLoading = false
                        )
                    }
                    loadChatHistory()
                    showPrModalIfNeeded(chatResponse.prs)
                } else {
                    _state.update { state ->
                        state.copy(
                            messages = state.messages.filter { it.id != placeholderId },
                            isLoading = false,
                            lastError = ApiError("No response from server", "?", null)
                        )
                    }
                }
            }
            is kotlin.Result.Failure -> {
                _state.update { state ->
                    state.copy(
                        messages = state.messages.filter { it.id != placeholderId },
                        isLoading = false,
                        lastError = ApiError(
                            error = pollResult.exceptionOrNull()?.message ?: "Request failed",
                            code = "?",
                            errorToken = null
                        )
                    )
                }
            }
        }
    }

    fun retryPrImage(prId: String) {
        _state.update { s ->
            val updated = s.pendingPrModal?.map { p ->
                if (p.pr.id == prId) p.copy(imageLoadFailed = false) else p
            }
            s.copy(pendingPrModal = updated)
        }
        viewModelScope.launch {
            imageLoader.loadPrImage(prId)
                .onSuccess { bytes ->
                    _state.update { state ->
                        val updated = state.pendingPrModal?.map { p ->
                            if (p.pr.id == prId) p.copy(imageBytes = bytes, imageLoadFailed = false) else p
                        }
                        state.copy(pendingPrModal = updated)
                    }
                }
                .onFailure {
                    _state.update { s ->
                        val updated = s.pendingPrModal?.map { p ->
                            if (p.pr.id == prId) p.copy(imageLoadFailed = true) else p
                        }
                        s.copy(pendingPrModal = updated)
                    }
                }
        }
    }

    private fun showPrModalIfNeeded(prs: List<PersonalRecord>?) {
        runCatching {
            prs?.takeIf { it.isNotEmpty() }?.let { list ->
                val initial = list.map { PrWithImage(pr = it, imageBytes = null) }
                _state.update { it.copy(pendingPrModal = initial) }
                list.forEach { pr ->
                    viewModelScope.launch {
                        imageLoader.loadPrImage(pr.id)
                            .onSuccess { bytes ->
                                _state.update { state ->
                                    val updated = state.pendingPrModal?.map { p ->
                                        if (p.pr.id == pr.id) p.copy(imageBytes = bytes) else p
                                    }
                                    state.copy(pendingPrModal = updated)
                                }
                            }
                            .onFailure {
                                _state.update { s ->
                                    val updated = s.pendingPrModal?.map { p ->
                                        if (p.pr.id == pr.id) p.copy(imageLoadFailed = true) else p
                                    }
                                    s.copy(pendingPrModal = updated)
                                }
                            }
                    }
                }
            }
        }.onFailure {
            _state.update { s ->
                s.copy(lastError = ApiError(it.message ?: "Could not show PR", "?", null))
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
