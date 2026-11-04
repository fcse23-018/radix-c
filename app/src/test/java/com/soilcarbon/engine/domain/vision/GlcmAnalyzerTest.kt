package com.soilcarbon.engine.domain.vision

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class GlcmAnalyzerTest {

    @Test
    fun `uniform patch is perfectly ordered`() {
        val width = 24
        val height = 24
        val gray = FloatArray(width * height) { 0.5f }

        val texture = GlcmAnalyzer.analyze(gray, width, height)

        assertThat(texture.contrast).isWithin(1e-9).of(0.0)
        assertThat(texture.energy).isWithin(1e-9).of(1.0)
        assertThat(texture.homogeneity).isWithin(1e-9).of(1.0)
        assertThat(texture.correlation).isWithin(1e-9).of(1.0)
        assertThat(texture.entropy).isWithin(1e-9).of(0.0)
        assertThat(texture.meanGray).isWithin(1e-6).of(0.5)
        assertThat(texture.orientations).isEqualTo(4)
    }

    @Test
    fun `checkerboard produces more contrast and less energy than uniform soil`() {
        val width = 32
        val height = 32
        val checker = FloatArray(width * height) { index ->
            val x = index % width
            val y = index / width
            if ((x + y) % 2 == 0) 0.02f else 0.98f
        }

        val texture = GlcmAnalyzer.analyze(checker, width, height)

        assertThat(texture.contrast).isGreaterThan(0.4)
        assertThat(texture.energy).isLessThan(0.95)
        assertThat(texture.homogeneity).isLessThan(0.6)
        assertThat(texture.correlation).isAtLeast(-1.0)
        assertThat(texture.correlation).isAtMost(1.0)
        assertThat(texture.entropy).isGreaterThan(0.01)
    }

    @Test
    fun `analysis is deterministic`() {
        val width = 19
        val height = 17
        val gray = FloatArray(width * height) { ((it * 37) % 100) / 100f }

        val first = GlcmAnalyzer.analyze(gray, width, height, grayLevels = 16, offsetPixels = 2)
        val second = GlcmAnalyzer.analyze(gray, width, height, grayLevels = 16, offsetPixels = 2)

        assertThat(first).isEqualTo(second)
        assertThat(first.grayLevels).isEqualTo(16)
        assertThat(first.offsetPixels).isEqualTo(2)
    }

    @Test
    fun `features remain normalised for random texture`() {
        val width = 40
        val height = 40
        val gray = FloatArray(width * height) { ((it * 7919) % 1000) / 1000f }

        val texture = GlcmAnalyzer.analyze(gray, width, height)

        assertThat(texture.contrast).isAtLeast(0.0)
        assertThat(texture.contrast).isAtMost(1.0)
        assertThat(texture.energy).isAtLeast(0.0)
        assertThat(texture.energy).isAtMost(1.0)
        assertThat(texture.homogeneity).isAtLeast(0.0)
        assertThat(texture.homogeneity).isAtMost(1.0)
        assertThat(texture.correlation).isAtLeast(-1.0)
        assertThat(texture.correlation).isAtMost(1.0)
        assertThat(texture.entropy).isAtLeast(0.0)
        assertThat(texture.entropy).isAtMost(1.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects degenerate ROI`() {
        GlcmAnalyzer.analyze(FloatArray(1), 1, 1)
    }
}
