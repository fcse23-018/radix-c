package com.soilcarbon.engine.domain.capture

import com.google.common.truth.Truth.assertThat
import com.soilcarbon.engine.domain.model.AppSettings
import org.junit.Test

class CaptureGateTest {

    @Test
    fun `RAW policy fails closed when the camera returned JPEG only`() {
        val required = AppSettings(requireRawCapture = true)

        assertThat(CaptureGate.rawCaptureError(required, rawSaved = false)).isNotNull()
        assertThat(CaptureGate.rawCaptureError(required, rawSaved = true)).isNull()
    }

    @Test
    fun `disabling the RAW policy allows a JPEG-only capture`() {
        val optional = AppSettings(requireRawCapture = false)

        assertThat(CaptureGate.rawCaptureError(optional, rawSaved = false)).isNull()
    }

    @Test
    fun `GPS policy only blocks when the setting is enabled`() {
        assertThat(CaptureGate.gpsError(AppSettings(requireGps = true), gpsAvailable = false)).isNotNull()
        assertThat(CaptureGate.gpsError(AppSettings(requireGps = false), gpsAvailable = false)).isNull()
        assertThat(CaptureGate.gpsError(AppSettings(requireGps = true), gpsAvailable = true)).isNull()
    }

    @Test
    fun `default patch gate requires four of six measurable patches`() {
        assertThat(CaptureGate.patchGatePasses(observed = 3)).isFalse()
        assertThat(CaptureGate.patchGatePasses(observed = 4)).isTrue()
        assertThat(CaptureGate.patchError(observed = 3)).contains("at least 4")
        assertThat(CaptureGate.patchError(observed = 4)).isNull()
    }

    @Test
    fun `patch gate minimum is configurable`() {
        assertThat(CaptureGate.patchGatePasses(observed = 5, minimum = 6)).isFalse()
        assertThat(CaptureGate.patchGatePasses(observed = 6, minimum = 6)).isTrue()
        assertThat(CaptureGate.patchError(observed = 2, minimum = 2)).isNull()
        assertThat(CaptureGate.patchError(observed = 1, minimum = 2, total = 6))
            .contains("Only 1/6 calibration patches")
    }
}
