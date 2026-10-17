package com.terrochroma.shared.camera.model

import kotlinx.serialization.Serializable

data class CaptureResult(
    val imageData: ByteArray,
    val imageHash: String,
    val timestamp: Long,
    val config: CameraConfig,
    val metadata: CaptureMetadata
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as CaptureResult

        if (!imageData.contentEquals(other.imageData)) return false
        if (imageHash != other.imageHash) return false
        if (timestamp != other.timestamp) return false
        if (config != other.config) return false
        if (metadata != other.metadata) return false

        return true
    }

    override fun hashCode(): Int {
        var result = imageData.contentHashCode()
        result = 31 * result + imageHash.hashCode()
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + config.hashCode()
        result = 31 * result + metadata.hashCode()
        return result
    }
}

@Serializable
data class CaptureMetadata(
    val width: Int,
    val height: Int,
    val pixelFormat: String,
    val colorSpace: String,
    val orientation: Int,
    val lensAperture: Float,
    val focalLength: Float,
    val sensorSize: String
)
