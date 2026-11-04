package com.soilcarbon.engine.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.soilcarbon.engine.domain.repository.CovariateRepository
import com.soilcarbon.engine.domain.repository.SampleRepository
import com.soilcarbon.engine.domain.repository.SettingsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class CovariateSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val sampleRepository: SampleRepository,
    private val covariateRepository: CovariateRepository,
    private val settingsRepository: SettingsRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val settings = settingsRepository.settings.first()
        if (!settings.allowNetworkSync) {
            return Result.success()
        }
        return runCatching {
            val samples = sampleRepository.observeSamples().first()
            var updated = 0
            for (sample in samples) {
                val location = sample.location ?: continue
                val needsRefresh = sample.covariates == null ||
                    sample.covariates.isOfflineFallback ||
                    sample.covariates.soilGridsFetchedAtMillis == null
                if (!needsRefresh) continue
                val covariates = covariateRepository.getCovariates(location)
                sampleRepository.upsert(sample.copy(covariates = covariates))
                updated++
            }
            val report = covariateRepository.syncPending()
            settingsRepository.setSyncResult(
                message = "${report.message} Samples enriched: $updated.",
                atMillis = System.currentTimeMillis()
            )
            Result.success()
        }.getOrElse { throwable ->
            settingsRepository.setSyncResult(
                message = "Sync failed: ${throwable.message ?: throwable.javaClass.simpleName}",
                atMillis = System.currentTimeMillis()
            )
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    companion object {
        const val UNIQUE_NAME = com.soilcarbon.engine.core.Constants.SYNC_WORK_NAME
    }
}
