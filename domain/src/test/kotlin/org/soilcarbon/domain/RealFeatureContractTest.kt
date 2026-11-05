package org.soilcarbon.domain

import java.io.File
import kotlin.test.*

/** Behaviour against the real eight-feature contract that ships in the app. */
class RealFeatureContractTest {
    private val spec = parseFeatureSpec(
        File(System.getProperty("contracts.dir") ?: "../contracts", "feature_spec.json").readText()
    )

    private fun metadata(placeholder: Boolean = false) = ModelMetadata(
        modelId = "soc-test",
        modelVersion = "0.1.0",
        specVersion = spec.specVersion,
        featureOrder = spec.names,
        isPlaceholder = placeholder,
        fallbackFeaturesTrained = listOf("ndvi_satellite"),
        targetMin = 0.0,
        targetMax = 20.0,
        validationPassed = true,
        dataSha256 = "abc123",
    )

    private fun inputs() = mapOf(
        "L_star" to FeatureInput(40.0, FeatureSource.MEASURED),
        "a_star" to FeatureInput(12.0, FeatureSource.MEASURED),
        "b_star" to FeatureInput(0.6, FeatureSource.MEASURED),
        "munsell_value" to FeatureInput(4.5, FeatureSource.MEASURED),
        "glcm_contrast" to FeatureInput(0.3, FeatureSource.MEASURED),
        "glcm_energy" to FeatureInput(0.4, FeatureSource.MEASURED),
        "ndvi_satellite" to FeatureInput(0.6, FeatureSource.MEASURED),
        "clay_content_percent" to FeatureInput(25.0, FeatureSource.MEASURED),
    )

    @Test fun realSpecInvalidValueNeverReachesTheModel() {
        val model = FakeModel(metadata())
        val result = AnalyzeSample(model, spec)(inputs() + ("clay_content_percent" to FeatureInput(200.0, FeatureSource.MEASURED)))
        assertIs<RejectReason.InvalidFeatures>((result as AnalysisResult.Rejected).reason)
        assertEquals(0, model.calls)
    }

    @Test fun realSpecMissingNdviUsesFallbackAndIsDegraded() {
        val result = AnalyzeSample(FakeModel(metadata()), spec)(inputs() - "ndvi_satellite") as AnalysisResult.Success
        assertTrue(result.prediction.degraded)
        assertEquals(FeatureSource.DEFAULT, result.prediction.provenance["ndvi_satellite"])
    }

    @Test fun realSpecPlaceholderIsBlockedByDefault() {
        val model = FakeModel(metadata(placeholder = true))
        val result = AnalyzeSample(model, spec)(inputs())
        assertEquals(RejectReason.PlaceholderBlocked, (result as AnalysisResult.Rejected).reason)
        assertEquals(0, model.calls)
    }
}
