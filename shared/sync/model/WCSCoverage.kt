package com.terrochroma.shared.sync.model

import kotlinx.serialization.Serializable

@Serializable
data class LatLon(
    val latitude: Double,
    val longitude: Double
)

@Serializable
data class WCSCapabilities(
    val serviceTitle: String,
    val serviceAbstract: String,
    val serviceIdentification: String,
    val serviceProvider: String,
    val operationMetadata: String,
    val coverages: List<WCSCoverageSummary>
)

@Serializable
data class WCSCoverageSummary(
    val coverageId: String,
    val title: String,
    val abstract: String,
    val keywords: List<String>,
    val boundingBox: WCSBoundingBox,
    val supportedCRS: List<String>,
    val supportedFormats: List<String>
)

@Serializable
data class WCSBoundingBox(
    val lowerCorner: List<Double>,
    val upperCorner: List<Double>
)

@Serializable
data class WCSCoverageDescription(
    val coverageId: String,
    val title: String,
    val abstract: String,
    val keywords: List<String>,
    val dimensions: List<WCSDimension>,
    val boundingBox: WCSBoundingBox,
    val supportedCRS: List<String>,
    val supportedFormats: List<String>
)

@Serializable
data class WCSDimension(
    val name: String,
    val description: String,
    val uom: String,
    val lowerBound: Double,
    val upperBound: Double
)

@Serializable
data class WCSCoverageData(
    val coverageId: String,
    val mimeType: String,
    val data: ByteArray,
    val bbox: WCSBoundingBox,
    val crs: String,
    val dimensions: List<WCSDimension>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as WCSCoverageData

        if (!data.contentEquals(other.data)) return false
        if (coverageId != other.coverageId) return false
        if (mimeType != other.mimeType) return false
        if (crs != other.crs) return false

        return true
    }

    override fun hashCode(): Int {
        var result = data.contentHashCode()
        result = 31 * result + coverageId.hashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + crs.hashCode()
        return result
    }
}

@Serializable
data class WCSSoilData(
    val soc: Double?,           // Soil Organic Carbon (%)
    val som: Double?,           // Soil Organic Matter (%)
    val ph: Double?,            // pH level
    val sand: Double?,          // Sand percentage
    val silt: Double?,          // Silt percentage
    val clay: Double?,          // Clay percentage
    val bulkDensity: Double?,   // Bulk density
    val cec: Double?,           // Cation Exchange Capacity
    val location: LatLon,
    val timestamp: Long
)

@Serializable
data class SoilGridsLayer(
    val id: String,
    val name: String,
    val description: String,
    val property: SoilProperty,
    val depthRange: String,
    val quantile: String
)

enum class SoilProperty {
    SOC,
    SOM,
    PH,
    SAND,
    SILT,
    CLAY,
    BULK_DENSITY,
    CEC
}
