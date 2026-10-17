package com.terrochroma.shared.ml.model

data class MLOutput(
    val socPercentage: Double,
    val confidenceInterval: Double,
    val somPercentage: Double,
    val moistureContent: Double?,
    val inferenceTimeMs: Long,
    val modelVersion: String
)
