package com.soilcarbon.engine.presentation.capture

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soilcarbon.engine.core.Dispatchers
import com.soilcarbon.engine.data.camera.CameraController
import com.soilcarbon.engine.data.camera.CameraDeviceInfo
import com.soilcarbon.engine.data.calibration.CalibrationSpecLoader
import com.soilcarbon.engine.data.security.EncryptedFileStore
import com.soilcarbon.engine.data.sensors.ImuMonitor
import com.soilcarbon.engine.data.sensors.LightMonitor
import com.soilcarbon.engine.data.location.LocationProvider
import com.soilcarbon.engine.data.vision.ArUcoTargetDetector
import com.soilcarbon.engine.data.vision.TargetImageProcessor
import com.soilcarbon.engine.domain.calibration.CalibrationTargetSpec
import com.soilcarbon.engine.domain.calibration.TargetDetection
import com.soilcarbon.engine.domain.model.AppSettings
import com.soilcarbon.engine.domain.model.CalibrationProfile
import com.soilcarbon.engine.domain.model.CalibrationSummary
import com.soilcarbon.engine.domain.model.CameraExposureParameters
import com.soilcarbon.engine.domain.model.EnvironmentalCovariates
import com.soilcarbon.engine.domain.model.GeoPoint
import com.soilcarbon.engine.domain.model.ImuReading
import com.soilcarbon.engine.domain.model.LightReading
import com.soilcarbon.engine.domain.model.SampleStatus
import com.soilcarbon.engine.domain.model.SoilSample
import com.soilcarbon.engine.domain.repository.CalibrationRepository
import com.soilcarbon.engine.domain.repository.CovariateRepository
import com.soilcarbon.engine.domain.repository.SampleRepository
import com.soilcarbon.engine.domain.repository.SettingsRepository
import com.soilcarbon.engine.domain.usecase.AnalysisInput
import com.soilcarbon.engine.domain.usecase.AnalyzeSampleUseCase
import com.soilcarbon.engine.domain.vision.ColorFeatures
import com.soilcarbon.engine.domain.vision.ColorScience
import com.soilcarbon.engine.domain.vision.EpoMoistureCorrector
import com.soilcarbon.engine.domain.vision.LabColor
import com.soilcarbon.engine.domain.vision.MoistureCorrection
import com.soilcarbon.engine.domain.vision.MoistureEstimate
import com.soilcarbon.engine.domain.vision.ReflectanceRgb
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min

data class CaptureUiState(
    val permissionGranted: Boolean = false,
    val cameraReady: Boolean = false,
    val cameraStarting: Boolean = false,
    val cameraError: String? = null,
    val cameraInfo: CameraDeviceInfo? = null,
    val detection: TargetDetection? = null,
    val imu: ImuReading? = null,
    val light: LightReading? = null,
    val location: GeoPoint? = null,
    val locating: Boolean = false,
    val settings: AppSettings = AppSettings(),
    val exposure: CameraExposureParameters? = null,
    val tag: String = "",
    val notes: String = "",
    val moisturePercent: String = "",
    val busy: Boolean = false,
    val analysisStep: String = "",
    val lastSample: SoilSample? = null,
    val message: String? = null,
    val error: String? = null
) {
    val levelOk: Boolean
        get() = imu?.let { kotlin.math.abs(it.pitchDegrees) <= settings.maxTiltDegrees && kotlin.math.abs(it.rollDegrees) <= settings.maxTiltDegrees } != false

    val lightOk: Boolean
        get() = light?.let { it.lux >= settings.minLux } != false

    val gpsOk: Boolean
        get() = !settings.requireGps || location != null

    val cardOk: Boolean
        get() = detection?.isAligned == true

    val canCapture: Boolean
        get() = permissionGranted && cameraReady && !busy && levelOk && lightOk && gpsOk && cardOk
}

/**
 * Drives the science capture workflow. The view model owns the camera lifecycle, live ArUco
 * guidance, quality gates and the pixel-to-SOC pipeline, while keeping the UI declarative.
 */
