package com.soilcarbon.engine.domain.model

import com.soilcarbon.engine.core.Constants
import java.security.MessageDigest
import java.util.Locale

/**
 * Human- and machine-readable provenance envelope. The digest is computed over a canonical,
 * deterministic serialisation of the scientific payload, not over the database row, so it can
 * be recomputed and verified after export.
 */
data class ProvenanceRecord(
    val sampleId: String,
    val createdAtUtc: String,
    val appVersion: String,
    val srsVersion: String,
    val featureVectorVersion: String,
    val modelId: String,
    val modelVersion: String,
    val calibrationSha256: String?,
    val rawImageSha256: String?,
    val jpegImageSha256: String?,
    val deviceModel: String?,
    val algorithm: String = "SHA-256",
    val digest: String,
    val reportStatus: String = Prediction.STATUS_VALID,
    val degraded: Boolean = false,
    val featureProvenance: Map<String, String> = emptyMap(),
) {
    fun toExportMap(): Map<String, Any?> = mapOf(
        "sample_id" to sampleId,
        "created_at" to createdAtUtc,
        "app_version" to appVersion,
        "srs_version" to srsVersion,
        "feature_vector_version" to featureVectorVersion,
        "model_id" to modelId,
        "model_version" to modelVersion,
        "calibration_sha256" to calibrationSha256,
        "raw_image_sha256" to rawImageSha256,
        "jpeg_image_sha256" to jpegImageSha256,
        "device_model" to deviceModel,
        "report_status" to reportStatus,
        "degraded" to degraded,
        "feature_provenance" to featureProvenance,
        "algorithm" to algorithm,
        "digest" to digest
    )
}

object ProvenanceHasher {

    fun canonicalPayload(sample: SoilSample): String = buildString {
        append("id=").append(sample.id).append('\n')
        append("tag=").append(sample.tag).append('\n')
        append("created=").append(sample.createdAtMillis).append('\n')
        append("captured=").append(sample.capturedAtMillis).append('\n')
        append("lat=").append(format(sample.location?.latitude)).append('\n')
        append("lon=").append(format(sample.location?.longitude)).append('\n')
        append("elevation=").append(format(sample.location?.altitudeMeters)).append('\n')
        append("accuracy=").append(sample.location?.accuracyMeters).append('\n')
        append("pitch=").append(sample.imu?.pitchDegrees).append('\n')
        append("roll=").append(sample.imu?.rollDegrees).append('\n')
        append("lux=").append(sample.light?.lux).append('\n')
        append("iso=").append(sample.exposure?.iso).append('\n')
        append("exposure_ns=").append(sample.exposure?.exposureTimeNs).append('\n')
        append("wb_kelvin=").append(sample.exposure?.whiteBalanceKelvin).append('\n')
        append("features=").append(sample.features?.toCanonicalString()).append('\n')
        append("soc=").append(format(sample.prediction?.socPercent)).append('\n')
        append("som=").append(format(sample.prediction?.somPercent)).append('\n')
        append("soc_ci=").append(format(sample.prediction?.socLower95))
            .append(';').append(format(sample.prediction?.socUpper95)).append('\n')
        append("model=").append(sample.prediction?.modelId).append('@')
            .append(sample.prediction?.modelVersion).append('\n')
        append("report_status=").append(sample.prediction?.reportStatus).append('\n')
        append("degraded=").append(sample.prediction?.degraded).append('\n')
        append("feature_provenance=").append(
            sample.prediction?.provenance.orEmpty().toSortedMap()
                .entries.joinToString(",") { "${it.key}:${it.value}" }
        ).append('\n')
        append("calibration=").append(sample.calibration?.let { calibrationHash(it) }).append('\n')
        append("raw_sha256=").append(sample.rawSha256).append('\n')
        append("jpeg_sha256=").append(sample.jpegSha256).append('\n')
        append("bulk_density=").append(sample.soil.bulkDensityGcm3).append('\n')
        append("depth_cm=").append(sample.soil.depthCm).append('\n')
        append("ndvi=").append(format(sample.covariates?.ndvi?.value)).append('\n')
        append("clay=").append(format(sample.covariates?.clayPercent?.value)).append('\n')
    }

    fun sha256(sample: SoilSample): String = sha256Hex(canonicalPayload(sample))

    fun provenance(sample: SoilSample): ProvenanceRecord = ProvenanceRecord(
        sampleId = sample.id,
        createdAtUtc = Constants.isoNow(sample.createdAtMillis),
        appVersion = Constants.SRS_VERSION,
        srsVersion = Constants.SRS_DOCUMENT_ID,
        featureVectorVersion = FeatureVector.FEATURE_VECTOR_VERSION,
        modelId = sample.prediction?.modelId ?: sample.modelId ?: "unknown",
        modelVersion = sample.prediction?.modelVersion ?: sample.modelVersion ?: "unknown",
        calibrationSha256 = sample.calibration?.let { calibrationHash(it) },
        rawImageSha256 = sample.rawSha256,
        jpegImageSha256 = sample.jpegSha256,
        deviceModel = sample.exposure?.deviceModel,
        digest = sha256(sample),
        reportStatus = sample.prediction?.reportStatus ?: Prediction.STATUS_VALID,
        degraded = sample.prediction?.degraded ?: false,
        featureProvenance = sample.prediction?.provenance.orEmpty(),
    )

    fun sha256Hex(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(value.toByteArray(Charsets.UTF_8)).toHex()
    }

    fun sha256Hex(value: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(value).toHex()

    private fun calibrationHash(calibration: CalibrationSummary): String = sha256Hex(
        listOf(
            calibration.cardVersion,
            calibration.createdAtMillis,
            calibration.redGain,
            calibration.redOffset,
            calibration.greenGain,
            calibration.greenOffset,
            calibration.blueGain,
            calibration.blueOffset,
            calibration.overallRmse,
            calibration.illuminationVariation
        ).joinToString("|")
    )

    private fun format(value: Double?): String =
        if (value == null || !value.isFinite()) "null"
        else String.format(Locale.US, "%.8f", value)

    private fun ByteArray.toHex(): String = joinToString("") { byte ->
        String.format(Locale.US, "%02x", byte.toInt() and 0xff)
    }
}
