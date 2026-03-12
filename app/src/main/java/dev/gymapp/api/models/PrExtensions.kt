package dev.gymapp.api.models

fun Pr.toPersonalRecord() = PersonalRecord(
    id = id,
    exerciseName = exerciseName,
    variantName = variantName,
    weight = weight,
    reps = reps,
    prType = prType
)
