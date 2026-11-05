package org.soilcarbon.domain

import kotlin.test.*

class SampleServiceTest {
    private val repo = InMemoryRepo()
    private val blobs = InMemoryBlobs()
    private val service = SampleService(repo, blobs)
    private val files = mapOf("raw" to byteArrayOf(1), "jpeg" to byteArrayOf(2), "aperture" to byteArrayOf(3))

    @Test fun createStoresRowAndAllBlobs() {
        val rec = service.create("s1", files, null)
        assertEquals(3, rec.blobs.size); assertEquals(3, blobs.store.size); assertNotNull(repo.get("s1"))
    }

    @Test fun deleteRemovesRowAndEveryBlob() {
        service.create("s1", files, null); service.create("s2", files, null)
        assertEquals(DeleteResult.Deleted, service.delete("s1"))
        assertNull(repo.get("s1"))
        assertTrue(blobs.store.keys.none { it.startsWith("s1/") })
        assertEquals(3, blobs.store.keys.count { it.startsWith("s2/") })   // neighbours untouched
    }

    @Test fun failedBlobDeleteKeepsRowSoItCanBeRetried() {
        service.create("s1", files, null)
        blobs.failDeleteOn = setOf("s1/jpeg")
        val r = service.delete("s1") as DeleteResult.PartialFailure
        assertEquals(listOf("s1/jpeg"), r.remaining.map { it.path })
        assertNotNull(repo.get("s1"))
        blobs.failDeleteOn = emptySet()
        assertEquals(DeleteResult.Deleted, service.delete("s1"))
        assertTrue(blobs.store.isEmpty())
    }

    @Test fun deleteUnknownIsNotFound() = assertEquals(DeleteResult.NotFound, service.delete("nope"))

    @Test fun rowInsertFailureRollsBackBlobs() {
        repo.failInsert = true
        assertFailsWith<IllegalStateException> { service.create("s1", files, null) }
        assertTrue(blobs.store.isEmpty())
    }

    @Test fun blobWriteFailureMidwayRollsBackEarlierBlobs() {
        blobs.failPutOnKind = "aperture"
        assertFailsWith<IllegalStateException> { service.create("s1", files, null) }
        assertTrue(blobs.store.isEmpty()); assertNull(repo.get("s1"))
    }

    @Test fun sweepRemovesOnlyOrphans() {
        service.create("live", files, null)
        blobs.put("ghost", "raw", byteArrayOf(9)); blobs.put("ghost", "jpeg", byteArrayOf(9))
        assertEquals(2, service.sweepOrphans())
        assertEquals(3, blobs.store.size)
        assertEquals(0, service.sweepOrphans())
    }
}
