package org.soilcarbon.domain

data class ModelMetadata(
    val modelId: String,
    val modelVersion: String,
    val specVersion: String,
    val featureOrder: List<String>,
    val isPlaceholder: Boolean,
    val fallbackFeaturesTrained: List<String>,
    val targetMin: Double,
    val targetMax: Double,
    val validationPassed: Boolean,
    val dataSha256: String,
)

sealed interface GateProblem {
    data class SpecVersionMismatch(val expected: String, val actual: String) : GateProblem
    data class FeatureOrderMismatch(val expected: List<String>, val actual: List<String>) : GateProblem
    data class FallbackNotTrained(val feature: String) : GateProblem
    data object NotValidated : GateProblem
    data object MissingTrainingHash : GateProblem
}

/** The app refuses to run a model whose sidecar does not match the feature contract. */
object ModelGate {
    fun check(spec: FeatureSpec, meta: ModelMetadata): List<GateProblem> {
        val problems = mutableListOf<GateProblem>()
        if (meta.specVersion != spec.specVersion) problems += GateProblem.SpecVersionMismatch(spec.specVersion, meta.specVersion)
        if (meta.featureOrder != spec.names) problems += GateProblem.FeatureOrderMismatch(spec.names, meta.featureOrder)
        spec.features.filter { it.fallback != null && it.name !in meta.fallbackFeaturesTrained }
            .forEach { problems += GateProblem.FallbackNotTrained(it.name) }
        if (!meta.isPlaceholder) {
            if (!meta.validationPassed) problems += GateProblem.NotValidated
            if (meta.dataSha256.isBlank()) problems += GateProblem.MissingTrainingHash
        }
        return problems
    }
}
