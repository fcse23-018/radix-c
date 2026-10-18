package com.terrochroma.shared.database

import app.cash.sqldelight.db.SqlDriver
import com.terrochroma.shared.database.TerroChromaDatabase
import com.terrochroma.shared.domain.model.SoilSample
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

expect class PlatformDatabaseDriverFactory {
    fun createDriver(): SqlDriver
    fun getEncryptionKey(): String
}

class DatabaseManager(private val driverFactory: PlatformDatabaseDriverFactory) {

    private val driver: SqlDriver by lazy {
        driverFactory.createDriver()
    }

    private val database: TerroChromaDatabase by lazy {
        TerroChromaDatabase(driver)
    }

    suspend fun getAllSamples(): List<SoilSample> = withContext(Dispatchers.IO) {
        database.soilSampleQueries.selectAllSamples()
            .executeAsList()
            .map { it.toDomainModel() }
    }

    suspend fun getSampleById(id: String): SoilSample? = withContext(Dispatchers.IO) {
        database.soilSampleQueries.selectSampleById(id)
            .executeAsOneOrNull()
            ?.toDomainModel()
    }

    suspend fun getSamplesByDateRange(startTime: Long, endTime: Long): List<SoilSample> =
        withContext(Dispatchers.IO) {
            database.soilSampleQueries.selectSamplesByDateRange(startTime, endTime)
                .executeAsList()
                .map { it.toDomainModel() }
        }

    suspend fun getUnsyncedSamples(): List<SoilSample> = withContext(Dispatchers.IO) {
        database.soilSampleQueries.selectUnsyncedSamples()
            .executeAsList()
            .map { it.toDomainModel() }
    }

    suspend fun insertSample(sample: SoilSample) = withContext(Dispatchers.IO) {
        database.soilSampleQueries.insertSample(
            id = sample.id,
            timestamp = sample.timestamp,
            latitude = sample.latitude,
            longitude = sample.longitude,
            altitude = sample.altitude,
            socPercentage = sample.socPercentage,
            confidenceInterval = sample.confidenceInterval,
            somPercentage = sample.somPercentage,
            labValue = sample.labValue,
            moistureContent = sample.moistureContent,
            imageHash = sample.imageHash,
            provenanceHash = sample.provenanceHash,
            cameraParams = Json.encodeToString(sample.cameraParams),
            colorFeatures = Json.encodeToString(sample.colorFeatures),
            textureFeatures = Json.encodeToString(sample.textureFeatures),
            modelVersion = sample.modelVersion,
            appVersion = sample.appVersion,
            isSynced = if (sample.isSynced) 1L else 0L,
            notes = sample.notes
        )
    }

    suspend fun updateSampleSyncStatus(id: String) = withContext(Dispatchers.IO) {
        database.soilSampleQueries.updateSampleSyncStatus(id)
    }

    suspend fun deleteSample(id: String) = withContext(Dispatchers.IO) {
        database.soilSampleQueries.deleteSample(id)
    }

    suspend fun deleteAllSamples() = withContext(Dispatchers.IO) {
        database.soilSampleQueries.deleteAllSamples()
    }

    suspend fun close() = withContext(Dispatchers.IO) {
        driver.close()
    }
}

private fun com.terrochroma.shared.SoilSample.toDomainModel(): SoilSample {
    return SoilSample(
        id = id,
        timestamp = timestamp,
        latitude = latitude,
        longitude = longitude,
        altitude = altitude,
        socPercentage = socPercentage,
        confidenceInterval = confidenceInterval,
        somPercentage = somPercentage,
        labValue = labValue,
        moistureContent = moistureContent,
        imageHash = imageHash,
        provenanceHash = provenanceHash,
        cameraParams = Json.decodeFromString(cameraParams),
        colorFeatures = Json.decodeFromString(colorFeatures),
        textureFeatures = Json.decodeFromString(textureFeatures),
        modelVersion = modelVersion,
        appVersion = appVersion,
        isSynced = isSynced == 1L,
        notes = notes
    )
}
