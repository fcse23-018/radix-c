package com.terrochroma.shared.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class SubscriptionTier {
    FREE,
    PRO_MONTHLY,
    PRO_YEARLY
}

@Serializable
data class Subscription(
    val tier: SubscriptionTier,
    val isActive: Boolean,
    val startDate: Long,
    val endDate: Long?,
    val autoRenew: Boolean,
    val purchaseToken: String?,
    val platform: Platform
)

@Serializable
enum class Platform {
    ANDROID,
    IOS
}

@Serializable
data class SubscriptionConfig(
    val freeMonthlyCaptures: Int = 10,
    val proMonthlyPrice: String = "$9.99",
    val proYearlyPrice: String = "$99.99",
    val proYearlyDiscount: Int = 17, // 17% discount for yearly
    val trialDays: Int = 7
)

fun getSubscriptionConfig(): SubscriptionConfig = SubscriptionConfig()

fun canCaptureSample(
    currentTier: SubscriptionTier,
    capturesThisMonth: Int,
    config: SubscriptionConfig = getSubscriptionConfig()
): Boolean {
    return when (currentTier) {
        SubscriptionTier.FREE -> capturesThisMonth < config.freeMonthlyCaptures
        SubscriptionTier.PRO_MONTHLY, SubscriptionTier.PRO_YEARLY -> true
    }
}
