package com.terrochroma.shared.data

import com.terrochroma.shared.database.DatabaseManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface DataDeletionService {
    suspend fun deleteAllUserData(): Result<Unit>
    suspend fun deleteSample(sampleId: String): Result<Unit>
    suspend fun deleteSamplesByDateRange(startTime: Long, endTime: Long): Result<Int>
    suspend fun deleteRawImages(): Result<Unit>
    suspend fun exportUserData(): Result<String>
}

class DataDeletionServiceImpl(
    private val databaseManager: DatabaseManager
) : DataDeletionService {

    override suspend fun deleteAllUserData(): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            // Delete all samples from database
            databaseManager.deleteAllSamples()
            
            // Clear analytics data (if using Firebase)
            // Firebase.analytics.resetAnalyticsData()
            
            // Clear cached data
            // clearImageCache()
            // clearNetworkCache()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteSample(sampleId: String): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            databaseManager.deleteSample(sampleId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteSamplesByDateRange(
        startTime: Long,
        endTime: Long
    ): Result<Int> = withContext(Dispatchers.IO) {
        return@withContext try {
            val samples = databaseManager.getSamplesByDateRange(startTime, endTime)
            var deletedCount = 0
            
            for (sample in samples) {
                databaseManager.deleteSample(sample.id)
                deletedCount++
            }
            
            Result.success(deletedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteRawImages(): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            // Get all samples
            val samples = databaseManager.getAllSamples()
            
            // In a real implementation, this would delete raw image files
            // from the file system while keeping the processed features
            // For now, this is a placeholder
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun exportUserData(): Result<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            val samples = databaseManager.getAllSamples()
            
            // Use the export service to create a comprehensive export
            // This would include all user data in a machine-readable format
            val exportService = com.terrochroma.shared.export.ExportServiceImpl(databaseManager)
            val result = exportService.exportToCSV(samples)
            
            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
