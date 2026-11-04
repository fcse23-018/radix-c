package com.soilcarbon.engine.data.repository

import com.soilcarbon.engine.data.local.SampleDao
import com.soilcarbon.engine.data.local.SampleMapper
import com.soilcarbon.engine.domain.model.SampleStats
import com.soilcarbon.engine.domain.model.SoilSample
import com.soilcarbon.engine.domain.model.SyncState
import com.soilcarbon.engine.domain.repository.SampleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class DefaultSampleRepository @Inject constructor(
    private val dao: SampleDao
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

    private fun List<Double>.averageOrNull(): Double? =
        if (isEmpty()) null else average()
}
