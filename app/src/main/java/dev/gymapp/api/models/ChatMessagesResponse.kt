package dev.gymapp.api.models

import com.google.gson.annotations.SerializedName

data class ChatMessagesResponse(
    val messages: List<ChatMessageDto>
)

data class ChatMessageDto(
    val id: String,
    val role: String,
    val content: String,
    @SerializedName("created_at") val createdAt: String
)
