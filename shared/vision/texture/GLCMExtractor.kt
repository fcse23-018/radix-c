package com.terrochroma.shared.vision.texture

import com.terrochroma.shared.vision.model.TextureFeatures
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Gray-Level Co-occurrence Matrix (GLCM) Texture Feature Extractor
 * Computes texture features: contrast, homogeneity, energy, entropy
 */
class GLCMExtractor(
    private val distance: Int = 1, // Pixel distance for co-occurrence
    private val angle: GLCMAngle = GLCMAngle.DEG_0,
    private val grayLevels: Int = 256 // Number of gray levels
) {

    /**
     * Extract GLCM texture features from grayscale image data
     */
    fun extractFeatures(
        imageData: ByteArray,
        width: Int,
        height: Int
    ): TextureFeatures {
        // Convert to grayscale if needed
        val grayData = convertToGrayscale(imageData)

        // Build GLCM
        val glcm = buildGLCM(grayData, width, height)

        // Normalize GLCM
        val normalizedGLCM = normalizeGLCM(glcm)

        // Compute texture features
        val contrast = computeContrast(normalizedGLCM)
        val homogeneity = computeHomogeneity(normalizedGLCM)
        val energy = computeEnergy(normalizedGLCM)
        val entropy = computeEntropy(normalizedGLCM)

        return TextureFeatures(
            contrast = contrast,
            homogeneity = homogeneity,
            energy = energy,
            entropy = entropy
        )
    }

    /**
     * Build Gray-Level Co-occurrence Matrix
     */
    private fun buildGLCM(grayData: IntArray, width: Int, height: Int): Array<DoubleArray> {
        val glcm = Array(grayLevels) { DoubleArray(grayLevels) }

        for (y in 0 until height) {
            for (x in 0 until width) {
                val currentLevel = grayData[y * width + x]

                // Get co-occurring pixel based on angle and distance
                val (x2, y2) = getCoOccurrencePixel(x, y, width, height)

                if (x2 >= 0 && x2 < width && y2 >= 0 && y2 < height) {
                    val coOccurringLevel = grayData[y2 * width + x2]
                    glcm[currentLevel][coOccurringLevel]++
                }
            }
        }

        return glcm
    }

    /**
     * Normalize GLCM to probabilities
     */
    private fun normalizeGLCM(glcm: Array<DoubleArray>): Array<DoubleArray> {
        val sum = glcm.sumOf { row -> row.sum() }
        return if (sum > 0) {
            glcm.map { row -> row.map { it / sum }.toDoubleArray() }.toTypedArray()
        } else {
            glcm
        }
    }

    /**
     * Compute Contrast: Measures local variations in the GLCM
     * High contrast = high texture variation
     */
    private fun computeContrast(glcm: Array<DoubleArray>): Double {
        var contrast = 0.0
        for (i in glcm.indices) {
            for (j in glcm.indices) {
                contrast += (i - j).toDouble().pow(2) * glcm[i][j]
            }
        }
        return contrast
    }

    /**
     * Compute Homogeneity: Measures closeness of the distribution
     * High homogeneity = smooth texture
     */
    private fun computeHomogeneity(glcm: Array<DoubleArray>): Double {
        var homogeneity = 0.0
        for (i in glcm.indices) {
            for (j in glcm.indices) {
                homogeneity += glcm[i][j] / (1.0 + (i - j).toDouble().pow(2))
            }
        }
        return homogeneity
    }

    /**
     * Compute Energy (Angular Second Moment): Measures textural uniformity
     * High energy = uniform texture
     */
    private fun computeEnergy(glcm: Array<DoubleArray>): Double {
        var energy = 0.0
        for (i in glcm.indices) {
            for (j in glcm.indices) {
                energy += glcm[i][j].pow(2)
            }
        }
        return sqrt(energy)
    }

    /**
     * Compute Entropy: Measures randomness of the GLCM
     * High entropy = high randomness/complexity
     */
    private fun computeEntropy(glcm: Array<DoubleArray>): Double {
        var entropy = 0.0
        for (i in glcm.indices) {
            for (j in glcm.indices) {
                val p = glcm[i][j]
                if (p > 0) {
                    entropy -= p * ln(p)
                }
            }
        }
        return entropy
    }

    /**
     * Convert RGB image data to grayscale
     * Using luminance formula: Y = 0.299R + 0.587G + 0.114B
     */
    private fun convertToGrayscale(imageData: ByteArray): IntArray {
        val grayData = IntArray(imageData.size / 3)

        for (i in grayData.indices) {
            val r = imageData[i * 3].toInt() and 0xFF
            val g = imageData[i * 3 + 1].toInt() and 0xFF
            val b = imageData[i * 3 + 2].toInt() and 0xFF

            grayData[i] = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
        }

        return grayData
    }

    /**
     * Get co-occurring pixel coordinates based on angle and distance
     */
    private fun getCoOccurrencePixel(x: Int, y: Int, width: Int, height: Int): Pair<Int, Int> {
        return when (angle) {
            GLCMAngle.DEG_0 -> Pair(x + distance, y)
            GLCMAngle.DEG_45 -> Pair(x + distance, y + distance)
            GLCMAngle.DEG_90 -> Pair(x, y + distance)
            GLCMAngle.DEG_135 -> Pair(x - distance, y + distance)
        }
    }
}

/**
 * GLCM calculation angles
 */
enum class GLCMAngle {
    DEG_0,   // Horizontal
    DEG_45,  // Diagonal 45°
    DEG_90,  // Vertical
    DEG_135  // Diagonal 135°
}
