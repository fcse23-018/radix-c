package com.soilcarbon.engine.nfr

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.soilcarbon.engine.data.ml.TfliteSocModel
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

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

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
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

        // Measure in microseconds: the placeholder model can run in well under one millisecond.
        val latenciesUs = LongArray(MEASURED_RUNS) {
            val started = System.nanoTime()
            model.predict(features)
            (System.nanoTime() - started) / 1_000
        }.sorted()

        val medianUs = latenciesUs[latenciesUs.size / 2]
        val p95Us = latenciesUs[(latenciesUs.size * 95 / 100).coerceAtMost(latenciesUs.lastIndex)]
        val medianMs = medianUs / 1_000.0
        val p95Ms = p95Us / 1_000.0

        Log.i(
            "SoilCarbonNFR",
            "NFR-1 medianMs=$medianMs p95Ms=$p95Ms runs=$MEASURED_RUNS limitMs=${NFR_LIMIT_US / 1_000}"
        )
        assertThat(medianUs).isLessThan(NFR_LIMIT_US)
        assertThat(p95Us).isLessThan(NFR_LIMIT_US)
    }

    private companion object {
        const val WARMUP_RUNS = 5
        const val MEASURED_RUNS = 50
        const val NFR_LIMIT_US = 2_000_000L
    }
}
