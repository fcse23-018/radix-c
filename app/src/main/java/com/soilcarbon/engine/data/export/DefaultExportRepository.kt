package com.soilcarbon.engine.data.export

import android.content.Context
import com.google.gson.GsonBuilder
import com.soilcarbon.engine.core.Constants
import com.soilcarbon.engine.domain.model.ProvenanceHasher
import com.soilcarbon.engine.domain.model.SoilSample
import com.soilcarbon.engine.domain.repository.ExportRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import dagger.hilt.android.qualifiers.ApplicationContext

class DefaultExportRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : ExportRepository {

    private val gson = GsonBuilder().setPrettyPrinting().serializeNulls().create()

    override suspend fun exportCsv(samples: List<SoilSample>, includeHeader: Boolean): String =
        withContext(Dispatchers.IO) {
            val file = newExportFile("csv")
            file.bufferedWriter().use { writer ->
                if (includeHeader) writer.appendLine(CSV_HEADER)
                samples.forEach { sample -> writer.appendLine(sample.toCsvRow()) }
            }
            file.absolutePath
        }

    override suspend fun exportGeoJson(samples: List<SoilSample>): String =
        withContext(Dispatchers.IO) {
            val features = samples.map { sample ->
                mapOf(
                    "type" to "Feature",
                    "geometry" to sample.location?.takeIf { it.isValid() }?.let { location ->
                        mapOf(
                            "type" to "Point",
                            "coordinates" to listOf(location.longitude, location.latitude)
                        )
                    },
                    "properties" to sample.toExportProperties()
                )
            }
            val root = mapOf(
                "type" to "FeatureCollection",
                "name" to "SoilCarbon Engine samples",
                "crs" to mapOf(
                    "type" to "name",
                    "properties" to mapOf("name" to "urn:ogc:def:crs:OGC:1.3:CRS84")
                ),
                "generated_utc" to Constants.isoNow(),
                "srs_version" to Constants.SRS_DOCUMENT_ID,
                "features" to features
            )
            val file = newExportFile("geojson")
            file.writeText(gson.toJson(root))
            file.absolutePath
        }

    override suspend fun exportBundle(samples: List<SoilSample>): String =
        withContext(Dispatchers.IO) {
            val records = samples.map { sample ->
                mapOf(
                    "sample" to sample.toExportProperties(),
                    "calibration" to sample.calibration?.let { calibration ->
                        mapOf(
                            "card_version" to calibration.cardVersion,
                            "quality" to calibration.qualityLabel,
                            "overall_rmse" to calibration.overallRmse,
                            "worst_r_squared" to calibration.worstRSquared,
                            "illumination_variation" to calibration.illuminationVariation,
                            "red_gain" to calibration.redGain,
                            "red_offset" to calibration.redOffset,
                            "green_gain" to calibration.greenGain,
                            "green_offset" to calibration.greenOffset,
                            "blue_gain" to calibration.blueGain,
                            "blue_offset" to calibration.blueOffset,
                            "captured_at_ms" to calibration.createdAtMillis
                        )
                    },
                    "moisture" to sample.moisture?.let {
                        mapOf(
                            "percent" to it.percentGravimetric,
                            "method" to it.method.name,
                            "confidence" to it.confidence,
                            "source" to it.source
                        )
                    },
                    "covariates" to sample.covariates?.toExportMap(),
                    "provenance" to ProvenanceHasher.provenance(sample).toExportMap()
                )
            }
            val root = mapOf(
                "schema" to "soilcarbon-bundle/1.0",
                "srs_version" to Constants.SRS_DOCUMENT_ID,
                "app_version" to Constants.SRS_VERSION,
                "feature_vector_version" to com.soilcarbon.engine.domain.model.FeatureVector.FEATURE_VECTOR_VERSION,
                "generated_utc" to Constants.isoNow(),
                "count" to samples.size,
                "records" to records
            )
            val file = newExportFile("bundle.json")
            file.writeText(gson.toJson(root))
            file.absolutePath
        }

    override suspend fun exportProvenance(samples: List<SoilSample>): String =
        withContext(Dispatchers.IO) {
            val records = samples.map { ProvenanceHasher.provenance(it).toExportMap() }
            val root = mapOf(
                "schema" to "soilcarbon-provenance/1.0",
                "algorithm" to "SHA-256",
                "generated_utc" to Constants.isoNow(),
                "records" to records
            )
            val file = newExportFile("provenance.json")
            file.writeText(gson.toJson(root))
            file.absolutePath
        }

