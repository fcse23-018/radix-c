package com.soilcarbon.engine.domain.usecase

import com.soilcarbon.engine.domain.model.Prediction
import com.soilcarbon.engine.domain.model.SoilProperties
import com.soilcarbon.engine.domain.model.FeatureVector
import org.soilcarbon.domain.ModelMetadata
import org.soilcarbon.domain.Prediction as DomainPrediction
import org.soilcarbon.domain.ReportStatus
import kotlin.math.max

/**
 * Turns the domain result (the only number that may be reported) into the richer UI/export record.
 * No SOC value is estimated here: SOC and the reporting status come straight from
 * [org.soilcarbon.domain.AnalyzeSample]. This class only derives presentation fields (SOM, CI,
 * stock) from that value and the model's validation metrics.
 */
object PredictionAssembler {

    fun from(
        prediction: DomainPrediction,
        metadata: ModelMetadata,
        soil: SoilProperties
    ): Prediction {
        val soc = prediction.socPercent.coerceIn(0.01, 40.0)
        val uncertainty = (
            prediction.uncertainty95
                ?: (metadata.validationRmse.takeIf { it > 0.0 }?.times(1.96) ?: 0.62)
            ).coerceIn(0.08, 6.0)
        val lower = (soc - uncertainty).coerceAtLeast(0.0)
        val upper = (soc + uncertainty).coerceAtMost(60.0)
        val confidence = (
            prediction.confidence
                ?: metadata.validationR2.takeIf { it > 0.0 }
                ?: 0.5
            ).coerceIn(0.0, 1.0)
        val demo = prediction.status == ReportStatus.DEMO_NOT_VALID

        return Prediction(
            socPercent = soc,
            somPercent = max(soc * Prediction.VAN_BEMMELEN_FACTOR, soc),
            socLower95 = lower,
            socUpper95 = upper,
            uncertainty95 = upper - soc,
            confidenceScore = confidence,
            carbonStockMgPerHa = carbonStock(soc, soil),
            modelId = prediction.modelId,
            modelVersion = prediction.modelVersion,
            inferenceMillis = prediction.inferenceMillis,
            fallbackUsed = demo,
            notes = buildString {
                if (demo) {
                    append("DEMO – NOT VALID. Placeholder model; never use these numbers for ")
                    append("reporting, accounting or land-management decisions.")
                } else {
                    append("TFLite inference (${prediction.modelId} ${prediction.modelVersion}).")
                }
                if (prediction.degraded) {
                    append(" Degraded: defaulted input(s) ")
                    append(prediction.defaulted.joinToString(", "))
                    append('.')
                }
            },
            reportStatus = prediction.status.name,
            degraded = prediction.degraded,
            provenance = prediction.provenance.mapValues { it.value.name },
        )
    }

    /** SOC stock (Mg C/ha) = SOC% x bulk density x depth(cm). See SRS section 10. */
    fun carbonStock(socPercent: Double, soil: SoilProperties): Double =
        (socPercent * soil.bulkDensityGcm3 * soil.depthCm).coerceAtLeast(0.0)
}
