package com.terrochroma.shared.ui.screens.subscription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.terrochroma.shared.billing.BillingService
import com.terrochroma.shared.billing.model.BillingPeriod
import com.terrochroma.shared.billing.model.SubscriptionOffer
import com.terrochroma.shared.domain.model.Subscription
import com.terrochroma.shared.domain.model.SubscriptionTier
import com.terrochroma.shared.domain.model.canCaptureSample
import com.terrochroma.shared.domain.model.getSubscriptionConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SubscriptionViewModel(
    private val billingService: BillingService
) : ViewModel() {

    private val _uiState = MutableStateFlow<SubscriptionUiState>(SubscriptionUiState.Loading)
    val uiState: StateFlow<SubscriptionUiState> = _uiState.asStateFlow()

    private val _paywallState = MutableStateFlow<PaywallState>(PaywallState.Hidden)
    val paywallState: StateFlow<PaywallState> = _paywallState.asStateFlow()

    private val _subscriptionStatus = MutableStateFlow<Subscription?>(null)
    val subscriptionStatus: StateFlow<Subscription?> = _subscriptionStatus.asStateFlow()

    private var currentSubscription: Subscription? = null
    private var capturesThisMonth = 0

    init {
        initializeBilling()
        observeSubscriptionStatus()
    }

    private fun initializeBilling() {
        viewModelScope.launch {
            _uiState.value = SubscriptionUiState.Loading
            try {
                billingService.initialize()
                loadSubscriptionOffers()
                loadCurrentSubscription()
            } catch (e: Exception) {
                _uiState.value = SubscriptionUiState.Error(e.message ?: "Failed to initialize billing")
            }
        }
    }

    private fun observeSubscriptionStatus() {
        viewModelScope.launch {
            billingService.getSubscriptionStatus().collect { subscription ->
                currentSubscription = subscription
                _subscriptionStatus.value = subscription
            }
        }
    }

    private fun loadSubscriptionOffers() {
        viewModelScope.launch {
            try {
                val offers = billingService.getSubscriptionOffers().getOrNull() ?: emptyList()
                _uiState.value = SubscriptionUiState.Success(
                    offers = offers,
                    currentSubscription = currentSubscription,
                    config = getSubscriptionConfig()
                )
            } catch (e: Exception) {
                _uiState.value = SubscriptionUiState.Error(e.message ?: "Failed to load offers")
            }
        }
    }

    private fun loadCurrentSubscription() {
        viewModelScope.launch {
            try {
                val subscription = billingService.getCurrentSubscription().getOrNull()
                currentSubscription = subscription
                _subscriptionStatus.value = subscription
            } catch (e: Exception) {
                // Continue with null subscription
            }
        }
    }

    fun purchaseSubscription(offerId: String) {
        viewModelScope.launch {
            _uiState.value = SubscriptionUiState.Purchasing
            try {
                val result = billingService.purchaseSubscription(offerId).getOrNull()
                if (result?.isSuccess == true) {
                    loadCurrentSubscription()
                    _uiState.value = SubscriptionUiState.PurchaseSuccess
                } else {
                    _uiState.value = SubscriptionUiState.Error(result?.error ?: "Purchase failed")
                }
            } catch (e: Exception) {
                _uiState.value = SubscriptionUiState.Error(e.message ?: "Purchase failed")
            }
        }
    }

    fun restorePurchases() {
        viewModelScope.launch {
            _uiState.value = SubscriptionUiState.Restoring
            try {
                val subscriptions = billingService.restorePurchases().getOrNull() ?: emptyList()
                currentSubscription = subscriptions.firstOrNull { it.isActive }
                _subscriptionStatus.value = currentSubscription
                _uiState.value = SubscriptionUiState.RestoreSuccess
            } catch (e: Exception) {
                _uiState.value = SubscriptionUiState.Error(e.message ?: "Restore failed")
            }
        }
    }

    fun manageSubscription() {
        viewModelScope.launch {
            try {
                billingService.manageSubscription()
            } catch (e: Exception) {
                _uiState.value = SubscriptionUiState.Error(e.message ?: "Failed to open subscription management")
            }
        }
    }

    fun showPaywall(reason: PaywallReason) {
        _paywallState.value = PaywallState.Visible(reason)
    }

    fun hidePaywall() {
        _paywallState.value = PaywallState.Hidden
    }

    fun incrementCaptureCount() {
        capturesThisMonth++
        checkPaywallTrigger()
    }

    private fun checkPaywallTrigger() {
        val config = getSubscriptionConfig()
        val tier = currentSubscription?.tier ?: SubscriptionTier.FREE

        if (!canCaptureSample(tier, capturesThisMonth, config)) {
            showPaywall(PaywallReason.LimitReached)
        }
    }

    fun dismissPaywall() {
        hidePaywall()
    }

    fun dismissError() {
        val currentState = _uiState.value
        if (currentState is SubscriptionUiState.Error) {
            loadSubscriptionOffers()
        }
    }
}

sealed class SubscriptionUiState {
    object Loading : SubscriptionUiState()
    data class Success(
        val offers: List<SubscriptionOffer>,
        val currentSubscription: Subscription?,
        val config: com.terrochroma.shared.domain.model.SubscriptionConfig
    ) : SubscriptionUiState()
    object Purchasing : SubscriptionUiState()
    object PurchaseSuccess : SubscriptionUiState()
    object Restoring : SubscriptionUiState()
    object RestoreSuccess : SubscriptionUiState()
    data class Error(val message: String) : SubscriptionUiState()
}

sealed class PaywallState {
    object Hidden : PaywallState()
    data class Visible(val reason: PaywallReason) : PaywallState()
}

enum class PaywallReason {
    LimitReached,
    TrialExpired,
    UserInitiated
}
