package com.terrochroma.shared.camera.model

import kotlinx.serialization.Serializable

@Serializable
data class CameraConfig(
    val iso: Int = 100,
    val exposureTime: Long = 10000000L, // nanoseconds
    val whiteBalanceMode: WhiteBalanceMode = WhiteBalanceMode.AUTO,
    val focusMode: FocusMode = FocusMode.AUTO,
    val flashMode: FlashMode = FlashMode.OFF,
    val rawFormat: RawFormat = RawFormat.DNG
)

@Serializable
enum class WhiteBalanceMode {
    AUTO,
    MANUAL,
    DAYLIGHT,
    CLOUDY,
    FLUORESCENT,
    INCANDESCENT
}

@Serializable
enum class FocusMode {
    AUTO,
    FIXED,
    MACRO,
    CONTINUOUS
}

@Serializable
enum class FlashMode {
    OFF,
    ON,
    AUTO,
    TORCH
}

@Serializable
enum class RawFormat {
    DNG, // Digital Negative (Android)
    BAYER // Bayer RAW (iOS)
}
