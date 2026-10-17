package com.terrochroma.shared.camera

import com.terrochroma.shared.camera.model.CameraConfig
import com.terrochroma.shared.camera.model.CaptureResult
import kotlinx.coroutines.flow.Flow

interface CameraService {
    suspend fun initializeCamera(): Result<Unit>
    suspend fun startPreview(): Result<Unit>
    suspend fun stopPreview(): Result<Unit>
    suspend fun captureRaw(config: CameraConfig): Result<CaptureResult>
    suspend fun lockCameraParameters(config: CameraConfig): Result<Unit>
    suspend fun unlockCameraParameters(): Result<Unit>
    fun getPreviewStream(): Flow<ByteArray>
    suspend fun releaseCamera(): Result<Unit>
}
