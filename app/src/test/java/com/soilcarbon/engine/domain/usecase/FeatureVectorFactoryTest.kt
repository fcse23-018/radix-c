package com.soilcarbon.engine.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.soilcarbon.engine.domain.model.EnvironmentalCovariates
import com.soilcarbon.engine.domain.model.FeatureVector
import com.soilcarbon.engine.domain.vision.ColorFeatures
import com.soilcarbon.engine.domain.vision.HsvColor
import com.soilcarbon.engine.domain.vision.LabColor
import com.soilcarbon.engine.domain.vision.MunsellColor
import com.soilcarbon.engine.domain.vision.ReflectanceRatios
import com.soilcarbon.engine.domain.vision.ReflectanceRgb
import com.soilcarbon.engine.domain.vision.TextureFeatures
import org.junit.Test

class FeatureVectorFactoryTest {

    private fun color(lab: LabColor = LabColor(38.5, 9.25, 18.75)): ColorFeatures = ColorFeatures(
        lab = lab,
        hsv = HsvColor(28.0, 0.42, 0.33),
        munsell = MunsellColor(hueFamily = "YR", hue = 6.0, value = 4.6, chroma = 4.2),
        reflectance = ReflectanceRgb(0.16, 0.12, 0.09),
        ratios = ReflectanceRatios(1.77, 0.14, 11.1, 0.13),
        samplePixelCount = 4_800,
        excludedPixelFraction = 0.03
    )

    private fun texture(contrast: Double = 0.31, energy: Double = 0.42): TextureFeatures =
        TextureFeatures(
            contrast = contrast,
            energy = energy,
            homogeneity = 0.63,
            correlation = 0.21,
            entropy = 0.58,
            dissimilarity = 0.24,
            meanGray = 0.33,
            grayLevels = 32,
            orientations = 4,
            offsetPixels = 1
        )

    @Test
    fun `builds the exact SRS feature order`() {
        val covariates = EnvironmentalCovariates.defaults().copy(
            ndvi = EnvironmentalCovariates.defaults().ndvi.copy(value = 0.47),
            clayPercent = EnvironmentalCovariates.defaults().clayPercent.copy(value = 31.5)
        )

        val vector = FeatureVectorFactory.build(color(), texture(), covariates)
        val values = vector.toFloatArray()

        assertThat(values).hasLength(8)
        assertThat(values[0]).isWithin(1e-5f).of(38.5f)
        assertThat(values[1]).isWithin(1e-5f).of(9.25f)
        assertThat(values[2]).isWithin(1e-5f).of(18.75f)
        assertThat(values[3]).isWithin(1e-5f).of(4.6f)
        assertThat(values[4]).isWithin(1e-5f).of(0.31f)
        assertThat(values[5]).isWithin(1e-5f).of(0.42f)
        assertThat(values[6]).isWithin(1e-5f).of(0.47f)
        assertThat(values[7]).isWithin(1e-5f).of(31.5f)
        assertThat(FeatureVector.FEATURE_NAMES).hasSize(8)
    }

    @Test
    fun `clamps every feature to its physical domain`() {
        val vector = FeatureVectorFactory.build(
            color = color(LabColor(140.0, -190.0, 220.0)).copy(
                munsell = MunsellColor("YR", 10.0, 14.0, 40.0)
            ),
            texture = texture(contrast = 1.9, energy = -0.3),
            covariates = EnvironmentalCovariates.defaults().copy(
                ndvi = EnvironmentalCovariates.defaults().ndvi.copy(value = 3.0),
                clayPercent = EnvironmentalCovariates.defaults().clayPercent.copy(value = 180.0)
            )
        )

        assertThat(vector.lStar).isEqualTo(100.0)
        assertThat(vector.aStar).isEqualTo(-128.0)
        assertThat(vector.bStar).isEqualTo(127.0)
        assertThat(vector.munsellValue).isEqualTo(10.0)
        assertThat(vector.glcmContrast).isEqualTo(1.0)
        assertThat(vector.glcmEnergy).isEqualTo(0.0)
        assertThat(vector.ndviSatellite).isEqualTo(1.0)
        assertThat(vector.clayContentPercent).isEqualTo(100.0)
    }

    @Test
    fun `canonical string reflects the mandated order`() {
        val vector = FeatureVector(38.5, 9.25, 18.75, 4.6, 0.31, 0.42, 0.47, 31.5)

        assertThat(vector.toCanonicalString())
            .isEqualTo("[38.500000, 9.250000, 18.750000, 4.600000, 0.310000, 0.420000, 0.470000, 31.500000]")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `feature vector rejects out-of-domain components`() {
        FeatureVector(101.0, 9.25, 18.75, 4.6, 0.31, 0.42, 0.47, 31.5)
    }
}
