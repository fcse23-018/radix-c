package com.soilcarbon.engine.domain.repository

import com.soilcarbon.engine.domain.model.AppSettings
import com.soilcarbon.engine.domain.model.CalibrationProfile
import com.soilcarbon.engine.domain.model.EnvironmentalCovariates
import com.soilcarbon.engine.domain.model.GeoPoint
import com.soilcarbon.engine.domain.model.ModelInfo
import com.soilcarbon.engine.domain.model.SampleStats
import com.soilcarbon.engine.domain.model.SoilSample
import com.soilcarbon.engine.domain.model.SyncState
import com.soilcarbon.engine.domain.model.UserRole
import kotlinx.coroutines.flow.Flow

interface SampleRepository {
    fun observeSamples(): Flow<List<SoilSample>>
    fun observeSample(id: String): Flow<SoilSample?>
    fun observeStats(): Flow<SampleStats>
    suspend fun getSample(id: String): SoilSample?
    suspend fun upsert(sample: SoilSample)
    suspend fun delete(id: String)
    suspend fun count(): Int
    suspend fun setSyncState(id: String, state: SyncState)
    suspend fun markExported(ids: List<String>, atMillis: Long)
}

interface CalibrationRepository {
    fun observeLatest(): Flow<CalibrationProfile?>
    suspend fun latest(): CalibrationProfile?
    suspend fun save(profile: CalibrationProfile)
    suspend fun clear()
}

interface SettingsRepository {
    val settings: Flow<AppSettings>
    suspend fun current(): AppSettings
    suspend fun setRole(role: UserRole)
    suspend fun updateSoilDefaults(bulkDensityGcm3: Double, depthCm: Double)
    suspend fun setOnboardingComplete(complete: Boolean)
    suspend fun setGdprConsent(atMillis: Long?)
    suspend fun setNetworkPolicy(allowNetwork: Boolean, autoSync: Boolean)
    suspend fun setCapturePolicy(requireRaw: Boolean, requireGps: Boolean, minLux: Float)
    suspend fun setGpuEnabled(enabled: Boolean)
    suspend fun setSyncResult(message: String, atMillis: Long)
}

interface CovariateRepository {
    suspend fun getCovariates(point: GeoPoint, forceRefresh: Boolean = false): EnvironmentalCovariates
    suspend fun syncPending(): com.soilcarbon.engine.domain.model.SyncReport
}

interface ModelRepository {
    suspend fun loadModel(): ModelInfo
    suspend fun metadata(): com.soilcarbon.engine.domain.model.ModelMetadata
}

interface ExportRepository {
    suspend fun exportCsv(samples: List<SoilSample>, includeHeader: Boolean = true): String
    suspend fun exportGeoJson(samples: List<SoilSample>): String
    suspend fun exportBundle(samples: List<SoilSample>): String
    suspend fun exportProvenance(samples: List<SoilSample>): String
}

object RepositoryDefaults {
    val emptyCovariates: EnvironmentalCovariates
        get() = EnvironmentalCovariates.defaults()
}
