package com.soilcarbon.engine.domain.repository

import com.soilcarbon.engine.domain.model.AppSettings
import com.soilcarbon.engine.domain.model.UserRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** In-memory SettingsRepository for JVM wiring tests. */
class FakeSettingsRepository(
    initial: AppSettings = AppSettings()
) : SettingsRepository {

    val state = MutableStateFlow(initial)
    val networkPolicies = mutableListOf<Pair<Boolean, Boolean>>()

    override val settings: Flow<AppSettings> = state

    override suspend fun current(): AppSettings = state.value

    override suspend fun setRole(role: UserRole) {
        state.value = state.value.copy(role = role)
    }

    override suspend fun updateSoilDefaults(bulkDensityGcm3: Double, depthCm: Double) {
        state.value = state.value.copy(bulkDensityGcm3 = bulkDensityGcm3, depthCm = depthCm)
    }

    override suspend fun setOnboardingComplete(complete: Boolean) {
        state.value = state.value.copy(onboardingComplete = complete)
    }

    override suspend fun setGdprConsent(atMillis: Long?) {
        state.value = state.value.copy(gdprConsentAtMillis = atMillis)
    }

    override suspend fun setNetworkPolicy(allowNetwork: Boolean, autoSync: Boolean) {
        networkPolicies += allowNetwork to autoSync
        state.value = state.value.copy(allowNetworkSync = allowNetwork, autoSync = autoSync)
    }

    override suspend fun setCapturePolicy(requireRaw: Boolean, requireGps: Boolean, minLux: Float) {
        state.value = state.value.copy(
            requireRawCapture = requireRaw,
            requireGps = requireGps,
            minLux = minLux
        )
    }

    override suspend fun setGpuEnabled(enabled: Boolean) {
        state.value = state.value.copy(enableGpuDelegate = enabled)
    }

    override suspend fun setSyncResult(message: String, atMillis: Long) {
        state.value = state.value.copy(lastSyncMessage = message, lastSyncAtMillis = atMillis)
    }

    override suspend fun setSentinelToken(token: String) {
        state.value = state.value.copy(sentinelToken = token)
    }
}
