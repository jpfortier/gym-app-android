package dev.gymapp.api.models

import com.google.gson.annotations.SerializedName

data class QueryResponse(
    @SerializedName("exercise_name") val exerciseName: String,
    @SerializedName("variant_name") val variantName: String,
    val entries: List<QueryEntry>
)

data class QueryEntry(
    @SerializedName("session_date") val sessionDate: String,
    @SerializedName("raw_speech") val rawSpeech: String? = null,
    val sets: List<QuerySet>,
    @SerializedName("created_at") val createdAt: String
)

data class QuerySet(
    val weight: Double? = null,
    val reps: Int,
    @SerializedName("set_type") val setType: String? = null
)
