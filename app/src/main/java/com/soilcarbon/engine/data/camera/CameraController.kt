package com.soilcarbon.engine.data.camera

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.params.RggbChannelVector
import android.os.Build
import android.util.Range
import android.util.Size
import android.view.Surface
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.soilcarbon.engine.domain.model.CameraExposureParameters
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume
import kotlin.math.max
import kotlin.math.min

data class CameraDeviceInfo(
    val cameraId: String,
    val sensorOrientationDegrees: Int,
    val isoMin: Int,
    val isoMax: Int,
    val exposureMinNs: Long,
    val exposureMaxNs: Long,
    val focalLengthsMm: List<Float>,
    val minimumFocusDistanceDiopters: Float,
    val rawSupported: Boolean,
    val pixelArray: Size,
    val manufacturer: String,
    val model: String,
    val apiLevel: Int
) {
    fun defaultExposure(): CameraExposureParameters = CameraExposureParameters(
        cameraId = cameraId,
        iso = isoMin.coerceAtLeast(50).let { min(it, 400) },
        exposureTimeNs = 8_000_000L.coerceIn(exposureMinNs, exposureMaxNs),
        focusDistanceDiopters = 6.67f.coerceIn(0f, minimumFocusDistanceDiopters.coerceAtLeast(0.1f)),
        whiteBalanceKelvin = 6500,
        sensorOrientationDegrees = sensorOrientationDegrees,
        focalLengthMm = focalLengthsMm.firstOrNull(),
        rawCaptureAvailable = rawSupported,
        rawJpegMode = rawSupported,
        jpegQuality = 100,
        deviceModel = model,
        deviceManufacturer = manufacturer,
        androidApiLevel = apiLevel
    )
}

data class CaptureFiles(
    val jpegSaved: Boolean,
    val rawSaved: Boolean,
    val jpegUri: String?,
    val rawUri: String?,
    val error: String?
)

/**
 * CameraX capture engine with Camera2 interop for deterministic exposure, focus and white
 * balance. A simultaneous RAW_SENSOR + JPEG capture is requested on devices advertising the RAW
 * capability; elsewhere the engine records JPEG only and marks the sample as non-RAW.
 */
