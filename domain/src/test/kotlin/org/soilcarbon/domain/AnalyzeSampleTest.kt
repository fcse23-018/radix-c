package org.soilcarbon.domain

import kotlin.test.*

class AnalyzeSampleTest {
    private val spec = testSpec()

    @Test fun happyPathReturnsValidAndFeedsModelInSpecOrder() {
        val model = FakeModel(testMeta())
        val r = AnalyzeSample(model, spec)(goodInputs()) as AnalysisResult.Success
        assertEquals(ReportStatus.VALID, r.prediction.status)
        assertContentEquals(floatArrayOf(40f, 12f, 0.6f), model.lastInput)
        assertFalse(r.prediction.degraded)
    }

    @Test fun invalidFeaturesNeverReachTheModel() {
        val model = FakeModel(testMeta())
        val r = AnalyzeSample(model, spec)(goodInputs() + ("lab_l" to FeatureInput(500.0, FeatureSource.MEASURED)))
        assertIs<RejectReason.InvalidFeatures>((r as AnalysisResult.Rejected).reason)
        assertEquals(0, model.calls)
    }

    @Test fun placeholderIsBlockedByDefault() {
        val model = FakeModel(testMeta(placeholder = true))
        val r = AnalyzeSample(model, spec)(goodInputs())
        assertEquals(RejectReason.PlaceholderBlocked, (r as AnalysisResult.Rejected).reason)
        assertEquals(0, model.calls)
    }

    @Test fun placeholderInDemoModeIsStampedNotValid() {
        val r = AnalyzeSample(FakeModel(testMeta(placeholder = true)), spec, PlaceholderPolicy.ALLOW_DEMO)(goodInputs())
        assertEquals(ReportStatus.DEMO_NOT_VALID, (r as AnalysisResult.Success).prediction.status)
    }

    @Test fun defaultedNdviMarksPredictionDegraded() {
        val r = AnalyzeSample(FakeModel(testMeta()), spec)(goodInputs() - "ndvi_sat") as AnalysisResult.Success
        assertTrue(r.prediction.degraded)
        assertEquals(FeatureSource.DEFAULT, r.prediction.provenance["ndvi_sat"])
    }

    @Test fun mismatchedModelIsRejectedBeforePredicting() {
        val model = FakeModel(testMeta(spec = "9.9.9"))
        val r = AnalyzeSample(model, spec)(goodInputs())
        assertIs<RejectReason.ModelRejected>((r as AnalysisResult.Rejected).reason)
        assertEquals(0, model.calls)
    }

    @Test fun badModelOutputIsRejected() {
        for (bad in listOf(Float.NaN, -1f, 99f)) {
            val r = AnalyzeSample(FakeModel(testMeta(), bad), spec)(goodInputs())
            assertIs<RejectReason.BadModelOutput>((r as AnalysisResult.Rejected).reason)
        }
    }
}
