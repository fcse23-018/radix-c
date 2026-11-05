package com.soilcarbon.engine.domain.model

import java.util.UUID

enum class UserRole(val displayName: String, val description: String) {
    FIELD_TECHNICIAN(
        "Field Technician",
        "Guided capture, angle gate, target alignment and sample tagging."
    ),
    AGRONOMIST(
        "Agronomist",
        "Field dashboard, sample map, review and CSV/GeoJSON export."
    ),
    DATA_SCIENTIST(
        "Data Scientist",
        "Dashboard, sample map, review and CSV/GeoJSON/provenance export."
    );

    companion object {
        fun fromName(name: String?): UserRole =
            entries.firstOrNull { it.name == name } ?: FIELD_TECHNICIAN
    }
}

data class GeoPoint(
    val latitude: Double,
    val longitude: Double,
    val altitudeMeters: Double? = null,
    val accuracyMeters: Float? = null,
    val provider: String = "unknown",
    val timestampMillis: Long = System.currentTimeMillis()
) {
    fun isValid(): Boolean =
        latitude in -90.0..90.0 &&
            longitude in -180.0..180.0 &&
            !(latitude == 0.0 && longitude == 0.0)

    fun formatted(): String = "%.6f, %.6f".format(latitude, longitude)
}

data class ImuReading(
    val pitchDegrees: Float,
    val rollDegrees: Float,
    val azimuthDegrees: Float = 0f,
    val timestampMillis: Long = System.currentTimeMillis()
) {
    val maxTiltDegrees: Float get() = maxOf(kotlin.math.abs(pitchDegrees), kotlin.math.abs(rollDegrees))
    fun isLevel(maxTilt: Float = 10f): Boolean = maxTiltDegrees <= maxTilt
}

data class LightReading(
    val lux: Float,
    val timestampMillis: Long = System.currentTimeMillis()
) {
    fun isAdequate(minLux: Float = 50f): Boolean = lux >= minLux
}

data class CameraExposureParameters(
    val cameraId: String = "0",
    val iso: Int = 100,
    val exposureTimeNs: Long = 8_000_000L,
    val focusDistanceDiopters: Float = 6.67f,
    val whiteBalanceKelvin: Int = 6500,
    val autoExposureLocked: Boolean = true,
    val autoWhiteBalanceLocked: Boolean = true,
    val autoFocusLocked: Boolean = true,
    val sensorOrientationDegrees: Int = 90,
    val focalLengthMm: Float? = null,
    val rawCaptureAvailable: Boolean = false,
    val rawJpegMode: Boolean = false,
    val jpegQuality: Int = 100,
    val deviceModel: String = "unknown",
    val deviceManufacturer: String = "unknown",
    val androidApiLevel: Int = 0
)

data class SampleIdentity(
    val id: String = UUID.randomUUID().toString(),
    val tag: String = "",
    val notes: String = "",
    val createdAtMillis: Long = System.currentTimeMillis()
)

enum class SyncState {
    LOCAL_ONLY,
    QUEUED,
    SYNCED,
    FAILED
}

enum class MoistureMethod {
    NONE,
    MANUAL_GRAVIMETRIC,
    BLE_NIR,
    OPTICAL_PROXY,
    EPO_PROJECTION
}
