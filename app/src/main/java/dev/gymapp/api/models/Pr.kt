package dev.gymapp.api.models

import com.google.gson.annotations.SerializedName

data class Pr(
    val id: String,
    @SerializedName("exercise_variant_id") val exerciseVariantId: String,
    @SerializedName("exercise_name") val exerciseName: String,
    @SerializedName("variant_name") val variantName: String,
    @SerializedName("pr_type") val prType: String,
    val weight: Double,
    val reps: Int,
    @SerializedName("image_url") val imageUrl: String? = null,
    @SerializedName("created_at") val createdAt: String
)
