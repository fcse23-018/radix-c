package com.soilcarbon.engine.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FactCheck
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.soilcarbon.engine.core.Constants
import com.soilcarbon.engine.domain.model.AppSettings
import com.soilcarbon.engine.domain.model.UserRole
import com.soilcarbon.engine.presentation.capture.CaptureScreen
import com.soilcarbon.engine.presentation.dashboard.DashboardScreen
import com.soilcarbon.engine.presentation.main.MainViewModel
import com.soilcarbon.engine.presentation.map.MapScreen
import com.soilcarbon.engine.presentation.onboarding.OnboardingScreen
import com.soilcarbon.engine.presentation.review.ReviewScreen
import com.soilcarbon.engine.presentation.settings.SettingsScreen
import com.soilcarbon.engine.presentation.theme.LeafGreen
import com.soilcarbon.engine.presentation.theme.SoilBrown
import com.soilcarbon.engine.presentation.theme.SoilDeep

enum class AppDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    CAPTURE("capture", "Capture", Icons.Outlined.CameraAlt),
    REVIEW("review", "Review", Icons.AutoMirrored.Outlined.FactCheck),
    DASHBOARD("dashboard", "Dashboard", Icons.Outlined.Dashboard),
    MAP("map", "Map", Icons.Outlined.Map),
    SETTINGS("settings", "Settings", Icons.Outlined.Settings)
}

/** Role-based navigation sets defined by the SRS workspace model. */
fun destinationsFor(role: UserRole): List<AppDestination> = when (role) {
    UserRole.FIELD_TECHNICIAN -> listOf(
        AppDestination.CAPTURE,
        AppDestination.REVIEW,
        AppDestination.SETTINGS
    )

    UserRole.AGRONOMIST,
    UserRole.DATA_SCIENTIST -> listOf(
        AppDestination.DASHBOARD,
        AppDestination.REVIEW,
        AppDestination.MAP,
        AppDestination.SETTINGS
    )
}

@Composable
fun SoilCarbonApp(viewModel: MainViewModel = hiltViewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val current = settings
    when {
        current == null -> BrandedSplash()
        !current.onboardingComplete -> OnboardingScreen(
            onComplete = viewModel::completeOnboarding
        )

        else -> MainScaffold(settings = current)
    }
}

@Composable
private fun MainScaffold(settings: AppSettings) {
    val navController = rememberNavController()
    val destinations = remember(settings.role) { destinationsFor(settings.role) }
    val startDestination = destinations.first().route
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    var pendingReviewId by rememberSaveable { mutableStateOf<String?>(null) }

    // A role change can remove the tab the user is standing on; fall back to the new home tab.
    LaunchedEffect(destinations) {
        val activeRoute = navController.currentDestination?.route
        if (activeRoute != null && destinations.none { it.route == activeRoute }) {
            navController.navigate(startDestination) {
                popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                destinations.forEach { destination ->
                    NavigationBarItem(
                        selected = currentRoute == destination.route,
                        onClick = {
                            if (currentRoute != destination.route) {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = { Icon(destination.icon, contentDescription = destination.label) },
                        label = { Text(destination.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(AppDestination.CAPTURE.route) {
                CaptureScreen(
                    onOpenReview = {
                        navController.navigate(AppDestination.REVIEW.route) {
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(AppDestination.REVIEW.route) {
                ReviewScreen(
                    focusSampleId = pendingReviewId,
                    onFocusConsumed = { pendingReviewId = null }
                )
            }
            composable(AppDestination.DASHBOARD.route) {
                DashboardScreen()
            }
            composable(AppDestination.MAP.route) {
                MapScreen(
                    onOpenReview = { sampleId ->
                        pendingReviewId = sampleId
                        navController.navigate(AppDestination.REVIEW.route) {
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(AppDestination.SETTINGS.route) {
                SettingsScreen()
            }
        }
    }
}

@Composable
fun BrandedSplash() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(SoilDeep, SoilBrown, LeafGreen.copy(alpha = 0.92f)))
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(74.dp)
                    .background(Color.White.copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Science,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(38.dp)
                )
            }
            Spacer(Modifier.height(20.dp))
            Text(
                text = "SoilCarbon Engine",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Offline-first soil organic carbon analysis",
                color = Color.White.copy(alpha = 0.84f),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(28.dp))
            CircularProgressIndicator(
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.24f),
                modifier = Modifier.size(28.dp),
                strokeWidth = 3.dp
            )
            Spacer(Modifier.height(24.dp))
            Text(
                text = "${Constants.SRS_DOCUMENT_ID} · v${Constants.SRS_VERSION}",
                color = Color.White.copy(alpha = 0.65f),
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}
