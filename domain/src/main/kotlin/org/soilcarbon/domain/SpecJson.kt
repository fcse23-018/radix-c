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
@Serializable private data class TrainingDto(
    @SerialName("data_sha256") val dataSha256: String,
    val n_samples: Int = 0,
    val n_sites: Int = 0,
    @SerialName("created_utc") val createdUtc: String = "",
    @SerialName("lab_method") val labMethod: String = "",
)
@Serializable private data class ValidationDto(
    val passed: Boolean,
    val rmse: Double = 0.0,
    val r2: Double = 0.0,
    val scheme: String = "",
)
@Serializable private data class ModelMetadataDto(
    @SerialName("model_id") val modelId: String,
    @SerialName("model_version") val modelVersion: String,
    @SerialName("spec_version") val specVersion: String,
    val description: String = "",
    @SerialName("feature_order") val featureOrder: List<String>,
    @SerialName("is_placeholder") val isPlaceholder: Boolean,
    @SerialName("fallback_features_trained") val fallbackFeaturesTrained: List<String>,
    val target: TargetDto, val training: TrainingDto, val validation: ValidationDto,
    @SerialName("feature_means") val featureMeans: List<Double> = emptyList(),
    @SerialName("feature_scales") val featureScales: List<Double> = emptyList(),
    @SerialName("output_scale") val outputScale: Double = 1.0,
    @SerialName("output_offset") val outputOffset: Double = 0.0,
    @SerialName("uncertainty_mode") val uncertaintyMode: String = "fixed-sigma",
)

/** Parses the model_metadata.json sidecar produced by training/train_soc.py. */
fun parseModelMetadata(text: String): ModelMetadata {
    val d = json.decodeFromString<ModelMetadataDto>(text)
    return ModelMetadata(
        modelId = d.modelId,
        modelVersion = d.modelVersion,
        specVersion = d.specVersion,
        featureOrder = d.featureOrder,
        isPlaceholder = d.isPlaceholder,
        fallbackFeaturesTrained = d.fallbackFeaturesTrained,
        targetMin = d.target.min,
        targetMax = d.target.max,
        validationPassed = d.validation.passed,
        dataSha256 = d.training.dataSha256,
        description = d.description,
        trainingDataset = listOfNotNull(
            d.training.labMethod.takeIf { it.isNotBlank() },
            d.training.n_samples.takeIf { it > 0 }?.let { "$it samples" },
            d.training.n_sites.takeIf { it > 0 }?.let { "$it sites" },
        ).joinToString(", "),
        validationRmse = d.validation.rmse,
        validationR2 = d.validation.r2,
        uncertaintyMode = d.uncertaintyMode,
        featureMeans = d.featureMeans,
        featureScales = d.featureScales,
        outputScale = d.outputScale,
        outputOffset = d.outputOffset,
    )
}
