package com.soilcarbon.engine.domain.model

import com.soilcarbon.engine.core.Constants

/**
 * SRS feature vector, in the exact order mandated by SRS-SOC-2026-V1 section 9:
 *
 * X = [L*, a*, b*, Munsell_Value, GLCM_Contrast, GLCM_Energy, NDVI_Sat, Clay_Content]
 *
 * The vector is the contract between the feature extraction pipeline and the TFLite model.
 * Do not reorder without bumping [FEATURE_VECTOR_VERSION] and retraining the model.
 */
data class FeatureVector(
    val lStar: Double,
    val aStar: Double,
    val bStar: Double,
    val munsellValue: Double,
    val glcmContrast: Double,
    val glcmEnergy: Double,
    val ndviSatellite: Double,
    val clayContentPercent: Double
) {
    init {
        require(lStar in 0.0..100.0) { "L* out of range: $lStar" }
        require(aStar in -128.0..127.0) { "a* out of range: $aStar" }
        require(bStar in -128.0..127.0) { "b* out of range: $bStar" }
        require(munsellValue in 0.0..10.0) { "Munsell Value out of range: $munsellValue" }
        require(glcmContrast in 0.0..1.0) { "GLCM contrast out of range: $glcmContrast" }
        require(glcmEnergy in 0.0..1.0) { "GLCM energy out of range: $glcmEnergy" }
        require(ndviSatellite in -1.0..1.0) { "NDVI out of range: $ndviSatellite" }
        require(clayContentPercent in 0.0..100.0) { "Clay content out of range: $clayContentPercent" }
    }

    fun toFloatArray(): FloatArray = floatArrayOf(
        lStar.toFloat(),
        aStar.toFloat(),
        bStar.toFloat(),
        munsellValue.toFloat(),
        glcmContrast.toFloat(),
        glcmEnergy.toFloat(),
        ndviSatellite.toFloat(),
        clayContentPercent.toFloat()
    )

    fun toDoubleArray(): DoubleArray = doubleArrayOf(
        lStar, aStar, bStar, munsellValue, glcmContrast, glcmEnergy, ndviSatellite, clayContentPercent
    )

    fun toCanonicalString(): String = toDoubleArray().joinToString(prefix = "[", postfix = "]") {
        "%.6f".format(java.util.Locale.US, it)
    }

    fun toDisplayPairs(): List<Pair<String, String>> = listOf(
        "L*" to "%.2f".format(lStar),
        "a*" to "%.2f".format(aStar),
        "b*" to "%.2f".format(bStar),
        "Munsell Value" to "%.2f".format(munsellValue),
        "GLCM Contrast" to "%.4f".format(glcmContrast),
        "GLCM Energy" to "%.4f".format(glcmEnergy),
        "NDVI (satellite)" to "%.3f".format(ndviSatellite),
        "Clay content" to "%.1f%%".format(clayContentPercent)
    )

    companion object {
        const val FEATURE_VECTOR_VERSION = "1.0"
        val FEATURE_NAMES = listOf(
            "L_star", "a_star", "b_star", "munsell_value",
            "glcm_contrast", "glcm_energy", "ndvi_satellite", "clay_content_percent"
        )

        fun fromValues(values: DoubleArray): FeatureVector {
            require(values.size == FEATURE_NAMES.size) {
                "Expected ${FEATURE_NAMES.size} features, got ${values.size}"
            }
            return FeatureVector(
                lStar = values[0].coerceIn(0.0, 100.0),
                aStar = values[1].coerceIn(-128.0, 127.0),
                bStar = values[2].coerceIn(-128.0, 127.0),
                munsellValue = values[3].coerceIn(0.0, 10.0),
                glcmContrast = values[4].coerceIn(0.0, 1.0),
                glcmEnergy = values[5].coerceIn(0.0, 1.0),
                ndviSatellite = values[6].coerceIn(-1.0, 1.0),
                clayContentPercent = values[7].coerceIn(0.0, 100.0)
            )
        }
    }
}

data class Prediction(
    val socPercent: Double,
    val somPercent: Double,
    val socLower95: Double,
    val socUpper95: Double,
    val uncertainty95: Double,
    val confidenceScore: Double,
    val carbonStockMgPerHa: Double?,
    val modelId: String,
    val modelVersion: String,
    val inferenceMillis: Long,
    val fallbackUsed: Boolean = false,
    val notes: String = ""
) {
    init {
        require(socPercent >= 0.0) { "SOC cannot be negative" }
        require(somPercent >= socPercent) { "SOM must be >= SOC (Van Bemmelen)" }
        require(socLower95 <= socPercent && socPercent <= socUpper95)
    }

    companion object {
        const val VAN_BEMMELEN_FACTOR = Constants.VAN_BEMMELEN_FACTOR
    }
}
