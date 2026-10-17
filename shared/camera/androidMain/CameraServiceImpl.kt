package com.terrochroma.shared.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.Image
import android.media.ImageReader
import android.util.Size
import android.view.Surface
import androidx.core.content.ContextCompat
import com.terrochroma.shared.camera.model.CameraConfig
import com.terrochroma.shared.camera.model.CaptureMetadata
import com.terrochroma.shared.camera.model.CaptureResult
import com.terrochroma.shared.camera.model.RawFormat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.security.MessageDigest

class CameraServiceImpl(private val context: Context) : CameraService {

    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var previewRequestBuilder: CaptureRequest.Builder? = null

    override suspend fun initializeCamera(): Result<Unit> {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList.firstOrNull {
                cameraManager.getCameraCharacteristics(it)
                    .get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
            } ?: return Result.failure(Exception("No back camera found"))

            cameraDevice = cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    // Camera opened successfully
                }

                override fun onDisconnected(camera: CameraDevice) {
                    camera.close()
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    camera.close()
                }
            }, null)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun startPreview(): Result<Unit> {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val characteristics = cameraManager.getCameraCharacteristics(cameraDevice?.id ?: "")
            val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            val previewSize = map?.getOutputSizes(SurfaceHolder::class.java)?.firstOrNull()
                ?: Size(1920, 1080)

            val surfaceTexture = android.graphics.SurfaceTexture(1)
            surfaceTexture.setDefaultBufferSize(previewSize.width, previewSize.height)
            val surface = Surface(surfaceTexture)

            previewRequestBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            previewRequestBuilder?.addTarget(surface)

            cameraDevice?.createCaptureSession(
                listOf(surface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        captureSession = session
                        previewRequestBuilder?.let { builder ->
                            builder.set(
                                CaptureRequest.CONTROL_AF_MODE,
                                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                            )
                            session.setRepeatingRequest(builder.build(), null, null)
                        }
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        // Handle failure
                    }
                },
                null
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun stopPreview(): Result<Unit> {
        return try {
            captureSession?.stopRepeating()
            captureSession?.close()
            captureSession = null
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun captureRaw(config: CameraConfig): Result<CaptureResult> {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val characteristics = cameraManager.getCameraCharacteristics(cameraDevice?.id ?: "")
            val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)

            // RAW capture requires DNG format
            val rawSize = map?.getOutputSizes(ImageFormat.RAW_SENSOR)?.firstOrNull()
                ?: Size(4032, 3024)

            val imageReader = ImageReader.newInstance(
                rawSize.width,
                rawSize.height,
                ImageFormat.RAW_SENSOR,
                1
            )

            val captureRequestBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            captureRequestBuilder?.addTarget(imageReader.surface)

            // Apply manual camera parameters
            captureRequestBuilder?.set(CaptureRequest.SENSOR_SENSITIVITY, config.iso.toLong())
            captureRequestBuilder?.set(CaptureRequest.SENSOR_EXPOSURE_TIME, config.exposureTime)

            cameraDevice?.createCaptureSession(
                listOf(imageReader.surface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        session.capture(captureRequestBuilder?.build(), null, null)
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        // Handle failure
                    }
                },
                null
            )

            val image = imageReader.acquireNextImage()
            val buffer = image.planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            image.close()

            val imageHash = generateHash(bytes)
            val timestamp = System.currentTimeMillis()

            val metadata = CaptureMetadata(
                width = rawSize.width,
                height = rawSize.height,
                pixelFormat = "RAW_SENSOR",
                colorSpace = "sRGB",
                orientation = 0,
                lensAperture = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES)?.firstOrNull() ?: 2.0f,
                focalLength = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)?.firstOrNull() ?: 4.0f,
                sensorSize = characteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)?.toString() ?: "Unknown"
            )

            val captureResult = CaptureResult(
                imageData = bytes,
                imageHash = imageHash,
                timestamp = timestamp,
                config = config,
                metadata = metadata
            )

            imageReader.close()
            Result.success(captureResult)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun lockCameraParameters(config: CameraConfig): Result<Unit> {
        return try {
            previewRequestBuilder?.set(CaptureRequest.SENSOR_SENSITIVITY, config.iso.toLong())
            previewRequestBuilder?.set(CaptureRequest.SENSOR_EXPOSURE_TIME, config.exposureTime)
            previewRequestBuilder?.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)
            captureSession?.setRepeatingRequest(previewRequestBuilder?.build(), null, null)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun unlockCameraParameters(): Result<Unit> {
        return try {
            previewRequestBuilder?.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
            captureSession?.setRepeatingRequest(previewRequestBuilder?.build(), null, null)
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
            captureSession?.close()
            cameraDevice?.close()
            captureSession = null
            cameraDevice = null
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun generateHash(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(data)
        return hash.joinToString("") { "%02x".format(it) }
    }
}
