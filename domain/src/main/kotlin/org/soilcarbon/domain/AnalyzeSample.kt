package org.soilcarbon.domain

/** Port: implemented by the TFLite adapter in the Android module, and by fakes in tests. */
interface SocModel {
    val metadata: ModelMetadata
    fun predict(features: FloatArray): Float
}

enum class PlaceholderPolicy { BLOCK, ALLOW_DEMO }
enum class ReportStatus { VALID, DEMO_NOT_VALID }

data class Prediction(
    val socPercent: Double,
    val modelId: String,
    val modelVersion: String,
    val status: ReportStatus,
    val provenance: Map<String, FeatureSource>,
    val defaulted: List<String>,
) {
    /** True when any input was a default rather than a real value. */
    val degraded: Boolean get() = defaulted.isNotEmpty()
}

sealed interface RejectReason {
    data class InvalidFeatures(val issues: List<FeatureIssue>) : RejectReason
    data class ModelRejected(val problems: List<GateProblem>) : RejectReason
    data object PlaceholderBlocked : RejectReason
    data class BadModelOutput(val value: Float) : RejectReason
}

sealed interface AnalysisResult {
    data class Success(val prediction: Prediction) : AnalysisResult
    data class Rejected(val reason: RejectReason) : AnalysisResult
}

/** The ONLY path from features to a number. Validation, gating and output checks live here, not in the UI. */
class AnalyzeSample(
    private val model: SocModel,
    private val spec: FeatureSpec,
    private val placeholderPolicy: PlaceholderPolicy = PlaceholderPolicy.BLOCK,
) {
    operator fun invoke(inputs: Map<String, FeatureInput>): AnalysisResult {
        val meta = model.metadata
        val problems = ModelGate.check(spec, meta)
        if (problems.isNotEmpty()) return AnalysisResult.Rejected(RejectReason.ModelRejected(problems))
        if (meta.isPlaceholder && placeholderPolicy == PlaceholderPolicy.BLOCK)
            return AnalysisResult.Rejected(RejectReason.PlaceholderBlocked)

        val validated = when (val v = FeatureValidator.validate(spec, inputs)) {
            is ValidationResult.Invalid -> return AnalysisResult.Rejected(RejectReason.InvalidFeatures(v.issues))
            is ValidationResult.Valid -> v
        }
        val raw = model.predict(validated.values.toFloatArray())
        if (!raw.isFinite() || raw < meta.targetMin || raw > meta.targetMax)
            return AnalysisResult.Rejected(RejectReason.BadModelOutput(raw))

        return AnalysisResult.Success(
            Prediction(
                socPercent = raw.toDouble(),
                modelId = meta.modelId,
                modelVersion = meta.modelVersion,
                status = if (meta.isPlaceholder) ReportStatus.DEMO_NOT_VALID else ReportStatus.VALID,
                provenance = validated.provenance,
                defaulted = validated.defaulted,
            )
        )
    }
}
