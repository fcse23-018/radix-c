package com.soilcarbon.engine.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.soilcarbon.engine.domain.model.SampleStats
import com.soilcarbon.engine.domain.model.SoilSample
import com.soilcarbon.engine.domain.model.StoredBlob
import com.soilcarbon.engine.domain.model.SyncState
import com.soilcarbon.engine.domain.repository.SampleBlobStore
import com.soilcarbon.engine.domain.repository.SampleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.soilcarbon.domain.BlobRef
import java.io.File

class SampleServiceTest {

    @get:Rule
    val temp = TemporaryFolder()

    private val repository = FakeSampleRepository()
    private val blobStore = FakeBlobStore()
    private val service = SampleService(repository, blobStore)

    private fun files(): List<BlobSource> = listOf(
        BlobSource("jpeg", "capture.jpg", source("jpeg")),
        BlobSource("raw", "capture.dng", source("raw")),
        BlobSource("aperture", "aperture.jpg", source("aperture"))
    )

    private fun source(kind: String): File =
        temp.newFile().apply { writeBytes(byteArrayOf(kind.length.toByte())) }

    private suspend fun create(id: String = "s1"): SoilSample =
        service.create(id, files()) { blobs -> SoilSample(id = id, jpegImagePath = blobs.getValue("jpeg").relativePath) }

    @Test
    fun `create stores the row and every blob`() = runTest {
        val sample = create()

        assertThat(sample.id).isEqualTo("s1")
        assertThat(repository.samples).containsKey("s1")
        assertThat(blobStore.files).hasSize(3)
        assertThat(repository.blobs.getValue("s1").map { it.kind })
            .containsExactly("jpeg", "raw", "aperture")
    }

    @Test
    fun `delete removes the row and every blob`() = runTest {
        create("s1")
        create("s2")

        assertThat(service.delete("s1")).isEqualTo(SampleDeleteResult.Deleted)

        assertThat(repository.samples).doesNotContainKey("s1")
        assertThat(blobStore.files.keys.none { it.startsWith("images/s1/") }).isTrue()
        assertThat(blobStore.files.keys.count { it.startsWith("images/s2/") }).isEqualTo(3)
    }

    @Test
    fun `failed blob delete keeps the row so it can be retried`() = runTest {
        create("s1")
        blobStore.failDeleteOn = setOf("images/s1/capture.dng")

        val result = service.delete("s1")

        assertThat(result).isInstanceOf(SampleDeleteResult.PartialFailure::class.java)
        val remaining = (result as SampleDeleteResult.PartialFailure).remaining
        assertThat(remaining.map { it.kind }).containsExactly("raw")
        assertThat(repository.samples).containsKey("s1")

        blobStore.failDeleteOn = emptySet()
        assertThat(service.delete("s1")).isEqualTo(SampleDeleteResult.Deleted)
        assertThat(blobStore.files).isEmpty()
    }

    @Test
    fun `deleting an unknown sample is NotFound`() = runTest {
        assertThat(service.delete("missing")).isEqualTo(SampleDeleteResult.NotFound)
    }

    @Test
    fun `prepare failure rolls back every blob`() = runTest {
        val thrown = runCatching {
            service.create("s1", files()) { error("analysis rejected") }
        }.exceptionOrNull()

        assertThat(thrown).isInstanceOf(IllegalStateException::class.java)
        assertThat(blobStore.files).isEmpty()
        assertThat(repository.samples).isEmpty()
    }

    @Test
    fun `row insert failure rolls back every blob`() = runTest {
        repository.failInsert = true

        val thrown = runCatching {
            service.create("s1", files()) { SoilSample(id = "s1") }
        }.exceptionOrNull()

        assertThat(thrown).isInstanceOf(IllegalStateException::class.java)
        assertThat(blobStore.files).isEmpty()
        assertThat(repository.samples).isEmpty()
    }

    @Test
    fun `blob import failure midway rolls back earlier blobs`() = runTest {
        blobStore.failImportOn = setOf("aperture")

        runCatching { service.create("s1", files()) { SoilSample(id = "s1") } }

        assertThat(blobStore.files).isEmpty()
        assertThat(repository.samples).isEmpty()
    }

    @Test
    fun `sweep removes only blobs without an owning row`() = runTest {
        create("live")
        blobStore.importFile("ghost", "raw", "capture.dng", source("ghost"))
        blobStore.importFile("ghost", "jpeg", "capture.jpg", source("ghost2"))

        assertThat(service.sweepOrphans()).isEqualTo(2)
        assertThat(blobStore.files.keys.count { it.startsWith("images/live/") }).isEqualTo(3)
        assertThat(service.sweepOrphans()).isEqualTo(0)
    }

    private class FakeSampleRepository : SampleRepository {
        val samples = mutableMapOf<String, SoilSample>()
        val blobs = mutableMapOf<String, MutableList<StoredBlob>>()
        var failInsert = false

        override fun observeSamples(): Flow<List<SoilSample>> = emptyFlow()
        override fun observeSample(id: String): Flow<SoilSample?> = emptyFlow()
        override fun observeStats(): Flow<SampleStats> = emptyFlow()
        override suspend fun getSample(id: String): SoilSample? = samples[id]

        override suspend fun upsert(sample: SoilSample) {
            samples[sample.id] = sample
        }

        override suspend fun delete(id: String) {
            samples.remove(id)
        }

        override suspend fun count(): Int = samples.size
        override suspend fun setSyncState(id: String, state: SyncState) = Unit
        override suspend fun markExported(ids: List<String>, atMillis: Long) = Unit

        override suspend fun blobsFor(id: String): List<StoredBlob>? =
            if (samples.containsKey(id)) blobs[id].orEmpty() else null

        override suspend fun insertWithBlobs(sample: SoilSample, blobs: List<StoredBlob>) {
            if (failInsert) error("insert failed")
            samples[sample.id] = sample
            this.blobs[sample.id] = blobs.toMutableList()
        }

        override suspend fun deleteWithBlobs(id: String): List<StoredBlob> {
            samples.remove(id)
            return blobs.remove(id).orEmpty()
        }

        override suspend fun allIds(): Set<String> = samples.keys.toSet()
    }

    private class FakeBlobStore : SampleBlobStore {
        val files = mutableMapOf<String, StoredBlob>()
        var failDeleteOn: Set<String> = emptySet()
        var failImportOn: Set<String> = emptySet()

        override suspend fun importFile(
            sampleId: String,
            kind: String,
            fileName: String,
            source: File
        ): StoredBlob {
            if (kind in failImportOn) error("import failed: $kind")
            val path = "images/$sampleId/$fileName"
            val blob = StoredBlob(
                sampleId = sampleId,
                kind = kind,
                relativePath = path,
                sha256 = "sha-$path",
                sizeBytes = source.length()
            )
            files[path] = blob
            return blob
        }

        override suspend fun deleteRef(ref: BlobRef): Boolean {
            if (ref.path in failDeleteOn) return false
            files.remove(ref.path)
            return true
        }

        override suspend fun listRefs(): List<BlobRef> = files.values.map {
            BlobRef(it.sampleId, it.kind, it.relativePath)
        }
    }
}
