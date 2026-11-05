package com.soilcarbon.engine.data.ml

import android.content.Context
import com.soilcarbon.engine.BuildConfig
import org.soilcarbon.domain.InferenceDiagnostics
import org.soilcarbon.domain.ModelMetadata
import org.soilcarbon.domain.SocModel
import org.soilcarbon.domain.parseModelMetadata
import org.tensorflow.lite.Delegate
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.nnapi.NnApiDelegate
import java.io.Closeable
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.max

/** Hardware delegate preference, in fallback order. */
internal enum class DelegateKind { GPU, NNAPI, CPU }

internal fun delegateChain(preferGpu: Boolean): List<DelegateKind> = buildList {
    if (preferGpu) add(DelegateKind.GPU)
    add(DelegateKind.NNAPI)
    add(DelegateKind.CPU)
}

/**
 * TFLite adapter for the pure-JVM [SocModel] port.
 *
 * - loads `soc_model.tflite` and parses `model_metadata.json` with [parseModelMetadata];
 * - uses a GPU -> NNAPI -> CPU delegate chain, rebuilt when the Settings GPU toggle changes;
 * - normalises inputs with the means/scales in the sidecar and applies the output scale/offset;
 * - never decides whether a number may be reported. That belongs to
 *   [org.soilcarbon.domain.AnalyzeSample], the only inference entry point.
 */
class TfliteSocModel(
    private val context: Context,
    private val gpuEnabled: () -> Boolean = { true },
) : SocModel, Closeable {

    override val metadata: ModelMetadata = runCatching {
        context.assets.open(BuildConfig.MODEL_METADATA_ASSET).bufferedReader().use {
            parseModelMetadata(it.readText())
        }
    }.getOrElse { failure ->
        ModelMetadata(
            modelId = "unavailable",
            modelVersion = "0.0.0",
            specVersion = "",
            featureOrder = emptyList(),
            isPlaceholder = true,
            fallbackFeaturesTrained = emptyList(),
            targetMin = 0.0,
            targetMax = 0.0,
            validationPassed = false,
            dataSha256 = "",
            description = "Model sidecar could not be parsed: ${failure.message}",
        )
    }

    private val lock = Any()
    private var interpreter: Interpreter? = null
    private var delegate: Delegate? = null
    private var builtGpuPreference: Boolean? = null
    private var loadFailure: String? = null
    private var lastDiagnostics: InferenceDiagnostics? = null

    val lastLoadFailure: String? get() = synchronized(lock) { loadFailure }

    override fun diagnostics(): InferenceDiagnostics? = synchronized(lock) { lastDiagnostics }

    override fun predict(features: FloatArray): Float = synchronized(lock) {
        ensureLoadedLocked()
        val active = interpreter ?: error("Model unavailable: ${loadFailure ?: "interpreter not loaded"}")
        check(features.size == metadata.featureOrder.size) {
            "Expected ${metadata.featureOrder.size} features, got ${features.size}"
        }
        check(features.all { it.isFinite() }) { "Non-finite feature value" }

        val inputBuffer = ByteBuffer.allocateDirect(features.size * Float.SIZE_BYTES)
            .order(ByteOrder.nativeOrder())
        val means = metadata.featureMeans
        val scales = metadata.featureScales
        val normalise = means.size == features.size && scales.size == features.size
        features.forEachIndexed { index, value ->
            val normalised = if (normalise) {
                val scale = scales[index].takeIf { abs(it) > 1e-9 } ?: 1.0
                ((value - means[index]) / scale).toFloat()
            } else {
                value
            }
            inputBuffer.putFloat(normalised)
        }
        inputBuffer.rewind()

        val outputShape = active.getOutputTensor(0).shape()
        val outputCount = outputShape.fold(1) { acc, value -> acc * value }
        check(outputCount >= 1) { "Model has no outputs" }
        val outputBuffer = ByteBuffer.allocateDirect(outputCount * Float.SIZE_BYTES)
            .order(ByteOrder.nativeOrder())

        val started = System.nanoTime()
        active.run(inputBuffer, outputBuffer)
        val elapsedMs = (System.nanoTime() - started) / 1_000_000
        outputBuffer.rewind()
        val raw = FloatArray(outputCount) { outputBuffer.float }

        val soc = (raw[0] * metadata.outputScale + metadata.outputOffset).toFloat()
        lastDiagnostics = InferenceDiagnostics(
            uncertainty95 = raw.getOrNull(1)?.takeIf { it.isFinite() && it > 0f }?.toDouble(),
            confidence = raw.getOrNull(2)?.takeIf { it.isFinite() }?.toDouble(),
            inferenceMillis = elapsedMs,
        )
        soc
    }

    private fun ensureLoadedLocked() {
        val preference = gpuEnabled()
        if (interpreter != null && builtGpuPreference == preference) return
        closeInterpreterLocked()

        try {
            val bytes = context.assets.open(BuildConfig.MODEL_ASSET).use { it.readBytes() }
            check(bytes.size > 1024) { "Model asset is too small to be a valid TFLite flatbuffer" }
            val loaded = createInterpreter(bytes, preference)
            val inputShape = loaded.getInputTensor(0).shape()
            val outputShape = loaded.getOutputTensor(0).shape()
            check(
                inputShape.size == 2 && inputShape[0] == 1 &&
                    inputShape[1] == metadata.featureOrder.size
            ) {
                "Model input must be [1,${metadata.featureOrder.size}], found ${inputShape.contentToString()}"
            }
            check(outputShape.isNotEmpty() && outputShape[0] == 1) {
                "Model output must start with batch size 1, found ${outputShape.contentToString()}"
            }
            interpreter = loaded
            builtGpuPreference = preference
            loadFailure = null
        } catch (failure: Throwable) {
            closeInterpreterLocked()
            loadFailure = failure.message ?: failure.javaClass.simpleName
        }
    }

    private fun createInterpreter(bytes: ByteArray, preferGpu: Boolean): Interpreter {
        var lastFailure: Throwable? = null
        for (kind in delegateChain(preferGpu)) {
            var candidate: Delegate? = null
            try {
                val options = Interpreter.Options().apply {
                    setNumThreads(max(2, Runtime.getRuntime().availableProcessors() / 2))
                }
                when (kind) {
                    DelegateKind.GPU -> {
                        val compatibility = CompatibilityList()
                        check(compatibility.isDelegateSupportedOnThisDevice) {
                            "GPU delegate is not supported on this device"
                        }
                        candidate = GpuDelegate()
                    }
                    DelegateKind.NNAPI -> candidate = NnApiDelegate()
                    DelegateKind.CPU -> Unit
                }
                candidate?.let(options::addDelegate)
                val created = Interpreter(
                    ByteBuffer.wrap(bytes).order(ByteOrder.nativeOrder()),
                    options
                )
                delegate = candidate
                return created
            } catch (failure: Throwable) {
                lastFailure = failure
                runCatching { candidate?.close() }
            }
        }
        throw IllegalStateException("No TFLite delegate could load the model", lastFailure)
    }

    private fun closeInterpreterLocked() {
        runCatching { interpreter?.close() }
        runCatching { delegate?.close() }
        interpreter = null
        delegate = null
        builtGpuPreference = null
        lastDiagnostics = null
    }

    override fun close() {
        synchronized(lock) { closeInterpreterLocked() }
    }
}
