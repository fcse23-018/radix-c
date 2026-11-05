package com.soilcarbon.engine.data.vision

import android.graphics.Bitmap
import com.soilcarbon.engine.domain.calibration.CalibrationTargetSpec
import com.soilcarbon.engine.domain.calibration.CardRect
import com.soilcarbon.engine.domain.calibration.NormalizedPoint
import com.soilcarbon.engine.domain.calibration.TargetDetection
import com.soilcarbon.engine.domain.capture.CaptureGate
import com.soilcarbon.engine.domain.vision.CalibrationResult
import com.soilcarbon.engine.domain.vision.ColorFeatures
import com.soilcarbon.engine.domain.vision.GlcmAnalyzer
import com.soilcarbon.engine.domain.vision.PatchObservation
import com.soilcarbon.engine.domain.vision.RadiometricCalibrator
import com.soilcarbon.engine.domain.vision.RgbColor
import com.soilcarbon.engine.domain.vision.SoilColorAnalyzer
import com.soilcarbon.engine.domain.vision.TextureFeatures
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import kotlin.math.sqrt

data class ApertureAnalysis(
    val color: ColorFeatures,
    val texture: TextureFeatures,
    val cropBitmap: Bitmap,
    val patchCount: Int
)

/**
 * Rectifies the card plane and extracts the two scientific products: (1) the six-patch
 * radiometric transfer function and (2) the soil aperture colour/texture sample.
 */
class TargetImageProcessor {

    fun calibrate(
        bitmap: Bitmap,
        detection: TargetDetection,
        spec: CalibrationTargetSpec
    ): CalibrationResult? {
        if (!CaptureGate.patchGatePasses(detection.patchQuads.size)) return null
        val observations = mutableListOf<PatchObservation>()
        for (patch in spec.patches) {
            val quad = detection.patchQuads[patch.id] ?: continue
            val warped = warp(bitmap, quad, PATCH_SIZE) ?: continue
            try {
                val sample = sampleSrgb(warped, insetFraction = spec.patchSampleInsetFraction, stride = 1)
                if (sample.values.size < 3) continue
                val mean = meanRgb(sample.values)
                val linear = RgbColor(
                    com.soilcarbon.engine.domain.vision.ColorScience.linearizeSrgb(mean.r),
                    com.soilcarbon.engine.domain.vision.ColorScience.linearizeSrgb(mean.g),
                    com.soilcarbon.engine.domain.vision.ColorScience.linearizeSrgb(mean.b)
                )
                observations += PatchObservation(
                    id = patch.id,
                    name = patch.name,
                    targetReflectance = patch.targetReflectance,
                    observedRgb = mean,
                    observedLinearRgb = linear,
                    spatialStdDev = SoilColorAnalyzer.luminanceVariation(sample.values),
                    pixelCount = sample.values.size / 3
                )
            } finally {
                warped.release()
            }
        }
        if (!CaptureGate.patchGatePasses(observations.size)) return null
        return RadiometricCalibrator.calibrate(
            observations = observations,
            sourceDescription = "Card ${spec.version}; six-patch affine fit (D65)"
        )
    }

