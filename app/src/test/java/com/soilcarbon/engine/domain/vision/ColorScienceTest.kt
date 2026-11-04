package com.soilcarbon.engine.domain.vision

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.math.abs

class ColorScienceTest {

    @Test
    fun `white sRGB maps to L 100 with neutral chromaticity`() {
        val lab = ColorScience.srgbToLab(RgbColor(1.0, 1.0, 1.0))

        assertThat(lab.l).isWithin(1e-4).of(100.0)
        assertThat(lab.a).isWithin(1e-4).of(0.0)
        assertThat(lab.b).isWithin(1e-4).of(0.0)
    }

    @Test
    fun `mid grey has the expected CIE lightness`() {
        val lab = ColorScience.srgbToLab(RgbColor(0.5, 0.5, 0.5))

        assertThat(lab.l).isWithin(0.01).of(53.39)
        assertThat(ColorScience.isNeutral(lab)).isTrue()
    }

    @Test
    fun `sRGB to Lab to sRGB round-trips within display precision`() {
        val original = RgbColor(0.34, 0.51, 0.72)
        val lab = ColorScience.srgbToLab(original)
        val roundTripped = ColorScience.xyzToSrgb(ColorScience.labToXyz(lab))

        assertThat(roundTripped.r).isWithin(1e-6).of(original.r)
        assertThat(roundTripped.g).isWithin(1e-6).of(original.g)
        assertThat(roundTripped.b).isWithin(1e-6).of(original.b)
    }

    @Test
    fun `sRGB linearisation round-trips`() {
        listOf(0.0, 0.02, 0.04045, 0.5, 0.98, 1.0).forEach { value ->
            val roundTripped = ColorScience.delinearizeSrgb(ColorScience.linearizeSrgb(value))
            assertThat(roundTripped).isWithin(1e-6).of(value)
        }
    }

    @Test
    fun `pure red has expected HSV coordinates`() {
        val hsv = ColorScience.srgbToHsv(RgbColor(1.0, 0.0, 0.0))

        assertThat(hsv.hueDegrees).isWithin(1e-6).of(0.0)
        assertThat(hsv.saturation).isWithin(1e-6).of(1.0)
        assertThat(hsv.value).isWithin(1e-6).of(1.0)
    }

    @Test
    fun `Munsell value follows the CIE lightness relation`() {
        val munsell = ColorScience.labToMunsell(LabColor(50.0, 5.0, 10.0))

        assertThat(munsell.value).isWithin(0.01).of(5.69)
        assertThat(munsell.notation()).contains("/")
    }

    @Test
    fun `delta E is zero for identical colours and positive otherwise`() {
        val first = LabColor(40.0, 8.0, 15.0)
        val second = LabColor(41.0, 8.0, 15.0)

        assertThat(ColorScience.deltaE76(first, first)).isEqualTo(0.0)
        assertThat(abs(ColorScience.deltaE76(first, second))).isWithin(1e-9).of(1.0)
        assertThat(ColorScience.colourDifferenceJnd(first, second)).isWithin(1e-9).of(1.0 / 2.3)
    }

    @Test
    fun `reflectance ratios are finite at black`() {
        val ratios = ColorScience.reflectanceRatios(ReflectanceRgb(0.0, 0.0, 0.0))

        assertThat(ratios.redOverBlue).isEqualTo(0.0)
        assertThat(ratios.normalizedDifference).isEqualTo(0.0)
        assertThat(ratios.rednessIndex).isEqualTo(0.0)
    }
}
