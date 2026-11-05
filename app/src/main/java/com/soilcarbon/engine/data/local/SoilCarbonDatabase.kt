package com.soilcarbon.engine.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [SampleEntity::class, SampleBlobEntity::class, CalibrationEntity::class, CovariateCacheEntity::class],
    version = 3,
    exportSchema = true
)
abstract class SoilCarbonDatabase : RoomDatabase() {
    abstract fun sampleDao(): SampleDao
    abstract fun sampleLifecycleDao(): SampleLifecycleDao
    abstract fun calibrationDao(): CalibrationDao
    abstract fun covariateCacheDao(): CovariateCacheDao

    companion object {
        const val NAME = "soilcarbon.db"
    }
}
