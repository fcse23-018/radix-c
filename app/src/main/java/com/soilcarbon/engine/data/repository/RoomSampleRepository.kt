package com.soilcarbon.engine.data.repository

import com.soilcarbon.engine.data.local.SampleBlobEntity
import com.soilcarbon.engine.data.local.SampleDao
import com.soilcarbon.engine.data.local.SampleEntity
import com.soilcarbon.engine.data.local.SampleLifecycleDao
import com.soilcarbon.engine.data.local.SampleMapper
import com.soilcarbon.engine.domain.model.SampleStats
import com.soilcarbon.engine.domain.model.SoilSample
import com.soilcarbon.engine.domain.model.StoredBlob
import com.soilcarbon.engine.domain.model.SyncState
import com.soilcarbon.engine.domain.repository.SampleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RoomSampleRepository @Inject constructor(
    private val dao: SampleDao,
    private val lifecycleDao: SampleLifecycleDao
) : SampleRepository {

    override fun observeSamples(): Flow<List<SoilSample>> =
        dao.observeAll().map { entities -> entities.map(SampleMapper::toDomain) }

    override fun observeSample(id: String): Flow<SoilSample?> =
        dao.observeById(id).map { it?.let(SampleMapper::toDomain) }

    override fun observeStats(): Flow<SampleStats> =
        dao.observeAll().map { entities ->
            val samples = entities.map(SampleMapper::toDomain)
            val analyzed = samples.mapNotNull { it.prediction }
            SampleStats(
                total = samples.size,
                analyzed = analyzed.size,
                pendingSync = samples.count { it.syncState != SyncState.SYNCED },
                meanSocPercent = analyzed.map { it.socPercent }.averageOrNull(),
                meanSomPercent = analyzed.map { it.somPercent }.averageOrNull(),
                meanUncertainty = analyzed.map { it.uncertainty95 }.averageOrNull(),
                meanConfidence = analyzed.map { it.confidenceScore }.averageOrNull(),
                totalCarbonMgPerHa = analyzed.mapNotNull { it.carbonStockMgPerHa }.averageOrNull()
            )
        }

    override suspend fun getSample(id: String): SoilSample? =
        dao.getById(id)?.let(SampleMapper::toDomain)

    override suspend fun upsert(sample: SoilSample) {
        dao.upsert(SampleMapper.toEntity(sample))
    }

    override suspend fun delete(id: String) {
        dao.delete(id)
    }

    override suspend fun count(): Int = dao.count()

    override suspend fun setSyncState(id: String, state: SyncState) {
        dao.setSyncState(id, state.name)
    }

    override suspend fun markExported(ids: List<String>, atMillis: Long) {
        if (ids.isNotEmpty()) dao.markExported(ids, atMillis)
    }

    override suspend fun blobsFor(id: String): List<StoredBlob>? {
        val entity = dao.getById(id) ?: return null
        val indexed = lifecycleDao.blobsFor(id)
        // Rows created before the blob index existed still carry their paths on the sample row.
        if (indexed.isEmpty()) return entity.legacyBlobs()
        return indexed.map { it.toDomain() }
    }

    override suspend fun insertWithBlobs(sample: SoilSample, blobs: List<StoredBlob>) {
        lifecycleDao.insertWithBlobs(
            sample = SampleMapper.toEntity(sample),
            blobs = blobs.map { it.toEntity(sample.id) }
        )
    }

    override suspend fun deleteWithBlobs(id: String): List<StoredBlob> =
        lifecycleDao.deleteWithBlobs(id).map { it.toDomain() }

    override suspend fun allIds(): Set<String> = lifecycleDao.allSampleIds().toSet()

    private fun SampleBlobEntity.toDomain(): StoredBlob = StoredBlob(
        sampleId = sampleId,
        kind = kind,
        relativePath = relativePath,
        sha256 = sha256,
        sizeBytes = sizeBytes
    )

    private fun StoredBlob.toEntity(sampleId: String): SampleBlobEntity = SampleBlobEntity(
        sampleId = sampleId,
        kind = kind,
        relativePath = relativePath,
        sha256 = sha256,
        sizeBytes = sizeBytes
    )

    private fun SampleEntity.legacyBlobs(): List<StoredBlob> =
        buildList {
            jpegImagePath?.let {
                add(StoredBlob(id, "jpeg", it, jpegSha256.orEmpty(), sizeBytes = 0))
            }
            rawImagePath?.let {
                add(StoredBlob(id, "raw", it, rawSha256.orEmpty(), sizeBytes = 0))
            }
            apertureCropPath?.let {
                add(StoredBlob(id, "aperture", it, sha256 = "", sizeBytes = 0))
            }
        }

    private fun List<Double>.averageOrNull(): Double? =
        if (isEmpty()) null else average()
}
