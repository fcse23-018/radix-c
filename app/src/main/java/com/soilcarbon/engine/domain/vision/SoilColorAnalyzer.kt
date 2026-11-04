package com.soilcarbon.engine.domain.vision

import kotlin.math.sqrt

/**
 * Robust soil-colour estimator over a rectified aperture sample. Input values are display-encoded
 * sRGB in [0,1] (r,g,b interleaved). The function performs sRGB linearisation, optional
 * radiometric calibration to reflectance, then CIE Lab / Munsell conversion.
 *
 * Specular highlights and lens vignette extremes are trimmed; the returned
 * [ColorFeatures.excludedPixelFraction] is persisted for quality control.
 */
object SoilColorAnalyzer {

    fun analyze(
        srgb: DoubleArray,
        calibration: CalibrationResult? = null,
        stride: Int = 1,
        specularThreshold: Double = 0.985
    ): ColorFeatures {
        require(srgb.size >= 3) { "At least one RGB pixel is required" }
        val pixelCount = srgb.size / 3
        val step = stride.coerceAtLeast(1)

        var kept = 0
        var sumR = 0.0
        var sumG = 0.0
        var sumB = 0.0
        var index = 0
        while (index < pixelCount) {
            val offset = index * 3
            val r = ColorScience.linearizeSrgb(srgb[offset])
            val g = ColorScience.linearizeSrgb(srgb[offset + 1])
            val b = ColorScience.linearizeSrgb(srgb[offset + 2])
            val luminance = 0.2126 * r + 0.7152 * g + 0.0722 * b
            val neutralSpecular = luminance > specularThreshold &&
                kotlin.math.abs(r - g) < 0.06 && kotlin.math.abs(g - b) < 0.06
            if (luminance > 0.004 && !neutralSpecular) {
                sumR += r
                sumG += g
                sumB += b
                kept++
            }
            index += step
        }

        val sampled = ((pixelCount + step - 1) / step).coerceAtLeast(1)
        val excluded = 1.0 - kept.toDouble() / sampled.toDouble()
        val divisor = kept.coerceAtLeast(1).toDouble()
        val meanLinear = RgbColor(sumR / divisor, sumG / divisor, sumB / divisor)

        val reflectance = if (calibration != null) {
            calibration.apply(meanLinear).clamped()
        } else {
            ReflectanceRgb(meanLinear.r, meanLinear.g, meanLinear.b).clamped()
        }
        val lab = ColorScience.reflectanceToLab(reflectance)
        val displayRgb = ColorScience.xyzToSrgb(ColorScience.reflectanceToXyz(reflectance))
        val hsv = ColorScience.srgbToHsv(displayRgb)
        val munsell = ColorScience.labToMunsell(lab)
        val ratios = ColorScience.reflectanceRatios(reflectance)

        return ColorFeatures(
            lab = lab,
            hsv = hsv,
            munsell = munsell,
            reflectance = reflectance,
            ratios = ratios,
            samplePixelCount = kept,
            excludedPixelFraction = excluded.coerceIn(0.0, 1.0)
        )
    }

    /**
     * Spatial coefficient of variation of log-luminance. Used to reject patch samples with
     * shadows or glare rather than averaging them into the calibration fit.
     */
    fun luminanceVariation(srgb: DoubleArray, stride: Int = 1): Double {
        val pixelCount = srgb.size / 3
        if (pixelCount < 2) return 0.0
        val step = stride.coerceAtLeast(1)
        var count = 0
        var sum = 0.0
        var sumSquares = 0.0
        var index = 0
        while (index < pixelCount) {
            val offset = index * 3
            val r = ColorScience.linearizeSrgb(srgb[offset])
            val g = ColorScience.linearizeSrgb(srgb[offset + 1])
            val b = ColorScience.linearizeSrgb(srgb[offset + 2])
            val luminance = 0.2126 * r + 0.7152 * g + 0.0722 * b
            if (luminance > 1e-4) {
                sum += luminance
                sumSquares += luminance * luminance
                count++
            }
            index += step
        }
        if (count < 2) return 0.0
        val mean = sum / count
        if (mean <= 1e-9) return 0.0
        val variance = (sumSquares / count) - (mean * mean)
        return sqrt(variance.coerceAtLeast(0.0)) / mean
    }
}
