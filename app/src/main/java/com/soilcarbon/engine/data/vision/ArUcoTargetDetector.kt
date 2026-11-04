package com.soilcarbon.engine.data.vision

import android.graphics.Bitmap
import com.soilcarbon.engine.domain.calibration.CalibrationTargetSpec
import com.soilcarbon.engine.domain.calibration.CardPoint
import com.soilcarbon.engine.domain.calibration.CardRect
import com.soilcarbon.engine.domain.calibration.DetectedMarker
import com.soilcarbon.engine.domain.calibration.MarkerStatus
import com.soilcarbon.engine.domain.calibration.NormalizedPoint
import com.soilcarbon.engine.domain.calibration.TargetDetection
import org.opencv.android.Utils
import org.opencv.calib3d.Calib3d
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.imgproc.Imgproc
import org.opencv.objdetect.ArucoDetector
import org.opencv.objdetect.DetectorParameters
import org.opencv.objdetect.Objdetect
import org.opencv.objdetect.RefineParameters
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot

/**
 * ArUco-based calibration-target localisation. Detection runs on a downscaled analysis copy for
 * speed; all returned geometry is normalised so it can be applied to the full-resolution image.
 */
class ArUcoTargetDetector(
    private val dictionary: Int = Objdetect.DICT_4X4_50,
    private val maxAnalysisWidth: Int = 1280
) {
    private val detector: ArucoDetector by lazy {
        val parameters = DetectorParameters().apply {
            set_adaptiveThreshWinSizeMin(5)
            set_adaptiveThreshWinSizeMax(45)
            set_adaptiveThreshWinSizeStep(5)
            set_adaptiveThreshConstant(7.0)
            set_cornerRefinementMethod(Objdetect.CORNER_REFINE_SUBPIX)
            set_cornerRefinementWinSize(5)
            set_cornerRefinementMaxIterations(50)
            set_cornerRefinementMinAccuracy(0.01)
        }
        ArucoDetector(
            Objdetect.getPredefinedDictionary(dictionary),
            parameters,
            RefineParameters(10f, 3f, true)
        )
    }

    fun detect(bitmap: Bitmap, spec: CalibrationTargetSpec): TargetDetection {
        return try {
            val scale = if (bitmap.width > maxAnalysisWidth) {
                maxAnalysisWidth.toDouble() / bitmap.width.toDouble()
            } else 1.0
            val width = (bitmap.width * scale).toInt().coerceAtLeast(1)
            val height = (bitmap.height * scale).toInt().coerceAtLeast(1)
            val working = if (scale < 1.0) {
                Bitmap.createScaledBitmap(bitmap, width, height, true)
            } else {
                bitmap
            }
            val mat = Mat()
            Utils.bitmapToMat(working, mat)
            val gray = Mat()
            Imgproc.cvtColor(mat, gray, Imgproc.COLOR_RGBA2GRAY)
            mat.release()

            val corners = mutableListOf<Mat>()
            val ids = Mat()
            detector.detectMarkers(gray, corners, ids)
            gray.release()

            if (ids.empty()) {
                corners.forEach { it.release() }
                ids.release()
                return TargetDetection(
                    status = MarkerStatus.SEARCHING,
                    markers = emptyList(),
                    message = "Point the camera at the SoilCarbon calibration card.",
                    analysisWidth = width,
                    analysisHeight = height
                )
            }

            val detectedIds = mutableSetOf<Int>()
            val markers = mutableListOf<DetectedMarker>()
            val imagePoints = mutableListOf<Point>()
            val cardPoints = mutableListOf<Point>()

            for (index in 0 until ids.rows()) {
                val id = ids.get(index, 0)?.getOrNull(0)?.toInt() ?: continue
                val markerSpec = spec.marker(id) ?: continue
                val contour = corners.getOrNull(index) ?: continue
                val contourPoints = MatOfPoint2f(contour).toArray()
                val points = contourPoints.map { Point(it.x / width, it.y / height) }
                if (points.size < 4) continue
                detectedIds += id
                markers += DetectedMarker(
                    id = id,
                    corners = points.take(4).map { NormalizedPoint(it.x.toFloat(), it.y.toFloat()) },
                    center = NormalizedPoint(
                        points.take(4).map { it.x }.average().toFloat(),
                        points.take(4).map { it.y }.average().toFloat()
                    )
                )
                val half = markerSpec.sizeMm / 2.0
                val cardCorners = listOf(
                    Point(markerSpec.center.xMm - half, markerSpec.center.yMm - half),
                    Point(markerSpec.center.xMm + half, markerSpec.center.yMm - half),
                    Point(markerSpec.center.xMm + half, markerSpec.center.yMm + half),
                    Point(markerSpec.center.xMm - half, markerSpec.center.yMm + half)
                )
                points.take(4).forEachIndexed { cornerIndex, point ->
                    imagePoints += Point(point.x * width, point.y * height)
                    cardPoints += cardCorners[cornerIndex]
                }
            }
            corners.forEach { it.release() }
            ids.release()

            if (detectedIds.isEmpty()) {
                return TargetDetection(
                    status = MarkerStatus.PARTIAL,
                    markers = emptyList(),
                    detectedIds = detectedIds,
                    message = "Markers found but none belong to this calibration card version.",
                    analysisWidth = width,
                    analysisHeight = height
                )
            }

            if (detectedIds.size < 3) {
                return TargetDetection(
                    status = MarkerStatus.PARTIAL,
                    markers = markers,
                    detectedIds = detectedIds,
                    message = "Only ${detectedIds.size}/5 markers visible. Move the card fully into frame.",
                    analysisWidth = width,
                    analysisHeight = height
                )
            }

            val homography = estimateHomography(cardPoints, imagePoints)
                ?: return TargetDetection(
                    status = MarkerStatus.PARTIAL,
                    markers = markers,
                    detectedIds = detectedIds,
                    message = "Marker geometry is inconsistent; flatten the card and retry.",
                    analysisWidth = width,
                    analysisHeight = height
                )

            try {
                val apertureQuad = transformRect(spec.aperture, homography, width, height)
                val cardQuad = transformRect(
                    CardRect(0.0, 0.0, spec.cardWidthMm, spec.cardHeightMm),
                    homography,
                    width,
                    height
                )
                val patchQuads = spec.patches.associate { patch ->
                    patch.id to transformRect(patch.rect, homography, width, height)
                }
                val rotation = estimateRotation(markers, spec)
                val apertureArea = polygonArea(apertureQuad)
                val insideFrame = apertureQuad.all { it.x in 0f..1f && it.y in 0f..1f }
                val status = when {
                    !insideFrame -> MarkerStatus.PARTIAL
                    apertureArea < 0.015f -> MarkerStatus.TOO_FAR
                    apertureArea > 0.62f -> MarkerStatus.TOO_CLOSE
                    detectedIds.size < 5 -> MarkerStatus.PARTIAL
                    else -> MarkerStatus.ALIGNED
                }
                val message = when (status) {
                    MarkerStatus.ALIGNED -> "Card locked. Keep still and capture."
                    MarkerStatus.TOO_FAR -> "Move closer until the aperture fills the frame."
                    MarkerStatus.TOO_CLOSE -> "Move back; the aperture is clipped by the frame."
                    MarkerStatus.PARTIAL -> "Markers: ${detectedIds.sorted().joinToString(", ")}. " +
                        "All five markers (20-24) are required."
                    else -> "Align the card within the frame."
                }
                return TargetDetection(
                    status = status,
                    markers = markers,
                    apertureQuad = apertureQuad,
                    cardQuad = cardQuad,
                    patchQuads = patchQuads,
                    rotationDegrees = rotation,
                    estimatedDistanceMm = null,
                    detectedIds = detectedIds,
                    message = message,
                    analysisWidth = width,
                    analysisHeight = height
                )
            } finally {
                homography.release()
            }
        } catch (t: Throwable) {
            TargetDetection(
                status = MarkerStatus.ERROR,
                markers = emptyList(),
                message = "Target detection unavailable: ${t.message ?: t.javaClass.simpleName}"
            )
        }
    }

    private fun estimateHomography(cardPoints: List<Point>, imagePoints: List<Point>): Mat? {
        if (cardPoints.size < 4) return null
        val source = MatOfPoint2f(*cardPoints.toTypedArray())
        val destination = MatOfPoint2f(*imagePoints.toTypedArray())
        return try {
            val result = Calib3d.findHomography(source, destination, Calib3d.RANSAC, 4.0)
            if (result == null || result.empty() || abs(Core.determinant(result)) < 1e-9) {
                result?.release()
                null
            } else {
                result
            }
        } finally {
            source.release()
            destination.release()
        }
    }

    private fun transformRect(
        rect: CardRect,
        homography: Mat,
        width: Int,
        height: Int
    ): List<NormalizedPoint> {
        val source = MatOfPoint2f(
            Point(rect.leftMm, rect.topMm),
            Point(rect.rightMm, rect.topMm),
            Point(rect.rightMm, rect.bottomMm),
            Point(rect.leftMm, rect.bottomMm)
        )
        val destination = MatOfPoint2f()
        try {
            Core.perspectiveTransform(source, destination, homography)
            return destination.toArray().map {
                NormalizedPoint(
                    (it.x / width).toFloat().coerceIn(0f, 1f),
                    (it.y / height).toFloat().coerceIn(0f, 1f)
                )
            }
        } finally {
            source.release()
            destination.release()
        }
    }

    private fun estimateRotation(markers: List<DetectedMarker>, spec: CalibrationTargetSpec): Float {
        val topLeft = markers.firstOrNull { it.id == 20 } ?: return 0f
        val topRight = markers.firstOrNull { it.id == 21 } ?: return 0f
        val dx = topRight.center.x - topLeft.center.x
        val dy = topRight.center.y - topLeft.center.y
        return Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
    }

    private fun polygonArea(points: List<NormalizedPoint>): Float {
        if (points.size < 3) return 0f
        var sum = 0.0
        for (i in points.indices) {
            val a = points[i]
            val b = points[(i + 1) % points.size]
            sum += a.x.toDouble() * b.y - b.x.toDouble() * a.y
        }
        return (abs(sum) / 2.0).toFloat()
    }

    /** Rough physical sanity check independent of intrinsics; useful for capture guidance. */
    fun approximateCardScale(markers: List<DetectedMarker>): Double? {
        val topLeft = markers.firstOrNull { it.id == 20 } ?: return null
        val topRight = markers.firstOrNull { it.id == 21 } ?: return null
        return hypot(
            (topRight.center.x - topLeft.center.x).toDouble(),
            (topRight.center.y - topLeft.center.y).toDouble()
        )
    }

    companion object {
        /** Card coordinates of the marker square corners, in ArUco order. */
        fun markerCorners(spec: CalibrationTargetSpec, id: Int): List<CardPoint>? {
            val marker = spec.marker(id) ?: return null
            val half = marker.sizeMm / 2.0
            return listOf(
                CardPoint(marker.center.xMm - half, marker.center.yMm - half),
                CardPoint(marker.center.xMm + half, marker.center.yMm - half),
                CardPoint(marker.center.xMm + half, marker.center.yMm + half),
                CardPoint(marker.center.xMm - half, marker.center.yMm + half)
            )
        }
    }
}