@OptIn(ExperimentalCamera2Interop::class)
class CameraController(
    private val context: Context
) {
    private var imageCapture: ImageCapture? = null
    private var imageAnalysis: ImageAnalysis? = null
    private var provider: ProcessCameraProvider? = null
    private var analysisExecutor: ExecutorService? = null
    private var frameAnalyzer: ((BitmapLike) -> Unit)? = null

    private val cameraManager: CameraManager =
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    val deviceInfo: CameraDeviceInfo? by lazy { queryBackCamera() }

    fun setFrameAnalyzer(analyzer: ((BitmapLike) -> Unit)?) {
        frameAnalyzer = analyzer
    }

    suspend fun start(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        exposure: CameraExposureParameters
    ): Result<CameraDeviceInfo> = runCatching {
        val info = deviceInfo ?: error("No back-facing camera available")
        val cameraProvider = ProcessCameraProvider.getInstance(context).awaitProvider()
        provider = cameraProvider

        val previewBuilder = Preview.Builder()
            .setTargetRotation(previewView.display?.rotation ?: Surface.ROTATION_0)
        applyManualControls(Camera2Interop.Extender(previewBuilder), exposure)
        val preview = previewBuilder.build().also {
            it.surfaceProvider = previewView.surfaceProvider
        }

        val outputFormat = if (info.rawSupported) {
            ImageCapture.OUTPUT_FORMAT_RAW_JPEG
        } else {
            ImageCapture.OUTPUT_FORMAT_JPEG
        }
        val captureBuilder = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setJpegQuality(100)
            .setOutputFormat(outputFormat)
            .setTargetRotation(previewView.display?.rotation ?: Surface.ROTATION_0)
        applyManualControls(Camera2Interop.Extender(captureBuilder), exposure)
        imageCapture = captureBuilder.build()

        val executor = analysisExecutor ?: Executors.newSingleThreadExecutor().also {
            analysisExecutor = it
        }
        val analysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .setTargetResolution(android.util.Size(960, 720))
            .build()
        analysis.setAnalyzer(executor) { proxy ->
            val callback = frameAnalyzer
            if (callback == null) {
                proxy.close()
                return@setAnalyzer
            }
            try {
                callback(BitmapLike(proxy.toBitmap()))
            } catch (_: Throwable) {
                // Analysis is advisory; never break preview on a frame failure.
            } finally {
                proxy.close()
            }
        }
        imageAnalysis = analysis

        cameraProvider.unbindAll()
        val bound = cameraProvider.bindToLifecycle(
            lifecycleOwner,
            CameraSelector.DEFAULT_BACK_CAMERA,
            preview,
            imageCapture,
            analysis
        )
        val sensorOrientation = bound.cameraInfo.sensorRotationDegrees
        info.copy(sensorOrientationDegrees = sensorOrientation)
    }

    suspend fun capture(jpegFile: File, rawFile: File?): CaptureFiles {
        val capture = imageCapture ?: return CaptureFiles(false, false, null, null, "Camera not ready")
        jpegFile.parentFile?.mkdirs()
        rawFile?.parentFile?.mkdirs()
        val jpegOptions = ImageCapture.OutputFileOptions.Builder(jpegFile).build()
        val info = deviceInfo
        val useRaw = rawFile != null && info?.rawSupported == true &&
            capture.outputFormat == ImageCapture.OUTPUT_FORMAT_RAW_JPEG

        return suspendCancellableCoroutine { continuation ->
            val finished = AtomicBoolean(false)
            val remaining = AtomicInteger(if (useRaw) 2 else 1)
            var jpegUri: String? = null
            var rawUri: String? = null
            var error: String? = null
            var jpegSaved = false
            var rawSaved = false

            fun complete() {
                if (finished.compareAndSet(false, true)) {
                    continuation.resume(
                        CaptureFiles(
                            jpegSaved = jpegSaved,
                            rawSaved = rawSaved,
                            jpegUri = jpegUri,
                            rawUri = rawUri,
                            error = error
                        )
                    )
                }
            }

            val callback = object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    if (outputFileResults.imageFormat == android.graphics.ImageFormat.RAW_SENSOR) {
                        rawSaved = true
                        rawUri = outputFileResults.savedUri?.toString()
                    } else {
                        jpegSaved = true
                        jpegUri = outputFileResults.savedUri?.toString()
                    }
                    if (remaining.decrementAndGet() <= 0) complete()
                }

                override fun onError(exception: ImageCaptureException) {
                    error = exception.message ?: "Capture failed"
                    if (remaining.decrementAndGet() <= 0) complete()
                }
            }

            val executor = ContextCompat.getMainExecutor(context)
            try {
                if (useRaw && rawFile != null) {
                    val rawOptions = ImageCapture.OutputFileOptions.Builder(rawFile).build()
                    capture.takePicture(rawOptions, jpegOptions, executor, callback)
                } else {
                    capture.takePicture(jpegOptions, executor, callback)
                }
            } catch (t: Throwable) {
                error = t.message ?: "Capture failed"
                finished.set(true)
                continuation.resume(CaptureFiles(false, false, null, null, error))
            }
        }
    }

    fun stop() {
        runCatching { provider?.unbindAll() }
        imageCapture = null
        imageAnalysis = null
        analysisExecutor?.shutdown()
        analysisExecutor = null
        frameAnalyzer = null
    }

    private fun applyManualControls(
        extender: Camera2Interop.Extender<*>,
        exposure: CameraExposureParameters
    ) {
        val info = deviceInfo
        val iso = exposure.iso.coerceIn(info?.isoMin ?: 50, info?.isoMax ?: 3200)
        val exposureNs = exposure.exposureTimeNs.coerceIn(
            info?.exposureMinNs ?: 100_000L,
            info?.exposureMaxNs ?: 1_000_000_000L
        )
        val focus = exposure.focusDistanceDiopters.coerceIn(
            0f,
            info?.minimumFocusDistanceDiopters?.takeIf { it > 0f } ?: 20f
        )
        extender.setCaptureRequestOption(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_OFF)
        extender.setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
        extender.setCaptureRequestOption(CaptureRequest.SENSOR_SENSITIVITY, iso)
        extender.setCaptureRequestOption(CaptureRequest.SENSOR_EXPOSURE_TIME, exposureNs)
        extender.setCaptureRequestOption(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)
        extender.setCaptureRequestOption(CaptureRequest.LENS_FOCUS_DISTANCE, focus)
        extender.setCaptureRequestOption(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF)
        extender.setCaptureRequestOption(
            CaptureRequest.COLOR_CORRECTION_GAINS,
            whiteBalanceGains(exposure.whiteBalanceKelvin)
        )
        extender.setCaptureRequestOption(CaptureRequest.CONTROL_AE_LOCK, true)
        extender.setCaptureRequestOption(CaptureRequest.CONTROL_AWB_LOCK, true)
    }

    /** Tanner Helland blackbody approximation converted to Camera2 RGGB gains. */
    private fun whiteBalanceGains(kelvin: Int): RggbChannelVector {
        val temperature = (kelvin.coerceIn(2000, 10000) / 100.0)
        val red = if (temperature <= 66.0) {
            255.0
        } else {
            (329.698727446 * Math.pow(temperature - 60.0, -0.1332047592)).coerceIn(0.0, 255.0)
        }
        val green = if (temperature <= 66.0) {
            99.4708025861 * Math.log(temperature) - 161.1195681661
        } else {
            288.1221695283 * Math.pow(temperature - 60.0, -0.0755148492)
        }.coerceIn(0.0, 255.0)
        val blue = when {
            temperature >= 66.0 -> 255.0
            temperature <= 19.0 -> 0.0
            else -> 138.5177312231 * Math.log(temperature - 10.0) - 305.0447927307
        }.coerceIn(0.0, 255.0)
        val g = max(green, 1.0)
        return RggbChannelVector(
            (red / g).toFloat().coerceIn(0.1f, 8f),
            1f,
            1f,
            (blue / g).toFloat().coerceIn(0.1f, 8f)
        )
    }

    private fun queryBackCamera(): CameraDeviceInfo? {
        return runCatching {
            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
            } ?: cameraManager.cameraIdList.firstOrNull() ?: return null
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            val sensitivity = characteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
                ?: Range(50, 3200)
            val exposure = characteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
                ?: Range(100_000L, 1_000_000_000L)
            val focalLengths = characteristics
                .get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                ?.toList() ?: emptyList()
            val capabilities = characteristics
                .get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES) ?: intArrayOf()
            val rawSupported = capabilities.contains(
                CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW
            )
            CameraDeviceInfo(
                cameraId = cameraId,
                sensorOrientationDegrees = characteristics
                    .get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 90,
                isoMin = sensitivity.lower,
                isoMax = sensitivity.upper,
                exposureMinNs = exposure.lower,
                exposureMaxNs = exposure.upper,
                focalLengthsMm = focalLengths,
                minimumFocusDistanceDiopters = characteristics
                    .get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE) ?: 0f,
                rawSupported = rawSupported,
                pixelArray = characteristics
                    .get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)
                    ?: Size(4000, 3000),
                manufacturer = Build.MANUFACTURER,
                model = Build.MODEL,
                apiLevel = Build.VERSION.SDK_INT
            )
        }.getOrNull()
    }

    private suspend fun <T> com.google.common.util.concurrent.ListenableFuture<T>.awaitProvider(): T =
        suspendCancellableCoroutine { continuation ->
            addListener(
                {
                    try {
                        continuation.resume(get())
                    } catch (t: Throwable) {
                        continuation.resumeWith(Result.failure(t))
                    }
                },
                ContextCompat.getMainExecutor(context)
            )
            continuation.invokeOnCancellation { cancel(true) }
        }

    /** Avoids exposing CameraX types to the presentation layer. */
    data class BitmapLike(val bitmap: android.graphics.Bitmap)
}