@HiltViewModel
class CaptureViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cameraController: CameraController,
    private val imuMonitor: ImuMonitor,
    private val lightMonitor: LightMonitor,
    private val locationProvider: LocationProvider,
    private val detector: ArUcoTargetDetector,
    private val imageProcessor: TargetImageProcessor,
    private val calibrationRepository: CalibrationRepository,
    private val covariateRepository: CovariateRepository,
    private val sampleRepository: SampleRepository,
    private val settingsRepository: SettingsRepository,
    private val analyzeSample: AnalyzeSampleUseCase,
    private val fileStore: EncryptedFileStore,
    val targetSpec: CalibrationTargetSpec
) : ViewModel() {

    private val _state = MutableStateFlow(CaptureUiState())
    val state: StateFlow<CaptureUiState> = _state.asStateFlow()

    private val moistureCorrector = EpoMoistureCorrector()
    private var previewView: PreviewView? = null
    private var lifecycleOwner: LifecycleOwner? = null

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _state.update { current -> current.copy(settings = settings) }
            }
        }
        viewModelScope.launch {
            imuMonitor.reading.collect { reading ->
                _state.update { it.copy(imu = reading) }
            }
        }
        viewModelScope.launch {
            lightMonitor.reading.collect { reading ->
                _state.update { it.copy(light = reading) }
            }
        }
    }

    fun onPermissionResult(granted: Boolean) {
        _state.update { it.copy(permissionGranted = granted) }
        if (granted) {
            val view = previewView
            val owner = lifecycleOwner
            if (view != null && owner != null) startCamera(owner, view)
        } else {
            _state.update { it.copy(cameraReady = false, error = "Camera permission is required for capture.") }
        }
    }

    fun startCamera(owner: LifecycleOwner, view: PreviewView) {
        lifecycleOwner = owner
        previewView = view
        if (!_state.value.permissionGranted || _state.value.cameraStarting) return
        _state.update { it.copy(cameraStarting = true, cameraError = null, error = null) }
        imuMonitor.start()
        lightMonitor.start()
        viewModelScope.launch {
            cameraController.setFrameAnalyzer { frame ->
                val detection = runCatching { detector.detect(frame.bitmap, targetSpec) }.getOrNull()
                if (detection != null) {
                    _state.update { it.copy(detection = detection) }
                }
            }
            val exposure = _state.value.exposure
                ?: cameraController.deviceInfo?.defaultExposure()
                ?: CameraExposureParameters()
            val result = cameraController.start(owner, view, exposure)
            result.onSuccess { info ->
                _state.update {
                    it.copy(
                        cameraReady = true,
                        cameraStarting = false,
                        cameraInfo = info,
                        exposure = it.exposure ?: info.defaultExposure(),
                        cameraError = null
                    )
                }
                refreshLocation()
            }.onFailure { throwable ->
                _state.update {
                    it.copy(
                        cameraReady = false,
                        cameraStarting = false,
                        cameraError = throwable.message ?: "Unable to start the camera.",
                        error = throwable.message ?: "Unable to start the camera."
                    )
                }
            }
        }
    }

    fun stopCamera() {
        cameraController.stop()
        imuMonitor.stop()
        lightMonitor.stop()
        _state.update { it.copy(cameraReady = false, cameraStarting = false) }
        previewView = null
        lifecycleOwner = null
    }

    fun refreshLocation() {
        if (_state.value.locating) return
        _state.update { it.copy(locating = true) }
        viewModelScope.launch {
            val location = runCatching { locationProvider.currentLocation() }.getOrNull()
                ?: locationProvider.lastKnown()
            _state.update {
                it.copy(
                    location = location ?: it.location,
                    locating = false
                )
            }
        }
    }

    fun setTag(value: String) = _state.update { it.copy(tag = value) }
    fun setNotes(value: String) = _state.update { it.copy(notes = value) }
    fun setMoisture(value: String) = _state.update { it.copy(moisturePercent = value) }
    fun clearMessage() = _state.update { it.copy(message = null, error = null) }

    fun updateExposure(
        iso: Int,
        exposureTimeNs: Long,
        focusDistanceDiopters: Float,
        whiteBalanceKelvin: Int
    ) {
        val current = _state.value.exposure ?: cameraController.deviceInfo?.defaultExposure()
            ?: CameraExposureParameters()
        val info = _state.value.cameraInfo
        _state.update {
            it.copy(
                exposure = current.copy(
                    iso = iso.coerceIn(info?.isoMin ?: 50, info?.isoMax ?: 6400),
                    exposureTimeNs = exposureTimeNs.coerceIn(
                        info?.exposureMinNs ?: 100_000L,
                        info?.exposureMaxNs ?: 1_000_000_000L
                    ),
                    focusDistanceDiopters = focusDistanceDiopters.coerceIn(
                        0f,
                        info?.minimumFocusDistanceDiopters?.takeIf { d -> d > 0f } ?: 20f
                    ),
                    whiteBalanceKelvin = whiteBalanceKelvin.coerceIn(2500, 9000)
                )
            )
        }
    }

    fun applyExposure() {
        val owner = lifecycleOwner ?: return
        val view = previewView ?: return
        val exposure = _state.value.exposure ?: return
        cameraController.stop()
        _state.update { it.copy(cameraReady = false) }
        startCamera(owner, view)
        _state.update { it.copy(exposure = exposure) }
    }

    fun capture() {
        if (_state.value.busy) return
        val snapshot = _state.value
        if (!snapshot.permissionGranted) {
            _state.update { it.copy(error = "Grant camera permission before capturing.") }
            return
        }
        if (!snapshot.levelOk) {
            _state.update { it.copy(error = "Hold the phone within ${snapshot.settings.maxTiltDegrees.toInt()}° of level.") }
            return
        }
        if (!snapshot.lightOk) {
            _state.update { it.copy(error = "Too dark for a reliable reflectance capture (minimum ${snapshot.settings.minLux.toInt()} lux).") }
            return
        }
        if (!snapshot.cardOk) {
            _state.update { it.copy(error = "Align all five ArUco markers and the six calibration patches.") }
            return
        }

        _state.update { it.copy(busy = true, error = null, message = null, analysisStep = "Capturing RAW + JPEG…") }
        viewModelScope.launch {
            val sampleId = UUID.randomUUID().toString()
            val tempDir = File(context.cacheDir, "capture-temp").apply { mkdirs() }
            val jpegFile = File(tempDir, "${sampleId}_capture.jpg")
            val rawFile = File(tempDir, "${sampleId}_capture.dng")
            var bitmap: Bitmap? = null
            try {
                val settings = settingsRepository.current()
                val existingLocation = _state.value.location
                val location = existingLocation ?: runCatching { locationProvider.currentLocation() }.getOrNull()
                if (settings.requireGps && location == null) {
                    throw IllegalStateException("A GPS fix is required. Move outdoors or disable the GPS gate in Settings.")
                }
                _state.update { it.copy(location = location ?: it.location) }

                val captureFiles = withContext(Dispatchers.io) {
                    cameraController.capture(jpegFile, rawFile)
                }
                if (!captureFiles.jpegSaved) {
                    throw IllegalStateException(captureFiles.error ?: "JPEG capture failed.")
                }

                _state.update { it.copy(analysisStep = "Securing encrypted evidence…") }
                val jpegSecure = withContext(Dispatchers.io) {
                    fileStore.importFile(jpegFile, "images/$sampleId/capture.jpg")
                }
                val rawSecure = withContext(Dispatchers.io) {
                    if (captureFiles.rawSaved && rawFile.exists() && rawFile.length() > 0) {
                        fileStore.importFile(rawFile, "images/$sampleId/capture.dng")
                    } else {
                        null
                    }
                }

                _state.update { it.copy(analysisStep = "Detecting the calibration card…") }
                val decoded = withContext(Dispatchers.io) { BitmapFactory.decodeFile(jpegFile.absolutePath) }
                    ?: throw IllegalStateException("Could not decode the captured JPEG.")
                bitmap = decoded
                val detection = withContext(Dispatchers.io) { detector.detect(decoded, targetSpec) }
                if (detection.apertureQuad.size != 4) {
                    throw IllegalStateException(
                        detection.message.ifBlank { "Calibration card not found in the captured frame." }
                    )
                }
                _state.update { it.copy(detection = detection) }

                _state.update { it.copy(analysisStep = "Fitting the six-patch D65 transfer function…") }
                val calibration = withContext(Dispatchers.io) {
                    imageProcessor.calibrate(decoded, detection, targetSpec)
                } ?: throw IllegalStateException(
                    "Only ${detection.patchQuads.size}/6 calibration patches were measurable. " +
                        "Flatten the card, avoid glare, and retry."
                )

                val calibrationSummary = CalibrationSummary.from(calibration, targetSpec.version)
                val deviceModel = _state.value.exposure?.deviceModel
                    ?: cameraController.deviceInfo?.model
                    ?: "unknown"
                withContext(Dispatchers.io) {
                    calibrationRepository.save(
                        CalibrationProfile(
                            id = UUID.randomUUID().toString(),
                            cardVersion = targetSpec.version,
                            deviceModel = deviceModel,
                            result = calibration,
                            sha256 = ""
                        )
                    )
                }

                _state.update { it.copy(analysisStep = "Extracting soil colour and GLCM texture…") }
                val aperture = withContext(Dispatchers.io) {
                    imageProcessor.analyzeAperture(
                        bitmap = decoded,
                        detection = detection,
                        calibration = calibration,
                        patchCount = detection.patchQuads.size
                    )
                } ?: throw IllegalStateException("The soil aperture could not be sampled. Check the card cut-out.")

                val cropFile = File(tempDir, "${sampleId}_aperture.jpg")
                withContext(Dispatchers.io) {
                    FileOutputStream(cropFile).use { output ->
                        aperture.cropBitmap.compress(Bitmap.CompressFormat.JPEG, 92, output)
                    }
                }
                val cropSecure = withContext(Dispatchers.io) {
                    fileStore.importFile(cropFile, "images/$sampleId/aperture.jpg")
                }
                aperture.cropBitmap.recycle()

                _state.update { it.copy(analysisStep = "Fusing geospatial covariates…") }
                val covariates = if (location != null) {
                    runCatching { covariateRepository.getCovariates(location) }
                        .getOrElse { EnvironmentalCovariates.defaults(location) }
                } else {
                    EnvironmentalCovariates.defaults()
                }

                val manualMoisture = _state.value.moisturePercent.trim().toDoubleOrNull()
                    ?.takeIf { it >= 0.0 }
                val moisture: MoistureEstimate = if (manualMoisture != null) {
                    EpoMoistureCorrector.fromManual(manualMoisture)
                } else {
                    EpoMoistureCorrector.none()
                }
                var color = aperture.color
                var correction: MoistureCorrection? = null
                if (moisture.method != com.soilcarbon.engine.domain.model.MoistureMethod.NONE) {
                    val corrected = moistureCorrector.correct(
                        reflectance = color.reflectance,
                        lab = color.lab,
                        moisture = moisture
                    )
                    color = color.withCorrectedReflectance(corrected.first, corrected.second.dryEquivalentLab)
                    correction = corrected.second
                }

                val exposure = (_state.value.exposure
                    ?: cameraController.deviceInfo?.defaultExposure()
                    ?: CameraExposureParameters()).copy(
                    rawCaptureAvailable = captureFiles.rawSaved,
                    rawJpegMode = captureFiles.rawSaved,
                    autoExposureLocked = true,
                    autoWhiteBalanceLocked = true,
                    autoFocusLocked = true
                )

                val draft = SoilSample(
                    id = sampleId,
                    tag = _state.value.tag.trim(),
                    notes = _state.value.notes.trim(),
                    role = settings.role,
                    status = SampleStatus.CAPTURED,
                    location = location,
                    imu = _state.value.imu,
                    light = _state.value.light,
                    exposure = exposure,
                    jpegImagePath = jpegSecure.relativePath,
                    rawImagePath = rawSecure?.relativePath,
                    jpegSha256 = jpegSecure.sha256,
                    rawSha256 = rawSecure?.sha256,
                    apertureCropPath = cropSecure.relativePath,
                    calibration = calibrationSummary,
                    moisture = moisture.takeIf { it.method != com.soilcarbon.engine.domain.model.MoistureMethod.NONE },
                    soil = settings.toSoilProperties()
                )

                _state.update { it.copy(analysisStep = "Running on-device inference…") }
                val analyzed = analyzeSample.invoke(
                    AnalysisInput(
                        sample = draft,
                        color = color,
                        texture = aperture.texture,
                        covariates = covariates,
                        calibration = calibrationSummary,
                        moisture = draft.moisture,
                        moistureCorrection = correction,
                        soil = settings.toSoilProperties()
                    )
                )
                sampleRepository.upsert(analyzed)

                _state.update {
                    it.copy(
                        busy = false,
                        analysisStep = "",
                        lastSample = analyzed,
                        tag = "",
                        notes = "",
                        message = buildString {
                            append("Saved ${analyzed.displayName}: ")
                            append("%.2f%% SOC".format(java.util.Locale.US, analyzed.prediction?.socPercent ?: 0.0))
                            append(" (${analyzed.calibration?.qualityLabel ?: "uncalibrated"} calibration)")
                        }
                    )
                }
            } catch (t: Throwable) {
                _state.update {
                    it.copy(
                        busy = false,
                        analysisStep = "",
                        error = t.message ?: "Capture pipeline failed."
                    )
                }
            } finally {
                bitmap?.recycle()
                runCatching { jpegFile.delete() }
                runCatching { rawFile.delete() }
                runCatching { File(tempDir, "${sampleId}_aperture.jpg").delete() }
            }
        }
    }

    private fun ColorFeatures.withCorrectedReflectance(
        reflectance: ReflectanceRgb,
        lab: LabColor
    ): ColorFeatures {
        val display = ColorScience.xyzToSrgb(ColorScience.reflectanceToXyz(reflectance))
        return copy(
            lab = lab,
            reflectance = reflectance,
            hsv = ColorScience.srgbToHsv(display),
            munsell = ColorScience.labToMunsell(lab),
            ratios = ColorScience.reflectanceRatios(reflectance)
        )
    }

    override fun onCleared() {
        super.onCleared()
        cameraController.stop()
        imuMonitor.stop()
        lightMonitor.stop()
    }

    /** Guidance text used by the capture overlay. */
    fun guidance(state: CaptureUiState): String = when {
        !state.permissionGranted -> "Camera permission required"
        !state.cameraReady -> "Preparing camera…"
        !state.levelOk -> "Level the phone (≤ ${state.settings.maxTiltDegrees.toInt()}°)"
        !state.lightOk -> "Find brighter, diffuse light"
        !state.gpsOk -> "Waiting for GPS fix"
        !state.cardOk -> state.detection?.message ?: "Align the calibration card"
        else -> "Ready — hold still and capture"
    }

    companion object {
        fun clampIso(value: Int, info: CameraDeviceInfo?): Int =
            value.coerceIn(info?.isoMin ?: 50, info?.isoMax ?: 6400)

        fun exposureRangeMillis(info: CameraDeviceInfo?): ClosedFloatingPointRange<Float> {
            val minMs = ((info?.exposureMinNs ?: 100_000L) / 1_000_000.0).toFloat().coerceAtLeast(0.05f)
            val maxMs = min(120f, max(1f, (info?.exposureMaxNs ?: 1_000_000_000L) / 1_000_000.0f))
            return minMs..maxMs
        }
    }
}
