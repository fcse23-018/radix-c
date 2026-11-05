package org.soilcarbon.domain

/** Where a feature value came from. DEFAULT must never be presented as a measurement. */
enum class FeatureSource { MEASURED, USER_ENTERED, DEFAULT }

data class FeatureDef(
    val name: String,
    val unit: String,
    val min: Double,
    val max: Double,
    val required: Boolean,
    val fallback: Double?,
) {
    init {
        require(min < max) { "feature '$name': min must be < max" }
        require(required || fallback != null) { "optional feature '$name' needs a fallback" }
        if (fallback != null) require(fallback in min..max) { "feature '$name': fallback out of range" }
    }
}

data class FeatureSpec(val specVersion: String, val features: List<FeatureDef>) {
    init { require(features.map { it.name }.toSet().size == features.size) { "duplicate feature names" } }
    val names: List<String> get() = features.map { it.name }
}

data class FeatureInput(val value: Double?, val source: FeatureSource)

sealed interface FeatureIssue {
    val feature: String
    data class Missing(override val feature: String) : FeatureIssue
    data class NotFinite(override val feature: String) : FeatureIssue
    data class OutOfRange(override val feature: String, val value: Double, val min: Double, val max: Double) : FeatureIssue
    data class Unknown(override val feature: String) : FeatureIssue
}

sealed interface ValidationResult {
    /** [values] are ordered exactly as [FeatureSpec.features]. */
    data class Valid(val values: List<Float>, val provenance: Map<String, FeatureSource>) : ValidationResult {
        val defaulted: List<String> get() = provenance.filterValues { it == FeatureSource.DEFAULT }.keys.toList()
    }
    data class Invalid(val issues: List<FeatureIssue>) : ValidationResult
}

/** Pure validation. Never clamps: an out-of-range value is an error, not something to quietly fix. */
object FeatureValidator {
    fun validate(spec: FeatureSpec, inputs: Map<String, FeatureInput>): ValidationResult {
        val issues = mutableListOf<FeatureIssue>()
        inputs.keys.filter { it !in spec.names }.forEach { issues += FeatureIssue.Unknown(it) }
        val values = mutableListOf<Float>()
        val provenance = linkedMapOf<String, FeatureSource>()
        for (def in spec.features) {
            val input = inputs[def.name]
            val raw = input?.value
            if (raw == null) {
                if (def.fallback != null) {
                    values += def.fallback.toFloat(); provenance[def.name] = FeatureSource.DEFAULT
                } else issues += FeatureIssue.Missing(def.name)
                continue
            }
            when {
                !raw.isFinite() -> issues += FeatureIssue.NotFinite(def.name)
                raw < def.min || raw > def.max -> issues += FeatureIssue.OutOfRange(def.name, raw, def.min, def.max)
                else -> { values += raw.toFloat(); provenance[def.name] = input.source }
            }
        }
        return if (issues.isEmpty()) ValidationResult.Valid(values, provenance) else ValidationResult.Invalid(issues)
    }
}
