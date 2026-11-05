package com.soilcarbon.engine.data.ml

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.soilcarbon.domain.parseFeatureSpec
import org.soilcarbon.domain.parseModelMetadata
import java.io.File

/**
 * Guards the two JSON files that ship in the APK. They must parse with the same domain code the
 * app uses, agree on feature order, and be flagged as a placeholder.
 */
class ShippedAssetsTest {

    private val assets = File("src/main/assets")

    @Test
    fun `shipped feature spec matches the app vector and marks NDVI optional`() {
        val spec = parseFeatureSpec(File(assets, "contracts/feature_spec.json").readText())

        assertThat(spec.names).containsExactly(
            "L_star", "a_star", "b_star", "munsell_value",
            "glcm_contrast", "glcm_energy", "ndvi_satellite", "clay_content_percent",
        ).inOrder()
        assertThat(spec.features.single { it.name == "ndvi_satellite" }.required).isFalse()
        assertThat(spec.features.single { it.name == "ndvi_satellite" }.fallback).isEqualTo(0.35)
    }

    @Test
    fun `shipped sidecar parses and remains an explicitly blocked placeholder`() {
        val spec = parseFeatureSpec(File(assets, "contracts/feature_spec.json").readText())
        val meta = parseModelMetadata(File(assets, "models/soc_model_metadata.json").readText())

        assertThat(meta.featureOrder).isEqualTo(spec.names)
        assertThat(meta.specVersion).isEqualTo(spec.specVersion)
        assertThat(meta.isPlaceholder).isTrue()
        assertThat(meta.validationPassed).isFalse()
        assertThat(meta.fallbackFeaturesTrained).contains("ndvi_satellite")
        assertThat(meta.featureMeans).hasSize(8)
        assertThat(meta.featureScales).hasSize(8)
    }
}
