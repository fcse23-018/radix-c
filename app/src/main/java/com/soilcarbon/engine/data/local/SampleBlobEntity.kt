package com.soilcarbon.engine.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Index of the encrypted files that belong to a sample. It is kept in the database so a delete
 * knows exactly which blobs to remove and so the row + index are written in one transaction.
 */
@Entity(
    tableName = "sample_blobs",
    primaryKeys = ["sampleId", "kind"],
    foreignKeys = [
        ForeignKey(
            entity = SampleEntity::class,
            parentColumns = ["id"],
            childColumns = ["sampleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sampleId")]
)
data class SampleBlobEntity(
    val sampleId: String,
    val kind: String,
    val relativePath: String,
    val sha256: String,
    val sizeBytes: Long
)
