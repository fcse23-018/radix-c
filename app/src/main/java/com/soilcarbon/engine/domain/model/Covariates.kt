package com.soilcarbon.engine.domain.model

data class CovariateValue<T>(
    val value: T,
    val source: CovariateSource,
    val fetchedAtMillis: Long = System.currentTimeMillis(),
    val resolutionMeters: Int? = null,
    val depthInterval: String? = null
)

enum class CovariateSource {
    OFFLINE_CACHE,
    SOILGRIDS_WCS,
    SOILGRIDS_REST,
    SENTINEL2,
    EARTH_ENGINE,
    OPEN_METEO_ERA5,
    REGIONAL_DEFAULT,
    DERIVED
}

data class EnvironmentalCovariates(
    val ndvi: CovariateValue<Double>,
    val clayPercent: CovariateValue<Double>,
    val socBaselinePercent: CovariateValue<Double>,
    val elevationMeters: CovariateValue<Double>?,
    val meanAnnualPrecipMm: CovariateValue<Double>?,
    val meanAnnualTempC: CovariateValue<Double>?,
    val soilGridsFetchedAtMillis: Long? = null,
    val isOfflineFallback: Boolean = true
) {
    fun toExportMap(): Map<String, Any?> = mapOf(
        "ndvi" to ndvi.value,
        "ndvi_source" to ndvi.source.name,
        "clay_percent" to clayPercent.value,
        "clay_source" to clayPercent.source.name,
        "soc_baseline_percent" to socBaselinePercent.value,
        "soc_baseline_source" to socBaselinePercent.source.name,
        "elevation_m" to elevationMeters?.value,
        "precip_mm" to meanAnnualPrecipMm?.value,
        "temp_c" to meanAnnualTempC?.value,
        "offline_fallback" to isOfflineFallback
    )

    companion object {
        fun defaults(location: GeoPoint? = null): EnvironmentalCovariates = EnvironmentalCovariates(
            ndvi = CovariateValue(0.35, CovariateSource.REGIONAL_DEFAULT, resolutionMeters = 250),
            clayPercent = CovariateValue(25.0, CovariateSource.REGIONAL_DEFAULT, resolutionMeters = 250),
            socBaselinePercent = CovariateValue(1.5, CovariateSource.REGIONAL_DEFAULT, resolutionMeters = 250),
            elevationMeters = location?.altitudeMeters?.let {
                CovariateValue(it, CovariateSource.DERIVED)
            },
            meanAnnualPrecipMm = null,
            meanAnnualTempC = null,
            isOfflineFallback = true
        )
    }
}

data class ModelMetadata(
    val modelId: String,
    val modelVersion: String,
    val description: String,
    val featureOrder: List<String>,
    val trainingDataset: String,
    val validationRmsePercent: Double,
    val validationR2: Double,
    val uncertaintyMode: String,
    val featureMeans: List<Double>,
    val featureScales: List<Double>,
    val outputScale: Double = 1.0,
    val outputOffset: Double = 0.0,
    val isPlaceholder: Boolean = true,
    val createdAtUtc: String = "",
    val sha256: String = ""
) {
    init {
        require(featureOrder.size == 8) { "Model metadata must declare 8 features" }
        require(featureMeans.size == 8) { "Model metadata must declare 8 feature means" }
        require(featureScales.size == 8) { "Model metadata must declare 8 feature scales" }
    }

    companion object {
        const val FALLBACK_MODEL_ID = "unavailable"
        const val FALLBACK_MODEL_VERSION = "0.0.0"

        fun fallback(): ModelMetadata = ModelMetadata(
            modelId = FALLBACK_MODEL_ID,
            modelVersion = FALLBACK_MODEL_VERSION,
            description = "No model asset or sidecar could be loaded. SOC inference is " +
                "unavailable; captures are rejected rather than estimated by a fallback.",
            featureOrder = FeatureVector.FEATURE_NAMES,
            trainingDataset = "None — no model loaded",
            validationRmsePercent = 0.0,
            validationR2 = 0.0,
            uncertaintyMode = "unavailable",
            featureMeans = listOf(42.0, 6.0, 14.0, 5.0, 0.35, 0.25, 0.35, 25.0),
            featureScales = listOf(18.0, 11.0, 14.0, 3.0, 0.30, 0.20, 0.25, 12.0),
            isPlaceholder = true,
            createdAtUtc = "2026-10-01T00:00:00Z"
        )
    }
}
