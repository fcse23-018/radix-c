package org.soilcarbon.domain

import kotlin.test.*

class ModelGateTest {
    private val spec = testSpec()

    @Test fun matchingModelPasses() {
        assertTrue(ModelGate.check(spec, testMeta()).isEmpty())
    }

    @Test fun specVersionMismatch() {
        assertIs<GateProblem.SpecVersionMismatch>(ModelGate.check(spec, testMeta(spec = "2.0.0")).single())
    }

    @Test fun featureOrderMismatchIsCaught() {
        val p = ModelGate.check(spec, testMeta(order = listOf("moisture_pct", "lab_l", "ndvi_sat")))
        assertIs<GateProblem.FeatureOrderMismatch>(p.single())
    }

    @Test fun modelNotTrainedForFallbackIsRejected() {
        val p = ModelGate.check(spec, testMeta(fallbackTrained = emptyList()))
        assertEquals(GateProblem.FallbackNotTrained("ndvi_sat"), p.single())
    }

    @Test fun realModelMustHavePassedValidationAndHaveDataHash() {
        val p = ModelGate.check(spec, testMeta(validated = false, hash = ""))
        assertTrue(GateProblem.NotValidated in p && GateProblem.MissingTrainingHash in p)
    }

    @Test fun placeholderNeedsNoValidationBecauseItIsNeverReportable() {
        assertTrue(ModelGate.check(spec, testMeta(placeholder = true, validated = false, hash = "")).isEmpty())
    }
}
