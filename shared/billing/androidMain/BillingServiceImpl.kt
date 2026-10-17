package com.terrochroma.shared.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import com.terrochroma.shared.billing.model.BillingPeriod
import com.terrochroma.shared.billing.model.PurchaseResult
import com.terrochroma.shared.billing.model.SubscriptionOffer
import com.terrochroma.shared.domain.model.Platform
import com.terrochroma.shared.domain.model.Subscription
import com.terrochroma.shared.domain.model.SubscriptionTier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BillingServiceImpl(private val context: Context) : BillingService,
    BillingClientStateListener,
    PurchasesUpdatedListener {

    private var billingClient: BillingClient? = null
    private val _subscriptionStatus = MutableStateFlow<Subscription?>(null)
    private var activity: Activity? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    override suspend fun initialize(): Result<Unit> {
        return try {
            billingClient = BillingClient.newBuilder(context)
                .setListener(this)
                .enablePendingPurchases()
                .build()

            billingClient?.startConnection(this)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun onBillingSetupFinished(billingResult: BillingResult) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            // Billing client is ready
            queryCurrentSubscription()
        }
    }

    override fun onBillingServiceDisconnected() {
        // Try to reconnect
        billingClient?.startConnection(this)
    }

    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: MutableList<Purchase>?
    ) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        }
    }

    override suspend fun getSubscriptionOffers(): Result<List<SubscriptionOffer>> {
        return try {
            val params = QueryProductDetailsParams.newBuilder()
                .setProductList(
                    listOf(
                        QueryProductDetailsParams.Product.newBuilder()
                            .setProductId("pro_monthly")
                            .setProductType(BillingClient.ProductType.SUBS)
                            .build(),
                        QueryProductDetailsParams.Product.newBuilder()
                            .setProductId("pro_yearly")
                            .setProductType(BillingClient.ProductType.SUBS)
                            .build()
                    )
                )
                .build()

            val result = billingClient?.queryProductDetails(params)
            if (result?.billingResult?.responseCode == BillingClient.BillingResponseCode.OK) {
                val offers = result.productDetailsList?.map { product ->
                    SubscriptionOffer(
                        offerId = product.productId,
                        productId = product.productId,
                        title = product.name,
                        description = product.description,
                        price = product.oneTimePurchaseOfferDetails?.formattedPrice ?: "",
                        currencyCode = product.oneTimePurchaseOfferDetails?.priceCurrencyCode ?: "USD",
                        billingPeriod = if (product.productId.contains("monthly")) {
                            BillingPeriod.MONTHLY
                        } else {
                            BillingPeriod.YEARLY
                        },
                        trialPeriodDays = 7,
                        hasFreeTrial = true
                    )
                } ?: emptyList()
                Result.success(offers)
            } else {
                Result.failure(Exception("Failed to query subscription offers"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun purchaseSubscription(offerId: String): Result<PurchaseResult> {
        return try {
            val productDetailsParams = QueryProductDetailsParams.newBuilder()
                .setProductList(
                    listOf(
                        QueryProductDetailsParams.Product.newBuilder()
                            .setProductId(offerId)
                            .setProductType(BillingClient.ProductType.SUBS)
                            .build()
                    )
                )
                .build()

            val productDetailsResult = billingClient?.queryProductDetails(productDetailsParams)
            val productDetails = productDetailsResult?.productDetailsList?.firstOrNull()

            if (productDetails != null) {
                val billingFlowParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(
                        listOf(
                            BillingFlowParams.ProductDetailsParams.newBuilder()
                                .setProductDetails(productDetails)
                                .build()
                        )
                    )
                    .build()

                val billingResult = billingClient?.launchBillingFlow(activity, billingFlowParams)

                if (billingResult?.responseCode == BillingClient.BillingResponseCode.OK) {
                    Result.success(PurchaseResult(true, offerId, null))
                } else {
                    Result.success(PurchaseResult(false, null, "Billing flow failed"))
                }
            } else {
                Result.failure(Exception("Product details not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun restorePurchases(): Result<List<Subscription>> {
        return try {
            val params = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()

            val result = billingClient?.queryPurchasesAsync(params)
            val purchases = result?.purchasesList ?: emptyList()

            val subscriptions = purchases.map { purchase ->
                val tier = if (purchase.products.contains("pro_yearly")) {
                    SubscriptionTier.PRO_YEARLY
                } else {
                    SubscriptionTier.PRO_MONTHLY
                }

                Subscription(
                    tier = tier,
                    isActive = purchase.purchaseState == Purchase.PurchaseState.PURCHASED,
                    startDate = purchase.purchaseTime,
                    endDate = null,
                    autoRenew = purchase.isAutoRenewing,
                    purchaseToken = purchase.purchaseToken,
                    platform = Platform.ANDROID
                )
            }

            Result.success(subscriptions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getCurrentSubscription(): Result<Subscription?> {
        return try {
            val subscriptions = restorePurchases().getOrNull() ?: emptyList()
            Result.success(subscriptions.firstOrNull { it.isActive })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun manageSubscription(): Result<Unit> {
        return try {
            val intent = billingClient?.launchBillingFlow(
                activity,
                BillingFlowParams.newBuilder().build()
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getSubscriptionStatus(): Flow<Subscription?> {
        return _subscriptionStatus.asStateFlow()
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()

                billingClient?.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        // Purchase acknowledged
                        queryCurrentSubscription()
                    }
                }
            }
        }
    }

    private fun queryCurrentSubscription() {
        scope.launch {
            val subscription = getCurrentSubscription().getOrNull()
            _subscriptionStatus.value = subscription
        }
    }

    fun setActivity(activity: Activity) {
        this.activity = activity
    }
}
