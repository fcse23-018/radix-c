package com.terrochroma.shared.vision.model

import kotlinx.serialization.Serializable

@Serializable
data class ColorFeatures(
    val cieLab: CIELab,
    val hsv: HSV,
    val munsell: Munsell,
    val rgb: RGB
)

@Serializable
data class CIELab(
    val L: Double,
    val a: Double,
    val b: Double
)

@Serializable
data class HSV(
    val h: Double,
    val s: Double,
    val v: Double
)

@Serializable
data class Munsell(
    val hue: String,
    val value: Double,
    val chroma: Double
)

@Serializable
data class RGB(
    val r: Int,
    val g: Int,
    val b: Int
)
