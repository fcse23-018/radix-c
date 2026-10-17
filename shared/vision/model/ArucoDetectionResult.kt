package com.terrochroma.shared.vision.model

import kotlinx.serialization.Serializable

@Serializable
data class ArucoDetectionResult(
    val isDetected: Boolean,
    val markerId: Int?,
    val corners: List<Point2D>?,
    val center: Point2D?,
    val confidence: Double,
    val rotation: Double?, // Rotation in degrees
    val isAligned: Boolean
)

@Serializable
data class Point2D(
    val x: Double,
    val y: Double
)

data class CalibrationCard(
    val markerId: Int = 0,
    val size: Double = 50.0, // mm
    val requiredAlignment: Double = 5.0, // degrees tolerance
    val requiredDistance: Double = 100.0 // mm optimal distance
)
