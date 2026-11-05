package com.soilcarbon.engine.data.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

/**
 * Device tests for the FileProvider share contract. The JVM suite asserts export contents; this
 * suite proves the real provider resolves the generated URI, that the chooser carries the read
 * grant, and that the provider refuses paths outside `cacheDir/exports`.
 */
@RunWith(AndroidJUnit4::class)
class ExportShareInstrumentedTest {

    private lateinit var context: Context
    private lateinit var sharer: ExportSharer
    private lateinit var exportsDir: File
    private val created = mutableListOf<File>()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        sharer = ExportSharer(context)
        exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
    }

    @After
    fun tearDown() {
        created.forEach { it.delete() }
        created.clear()
    }

    @Test
    fun generatedExportUriResolvesThroughFileProvider() {
        val payload = "sample_id,status\ns1,ANALYZED\n"
        val file = exportFile("share.csv", payload)

        val uri = sharer.uriFor(file.absolutePath)

        assertThat(uri.scheme).isEqualTo("content")
        assertThat(uri.authority).isEqualTo("${context.packageName}.fileprovider")
        val readBack = context.contentResolver.openInputStream(uri)!!.use { it.readBytes() }
        assertThat(String(readBack)).isEqualTo(payload)
    }

    @Test
    fun shareIntentCarriesActionStreamAndReadGrant() {
        val file = exportFile("share.json", "{\"ok\":true}")

        val intent = sharer.shareIntent(file.absolutePath, "application/json", "SoilCarbon export")
        val stream = intent.extras?.get(Intent.EXTRA_STREAM) as Uri

        assertThat(intent.action).isEqualTo(Intent.ACTION_SEND)
        assertThat(intent.type).isEqualTo("application/json")
        assertThat(stream).isEqualTo(sharer.uriFor(file.absolutePath))
        assertThat(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .isEqualTo(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        assertThat(intent.clipData).isNotNull()
        assertThat(intent.clipData!!.getItemAt(0).uri).isEqualTo(stream)
    }

    @Test
    fun chooserWrapsAnIntentThatRetainsTheReadGrant() {
        val file = exportFile("share.geojson", "{\"type\":\"FeatureCollection\"}")

        val chooser = sharer.chooser(file.absolutePath, "application/geo+json", "SoilCarbon GeoJSON")
        val inner = chooser.extras?.get(Intent.EXTRA_INTENT) as Intent

        assertThat(chooser.action).isEqualTo(Intent.ACTION_CHOOSER)
        assertThat(inner.action).isEqualTo(Intent.ACTION_SEND)
        assertThat(inner.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .isEqualTo(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    @Test
    fun providerRefusesFilesOutsideTheExportsDirectory() {
        val secret = File(context.filesDir, "secret-${UUID.randomUUID()}.txt")
            .apply { writeText("not an export") }
        created += secret

        val error = assertThrows(IllegalArgumentException::class.java) {
            sharer.uriFor(secret.absolutePath)
        }

        assertThat(error).hasMessageThat().contains("outside")
    }

    private fun exportFile(name: String, contents: String): File =
        File(exportsDir, "${UUID.randomUUID()}-$name")
            .apply { writeText(contents) }
            .also { created += it }
}
