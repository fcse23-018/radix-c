package com.soilcarbon.engine.domain.vision

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RadiometricCalibratorTest {

    private val targets = doubleArrayOf(0.99, 0.80, 0.60, 0.40, 0.20, 0.05)

    private fun patch(target: Double, observed: Double): PatchObservation = PatchObservation(
        id = "P${(target * 100).toInt()}",
        name = "target $target",
        targetReflectance = target,
        observedRgb = RgbColor(observed, observed, observed),
        observedLinearRgb = RgbColor(observed, observed, observed),
        spatialStdDev = 0.01,
        pixelCount = 2_500
    )

    private fun identityObservations(): List<PatchObservation> =
        targets.map { patch(it, it) }

    @Test
    fun `identity observations recover unit gain and zero offset`() {
        val result = RadiometricCalibrator.calibrate(identityObservations())

        assertThat(result.red.gain).isWithin(1e-9).of(1.0)
        assertThat(result.red.offset).isWithin(1e-9).of(0.0)
        assertThat(result.green.gain).isWithin(1e-9).of(1.0)
        assertThat(result.blue.offset).isWithin(1e-9).of(0.0)
        assertThat(result.overallRmse).isWithin(1e-9).of(0.0)
        assertThat(result.worstRSquared).isWithin(1e-9).of(1.0)
        assertThat(result.passesQualityGate).isTrue()
        assertThat(result.qualityLabel()).isEqualTo("Excellent")
    }

    @Test
    fun `affine fit recovers a synthetic sensor transfer function`() {
        val gain = 0.8
        val offset = 0.05
        val observed = targets.map { (it - offset) / gain }

        val fitted = RadiometricCalibrator.fitAffine(observed.toList(), targets.toList())

        assertThat(fitted.gain).isWithin(1e-9).of(gain)
        assertThat(fitted.offset).isWithin(1e-9).of(offset)
        assertThat(fitted.rSquared).isWithin(1e-9).of(1.0)
        assertThat(fitted.rmse).isWithin(1e-9).of(0.0)
    }

    @Test
    fun `calibration applies the fitted transfer function to soil reflectance`() {
        val gain = 1.15
        val offset = -0.02
        val observations = targets.map { patch(it, (it - offset) / gain) }
        val result = RadiometricCalibrator.calibrate(observations)

        val observedForTarget = (0.42 - offset) / gain
        val corrected = result.apply(RgbColor(observedForTarget, observedForTarget, observedForTarget))

        assertThat(corrected.r).isWithin(1e-9).of(0.42)
        assertThat(corrected.g).isWithin(1e-9).of(0.42)
        assertThat(corrected.b).isWithin(1e-9).of(0.42)
    }

    @Test
    fun `illumination variation is zero for a clean monotonic ladder`() {
        val variation = RadiometricCalibrator.illuminationVariation(identityObservations())

        assertThat(variation).isWithin(1e-9).of(0.0)
    }

    @Test
    fun `channel application clamps physically impossible reflectance`() {
        val channel = ChannelAffine(gain = 1.2, offset = 0.1, rmse = 0.01, rSquared = 0.99)

        assertThat(channel.apply(1.0)).isWithin(1e-9).of(1.2)
        assertThat(channel.apply(-1.0)).isWithin(1e-9).of(0.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `requires at least four patches`() {
        RadiometricCalibrator.calibrate(identityObservations().take(3))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `requires a white reference`() {
        RadiometricCalibrator.calibrate(identityObservations().drop(1))
    }
}
