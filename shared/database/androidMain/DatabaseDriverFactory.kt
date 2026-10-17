package com.terrochroma.shared.database

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import net.sqlcipher.database.SupportFactory

actual class PlatformDatabaseDriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver {
        val passphrase = getEncryptionKey()
        val factory = SupportFactory(passphrase.toByteArray())

        return AndroidSqliteDriver(
            schema = TerroChromaDatabase.Schema,
            context = context,
            name = "terrochroma.db",
            factory = factory
        )
    }

    actual fun getEncryptionKey(): String {
        // In production, this should be stored securely (e.g., Android Keystore)
        // For now, generate a deterministic key from device info
        val deviceId = android.provider.Settings.Secure.getString(
            context.contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        )
        return deviceId.take(32).padEnd(32, '0')
    }
}
