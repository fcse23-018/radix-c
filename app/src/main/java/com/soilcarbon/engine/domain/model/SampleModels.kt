package com.soilcarbon.engine.domain.model

import com.soilcarbon.engine.domain.vision.CalibrationResult
import com.soilcarbon.engine.domain.vision.ColorFeatures
import com.soilcarbon.engine.domain.vision.LabColor
import com.soilcarbon.engine.domain.vision.MoistureEstimate
import com.soilcarbon.engine.domain.vision.MunsellColor
import com.soilcarbon.engine.domain.vision.ReflectanceRgb
import com.soilcarbon.engine.domain.vision.TextureFeatures

enum class SampleStatus {
    DRAFT,
    CAPTURED,
    ANALYZING,
    ANALYZED,
    EXPORTED,
    FAILED;

    val isTerminal: Boolean get() = this == ANALYZED || this == EXPORTED
}

data class SoilProperties(
    val bulkDensityGcm3: Double = 1.30,
    val depthCm: Double = 30.0
) {
    init {
        require(bulkDensityGcm3 in 0.5..2.5) { "Bulk density out of range" }
        require(depthCm in 1.0..200.0) { "Depth out of range" }
    }
}

data class CalibrationSummary(
    val cardVersion: String,
    val sourceDescription: String,
    val overallRmse: Double,
    val worstRSquared: Double,
    val illuminationVariation: Double,
    val qualityLabel: String,
    val redGain: Double,
    val redOffset: Double,
    val greenGain: Double,
    val greenOffset: Double,
    val blueGain: Double,
    val blueOffset: Double,
    val createdAtMillis: Long
) {
    companion object {
        fun from(result: CalibrationResult, cardVersion: String): CalibrationSummary = CalibrationSummary(
            cardVersion = cardVersion,
            sourceDescription = result.sourceDescription,
            overallRmse = result.overallRmse,
            worstRSquared = result.worstRSquared,
            illuminationVariation = result.illuminationVariation,
            qualityLabel = result.qualityLabel(),
            redGain = result.red.gain,
            redOffset = result.red.offset,
            greenGain = result.green.gain,
            greenOffset = result.green.offset,
            blueGain = result.blue.gain,
            blueOffset = result.blue.offset,
            createdAtMillis = result.createdAtMillis
        )
    }
}

data class CalibrationProfile(
    val id: String,
    val cardVersion: String,
    val deviceModel: String,
    val result: CalibrationResult,
    val sha256: String,
    val createdAtMillis: Long = System.currentTimeMillis()
)

/**
 * One encrypted file owned by a sample. [kind] is the logical slot (`jpeg`, `raw`, `aperture`),
 * while [relativePath] is the AES-GCM envelope path inside [com.soilcarbon.engine.data.security.EncryptedFileStore].
 */
data class StoredBlob(
    val sampleId: String,
    val kind: String,
    val relativePath: String,
    val sha256: String,
    val sizeBytes: Long
)

/**
 * Complete, self-contained record of one field observation. Everything required to reproduce
 * the SOC estimate is retained: capture conditions, calibration, raw feature vector, covariates,
 * model identity and provenance digest.
 */
data class SoilSample(
    val id: String,
    val tag: String = "",
    val notes: String = "",
    val role: UserRole = UserRole.FIELD_TECHNICIAN,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val capturedAtMillis: Long? = null,
    val status: SampleStatus = SampleStatus.DRAFT,
    val syncState: SyncState = SyncState.LOCAL_ONLY,

    val location: GeoPoint? = null,
    val imu: ImuReading? = null,
    val light: LightReading? = null,
    val exposure: CameraExposureParameters? = null,

    val rawImagePath: String? = null,
    val jpegImagePath: String? = null,
    val cardJpegPath: String? = null,
    val apertureCropPath: String? = null,
    val rawSha256: String? = null,
    val jpegSha256: String? = null,

    val calibration: CalibrationSummary? = null,
    val color: ColorFeatures? = null,
    val texture: TextureFeatures? = null,
    val lab: LabColor? = null,
    val munsell: MunsellColor? = null,
    val reflectance: ReflectanceRgb? = null,
    val moisture: MoistureEstimate? = null,
    val moistureCorrectionNotes: String? = null,

    val features: FeatureVector? = null,
    val prediction: Prediction? = null,
    val covariates: EnvironmentalCovariates? = null,
    val soil: SoilProperties = SoilProperties(),

    val provenanceSha256: String? = null,
    val modelId: String? = null,
    val modelVersion: String? = null,

    val exportedAtMillis: Long? = null,
    val syncedAtMillis: Long? = null,
    val errorMessage: String? = null
) {
    val isAnalyzed: Boolean get() = prediction != null && features != null
    val displayName: String get() = tag.ifBlank { "Sample ${id.take(8)}" }
    val shortId: String get() = id.take(8)
}

data class DraftCapture(
    val tag: String = "",
    val notes: String = "",
    val location: GeoPoint? = null,
    val imu: ImuReading? = null,
    val light: LightReading? = null,
    val exposure: CameraExposureParameters? = null
)

data class SampleStats(
    val total: Int = 0,
    val analyzed: Int = 0,
    val pendingSync: Int = 0,
    val meanSocPercent: Double? = null,
    val meanSomPercent: Double? = null,
    val meanUncertainty: Double? = null,
    val meanConfidence: Double? = null,
    val totalCarbonMgPerHa: Double? = null
) {
    val captureReadiness: Float
        get() = if (total == 0) 0f else (analyzed.toFloat() / total.toFloat()).coerceIn(0f, 1f)
}
