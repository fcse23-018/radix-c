package com.soilcarbon.engine.domain.vision

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cbrt
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

/** Device-independent colour models used throughout the pipeline. */
data class RgbColor(val r: Double, val g: Double, val b: Double) {
    fun clamped(): RgbColor = RgbColor(r.coerceIn(0.0, 1.0), g.coerceIn(0.0, 1.0), b.coerceIn(0.0, 1.0))
    fun isFinite(): Boolean = r.isFinite() && g.isFinite() && b.isFinite()
    fun luminance(): Double = 0.2126 * r + 0.7152 * g + 0.0722 * b

    companion object {
        val BLACK = RgbColor(0.0, 0.0, 0.0)
        val WHITE = RgbColor(1.0, 1.0, 1.0)
    }
}

data class XyzColor(val x: Double, val y: Double, val z: Double)

data class LabColor(val l: Double, val a: Double, val b: Double) {
    fun chroma(): Double = sqrt(a * a + b * b)
    fun hueAngleDegrees(): Double {
        val degrees = Math.toDegrees(atan2(b, a))
        return if (degrees < 0) degrees + 360.0 else degrees
    }
}

data class HsvColor(val hueDegrees: Double, val saturation: Double, val value: Double)

/**
 * Approximate Munsell coordinates. Munsell Value is accurate to the CIE lightness relation;
 * Hue/Chroma are an approximation intended for screening, not soil-colour chart replacement.
 */
data class MunsellColor(
    val hueFamily: String,
    val hue: Double,
    val value: Double,
    val chroma: Double
) {
    fun notation(): String = "${"%.1f".format(hue)}${hueFamily} ${"%.1f".format(value)}/${"%.1f".format(chroma)}"
}

data class ReflectanceRgb(val r: Double, val g: Double, val b: Double) {
    fun clamped(): ReflectanceRgb = ReflectanceRgb(
        r.coerceIn(0.0, 1.2),
        g.coerceIn(0.0, 1.2),
        b.coerceIn(0.0, 1.2)
    )

    fun luminance(): Double = 0.2126 * r + 0.7152 * g + 0.0722 * b

    fun average(): Double = (r + g + b) / 3.0
}

data class ReflectanceRatios(
    val redOverBlue: Double,
    val normalizedDifference: Double,
    val rednessIndex: Double,
    val brightness: Double
)

data class ColorFeatures(
    val lab: LabColor,
    val hsv: HsvColor,
    val munsell: MunsellColor,
    val reflectance: ReflectanceRgb,
    val ratios: ReflectanceRatios,
    val samplePixelCount: Int,
    val excludedPixelFraction: Double
)

enum class Illuminant(
    val displayName: String,
    val xyzWhite: XyzColor,
    val kelvin: Int
) {
    D65("CIE D65", XyzColor(0.95047, 1.0, 1.08883), 6504),
    D50("CIE D50", XyzColor(0.96422, 1.0, 0.82521), 5003),
    A("CIE A (incandescent)", XyzColor(1.09850, 1.0, 0.35585), 2856),
    F2("CIE F2 (cool fluorescent)", XyzColor(0.99186, 1.0, 0.67393), 4230),
    F11("CIE F11 (fluorescent)", XyzColor(1.00962, 1.0, 0.64350), 4000)
}

/**
 * CIE colour science implementation with no Android dependencies.
 *
 * Conventions:
 *  - sRGB values are [0, 1] display encoded values.
 *  - Reflectance values are linear and may exceed 1.0 slightly at specular highlights.
 *  - Lab uses the CIE 1931 2 degree observer and the D65 white point by default.
 */
object ColorScience {

    private val D65_WHITE = Illuminant.D65.xyzWhite

    // sRGB D65 -> XYZ (CIE 15:2004)
    private val RGB_TO_XYZ = arrayOf(
        doubleArrayOf(0.4124564, 0.3575761, 0.1804375),
        doubleArrayOf(0.2126729, 0.7151522, 0.0721750),
        doubleArrayOf(0.0193339, 0.1191920, 0.9503041)
    )

    private val BRADFORD = arrayOf(
        doubleArrayOf(0.8951, 0.2664, -0.1614),
        doubleArrayOf(-0.7502, 1.7135, 0.0367),
        doubleArrayOf(0.0389, -0.0685, 1.0296)
    )

    private val BRADFORD_INV = invert3(BRADFORD)

    fun linearizeSrgb(channel: Double): Double {
        val c = channel.coerceIn(0.0, 1.0)
        return if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
    }

    fun delinearizeSrgb(channel: Double): Double {
        val c = channel.coerceIn(0.0, 1.0)
        return if (c <= 0.0031308) c * 12.92 else 1.055 * c.pow(1.0 / 2.4) - 0.055
    }

