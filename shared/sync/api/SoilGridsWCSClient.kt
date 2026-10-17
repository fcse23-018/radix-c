package com.terrochroma.shared.sync.api

import com.terrochroma.shared.sync.model.*
import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.call.receive
import io.ktor.client.request.*
import io.ktor.client.statement.HttpStatement
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString

class SoilGridsWCSClient(private val httpClient: HttpClient) {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
    }

    companion object {
        // ISRIC SoilGrids WCS endpoints
        private const val BASE_URL = "https://maps.isric.org"
        
        // Available WCS services for different soil properties
        private const val WCS_SOC_SERVICE = "/ows"
        private const val WCS_SOM_SERVICE = "/ows"
        private const val WCS_PH_SERVICE = "/ows"
        private const val WCS_TEXTURE_SERVICE = "/ows"
        private const val WCS_BULK_DENSITY_SERVICE = "/ows"
        private const val WCS_CEC_SERVICE = "/ows"
        
        // Standard depth intervals
        private const val DEPTH_0_5 = "0-5cm"
        private const val DEPTH_5_15 = "5-15cm"
        private const val DEPTH_15_30 = "15-30cm"
        private const val DEPTH_30_60 = "30-60cm"
        private const val DEPTH_60_100 = "60-100cm"
        private const val DEPTH_100_200 = "100-200cm"
        
        // Quantiles
        private const val QUANTILE_MEAN = "mean"
        private const val QUANTILE_MEDIAN = "median"
        private const val QUANTILE_Q05 = "Q0.05"
        private const val QUANTILE_Q25 = "Q0.25"
        private const val QUANTILE_Q75 = "Q0.75"
        private const val QUANTILE_Q95 = "Q0.95"
    }

    /**
     * GetCapabilities - List all available coverages from a WCS service
     * @param servicePath The WCS service path (e.g., "/ows" for SOC)
     * @return WCSCapabilities with all available coverages
     */
    suspend fun getCapabilities(servicePath: String = WCS_SOC_SERVICE): Result<WCSCapabilities> {
        return try {
            val response: HttpResponse = httpClient.get("$BASE_URL$servicePath") {
                parameter("service", "WCS")
                parameter("version", "2.0.1")
                parameter("request", "GetCapabilities")
            }

            val xmlContent = response.bodyAsText()
            val capabilities = parseGetCapabilities(xmlContent)
            Result.success(capabilities)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * DescribeCoverage - Get detailed metadata for a specific coverage
     * @param servicePath The WCS service path
     * @param coverageId The coverage identifier
     * @return WCSCoverageDescription with detailed metadata
     */
    suspend fun describeCoverage(
        servicePath: String = WCS_SOC_SERVICE,
        coverageId: String
    ): Result<WCSCoverageDescription> {
        return try {
            val response: HttpResponse = httpClient.get("$BASE_URL$servicePath") {
                parameter("service", "WCS")
                parameter("version", "2.0.1")
                parameter("request", "DescribeCoverage")
                parameter("coverageid", coverageId)
            }

            val xmlContent = response.bodyAsText()
            val description = parseDescribeCoverage(xmlContent)
            Result.success(description)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * GetCoverage - Download spatial subset of soil data
     * @param servicePath The WCS service path
     * @param coverageId The coverage identifier
     * @param boundingBox Bounding box [minLon, minLat, maxLon, maxLat]
     * @param crs Coordinate reference system (default: EPSG:4326)
     * @param format Output format (default: GeoTIFF)
     * @return WCSCoverageData with the downloaded data
     */
    suspend fun getCoverage(
        servicePath: String = WCS_SOC_SERVICE,
        coverageId: String,
        boundingBox: List<Double>,
        crs: String = "EPSG:4326",
        format: String = "image/geotiff"
    ): Result<WCSCoverageData> {
        return try {
            val response: HttpResponse = httpClient.get("$BASE_URL$servicePath") {
                parameter("service", "WCS")
                parameter("version", "2.0.1")
                parameter("request", "GetCoverage")
                parameter("coverageid", coverageId)
                parameter("bbox", boundingBox.join(","))
                parameter("crs", crs)
                parameter("format", format)
                parameter("response", "raw")
            }

            val data = response.bodyAsByteArray()
            val description = parseCoverageDescription(response)
            
            val coverageData = WCSCoverageData(
                coverageId = coverageId,
                mimeType = format,
                data = data,
                bbox = WCSBoundingBox(
                    lowerCorner = listOf(boundingBox[1], boundingBox[0]),
                    upperCorner = listOf(boundingBox[3], boundingBox[2])
                ),
                crs = crs,
                dimensions = description.dimensions
            )
            
            Result.success(coverageData)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get soil data at a specific point
     * @param latitude Latitude in decimal degrees
     * @param longitude Longitude in decimal degrees
     * @param property Soil property to fetch
     * @param depthRange Depth interval (default: 0-5cm)
     * @param quantile Quantile (default: median)
     * @return WCSSoilData with soil properties at the point
     */
    suspend fun getSoilDataAtPoint(
        latitude: Double,
        longitude: Double,
        property: SoilProperty = SoilProperty.SOC,
        depthRange: String = DEPTH_0_5,
        quantile: String = QUANTILE_MEDIAN
    ): Result<WCSSoilData> {
        return try {
            val servicePath = getServicePathForProperty(property)
            val coverageId = buildCoverageId(property, depthRange, quantile)
            
            // Create a small bounding box around the point (1km radius ~0.01 degrees)
            val bbox = listOf(
                longitude - 0.01,
                latitude - 0.01,
                longitude + 0.01,
                latitude + 0.01
            )
            
            val coverageResult = getCoverage(
                servicePath = servicePath,
                coverageId = coverageId,
                boundingBox = bbox,
                crs = "EPSG:4326",
                format = "image/geotiff"
            )
            
            if (coverageResult.isSuccess) {
                val coverageData = coverageResult.getOrNull()
                // Parse the GeoTIFF to extract value at the center point
                // This is a simplified implementation - actual implementation would use a GeoTIFF library
                val value = parseGeoTIFFValue(coverageData, latitude, longitude)
                
                val soilData = WCSSoilData(
                    soc = if (property == SoilProperty.SOC) value else null,
                    som = if (property == SoilProperty.SOM) value else null,
                    ph = if (property == SoilProperty.PH) value else null,
                    sand = if (property == SoilProperty.SAND) value else null,
                    silt = if (property == SoilProperty.SILT) value else null,
                    clay = if (property == SoilProperty.CLAY) value else null,
                    bulkDensity = if (property == SoilProperty.BULK_DENSITY) value else null,
                    cec = if (property == SoilProperty.CEC) value else null,
                    location = LatLon(latitude, longitude),
                    timestamp = System.currentTimeMillis()
                )
                
                Result.success(soilData)
            } else {
                Result.failure(coverageResult.exceptionOrNull() ?: Exception("Failed to fetch coverage"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get multiple soil properties at a point
     * @param latitude Latitude in decimal degrees
     * @param longitude Longitude in decimal degrees
     * @param depthRange Depth interval (default: 0-5cm)
     * @param quantile Quantile (default: median)
     * @return WCSSoilData with all available properties
     */
    suspend fun getAllSoilDataAtPoint(
        latitude: Double,
        longitude: Double,
        depthRange: String = DEPTH_0_5,
        quantile: String = QUANTILE_MEDIAN
    ): Result<WCSSoilData> {
        return try {
            val properties = listOf(
                SoilProperty.SOC,
                SoilProperty.SOM,
                SoilProperty.PH,
                SoilProperty.SAND,
                SoilProperty.SILT,
                SoilProperty.CLAY,
                SoilProperty.BULK_DENSITY,
                SoilProperty.CEC
            )
            
            val results = properties.map { property ->
                getSoilDataAtPoint(latitude, longitude, property, depthRange, quantile)
            }
            
            // Combine all results into one WCSSoilData
            val combinedData = WCSSoilData(
                soc = results[0].getOrNull()?.soc,
                som = results[1].getOrNull()?.som,
                ph = results[2].getOrNull()?.ph,
                sand = results[3].getOrNull()?.sand,
                silt = results[4].getOrNull()?.silt,
                clay = results[5].getOrNull()?.clay,
                bulkDensity = results[6].getOrNull()?.bulkDensity,
                cec = results[7].getOrNull()?.cec,
                location = LatLon(latitude, longitude),
                timestamp = System.currentTimeMillis()
            )
            
            Result.success(combinedData)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Download complete global map for a soil property
     * @param property Soil property to download
     * @param depthRange Depth interval
     * @param quantile Quantile
     * @return WCSCoverageData with complete global map
     */
    suspend fun downloadGlobalMap(
        property: SoilProperty = SoilProperty.SOC,
        depthRange: String = DEPTH_0_5,
        quantile: String = QUANTILE_MEDIAN
    ): Result<WCSCoverageData> {
        return try {
            val servicePath = getServicePathForProperty(property)
            val coverageId = buildCoverageId(property, depthRange, quantile)
            
            // Global bounding box (-180, -90, 180, 90)
            val globalBbox = listOf(-180.0, -90.0, 180.0, 90.0)
            
            val coverageResult = getCoverage(
                servicePath = servicePath,
                coverageId = coverageId,
                boundingBox = globalBbox,
                crs = "EPSG:4326",
                format = "image/geotiff"
            )
            
            coverageResult
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Helper functions

    private fun getServicePathForProperty(property: SoilProperty): String {
        // Most SoilGrids properties use the same OWS service
        return "/ows"
    }

    private fun buildCoverageId(property: SoilProperty, depthRange: String, quantile: String): String {
        // Coverage ID format varies by service, but generally follows a pattern
        // This is a simplified implementation - actual IDs may differ
        val propertyCode = when (property) {
            SoilProperty.SOC -> "soc"
            SoilProperty.SOM -> "som"
            SoilProperty.PH -> "ph"
            SoilProperty.SAND -> "sand"
            SoilProperty.SILT -> "silt"
            SoilProperty.CLAY -> "clay"
            SoilProperty.BULK_DENSITY -> "bdod"
            SoilProperty.CEC -> "cec"
        }
        
        return "${propertyCode}_${depthRange}_$quantile"
    }

    private fun parseGetCapabilities(xmlContent: String): WCSCapabilities {
        // Parse XML response from GetCapabilities
        // This is a simplified parser - actual implementation would use an XML parsing library
        val coverages = mutableListOf<WCSCoverageSummary>()
        
        // Extract coverage information from XML
        // For now, return placeholder data
        return WCSCapabilities(
            serviceTitle = "ISRIC SoilGrids",
            serviceAbstract = "Global gridded soil information",
            serviceIdentification = "SoilGrids 2.0",
            serviceProvider = "ISRIC - World Soil Information",
            operationMetadata = "WCS 2.0.1",
            coverages = coverages
        )
    }

    private fun parseDescribeCoverage(xmlContent: String): WCSCoverageDescription {
        // Parse XML response from DescribeCoverage
        // This is a simplified parser - actual implementation would use an XML parsing library
        return WCSCoverageDescription(
            coverageId = "soc_0-5cm_median",
            title = "Soil Organic Carbon 0-5cm Median",
            abstract = "Median prediction of Soil Organic Carbon content in topsoil",
            keywords = listOf("SOC", "carbon", "soil", "organic"),
            dimensions = listOf(
                WCSDimension("Latitude", "Geographic latitude", "degrees", -90.0, 90.0),
                WCSDimension("Longitude", "Geographic longitude", "degrees", -180.0, 180.0)
            ),
            boundingBox = WCSBoundingBox(
                lowerCorner = listOf(-180.0, -90.0),
                upperCorner = listOf(180.0, 90.0)
            ),
            supportedCRS = listOf("EPSG:4326", "EPSG:3857"),
            supportedFormats = listOf("image/geotiff", "image/tiff", "application/geotiff")
        )
    }

    private fun parseCoverageDescription(response: HttpResponse): WCSCoverageDescription {
        // Extract coverage description from response headers
        // This is a simplified implementation
        return WCSCoverageDescription(
            coverageId = "soc_0-5cm_median",
            title = "Soil Organic Carbon 0-5cm Median",
            abstract = "Median prediction of Soil Organic Carbon content in topsoil",
            keywords = listOf("SOC", "carbon", "soil", "organic"),
            dimensions = listOf(
                WCSDimension("Latitude", "Geographic latitude", "degrees", -90.0, 90.0),
                WCSDimension("Longitude", "Geographic longitude", "degrees", -180.0, 180.0)
            ),
            boundingBox = WCSBoundingBox(
                lowerCorner = listOf(-180.0, -90.0),
                upperCorner = listOf(180.0, 90.0)
            ),
            supportedCRS = listOf("EPSG:4326", "EPSG:3857"),
            supportedFormats = listOf("image/geotiff", "image/tiff", "application/geotiff")
        )
    }

    private fun parseGeoTIFFValue(coverageData: WCSCoverageData?, latitude: Double, longitude: Double): Double {
        // Parse GeoTIFF to extract value at specific coordinates
        // This is a simplified implementation - actual implementation would use a GeoTIFF library
        // For now, return a placeholder value
        return 2.5 // Placeholder SOC value
    }
}
