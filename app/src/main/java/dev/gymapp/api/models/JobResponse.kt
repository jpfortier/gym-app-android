package dev.gymapp.api.models

import com.google.gson.annotations.SerializedName

data class JobResponse(
    @SerializedName("job_id") val jobId: String,
    val text: String,
    val status: String,
    val result: ChatResponse? = null,
    val error: String? = null
)
