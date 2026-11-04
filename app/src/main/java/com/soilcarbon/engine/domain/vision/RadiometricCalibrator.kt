package com.soilcarbon.engine.domain.vision

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Affine per-channel radiometric transfer function:
 *
 *   reflectance = gain * observedLinear + offset
 *
 * The six neutral calibration patches provide the known target reflectances. Fitting each
 * sensor channel independently removes illuminant intensity, correlated colour temperature and
 * the camera's tone curve (after sRGB linearisation) from the soil measurement.
 */
data class ChannelAffine(
    val gain: Double,
    val offset: Double,
    val rmse: Double,
    val rSquared: Double
) {
    fun apply(observedLinear: Double): Double = (gain * observedLinear + offset).coerceIn(0.0, 1.2)
}

data class PatchObservation(
    val id: String,
    val name: String,
    val targetReflectance: Double,
    val observedRgb: RgbColor,
    val observedLinearRgb: RgbColor,
    val spatialStdDev: Double,
    val pixelCount: Int
)

data class CalibrationResult(
    val patches: List<PatchObservation>,
    val red: ChannelAffine,
    val green: ChannelAffine,
    val blue: ChannelAffine,
    val overallRmse: Double,
    val worstRSquared: Double,
    val illuminationVariation: Double,
    val d65WhitePoint: XyzColor,
    val sourceDescription: String,
    val createdAtMillis: Long = System.currentTimeMillis()
) {
    val passesQualityGate: Boolean
        get() = illuminationVariation <= 0.15 && worstRSquared >= 0.80

    fun apply(observedLinearRgb: RgbColor): ReflectanceRgb = ReflectanceRgb(
        red.apply(observedLinearRgb.r),
        green.apply(observedLinearRgb.g),
        blue.apply(observedLinearRgb.b)
    )

    fun qualityLabel(): String = when {
        worstRSquared >= 0.995 && illuminationVariation <= 0.05 -> "Excellent"
        worstRSquared >= 0.98 && illuminationVariation <= 0.10 -> "Good"
        passesQualityGate -> "Marginal"
        else -> "Failed"
    }
}

object RadiometricCalibrator {

    private val TARGETS = doubleArrayOf(0.99, 0.80, 0.60, 0.40, 0.20, 0.05)

    /**
     * Fits the six-step neutral reflectance series. The darker patches are weighted slightly
     * higher because soil chromophore absorption is most informative in the 5–40% range, and
     * because sensor noise is proportionally larger there.
     */
    fun calibrate(
        observations: List<PatchObservation>,
        d65WhitePoint: XyzColor = Illuminant.D65.xyzWhite,
        sourceDescription: String = "DNG/JPEG locked manual capture"
    ): CalibrationResult {
        require(observations.size >= 4) { "At least four calibration patches are required" }
        require(observations.any { it.targetReflectance > 0.9 }) { "A white reference patch is required" }
        require(observations.any { it.targetReflectance < 0.1 }) { "A black reference patch is required" }

        val weights = observations.map { 1.0 + (1.0 - it.targetReflectance) * 0.5 }
        val red = fitAffine(
            observations.map { it.observedLinearRgb.r },
            observations.map { it.targetReflectance },
            weights
        )
        val green = fitAffine(
            observations.map { it.observedLinearRgb.g },
            observations.map { it.targetReflectance },
            weights
        )
        val blue = fitAffine(
            observations.map { it.observedLinearRgb.b },
            observations.map { it.targetReflectance },
            weights
        )

        val overallRmse = sqrt(
            observations.map { observation ->
                val corrected = ReflectanceRgb(
                    red.apply(observation.observedLinearRgb.r),
                    green.apply(observation.observedLinearRgb.g),
                    blue.apply(observation.observedLinearRgb.b)
                )
                val mean = corrected.average()
                val error = mean - observation.targetReflectance
                error * error
            }.average()
        )

        val variation = illuminationVariation(observations)

        return CalibrationResult(
            patches = observations,
            red = red,
            green = green,
            blue = blue,
            overallRmse = overallRmse,
            worstRSquared = minOf(red.rSquared, green.rSquared, blue.rSquared),
            illuminationVariation = variation,
            d65WhitePoint = d65WhitePoint,
            sourceDescription = sourceDescription
        )
    }

    /**
     * Variation from a monotonic six-step ladder. A perfectly uniform illuminant produces a
     * smooth monotonic progression. This metric is robust to the known reflectance spacing and
     * captures local exposure banding/shadow contamination.
     */
    fun illuminationVariation(observations: List<PatchObservation>): Double {
        val sorted = observations.sortedByDescending { it.targetReflectance }
        if (sorted.size < 3) return 1.0
        val residuals = sorted.mapIndexed { index, patch ->
            val expected = TARGETS.getOrElse(index) { patch.targetReflectance }
            val observedMean = patch.observedLinearRgb.let { (it.r + it.g + it.b) / 3.0 }
            val normalizer = max(expected, 0.05)
            abs(observedMean - expected) / normalizer
        }
        // Use a trimmed mean so a single defective patch does not dominate.
        val trimmed = residuals.sorted().drop(1).dropLast(1).ifEmpty { residuals }
        return trimmed.average().coerceIn(0.0, 1.0)
    }

    fun fitAffine(x: List<Double>, y: List<Double>, weights: List<Double>? = null): ChannelAffine {
        require(x.size == y.size && x.size >= 2)
        val w = weights ?: List(x.size) { 1.0 }
        var sumW = 0.0
        var sumX = 0.0
        var sumY = 0.0
        var sumXX = 0.0
        var sumXY = 0.0
        for (i in x.indices) {
            val weight = w[i].coerceAtLeast(1e-6)
            sumW += weight
            sumX += weight * x[i]
            sumY += weight * y[i]
            sumXX += weight * x[i] * x[i]
            sumXY += weight * x[i] * y[i]
        }
        val denominator = sumW * sumXX - sumX * sumX
        val gain = if (abs(denominator) < 1e-12) 1.0 else (sumW * sumXY - sumX * sumY) / denominator
        val offset = (sumY - gain * sumX) / sumW

        var sse = 0.0
        var sst = 0.0
        val yMean = sumY / sumW
        for (i in x.indices) {
            val predicted = gain * x[i] + offset
            sse += w[i] * (y[i] - predicted) * (y[i] - predicted)
            sst += w[i] * (y[i] - yMean) * (y[i] - yMean)
        }
        val rmse = sqrt(sse / sumW)
        val rSquared = if (sst < 1e-12) 1.0 else (1.0 - sse / sst).coerceIn(0.0, 1.0)
        return ChannelAffine(gain, offset, rmse, rSquared)
    }
}
