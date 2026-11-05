package com.soilcarbon.engine.data.export

import com.soilcarbon.engine.core.Constants
import com.soilcarbon.engine.domain.model.SoilSample

/**
 * Pure mapping from a stored sample to the scientific export payload.
 *
 * Kept free of Android and file-system types so the export contract (required keys, CSV column
 * count, escaping) can be asserted by JVM tests. [DefaultExportRepository] only adds the file and
 * serialisation concerns.
 */
object ExportMapper {

    /**
     * CSV columns, in the exact order produced by [csvRow]. Any change here must also change
     * [csvRow]; `ExportMapperTest` asserts the two stay in lockstep.
     */
    const val CSV_HEADER =
        "sample_id,tag,created_utc,latitude,longitude,altitude_m,gps_accuracy_m,status," +
            "soc_percent,som_percent,soc_lower_95,soc_upper_95,uncertainty_95,confidence," +
            "carbon_stock_mg_per_ha,L_star,a_star,b_star,munsell_value,glcm_contrast,glcm_energy," +
            "ndvi_satellite,clay_content_percent,ndvi_source,clay_source,moisture_percent," +
            "moisture_method,bulk_density_g_cm3,depth_cm,model_id,model_version," +
            "report_status,degraded,demo_not_valid,feature_provenance," +
            "provenance_sha256,jpeg_sha256,raw_sha256,calibration_quality,exposure_iso," +
            "exposure_time_ns,white_balance_kelvin,lux,tilt_degrees,notes"

    /** Keys that every JSON/GeoJSON export record must carry. */
    val REQUIRED_KEYS = listOf(
        "status",
        "report_status",
        "degraded",
        "feature_provenance",
        "model_id",
        "model_version"
    )

    fun properties(sample: SoilSample): Map<String, Any?> = mapOf(
        "sample_id" to sample.id,
        "tag" to sample.tag,
        "notes" to sample.notes,
        "role" to sample.role.name,
        "status" to sample.status.name,
        "created_utc" to Constants.isoNow(sample.createdAtMillis),
        "latitude" to sample.location?.latitude,
        "longitude" to sample.location?.longitude,
        "altitude_m" to sample.location?.altitudeMeters,
        "gps_accuracy_m" to sample.location?.accuracyMeters,
        "soc_percent" to sample.prediction?.socPercent,
        "som_percent" to sample.prediction?.somPercent,
        "soc_lower_95" to sample.prediction?.socLower95,
        "soc_upper_95" to sample.prediction?.socUpper95,
        "uncertainty_95" to sample.prediction?.uncertainty95,
        "confidence" to sample.prediction?.confidenceScore,
        "carbon_stock_mg_per_ha" to sample.prediction?.carbonStockMgPerHa,
        "L_star" to sample.features?.lStar,
        "a_star" to sample.features?.aStar,
        "b_star" to sample.features?.bStar,
        "munsell_value" to sample.features?.munsellValue,
        "munsell_notation" to sample.munsell?.notation(),
        "glcm_contrast" to sample.features?.glcmContrast,
        "glcm_energy" to sample.features?.glcmEnergy,
        "glcm_homogeneity" to sample.texture?.homogeneity,
        "glcm_correlation" to sample.texture?.correlation,
        "ndvi_satellite" to sample.features?.ndviSatellite,
        "clay_content_percent" to sample.features?.clayContentPercent,
        "ndvi_source" to sample.covariates?.ndvi?.source?.name,
        "clay_source" to sample.covariates?.clayPercent?.source?.name,
        "offline_fallback" to sample.covariates?.isOfflineFallback,
        "moisture_percent" to sample.moisture?.percentGravimetric,
        "moisture_method" to sample.moisture?.method?.name,
        "bulk_density_g_cm3" to sample.soil.bulkDensityGcm3,
        "depth_cm" to sample.soil.depthCm,
        "model_id" to sample.prediction?.modelId,
        "model_version" to sample.prediction?.modelVersion,
        "fallback_model" to sample.prediction?.fallbackUsed,
        "report_status" to sample.prediction?.reportStatus,
        "degraded" to sample.prediction?.degraded,
        "demo_not_valid" to (sample.prediction?.isDemo == true),
        "feature_provenance" to sample.prediction?.provenance,
        "tilt_degrees" to sample.imu?.maxTiltDegrees,
        "lux" to sample.light?.lux,
        "exposure_iso" to sample.exposure?.iso,
        "exposure_time_ns" to sample.exposure?.exposureTimeNs,
        "white_balance_kelvin" to sample.exposure?.whiteBalanceKelvin,
        "device_model" to sample.exposure?.deviceModel,
        "raw_image_sha256" to sample.rawSha256,
        "jpeg_image_sha256" to sample.jpegSha256,
        "provenance_sha256" to sample.provenanceSha256
    )

    fun csvRow(sample: SoilSample): String {
        val values = listOf(
            sample.id,
            sample.tag,
            Constants.isoNow(sample.createdAtMillis),
            sample.location?.latitude?.toString() ?: "",
            sample.location?.longitude?.toString() ?: "",
            sample.location?.altitudeMeters?.toString() ?: "",
            sample.location?.accuracyMeters?.toString() ?: "",
            sample.status.name,
            sample.prediction?.socPercent?.toString() ?: "",
            sample.prediction?.somPercent?.toString() ?: "",
            sample.prediction?.socLower95?.toString() ?: "",
            sample.prediction?.socUpper95?.toString() ?: "",
            sample.prediction?.uncertainty95?.toString() ?: "",
            sample.prediction?.confidenceScore?.toString() ?: "",
            sample.prediction?.carbonStockMgPerHa?.toString() ?: "",
            sample.features?.lStar?.toString() ?: "",
            sample.features?.aStar?.toString() ?: "",
            sample.features?.bStar?.toString() ?: "",
            sample.features?.munsellValue?.toString() ?: "",
            sample.features?.glcmContrast?.toString() ?: "",
            sample.features?.glcmEnergy?.toString() ?: "",
            sample.features?.ndviSatellite?.toString() ?: "",
            sample.features?.clayContentPercent?.toString() ?: "",
            sample.covariates?.ndvi?.source?.name ?: "",
            sample.covariates?.clayPercent?.source?.name ?: "",
            sample.moisture?.percentGravimetric?.toString() ?: "",
            sample.moisture?.method?.name ?: "",
            sample.soil.bulkDensityGcm3.toString(),
            sample.soil.depthCm.toString(),
            sample.prediction?.modelId ?: "",
            sample.prediction?.modelVersion ?: "",
            sample.prediction?.reportStatus ?: "",
            (sample.prediction?.degraded == true).toString(),
            (sample.prediction?.isDemo == true).toString(),
            sample.prediction?.provenance.orEmpty().toSortedMap()
                .entries.joinToString(";") { "${it.key}:${it.value}" },
            sample.provenanceSha256 ?: "",
            sample.jpegSha256 ?: "",
            sample.rawSha256 ?: "",
            sample.calibration?.qualityLabel ?: "",
            sample.exposure?.iso?.toString() ?: "",
            sample.exposure?.exposureTimeNs?.toString() ?: "",
            sample.exposure?.whiteBalanceKelvin?.toString() ?: "",
            sample.light?.lux?.toString() ?: "",
            sample.imu?.maxTiltDegrees?.toString() ?: "",
            sample.notes
        )
        return values.joinToString(",") { escapeCsv(it) }
    }

    fun escapeCsv(value: String): String {
        val needsQuotes = value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        val escaped = value.replace("\"", "\"\"")
        return if (needsQuotes) "\"$escaped\"" else escaped
    }
}
