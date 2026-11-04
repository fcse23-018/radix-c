package com.soilcarbon.engine.domain.calibration

import com.soilcarbon.engine.core.Constants

data class CardPoint(val xMm: Double, val yMm: Double)

data class CardRect(val leftMm: Double, val topMm: Double, val widthMm: Double, val heightMm: Double) {
    val rightMm: Double get() = leftMm + widthMm
    val bottomMm: Double get() = topMm + heightMm
    val centerMm: CardPoint get() = CardPoint(leftMm + widthMm / 2.0, topMm + heightMm / 2.0)
    fun contains(x: Double, y: Double): Boolean =
        x in leftMm..rightMm && y in topMm..bottomMm
}

data class MarkerSpec(
    val id: Int,
    val role: String,
    val center: CardPoint,
    val sizeMm: Double
)

data class PatchSpec(
    val id: String,
    val name: String,
    val targetReflectance: Double,
    val rect: CardRect
)

data class CalibrationTargetSpec(
    val version: String,
    val cardWidthMm: Double,
    val cardHeightMm: Double,
    val markers: List<MarkerSpec>,
    val patches: List<PatchSpec>,
    val aperture: CardRect,
    val patchSampleInsetFraction: Double = 0.20,
    val substrate: String = "Matte synthetic polymer (Yupo/polycarbonate)",
    val notes: String = ""
) {
    val cornerMarkers: List<MarkerSpec>
        get() = markers.filter { it.id in Constants.ARUCO_CORNER_IDS }

    val centerMarker: MarkerSpec?
        get() = markers.firstOrNull { it.id == Constants.ARUCO_CENTER_ID }

    fun marker(id: Int): MarkerSpec? = markers.firstOrNull { it.id == id }

    companion object {
        /**
         * Default (v1.0) physical target. All dimensions are millimetres in card space,
         * origin at the top-left printable boundary. The same specification is shipped as
         * assets/calibration/calibration_target.json and docs/CALIBRATION_TARGET.md.
         *
         * Layout (180 x 140 mm):
         *   - Four 22 mm ArUco corner markers (IDs 20-23)
         *   - One 22 mm ArUco centre marker (ID 24)
         *   - Six 22 mm neutral patches at 99/80/60/40/20/5 % reflectance
         *   - A 30 x 30 mm soil aperture
         */
        fun default(): CalibrationTargetSpec = CalibrationTargetSpec(
            version = "SOC-CARD-2026.1",
            cardWidthMm = 180.0,
            cardHeightMm = 140.0,
            markers = listOf(
                MarkerSpec(20, "TOP_LEFT", CardPoint(16.0, 16.0), 22.0),
                MarkerSpec(21, "TOP_RIGHT", CardPoint(164.0, 16.0), 22.0),
                MarkerSpec(22, "BOTTOM_RIGHT", CardPoint(164.0, 124.0), 22.0),
                MarkerSpec(23, "BOTTOM_LEFT", CardPoint(16.0, 124.0), 22.0),
                MarkerSpec(24, "CENTER_ANCHOR", CardPoint(90.0, 16.0), 22.0)
            ),
            patches = listOf(
                PatchSpec("P99", "99% white", 0.99, CardRect(10.0, 38.0, 22.0, 22.0)),
                PatchSpec("P80", "80% grey", 0.80, CardRect(37.0, 38.0, 22.0, 22.0)),
                PatchSpec("P60", "60% grey", 0.60, CardRect(64.0, 38.0, 22.0, 22.0)),
                PatchSpec("P40", "40% grey", 0.40, CardRect(91.0, 38.0, 22.0, 22.0)),
                PatchSpec("P20", "20% grey", 0.20, CardRect(118.0, 38.0, 22.0, 22.0)),
                PatchSpec("P05", "5% black", 0.05, CardRect(145.0, 38.0, 22.0, 22.0))
            ),
            aperture = CardRect(75.0, 77.0, 30.0, 30.0),
            notes = "Print at 100% scale on matte synthetic stock. Verify the 100 mm scale bar " +
                "after printing. The aperture is a physical cut-out; place the card on a flat, " +
                "level soil surface."
        )
    }
}

enum class MarkerStatus {
    SEARCHING,
    PARTIAL,
    ALIGNED,
    TOO_CLOSE,
    TOO_FAR,
    BAD_ANGLE,
    ERROR
}

data class NormalizedPoint(val x: Float, val y: Float)

data class DetectedMarker(
    val id: Int,
    val corners: List<NormalizedPoint>,
    val center: NormalizedPoint
)

data class TargetDetection(
    val status: MarkerStatus,
    val markers: List<DetectedMarker>,
    val apertureQuad: List<NormalizedPoint> = emptyList(),
    val cardQuad: List<NormalizedPoint> = emptyList(),
    val patchQuads: Map<String, List<NormalizedPoint>> = emptyMap(),
    val rotationDegrees: Float = 0f,
    val estimatedDistanceMm: Float? = null,
    val detectedIds: Set<Int> = emptySet(),
    val message: String = "",
    val analysisWidth: Int = 0,
    val analysisHeight: Int = 0
) {
    val hasCenterMarker: Boolean get() = Constants.ARUCO_CENTER_ID in detectedIds
    val cornerCount: Int get() = detectedIds.count { it in Constants.ARUCO_CORNER_IDS }
    val isAligned: Boolean get() = status == MarkerStatus.ALIGNED
    val isComplete: Boolean get() = hasCenterMarker && cornerCount >= 3
}
