package org.soilcarbon.domain

fun testSpec() = FeatureSpec("1.0.0", listOf(
    FeatureDef("lab_l", "L*", 0.0, 100.0, true, null),
    FeatureDef("moisture_pct", "%", 0.0, 60.0, true, null),
    FeatureDef("ndvi_sat", "idx", -0.2, 1.0, false, 0.35),
))

fun testMeta(
    placeholder: Boolean = false, validated: Boolean = true, spec: String = "1.0.0",
    order: List<String> = listOf("lab_l", "moisture_pct", "ndvi_sat"),
    fallbackTrained: List<String> = listOf("ndvi_sat"), hash: String = "abc123",
) = ModelMetadata("m", "1.0.0", spec, order, placeholder, fallbackTrained, 0.0, 20.0, validated, hash)

class FakeModel(override val metadata: ModelMetadata, private val output: Float = 2.5f) : SocModel {
    var lastInput: FloatArray? = null
    var calls = 0
    override fun predict(features: FloatArray): Float { calls++; lastInput = features; return output }
}

fun goodInputs() = mapOf(
    "lab_l" to FeatureInput(40.0, FeatureSource.MEASURED),
    "moisture_pct" to FeatureInput(12.0, FeatureSource.USER_ENTERED),
    "ndvi_sat" to FeatureInput(0.6, FeatureSource.MEASURED),
)

class InMemoryBlobs : BlobStore {
    val store = linkedMapOf<String, ByteArray>()
    var failPutOnKind: String? = null
    var failDeleteOn: Set<String> = emptySet()
    override fun put(sampleId: String, kind: String, bytes: ByteArray): BlobRef {
        if (kind == failPutOnKind) error("disk full")
        val ref = BlobRef(sampleId, kind, "$sampleId/$kind"); store[ref.path] = bytes; return ref
    }
    override fun delete(ref: BlobRef): Boolean {
        if (ref.path in failDeleteOn) return false
        store.remove(ref.path); return true
    }
    override fun listAll() = store.keys.map { BlobRef(it.substringBefore('/'), it.substringAfter('/'), it) }
}

class InMemoryRepo : SampleRepository {
    val rows = linkedMapOf<String, SampleRecord>()
    var failInsert = false
    override fun insert(record: SampleRecord) { if (failInsert) error("db locked"); rows[record.id] = record }
    override fun get(id: String) = rows[id]
    override fun delete(id: String) { rows.remove(id) }
    override fun allIds() = rows.keys.toSet()
}
