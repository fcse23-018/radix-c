package com.terrochroma.shared.vision

import com.terrochroma.shared.vision.texture.GLCMExtractor
import kotlin.test.Test
import kotlin.test.assertTrue

class GLCMExtractorTest {

    private val extractor = GLCMExtractor()

    @Test
    fun testFeatureExtraction() {
        // Create simple test image (3x3 RGB)
        val width = 3
        val height = 3
        val imageData = ByteArray(width * height * 3)
        
        // Fill with alternating pattern
        for (i in imageData.indices) {
            imageData[i] = if (i % 2 == 0) 255.toByte() else 0.toByte()
        }
        
        val features = extractor.extractFeatures(imageData, width, height)
        
        // Features should be extracted successfully
        assertTrue(features.contrast >= 0, "Contrast should be non-negative")
        assertTrue(features.homogeneity >= 0 && features.homogeneity <= 1, "Homogeneity should be in valid range")
        assertTrue(features.energy >= 0, "Energy should be non-negative")
        assertTrue(features.entropy >= 0, "Entropy should be non-negative")
    }

    @Test
    fun testUniformImage() {
        // Create uniform image (all same color)
        val width = 5
        val height = 5
        val imageData = ByteArray(width * height * 3) { 128.toByte() }
        
        val features = extractor.extractFeatures(imageData, width, height)
        
        // Uniform image should have high homogeneity and energy, low contrast and entropy
        assertTrue(features.homogeneity > 0.8, "Uniform image should have high homogeneity")
        assertTrue(features.contrast < 1.0, "Uniform image should have low contrast")
    }

    @Test
    fun testHighContrastImage() {
        // Create high contrast image (black and white)
        val width = 5
        val height = 5
        val imageData = ByteArray(width * height * 3)
        
        for (i in imageData.indices step 3) {
            val isWhite = ((i / 3) % 2 == 0)
            imageData[i] = if (isWhite) 255.toByte() else 0.toByte()      // R
            imageData[i + 1] = if (isWhite) 255.toByte() else 0.toByte()  // G
            imageData[i + 2] = if (isWhite) 255.toByte() else 0.toByte()  // B
        }
        
        val features = extractor.extractFeatures(imageData, width, height)
        
        // High contrast image should have high contrast
        assertTrue(features.contrast > 0, "High contrast image should have positive contrast")
    }
}
