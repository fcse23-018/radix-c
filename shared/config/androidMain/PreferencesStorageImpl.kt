package com.terrochroma.shared.config

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.terrochroma.shared.config.ConsentPreferences
import com.terrochroma.shared.config.DataRetentionSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

actual class PreferencesStorage(private val context: Context) {

    private val masterKeyAlias = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            "terrochroma_encrypted_prefs",
            masterKeyAlias,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    override suspend fun saveConsentPreferences(preferences: ConsentPreferences): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            sharedPreferences.edit().apply {
                putBoolean("location_enabled", preferences.locationEnabled)
                putBoolean("analytics_enabled", preferences.analyticsEnabled)
                putBoolean("crash_reporting_enabled", preferences.crashReportingEnabled)
                putBoolean("privacy_policy_accepted", preferences.privacyPolicyAccepted)
                if (preferences.privacyPolicyAcceptedDate != null) {
                    putLong("privacy_policy_accepted_date", preferences.privacyPolicyAcceptedDate)
                } else {
                    remove("privacy_policy_accepted_date")
                }
                apply()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getConsentPreferences(): Result<ConsentPreferences> = withContext(Dispatchers.IO) {
        return@withContext try {
            val preferences = ConsentPreferences(
                locationEnabled = sharedPreferences.getBoolean("location_enabled", true),
                analyticsEnabled = sharedPreferences.getBoolean("analytics_enabled", false),
                crashReportingEnabled = sharedPreferences.getBoolean("crash_reporting_enabled", true),
                privacyPolicyAccepted = sharedPreferences.getBoolean("privacy_policy_accepted", false),
                privacyPolicyAcceptedDate = if (sharedPreferences.contains("privacy_policy_accepted_date")) {
                    sharedPreferences.getLong("privacy_policy_accepted_date", 0)
                } else {
                    null
                }
            )
            Result.success(preferences)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveDataRetentionSettings(settings: DataRetentionSettings): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            sharedPreferences.edit().apply {
                putBoolean("auto_delete_enabled", settings.autoDeleteEnabled)
                putInt("auto_delete_months", settings.autoDeleteMonths)
                putBoolean("delete_raw_images", settings.deleteRawImagesAfterProcessing)
                apply()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getDataRetentionSettings(): Result<DataRetentionSettings> = withContext(Dispatchers.IO) {
        return@withContext try {
            val settings = DataRetentionSettings(
                autoDeleteEnabled = sharedPreferences.getBoolean("auto_delete_enabled", false),
                autoDeleteMonths = sharedPreferences.getInt("auto_delete_months", 12),
                deleteRawImagesAfterProcessing = sharedPreferences.getBoolean("delete_raw_images", false)
            )
            Result.success(settings)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun clearAllPreferences(): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            sharedPreferences.edit().clear().apply()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun isOnboardingCompleted(): Result<Boolean> = withContext(Dispatchers.IO) {
        return@withContext try {
            val completed = sharedPreferences.getBoolean("onboarding_completed", false)
            Result.success(completed)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun setOnboardingCompleted(): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            sharedPreferences.edit()
                .putBoolean("onboarding_completed", true)
                .putLong("onboarding_completed_date", System.currentTimeMillis())
                .apply()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
