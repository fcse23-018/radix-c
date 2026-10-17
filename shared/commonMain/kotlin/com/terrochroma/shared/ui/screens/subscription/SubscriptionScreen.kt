package com.terrochroma.shared.ui.screens.subscription

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.terrochroma.shared.ui.theme.LeafGreen
import com.terrochroma.shared.ui.theme.SoilBrown

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen(
    viewModel: SubscriptionViewModel = viewModel(),
    onBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val subscriptionStatus by viewModel.subscriptionStatus.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Subscription") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            when (val state = uiState) {
                is SubscriptionUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
                is SubscriptionUiState.Success -> {
                    SubscriptionContent(
                        offers = state.offers,
                        currentSubscription = state.currentSubscription,
                        config = state.config,
                        onPurchase = { viewModel.purchaseSubscription(it) },
                        onRestore = { viewModel.restorePurchases() },
                        onManage = { viewModel.manageSubscription() }
                    )
                }
                is SubscriptionUiState.Purchasing -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Processing purchase...")
                    }
                }
                is SubscriptionUiState.PurchaseSuccess -> {
                    SuccessMessage(
                        message = "Purchase successful!",
                        onDismiss = { viewModel.dismissError() }
                    )
                }
                is SubscriptionUiState.Restoring -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Restoring purchases...")
                    }
                }
                is SubscriptionUiState.RestoreSuccess -> {
                    SuccessMessage(
                        message = "Purchases restored successfully!",
                        onDismiss = { viewModel.dismissError() }
                    )
                }
                is SubscriptionUiState.Error -> {
                    ErrorMessage(
                        message = state.message,
                        onDismiss = { viewModel.dismissError() }
                    )
                }
            }
        }
    }
}

@Composable
private fun SubscriptionContent(
    offers: List<com.terrochroma.shared.billing.model.SubscriptionOffer>,
    currentSubscription: com.terrochroma.shared.domain.model.Subscription?,
    config: com.terrochroma.shared.domain.model.SubscriptionConfig,
    onPurchase: (String) -> Unit,
    onRestore: () -> Unit,
    onManage: () -> Unit
) {
    // Current Subscription Status
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (currentSubscription?.isActive == true) {
                LeafGreen
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Current Plan",
                style = MaterialTheme.typography.titleLarge,
                color = if (currentSubscription?.isActive == true) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = currentSubscription?.tier?.name ?: "Free Tier",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = if (currentSubscription?.isActive == true) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            if (currentSubscription?.isActive == true) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onManage,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(
                        text = "Manage Subscription",
                        color = LeafGreen
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Subscription Offers
    Text(
        text = "Upgrade to Pro",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(16.dp))

    offers.forEach { offer ->
        SubscriptionOfferCard(
            offer = offer,
            isSubscribed = currentSubscription?.isActive == true,
            onPurchase = { onPurchase(offer.offerId) }
        )
        Spacer(modifier = Modifier.height(12.dp))
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Restore Purchases
    OutlinedButton(
        onClick = onRestore,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Restore Purchases")
    }
}

@Composable
private fun SubscriptionOfferCard(
    offer: com.terrochroma.shared.billing.model.SubscriptionOffer,
    isSubscribed: Boolean,
    onPurchase: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSubscribed) {
                LeafGreen
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = offer.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isSubscribed) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                Text(
                    text = offer.price,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isSubscribed) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        LeafGreen
                    }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = offer.description,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isSubscribed) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            if (offer.hasFreeTrial) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${offer.trialPeriodDays}-day free trial",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSubscribed) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        LeafGreen
                    }
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onPurchase,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSubscribed,
                colors = if (isSubscribed) {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    ButtonDefaults.buttonColors(
                        containerColor = LeafGreen
                    )
                }
            ) {
                Text(
                    text = if (isSubscribed) "Subscribed" else "Subscribe",
                    color = if (isSubscribed) LeafGreen else MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
private fun SuccessMessage(
    message: String,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = LeafGreen
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    text = "OK",
                    color = LeafGreen
                )
            }
        }
    }
}

@Composable
private fun ErrorMessage(
    message: String,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.error
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onError,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onError
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text(
                    text = "OK",
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
