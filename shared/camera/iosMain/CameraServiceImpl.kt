package com.terrochroma.shared.camera

import com.terrochroma.shared.camera.model.CameraConfig
import com.terrochroma.shared.camera.model.CaptureMetadata
import com.terrochroma.shared.camera.model.CaptureResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import platform.AVFoundation.*
import platform.CoreMedia.CMTime
import platform.CoreVideo.CVPixelBuffer
import platform.Foundation.*
import platform.Photos.*

class CameraServiceImpl : CameraService {

    private var captureSession: AVCaptureSession? = null
    private var captureDevice: AVCaptureDevice? = null
    private var photoOutput: AVCapturePhotoOutput? = null
    private var previewLayer: AVCaptureVideoPreviewLayer? = null

    override suspend fun initializeCamera(): Result<Unit> {
        return try {
            captureSession = AVCaptureSession()
            captureSession?.sessionPreset = AVCaptureSessionPresetPhoto

            val deviceTypes = listOf(
                AVCaptureDeviceTypeBuiltInWideAngleCamera,
                AVCaptureDeviceTypeBuiltInTelephotoCamera,
                AVCaptureDeviceTypeBuiltInUltraWideCamera
            )

            val discoverySession = AVCaptureDeviceDiscoverySession(
                deviceTypes = deviceTypes,
                mediaType = AVMediaTypeVideo,
                position = AVCaptureDevicePositionBack
            )

            captureDevice = discoverySession.devices.firstOrNull()
                ?: return Result.failure(Exception("No back camera found"))

            val input = AVCaptureDeviceInput(captureDevice!!, null)
            captureSession?.addInput(input)

            photoOutput = AVCapturePhotoOutput()
            photoOutput?.setPreparedPhotoSettings(
                listOf(AVCapturePhotoSettings().apply {
                    isHighResolutionPhotoEnabled = true
                })
            )
            captureSession?.addOutput(photoOutput)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun startPreview(): Result<Unit> {
        return try {
            captureSession?.startRunning()
            previewLayer = AVCaptureVideoPreviewLayer(captureSession)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun stopPreview(): Result<Unit> {
        return try {
            captureSession?.stopRunning()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun captureRaw(config: CameraConfig): Result<CaptureResult> {
        return try {
            val settings = AVCapturePhotoSettings().apply {
                isHighResolutionPhotoEnabled = true
                isAutoStillImageStabilizationEnabled = false
            }

            // Manual camera parameters
            captureDevice?.lockForConfiguration(null)
            captureDevice?.iso = config.iso.toFloat()
            captureDevice?.exposureMode = AVCaptureExposureModeCustom
            captureDevice?.setExposureModeCustomWithDuration(
                CMTime(seconds = config.exposureTime / 1_000_000_000.0, preferredTimescale = 1_000_000_000),
                iso = config.iso.toFloat(),
                completionHandler = null
            )
            captureDevice?.unlockForConfiguration()

            photoOutput?.capturePhotoWithSettings(
                settings,
                delegate = object : AVCapturePhotoCaptureDelegate {
                    override fun didFinishProcessingPhoto(
                        photoSampleBuffer: CMSampleBuffer?,
                        previewPhotoSampleBuffer: CMSampleBuffer?,
                        resolvedSettings: AVCaptureResolvedPhotoSettings,
                        bracketSettings: AVCaptureBracketedStillImageSettings?,
                        error: NSError?
                    ) {
                        // Process captured photo
                    }
                }
            )

            // Placeholder for actual capture result
            val captureResult = CaptureResult(
                imageData = ByteArray(0),
                imageHash = "",
                timestamp = System.currentTimeMillis(),
                config = config,
                metadata = CaptureMetadata(
                    width = 4032,
                    height = 3024,
                    pixelFormat = "BAYER",
                    colorSpace = "sRGB",
                    orientation = 0,
                    lensAperture = 2.0f,
                    focalLength = 4.0f,
                    sensorSize = "Unknown"
                )
            )

            Result.success(captureResult)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun lockCameraParameters(config: CameraConfig): Result<Unit> {
        return try {
            captureDevice?.lockForConfiguration(null)
            captureDevice?.iso = config.iso.toFloat()
            captureDevice?.exposureMode = AVCaptureExposureModeLocked
            captureDevice?.focusMode = AVCaptureFocusModeLocked
            captureDevice?.unlockForConfiguration()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun unlockCameraParameters(): Result<Unit> {
        return try {
            captureDevice?.lockForConfiguration(null)
            captureDevice?.exposureMode = AVCaptureExposureModeContinuousAutoExposure
            captureDevice?.focusMode = AVCaptureFocusModeContinuousAutoFocus
            captureDevice?.unlockForConfiguration()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getPreviewStream(): Flow<ByteArray> {
        return flowOf()
    }

    override suspend fun releaseCamera(): Result<Unit> {
        return try {
            captureSession?.stopRunning()
            captureSession = null
            captureDevice = null
            photoOutput = null
            previewLayer = null
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
