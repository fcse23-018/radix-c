package org.soilcarbon.domain

import java.io.File
import kotlin.test.*

/** Proves the shared JSON contract is loadable by the app and internally valid. */
class ContractFilesTest {
    private val dir = File(System.getProperty("contracts.dir") ?: "../contracts")

    @Test fun sharedFeatureSpecLoads() {
        val spec = parseFeatureSpec(File(dir, "feature_spec.json").readText())
        assertEquals(8, spec.features.size)
        assertEquals(
            listOf(
                "L_star", "a_star", "b_star", "munsell_value",
                "glcm_contrast", "glcm_energy", "ndvi_satellite", "clay_content_percent",
            ),
            spec.names,
        )
        assertTrue(spec.features.single { it.name == "ndvi_satellite" }.fallback != null)
    }

    @Test fun trainingOutputMetadataIsAcceptedOrRejectedByGate() {
        val spec = parseFeatureSpec(File(dir, "feature_spec.json").readText())
        val sample = File(dir, "sample_model_metadata.json")           // produced by `train_soc.py --synthetic`
        if (!sample.exists()) return
        val meta = parseModelMetadata(sample.readText())
        assertTrue(meta.isPlaceholder)
        assertTrue(ModelGate.check(spec, meta).isEmpty(), "synthetic sidecar must match the shared spec")
    }
}
