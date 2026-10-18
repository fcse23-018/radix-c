package com.terrochroma.shared.config

import com.terrochroma.shared.config.ConsentPreferences
import com.terrochroma.shared.config.DataRetentionSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.*

actual class PreferencesStorage {

    private val userDefaults = NSUserDefaults.standardUserDefaults

    override suspend fun saveConsentPreferences(preferences: ConsentPreferences): Result<Unit> = withContext(Dispatchers.Main) {
        return@withContext try {
            userDefaults.setBool(preferences.locationEnabled, forKey = "location_enabled")
            userDefaults.setBool(preferences.analyticsEnabled, forKey = "analytics_enabled")
            userDefaults.setBool(preferences.crashReportingEnabled, forKey = "crash_reporting_enabled")
            userDefaults.setBool(preferences.privacyPolicyAccepted, forKey = "privacy_policy_accepted")
            preferences.privacyPolicyAcceptedDate?.let {
                userDefaults.setObject(it, forKey = "privacy_policy_accepted_date")
            } ?: run {
                userDefaults.removeObject(forKey = "privacy_policy_accepted_date")
            }
            userDefaults.synchronize()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getConsentPreferences(): Result<ConsentPreferences> = withContext(Dispatchers.Main) {
        return@withContext try {
            val preferences = ConsentPreferences(
                locationEnabled = userDefaults.boolForKey("location_enabled", true),
                analyticsEnabled = userDefaults.boolForKey("analytics_enabled", false),
                crashReportingEnabled = userDefaults.boolForKey("crash_reporting_enabled", true),
                privacyPolicyAccepted = userDefaults.boolForKey("privacy_policy_accepted", false),
                privacyPolicyAcceptedDate = userDefaults.objectForKey("privacy_policy_accepted_date") as? Long
            )
            Result.success(preferences)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveDataRetentionSettings(settings: DataRetentionSettings): Result<Unit> = withContext(Dispatchers.Main) {
        return@withContext try {
            userDefaults.setBool(settings.autoDeleteEnabled, forKey = "auto_delete_enabled")
            userDefaults.setInteger(settings.autoDeleteMonths, forKey = "auto_delete_months")
            userDefaults.setBool(settings.deleteRawImagesAfterProcessing, forKey = "delete_raw_images")
            userDefaults.synchronize()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getDataRetentionSettings(): Result<DataRetentionSettings> = withContext(Dispatchers.Main) {
        return@withContext try {
            val settings = DataRetentionSettings(
                autoDeleteEnabled = userDefaults.boolForKey("auto_delete_enabled", false),
                autoDeleteMonths = userDefaults.integerForKey("auto_delete_months", 12).toInt(),
                deleteRawImagesAfterProcessing = userDefaults.boolForKey("delete_raw_images", false)
            )
            Result.success(settings)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun clearAllPreferences(): Result<Unit> = withContext(Dispatchers.Main) {
        return@withContext try {
            val domain = userDefaults.persistentDomainForName("com.terrochroma.app")
            domain?.removeObjectForKey("location_enabled")
            domain?.removeObjectForKey("analytics_enabled")
            domain?.removeObjectForKey("crash_reporting_enabled")
            domain?.removeObjectForKey("privacy_policy_accepted")
            domain?.removeObjectForKey("privacy_policy_accepted_date")
            domain?.removeObjectForKey("auto_delete_enabled")
            domain?.removeObjectForKey("auto_delete_months")
            domain?.removeObjectForKey("delete_raw_images")
            domain?.removeObjectForKey("onboarding_completed")
            domain?.removeObjectForKey("onboarding_completed_date")
            userDefaults.synchronize()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun isOnboardingCompleted(): Result<Boolean> = withContext(Dispatchers.Main) {
        return@withContext try {
            val completed = userDefaults.boolForKey("onboarding_completed", false)
            Result.success(completed)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun setOnboardingCompleted(): Result<Unit> = withContext(Dispatchers.Main) {
        return@withContext try {
            userDefaults.setBool(true, forKey = "onboarding_completed")
            userDefaults.setObject(System.currentTimeMillis(), forKey = "onboarding_completed_date")
            userDefaults.synchronize()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
