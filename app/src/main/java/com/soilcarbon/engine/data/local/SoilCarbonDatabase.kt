package com.soilcarbon.engine.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [SampleEntity::class, CalibrationEntity::class, CovariateCacheEntity::class],
    version = 1,
    exportSchema = true
)
abstract class SoilCarbonDatabase : RoomDatabase() {
    abstract fun sampleDao(): SampleDao
    abstract fun calibrationDao(): CalibrationDao
    abstract fun covariateCacheDao(): CovariateCacheDao

    companion object {
        const val NAME = "soilcarbon.db"
    }
}
