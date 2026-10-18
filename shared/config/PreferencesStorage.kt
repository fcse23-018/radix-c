package com.terrochroma.shared.config

import kotlinx.coroutines.flow.Flow

data class ConsentPreferences(
    val locationEnabled: Boolean = true,
    val analyticsEnabled: Boolean = false,
    val crashReportingEnabled: Boolean = true,
    val privacyPolicyAccepted: Boolean = false,
    val privacyPolicyAcceptedDate: Long? = null
)

data class DataRetentionSettings(
    val autoDeleteEnabled: Boolean = false,
    val autoDeleteMonths: Int = 12,
    val deleteRawImagesAfterProcessing: Boolean = false
)

expect class PreferencesStorage {
    suspend fun saveConsentPreferences(preferences: ConsentPreferences): Result<Unit>
    suspend fun getConsentPreferences(): Result<ConsentPreferences>
    suspend fun saveDataRetentionSettings(settings: DataRetentionSettings): Result<Unit>
    suspend fun getDataRetentionSettings(): Result<DataRetentionSettings>
    suspend fun clearAllPreferences(): Result<Unit>
    suspend fun isOnboardingCompleted(): Result<Boolean>
    suspend fun setOnboardingCompleted(): Result<Unit>
}
