package com.soilcarbon.engine.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.soilcarbon.engine.data.local.SoilCarbonDatabase
import com.soilcarbon.engine.data.repository.RoomSampleRepository
import com.soilcarbon.engine.data.security.EncryptedBlobStore
import com.soilcarbon.engine.data.security.EncryptedFileStore
import com.soilcarbon.engine.data.security.KeystoreSecretStore
import com.soilcarbon.engine.domain.model.SoilSample
import com.soilcarbon.engine.domain.repository.SampleBlobStore
import com.soilcarbon.engine.domain.usecase.BlobSource
import com.soilcarbon.engine.domain.usecase.SampleDeleteResult
import com.soilcarbon.engine.domain.usecase.SampleService
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.soilcarbon.domain.BlobRef
import java.io.File
import java.util.UUID

/**
 * Device tests for the one-lifecycle-unit contract. The JVM suite covers the service with fakes;
 * this suite proves the real Room transaction and the real Keystore-backed file envelope agree.
 */
@RunWith(AndroidJUnit4::class)
class SampleLifecycleInstrumentedTest {

    private lateinit var context: Context
    private lateinit var database: SoilCarbonDatabase
    private lateinit var secureRoot: File
    private lateinit var fileStore: EncryptedFileStore
    private lateinit var blobStore: EncryptedBlobStore
    private lateinit var repository: RoomSampleRepository
    private lateinit var service: SampleService

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, SoilCarbonDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        secureRoot = File(context.cacheDir, "test-secure-${UUID.randomUUID()}").apply { mkdirs() }
        fileStore = EncryptedFileStore(secureRoot, KeystoreSecretStore(context))
        blobStore = EncryptedBlobStore(fileStore)
        repository = RoomSampleRepository(database.sampleDao(), database.sampleLifecycleDao())
        service = SampleService(repository, blobStore)
    }

    @After
    fun tearDown() {
        database.close()
        secureRoot.deleteRecursively()
    }

    @Test
    fun createAndDeleteMoveRowAndEncryptedBlobsTogether() = runBlocking {
        val jpegBytes = "jpeg-payload".toByteArray()
        val rawBytes = "raw-payload".toByteArray()
        val sample = service.create(
            sampleId = "s1",
            sources = listOf(
                BlobSource("jpeg", "capture.jpg", tempFile("capture.jpg", jpegBytes)),
                BlobSource("raw", "capture.dng", tempFile("capture.dng", rawBytes))
            )
        ) { blobs ->
            SoilSample(id = "s1", jpegImagePath = blobs.getValue("jpeg").relativePath)
        }

        assertThat(sample.id).isEqualTo("s1")
        assertThat(repository.getSample("s1")).isNotNull()
        assertThat(repository.blobsFor("s1")).hasSize(2)
        assertThat(fileStore.listAll()).hasSize(2)

        val jpegBlob = repository.blobsFor("s1")!!.first { it.kind == "jpeg" }
        val decrypted = fileStore.decryptToTemp(jpegBlob.relativePath, context.cacheDir)
        try {
            assertThat(decrypted.readBytes()).isEqualTo(jpegBytes)
        } finally {
            decrypted.delete()
        }

        assertThat(service.delete("s1")).isEqualTo(SampleDeleteResult.Deleted)
        assertThat(repository.getSample("s1")).isNull()
        assertThat(repository.blobsFor("s1")).isNull()
        assertThat(fileStore.listAll()).isEmpty()
    }

    @Test
    fun blobDeleteFailureKeepsARetryableRowAndNoOrphans() = runBlocking {
        service.create(
            sampleId = "s1",
            sources = listOf(
                BlobSource("jpeg", "capture.jpg", tempFile("capture.jpg", "jpeg".toByteArray())),
                BlobSource("raw", "capture.dng", tempFile("capture.dng", "raw".toByteArray()))
            )
        ) { SoilSample(id = "s1") }

        val failing = FailingBlobStore(blobStore, failPath = "images/s1/capture.dng")
        val failingService = SampleService(repository, failing)

        val result = failingService.delete("s1")
        assertThat(result).isInstanceOf(SampleDeleteResult.PartialFailure::class.java)
        assertThat((result as SampleDeleteResult.PartialFailure).remaining.map { it.kind })
            .containsExactly("raw")
        assertThat(repository.getSample("s1")).isNotNull()
        assertThat(repository.blobsFor("s1")).hasSize(2)

        assertThat(service.delete("s1")).isEqualTo(SampleDeleteResult.Deleted)
        assertThat(repository.getSample("s1")).isNull()
        assertThat(fileStore.listAll()).isEmpty()
    }

    @Test
    fun startupSweepRemovesOnlyBlobsWithoutARow() = runBlocking {
        service.create(
            "live",
            listOf(BlobSource("raw", "capture.dng", tempFile("live.dng", byteArrayOf(1))))
        ) { SoilSample(id = "live") }
        blobStore.importFile("ghost", "raw", "capture.dng", tempFile("ghost.dng", byteArrayOf(9)))

        assertThat(service.sweepOrphans()).isEqualTo(1)
        assertThat(repository.getSample("live")).isNotNull()
        assertThat(fileStore.listAll().map { it.sampleId }).containsExactly("live")
        assertThat(service.sweepOrphans()).isEqualTo(0)
    }

    private fun tempFile(name: String, bytes: ByteArray): File =
        File(context.cacheDir, "test-${UUID.randomUUID()}-$name").apply { writeBytes(bytes) }

    private class FailingBlobStore(
        private val delegate: SampleBlobStore,
        private val failPath: String
    ) : SampleBlobStore by delegate {
        override suspend fun deleteRef(ref: BlobRef): Boolean =
            if (ref.path == failPath) false else delegate.deleteRef(ref)
    }
}
