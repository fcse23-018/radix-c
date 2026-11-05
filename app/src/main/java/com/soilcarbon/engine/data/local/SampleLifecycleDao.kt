package com.soilcarbon.engine.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

/**
 * Lifecycle DAO: the sample row and its blob index are always written or removed together.
 *
 * Room's `@Transaction` on these abstract methods wraps the two statements in one SQLite
 * transaction, so a crash between them cannot leave a row without its blob index (or vice versa).
 */
@Dao
abstract class SampleLifecycleDao {

    @Query("SELECT * FROM sample_blobs WHERE sampleId = :id ORDER BY kind")
    abstract suspend fun blobsFor(id: String): List<SampleBlobEntity>

    @Query("SELECT * FROM sample_blobs ORDER BY sampleId, kind")
    abstract suspend fun allBlobs(): List<SampleBlobEntity>

    @Query("SELECT id FROM samples")
    abstract suspend fun allSampleIds(): List<String>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun insertSample(sample: SampleEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun insertBlobs(blobs: List<SampleBlobEntity>)

    @Query("DELETE FROM samples WHERE id = :id")
    abstract suspend fun deleteSample(id: String)

    @Query("DELETE FROM sample_blobs WHERE sampleId = :id")
    abstract suspend fun deleteBlobsFor(id: String)

    @Transaction
    open suspend fun insertWithBlobs(sample: SampleEntity, blobs: List<SampleBlobEntity>): SampleEntity {
        insertSample(sample)
        if (blobs.isNotEmpty()) insertBlobs(blobs)
        return sample
    }

    @Transaction
    open suspend fun deleteWithBlobs(id: String): List<SampleBlobEntity> {
        val blobs = blobsFor(id)
        deleteSample(id)
        deleteBlobsFor(id)
        return blobs
    }
}
