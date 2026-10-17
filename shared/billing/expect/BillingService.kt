package com.terrochroma.shared.billing

import com.terrochroma.shared.billing.model.SubscriptionOffer
import com.terrochroma.shared.billing.model.PurchaseResult
import com.terrochroma.shared.domain.model.Subscription
import kotlinx.coroutines.flow.Flow

interface BillingService {
    suspend fun initialize(): Result<Unit>
    suspend fun getSubscriptionOffers(): Result<List<SubscriptionOffer>>
    suspend fun purchaseSubscription(offerId: String): Result<PurchaseResult>
    suspend fun restorePurchases(): Result<List<Subscription>>
    suspend fun getCurrentSubscription(): Result<Subscription?>
    suspend fun manageSubscription(): Result<Unit>
    fun getSubscriptionStatus(): Flow<Subscription?>
}
