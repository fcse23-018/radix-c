package com.soilcarbon.engine

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.soilcarbon.engine.data.sync.SyncScheduler
import com.soilcarbon.engine.domain.usecase.SampleService
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.opencv.android.OpenCVLoader
import javax.inject.Inject

@HiltAndroidApp
class SoilCarbonApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var syncScheduler: SyncScheduler

    @Inject
    lateinit var sampleService: SampleService

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(if (BuildConfig.DEBUG) android.util.Log.DEBUG else android.util.Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        runCatching { System.loadLibrary("sqlcipher") }
        runCatching { OpenCVLoader.initLocal() }
        runCatching { syncScheduler.schedulePeriodic() }
        // Once per process, before any capture can run: remove legacy orphan blobs.
        appScope.launch {
            runCatching { sampleService.sweepOrphans() }
                .onSuccess { count ->
                    if (count > 0) {
                        android.util.Log.i(TAG, "Removed $count orphaned encrypted blob(s) at startup")
                    }
                }
                .onFailure { error ->
                    android.util.Log.w(TAG, "Startup orphan sweep failed", error)
                }
        }
    }

    private companion object {
        const val TAG = "SoilCarbonApplication"
    }
}
