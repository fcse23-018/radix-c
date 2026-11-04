package com.soilcarbon.engine.domain.ml

import com.soilcarbon.engine.domain.model.FeatureVector
import com.soilcarbon.engine.domain.model.ModelMetadata
import com.soilcarbon.engine.domain.model.Prediction
import com.soilcarbon.engine.domain.model.EnvironmentalCovariates
import com.soilcarbon.engine.domain.model.SoilProperties
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Contract for the SOC regressor. Implementations must be safe to call off the main thread.
 * A model that cannot be loaded must degrade to [HeuristicSocRegressor] rather than failing the
 * capture: field work cannot wait for a download.
 */
interface SocPredictor {
    val metadata: ModelMetadata
    suspend fun predict(
        features: FeatureVector,
        covariates: EnvironmentalCovariates?,
        soil: SoilProperties,
        moisturePenalty: Double = 0.0
    ): Prediction
}

/**
 * Physics-guided visible-colour regression used when the TFLite model is missing or rejects the
 * input. Coefficients are deliberately conservative and are documented as research-grade only.
 */
object HeuristicSocRegressor : SocPredictor {

    override val metadata: ModelMetadata = ModelMetadata.fallback()

    override suspend fun predict(
        features: FeatureVector,
        covariates: EnvironmentalCovariates?,
        soil: SoilProperties,
        moisturePenalty: Double
    ): Prediction {
        val darkness = (100.0 - features.lStar) / 100.0
        val redness = (features.bStar - 8.0).coerceAtLeast(0.0) / 30.0
        val roughness = 1.0 - features.glcmEnergy
        val clay = features.clayContentPercent / 100.0
        val vegetation = features.ndviSatellite.coerceIn(-1.0, 1.0)

        val soc = (
            0.28 +
                darkness * 3.85 +
                redness * 0.55 +
                roughness * 0.95 +
                clay * 1.10 +
                max(0.0, vegetation) * 0.65
            ).coerceIn(0.05, 15.0)

        // Uncertainty budget: colour-model spread, texture instability, moisture proxy and the
        // inherent gap between visible reflectance and dry-combustion carbon.
        val relative = 0.17 +
            abs(0.55 - darkness) * 0.10 +
            moisturePenalty * 0.35 +
            if (features.ndviSatellite < 0.0) 0.05 else 0.0
        val uncertainty = (soc * relative + 0.12).coerceIn(0.08, 4.5)
        val confidence = (
            0.88 -
                moisturePenalty * 0.22 -
                if (features.glcmEnergy < 0.08) 0.08 else 0.0 -
                if (soc >= 14.5 || soc <= 0.08) 0.20 else 0.0
            ).coerceIn(0.30, 0.92)

        return buildPrediction(
            soc = soc,
            uncertainty = uncertainty,
            confidence = confidence,
            soil = soil,
            metadata = metadata,
            fallbackUsed = true,
            notes = "Heuristic fallback: uncalibrated visible-colour regression. " +
                "Use only for relative screening until a validated regional model is installed."
        )
    }

    fun buildPrediction(
        soc: Double,
        uncertainty: Double,
        confidence: Double,
        soil: SoilProperties,
        metadata: ModelMetadata,
        fallbackUsed: Boolean,
        notes: String,
        inferenceMillis: Long = 0L
    ): Prediction {
        val safeSoc = soc.coerceIn(0.01, 40.0)
        val lower = (safeSoc - uncertainty).coerceAtLeast(0.0)
        val upper = (safeSoc + uncertainty).coerceAtMost(60.0)
        val som = safeSoc * 1.724
        return Prediction(
            socPercent = safeSoc,
            somPercent = som,
            socLower95 = lower,
            socUpper95 = upper,
            uncertainty95 = upper - safeSoc,
            confidenceScore = confidence.coerceIn(0.0, 1.0),
            carbonStockMgPerHa = carbonStock(safeSoc, soil),
            modelId = metadata.modelId,
            modelVersion = metadata.modelVersion,
            inferenceMillis = inferenceMillis,
            fallbackUsed = fallbackUsed,
            notes = notes
        )
    }

    /** SOC stock (Mg C/ha) = SOC% x bulk density x depth(cm). See SRS section 10. */
    fun carbonStock(socPercent: Double, soil: SoilProperties): Double =
        (socPercent * soil.bulkDensityGcm3 * soil.depthCm).coerceAtLeast(0.0)

    fun confidenceBand(confidence: Double): String = when {
        confidence >= 0.80 -> "High"
        confidence >= 0.55 -> "Medium"
        else -> "Low"
    }

    fun roundedPercent(value: Double): String = "${(value * 100).roundToInt() / 100.0}%"
}
