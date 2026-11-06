package com.soilcarbon.engine.nfr

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.soilcarbon.engine.data.ml.TfliteSocModel
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * NFR-1: "On-device Machine Learning inference (SOC % prediction from a single captured sample)
 * shall execute in less than 2.0 seconds on mid-tier mobile hardware."
 *
 * Device-only test. It measures the real TFLite interpreter (CPU/NNAPI chain) on the test device,
 * not capture, calibration or feature extraction. Run with:
 * `./gradlew connectedDebugAndroidTest --tests '*InferenceLatencyInstrumentedTest'`.
 */
@RunWith(AndroidJUnit4::class)
class InferenceLatencyInstrumentedTest {

    private lateinit var context: Context
    private lateinit var model: TfliteSocModel
    private lateinit var metricsFile: File

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        metricsFile = File(context.filesDir, "nfr1-results.txt").apply { writeText("") }
        // Force the CPU/NNAPI path so the measurement is not flattered by a GPU delegate.
        model = TfliteSocModel(context) { false }
    }

    @After
    fun tearDown() {
        model.close()
    }

    @Test
    fun nfr1_singleInferenceCompletesUnderTwoSeconds() {
        val features = FloatArray(model.metadata.featureOrder.size) { index ->
            // Plausible mid-range visible-colour / texture / covariate values.
            when (index) {
                0 -> 42f
                1 -> 6f
                2 -> 14f
                3 -> 5f
                4 -> 0.35f
                5 -> 0.25f
                6 -> 0.35f
                else -> 25f
            }
        }

        // Warm up interpreter load and the delegate chain; NFR-1 covers inference, not startup.
        repeat(WARMUP_RUNS) { model.predict(features) }

        val latenciesMs = LongArray(MEASURED_RUNS) {
            val started = System.nanoTime()
            model.predict(features)
            (System.nanoTime() - started) / 1_000_000
        }.sorted()

        val median = latenciesMs[latenciesMs.size / 2]
        val p95 = latenciesMs[(latenciesMs.size * 95 / 100).coerceAtMost(latenciesMs.lastIndex)]

        println("NFR-1 medianMs=$median p95Ms=$p95 runs=$MEASURED_RUNS limitMs=$NFR_LIMIT_MS")
        metricsFile.writeText(
            "NFR-1 medianMs=$median p95Ms=$p95 runs=$MEASURED_RUNS limitMs=$NFR_LIMIT_MS\n"
        )
        assertThat(median).isLessThan(NFR_LIMIT_MS)
        assertThat(p95).isLessThan(NFR_LIMIT_MS)
    }

    private companion object {
        const val WARMUP_RUNS = 5
        const val MEASURED_RUNS = 50
        const val NFR_LIMIT_MS = 2_000L
    }
}
