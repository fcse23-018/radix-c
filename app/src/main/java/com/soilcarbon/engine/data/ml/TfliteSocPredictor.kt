package com.soilcarbon.engine.data.ml

import android.content.Context
import com.soilcarbon.engine.BuildConfig
import com.soilcarbon.engine.domain.ml.HeuristicSocRegressor
import com.soilcarbon.engine.domain.ml.SocPredictor
import com.soilcarbon.engine.domain.model.EnvironmentalCovariates
import com.soilcarbon.engine.domain.model.FeatureVector
import com.soilcarbon.engine.domain.model.ModelMetadata
import com.soilcarbon.engine.domain.model.Prediction
import com.soilcarbon.engine.domain.model.SoilProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import java.io.Closeable
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * TFLite-backed regressor. Loading is lazy and every failure path degrades to
 * [HeuristicSocRegressor]; the capture flow must remain functional with no model installed.
 */
class TfliteSocPredictor(
    private val context: Context,
    private val enableGpu: () -> Boolean = { true }
) : SocPredictor, Closeable {

    private val mutex = Mutex()
    private var interpreter: Interpreter? = null
    private var gpuDelegate: GpuDelegate? = null
    private var loadFailure: String? = null
    private var loadedMetadata: ModelMetadata = ModelMetadata.fallback()

    override val metadata: ModelMetadata
        get() = loadedMetadata

    val lastLoadFailure: String? get() = loadFailure

    private fun ensureLoaded() {
        if (interpreter != null || loadFailure != null) return
        try {
            val bytes = context.assets.open(BuildConfig.MODEL_ASSET).use { it.readBytes() }
            check(bytes.size > 1024) { "Model asset is too small to be a valid TFLite flatbuffer" }
            val options = Interpreter.Options().apply {
                setNumThreads(max(2, Runtime.getRuntime().availableProcessors() / 2))
            }
            if (enableGpu()) {
                runCatching {
                    val compat = CompatibilityList()
                    if (compat.isDelegateSupportedOnThisDevice) {
                        gpuDelegate = GpuDelegate()
                        options.addDelegate(gpuDelegate)
                    }
                }
            }
            val loaded = Interpreter(
                ByteBuffer.wrap(bytes).order(ByteOrder.nativeOrder()),
                options
            )
            val inputShape = loaded.getInputTensor(0).shape()
            val outputShape = loaded.getOutputTensor(0).shape()
            check(inputShape.size == 2 && inputShape[0] == 1 && inputShape[1] == 8) {
                "Model input must be [1,8], found ${inputShape.contentToString()}"
            }
            check(outputShape.isNotEmpty() && outputShape[0] == 1) {
                "Model output must start with batch size 1, found ${outputShape.contentToString()}"
            }
            interpreter = loaded
            loadedMetadata = ModelMetadataLoader.load(context)
        } catch (t: Throwable) {
            loadFailure = t.message ?: t.javaClass.simpleName
            runCatching { gpuDelegate?.close() }
            gpuDelegate = null
            interpreter = null
            loadedMetadata = ModelMetadataLoader.load(context).copy(
                isPlaceholder = true,
                description = "TFLite model unavailable (${loadFailure}); heuristic fallback active."
            )
        }
    }

    override suspend fun predict(
        features: FeatureVector,
        covariates: EnvironmentalCovariates?,
        soil: SoilProperties,
        moisturePenalty: Double
    ): Prediction = withContext(Dispatchers.IO) {
        mutex.withLock {
            ensureLoaded()
            val loaded = interpreter
            if (loaded == null) {
                return@withLock HeuristicSocRegressor.predict(features, covariates, soil, moisturePenalty)
            }
            try {
                val meta = loadedMetadata
                val values = features.toDoubleArray()
                val input = ByteBuffer.allocateDirect(8 * 4).order(ByteOrder.nativeOrder())
                for (i in 0 until 8) {
                    val scale = meta.featureScales[i].let { if (abs(it) < 1e-9) 1.0 else it }
                    input.putFloat(((values[i] - meta.featureMeans[i]) / scale).toFloat())
                }
                input.rewind()

                val outputShape = loaded.getOutputTensor(0).shape()
                val outputCount = outputShape.fold(1) { acc, value -> acc * value }
                val output = ByteBuffer.allocateDirect(outputCount * 4).order(ByteOrder.nativeOrder())
                val started = System.nanoTime()
                loaded.run(input, output)
                val elapsedMs = (System.nanoTime() - started) / 1_000_000
                output.rewind()

                val raw = FloatArray(outputCount) { output.float }
                val soc = (raw[0] * meta.outputScale + meta.outputOffset).coerceIn(0.01, 40.0)
                val staticUncertainty = (meta.validationRmsePercent * 1.96).coerceAtLeast(0.08)
                val modelSigma = if (raw.size >= 2 && raw[1].isFinite() && raw[1] > 0f) {
                    raw[1].toDouble()
                } else staticUncertainty
                val novelty = ModelMetadataLoader.featureVectorNovelty(features, meta)
                val uncertainty = (modelSigma + novelty * 0.06 + moisturePenalty * 0.8)
                    .coerceIn(0.08, 6.0)
                val modelConfidence = if (raw.size >= 3 && raw[2].isFinite()) {
                    raw[2].toDouble()
                } else {
                    (meta.validationR2.coerceIn(0.0, 1.0) * 0.92)
                }
                val confidence = (modelConfidence - novelty * 0.04 - moisturePenalty * 0.2)
                    .coerceIn(0.20, 0.98)
                HeuristicSocRegressor.buildPrediction(
                    soc = soc,
                    uncertainty = uncertainty,
                    confidence = confidence,
                    soil = soil,
                    metadata = meta,
                    fallbackUsed = meta.isPlaceholder,
                    notes = if (meta.isPlaceholder) {
                        "Placeholder model — replace with a validated regional TFLite model."
                    } else {
                        "TFLite inference (${meta.modelId} ${meta.modelVersion})."
                    },
                    inferenceMillis = elapsedMs
                )
            } catch (t: Throwable) {
                HeuristicSocRegressor.predict(features, covariates, soil, moisturePenalty)
            }
        }
    }

    override fun close() {
        runCatching { interpreter?.close() }
        runCatching { gpuDelegate?.close() }
        interpreter = null
        gpuDelegate = null
    }
}
