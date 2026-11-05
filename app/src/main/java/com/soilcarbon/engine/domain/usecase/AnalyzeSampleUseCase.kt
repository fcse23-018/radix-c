package com.soilcarbon.engine.domain.usecase

import com.soilcarbon.engine.domain.model.CalibrationSummary
import com.soilcarbon.engine.domain.model.EnvironmentalCovariates
import com.soilcarbon.engine.domain.model.FeatureVector
import com.soilcarbon.engine.domain.model.ProvenanceHasher
import com.soilcarbon.engine.domain.model.SampleStatus
import com.soilcarbon.engine.domain.model.SoilProperties
import com.soilcarbon.engine.domain.model.SoilSample
import com.soilcarbon.engine.domain.vision.ColorFeatures
import com.soilcarbon.engine.domain.vision.MoistureCorrection
import com.soilcarbon.engine.domain.vision.MoistureEstimate
import com.soilcarbon.engine.domain.vision.TextureFeatures
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.soilcarbon.domain.AnalysisResult
import org.soilcarbon.domain.AnalyzeSample
import org.soilcarbon.domain.FeatureIssue
import org.soilcarbon.domain.GateProblem
import org.soilcarbon.domain.RejectReason
import org.soilcarbon.domain.SocModel

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

sealed interface AnalysisOutcome {
    data class Success(val sample: SoilSample) : AnalysisOutcome
    data class Rejected(val reason: RejectReason, val message: String) : AnalysisOutcome
}

/**
 * Single orchestration point for the scientifically significant transformation from captured
 * pixels to a stored SOC estimate. Every number passes through [AnalyzeSample]; this class only
 * assembles inputs, maps the domain result to the app record, and computes its provenance digest.
 */
class AnalyzeSampleUseCase(
    private val analyzeSample: AnalyzeSample,
    private val model: SocModel,
    private val ioDispatcher: CoroutineDispatcher = com.soilcarbon.engine.core.Dispatchers.io,
) {
    suspend fun invoke(input: AnalysisInput): AnalysisOutcome = withContext(ioDispatcher) {
        val inputs = FeatureInputFactory.build(input.color, input.texture, input.covariates)
        val result = runCatching { analyzeSample(inputs) }.getOrElse { failure ->
            return@withContext AnalysisOutcome.Rejected(
                reason = RejectReason.BadModelOutput(Float.NaN),
                message = "The model could not be run: ${failure.message ?: failure.javaClass.simpleName}"
            )
        }

        when (result) {
            is AnalysisResult.Rejected -> AnalysisOutcome.Rejected(
                reason = result.reason,
                message = result.reason.toUserMessage()
            )
            is AnalysisResult.Success -> {
                val features = FeatureVectorFactory.build(input.color, input.texture, input.covariates)
                val prediction = PredictionAssembler.from(result.prediction, model.metadata, input.soil)
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
                    moistureCorrectionNotes = input.moistureCorrection?.notes
                        ?: input.sample.moistureCorrectionNotes,
                    soil = input.soil,
                    modelId = prediction.modelId,
                    modelVersion = prediction.modelVersion,
                    errorMessage = null,
                )
                AnalysisOutcome.Success(analyzed.copy(provenanceSha256 = ProvenanceHasher.sha256(analyzed)))
            }
        }
    }

    private fun RejectReason.toUserMessage(): String = when (this) {
        is RejectReason.InvalidFeatures -> "Capture rejected: " + issues.joinToString("; ") { it.describe() }
        is RejectReason.ModelRejected -> "Model refused: " + problems.joinToString("; ") { it.describe() }
        RejectReason.PlaceholderBlocked ->
            "The bundled model is a placeholder. Release builds do not report SOC from it; " +
                "install a validated regional model or use a debug build for demonstration only."
        is RejectReason.BadModelOutput ->
            "The model returned an implausible value ($value). Nothing was saved."
    }

    private fun FeatureIssue.describe(): String = when (this) {
        is FeatureIssue.Missing -> "$feature is missing"
        is FeatureIssue.NotFinite -> "$feature is not a finite number"
        is FeatureIssue.OutOfRange -> "$feature=$value is outside $min..$max"
        is FeatureIssue.Unknown -> "$feature is not part of the model contract"
    }

    private fun GateProblem.describe(): String = when (this) {
        is GateProblem.SpecVersionMismatch -> "sidecar spec $actual does not match app spec $expected"
        is GateProblem.FeatureOrderMismatch -> "sidecar feature order does not match the app contract"
        is GateProblem.FallbackNotTrained -> "$feature was never trained with its fallback value"
        GateProblem.NotValidated -> "the model has not passed validation"
        GateProblem.MissingTrainingHash -> "the sidecar has no training-data hash"
    }
}
