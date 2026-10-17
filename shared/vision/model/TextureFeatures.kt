package com.terrochroma.shared.vision.model

import kotlinx.serialization.Serializable

@Serializable
data class TextureFeatures(
    val contrast: Double,
    val homogeneity: Double,
    val energy: Double,
    val entropy: Double
)
