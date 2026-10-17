package com.terrochroma.shared.vision

import android.content.Context
import com.terrochroma.shared.vision.color.ColorSpaceConverter
import com.terrochroma.shared.vision.model.ArucoDetectionResult
import com.terrochroma.shared.vision.model.CIELab
import com.terrochroma.shared.vision.model.ColorFeatures
import com.terrochroma.shared.vision.model.HSV
import com.terrochroma.shared.vision.model.Munsell
import com.terrochroma.shared.vision.model.RGB
import com.terrochroma.shared.vision.model.TextureFeatures
import com.terrochroma.shared.vision.texture.GLCMExtractor
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.aruco.Aruco
import org.opencv.aruco.Dictionary
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Rect
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc
import java.io.ByteArrayOutputStream

class VisionServiceImpl(private val context: Context) : VisionService {

    private var isOpenCVLoaded = false
    private val arucoDictionary: Dictionary by lazy {
        Aruco.getPredefinedDictionary(Aruco.DICT_4X4_50)
    }
    private val colorConverter = ColorSpaceConverter
    private val glcmExtractor = GLCMExtractor()

    private val loaderCallback = object : BaseLoaderCallback(context) {
        override fun onManagerConnected(status: Int) {
            when (status) {
                LoaderCallbackInterface.SUCCESS -> {
                    isOpenCVLoaded = true
                }
                else -> {
                    super.onManagerConnected(status)
                }
            }
        }
    }

