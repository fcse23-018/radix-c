package com.soilcarbon.engine.data.calibration

import android.content.Context
import com.google.gson.Gson
import com.soilcarbon.engine.BuildConfig
import com.soilcarbon.engine.core.Constants
import com.soilcarbon.engine.domain.calibration.CalibrationTargetSpec

/**
 * Loads the printable-target specification that ships in assets. The loader is intentionally
 * forgiving: a malformed or missing asset must never prevent field capture, so the compiled-in
 * default card geometry is used as a fallback. Any accepted spec is validated against the ArUco
 * IDs and reference reflectances mandated by the SRS.
 */
object CalibrationSpecLoader {

    private val gson = Gson()

    fun load(
        context: Context,
        assetPath: String = BuildConfig.CALIBRATION_SPEC_ASSET
    ): CalibrationTargetSpec {
        val parsed = runCatching {
            context.assets.open(assetPath).bufferedReader().use { reader ->
                gson.fromJson(reader, CalibrationTargetSpec::class.java)
            }
        }.getOrNull()
        return parsed?.takeIf(::isValid) ?: CalibrationTargetSpec.default()
    }

    private fun isValid(spec: CalibrationTargetSpec): Boolean {
        if (spec.cardWidthMm <= 0.0 || spec.cardHeightMm <= 0.0) return false
        if (spec.markers.size < 5) return false
        if (spec.patches.size != 6) return false
        val ids = spec.markers.map { it.id }.toSet()
        if (!ids.containsAll(Constants.ARUCO_CORNER_IDS.toSet()) ||
            Constants.ARUCO_CENTER_ID !in ids
        ) {
            return false
        }
        val reflectances = spec.patches.map { it.targetReflectance }.sortedDescending()
        val expected = Constants.CALIBRATION_REFLECTANCES.sortedDescending()
        return reflectances.zip(expected).all { (actual, target) ->
            kotlin.math.abs(actual - target) < 0.02
        } && spec.aperture.widthMm > 0.0 && spec.aperture.heightMm > 0.0
    }
}
