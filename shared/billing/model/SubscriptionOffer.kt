package com.terrochroma.shared.billing.model

import kotlinx.serialization.Serializable

@Serializable
data class SubscriptionOffer(
    val offerId: String,
    val productId: String,
    val title: String,
    val description: String,
    val price: String,
    val currencyCode: String,
    val billingPeriod: BillingPeriod,
    val trialPeriodDays: Int?,
    val hasFreeTrial: Boolean
)

@Serializable
enum class BillingPeriod {
    MONTHLY,
    YEARLY
}

@Serializable
data class PurchaseResult(
    val isSuccess: Boolean,
    val subscriptionId: String?,
    val error: String?
)
