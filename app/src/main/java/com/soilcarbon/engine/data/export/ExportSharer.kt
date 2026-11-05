package com.soilcarbon.engine.data.export

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.soilcarbon.engine.core.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Shares generated exports through the app's [FileProvider] and (optionally) copies them to a
 * user-picked SAF document.
 *
 * Exports are written to `cacheDir/exports`; that directory is the only cache root exposed in
 * `res/xml/file_paths.xml`, so no encrypted sample evidence can leak through this provider.
 */
@Singleton
class ExportSharer @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Resolves a generated export to a content URI.
     *
     * @throws IllegalArgumentException when [path] is outside the exports directory. This is
     * deliberate: a bad path must fail loudly rather than expose arbitrary app files.
     */
    fun uriFor(path: String): Uri {
        val exportsDir = File(context.cacheDir, Constants.EXPORT_DIR).canonicalFile
        val file = File(path).canonicalFile
        val inside = file.path == exportsDir.path ||
            file.path.startsWith(exportsDir.path + File.separator)
        require(inside) { "Refusing to share a file outside ${exportsDir.path}: ${file.path}" }
        require(file.isFile) { "Export file does not exist: ${file.path}" }
        return FileProvider.getUriForFile(context, authority, file)
    }

    /** Plain `ACTION_SEND` intent carrying the read grant for the export URI. */
    fun shareIntent(path: String, mimeType: String, subject: String): Intent {
        val uri = uriFor(path)
        return Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            clipData = ClipData.newRawUri(subject, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    /** User-facing chooser; the wrapped intent carries the read grant. */
    fun chooser(path: String, mimeType: String, subject: String): Intent =
        Intent.createChooser(
            shareIntent(path, mimeType, subject),
            "Share SoilCarbon export"
        )

    /** Copies an already-generated export to a SAF document URI chosen by the user. */
    suspend fun writeTo(destination: Uri, sourcePath: String) = withContext(Dispatchers.IO) {
        val source = File(sourcePath)
        require(source.isFile) { "Export file does not exist: ${source.path}" }
        val output = context.contentResolver.openOutputStream(destination)
            ?: error("Could not open the selected destination for writing.")
        source.inputStream().use { input ->
            output.use { out -> input.copyTo(out) }
        }
    }

    val authority: String get() = "${context.packageName}.fileprovider"
}
