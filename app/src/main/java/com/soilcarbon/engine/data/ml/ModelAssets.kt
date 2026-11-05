package com.soilcarbon.engine.data.ml

import android.content.Context
import com.soilcarbon.engine.BuildConfig
import com.soilcarbon.engine.domain.model.ProvenanceHasher
import org.soilcarbon.domain.FeatureSpec
import org.soilcarbon.domain.ModelMetadata
import org.soilcarbon.domain.parseFeatureSpec
import org.soilcarbon.domain.parseModelMetadata
import com.soilcarbon.engine.domain.model.ModelMetadata as AppModelMetadata

/**
 * Single loader for the two contracts that ship inside the APK: the feature spec and the model
 * sidecar. Both are parsed by the pure-JVM domain module, so the app and the training pipeline
 * cannot drift apart silently.
 */
object ModelAssets {

    fun featureSpec(context: Context): FeatureSpec =
        context.assets.open(BuildConfig.FEATURE_SPEC_ASSET).bufferedReader().use {
            parseFeatureSpec(it.readText())
        }

    fun metadata(context: Context): ModelMetadata =
        context.assets.open(BuildConfig.MODEL_METADATA_ASSET).bufferedReader().use {
            parseModelMetadata(it.readText())
        }

    fun modelBytes(context: Context): ByteArray =
        context.assets.open(BuildConfig.MODEL_ASSET).use { it.readBytes() }

    fun sha256(context: Context): String =
        runCatching { ProvenanceHasher.sha256Hex(modelBytes(context)) }.getOrDefault("")

    fun sizeBytes(context: Context): Long =
        runCatching { context.assets.openFd(BuildConfig.MODEL_ASSET).use { it.length } }.getOrDefault(0L)
}

fun ModelMetadata.toAppModelMetadata(sha256: String = ""): AppModelMetadata = AppModelMetadata(
    modelId = modelId,
    modelVersion = modelVersion,
    description = description,
    featureOrder = featureOrder,
    trainingDataset = trainingDataset,
    validationRmsePercent = validationRmse,
    validationR2 = validationR2,
    uncertaintyMode = uncertaintyMode,
    featureMeans = featureMeans.ifEmpty { AppModelMetadata.fallback().featureMeans },
    featureScales = featureScales.ifEmpty { AppModelMetadata.fallback().featureScales },
    outputScale = outputScale,
    outputOffset = outputOffset,
    isPlaceholder = isPlaceholder,
    createdAtUtc = "",
    sha256 = sha256,
)
