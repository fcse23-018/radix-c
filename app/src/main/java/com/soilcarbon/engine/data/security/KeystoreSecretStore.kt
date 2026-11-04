package com.soilcarbon.engine.data.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Hardware-backed key material. The SQLCipher passphrase never leaves the app: it is generated
 * once with a CSPRNG and wrapped with an AES-256-GCM key held in Android Keystore.
 */
class KeystoreSecretStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val random = SecureRandom()

    fun databasePassphrase(): ByteArray {
        val encoded = prefs.getString(KEY_WRAPPED_PASSPHRASE, null)
        if (encoded != null) {
            return runCatching { unwrap(Base64.decode(encoded, Base64.NO_WRAP)) }
                .getOrElse { createAndStorePassphrase() }
        }
        return createAndStorePassphrase()
    }

    fun encrypt(plain: ByteArray): ByteArray = wrap(plain)

    fun decrypt(payload: ByteArray): ByteArray = unwrap(payload)

    fun randomToken(bytes: Int = 32): String {
        val buffer = ByteArray(bytes)
        random.nextBytes(buffer)
        return Base64.encodeToString(buffer, Base64.NO_WRAP)
    }

    private fun createAndStorePassphrase(): ByteArray {
        val passphrase = ByteArray(32)
        random.nextBytes(passphrase)
        val wrapped = wrap(passphrase)
        prefs.edit().putString(KEY_WRAPPED_PASSPHRASE, Base64.encodeToString(wrapped, Base64.NO_WRAP)).apply()
        return passphrase
    }

    private fun wrap(plain: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey(KEYSTORE_ALIAS))
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plain)
        return byteArrayOf(iv.size.toByte()) + iv + ciphertext
    }

    private fun unwrap(payload: ByteArray): ByteArray {
        require(payload.size > 13) { "Wrapped payload is truncated" }
        val ivSize = payload[0].toInt() and 0xff
        require(ivSize in 12..16) { "Unsupported IV length" }
        val iv = payload.copyOfRange(1, 1 + ivSize)
        val ciphertext = payload.copyOfRange(1 + ivSize, payload.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey(KEYSTORE_ALIAS), GCMParameterSpec(128, iv))
        return cipher.doFinal(ciphertext)
    }

    private fun secretKey(alias: String): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getEntry(alias, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)
            .build()
        generator.init(spec)
        return generator.generateKey()
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEYSTORE_ALIAS = "soilcarbon_master_key_v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val PREFS_NAME = "soilcarbon_secure_prefs"
        const val KEY_WRAPPED_PASSPHRASE = "wrapped_db_passphrase"
    }
}
