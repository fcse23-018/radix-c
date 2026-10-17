package com.terrochroma.shared.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Capture : Screen("capture")
    object Map : Screen("map")
    object Library : Screen("library")
    object Settings : Screen("settings")
    object Onboarding : Screen("onboarding")
    object Subscription : Screen("subscription")
}
