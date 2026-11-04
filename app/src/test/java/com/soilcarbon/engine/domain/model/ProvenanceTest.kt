package com.soilcarbon.engine.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ProvenanceTest {

    private fun sample(tag: String = "North paddock A3"): SoilSample = SoilSample(
        id = "sample-1",
        tag = tag,
        notes = "level ground, diffuse light",
        role = UserRole.FIELD_TECHNICIAN,
        createdAtMillis = 1_700_000_000_000,
        capturedAtMillis = 1_700_000_001_000,
        status = SampleStatus.ANALYZED,
        location = GeoPoint(
            latitude = 51.5012,
            longitude = -0.1425,
            altitudeMeters = 35.0,
            accuracyMeters = 4.0f,
            provider = "gps",
            timestampMillis = 1_700_000_001_000
        ),
        imu = ImuReading(pitchDegrees = 2.5f, rollDegrees = -1.5f),
        light = LightReading(lux = 12_400f),
        exposure = CameraExposureParameters(
            iso = 100,
            exposureTimeNs = 8_000_000L,
            whiteBalanceKelvin = 6500,
            deviceModel = "Pixel 8",
            deviceManufacturer = "Google"
        ),
        rawSha256 = "a".repeat(64),
        jpegSha256 = "b".repeat(64),
        soil = SoilProperties(bulkDensityGcm3 = 1.30, depthCm = 30.0),
        features = FeatureVector(38.5, 9.25, 18.75, 4.6, 0.31, 0.42, 0.47, 31.5),
        prediction = Prediction(
            socPercent = 1.42,
            somPercent = 1.42 * 1.724,
            socLower95 = 1.18,
            socUpper95 = 1.66,
            uncertainty95 = 0.24,
            confidenceScore = 0.78,
            carbonStockMgPerHa = 55.38,
            modelId = "soc-visible-color-v1",
            modelVersion = "1.0.0-placeholder",
            inferenceMillis = 12,
            fallbackUsed = true,
            notes = "test"
        )
    )

    @Test
    fun `digest is stable for identical payloads`() {
        val first = ProvenanceHasher.sha256(sample())
        val second = ProvenanceHasher.sha256(sample())

        assertThat(first).isEqualTo(second)
        assertThat(first).hasLength(64)
        assertThat(first).matches("[0-9a-f]{64}")
    }

    @Test
    fun `digest changes when the scientific payload changes`() {
        val baseline = ProvenanceHasher.sha256(sample())
        val renamed = ProvenanceHasher.sha256(sample(tag = "North paddock A4"))

        assertThat(renamed).isNotEqualTo(baseline)
    }

    @Test
    fun `provenance record carries model identity and digests`() {
        val record = ProvenanceHasher.provenance(sample())

        assertThat(record.sampleId).isEqualTo("sample-1")
        assertThat(record.modelId).isEqualTo("soc-visible-color-v1")
        assertThat(record.modelVersion).isEqualTo("1.0.0-placeholder")
        assertThat(record.rawImageSha256).isEqualTo("a".repeat(64))
        assertThat(record.jpegImageSha256).isEqualTo("b".repeat(64))
        assertThat(record.appVersion).isEqualTo("2.4.0")
        assertThat(record.srsVersion).isEqualTo("SRS-SOC-2026-V1")
        assertThat(record.featureVectorVersion).isEqualTo(FeatureVector.FEATURE_VECTOR_VERSION)
        assertThat(record.digest).isEqualTo(ProvenanceHasher.sha256(sample()))
        assertThat(record.toExportMap()["digest"]).isEqualTo(record.digest)
    }

    @Test
    fun `known SHA-256 vector matches the FIPS 180 reference`() {
        assertThat(ProvenanceHasher.sha256Hex("abc"))
            .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad")
    }

    @Test
    fun `canonical payload is newline delimited and order independent of map iteration`() {
        val payload = ProvenanceHasher.canonicalPayload(sample())

        assertThat(payload).startsWith("id=sample-1\n")
        assertThat(payload).contains("features=[38.500000")
        assertThat(payload).contains("model=soc-visible-color-v1@1.0.0-placeholder\n")
        assertThat(payload.trim().lines().size).isGreaterThan(20)
    }
}
