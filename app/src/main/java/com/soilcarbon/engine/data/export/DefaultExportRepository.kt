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
                if (includeHeader) writer.appendLine(ExportMapper.CSV_HEADER)
                samples.forEach { sample -> writer.appendLine(ExportMapper.csvRow(sample)) }
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
                    "properties" to ExportMapper.properties(sample)
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
                    "sample" to ExportMapper.properties(sample),
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
        val directory = File(context.cacheDir, Constants.EXPORT_DIR).apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }
            .format(Date())
        return File(directory, "soilcarbon_$stamp.$extension")
    }

}
