package com.soilcarbon.engine.data.repository

import com.soilcarbon.engine.data.local.CalibrationDao
import com.soilcarbon.engine.data.local.CalibrationMapper
import com.soilcarbon.engine.domain.model.CalibrationProfile
import com.soilcarbon.engine.domain.repository.CalibrationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class DefaultCalibrationRepository @Inject constructor(
    private val dao: CalibrationDao
) : CalibrationRepository {

    override fun observeLatest(): Flow<CalibrationProfile?> =
        dao.observeLatest().map { it?.let(CalibrationMapper::toDomain) }

    override suspend fun latest(): CalibrationProfile? =
        dao.latest()?.let(CalibrationMapper::toDomain)

    override suspend fun save(profile: CalibrationProfile) {
        val digest = profile.sha256.ifBlank { CalibrationMapper.digest(profile) }
        dao.upsert(CalibrationMapper.toEntity(profile.copy(sha256 = digest)))
    }

    override suspend fun clear() {
        dao.clear()
    }
}
