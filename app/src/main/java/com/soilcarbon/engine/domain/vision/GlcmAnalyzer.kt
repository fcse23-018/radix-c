package com.soilcarbon.engine.domain.vision

import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.sqrt

/**
 * Gray-Level Co-occurrence Matrix features computed on the rectified soil aperture.
 * All features are normalised so that they can be used directly in the SRS feature vector
 * without device- or resolution-dependent scaling.
 */
data class TextureFeatures(
    val contrast: Double,
    val energy: Double,
    val homogeneity: Double,
    val correlation: Double,
    val entropy: Double,
    val dissimilarity: Double,
    val meanGray: Double,
    val grayLevels: Int,
    val orientations: Int,
    val offsetPixels: Int
)

object GlcmAnalyzer {

    private val OFFSETS = arrayOf(
        intArrayOf(1, 0),
        intArrayOf(1, 1),
        intArrayOf(0, 1),
        intArrayOf(-1, 1)
    )

    /**
     * @param gray luminance values in [0, 1], row-major, length = width * height
     */
    fun analyze(
        gray: FloatArray,
        width: Int,
        height: Int,
        grayLevels: Int = 32,
        offsetPixels: Int = 1,
        orientations: Int = 4
    ): TextureFeatures {
        require(width > 1 && height > 1) { "Texture ROI must be at least 2x2" }
        require(gray.size >= width * height) { "Gray buffer is smaller than ROI" }
        require(grayLevels in 4..256) { "grayLevels must be between 4 and 256" }
        require(orientations in 1..4) { "orientations must be 1..4" }

        val quantized = quantize(gray, width, height, grayLevels)
        val levels = grayLevels
        val featureSums = DoubleArray(6)
        var usedOrientations = 0

        for (orientation in 0 until orientations) {
            val dx = OFFSETS[orientation][0] * offsetPixels
            val dy = OFFSETS[orientation][1] * offsetPixels
            val counts = IntArray(levels * levels)
            var pairCount = 0

            for (y in 0 until height) {
                for (x in 0 until width) {
                    val nx = x + dx
                    val ny = y + dy
                    if (nx !in 0 until width || ny !in 0 until height) continue
                    val first = quantized[y * width + x]
                    val second = quantized[ny * width + nx]
                    counts[first * levels + second]++
                    counts[second * levels + first]++
                    pairCount += 2
                }
            }
            if (pairCount == 0) continue

            var contrast = 0.0
            var asm = 0.0
            var homogeneity = 0.0
            var entropy = 0.0
            var dissimilarity = 0.0
            var meanI = 0.0
            var meanJ = 0.0

            for (i in 0 until levels) {
                for (j in 0 until levels) {
                    val p = counts[i * levels + j].toDouble() / pairCount
                    if (p <= 0.0) continue
                    val diff = (i - j).toDouble()
                    contrast += p * diff * diff
                    asm += p * p
                    homogeneity += p / (1.0 + diff * diff)
                    entropy -= p * ln(p)
                    dissimilarity += p * abs(diff)
                    meanI += i * p
                    meanJ += j * p
                }
            }

            var varianceI = 0.0
            var varianceJ = 0.0
            var covariance = 0.0
            for (i in 0 until levels) {
                for (j in 0 until levels) {
                    val p = counts[i * levels + j].toDouble() / pairCount
                    if (p <= 0.0) continue
                    val di = i - meanI
                    val dj = j - meanJ
                    varianceI += p * di * di
                    varianceJ += p * dj * dj
                    covariance += p * di * dj
                }
            }
            val denominator = sqrt(varianceI * varianceJ)
            val correlation = if (denominator < 1e-12) 1.0 else (covariance / denominator).coerceIn(-1.0, 1.0)

            val maxDiff = (levels - 1).toDouble()
            val maxEntropy = 2.0 * ln(levels.toDouble())
            featureSums[0] += contrast / (maxDiff * maxDiff)
            featureSums[1] += sqrt(asm)
            featureSums[2] += homogeneity
            featureSums[3] += correlation
            featureSums[4] += if (maxEntropy < 1e-12) 0.0 else entropy / maxEntropy
            featureSums[5] += dissimilarity / maxDiff
            usedOrientations++
        }

        if (usedOrientations == 0) {
            return TextureFeatures(0.0, 1.0, 1.0, 1.0, 0.0, 0.0, 0.0, grayLevels, 0, offsetPixels)
        }

        var mean = 0.0
        for (i in 0 until width * height) mean += gray[i]
        mean /= (width * height).toDouble()
        val n = usedOrientations.toDouble()

        return TextureFeatures(
            contrast = (featureSums[0] / n).coerceIn(0.0, 1.0),
            energy = (featureSums[1] / n).coerceIn(0.0, 1.0),
            homogeneity = (featureSums[2] / n).coerceIn(0.0, 1.0),
            correlation = (featureSums[3] / n).coerceIn(-1.0, 1.0),
            entropy = (featureSums[4] / n).coerceIn(0.0, 1.0),
            dissimilarity = (featureSums[5] / n).coerceIn(0.0, 1.0),
            meanGray = mean.coerceIn(0.0, 1.0),
            grayLevels = grayLevels,
            orientations = usedOrientations,
            offsetPixels = offsetPixels
        )
    }

    private fun quantize(gray: FloatArray, width: Int, height: Int, levels: Int): IntArray {
        val out = IntArray(width * height)
        val scale = (levels - 1).toDouble()
        for (i in 0 until width * height) {
            out[i] = (gray[i].coerceIn(0f, 1f) * scale).toInt().coerceIn(0, levels - 1)
        }
        return out
    }
}