    fun srgbToXyz(rgb: RgbColor): XyzColor {
        val r = linearizeSrgb(rgb.r)
        val g = linearizeSrgb(rgb.g)
        val b = linearizeSrgb(rgb.b)
        return XyzColor(
            RGB_TO_XYZ[0][0] * r + RGB_TO_XYZ[0][1] * g + RGB_TO_XYZ[0][2] * b,
            RGB_TO_XYZ[1][0] * r + RGB_TO_XYZ[1][1] * g + RGB_TO_XYZ[1][2] * b,
            RGB_TO_XYZ[2][0] * r + RGB_TO_XYZ[2][1] * g + RGB_TO_XYZ[2][2] * b
        )
    }

    /** Treats already-linear reflectance values as XYZ-tristimulus contributions. */
    fun reflectanceToXyz(reflectance: ReflectanceRgb): XyzColor {
        val r = reflectance.r
        val g = reflectance.g
        val b = reflectance.b
        return XyzColor(
            RGB_TO_XYZ[0][0] * r + RGB_TO_XYZ[0][1] * g + RGB_TO_XYZ[0][2] * b,
            RGB_TO_XYZ[1][0] * r + RGB_TO_XYZ[1][1] * g + RGB_TO_XYZ[1][2] * b,
            RGB_TO_XYZ[2][0] * r + RGB_TO_XYZ[2][1] * g + RGB_TO_XYZ[2][2] * b
        )
    }

    fun xyzToLab(xyz: XyzColor, white: XyzColor = D65_WHITE): LabColor {
        val fx = labF(xyz.x / white.x)
        val fy = labF(xyz.y / white.y)
        val fz = labF(xyz.z / white.z)
        return LabColor(
            l = 116.0 * fy - 16.0,
            a = 500.0 * (fx - fy),
            b = 200.0 * (fy - fz)
        )
    }

    fun labToXyz(lab: LabColor, white: XyzColor = D65_WHITE): XyzColor {
        val fy = (lab.l + 16.0) / 116.0
        val fx = fy + lab.a / 500.0
        val fz = fy - lab.b / 200.0
        return XyzColor(
            white.x * labFInv(fx),
            white.y * labFInv(fy),
            white.z * labFInv(fz)
        )
    }

    fun xyzToSrgb(xyz: XyzColor): RgbColor {
        val x = xyz.x
        val y = xyz.y
        val z = xyz.z
        val r = 3.2404542 * x - 1.5371385 * y - 0.4985314 * z
        val g = -0.9692660 * x + 1.8760108 * y + 0.0415560 * z
        val b = 0.0556434 * x - 0.2040259 * y + 1.0572252 * z
        return RgbColor(delinearizeSrgb(r), delinearizeSrgb(g), delinearizeSrgb(b))
    }

    fun srgbToLab(rgb: RgbColor, white: XyzColor = D65_WHITE): LabColor =
        xyzToLab(srgbToXyz(rgb), white)

    fun reflectanceToLab(reflectance: ReflectanceRgb, white: XyzColor = D65_WHITE): LabColor =
        xyzToLab(reflectanceToXyz(reflectance), white)

    fun srgbToHsv(rgb: RgbColor): HsvColor {
        val maxC = max(rgb.r, max(rgb.g, rgb.b))
        val minC = min(rgb.r, min(rgb.g, rgb.b))
        val delta = maxC - minC
        val value = maxC
        val saturation = if (maxC <= 1e-12) 0.0 else delta / maxC
        if (delta <= 1e-12) return HsvColor(0.0, saturation, value)
        val hue = when (maxC) {
            rgb.r -> 60.0 * (((rgb.g - rgb.b) / delta) % 6.0)
            rgb.g -> 60.0 * (((rgb.b - rgb.r) / delta) + 2.0)
            else -> 60.0 * (((rgb.r - rgb.g) / delta) + 4.0)
        }
        return HsvColor(if (hue < 0) hue + 360.0 else hue, saturation, value)
    }

    fun labToHsv(lab: LabColor): HsvColor {
        val xyz = labToXyz(lab)
        return srgbToHsv(xyzToSrgb(xyz))
    }

