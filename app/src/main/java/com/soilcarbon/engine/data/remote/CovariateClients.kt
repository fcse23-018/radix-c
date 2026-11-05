package com.soilcarbon.engine.data.remote

import com.google.gson.JsonObject
import com.soilcarbon.engine.domain.model.GeoPoint
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

data class SoilGridsSnapshot(
    val clayPercent: Double,
    val socBaselinePercent: Double,
    val fetchedAtMillis: Long = System.currentTimeMillis()
)

data class ClimateSnapshot(
    val meanAnnualPrecipMm: Double,
    val meanAnnualTempC: Double,
    val fetchedAtMillis: Long = System.currentTimeMillis()
)

data class NdviSnapshot(
    val ndvi: Double,
    val fetchedAtMillis: Long = System.currentTimeMillis()
)

class SoilGridsClient(
    private val api: SoilGridsApi,
    private val rateLimiter: RateLimiter = RateLimiter(1_200)
) {
    suspend fun fetch(point: GeoPoint): SoilGridsSnapshot {
        rateLimiter.acquire()
        val response = api.query(
            longitude = point.longitude,
            latitude = point.latitude,
            property = listOf("clay", "soc"),
            depth = listOf("0-5cm", "5-15cm", "15-30cm")
        )
        var clay: Double? = null
        var soc: Double? = null
        val layers = response.getAsJsonObject("properties")?.getAsJsonArray("layers")
        layers?.forEach { element ->
            val layer = element.asJsonObject
            val name = layer.get("name")?.asString ?: return@forEach
            val depthValues = layer.getAsJsonArray("depths")?.mapNotNull { depthElement ->
                val depth = depthElement.asJsonObject
                depth.getAsJsonObject("values")?.get("mean")?.takeIf { !it.isJsonNull }?.asDouble
            } ?: emptyList()
            val mean = depthValues.takeIf { it.isNotEmpty() }?.average() ?: return@forEach
            when (name) {
                "clay" -> clay = (mean / 10.0).coerceIn(0.0, 90.0)
                "soc" -> soc = (mean / 100.0).coerceIn(0.05, 20.0)
            }
        }
        return SoilGridsSnapshot(
            clayPercent = clay ?: 25.0,
            socBaselinePercent = soc ?: 1.5
        )
    }
}

class OpenMeteoClient(
    private val api: OpenMeteoApi,
    private val rateLimiter: RateLimiter = RateLimiter(1_500)
) {
    suspend fun fetchClimatology(point: GeoPoint): ClimateSnapshot {
        rateLimiter.acquire()
        val end = LocalDate.now().minusDays(2)
        val start = end.minusYears(5)
        val formatter = DateTimeFormatter.ISO_LOCAL_DATE
        val response = api.archive(
            latitude = point.latitude,
            longitude = point.longitude,
            startDate = start.format(formatter),
            endDate = end.format(formatter)
        )
        val daily = response.getAsJsonObject("daily")
        val temps = daily?.getAsJsonArray("temperature_2m_mean")
            ?.mapNotNull { if (it.isJsonNull) null else it.asDouble }
            ?: emptyList()
        val precipitation = daily?.getAsJsonArray("precipitation_sum")
            ?.mapNotNull { if (it.isJsonNull) null else it.asDouble }
            ?: emptyList()
        val annualTemp = temps.takeIf { it.isNotEmpty() }?.average() ?: Double.NaN
        val annualPrecip = if (precipitation.isEmpty()) {
            Double.NaN
        } else {
            // Daily sums averaged over five years -> mm/year.
            precipitation.sum() / 5.0
        }
        return ClimateSnapshot(
            meanAnnualPrecipMm = annualPrecip.takeIf { it.isFinite() }
                ?.let { (it * 10).roundToInt() / 10.0 } ?: 0.0,
            meanAnnualTempC = annualTemp.takeIf { it.isFinite() }
                ?.let { (it * 10).roundToInt() / 10.0 } ?: 0.0
        )
    }
}

class SentinelNdviClient(
    private val api: SentinelStatisticsApi,
    private val tokenProvider: suspend () -> String,
    private val rateLimiter: RateLimiter = RateLimiter(2_000)
) {
    suspend fun fetch(point: GeoPoint): NdviSnapshot? {
        val token = tokenProvider().trim()
        if (token.isBlank()) return null
        rateLimiter.acquire()
        val delta = 0.0012
        val end = LocalDate.now().minusDays(3)
        val start = end.minusDays(45)
        val request = JsonObject().apply {
            add("input", JsonObject().apply {
                add("bounds", JsonObject().apply {
                    add("bbox", com.google.gson.JsonArray().apply {
                        add(point.longitude - delta)
                        add(point.latitude - delta)
                        add(point.longitude + delta)
                        add(point.latitude + delta)
                    })
                    addProperty("properties", "eo:cloud_cover")
                    add("data", com.google.gson.JsonArray().apply {
                        add(JsonObject().apply {
                            addProperty("type", "sentinel-2-l2a")
                            add("dataFilter", JsonObject().apply {
                                addProperty("maxCloudCoverage", 25)
                            })
                        })
                    })
                })
                add("data", com.google.gson.JsonArray())
            })
            add("aggregation", JsonObject().apply {
                addProperty("timeRange", "${start}T00:00:00Z/${end}T23:59:59Z")
                addProperty("aggregationInterval", "P30D")
                addProperty("width", 64)
                addProperty("height", 64)
                add("evalscript", com.google.gson.JsonPrimitive(EVALSCRIPT))
            })
            add("calculations", JsonObject().apply {
                add("B0", JsonObject().apply { addProperty("histograms", false) })
            })
        }
        val response = api.statistics("Bearer $token", request)
        val mean = response.getAsJsonArray("data")
            ?.firstOrNull()
            ?.asJsonObject
            ?.getAsJsonArray("outputs")
            ?.firstOrNull()
            ?.asJsonObject
            ?.getAsJsonObject("bands")
            ?.getAsJsonObject("B0")
            ?.getAsJsonObject("stats")
            ?.get("mean")
            ?.takeIf { !it.isJsonNull }
            ?.asDouble
            ?: return null
        return NdviSnapshot(ndvi = mean.coerceIn(-1.0, 1.0))
    }

    private companion object {
        const val EVALSCRIPT = """
            //VERSION=3
            function setup() {
              return {
                input: [{ bands: ["B04", "B08", "SCL", "dataMask"] }],
                output: [{ id: "B0", bands: 1, sampleType: "FLOAT32" }]
              };
            }
            function evaluatePixel(s) {
              let ndvi = (s.B08 - s.B04) / (s.B08 + s.B04);
              let valid = s.dataMask == 1 && s.SCL != 3 && s.SCL != 8 && s.SCL != 9 && s.SCL != 10;
              return valid ? [ndvi] : [NaN];
            }
        """
    }
}
