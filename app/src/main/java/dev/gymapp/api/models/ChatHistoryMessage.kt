package dev.gymapp.api.models

import com.google.gson.annotations.SerializedName

data class ChatHistoryMessage(
    val role: String,
    val content: String,
    @SerializedName("created_at") val createdAt: String? = null
)
