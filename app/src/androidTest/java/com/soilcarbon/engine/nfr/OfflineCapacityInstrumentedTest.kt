package com.soilcarbon.engine.nfr

import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.soilcarbon.engine.data.local.SampleMapper
import com.soilcarbon.engine.data.local.SoilCarbonDatabase
import com.soilcarbon.engine.domain.model.SampleStatus
import com.soilcarbon.engine.domain.model.SoilSample
import com.soilcarbon.engine.domain.model.SyncState
import kotlinx.coroutines.runBlocking
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/**
 * NFR-2: "The mobile app shall securely cache up to 1,000 un-synced field samples locally in an
 * encrypted database (SQLite/Room) during extended offline field operations."
 *
 * Device-only test. It uses the same SQLCipher open-helper factory as production, inserts 1,000
 * local-only rows, closes/reopens the database and verifies that the predictable tag text never
 * appears in the database files.
 */
@RunWith(AndroidJUnit4::class)
class OfflineCapacityInstrumentedTest {

    private lateinit var context: Context
    private val databaseName = "nfr2-${UUID.randomUUID()}.db"

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        // The production Application loads this; make the test independently runnable too.
        runCatching { System.loadLibrary("sqlcipher") }
        context.deleteDatabase(databaseName)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(databaseName)
    }

    @Test
    fun nfr2_oneThousandUnsyncedSamplesFitInTheEncryptedDatabase() = runBlocking {
        val startedAt = System.nanoTime()
        val passphrase = "nfr2-test-passphrase".toByteArray()
        val database = openDatabase(passphrase)
        try {
            val lifecycleDao = database.sampleLifecycleDao()
            repeat(SAMPLE_COUNT) { index ->
                lifecycleDao.insertWithBlobs(
                    sample = SampleMapper.toEntity(
                        SoilSample(
                            id = "nfr2-$index",
                            tag = "capacity-probe-$index",
                            status = SampleStatus.CAPTURED,
                            syncState = SyncState.LOCAL_ONLY
                        )
                    ),
                    blobs = emptyList()
                )
            }

            assertThat(database.sampleDao().count()).isEqualTo(SAMPLE_COUNT)
            assertThat(database.sampleDao().getPendingSync()).hasSize(SAMPLE_COUNT)
            assertThat(lifecycleDao.allSampleIds()).hasSize(SAMPLE_COUNT)
        } finally {
            database.close()
        }

        // Prove the 1,000 rows survive a close/reopen of the encrypted file.
        val reopened = openDatabase(passphrase)
        try {
            assertThat(reopened.sampleDao().count()).isEqualTo(SAMPLE_COUNT)
        } finally {
            reopened.close()
        }

        // Prove the database payload is encrypted: the predictable tag must not appear in plaintext.
        val probe = "capacity-probe-${SAMPLE_COUNT - 1}"
        val databaseFiles = context.getDatabasePath(databaseName).parentFile
            ?.listFiles { file -> file.name.startsWith(databaseName) }
            .orEmpty()
        assertThat(databaseFiles).isNotEmpty()
        databaseFiles.forEach { file ->
            val bytes = file.readBytes()
            assertThat(bytes).isNotEmpty()
            assertThat(String(bytes, Charsets.ISO_8859_1)).doesNotContain(probe)
        }
        val elapsedMs = (System.nanoTime() - startedAt) / 1_000_000
        Log.i(
            "SoilCarbonNFR",
            "NFR-2 samples=$SAMPLE_COUNT databaseFiles=${databaseFiles.size} " +
                "reopenCount=$SAMPLE_COUNT plaintextAbsent=true totalMs=$elapsedMs"
        )
        Unit
    }

    private fun openDatabase(passphrase: ByteArray): SoilCarbonDatabase =
        Room.databaseBuilder(context, SoilCarbonDatabase::class.java, databaseName)
            .openHelperFactory(SupportOpenHelperFactory(passphrase))
            .allowMainThreadQueries()
            .build()

    private companion object {
        const val SAMPLE_COUNT = 1_000
    }
}
