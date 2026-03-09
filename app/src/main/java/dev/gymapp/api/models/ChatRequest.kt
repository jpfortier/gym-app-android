package dev.gymapp.api.models

import com.google.gson.annotations.SerializedName

data class ChatRequest(
    val text: String? = null,
    @SerializedName("audio_base64") val audioBase64: String? = null,
    @SerializedName("audio_format") val audioFormat: String? = null
)