    /**
     * Approximate Munsell notation from CIE Lab.
     * Value uses the CIE 1976 lightness relation V = (L* + 16) / 11.6. Hue/chroma are
     * interpolated from the CIE hue angle and chroma, which is adequate for QC display but
     * is not a substitute for a Munsell soil colour chart.
     */
    fun labToMunsell(lab: LabColor): MunsellColor {
        val hueAngle = lab.hueAngleDegrees()
        val families = arrayOf(
            "R", "YR", "Y", "GY", "G", "BG", "B", "PB", "P", "RP"
        )
        // Munsell hue families are 36 degrees apart; 0 deg Lab hue is roughly 10 RP.
        val shifted = (hueAngle + 20.0) % 360.0
        val familyIndex = ((shifted / 36.0).toInt()).coerceIn(0, 9)
        val withinFamily = ((shifted % 36.0) / 3.6).coerceIn(0.0, 10.0)
        val value = ((lab.l + 16.0) / 11.6).coerceIn(0.0, 10.0)
        val chroma = (lab.chroma() / 5.0).coerceIn(0.0, 20.0)
        return MunsellColor(
            hueFamily = families[familyIndex],
            hue = if (withinFamily == 0.0) 10.0 else withinFamily,
            value = value,
            chroma = chroma
        )
    }

    fun reflectanceRatios(reflectance: ReflectanceRgb): ReflectanceRatios {
        val r = reflectance.r
        val g = reflectance.g
        val b = reflectance.b
        return ReflectanceRatios(
            redOverBlue = if (b <= 1e-6) 0.0 else r / b,
            normalizedDifference = if ((r + g) <= 1e-6) 0.0 else (r - g) / (r + g),
            rednessIndex = if (g <= 1e-6) 0.0 else r / (g * g),
            brightness = reflectance.luminance()
        )
    }

    /** Bradford chromatic adaptation from the source white to the destination white. */
    fun adaptXyz(xyz: XyzColor, source: XyzColor, destination: XyzColor): XyzColor {
        val sourceCone = multiply(BRADFORD, doubleArrayOf(source.x, source.y, source.z))
        val destCone = multiply(BRADFORD, doubleArrayOf(destination.x, destination.y, destination.z))
        val scale = doubleArrayOf(
            destCone[0] / max(sourceCone[0], 1e-9),
            destCone[1] / max(sourceCone[1], 1e-9),
            destCone[2] / max(sourceCone[2], 1e-9)
        )
        val adaptedCone = doubleArrayOf(
            sourceCone[0] * scale[0],
            sourceCone[1] * scale[1],
            sourceCone[2] * scale[2]
        )
        val adapted = multiply(BRADFORD_INV, adaptedCone)
        return XyzColor(adapted[0], adapted[1], adapted[2])
    }

    fun adaptLab(lab: LabColor, source: Illuminant, destination: Illuminant): LabColor {
        val adapted = adaptXyz(labToXyz(lab, source.xyzWhite), source.xyzWhite, destination.xyzWhite)
        return xyzToLab(adapted, destination.xyzWhite)
    }

    fun deltaE76(first: LabColor, second: LabColor): Double {
        val dl = first.l - second.l
        val da = first.a - second.a
        val db = first.b - second.b
        return sqrt(dl * dl + da * da + db * db)
    }

    /** CIE76 colour difference converted to an approximate JND score. */
    fun colourDifferenceJnd(first: LabColor, second: LabColor): Double = deltaE76(first, second) / 2.3

    fun isNeutral(lab: LabColor, tolerance: Double = 2.0): Boolean =
        abs(lab.a) <= tolerance && abs(lab.b) <= tolerance

    private fun labF(t: Double): Double =
        if (t > 0.008856451679035631) cbrt(t) else (903.2962962962963 * t + 16.0) / 116.0

    private fun labFInv(t: Double): Double {
        val t3 = t * t * t
        return if (t3 > 0.008856451679035631) t3 else (116.0 * t - 16.0) / 903.2962962962963
    }

    private fun multiply(matrix: Array<DoubleArray>, vector: DoubleArray): DoubleArray =
        DoubleArray(3) { row ->
            matrix[row][0] * vector[0] + matrix[row][1] * vector[1] + matrix[row][2] * vector[2]
        }

    private fun invert3(matrix: Array<DoubleArray>): Array<DoubleArray> {
        val a = matrix[0][0]
        val b = matrix[0][1]
        val c = matrix[0][2]
        val d = matrix[1][0]
        val e = matrix[1][1]
        val f = matrix[1][2]
        val g = matrix[2][0]
        val h = matrix[2][1]
        val i = matrix[2][2]
        val det = a * (e * i - f * h) - b * (d * i - f * g) + c * (d * h - e * g)
        require(abs(det) > 1e-12) { "Singular matrix" }
        val invDet = 1.0 / det
        return arrayOf(
            doubleArrayOf((e * i - f * h) * invDet, (c * h - b * i) * invDet, (b * f - c * e) * invDet),
            doubleArrayOf((f * g - d * i) * invDet, (a * i - c * g) * invDet, (c * d - a * f) * invDet),
            doubleArrayOf((d * h - e * g) * invDet, (b * g - a * h) * invDet, (a * e - b * d) * invDet)
        )
    }
}
