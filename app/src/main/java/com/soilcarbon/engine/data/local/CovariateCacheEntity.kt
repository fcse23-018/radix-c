package com.soilcarbon.engine.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "covariate_cache")
data class CovariateCacheEntity(
    @PrimaryKey val cellKey: String,
    val latitude: Double,
    val longitude: Double,
    val ndvi: Double,
    val ndviSource: String,
    val clayPercent: Double,
    val claySource: String,
    val socBaselinePercent: Double,
    val socBaselineSource: String,
    val elevationMeters: Double?,
    val precipMm: Double?,
    val tempC: Double?,
    val fetchedAtMillis: Long,
    val offlineFallback: Boolean
)
