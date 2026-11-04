package com.soilcarbon.engine.domain.usecase

import com.soilcarbon.engine.domain.ml.SocPredictor
import com.soilcarbon.engine.domain.model.CalibrationSummary
import com.soilcarbon.engine.domain.model.EnvironmentalCovariates
import com.soilcarbon.engine.domain.model.ProvenanceHasher
import com.soilcarbon.engine.domain.model.SampleStatus
import com.soilcarbon.engine.domain.model.SoilProperties
import com.soilcarbon.engine.domain.model.SoilSample
import com.soilcarbon.engine.domain.vision.ColorFeatures
import com.soilcarbon.engine.domain.vision.MoistureCorrection
import com.soilcarbon.engine.domain.vision.MoistureEstimate
import com.soilcarbon.engine.domain.vision.TextureFeatures

data class AnalysisInput(
    val sample: SoilSample,
    val color: ColorFeatures,
    val texture: TextureFeatures,
    val covariates: EnvironmentalCovariates,
    val calibration: CalibrationSummary? = sample.calibration,
    val moisture: MoistureEstimate? = sample.moisture,
    val moistureCorrection: MoistureCorrection? = null,
    val soil: SoilProperties = sample.soil
)

/**
 * Single orchestration point for the scientifically significant transformation from captured
 * pixels to a stored SOC estimate. Keeping this atomic makes the audit trail reproducible.
 */
class AnalyzeSampleUseCase(
    private val predictor: SocPredictor
) {
    suspend fun invoke(input: AnalysisInput): SoilSample {
        val features = FeatureVectorFactory.build(input.color, input.texture, input.covariates)
        val penalty = input.moisture?.let { com.soilcarbon.engine.domain.vision.EpoMoistureCorrector.uncertaintyPenalty(it) } ?: 0.0
        val prediction = predictor.predict(
            features = features,
            covariates = input.covariates,
            soil = input.soil,
            moisturePenalty = penalty
        )

        val analyzed = input.sample.copy(
            status = SampleStatus.ANALYZED,
            capturedAtMillis = input.sample.capturedAtMillis ?: System.currentTimeMillis(),
            color = input.color,
            lab = input.color.lab,
            munsell = input.color.munsell,
            reflectance = input.color.reflectance,
            texture = input.texture,
            features = features,
            prediction = prediction,
            covariates = input.covariates,
            calibration = input.calibration ?: input.sample.calibration,
            moisture = input.moisture ?: input.sample.moisture,
            moistureCorrectionNotes = input.moistureCorrection?.notes ?: input.sample.moistureCorrectionNotes,
            soil = input.soil,
            modelId = prediction.modelId,
            modelVersion = prediction.modelVersion,
            errorMessage = null
        )
        val digest = ProvenanceHasher.sha256(analyzed)
        return analyzed.copy(provenanceSha256 = digest)
    }
}
