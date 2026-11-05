package org.soilcarbon.domain

import kotlin.test.*

class FeatureValidatorTest {
    private val spec = testSpec()

    @Test fun validInputsKeepSpecOrderAndProvenance() {
        val r = FeatureValidator.validate(spec, goodInputs().entries.reversed().associate { it.key to it.value })
        r as ValidationResult.Valid
        assertEquals(listOf(40f, 12f, 0.6f), r.values)
        assertEquals(FeatureSource.USER_ENTERED, r.provenance["moisture_pct"])
        assertTrue(r.defaulted.isEmpty())
    }

    @Test fun missingOptionalUsesFallbackAndIsFlaggedDefault() {
        val r = FeatureValidator.validate(spec, goodInputs() - "ndvi_sat") as ValidationResult.Valid
        assertEquals(0.35f, r.values[2])
        assertEquals(listOf("ndvi_sat"), r.defaulted)
    }

    @Test fun nullValueTreatedAsMissing() {
        val r = FeatureValidator.validate(spec, goodInputs() + ("ndvi_sat" to FeatureInput(null, FeatureSource.MEASURED)))
        assertEquals(listOf("ndvi_sat"), (r as ValidationResult.Valid).defaulted)
    }

    @Test fun missingRequiredIsRejected() {
        val r = FeatureValidator.validate(spec, goodInputs() - "lab_l") as ValidationResult.Invalid
        assertEquals(listOf<FeatureIssue>(FeatureIssue.Missing("lab_l")), r.issues)
    }

    @Test fun outOfRangeIsRejectedNotClamped() {
        val r = FeatureValidator.validate(spec, goodInputs() + ("moisture_pct" to FeatureInput(75.0, FeatureSource.MEASURED)))
        assertIs<FeatureIssue.OutOfRange>((r as ValidationResult.Invalid).issues.single())
    }

    @Test fun nanAndInfinityRejected() {
        for (bad in listOf(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)) {
            val r = FeatureValidator.validate(spec, goodInputs() + ("lab_l" to FeatureInput(bad, FeatureSource.MEASURED)))
            assertIs<FeatureIssue.NotFinite>((r as ValidationResult.Invalid).issues.single())
        }
    }

    @Test fun unknownFeatureRejected() {
        val r = FeatureValidator.validate(spec, goodInputs() + ("mystery" to FeatureInput(1.0, FeatureSource.MEASURED)))
        assertTrue((r as ValidationResult.Invalid).issues.any { it is FeatureIssue.Unknown })
    }

    @Test fun boundariesAreInclusive() {
        val r = FeatureValidator.validate(spec, goodInputs() + ("lab_l" to FeatureInput(0.0, FeatureSource.MEASURED)) + ("moisture_pct" to FeatureInput(60.0, FeatureSource.MEASURED)))
        assertIs<ValidationResult.Valid>(r)
    }

    @Test fun sweepOfRandomVectorsNeverLetsOutOfRangeThrough() {
        val rnd = java.util.Random(42)
        repeat(2000) {
            val inputs = spec.features.associate { it.name to FeatureInput((rnd.nextDouble() - 0.5) * 400, FeatureSource.MEASURED) }
            val r = FeatureValidator.validate(spec, inputs)
            if (r is ValidationResult.Valid) spec.features.forEachIndexed { i, d ->
                assertTrue(r.values[i] >= d.min.toFloat() && r.values[i] <= d.max.toFloat())
            }
        }
    }

    @Test fun specRejectsBadDefinitions() {
        assertFailsWith<IllegalArgumentException> { FeatureDef("x", "", 5.0, 1.0, true, null) }
        assertFailsWith<IllegalArgumentException> { FeatureDef("x", "", 0.0, 1.0, false, null) }
        assertFailsWith<IllegalArgumentException> { FeatureDef("x", "", 0.0, 1.0, false, 9.0) }
    }
}
