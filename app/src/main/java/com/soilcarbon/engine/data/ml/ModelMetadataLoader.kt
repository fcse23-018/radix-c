package com.soilcarbon.engine.data.ml

import android.content.Context
import com.google.gson.Gson
import com.soilcarbon.engine.BuildConfig
import com.soilcarbon.engine.domain.model.FeatureVector
import com.soilcarbon.engine.domain.model.ModelMetadata
import com.soilcarbon.engine.domain.model.ProvenanceHasher
import kotlin.math.abs

private data class ModelMetadataDto(
    val model_id: String = ModelMetadata.FALLBACK_MODEL_ID,
    val model_version: String = ModelMetadata.FALLBACK_MODEL_VERSION,
    val description: String = "",
    val feature_order: List<String> = FeatureVector.FEATURE_NAMES,
    val training_dataset: String = "",
    val validation_rmse_percent: Double = 0.62,
    val validation_r2: Double = 0.58,
    val uncertainty_mode: String = "fixed-sigma",
    val feature_means: List<Double> = listOf(42.0, 6.0, 14.0, 5.0, 0.35, 0.25, 0.35, 25.0),
    val feature_scales: List<Double> = listOf(18.0, 11.0, 14.0, 3.0, 0.30, 0.20, 0.25, 12.0),
    val output_scale: Double = 1.0,
    val output_offset: Double = 0.0,
    val is_placeholder: Boolean = true,
    val created_at_utc: String = "",
    val sha256: String = ""
)

object ModelMetadataLoader {

    private val gson = Gson()

    fun load(context: Context, assetPath: String = BuildConfig.MODEL_METADATA_ASSET): ModelMetadata =
        runCatching {
            context.assets.open(assetPath).bufferedReader().use { reader ->
                val dto = gson.fromJson(reader, ModelMetadataDto::class.java)
                dto.toDomain()
            }
        }.getOrElse { ModelMetadata.fallback() }

    fun sha256(context: Context, assetPath: String = BuildConfig.MODEL_ASSET): String =
        runCatching {
            context.assets.open(assetPath).use { input ->
                ProvenanceHasher.sha256Hex(input.readBytes())
            }
        }.getOrElse { "" }

    private fun ModelMetadataDto.toDomain(): ModelMetadata {
        val order = if (feature_order.size == 8) feature_order else FeatureVector.FEATURE_NAMES
        val means = if (feature_means.size == 8) feature_means else ModelMetadata.fallback().featureMeans
        val scales = if (feature_scales.size == 8) feature_scales else ModelMetadata.fallback().featureScales
        return ModelMetadata(
            modelId = model_id,
            modelVersion = model_version,
            description = description,
            featureOrder = order,
            trainingDataset = training_dataset,
            validationRmsePercent = validation_rmse_percent,
            validationR2 = validation_r2,
            uncertaintyMode = uncertainty_mode,
            featureMeans = means,
            featureScales = scales,
            outputScale = output_scale,
            outputOffset = output_offset,
            isPlaceholder = is_placeholder,
            createdAtUtc = created_at_utc,
            sha256 = sha256
        )
    }

    fun featureVectorNovelty(features: FeatureVector, metadata: ModelMetadata): Double {
        val values = features.toDoubleArray()
        var sum = 0.0
        for (i in 0 until 8) {
            val scale = metadata.featureScales[i].let { if (abs(it) < 1e-9) 1.0 else it }
            val z = (values[i] - metadata.featureMeans[i]) / scale
            sum += abs(z)
        }
        return (sum / 8.0).coerceIn(0.0, 6.0)
    }
}
