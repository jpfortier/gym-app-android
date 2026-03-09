package dev.gymapp.api.models

import com.google.gson.annotations.SerializedName

data class ApiError(
    val error: String,
    val code: String,
    @SerializedName("error_token") val errorToken: String? = null
)
