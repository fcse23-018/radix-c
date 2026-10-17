package com.terrochroma.shared.domain.model

import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Serializable
data class SoilSample(
    val id: String,
    val timestamp: Long,
    val latitude: Double?,
    val longitude: Double?,
    val altitude: Double?,
    val socPercentage: Double,
    val confidenceInterval: Double,
    val somPercentage: Double,
    val labValue: Double?, // Calibrated lab reference if available
    val moistureContent: Double?,
    val imageHash: String,
    val provenanceHash: String,
    val cameraParams: CameraParams,
    val colorFeatures: ColorFeatures,
    val textureFeatures: TextureFeatures,
    val modelVersion: String,
    val appVersion: String,
    val isSynced: Boolean = false,
    val notes: String? = null
)

@Serializable
data class CameraParams(
    val iso: Int,
    val exposureTime: Long,
    val whiteBalanceMode: String,
    val focusMode: String,
    val flashMode: String,
    val rawFormat: String
)

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

@Serializable
data class TextureFeatures(
    val contrast: Double,
    val homogeneity: Double,
    val energy: Double,
    val entropy: Double
)

@OptIn(ExperimentalUuidApi::class)
fun createSoilSample(
    socPercentage: Double,
    confidenceInterval: Double,
    somPercentage: Double,
    moistureContent: Double?,
    imageHash: String,
    cameraParams: CameraParams,
    colorFeatures: ColorFeatures,
    textureFeatures: TextureFeatures,
    modelVersion: String,
    appVersion: String,
    latitude: Double? = null,
    longitude: Double? = null,
    altitude: Double? = null,
    labValue: Double? = null,
    notes: String? = null
): SoilSample {
    val id = Uuid.random().toString()
    val timestamp = System.currentTimeMillis()
    val provenanceHash = generateProvenanceHash(
        id, timestamp, socPercentage, imageHash, cameraParams, modelVersion, appVersion
    )

    return SoilSample(
        id = id,
        timestamp = timestamp,
        latitude = latitude,
        longitude = longitude,
        altitude = altitude,
        socPercentage = socPercentage,
        confidenceInterval = confidenceInterval,
        somPercentage = somPercentage,
        labValue = labValue,
        moistureContent = moistureContent,
        imageHash = imageHash,
        provenanceHash = provenanceHash,
        cameraParams = cameraParams,
        colorFeatures = colorFeatures,
        textureFeatures = textureFeatures,
        modelVersion = modelVersion,
        appVersion = appVersion,
        isSynced = false,
        notes = notes
    )
}

private fun generateProvenanceHash(
    id: String,
    timestamp: Long,
    socPercentage: Double,
    imageHash: String,
    cameraParams: CameraParams,
    modelVersion: String,
    appVersion: String
): String {
    val input = "$id:$timestamp:$socPercentage:$imageHash:$cameraParams:$modelVersion:$appVersion"
    return input.hashCode().toString(16)
}
