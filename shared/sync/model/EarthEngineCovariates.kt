package com.terrochroma.shared.sync.model

import kotlinx.serialization.Serializable

@Serializable
data class EarthEngineCovariates(
    val ndvi: Double?,           // Normalized Difference Vegetation Index
    val precipitation: Double?, // Precipitation in mm
    val temperature: Double?,  // Temperature in Celsius
    val elevation: Double?,    // Elevation in meters
    val location: LatLon,
    val timestamp: Long
)
