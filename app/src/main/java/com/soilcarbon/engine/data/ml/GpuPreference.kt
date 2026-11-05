package com.soilcarbon.engine.data.ml

import com.soilcarbon.engine.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cached view of the Settings "GPU delegate" toggle.
 *
 * [TfliteSocModel] runs synchronously on an inference thread and cannot suspend to read
 * DataStore, so a small collector keeps this flag in sync. Toggling the setting changes the
 * value, which makes the model close and rebuild its interpreter on the next prediction.
 */
@Singleton
class GpuPreference @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    private val cached = AtomicBoolean(true)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        scope.launch {
            settingsRepository.settings.collect { cached.set(it.enableGpuDelegate) }
        }
    }

    fun enabled(): Boolean = cached.get()

    /** Test/startup hook: read the persisted value immediately. */
    suspend fun refreshFromSettings() {
        cached.set(settingsRepository.current().enableGpuDelegate)
    }
}
