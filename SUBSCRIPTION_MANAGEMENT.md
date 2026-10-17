# Subscription Management UI - Implementation Summary

## Overview
Complete subscription management system with paywall, subscription selection, and trial flow implementation.

## Components Implemented

### 1. SubscriptionViewModel
**File**: `shared/commonMain/kotlin/com/terrochroma/shared/ui/screens/subscription/SubscriptionViewModel.kt`

ViewModel managing subscription state and billing operations:

#### State Management
- `SubscriptionUiState`: Loading, Success, Purchasing, PurchaseSuccess, Restoring, RestoreSuccess, Error
- `PaywallState`: Hidden, Visible (with reason: LimitReached, TrialExpired, UserInitiated)
- `subscriptionStatus`: Flow of current subscription

#### Key Features
- **Billing Initialization**: Async initialization of billing service
- **Load Subscription Offers**: Fetch available subscription plans
- **Load Current Subscription**: Fetch user's active subscription
- **Purchase Subscription**: Handle subscription purchase flow
- **Restore Purchases**: Restore previous purchases
- **Manage Subscription**: Deep-link to native store UI
- **Paywall Trigger**: Check capture limit and show paywall
- **Capture Count Tracking**: Track monthly captures for free tier

#### Integration with Billing Service
- Uses existing `BillingService` expect/actual implementation
- Observes subscription status via Flow
- Handles purchase results and errors

### 2. SubscriptionScreen
**File**: `shared/commonMain/kotlin/com/terrochroma/shared/ui/screens/subscription/SubscriptionScreen.kt`

Main subscription management screen:

#### UI Components
- **TopAppBar**: With back navigation
- **Current Plan Card**: Shows active subscription with "Manage" button
- **Subscription Offers**: List of available plans with pricing
- **Subscribe Buttons**: For each plan (disabled if already subscribed)
- **Restore Purchases**: Outlined button to restore purchases
- **Success/Error Messages**: Feedback for purchase/restore operations

#### Features
- Displays current subscription status
- Shows all available subscription offers
- Highlights subscribed plan
- Shows trial information
- Handles loading, purchasing, and error states
- Material 3 design with TerroChroma color scheme

### 3. PaywallScreen
**File**: `shared/commonMain/kotlin/com/terrochroma/shared/ui/screens/subscription/PaywallScreen.kt`

Bottom sheet paywall for subscription prompts:

#### Paywall Reasons
- **LimitReached**: User reached monthly free tier limit
- **TrialExpired**: Free trial has expired
- **UserInitiated**: User manually triggered paywall

#### UI Components
- **Dialog-based Bottom Sheet**: Rounded corners, dismissible
- **Header**: Title with close button
- **Contextual Message**: Based on paywall reason
- **Quick Subscribe Buttons**: Monthly and yearly plans
- **Yearly Plan Highlighted**: Shows 17% savings and 7-day trial
- **View All Plans**: Navigate to full subscription screen

#### Features
- Soft paywall (user can dismiss)
- Contextual messaging
- Quick subscribe options
- Visual hierarchy (yearly plan highlighted)
- Dismissible on back press or tap outside

### 4. Navigation Integration
**Updated**: `shared/commonMain/kotlin/com/terrochroma/shared/ui/TerroChromaApp.kt`

- Added subscription screen route
- Integrated with existing navigation graph
- Settings screen now navigates to subscription

**Updated**: `shared/commonMain/kotlin/com/terrochroma/shared/ui/screens/settings/SettingsScreen.kt`

- "Upgrade to Pro" button now navigates to subscription screen

## Subscription Flow

### 1. Free Tier Flow
1. User starts with Free Tier
2. Can capture up to configured limit (default: 10/month)
3. After limit reached, paywall shows with "LimitReached" reason
4. User can dismiss paywall or subscribe
5. If dismissed, no more captures until month reset

### 2. Trial Flow
1. User selects yearly plan with 7-day trial
2. Trial starts immediately
3. Full access during trial period
4. After trial expires, paywall shows with "TrialExpired" reason
5. User can subscribe or cancel

### 3. Purchase Flow
1. User navigates to subscription screen
2. Views available plans
3. Taps "Subscribe" on desired plan
4. Shows "Processing purchase..." loading state
5. Native billing UI appears (Google Play / App Store)
6. On success: Shows success message
7. On error: Shows error message with retry option

### 4. Restore Flow
1. User taps "Restore Purchases"
2. Shows "Restoring purchases..." loading state
3. Billing service queries previous purchases
4. On success: Shows success message
5. Updates current subscription status

## Design System Integration

### Colors Used
- **LeafGreen** (#2E7D32): Primary action buttons, subscribed states
- **SoilBrown** (#5D4037): Navigation bar, titles
- **DeepSoil** (#3E2723): Secondary text
- **MaterialTheme.colorScheme.error**: Error states
- **MaterialTheme.colorScheme.onPrimary**: Text on green backgrounds

### Material 3 Components
- Cards with rounded corners
- OutlinedButton for secondary actions
- Button for primary actions
- Dialog for paywall
- TopAppBar with navigation
- CircularProgressIndicator for loading states

## Integration Points

### With Capture Flow
```kotlin
// Before capture
val canCapture = canCaptureSample(currentTier, capturesThisMonth, config)
if (!canCapture) {
    viewModel.showPaywall(PaywallReason.LimitReached)
    return
}

// After successful capture
viewModel.incrementCaptureCount()
```

### With Settings
```kotlin
Button(onClick = { navController.navigate("subscription") }) {
    Text("Upgrade to Pro")
}
```

### With Home Screen
```kotlin
// Show capture count for free tier
Text("Captures this month: $capturesThisMonth / $freeLimit")
```

## Configuration

Subscription limits and pricing are configured in:
```kotlin
data class SubscriptionConfig(
    val freeMonthlyCaptures: Int = 10,
    val proMonthlyPrice: String = "$9.99",
    val proYearlyPrice: String = "$99.99",
    val proYearlyDiscount: Int = 17,
    val trialDays: Int = 7
)
```

## Testing Recommendations

### Unit Tests
- ViewModel state transitions
- Paywall trigger logic
- Capture count tracking
- Subscription eligibility checks

### UI Tests
- Navigation to subscription screen
- Purchase button interactions
- Paywall display and dismissal
- Restore purchases flow
- Error state handling

### Integration Tests
- End-to-end purchase flow
- Billing service integration
- Subscription status updates
- Capture limit enforcement

## Future Enhancements

1. **Annual Plans Display**: Show savings calculation dynamically
2. **Promotional Offers**: Support for discount codes and promotions
3. **Family Plans**: Multi-user subscription support
4. **Grace Period**: Allow limited access after subscription expiry
5. **Subscription Analytics**: Track conversion rates and retention
6. **A/B Testing**: Test different paywall messages and pricing

## Notes

- Paywall is "soft" - users can dismiss and continue exploring
- Yearly plan is highlighted as recommended (17% savings + trial)
- All billing operations are async with proper error handling
- Subscription status is observed via Flow for real-time updates
- Material 3 design follows TerroChroma color system
- Native billing UI provides familiar purchase experience
