package com.soilcarbon.engine.data.security

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Decrypts one evidence file into the app cache for display. The caller owns the returned file
 * and must delete it as soon as the UI no longer needs it.
 */
@Singleton
class EvidenceImageLoader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val store: EncryptedFileStore
) {
    suspend fun decryptToCache(relativePath: String): File =
        store.decryptToTemp(relativePath, context.cacheDir)
}
