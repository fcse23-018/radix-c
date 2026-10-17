package com.terrochroma.shared.map.model

import kotlinx.serialization.Serializable

@Serializable
data class MapPin(
    val sampleId: String,
    val latitude: Double,
    val longitude: Double,
    val socPercentage: Double,
    val color: MapPinColor
)

enum class MapPinColor {
    RED,    // < 1% SOC
    GREEN,  // 1-2% SOC
    CYAN,   // 2-3% SOC
    YELLOW, // 3-4% SOC
    BLUE    // > 4% SOC
}

fun getMapPinColorForSOC(soc: Double): MapPinColor {
    return when {
        soc < 1.0 -> MapPinColor.RED
        soc < 2.0 -> MapPinColor.GREEN
        soc < 3.0 -> MapPinColor.CYAN
        soc < 4.0 -> MapPinColor.YELLOW
        else -> MapPinColor.BLUE
    }
}