    fun analyzeAperture(
        bitmap: Bitmap,
        detection: TargetDetection,
        calibration: CalibrationResult?,
        patchCount: Int = detection.patchQuads.size
    ): ApertureAnalysis? {
        val quad = detection.apertureQuad.takeIf { it.size == 4 } ?: return null
        val warped = warp(bitmap, quad, APERTURE_SIZE) ?: return null
        try {
            val sample = sampleSrgb(warped, insetFraction = 0.04, stride = 2)
            if (sample.values.size < 30) return null
            val color = SoilColorAnalyzer.analyze(sample.values, calibration)

            val gray = Mat()
            Imgproc.cvtColor(warped, gray, Imgproc.COLOR_RGBA2GRAY)
            val grayBytes = ByteArray(APERTURE_SIZE * APERTURE_SIZE)
            gray.get(0, 0, grayBytes)
            gray.release()
            val floatGray = FloatArray(grayBytes.size) { index ->
                (grayBytes[index].toInt() and 0xff) / 255f
            }
            val texture = GlcmAnalyzer.analyze(
                gray = floatGray,
                width = APERTURE_SIZE,
                height = APERTURE_SIZE,
                grayLevels = 32,
                offsetPixels = 1,
                orientations = 4
            )
            val cropBitmap = Bitmap.createBitmap(APERTURE_SIZE, APERTURE_SIZE, Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(warped, cropBitmap)
            return ApertureAnalysis(color, texture, cropBitmap, patchCount)
        } finally {
            warped.release()
        }
    }

    fun warpAperture(bitmap: Bitmap, detection: TargetDetection): Bitmap? {
        val quad = detection.apertureQuad.takeIf { it.size == 4 } ?: return null
        val warped = warp(bitmap, quad, APERTURE_SIZE) ?: return null
        return try {
            Bitmap.createBitmap(APERTURE_SIZE, APERTURE_SIZE, Bitmap.Config.ARGB_8888).also {
                Utils.matToBitmap(warped, it)
            }
        } finally {
            warped.release()
        }
    }

    private fun warp(bitmap: Bitmap, quad: List<NormalizedPoint>, size: Int): Mat? {
        if (quad.size != 4) return null
        val sourceMat = Mat()
        Utils.bitmapToMat(bitmap, sourceMat)
        val source = MatOfPoint2f(
            Point((quad[0].x * bitmap.width).toDouble(), (quad[0].y * bitmap.height).toDouble()),
            Point((quad[1].x * bitmap.width).toDouble(), (quad[1].y * bitmap.height).toDouble()),
            Point((quad[2].x * bitmap.width).toDouble(), (quad[2].y * bitmap.height).toDouble()),
            Point((quad[3].x * bitmap.width).toDouble(), (quad[3].y * bitmap.height).toDouble())
        )
        val destination = MatOfPoint2f(
            Point(0.0, 0.0),
            Point(size.toDouble() - 1, 0.0),
            Point(size.toDouble() - 1, size.toDouble() - 1),
            Point(0.0, size.toDouble() - 1)
        )
        return try {
            val transform = Imgproc.getPerspectiveTransform(source, destination)
            val output = Mat()
            Imgproc.warpPerspective(
                sourceMat,
                output,
                transform,
                Size(size.toDouble(), size.toDouble()),
                Imgproc.INTER_LINEAR
            )
            transform.release()
            output
        } catch (t: Throwable) {
            null
        } finally {
            sourceMat.release()
            source.release()
            destination.release()
        }
    }

    private fun sampleSrgb(
        mat: Mat,
        insetFraction: Double,
        stride: Int
    ): RgbSample {
        val width = mat.cols()
        val height = mat.rows()
        val insetX = (width * insetFraction).toInt().coerceIn(0, width / 2 - 1)
        val insetY = (height * insetFraction).toInt().coerceIn(0, height / 2 - 1)
        val startX = insetX
        val endX = width - insetX
        val startY = insetY
        val endY = height - insetY
        val step = stride.coerceAtLeast(1)
        val values = ArrayList<Double>(((endX - startX) / step + 1) * ((endY - startY) / step + 1) * 3)
        val row = ByteArray(width * 4)
        var y = startY
        while (y < endY) {
            mat.get(y, 0, row)
            var x = startX
            while (x < endX) {
                val offset = x * 4
                values += (row[offset].toInt() and 0xff) / 255.0
                values += (row[offset + 1].toInt() and 0xff) / 255.0
                values += (row[offset + 2].toInt() and 0xff) / 255.0
                x += step
            }
            y += step
        }
        return RgbSample(values.toDoubleArray())
    }

    private fun meanRgb(values: DoubleArray): RgbColor {
        if (values.isEmpty()) return RgbColor.BLACK
        var r = 0.0
        var g = 0.0
        var b = 0.0
        var count = 0
        var i = 0
        while (i + 2 < values.size) {
            r += values[i]
            g += values[i + 1]
            b += values[i + 2]
            count++
            i += 3
        }
        val divisor = count.coerceAtLeast(1).toDouble()
        return RgbColor(r / divisor, g / divisor, b / divisor)
    }

    private data class RgbSample(val values: DoubleArray) {
        fun coefficientOfVariation(): Double {
            if (values.isEmpty()) return 0.0
            val mean = values.average()
            if (mean <= 1e-9) return 0.0
            val variance = values.sumOf { (it - mean) * (it - mean) } / values.size
            return sqrt(variance) / mean
        }
    }

    companion object {
        const val APERTURE_SIZE = 448
        const val PATCH_SIZE = 96
    }
}
