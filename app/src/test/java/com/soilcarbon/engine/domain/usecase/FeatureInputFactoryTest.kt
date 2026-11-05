package com.soilcarbon.engine.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.soilcarbon.engine.domain.model.CovariateSource
import com.soilcarbon.engine.domain.model.CovariateValue
import com.soilcarbon.engine.domain.model.EnvironmentalCovariates
import com.soilcarbon.engine.domain.vision.ColorFeatures
import com.soilcarbon.engine.domain.vision.HsvColor
import com.soilcarbon.engine.domain.vision.LabColor
import com.soilcarbon.engine.domain.vision.MunsellColor
import com.soilcarbon.engine.domain.vision.ReflectanceRatios
import com.soilcarbon.engine.domain.vision.ReflectanceRgb
import com.soilcarbon.engine.domain.vision.TextureFeatures
import org.junit.Test
import org.soilcarbon.domain.FeatureSource

class FeatureInputFactoryTest {

    private fun color() = ColorFeatures(
        lab = LabColor(38.5, 9.25, 18.75),
        hsv = HsvColor(28.0, 0.42, 0.33),
        munsell = MunsellColor(hueFamily = "YR", hue = 6.0, value = 4.6, chroma = 4.2),
        reflectance = ReflectanceRgb(0.16, 0.12, 0.09),
        ratios = ReflectanceRatios(1.77, 0.14, 11.1, 0.13),
        samplePixelCount = 4_800,
        excludedPixelFraction = 0.03
    )

    private fun texture() = TextureFeatures(
        contrast = 0.31,
        energy = 0.42,
        homogeneity = 0.63,
        correlation = 0.21,
        entropy = 0.58,
        dissimilarity = 0.24,
        meanGray = 0.33,
        grayLevels = 32,
        orientations = 4,
        offsetPixels = 1
    )

    private fun covariates(ndviSource: CovariateSource) = EnvironmentalCovariates.defaults().copy(
        ndvi = CovariateValue(0.35, ndviSource),
        clayPercent = CovariateValue(25.0, CovariateSource.SOILGRIDS_WCS),
    )

    @Test
    fun `regional default NDVI is tagged DEFAULT and degrades the result`() {
        val inputs = FeatureInputFactory.build(
            color(),
            texture(),
            covariates(CovariateSource.REGIONAL_DEFAULT),
        )

        assertThat(inputs.keys).containsExactly(
            "L_star", "a_star", "b_star", "munsell_value",
            "glcm_contrast", "glcm_energy", "ndvi_satellite", "clay_content_percent",
        ).inOrder()
        assertThat(inputs.getValue("ndvi_satellite").source).isEqualTo(FeatureSource.DEFAULT)
        assertThat(inputs.getValue("clay_content_percent").source).isEqualTo(FeatureSource.MEASURED)
        assertThat(inputs.getValue("L_star").source).isEqualTo(FeatureSource.MEASURED)
    }

    @Test
    fun `live or cached NDVI is tagged MEASURED`() {
        val inputs = FeatureInputFactory.build(
            color(),
            texture(),
            covariates(CovariateSource.SENTINEL2),
        )

        assertThat(inputs.getValue("ndvi_satellite").source).isEqualTo(FeatureSource.MEASURED)
    }
}
