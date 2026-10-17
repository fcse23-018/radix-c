package com.terrochroma.shared.export

import com.terrochroma.shared.domain.model.SoilSample

interface ExportService {
    suspend fun exportToGeoJSON(samples: List<SoilSample>): Result<String>
    suspend fun exportToKML(samples: List<SoilSample>): Result<String>
    suspend fun exportToCSV(samples: List<SoilSample>): Result<String>
    suspend fun exportAllToGeoJSON(): Result<String>
    suspend fun exportAllToKML(): Result<String>
    suspend fun exportAllToCSV(): Result<String>
}

enum class ExportFormat {
    GEOJSON,
    KML,
    CSV
}
