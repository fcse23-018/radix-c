package com.soilcarbon.engine.core

import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

object Constants {
    const val APP_NAME = "SoilCarbon Engine"
    const val SRS_VERSION = "2.4.0"
    const val SRS_DOCUMENT_ID = "SRS-SOC-2026-V1"

    /** Maximum permitted tilt from the target normal (FR-1.3). */
    const val MAX_TILT_DEGREES = 10.0f

    /** Maximum permitted coefficient of variation across calibration patches. */
    const val MAX_PATCH_VARIATION = 0.15f

    /** A minimum of 50 lux is required for a reliable diffuse-reflectance capture. */
    const val MIN_LUX = 50.0f

    /** Target reflectance values of the six-step calibration array (SRS section 8). */
    val CALIBRATION_REFLECTANCES = doubleArrayOf(0.99, 0.80, 0.60, 0.40, 0.20, 0.05)

    /** ArUco IDs printed on the calibration target. */
    const val ARUCO_CENTER_ID = 24
    val ARUCO_CORNER_IDS = intArrayOf(20, 21, 22, 23)

    const val VAN_BEMMELEN_FACTOR = 1.724

    const val MAX_OFFLINE_SAMPLES = 1_000
    const val DEFAULT_BULK_DENSITY = 1.30
    const val DEFAULT_DEPTH_CM = 30.0
    const val DEFAULT_CLAY_PERCENT = 25.0
    const val DEFAULT_NDVI = 0.35
    const val DEFAULT_SOC_BASELINE = 1.5

    /** WorkManager unique names. */
    const val SYNC_WORK_NAME = "soilcarbon-covariate-sync"
    const val MODEL_UPDATE_WORK_NAME = "soilcarbon-model-update"

    const val IMAGE_DIR = "images"
    const val EXPORT_DIR = "exports"
    const val CAPTURE_TEMP_DIR = "capture-temp"

    private val isoFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
        .withZone(ZoneOffset.UTC)

    fun isoNow(epochMillis: Long = System.currentTimeMillis()): String =
        isoFormatter.format(Instant.ofEpochMilli(epochMillis))

    fun displayDate(epochMillis: Long): String =
        DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")
            .withZone(ZoneOffset.UTC)
            .format(Instant.ofEpochMilli(epochMillis))
}
