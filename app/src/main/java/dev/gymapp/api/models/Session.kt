package dev.gymapp.api.models

import com.google.gson.annotations.SerializedName

data class Session(
    val id: String,
    val date: String,
    @SerializedName("created_at") val createdAt: String,
    val entries: List<SessionEntry>? = null
)

data class SessionEntry(
    val id: String,
    @SerializedName("exercise_variant_id") val exerciseVariantId: String,
    @SerializedName("exercise_name") val exerciseName: String,
    @SerializedName("variant_name") val variantName: String,
    @SerializedName("raw_speech") val rawSpeech: String? = null,
    val notes: String? = null,
    val sets: List<EntrySet>
)

data class EntrySet(
    val weight: Double? = null,
    val reps: Int,
    @SerializedName("set_type") val setType: String? = null
)
