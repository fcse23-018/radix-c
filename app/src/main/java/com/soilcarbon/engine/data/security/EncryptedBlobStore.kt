package com.soilcarbon.engine.data.security

import com.soilcarbon.engine.domain.model.StoredBlob
import com.soilcarbon.engine.domain.repository.SampleBlobStore
import kotlinx.coroutines.runBlocking
import org.soilcarbon.domain.BlobRef
import org.soilcarbon.domain.BlobStore
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Adapter between the pure-JVM [BlobStore] contract and the AES-GCM [EncryptedFileStore].
 *
 * The suspend methods are what the Android capture/delete pipeline uses; the synchronous
 * [BlobStore] methods are kept so the scaffold's `SampleService` can be exercised unchanged in
 * JVM tests. Never call the synchronous variants from the main thread.
 */
@Singleton
class EncryptedBlobStore @Inject constructor(
    private val store: EncryptedFileStore
) : SampleBlobStore, BlobStore {

    override suspend fun importFile(
        sampleId: String,
        kind: String,
        fileName: String,
        source: File
    ): StoredBlob {
        require(!fileName.contains('/') && !fileName.contains('\\')) {
            "Blob file name must not contain path separators: $fileName"
        }
        val secure = store.importFile(source, pathFor(sampleId, fileName))
        return StoredBlob(
            sampleId = sampleId,
            kind = kind,
            relativePath = secure.relativePath,
            sha256 = secure.sha256,
            sizeBytes = secure.sizeBytes
        )
    }

    override suspend fun deleteRef(ref: BlobRef): Boolean = store.delete(ref)

    override suspend fun listRefs(): List<BlobRef> = store.listAll()

    override fun put(sampleId: String, kind: String, bytes: ByteArray): BlobRef = runBlocking {
        require(!kind.contains('/') && !kind.contains('\\')) {
            "Blob kind must not contain path separators: $kind"
        }
        val secure = store.writeBytes(pathFor(sampleId, kind), bytes)
        BlobRef(sampleId = sampleId, kind = kind, path = secure.relativePath)
    }

    override fun delete(ref: BlobRef): Boolean = runBlocking { store.delete(ref) }

    override fun listAll(): List<BlobRef> = store.listAll()

    private fun pathFor(sampleId: String, fileName: String): String =
        "images/$sampleId/$fileName"
}