    private fun newExportFile(extension: String): File {
        val directory = File(context.cacheDir, "exports").apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }
            .format(Date())
        return File(directory, "soilcarbon_$stamp.$extension")
    }

    private fun SoilSample.toExportProperties(): Map<String, Any?> = mapOf(
        "sample_id" to id,
        "tag" to tag,
        "notes" to notes,
        "role" to role.name,
        "status" to status.name,
        "created_utc" to Constants.isoNow(createdAtMillis),
        "latitude" to location?.latitude,
        "longitude" to location?.longitude,
        "altitude_m" to location?.altitudeMeters,
        "gps_accuracy_m" to location?.accuracyMeters,
        "soc_percent" to prediction?.socPercent,
        "som_percent" to prediction?.somPercent,
        "soc_lower_95" to prediction?.socLower95,
        "soc_upper_95" to prediction?.socUpper95,
        "uncertainty_95" to prediction?.uncertainty95,
        "confidence" to prediction?.confidenceScore,
        "carbon_stock_mg_per_ha" to prediction?.carbonStockMgPerHa,
        "L_star" to features?.lStar,
        "a_star" to features?.aStar,
        "b_star" to features?.bStar,
        "munsell_value" to features?.munsellValue,
        "munsell_notation" to munsell?.notation(),
        "glcm_contrast" to features?.glcmContrast,
        "glcm_energy" to features?.glcmEnergy,
        "glcm_homogeneity" to texture?.homogeneity,
        "glcm_correlation" to texture?.correlation,
        "ndvi_satellite" to features?.ndviSatellite,
        "clay_content_percent" to features?.clayContentPercent,
        "ndvi_source" to covariates?.ndvi?.source?.name,
        "clay_source" to covariates?.clayPercent?.source?.name,
        "offline_fallback" to covariates?.isOfflineFallback,
        "moisture_percent" to moisture?.percentGravimetric,
        "moisture_method" to moisture?.method?.name,
        "bulk_density_g_cm3" to soil.bulkDensityGcm3,
        "depth_cm" to soil.depthCm,
        "model_id" to prediction?.modelId,
        "model_version" to prediction?.modelVersion,
        "fallback_model" to prediction?.fallbackUsed,
        "report_status" to prediction?.reportStatus,
        "degraded" to prediction?.degraded,
        "demo_not_valid" to (prediction?.isDemo == true),
        "feature_provenance" to prediction?.provenance,
        "tilt_degrees" to imu?.maxTiltDegrees,
        "lux" to light?.lux,
        "exposure_iso" to exposure?.iso,
        "exposure_time_ns" to exposure?.exposureTimeNs,
        "white_balance_kelvin" to exposure?.whiteBalanceKelvin,
        "device_model" to exposure?.deviceModel,
        "raw_image_sha256" to rawSha256,
        "jpeg_image_sha256" to jpegSha256,
        "provenance_sha256" to provenanceSha256
    )

    private fun SoilSample.toCsvRow(): String {
        val values = listOf(
            id,
            tag,
            Constants.isoNow(createdAtMillis),
            location?.latitude?.toString() ?: "",
            location?.longitude?.toString() ?: "",
            location?.altitudeMeters?.toString() ?: "",
            location?.accuracyMeters?.toString() ?: "",
            status.name,
            prediction?.socPercent?.toString() ?: "",
            prediction?.somPercent?.toString() ?: "",
            prediction?.socLower95?.toString() ?: "",
            prediction?.socUpper95?.toString() ?: "",
            prediction?.uncertainty95?.toString() ?: "",
            prediction?.confidenceScore?.toString() ?: "",
            prediction?.carbonStockMgPerHa?.toString() ?: "",
            features?.lStar?.toString() ?: "",
            features?.aStar?.toString() ?: "",
            features?.bStar?.toString() ?: "",
            features?.munsellValue?.toString() ?: "",
            features?.glcmContrast?.toString() ?: "",
            features?.glcmEnergy?.toString() ?: "",
            features?.ndviSatellite?.toString() ?: "",
            features?.clayContentPercent?.toString() ?: "",
            covariates?.ndvi?.source?.name ?: "",
            covariates?.clayPercent?.source?.name ?: "",
            moisture?.percentGravimetric?.toString() ?: "",
            moisture?.method?.name ?: "",
            soil.bulkDensityGcm3.toString(),
            soil.depthCm.toString(),
            prediction?.modelId ?: "",
            prediction?.modelVersion ?: "",
            prediction?.reportStatus ?: "",
            (prediction?.degraded == true).toString(),
            (prediction?.isDemo == true).toString(),
            prediction?.provenance.orEmpty().toSortedMap()
                .entries.joinToString(";") { "${it.key}:${it.value}" },
            provenanceSha256 ?: "",
            jpegSha256 ?: "",
            rawSha256 ?: "",
            calibration?.qualityLabel ?: "",
            exposure?.iso?.toString() ?: "",
            exposure?.exposureTimeNs?.toString() ?: "",
            exposure?.whiteBalanceKelvin?.toString() ?: "",
            light?.lux?.toString() ?: "",
            imu?.maxTiltDegrees?.toString() ?: "",
            notes
        )
        return values.joinToString(",") { escapeCsv(it) }
    }

    private fun escapeCsv(value: String): String {
        val needsQuotes = value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        val escaped = value.replace("\"", "\"\"")
        return if (needsQuotes) "\"$escaped\"" else escaped
    }

    private companion object {
        const val CSV_HEADER =
            "sample_id,tag,created_utc,latitude,longitude,altitude_m,gps_accuracy_m,status," +
                "soc_percent,som_percent,soc_lower_95,soc_upper_95,uncertainty_95,confidence," +
                "carbon_stock_mg_per_ha,L_star,a_star,b_star,munsell_value,glcm_contrast,glcm_energy," +
                "ndvi_satellite,clay_content_percent,ndvi_source,clay_source,moisture_percent," +
                "moisture_method,bulk_density_g_cm3,depth_cm,model_id,model_version," +
                "report_status,degraded,demo_not_valid,feature_provenance," +
                "provenance_sha256,jpeg_sha256,raw_sha256,calibration_quality,exposure_iso," +
                "exposure_time_ns,white_balance_kelvin,lux,tilt_degrees,notes"
    }
}
