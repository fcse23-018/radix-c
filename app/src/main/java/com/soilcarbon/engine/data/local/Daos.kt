package com.soilcarbon.engine.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SampleDao {
    @Query("SELECT * FROM samples ORDER BY createdAtMillis DESC")
    fun observeAll(): Flow<List<SampleEntity>>

    @Query("SELECT * FROM samples WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<SampleEntity?>

    @Query("SELECT * FROM samples WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): SampleEntity?

    @Query("SELECT * FROM samples ORDER BY createdAtMillis DESC")
    suspend fun getAll(): List<SampleEntity>

    @Query("SELECT * FROM samples WHERE syncState != 'SYNCED' ORDER BY createdAtMillis ASC")
    suspend fun getPendingSync(): List<SampleEntity>

    @Upsert
    suspend fun upsert(entity: SampleEntity)

    @Query("DELETE FROM samples WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT COUNT(*) FROM samples")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM samples WHERE createdAtMillis >= :sinceMillis")
    fun observeCountSince(sinceMillis: Long): Flow<Int>

    @Query("UPDATE samples SET syncState = :state WHERE id = :id")
    suspend fun setSyncState(id: String, state: String)

    @Query("UPDATE samples SET exportedAtMillis = :atMillis WHERE id IN (:ids)")
    suspend fun markExported(ids: List<String>, atMillis: Long)

    @Query("UPDATE samples SET syncedAtMillis = :atMillis, syncState = 'SYNCED' WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<String>, atMillis: Long)
}

@Dao
interface CovariateCacheDao {
    @Query("SELECT * FROM covariate_cache WHERE cellKey = :cellKey LIMIT 1")
    suspend fun get(cellKey: String): CovariateCacheEntity?

    @Upsert
    suspend fun upsert(entity: CovariateCacheEntity)

    @Query("DELETE FROM covariate_cache WHERE fetchedAtMillis < :cutoffMillis")
    suspend fun deleteOlderThan(cutoffMillis: Long)

    @Query("SELECT COUNT(*) FROM covariate_cache")
    suspend fun count(): Int

    @Query("SELECT * FROM covariate_cache ORDER BY fetchedAtMillis DESC")
    suspend fun all(): List<CovariateCacheEntity>
}

@Dao
interface CalibrationDao {
    @Query("SELECT * FROM calibration_profiles ORDER BY createdAtMillis DESC LIMIT 1")
    fun observeLatest(): Flow<CalibrationEntity?>

    @Query("SELECT * FROM calibration_profiles ORDER BY createdAtMillis DESC LIMIT 1")
    suspend fun latest(): CalibrationEntity?

    @Query("SELECT * FROM calibration_profiles ORDER BY createdAtMillis DESC")
    suspend fun all(): List<CalibrationEntity>

    @Upsert
    suspend fun upsert(entity: CalibrationEntity)

    @Query("DELETE FROM calibration_profiles")
    suspend fun clear()
}
