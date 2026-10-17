package com.terrochroma.shared.ble.model

import kotlinx.serialization.Serializable

@Serializable
data class BLEDevice(
    val id: String,
    val name: String,
    val rssi: Int,
    val address: String
)

@Serializable
data class SpectrumData(
    val wavelengths: List<Double>, // 900-2500 nm
    val intensities: List<Double>,
    val timestamp: Long,
    val deviceId: String
)

enum class BLEConnectionState {
    DISCONNECTED,
    SCANNING,
    CONNECTING,
    CONNECTED,
    ERROR
}

@Serializable
data class NIRSpectrometerConfig(
    val wavelengthRange: ClosedRange<Double> = 900.0..2500.0,
    val resolution: Double = 1.0, // nm
    val integrationTime: Long = 100, // ms
    val scanCount: Int = 1
)
