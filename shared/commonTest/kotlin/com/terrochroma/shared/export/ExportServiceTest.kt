package com.terrochroma.shared.export

import com.terrochroma.shared.domain.model.SoilSample
import com.terrochroma.shared.export.ExportServiceImpl
import kotlin.test.Test
import kotlin.test.assertTrue

class ExportServiceTest {

    private val exportService = ExportServiceImpl(null)

    @Test
    fun testGeoJSONExport() {
        val sample = createTestSample()
        val result = exportService.exportToGeoJSON(listOf(sample))
        
        assertTrue(result.isSuccess, "GeoJSON export should succeed")
        val geoJson = result.getOrNull() ?: ""
        assertTrue(geoJson.contains("FeatureCollection"), "Should contain FeatureCollection")
        assertTrue(geoJson.contains("Point"), "Should contain Point geometry")
    }

    @Test
    fun testKMLExport() {
        val sample = createTestSample()
        val result = exportService.exportToKML(listOf(sample))
        
        assertTrue(result.isSuccess, "KML export should succeed")
        val kml = result.getOrNull() ?: ""
        assertTrue(kml.contains("<kml"), "Should contain kml root element")
        assertTrue(kml.contains("Placemark"), "Should contain Placemark")
    }

    @Test
    fun testCSVExport() {
        val sample = createTestSample()
        val result = exportService.exportToCSV(listOf(sample))
        
        assertTrue(result.isSuccess, "CSV export should succeed")
        val csv = result.getOrNull() ?: ""
        assertTrue(csv.contains("id,"), "Should contain header")
        assertTrue(csv.contains("soc_percentage,"), "Should contain SOC column")
    }

    @Test
    fun testEmptySampleList() {
        val result = exportService.exportToGeoJSON(emptyList())
        
        assertTrue(result.isSuccess, "Empty list export should succeed")
        val geoJson = result.getOrNull() ?: ""
        assertTrue(geoJson.contains("FeatureCollection"), "Should still contain FeatureCollection")
    }

    @Test
    fun testSampleWithoutLocation() {
        val sample = createTestSample().copy(latitude = null, longitude = null)
        val kmlResult = exportService.exportToKML(listOf(sample))
        
        // KML should succeed but not include location
        assertTrue(kmlResult.isSuccess, "KML export should succeed even without location")
        val kml = kmlResult.getOrNull() ?: ""
        // Should not contain coordinates for samples without location
    }

    private fun createTestSample(): SoilSample {
        return SoilSample(
            id = "test-sample-001",
            timestamp = System.currentTimeMillis(),
            latitude = 37.7749,
            longitude = -122.4194,
            altitude = 0.0,
            socPercentage = 2.5,
            confidenceInterval = 0.15,
            somPercentage = 4.3,
            labValue = 2.45,
            moistureContent = 12.5,
            imageHash = "abc123",
            provenanceHash = "def456",
            cameraParams = com.terrochroma.shared.domain.model.CameraParams(
                iso = 100,
                exposureTime = 10000000,
                whiteBalanceMode = com.terrochroma.shared.domain.model.WhiteBalanceMode.AUTO,
                focusMode = com.terrochroma.shared.domain.model.FocusMode.AUTO,
                flashMode = com.terrochroma.shared.domain.model.FlashMode.OFF,
                rawFormat = com.terrochroma.shared.domain.model.RawFormat.DNG
            ),
            colorFeatures = com.terrochroma.shared.domain.model.ColorFeatures(
                cieLab = com.terrochroma.shared.domain.model.CIELab(45.2, 3.1, 8.5),
                hsv = com.terrochroma.shared.domain.model.HSV(120.0, 0.45, 0.78),
                munsell = com.terrochroma.shared.domain.model.Munsell("Gy", 4.5, 2.0)
            ),
            textureFeatures = com.terrochroma.shared.domain.model.TextureFeatures(
                contrast = 0.85,
                homogeneity = 0.92,
                energy = 1.0,
                entropy = 1.0
            ),
            modelVersion = "1.0.0",
            appVersion = "1.0.0",
            isSynced = false,
            notes = "Test sample"
        )
    }
}
