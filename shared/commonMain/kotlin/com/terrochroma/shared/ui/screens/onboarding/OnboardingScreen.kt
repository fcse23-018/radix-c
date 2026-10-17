package com.terrochroma.shared.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.terrochroma.shared.ui.theme.LeafGreen
import com.terrochroma.shared.ui.theme.SoilBrown
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel = viewModel(),
    onComplete: () -> Unit = {}
) {
    val currentStep by viewModel.currentStep.collectAsState()
    val isCompleted by viewModel.isCompleted.collectAsState()
    val totalSteps = viewModel.getTotalSteps()
    val coroutineScope = rememberCoroutineScope()

    // Auto-complete when onboarding is finished
    LaunchedEffect(isCompleted) {
        if (isCompleted) {
            onComplete()
        }
    }

    val pagerState = rememberPagerState(pageCount = { totalSteps })

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(SoilBrown)
        ) {
            // Step indicator dots
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(totalSteps) { index ->
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (index == currentStep) LeafGreen else Color.White.copy(alpha = 0.3f)
                            )
                    )
                    if (index < totalSteps - 1) {
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }
            }

            // Page content
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                userScrollEnabled = false
            ) { page ->
                OnboardingPage(
                    step = getOnboardingSteps()[page],
                    stepNumber = page + 1,
                    totalSteps = totalSteps
                )
            }

            // Bottom actions
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Skip button
                    TextButton(
                        onClick = { viewModel.skipOnboarding() },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Color.White
                        )
                    ) {
                        Text("Skip")
                    }

                    // Next/Complete button
                    Button(
                        onClick = {
                            viewModel.nextStep()
                            if (currentStep < totalSteps - 1) {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(currentStep + 1)
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LeafGreen
                        ),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text(
                            text = if (currentStep == totalSteps - 1) "Start Measuring" else "Next",
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingPage(
    step: OnboardingStep,
    stepNumber: Int,
    totalSteps: Int
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Illustration placeholder
        Card(
            modifier = Modifier
                .size(200.dp)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = LeafGreen
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                when (stepNumber) {
                    1 -> {
                        // Welcome - Logo
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = step.illustrationDescription,
                            tint = Color.White,
                            modifier = Modifier.size(80.dp)
                        )
                    }
                    2 -> {
                        // Calibration Card
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        )
                    }
                    3 -> {
                        // Capture
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        )
                    }
                    4 -> {
                        // Results
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "2.5%",
                                style = MaterialTheme.typography.displaySmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "SOC",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White
                            )
                        }
                    }
                    5 -> {
                        // Ready
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = step.illustrationDescription,
                            tint = Color.White,
                            modifier = Modifier.size(80.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Step number
        Text(
            text = "Step $stepNumber of $totalSteps",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Title
        Text(
            text = step.title,
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Description
        Text(
            text = step.description,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.9f),
            textAlign = TextAlign.Center
        )
    }
}
