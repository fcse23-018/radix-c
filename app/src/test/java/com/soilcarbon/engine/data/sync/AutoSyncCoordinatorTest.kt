package com.soilcarbon.engine.data.sync

import com.google.common.truth.Truth.assertThat
import com.soilcarbon.engine.domain.model.AppSettings
import com.soilcarbon.engine.domain.repository.FakeSettingsRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AutoSyncCoordinatorTest {

    private val scheduler = RecordingScheduler()
    private val settings = FakeSettingsRepository()

    private fun coordinator() = AutoSyncCoordinator(scheduler, settings)

    @Test
    fun `startup schedules periodic work when lookup and auto refresh are enabled`() = runTest {
        settings.setNetworkPolicy(allowNetwork = true, autoSync = true)

        assertThat(coordinator().applyCurrent()).isTrue()
        assertThat(scheduler.scheduled).isEqualTo(1)
        assertThat(scheduler.cancelled).isEqualTo(0)
    }

    @Test
    fun `startup cancels periodic work when auto refresh is off`() = runTest {
        settings.setNetworkPolicy(allowNetwork = true, autoSync = false)

        assertThat(coordinator().applyCurrent()).isFalse()
        assertThat(scheduler.scheduled).isEqualTo(0)
        assertThat(scheduler.cancelled).isEqualTo(1)
    }

    @Test
    fun `offline mode cancels periodic work even when auto refresh is on`() = runTest {
        settings.setNetworkPolicy(allowNetwork = false, autoSync = true)

        assertThat(coordinator().applyCurrent()).isFalse()
        assertThat(scheduler.cancelled).isEqualTo(1)
    }

    @Test
    fun `turning auto refresh off after it was on cancels the worker`() = runTest {
        val coordinator = coordinator()

        assertThat(coordinator.apply(allowNetworkLookup = true, autoSync = true)).isTrue()
        assertThat(coordinator.apply(allowNetworkLookup = true, autoSync = false)).isFalse()

        assertThat(scheduler.scheduled).isEqualTo(1)
        assertThat(scheduler.cancelled).isEqualTo(1)
    }

    private class RecordingScheduler : AutoSyncScheduler {
        var scheduled = 0
        var cancelled = 0

        override fun schedulePeriodic() {
            scheduled++
        }

        override fun cancel() {
            cancelled++
        }
    }
}
