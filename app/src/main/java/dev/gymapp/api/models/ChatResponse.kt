package dev.gymapp.api.models

import com.google.gson.annotations.SerializedName

data class ChatResponse(
    val intent: String? = null,
    val message: String? = null,
    val entries: List<LogEntry>? = null,
    val prs: List<PersonalRecord>? = null,
    val history: ChatHistory? = null
)

data class LogEntry(
    @SerializedName("exercise_name") val exerciseName: String,
    @SerializedName("variant_name") val variantName: String,
    @SerializedName("session_date") val sessionDate: String,
    @SerializedName("entry_id") val entryId: String
)

data class PersonalRecord(
    val id: String,
    @SerializedName("exercise_name") val exerciseName: String,
    @SerializedName("variant_name") val variantName: String,
    val weight: Double,
    val reps: Int,
    @SerializedName("pr_type") val prType: String
)

data class ChatHistory(
    @SerializedName("exercise_name") val exerciseName: String,
    @SerializedName("variant_name") val variantName: String,
    val entries: List<HistoryEntry>
)

data class HistoryEntry(
    @SerializedName("session_date") val sessionDate: String,
    @SerializedName("raw_speech") val rawSpeech: String? = null,
    val sets: List<HistorySet>,
    @SerializedName("created_at") val createdAt: String
)

data class HistorySet(
    val weight: Double? = null,
    val reps: Int,
    @SerializedName("set_type") val setType: String? = null
)
