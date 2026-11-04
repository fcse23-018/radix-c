package com.soilcarbon.engine.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.soilcarbon.engine.domain.model.AppSettings
import com.soilcarbon.engine.domain.model.UserRole
import com.soilcarbon.engine.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import dagger.hilt.android.qualifiers.ApplicationContext

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "soilcarbon_settings"
)

class DefaultSettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : SettingsRepository {

    override val settings: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        AppSettings(
            role = UserRole.fromName(prefs[Keys.ROLE]),
            bulkDensityGcm3 = prefs[Keys.BULK_DENSITY] ?: 1.30,
            depthCm = prefs[Keys.DEPTH_CM] ?: 30.0,
            requireRawCapture = prefs[Keys.REQUIRE_RAW] ?: true,
            requireGps = prefs[Keys.REQUIRE_GPS] ?: true,
            minLux = prefs[Keys.MIN_LUX] ?: 50f,
            maxTiltDegrees = prefs[Keys.MAX_TILT] ?: 10f,
            enableGpuDelegate = prefs[Keys.ENABLE_GPU] ?: true,
            allowNetworkSync = prefs[Keys.ALLOW_NETWORK] ?: true,
            autoSync = prefs[Keys.AUTO_SYNC] ?: true,
            lastSyncAtMillis = prefs[Keys.LAST_SYNC_AT],
            lastSyncMessage = prefs[Keys.LAST_SYNC_MESSAGE],
            onboardingComplete = prefs[Keys.ONBOARDING] ?: false,
            gdprConsentAtMillis = prefs[Keys.GDPR_CONSENT_AT],
            apiBaseUrl = prefs[Keys.API_BASE_URL] ?: "https://rest.isric.org/",
            modelSourceUrl = prefs[Keys.MODEL_SOURCE_URL] ?: ""
        )
    }

    override suspend fun current(): AppSettings = settings.first()

    override suspend fun setRole(role: UserRole) {
        context.settingsDataStore.edit { it[Keys.ROLE] = role.name }
    }

    override suspend fun updateSoilDefaults(bulkDensityGcm3: Double, depthCm: Double) {
        context.settingsDataStore.edit {
            it[Keys.BULK_DENSITY] = bulkDensityGcm3.coerceIn(0.5, 2.5)
            it[Keys.DEPTH_CM] = depthCm.coerceIn(1.0, 200.0)
        }
    }

    override suspend fun setOnboardingComplete(complete: Boolean) {
        context.settingsDataStore.edit { it[Keys.ONBOARDING] = complete }
    }

    override suspend fun setGdprConsent(atMillis: Long?) {
        context.settingsDataStore.edit {
            if (atMillis == null) it.remove(Keys.GDPR_CONSENT_AT) else it[Keys.GDPR_CONSENT_AT] = atMillis
        }
    }

    override suspend fun setNetworkPolicy(allowNetwork: Boolean, autoSync: Boolean) {
        context.settingsDataStore.edit {
            it[Keys.ALLOW_NETWORK] = allowNetwork
            it[Keys.AUTO_SYNC] = autoSync
        }
    }

    override suspend fun setCapturePolicy(requireRaw: Boolean, requireGps: Boolean, minLux: Float) {
        context.settingsDataStore.edit {
            it[Keys.REQUIRE_RAW] = requireRaw
            it[Keys.REQUIRE_GPS] = requireGps
            it[Keys.MIN_LUX] = minLux.coerceIn(10f, 1000f)
        }
    }

    override suspend fun setGpuEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.ENABLE_GPU] = enabled }
    }

    override suspend fun setSyncResult(message: String, atMillis: Long) {
        context.settingsDataStore.edit {
            it[Keys.LAST_SYNC_MESSAGE] = message
            it[Keys.LAST_SYNC_AT] = atMillis
        }
    }

    private object Keys {
        val ROLE = stringPreferencesKey("role")
        val BULK_DENSITY = doublePreferencesKey("bulk_density")
        val DEPTH_CM = doublePreferencesKey("depth_cm")
        val REQUIRE_RAW = booleanPreferencesKey("require_raw")
        val REQUIRE_GPS = booleanPreferencesKey("require_gps")
        val MIN_LUX = floatPreferencesKey("min_lux")
        val MAX_TILT = floatPreferencesKey("max_tilt")
        val ENABLE_GPU = booleanPreferencesKey("enable_gpu")
        val ALLOW_NETWORK = booleanPreferencesKey("allow_network")
        val AUTO_SYNC = booleanPreferencesKey("auto_sync")
        val LAST_SYNC_AT = longPreferencesKey("last_sync_at")
        val LAST_SYNC_MESSAGE = stringPreferencesKey("last_sync_message")
        val ONBOARDING = booleanPreferencesKey("onboarding")
        val GDPR_CONSENT_AT = longPreferencesKey("gdpr_consent_at")
        val API_BASE_URL = stringPreferencesKey("api_base_url")
        val MODEL_SOURCE_URL = stringPreferencesKey("model_source_url")
    }
}
