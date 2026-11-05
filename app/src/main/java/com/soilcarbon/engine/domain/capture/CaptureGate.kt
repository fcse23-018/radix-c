package com.soilcarbon.engine.domain.capture

import com.soilcarbon.engine.domain.model.AppSettings

/**
 * Pure capture-policy gates.
 *
 * The capture pipeline calls these so a persisted setting (RAW requirement, GPS requirement,
 * calibration-patch minimum) demonstrably changes behaviour instead of only being stored.
 */
object CaptureGate {

    /** At least four of the six reference patches must be measurable to fit the transfer. */
    const val MINIMUM_CALIBRATION_PATCHES = 4

    /** The printable target carries six neutral patches. */
    const val TOTAL_CALIBRATION_PATCHES = 6

    fun gpsError(settings: AppSettings, gpsAvailable: Boolean): String? =
        if (settings.requireGps && !gpsAvailable) {
            "A GPS fix is required. Move outdoors or disable the GPS gate in Settings."
        } else {
            null
        }

    /**
     * FAIL CLOSED when the capture policy asks for RAW and the camera only returned JPEG.
     * Without the DNG there is no lossless reflectance evidence to archive.
     */
    fun rawCaptureError(settings: AppSettings, rawSaved: Boolean): String? =
        if (settings.requireRawCapture && !rawSaved) {
            "The capture policy requires a RAW (DNG) frame, but the camera returned JPEG only. " +
                "Enable RAW capture on the device or turn off \"Require RAW + JPEG\" in Settings."
        } else {
            null
        }

    fun patchError(
        observed: Int,
        minimum: Int = MINIMUM_CALIBRATION_PATCHES,
        total: Int = TOTAL_CALIBRATION_PATCHES
    ): String? =
        if (observed >= minimum) {
            null
        } else {
            "Only $observed/$total calibration patches were measurable; at least $minimum are " +
                "required. Flatten the card, avoid glare, and retry."
        }

    fun patchGatePasses(
        observed: Int,
        minimum: Int = MINIMUM_CALIBRATION_PATCHES
    ): Boolean = observed >= minimum
}
