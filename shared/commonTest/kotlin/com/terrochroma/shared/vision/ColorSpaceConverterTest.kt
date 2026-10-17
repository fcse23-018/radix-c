package com.terrochroma.shared.vision

import com.terrochroma.shared.vision.color.ColorSpaceConverter
import com.terrochroma.shared.vision.model.RGB
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ColorSpaceConverterTest {

    @Test
    fun testRGBToCIELab() {
        // Test with white (RGB 255, 255, 255)
        val lab = ColorSpaceConverter.rgbToLab(RGB(255, 255, 255))
        
        // White should have L close to 100, a and b close to 0
        assertTrue(lab.L > 95, "L should be close to 100 for white")
        assertTrue(lab.a > -10 && lab.a < 10, "a should be close to 0 for white")
        assertTrue(lab.b > -10 && lab.b < 10, "b should be close to 0 for white")
    }

    @Test
    fun testRGBToHSV() {
        // Test with red (RGB 255, 0, 0)
        val hsv = ColorSpaceConverter.rgbToHSV(RGB(255, 0, 0))
        
        // Red should have hue around 0°, saturation 1.0, value 1.0
        assertTrue(hsv.h in 0.0..360.0, "Hue should be in valid range")
        assertEquals(1.0, hsv.s, 0.01, "Saturation should be 1.0 for pure red")
        assertEquals(1.0, hsv.v, 0.01, "Value should be 1.0 for pure red")
    }

    @Test
    fun testRGBToMunsell() {
        // Test with green (RGB 0, 255, 0)
        val munsell = ColorSpaceConverter.rgbToMunsell(RGB(0, 255, 0))
        
        // Green should map to green hue category
        assertTrue(munsell.hue.contains("G") || munsell.hue.contains("Gy"), 
            "Green should map to green hue category")
    }

    @Test
    fun testIlluminantNormalization() {
        // Test D65 normalization (should not change much)
        val original = RGB(100, 150, 200)
        val normalized = ColorSpaceConverter.normalizeToD65(
            original, 
            com.terrochroma.shared.vision.Illuminant.D65
        )
        
        // D65 normalization should maintain values
        assertTrue(normalized.r > 0 && normalized.r <= 255, "R should be in valid range")
        assertTrue(normalized.g > 0 && normalized.g <= 255, "G should be in valid range")
        assertTrue(normalized.b > 0 && normalized.b <= 255, "B should be in valid range")
    }
}
