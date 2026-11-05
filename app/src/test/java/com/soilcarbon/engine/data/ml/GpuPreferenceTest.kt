package com.soilcarbon.engine.data.ml

import com.google.common.truth.Truth.assertThat
import com.soilcarbon.engine.domain.model.AppSettings
import com.soilcarbon.engine.domain.repository.FakeSettingsRepository
import kotlinx.coroutines.runBlocking
import org.junit.Test

/**
 * Proves the Settings GPU toggle reaches the delegate chain used by [TfliteSocModel], rather
 * than only being persisted.
 */
class GpuPreferenceTest {

    @Test
    fun `disabling the GPU setting rebuilds the chain without GPU`() = runBlocking {
        val settings = FakeSettingsRepository(AppSettings(enableGpuDelegate = false))
        val preference = GpuPreference(settings)

        preference.refreshFromSettings()

        assertThat(preference.enabled()).isFalse()
        assertThat(delegateChain(preference.enabled())).containsExactly(
            DelegateKind.NNAPI,
            DelegateKind.CPU,
        ).inOrder()
    }

    @Test
    fun `enabling the GPU setting puts GPU first`() = runBlocking {
        val settings = FakeSettingsRepository(AppSettings(enableGpuDelegate = true))
        val preference = GpuPreference(settings)

        preference.refreshFromSettings()

        assertThat(preference.enabled()).isTrue()
        assertThat(delegateChain(preference.enabled())).containsExactly(
            DelegateKind.GPU,
            DelegateKind.NNAPI,
            DelegateKind.CPU,
        ).inOrder()
    }

    @Test
    fun `changing the setting changes the preferred chain on the next refresh`() = runBlocking {
        val settings = FakeSettingsRepository(AppSettings(enableGpuDelegate = true))
        val preference = GpuPreference(settings)
        preference.refreshFromSettings()

        settings.setGpuEnabled(false)
        preference.refreshFromSettings()

        assertThat(delegateChain(preference.enabled())).doesNotContain(DelegateKind.GPU)
    }
}
