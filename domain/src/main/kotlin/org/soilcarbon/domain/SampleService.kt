package org.soilcarbon.domain

data class BlobRef(val sampleId: String, val kind: String, val path: String)
data class SampleRecord(val id: String, val blobs: List<BlobRef>, val prediction: Prediction?)

interface BlobStore {
    fun put(sampleId: String, kind: String, bytes: ByteArray): BlobRef
    /** Idempotent: returns true if the blob no longer exists afterwards. */
    fun delete(ref: BlobRef): Boolean
    fun listAll(): List<BlobRef>
}

interface SampleRepository {
    fun insert(record: SampleRecord)
    fun get(id: String): SampleRecord?
    fun delete(id: String)
    fun allIds(): Set<String>
}

sealed interface DeleteResult {
    data object Deleted : DeleteResult
    data object NotFound : DeleteResult
    /** Row kept so the delete can be retried; nothing is orphaned. */
    data class PartialFailure(val remaining: List<BlobRef>) : DeleteResult
}

/**
 * A sample is ONE lifecycle unit: DB row + encrypted blobs, created and deleted together.
 * Create: blobs first, row last, compensating delete if the row insert fails.
 * Delete: blobs first, row last, so a failure leaves a retryable row instead of orphan files.
 */
class SampleService(private val repo: SampleRepository, private val blobs: BlobStore) {

    fun create(id: String, files: Map<String, ByteArray>, prediction: Prediction?): SampleRecord {
        val written = mutableListOf<BlobRef>()
        try {
            files.forEach { (kind, bytes) -> written += blobs.put(id, kind, bytes) }
            val record = SampleRecord(id, written.toList(), prediction)
            repo.insert(record)
            return record
        } catch (t: Throwable) {
            written.forEach { runCatching { blobs.delete(it) } }
            throw t
        }
    }

    fun delete(id: String): DeleteResult {
        val record = repo.get(id) ?: return DeleteResult.NotFound
        val remaining = record.blobs.filterNot { runCatching { blobs.delete(it) }.getOrDefault(false) }
        if (remaining.isNotEmpty()) return DeleteResult.PartialFailure(remaining)
        repo.delete(id)
        return DeleteResult.Deleted
    }

    /** Removes blobs with no owning row (legacy leftovers). Run at startup only, never concurrently with create(). */
    fun sweepOrphans(): Int {
        val live = repo.allIds()
        return blobs.listAll().filter { it.sampleId !in live }.count { runCatching { blobs.delete(it) }.getOrDefault(false) }
    }
}
