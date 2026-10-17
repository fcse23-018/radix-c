package com.terrochroma.shared.vision

import com.terrochroma.shared.camera.model.CaptureResult
import com.terrochroma.shared.vision.model.ArucoDetectionResult
import com.terrochroma.shared.vision.model.ColorFeatures
import com.terrochroma.shared.vision.model.TextureFeatures

interface VisionService {
    suspend fun initialize(): Result<Unit>
    suspend fun detectAruco(imageData: ByteArray): Result<ArucoDetectionResult>
    suspend fun extractColorFeatures(imageData: ByteArray): Result<ColorFeatures>
    suspend fun extractTextureFeatures(imageData: ByteArray): Result<TextureFeatures>
    suspend fun normalizeIlluminant(imageData: ByteArray, illuminant: Illuminant): Result<ByteArray>
    suspend fun release(): Result<Unit>
}

enum class Illuminant {
    D65, // Standard daylight
    D50, // Horizon daylight
    A,   // Incandescent
    F2,  // Fluorescent cool white
    F11  // Fluorescent high efficiency
}
