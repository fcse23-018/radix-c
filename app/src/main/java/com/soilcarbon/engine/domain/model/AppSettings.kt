package com.soilcarbon.engine.domain.model

data class AppSettings(
    val role: UserRole = UserRole.FIELD_TECHNICIAN,
    val bulkDensityGcm3: Double = 1.30,
    val depthCm: Double = 30.0,
    val requireRawCapture: Boolean = true,
    val requireGps: Boolean = true,
    val minLux: Float = 50f,
    val maxTiltDegrees: Float = 10f,
    val enableGpuDelegate: Boolean = true,
    val allowNetworkSync: Boolean = true,
    val autoSync: Boolean = true,
    val lastSyncAtMillis: Long? = null,
    val lastSyncMessage: String? = null,
    val onboardingComplete: Boolean = false,
    val gdprConsentAtMillis: Long? = null,
    val apiBaseUrl: String = "https://rest.isric.org/",
    val modelSourceUrl: String = "",
    /** Sentinel Hub token. Blank means satellite NDVI is unavailable and must be DEFAULT. */
    val sentinelToken: String = ""
) {
    fun toSoilProperties(): SoilProperties = SoilProperties(bulkDensityGcm3, depthCm)
}

data class SyncReport(
    val attempted: Int,
    val succeeded: Int,
    val failed: Int,
    val message: String,
    val finishedAtMillis: Long = System.currentTimeMillis()
)

data class ModelInfo(
    val metadata: ModelMetadata,
    val sha256: String,
    val bytes: Long,
    val loaded: Boolean,
    val fallbackReason: String? = null
)
