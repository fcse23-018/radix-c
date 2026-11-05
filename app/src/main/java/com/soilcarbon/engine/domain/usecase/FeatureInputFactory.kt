package com.soilcarbon.engine.domain.usecase

import com.soilcarbon.engine.domain.model.CovariateSource
import com.soilcarbon.engine.domain.model.CovariateValue
import com.soilcarbon.engine.domain.model.EnvironmentalCovariates
import com.soilcarbon.engine.domain.vision.ColorFeatures
import com.soilcarbon.engine.domain.vision.TextureFeatures
import org.soilcarbon.domain.FeatureInput
import org.soilcarbon.domain.FeatureSource

/**
 * Builds the provenance-tagged input map consumed by [org.soilcarbon.domain.AnalyzeSample].
 *
 * Pixel-derived values are MEASURED. A covariate is DEFAULT only when it came from the
 * regional fallback (which is what happens when no Sentinel token is configured); cached or
 * remotely fetched values remain MEASURED. USER_ENTERED is reserved for values the operator
 * types in; the current capture flow has none, so it is never fabricated here.
 */
object FeatureInputFactory {

    fun build(
        color: ColorFeatures,
        texture: TextureFeatures,
        covariates: EnvironmentalCovariates
    ): Map<String, FeatureInput> = linkedMapOf(
        "L_star" to FeatureInput(color.lab.l, FeatureSource.MEASURED),
        "a_star" to FeatureInput(color.lab.a, FeatureSource.MEASURED),
        "b_star" to FeatureInput(color.lab.b, FeatureSource.MEASURED),
        "munsell_value" to FeatureInput(color.munsell.value, FeatureSource.MEASURED),
        "glcm_contrast" to FeatureInput(texture.contrast, FeatureSource.MEASURED),
        "glcm_energy" to FeatureInput(texture.energy, FeatureSource.MEASURED),
        "ndvi_satellite" to covariates.ndvi.toFeatureInput(),
        "clay_content_percent" to covariates.clayPercent.toFeatureInput(),
    )

    private fun CovariateValue<Double>.toFeatureInput(): FeatureInput = FeatureInput(
        value = value,
        source = when (source) {
            CovariateSource.REGIONAL_DEFAULT -> FeatureSource.DEFAULT
            else -> FeatureSource.MEASURED
        },
    )
}
