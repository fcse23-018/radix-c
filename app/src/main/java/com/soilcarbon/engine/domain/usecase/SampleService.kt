package com.soilcarbon.engine.domain.usecase

import com.soilcarbon.engine.domain.model.SoilSample
import com.soilcarbon.engine.domain.model.StoredBlob
import com.soilcarbon.engine.domain.repository.SampleBlobStore
import com.soilcarbon.engine.domain.repository.SampleRepository
import org.soilcarbon.domain.BlobRef
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** A temporary capture file to be encrypted into the sample's blob set. */
data class BlobSource(
    val kind: String,
    val fileName: String,
    val source: File
)

sealed interface SampleDeleteResult {
    data object Deleted : SampleDeleteResult
    data object NotFound : SampleDeleteResult

    /**
     * The row is kept so the delete can be retried. Nothing is orphaned: every blob in
     * [remaining] still belongs to a live row.
     */
    data class PartialFailure(val remaining: List<StoredBlob>) : SampleDeleteResult
}

/**
 * Android-side implementation of the scaffold's `org.soilcarbon.domain.SampleService` semantics.
 *
 * Create: blobs first, row last, compensating deletes if preparation or the row insert fails.
 * Delete: blobs first, row last, so a blob failure leaves a retryable row instead of orphan files.
 * The row + blob index are written/removed in one Room `@Transaction`.
 */
@Singleton
class SampleService @Inject constructor(
    private val repository: SampleRepository,
    private val blobStore: SampleBlobStore
) {

    suspend fun create(
        sampleId: String,
        sources: List<BlobSource>,
        prepare: suspend (Map<String, StoredBlob>) -> SoilSample
    ): SoilSample {
        val written = mutableListOf<StoredBlob>()
        try {
            sources.forEach { source ->
                written += blobStore.importFile(
                    sampleId = sampleId,
                    kind = source.kind,
                    fileName = source.fileName,
                    source = source.source
                )
            }
            val byKind = written.associateBy { it.kind }
            val sample = prepare(byKind)
            repository.insertWithBlobs(sample, written)
            return sample
        } catch (t: Throwable) {
            written.forEach { blob ->
                runCatching { blobStore.deleteRef(blob.toRef()) }
            }
            throw t
        }
    }

    suspend fun delete(id: String): SampleDeleteResult {
        val blobs = repository.blobsFor(id) ?: return SampleDeleteResult.NotFound
        val remaining = blobs.filterNot { blob ->
            runCatching { blobStore.deleteRef(blob.toRef()) }.getOrDefault(false)
        }
        if (remaining.isNotEmpty()) return SampleDeleteResult.PartialFailure(remaining)
        repository.deleteWithBlobs(id)
        return SampleDeleteResult.Deleted
    }

    /** Removes blobs with no owning row. Called once at startup, never concurrently with create(). */
    suspend fun sweepOrphans(): Int {
        val live = repository.allIds()
        return blobStore.listRefs()
            .filter { it.sampleId !in live }
            .count { ref -> runCatching { blobStore.deleteRef(ref) }.getOrDefault(false) }
    }

    private fun StoredBlob.toRef(): BlobRef = BlobRef(
        sampleId = sampleId,
        kind = kind,
        path = relativePath
    )
}
