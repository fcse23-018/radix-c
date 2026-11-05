package com.soilcarbon.engine.data.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) : AutoSyncScheduler {

    override fun schedulePeriodic() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()
        val request = PeriodicWorkRequestBuilder<CovariateSyncWorker>(6, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            CovariateSyncWorker.UNIQUE_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun enqueueNow() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = OneTimeWorkRequestBuilder<CovariateSyncWorker>()
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            CovariateSyncWorker.UNIQUE_NAME + "-manual",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    override fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(CovariateSyncWorker.UNIQUE_NAME)
    }
}
