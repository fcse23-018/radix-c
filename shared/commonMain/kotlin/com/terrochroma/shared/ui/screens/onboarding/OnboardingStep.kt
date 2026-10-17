package com.terrochroma.shared.ui.screens.onboarding

import kotlinx.serialization.Serializable

@Serializable
data class OnboardingStep(
    val stepNumber: Int,
    val title: String,
    val description: String,
    val illustrationDescription: String, // For accessibility
    val actionText: String
)

enum class OnboardingStep {
    WELCOME,
    CALIBRATION_CARD,
    CAPTURE,
    RESULTS,
    READY
}

fun getOnboardingSteps(): List<OnboardingStep> {
    return listOf(
        OnboardingStep(
            stepNumber = 1,
            title = "Welcome to TerroChroma",
            description = "Turn your smartphone into a scientific-grade soil carbon analyzer. Measure Soil Organic Carbon (SOC) in under 2 seconds, fully offline.",
            illustrationDescription = "App logo with soil and leaf motif",
            actionText = "Get Started"
        ),
        OnboardingStep(
            stepNumber = 2,
            title = "Calibration Card",
            description = "Place the TerroChroma calibration card in the camera view. This ensures accurate color readings and D65 illuminant normalization.",
            illustrationDescription = "Calibration card with ArUco markers",
            actionText = "Next"
        ),
        OnboardingStep(
            stepNumber = 3,
            title = "Capture",
            description = "Point your camera at the soil sample. The app will detect the calibration card, check stability, and capture RAW image data.",
            illustrationDescription = "Camera viewfinder with calibration card detected",
            actionText = "Next"
        ),
        OnboardingStep(
            stepNumber = 4,
            title = "Results",
            description = "Get instant SOC percentage with confidence interval. Results include color features, texture analysis, and provenance hash for data integrity.",
            illustrationDescription = "SOC percentage result with confidence interval",
            actionText = "Next"
        ),
        OnboardingStep(
            stepNumber = 5,
            title = "You're Ready!",
            description = "Your phone is now a soil carbon laboratory. Start measuring and contribute to regenerative agriculture data.",
            illustrationDescription = "Ready state with checkmark",
            actionText = "Start Measuring"
        )
    )
}
