package com.terrochroma.shared.export

import com.terrochroma.shared.database.DatabaseManager
import com.terrochroma.shared.domain.model.SoilSample
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class ExportServiceImpl(
    private val databaseManager: DatabaseManager? = null
) : ExportService {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    override suspend fun exportToGeoJSON(samples: List<SoilSample>): Result<String> {
        return try {
            val features = samples.map { sample ->
                buildJsonObject {
                    put("type", "Feature")
                    put("geometry", buildJsonObject {
                        put("type", "Point")
                        put("coordinates", buildJsonArray {
                            sample.longitude?.let { add(it) }
                            sample.latitude?.let { add(it) }
                            sample.altitude?.let { add(it) }
                        })
                    })
                    put("properties", buildJsonObject {
                        put("id", sample.id)
                        put("timestamp", sample.timestamp)
                        put("soc_percentage", sample.socPercentage)
                        put("confidence_interval", sample.confidenceInterval)
                        put("som_percentage", sample.somPercentage)
                        put("lab_value", sample.labValue)
                        put("moisture_content", sample.moistureContent)
                        put("image_hash", sample.imageHash)
                        put("provenance_hash", sample.provenanceHash)
                        put("model_version", sample.modelVersion)
                        put("app_version", sample.appVersion)
                        put("is_synced", sample.isSynced)
                        put("notes", sample.notes)

                        // Camera parameters
                        put("camera_iso", sample.cameraParams.iso)
                        put("camera_exposure_time", sample.cameraParams.exposureTime)
                        put("camera_white_balance", sample.cameraParams.whiteBalanceMode.name)
                        put("camera_focus", sample.cameraParams.focusMode.name)
                        put("camera_flash", sample.cameraParams.flashMode.name)
                        put("camera_raw_format", sample.cameraParams.rawFormat.name)

                        // Color features
                        put("cie_lab_l", sample.colorFeatures.cieLab.L)
                        put("cie_lab_a", sample.colorFeatures.cieLab.a)
                        put("cie_lab_b", sample.colorFeatures.cieLab.b)
                        put("hsv_h", sample.colorFeatures.hsv.h)
                        put("hsv_s", sample.colorFeatures.hsv.s)
                        put("hsv_v", sample.colorFeatures.hsv.v)
                        put("munsell_hue", sample.colorFeatures.munsell.hue)
                        put("munsell_value", sample.colorFeatures.munsell.value)
                        put("munsell_chroma", sample.colorFeatures.munsell.chroma)

                        // Texture features
                        put("texture_contrast", sample.textureFeatures.contrast)
                        put("texture_homogeneity", sample.textureFeatures.homogeneity)
                        put("texture_energy", sample.textureFeatures.energy)
                        put("texture_entropy", sample.textureFeatures.entropy)
                    })
                }
            }

            val geoJson = buildJsonObject {
                put("type", "FeatureCollection")
                put("features", buildJsonArray { features.forEach { add(it) } })
            }

            Result.success(json.encodeToString(geoJson))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun exportToKML(samples: List<SoilSample>): Result<String> {
        return try {
            val kmlBuilder = StringBuilder()
            kmlBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
            kmlBuilder.append("<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n")
            kmlBuilder.append("  <Document>\n")
            kmlBuilder.append("    <name>TerroChroma Soil Samples</name>\n")
            kmlBuilder.append("    <description>Soil Organic Carbon measurements</description>\n")

            samples.forEach { sample ->
                if (sample.latitude != null && sample.longitude != null) {
                    kmlBuilder.append("    <Placemark>\n")
                    kmlBuilder.append("      <name>Sample ${sample.id.take(8)}</name>\n")
                    kmlBuilder.append("      <description>\n")
                    kmlBuilder.append("        SOC: ${sample.socPercentage}%\n")
                    kmlBuilder.append("        Confidence: ±${sample.confidenceInterval}%\n")
                    kmlBuilder.append("        SOM: ${sample.somPercentage}%\n")
                    if (sample.moistureContent != null) {
                        kmlBuilder.append("        Moisture: ${sample.moistureContent}%\n")
                    }
                    kmlBuilder.append("        Date: ${java.util.Date(sample.timestamp)}\n")
                    kmlBuilder.append("      </description>\n")
                    kmlBuilder.append("      <Point>\n")
                    kmlBuilder.append("        <coordinates>${sample.longitude},${sample.latitude},${sample.altitude ?: 0.0}</coordinates>\n")
                    kmlBuilder.append("      </Point>\n")
                    kmlBuilder.append("      <Style>\n")
                    kmlBuilder.append("        <IconStyle>\n")
                    kmlBuilder.append("          <color>${getKMLColorForSOC(sample.socPercentage)}</color>\n")
                    kmlBuilder.append("          <scale>1.0</scale>\n")
                    kmlBuilder.append("        </IconStyle>\n")
                    kmlBuilder.append("      </Style>\n")
                    kmlBuilder.append("    </Placemark>\n")
                }
            }

            kmlBuilder.append("  </Document>\n")
            kmlBuilder.append("</kml>")

            Result.success(kmlBuilder.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun exportToCSV(samples: List<SoilSample>): Result<String> {
        return try {
            val csvBuilder = StringBuilder()

            // Header
            csvBuilder.append("id,timestamp,latitude,longitude,altitude,soc_percentage,")
            csvBuilder.append("confidence_interval,som_percentage,lab_value,moisture_content,")
            csvBuilder.append("image_hash,provenance_hash,camera_iso,camera_exposure_time,")
            csvBuilder.append("cie_lab_l,cie_lab_a,cie_lab_b,hsv_h,hsv_s,hsv_v,")
            csvBuilder.append("texture_contrast,texture_homogeneity,texture_energy,texture_entropy,")
            csvBuilder.append("model_version,app_version,is_synced,notes\n")

            // Data rows
            samples.forEach { sample ->
                csvBuilder.append("${escapeCSV(sample.id)},")
                csvBuilder.append("${sample.timestamp},")
                csvBuilder.append("${sample.latitude ?: ""},")
                csvBuilder.append("${sample.longitude ?: ""},")
                csvBuilder.append("${sample.altitude ?: ""},")
                csvBuilder.append("${sample.socPercentage},")
                csvBuilder.append("${sample.confidenceInterval},")
                csvBuilder.append("${sample.somPercentage},")
                csvBuilder.append("${sample.labValue ?: ""},")
                csvBuilder.append("${sample.moistureContent ?: ""},")
                csvBuilder.append("${escapeCSV(sample.imageHash)},")
                csvBuilder.append("${escapeCSV(sample.provenanceHash)},")
                csvBuilder.append("${sample.cameraParams.iso},")
                csvBuilder.append("${sample.cameraParams.exposureTime},")
                csvBuilder.append("${sample.colorFeatures.cieLab.L},")
                csvBuilder.append("${sample.colorFeatures.cieLab.a},")
                csvBuilder.append("${sample.colorFeatures.cieLab.b},")
                csvBuilder.append("${sample.colorFeatures.hsv.h},")
                csvBuilder.append("${sample.colorFeatures.hsv.s},")
                csvBuilder.append("${sample.colorFeatures.hsv.v},")
                csvBuilder.append("${sample.textureFeatures.contrast},")
                csvBuilder.append("${sample.textureFeatures.homogeneity},")
                csvBuilder.append("${sample.textureFeatures.energy},")
                csvBuilder.append("${sample.textureFeatures.entropy},")
                csvBuilder.append("${escapeCSV(sample.modelVersion)},")
                csvBuilder.append("${escapeCSV(sample.appVersion)},")
                csvBuilder.append("${sample.isSynced},")
                csvBuilder.append("${escapeCSV(sample.notes ?: "")}\n")
            }

            Result.success(csvBuilder.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun exportAllToGeoJSON(): Result<String> {
        return try {
            // Placeholder - in production, this would query the database
            val samples = emptyList<SoilSample>()
            exportToGeoJSON(samples)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun exportAllToKML(): Result<String> {
        return try {
            // Placeholder - in production, this would query the database
            val samples = emptyList<SoilSample>()
            exportToKML(samples)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun exportAllToCSV(): Result<String> {
        return try {
            // Placeholder - in production, this would query the database
            val samples = emptyList<SoilSample>()
            exportToCSV(samples)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Helper functions

    private fun getKMLColorForSOC(soc: Double): String {
        // Color code based on SOC percentage
        // KML uses AABBGGRR format (alpha, blue, green, red)
        return when {
            soc < 1.0 -> "ff0000ff" // Red (low SOC)
            soc < 2.0 -> "ff00ff00" // Green (medium-low SOC)
            soc < 3.0 -> "ff00ffff" // Cyan (medium SOC)
            soc < 4.0 -> "ffffff00" // Yellow (medium-high SOC)
            else -> "ffff0000" // Blue (high SOC)
        }
    }

    private fun escapeCSV(value: String): String {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"${value.replace("\"", "\"\"")}\""
        }
        return value
    }
}
