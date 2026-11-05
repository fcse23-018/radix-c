package com.soilcarbon.engine.data.export

import com.google.common.truth.Truth.assertThat
import com.soilcarbon.engine.domain.model.FeatureVector
import com.soilcarbon.engine.domain.model.Prediction
import com.soilcarbon.engine.domain.model.SampleStatus
import com.soilcarbon.engine.domain.model.SoilSample
import org.junit.Test

class ExportMapperTest {

    @Test
    fun `export record carries status degradation provenance and model identity`() {
        val properties = ExportMapper.properties(sample())

        assertThat(ExportMapper.REQUIRED_KEYS).containsAtLeast(
            "status",
            "report_status",
            "degraded",
            "feature_provenance",
            "model_id",
            "model_version"
        )
        ExportMapper.REQUIRED_KEYS.forEach { key ->
            assertThat(properties).containsKey(key)
        }
        assertThat(properties["status"]).isEqualTo(SampleStatus.ANALYZED.name)
        assertThat(properties["report_status"]).isEqualTo(Prediction.STATUS_DEMO_NOT_VALID)
        assertThat(properties["degraded"]).isEqualTo(true)
        assertThat(properties["model_id"]).isEqualTo("soc-ridge-v1")
        assertThat(properties["model_version"]).isEqualTo("1.0.0")
        @Suppress("UNCHECKED_CAST")
        val provenance = properties["feature_provenance"] as Map<String, String>
        assertThat(provenance["ndvi_satellite"]).isEqualTo("DEFAULT")
        assertThat(provenance["L_star"]).isEqualTo("MEASURED")
    }

    @Test
    fun `CSV header and row stay in lockstep`() {
        val header = ExportMapper.CSV_HEADER
        val row = ExportMapper.csvRow(sample())

        val headerColumns = parseCsv(header)
        val rowValues = parseCsv(row)

        assertThat(headerColumns).hasSize(45)
        assertThat(rowValues).hasSize(headerColumns.size)
        assertThat(headerColumns).containsAtLeast(
            "status",
            "model_id",
            "model_version",
            "report_status",
            "degraded",
            "feature_provenance"
        ).inOrder()
    }

    @Test
    fun `CSV quoting survives commas quotes and newlines`() {
        val sample = sample(tag = "plot \"A\", north", notes = "first line\nsecond line")
        val row = ExportMapper.csvRow(sample)
        val values = parseCsv(row)

        assertThat(row).contains("\"plot \"\"A\"\", north\"")
        assertThat(row).contains("\"first line\nsecond line\"")
        assertThat(values[1]).isEqualTo("plot \"A\", north")
        assertThat(values.last()).isEqualTo("first line\nsecond line")
    }

    @Test
    fun `properties include the eight feature values`() {
        val properties = ExportMapper.properties(sample())

        assertThat(properties["L_star"]).isEqualTo(42.0)
        assertThat(properties["a_star"]).isEqualTo(6.0)
        assertThat(properties["b_star"]).isEqualTo(14.0)
        assertThat(properties["munsell_value"]).isEqualTo(5.0)
        assertThat(properties["glcm_contrast"]).isEqualTo(0.35)
        assertThat(properties["glcm_energy"]).isEqualTo(0.25)
        assertThat(properties["ndvi_satellite"]).isEqualTo(0.35)
        assertThat(properties["clay_content_percent"]).isEqualTo(25.0)
    }

    private fun sample(
        tag: String = "plot-1",
        notes: String = "baseline"
    ) = SoilSample(
        id = "sample-1",
        tag = tag,
        notes = notes,
        status = SampleStatus.ANALYZED,
        features = FeatureVector(
            lStar = 42.0,
            aStar = 6.0,
            bStar = 14.0,
            munsellValue = 5.0,
            glcmContrast = 0.35,
            glcmEnergy = 0.25,
            ndviSatellite = 0.35,
            clayContentPercent = 25.0
        ),
        prediction = Prediction(
            socPercent = 2.0,
            somPercent = 3.448,
            socLower95 = 1.8,
            socUpper95 = 2.2,
            uncertainty95 = 0.2,
            confidenceScore = 0.8,
            carbonStockMgPerHa = 12.5,
            modelId = "soc-ridge-v1",
            modelVersion = "1.0.0",
            inferenceMillis = 12,
            reportStatus = Prediction.STATUS_DEMO_NOT_VALID,
            degraded = true,
            provenance = mapOf(
                "L_star" to "MEASURED",
                "ndvi_satellite" to "DEFAULT"
            )
        )
    )

    /** Minimal RFC 4180 parser used to prove the row still matches the header. */
    private fun parseCsv(line: String): List<String> {
        val values = mutableListOf<String>()
        val current = StringBuilder()
        var quoted = false
        var index = 0
        while (index < line.length) {
            val char = line[index]
            if (quoted) {
                if (char == '"') {
                    if (index + 1 < line.length && line[index + 1] == '"') {
                        current.append('"')
                        index++
                    } else {
                        quoted = false
                    }
                } else {
                    current.append(char)
                }
            } else {
                when (char) {
                    '"' -> quoted = true
                    ',' -> {
                        values += current.toString()
                        current.clear()
                    }
                    else -> current.append(char)
                }
            }
            index++
        }
        values += current.toString()
        return values
    }
}
