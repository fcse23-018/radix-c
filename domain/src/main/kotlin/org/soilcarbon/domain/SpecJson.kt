package org.soilcarbon.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

@Serializable private data class FeatureDefDto(
    val name: String, val unit: String, val min: Double, val max: Double,
    val required: Boolean, val fallback: Double? = null,
)
@Serializable private data class FeatureSpecDto(
    @SerialName("spec_version") val specVersion: String, val features: List<FeatureDefDto>,
)

fun parseFeatureSpec(text: String): FeatureSpec {
    val dto = json.decodeFromString<FeatureSpecDto>(text)
    return FeatureSpec(dto.specVersion, dto.features.map { FeatureDef(it.name, it.unit, it.min, it.max, it.required, it.fallback) })
}

@Serializable private data class TargetDto(val min: Double, val max: Double)
@Serializable private data class TrainingDto(@SerialName("data_sha256") val dataSha256: String)
@Serializable private data class ValidationDto(val passed: Boolean)
@Serializable private data class ModelMetadataDto(
    @SerialName("model_id") val modelId: String,
    @SerialName("model_version") val modelVersion: String,
    @SerialName("spec_version") val specVersion: String,
    @SerialName("feature_order") val featureOrder: List<String>,
    @SerialName("is_placeholder") val isPlaceholder: Boolean,
    @SerialName("fallback_features_trained") val fallbackFeaturesTrained: List<String>,
    val target: TargetDto, val training: TrainingDto, val validation: ValidationDto,
)

/** Parses the model_metadata.json sidecar produced by training/train_soc.py. */
fun parseModelMetadata(text: String): ModelMetadata {
    val d = json.decodeFromString<ModelMetadataDto>(text)
    return ModelMetadata(d.modelId, d.modelVersion, d.specVersion, d.featureOrder, d.isPlaceholder,
        d.fallbackFeaturesTrained, d.target.min, d.target.max, d.validation.passed, d.training.dataSha256)
}
