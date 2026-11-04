package com.soilcarbon.engine.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.soilcarbon.engine.data.local.CovariateCacheDao
import com.soilcarbon.engine.data.local.CovariateCacheEntity
import com.soilcarbon.engine.data.remote.ClimateSnapshot
import com.soilcarbon.engine.data.remote.NdviSnapshot
import com.soilcarbon.engine.data.remote.OpenMeteoClient
import com.soilcarbon.engine.data.remote.SentinelNdviClient
import com.soilcarbon.engine.data.remote.SoilGridsClient
import com.soilcarbon.engine.data.remote.SoilGridsSnapshot
import com.soilcarbon.engine.domain.model.CovariateSource
import com.soilcarbon.engine.domain.model.CovariateValue
import com.soilcarbon.engine.domain.model.EnvironmentalCovariates
import com.soilcarbon.engine.domain.model.GeoPoint
import com.soilcarbon.engine.domain.model.SyncReport
import com.soilcarbon.engine.domain.repository.CovariateRepository
import com.soilcarbon.engine.domain.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlin.math.roundToLong
import javax.inject.Inject
import dagger.hilt.android.qualifiers.ApplicationContext

class DefaultCovariateRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val cacheDao: CovariateCacheDao,
    private val soilGridsClient: SoilGridsClient,
    private val openMeteoClient: OpenMeteoClient,
    private val sentinelNdviClient: SentinelNdviClient
) : CovariateRepository {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    override suspend fun getCovariates(
        point: GeoPoint,
        forceRefresh: Boolean
    ): EnvironmentalCovariates = withContext(Dispatchers.IO) {
        if (!point.isValid()) return@withContext EnvironmentalCovariates.defaults(point)
        val settings = settingsRepository.settings.first()
        val cellKey = cellKey(point)
        val cached = cacheDao.get(cellKey)
        val fresh = cached != null && System.currentTimeMillis() - cached.fetchedAtMillis < CACHE_TTL_MS
        if (cached != null && fresh && !forceRefresh && !cached.offlineFallback) {
            return@withContext cached.toDomain()
        }
        if (!settings.allowNetworkSync || !isOnline()) {
            return@withContext cached?.toDomain() ?: offlineDefaults(point)
        }

        coroutineScope {
            val soilDeferred = async { runCatching { soilGridsClient.fetch(point) }.getOrNull() }
            val climateDeferred = async { runCatching { openMeteoClient.fetchClimatology(point) }.getOrNull() }
            val ndviDeferred = async { runCatching { sentinelNdviClient.fetch(point) }.getOrNull() }

            val soil: SoilGridsSnapshot? = soilDeferred.await()
            val climate: ClimateSnapshot? = climateDeferred.await()
            val ndvi: NdviSnapshot? = ndviDeferred.await()

            val covariates = buildCovariates(point, cached, soil, climate, ndvi)
            if (soil != null || climate != null || ndvi != null) {
                cacheDao.upsert(covariates.toEntity(cellKey, point))
            }
            covariates
        }
    }

    override suspend fun syncPending(): SyncReport = withContext(Dispatchers.IO) {
        val cells = cacheDao.all()
        var succeeded = 0
        var failed = 0
        val now = System.currentTimeMillis()
        for (cell in cells) {
            if (now - cell.fetchedAtMillis < CACHE_TTL_MS && !cell.offlineFallback) {
                succeeded++
                continue
            }
            runCatching {
                getCovariates(
                    GeoPoint(cell.latitude, cell.longitude, provider = "cache-refresh"),
                    forceRefresh = true
                )
            }.onSuccess { succeeded++ }.onFailure { failed++ }
        }
        cacheDao.deleteOlderThan(now - CACHE_RETENTION_MS)
        SyncReport(
            attempted = cells.size,
            succeeded = succeeded,
            failed = failed,
            message = if (cells.isEmpty()) {
                "No cached locations to refresh."
            } else {
                "Covariate cache refresh: $succeeded ok, $failed failed."
            }
        )
    }

    private fun buildCovariates(
        point: GeoPoint,
        cached: CovariateCacheEntity?,
        soil: SoilGridsSnapshot?,
        climate: ClimateSnapshot?,
        ndvi: NdviSnapshot?
    ): EnvironmentalCovariates {
        val clay = soil?.clayPercent ?: cached?.clayPercent ?: 25.0
        val soc = soil?.socBaselinePercent ?: cached?.socBaselinePercent ?: 1.5
        val ndviValue = ndvi?.ndvi ?: cached?.ndvi ?: 0.35
        val precip = climate?.meanAnnualPrecipMm
            ?.takeIf { it > 0.0 }
            ?: cached?.precipMm
        val temp = climate?.meanAnnualTempC
            ?.takeIf { it.isFinite() }
            ?: cached?.tempC

        val claySource = if (soil != null) CovariateSource.SOILGRIDS_WCS
        else if (cached != null) CovariateSource.OFFLINE_CACHE else CovariateSource.REGIONAL_DEFAULT
        val socSource = if (soil != null) CovariateSource.SOILGRIDS_WCS
        else if (cached != null) CovariateSource.OFFLINE_CACHE else CovariateSource.REGIONAL_DEFAULT
        val ndviSource = if (ndvi != null) CovariateSource.SENTINEL2
        else if (cached != null) CovariateSource.OFFLINE_CACHE else CovariateSource.REGIONAL_DEFAULT
        val climateSource = if (climate != null) CovariateSource.OPEN_METEO_ERA5
        else CovariateSource.OFFLINE_CACHE

        return EnvironmentalCovariates(
            ndvi = CovariateValue(ndviValue, ndviSource, resolutionMeters = 10),
            clayPercent = CovariateValue(clay, claySource, resolutionMeters = 250),
            socBaselinePercent = CovariateValue(soc, socSource, resolutionMeters = 250),
            elevationMeters = point.altitudeMeters?.let {
                CovariateValue(it, CovariateSource.DERIVED)
            } ?: cached?.elevationMeters?.let {
                CovariateValue(it, CovariateSource.OFFLINE_CACHE)
            },
            meanAnnualPrecipMm = precip?.let {
                CovariateValue(it, climateSource, resolutionMeters = 1000)
            },
            meanAnnualTempC = temp?.let {
                CovariateValue(it, climateSource, resolutionMeters = 1000)
            },
            soilGridsFetchedAtMillis = System.currentTimeMillis(),
            isOfflineFallback = soil == null || ndvi == null
        )
    }

    private fun offlineDefaults(point: GeoPoint): EnvironmentalCovariates =
        EnvironmentalCovariates.defaults(point)

    private fun CovariateCacheEntity.toDomain(): EnvironmentalCovariates =
        EnvironmentalCovariates(
            ndvi = CovariateValue(ndvi, parseSource(ndviSource)),
            clayPercent = CovariateValue(clayPercent, parseSource(claySource)),
            socBaselinePercent = CovariateValue(socBaselinePercent, parseSource(socBaselineSource)),
            elevationMeters = elevationMeters?.let {
                CovariateValue(it, CovariateSource.DERIVED)
            },
            meanAnnualPrecipMm = precipMm?.let {
                CovariateValue(it, CovariateSource.OFFLINE_CACHE)
            },
            meanAnnualTempC = tempC?.let {
                CovariateValue(it, CovariateSource.OFFLINE_CACHE)
            },
            soilGridsFetchedAtMillis = fetchedAtMillis,
            isOfflineFallback = offlineFallback
        )

    private fun EnvironmentalCovariates.toEntity(
        cellKey: String,
        point: GeoPoint
    ): CovariateCacheEntity = CovariateCacheEntity(
        cellKey = cellKey,
        latitude = point.latitude,
        longitude = point.longitude,
        ndvi = ndvi.value,
        ndviSource = ndvi.source.name,
        clayPercent = clayPercent.value,
        claySource = clayPercent.source.name,
        socBaselinePercent = socBaselinePercent.value,
        socBaselineSource = socBaselinePercent.source.name,
        elevationMeters = elevationMeters?.value,
        precipMm = meanAnnualPrecipMm?.value,
        tempC = meanAnnualTempC?.value,
        fetchedAtMillis = System.currentTimeMillis(),
        offlineFallback = isOfflineFallback
    )

    private fun parseSource(name: String): CovariateSource =
        runCatching { CovariateSource.valueOf(name) }.getOrDefault(CovariateSource.OFFLINE_CACHE)

    private fun isOnline(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun cellKey(point: GeoPoint): String {
        val lat = (point.latitude * 1000).roundToLong() / 1000.0
        val lon = (point.longitude * 1000).roundToLong() / 1000.0
        return "$lat,$lon"
    }

    private companion object {
        const val CACHE_TTL_MS = 30L * 24 * 60 * 60 * 1000
        const val CACHE_RETENTION_MS = 365L * 24 * 60 * 60 * 1000
    }
}