    override suspend fun initialize(): Result<Unit> {
        return try {
            if (!OpenCVLoader.initDebug()) {
                OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, context, loaderCallback)
            } else {
                isOpenCVLoaded = true
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun detectAruco(imageData: ByteArray): Result<ArucoDetectionResult> {
        return try {
            if (!isOpenCVLoaded) {
                return Result.failure(Exception("OpenCV not loaded"))
            }

            // Convert byte array to Mat
            val mat = byteToMat(imageData)

            // Convert to grayscale for ArUco detection
            val grayMat = Mat()
            Imgproc.cvtColor(mat, grayMat, Imgproc.COLOR_RGB2GRAY)

            // Detect markers
            val markerCorners = mutableListOf<MatOfPoint>()
            val markerIds = Mat()
            val rejectedCandidates = MatOfPoint()

            Aruco.detectMarkers(
                grayMat,
                arucoDictionary,
                markerCorners,
                markerIds,
                Aruco.DetectorParameters.create(),
                rejectedCandidates
            )

            if (markerIds.empty()) {
                mat.release()
                grayMat.release()
                markerIds.release()
                rejectedCandidates.release()

                return Result.success(
                    ArucoDetectionResult(
                        isDetected = false,
                        markerId = null,
                        corners = null,
                        center = null,
                        confidence = 0.0,
                        rotation = null,
                        isAligned = false
                    )
                )
            }

            // Get first detected marker
            val id = markerIds.get(0, 0)[0].toInt()
            val corners = markerCorners[0]
            val cornersList = corners.toList()

            // Calculate center
            val centerX = cornersList.map { it.x }.average()
            val centerY = cornersList.map { it.y }.average()

            // Calculate rotation (angle from top-left to top-right)
            val rotation = calculateRotation(cornersList)

            // Check alignment (rotation should be close to 0 or 180)
            val isAligned = isMarkerAligned(rotation)

            mat.release()
            grayMat.release()
            markerIds.release()
            rejectedCandidates.release()

            Result.success(
                ArucoDetectionResult(
                    isDetected = true,
                    markerId = id,
                    corners = cornersList.map { com.terrochroma.shared.vision.model.Point2D(it.x, it.y) },
                    center = com.terrochroma.shared.vision.model.Point2D(centerX, centerY),
                    confidence = 0.95, // Placeholder - actual confidence from detection
                    rotation = rotation,
                    isAligned = isAligned
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun extractColorFeatures(imageData: ByteArray): Result<ColorFeatures> {
        return try {
            // Convert byte array to Mat
            val mat = byteToMat(imageData)

            // Sample center region for color extraction
            val width = mat.cols()
            val height = mat.rows()
            val sampleSize = 50

            val x = (width - sampleSize) / 2
            val y = (height - sampleSize) / 2

            val roi = Rect(x, y, sampleSize, sampleSize)
            val sampleMat = Mat(mat, roi)

            // Calculate average color
            val meanColor = org.opencv.core.Core.mean(sampleMat)
            val avgRGB = RGB(
                r = meanColor.`val`[2].toInt().coerceIn(0, 255),
                g = meanColor.`val`[1].toInt().coerceIn(0, 255),
                b = meanColor.`val`[0].toInt().coerceIn(0, 255)
            )

            // Normalize to D65
            val normalizedRGB = colorConverter.normalizeToD65(avgRGB, Illuminant.D65)

            // Convert to color spaces
            val cieLab = colorConverter.rgbToLab(normalizedRGB)
            val hsv = colorConverter.rgbToHSV(normalizedRGB)
            val munsell = colorConverter.rgbToMunsell(normalizedRGB)

            mat.release()
            sampleMat.release()

            Result.success(
                ColorFeatures(
                    cieLab = cieLab,
                    hsv = hsv,
                    munsell = munsell,
                    rgb = normalizedRGB
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun extractTextureFeatures(imageData: ByteArray): Result<TextureFeatures> {
        return try {
            // Get image dimensions from metadata
            val width = 100 // Placeholder - should come from actual image
            val height = 100

            // Extract GLCM features
            val features = glcmExtractor.extractFeatures(imageData, width, height)

            Result.success(features)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun normalizeIlluminant(imageData: ByteArray, illuminant: Illuminant): Result<ByteArray> {
        return try {
            val mat = byteToMat(imageData)

            // Convert to RGB
            val rgbMat = Mat()
            Imgproc.cvtColor(mat, rgbMat, Imgproc.COLOR_BGR2RGB)

            // Apply white balance correction
            // Simple gray world assumption
            val meanColor = org.opencv.core.Core.mean(rgbMat)
            val avg = (meanColor.`val`[0] + meanColor.`val`[1] + meanColor.`val`[2]) / 3.0

            val alpha = avg / meanColor.`val`[0]
            val beta = avg / meanColor.`val`[1]
            val gamma = avg / meanColor.`val`[2]

            val correctedMat = Mat()
            rgbMat.convertTo(correctedMat, -1, 1.0, 0.0)

            // Apply correction
            val channels = mutableListOf<Mat>()
            org.opencv.core.Core.split(correctedMat, channels)

            channels[0].convertTo(channels[0], -1, alpha, 0.0)
            channels[1].convertTo(channels[1], -1, beta, 0.0)
            channels[2].convertTo(channels[2], -1, gamma, 0.0)

            org.opencv.core.Core.merge(channels, correctedMat)

            val result = matToByte(correctedMat)

            mat.release()
            rgbMat.release()
            correctedMat.release()
            channels.forEach { it.release() }

            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun release(): Result<Unit> {
        return Result.success(Unit)
    }

    // Helper functions

    private fun byteToMat(imageData: ByteArray): Mat {
        val mat = Mat(1, imageData.size, org.opencv.core.CvType.CV_8UC1)
        mat.put(0, 0, imageData)
        return mat
    }

    private fun matToByte(mat: Mat): ByteArray {
        val byteArray = ByteArray(mat.cols() * mat.rows() * mat.channels())
        mat.get(0, 0, byteArray)
        return byteArray
    }

    private fun calculateRotation(corners: List<Point>): Double {
        val topLeft = corners[0]
        val topRight = corners[1]

        val dx = topRight.x - topLeft.x
        val dy = topRight.y - topLeft.y

        val angleRad = kotlin.math.atan2(dy, dx)
        var angleDeg = kotlin.math.toDegrees(angleRad)

        // Normalize to 0-180 range
        if (angleDeg < 0) angleDeg += 180.0

        return angleDeg
    }

    private fun isMarkerAligned(rotation: Double): Boolean {
        // Allow 5 degrees tolerance
        val tolerance = 5.0
        return rotation <= tolerance || rotation >= (180.0 - tolerance)
    }
}
