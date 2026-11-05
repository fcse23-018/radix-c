package com.soilcarbon.engine.data.sync

import com.soilcarbon.engine.domain.repository.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton

/** Narrow seam over WorkManager so the auto-sync policy can be unit-tested off-device. */
interface AutoSyncScheduler {
    fun schedulePeriodic()
    fun cancel()
}

/**
 * Applies the "allow network covariate lookup" + "automatic covariate refresh" settings to the
 * WorkManager schedule.
 *
 * Before this coordinator existed both toggles were persisted but the periodic worker was always
 * enqueued at startup. Now every settings change either enqueues or cancels the unique periodic
 * work, so turning automatic refresh off actually stops the background fetches.
 */
@Singleton
class AutoSyncCoordinator @Inject constructor(
    private val scheduler: AutoSyncScheduler,
    private val settingsRepository: SettingsRepository
) {

    /** Returns true when periodic work was scheduled, false when it was cancelled. */
    fun apply(allowNetworkLookup: Boolean, autoSync: Boolean): Boolean {
        val shouldRun = allowNetworkLookup && autoSync
        if (shouldRun) scheduler.schedulePeriodic() else scheduler.cancel()
        return shouldRun
    }

    suspend fun applyCurrent(): Boolean {
        val settings = settingsRepository.current()
        return apply(settings.allowNetworkSync, settings.autoSync)
    }
}
