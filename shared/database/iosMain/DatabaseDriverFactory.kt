package com.terrochroma.shared.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import platform.Foundation.NSUserDefaults

actual class PlatformDatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        return NativeSqliteDriver(
            schema = TerroChromaDatabase.Schema,
            name = "terrochroma.db"
        )
    }

    actual fun getEncryptionKey(): String {
        // In production, use Keychain for secure storage
        // For now, use UserDefaults as placeholder
        val defaults = NSUserDefaults.standardUserDefaults
        val deviceId = defaults.stringForKey("device_id") ?: generateDeviceId()
        if (defaults.stringForKey("device_id") == null) {
            defaults.setObject(deviceId, forKey = "device_id")
        }
        return deviceId.take(32).padEnd(32, '0')
    }

    private fun generateDeviceId(): String {
        // Generate a random device ID
        val uuid = platform.Foundation.NSUUID.UUID()
        return uuid.UUIDString
    }
}
