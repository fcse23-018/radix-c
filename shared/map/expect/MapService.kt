package com.terrochroma.shared.map

import com.terrochroma.shared.domain.model.SoilSample

interface MapService {
    suspend fun initialize(): Result<Unit>
    suspend fun addSamplePin(sample: SoilSample): Result<Unit>
    suspend fun removeSamplePin(sampleId: String): Result<Unit>
    suspend fun clearAllPins(): Result<Unit>
    suspend fun centerOnLocation(latitude: Double, longitude: Double, zoom: Double): Result<Unit>
    fun getMapController(): Any
}
