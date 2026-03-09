package dev.gymapp.api.models

import com.google.gson.annotations.SerializedName

data class ExerciseVariant(
    @SerializedName("category_id") val categoryId: String,
    @SerializedName("category_name") val categoryName: String,
    @SerializedName("variant_id") val variantId: String,
    @SerializedName("variant_name") val variantName: String,
    @SerializedName("show_weight") val showWeight: Boolean = true,
    @SerializedName("show_reps") val showReps: Boolean = true
)
