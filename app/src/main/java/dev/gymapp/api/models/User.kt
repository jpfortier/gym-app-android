package dev.gymapp.api.models

import com.google.gson.annotations.SerializedName

data class User(
    val id: String,
    val email: String,
    val name: String,
    @SerializedName("photo_url") val photoUrl: String? = null
)
