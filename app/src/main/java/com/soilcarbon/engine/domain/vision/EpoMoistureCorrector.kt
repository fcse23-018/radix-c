package com.soilcarbon.engine.domain.vision

import com.soilcarbon.engine.domain.model.MoistureMethod
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

data class MoistureEstimate(
    val percentGravimetric: Double,
    val method: MoistureMethod,
    val confidence: Double,
    val source: String
)

data class MoistureCorrection(
    val applied: Boolean,
    val method: MoistureMethod,
    val inputMoisturePercent: Double,
    val dryEquivalentLab: LabColor,
    val lStarDelta: Double,
    val estimatedUncertaintyPercent: Double,
    val notes: String
)

/**
 * EPO (External Parameter Orthogonalization) projection.
 *
 * Given column directions B spanning the moisture-induced variation subspace, the correction
 * applied to a spectrum x is:
 *
 *   x_corrected = x * (I - B * (B^T B)^-1 * B^T)
 *
 * The projection removes only variation aligned with the supplied moisture directions. The
 * shipped default model uses one direction (diffuse water darkening) plus a scalar L* bias
 * derived from the external moisture measurement. Regional deployments should replace the
 * direction matrix and retrain the regressor on EPO-corrected spectra (see docs/MODEL_TRAINING.md).
 */
class EpoMoistureCorrector(
    private val directions: List<DoubleArray> = DEFAULT_DIRECTIONS,
    private val lStarSlopePerPercent: Double = 0.42,
    private val referenceMoisturePercent: Double = 5.0
) {
    fun correct(
        reflectance: ReflectanceRgb,
        lab: LabColor,
        moisture: MoistureEstimate
    ): Pair<ReflectanceRgb, MoistureCorrection> {
        if (moisture.percentGravimetric < referenceMoisturePercent ||
            moisture.method == MoistureMethod.NONE
        ) {
            return reflectance to MoistureCorrection(
                applied = false,
                method = moisture.method,
                inputMoisturePercent = moisture.percentGravimetric,
                dryEquivalentLab = lab,
                lStarDelta = 0.0,
                estimatedUncertaintyPercent = 0.0,
                notes = "Moisture below 5% or no moisture estimate; EPO not required by FR-3.3."
            )
        }

        val vector = doubleArrayOf(reflectance.r, reflectance.g, reflectance.b)
        var corrected = vector.copyOf()
        for (direction in directions) {
            corrected = projectOut(corrected, direction)
        }
        val correctedReflectance = ReflectanceRgb(corrected[0], corrected[1], corrected[2]).clamped()
        val deltaL = lStarSlopePerPercent * (moisture.percentGravimetric - referenceMoisturePercent)
        val correctedLab = ColorScience.reflectanceToLab(correctedReflectance)
        val dryLab = LabColor(
            l = (lab.l + deltaL).coerceIn(0.0, 100.0),
            a = correctedLab.a,
            b = correctedLab.b
        )
        val uncertainty = 0.35 + abs(moisture.percentGravimetric - referenceMoisturePercent) * 0.06
        return correctedReflectance to MoistureCorrection(
            applied = true,
            method = MoistureMethod.EPO_PROJECTION,
            inputMoisturePercent = moisture.percentGravimetric,
            dryEquivalentLab = dryLab,
            lStarDelta = deltaL,
            estimatedUncertaintyPercent = uncertainty,
            notes = "EPO projection applied to visible reflectance; L* adjusted to dry-equivalent " +
                "baseline (${"%.2f".format(deltaL)} units)."
        )
    }

    private fun projectOut(vector: DoubleArray, direction: DoubleArray): DoubleArray {
        require(direction.size == 3)
        val normSq = direction.sumOf { it * it }
        if (normSq < 1e-12) return vector
        val dot = vector[0] * direction[0] + vector[1] * direction[1] + vector[2] * direction[2]
        val scale = dot / normSq
        return doubleArrayOf(
            vector[0] - scale * direction[0],
            vector[1] - scale * direction[1],
            vector[2] - scale * direction[2]
        )
    }

    companion object {
        /** Unit direction of diffuse water darkening in linear RGB. */
        val DEFAULT_DIRECTIONS: List<DoubleArray> = listOf(
            doubleArrayOf(1.0 / sqrt(3.0), 1.0 / sqrt(3.0), 1.0 / sqrt(3.0))
        )

        /**
         * Optical moisture proxy used when no gravimetric or NIR value is available. This is a
         * low-confidence screening estimate only: wet soil is darker and less saturated. It is
         * deliberately conservative and is never presented as a laboratory moisture result.
         */
        fun opticalProxy(reflectance: ReflectanceRgb, lab: LabColor): MoistureEstimate {
            val redBlueRatio = if (reflectance.b < 1e-5) 1.0 else reflectance.r / reflectance.b
            val darkness = 1.0 - (lab.l / 100.0)
            val percent = (4.0 + darkness * 42.0 + (redBlueRatio - 1.25) * 9.0)
                .coerceIn(3.0, 38.0)
            return MoistureEstimate(
                percentGravimetric = percent,
                method = MoistureMethod.OPTICAL_PROXY,
                confidence = 0.35,
                source = "Visible reflectance proxy (low confidence)"
            )
        }

        fun fromManual(percent: Double): MoistureEstimate = MoistureEstimate(
            percentGravimetric = percent.coerceIn(0.0, 80.0),
            method = MoistureMethod.MANUAL_GRAVIMETRIC,
            confidence = 0.95,
            source = "Operator gravimetric entry"
        )

        fun fromBle(percent: Double, source: String): MoistureEstimate = MoistureEstimate(
            percentGravimetric = percent.coerceIn(0.0, 80.0),
            method = MoistureMethod.BLE_NIR,
            confidence = 0.85,
            source = source
        )

        fun none(): MoistureEstimate = MoistureEstimate(
            percentGravimetric = 0.0,
            method = MoistureMethod.NONE,
            confidence = 0.0,
            source = "No moisture measurement"
        )

        /** Returns a 0..1 penalty for uncertainty budgeting. */
        fun uncertaintyPenalty(moisture: MoistureEstimate): Double = when {
            moisture.method == MoistureMethod.NONE -> 0.0
            moisture.confidence >= 0.9 -> 0.05
            moisture.confidence >= 0.8 -> 0.12
            else -> 0.25
        }
    }
}
