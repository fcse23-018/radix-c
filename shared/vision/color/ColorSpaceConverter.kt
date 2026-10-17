package com.terrochroma.shared.vision.color

import com.terrochroma.shared.vision.model.CIELab
import com.terrochroma.shared.vision.model.HSV
import com.terrochroma.shared.vision.model.Munsell
import com.terrochroma.shared.vision.model.RGB
import kotlin.math.*

object ColorSpaceConverter {

    /**
     * Convert RGB to CIE L*a*b* color space
     * Uses D65 illuminant as reference white
     */
    fun rgbToLab(rgb: RGB): CIELab {
        // Normalize RGB to 0-1 range
        val r = rgb.r / 255.0
        val g = rgb.g / 255.0
        val b = rgb.b / 255.0

        // Convert to linear RGB (gamma correction)
        val rLinear = gammaCorrection(r)
        val gLinear = gammaCorrection(g)
        val bLinear = gammaCorrection(b)

        // Convert to XYZ (D65 illuminant)
        val xyz = rgbToXYZ(rLinear, gLinear, bLinear)

        // Convert to Lab
        return xyzToLab(xyz[0], xyz[1], xyz[2])
    }

    /**
     * Convert RGB to HSV color space
     */
    fun rgbToHSV(rgb: RGB): HSV {
        val r = rgb.r / 255.0
        val g = rgb.g / 255.0
        val b = rgb.b / 255.0

        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val delta = max - min

        val h = when {
            delta == 0.0 -> 0.0
            max == r -> 60.0 * (((g - b) / delta) % 6)
            max == g -> 60.0 * (((b - r) / delta) + 2)
            else -> 60.0 * (((r - g) / delta) + 4)
        }

        val s = if (max == 0.0) 0.0 else (delta / max)
        val v = max

        return HSV(
            h = if (h < 0) h + 360 else h,
            s = s,
            v = v
        )
    }

    /**
     * Convert RGB to Munsell color space
     * Simplified approximation - actual conversion requires lookup tables
     */
    fun rgbToMunsell(rgb: RGB): Munsell {
        val lab = rgbToLab(rgb)

        // Simplified hue calculation
        val hueAngle = atan2(lab.b, lab.a) * (180.0 / PI)
        val hue = hueToMunsellHue(hueAngle)

        // Value (lightness)
        val value = lab.L / 10.0

        // Chroma (saturation)
        val chroma = sqrt(lab.a * lab.a + lab.b * lab.b) / 10.0

        return Munsell(
            hue = hue,
            value = value.coerceIn(0.0, 10.0),
            chroma = chroma.coerceIn(0.0, 20.0)
        )
    }

    /**
     * Normalize RGB image to D65 illuminant
     * Applies white balance correction
     */
    fun normalizeToD65(rgb: RGB, currentIlluminant: com.terrochroma.shared.vision.Illuminant): RGB {
        val correction = getIlluminantCorrection(currentIlluminant)

        return RGB(
            r = (rgb.r * correction.r).toInt().coerceIn(0, 255),
            g = (rgb.g * correction.g).toInt().coerceIn(0, 255),
            b = (rgb.b * correction.b).toInt().coerceIn(0, 255)
        )
    }

    // Private helper functions

    private fun gammaCorrection(value: Double): Double {
        return if (value > 0.04045) {
            pow((value + 0.055) / 1.055, 2.4)
        } else {
            value / 12.92
        }
    }

    private fun rgbToXYZ(r: Double, g: Double, b: Double): DoubleArray {
        // sRGB to XYZ transformation matrix (D65)
        val x = r * 0.4124564 + g * 0.3575761 + b * 0.1804375
        val y = r * 0.2126729 + g * 0.7151522 + b * 0.0721750
        val z = r * 0.0193339 + g * 0.1191920 + b * 0.9503041

        return doubleArrayOf(x, y, z)
    }

    private fun xyzToLab(x: Double, y: Double, z: Double): CIELab {
        // D65 reference white
        val xn = 0.95047
        val yn = 1.0
        val zn = 1.08883

        val fx = labHelper(x / xn)
        val fy = labHelper(y / yn)
        val fz = labHelper(z / zn)

        val L = 116.0 * fy - 16.0
        val a = 500.0 * (fx - fy)
        val b = 200.0 * (fy - fz)

        return CIELab(L, a, b)
    }

    private fun labHelper(t: Double): Double {
        return if (t > 0.008856) {
            pow(t, 1.0 / 3.0)
        } else {
            7.787 * t + 16.0 / 116.0
        }
    }

    private fun hueToMunsellHue(angle: Double): String {
        val normalizedAngle = if (angle < 0) angle + 360 else angle

        return when {
            normalizedAngle in 0.0..25.0 -> "R"
            normalizedAngle in 25.0..45.0 -> "YR"
            normalizedAngle in 45.0..70.0 -> "Y"
            normalizedAngle in 70.0..95.0 -> "GY"
            normalizedAngle in 95.0..135.0 -> "G"
            normalizedAngle in 135.0..165.0 -> "BG"
            normalizedAngle in 165.0..195.0 -> "B"
            normalizedAngle in 195.0->255.0 -> "PB"
            normalizedAngle in 255.0->285.0 -> "P"
            else -> "RP"
        }
    }

    private fun getIlluminantCorrection(illuminant: com.terrochroma.shared.vision.Illuminant): RGB {
        // Simplified correction factors (actual values depend on calibration)
        return when (illuminant) {
            com.terrochroma.shared.vision.Illuminant.D65 -> RGB(255, 255, 255) // No correction needed
            com.terrochroma.shared.vision.Illuminant.D50 -> RGB(255, 248, 230)
            com.terrochroma.shared.vision.Illuminant.A -> RGB(255, 200, 150)
            com.terrochroma.shared.vision.Illuminant.F2 -> RGB(247, 255, 237)
            com.terrochroma.shared.vision.Illuminant.F11 -> RGB(255, 255, 243)
        }
    }
}
