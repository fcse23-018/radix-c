package com.soilcarbon.engine.presentation.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Agriculture
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soilcarbon.engine.core.Constants
import com.soilcarbon.engine.domain.model.UserRole
import com.soilcarbon.engine.presentation.components.SectionHeader
import com.soilcarbon.engine.presentation.theme.LeafGreen
import com.soilcarbon.engine.presentation.theme.SoilBrown
import com.soilcarbon.engine.presentation.theme.SoilDeep

@Composable
fun OnboardingScreen(
    onComplete: (UserRole, Double, Double, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var role by rememberSaveable { mutableStateOf(UserRole.FIELD_TECHNICIAN) }
    var consent by rememberSaveable { mutableStateOf(false) }
    var bulkDensity by rememberSaveable { mutableStateOf("1.30") }
    var depth by rememberSaveable { mutableStateOf("30") }
    var showValidation by remember { mutableStateOf(false) }

    val bulkValue = bulkDensity.toDoubleOrNull()
    val depthValue = depth.toDoubleOrNull()
    val bulkValid = bulkValue != null && bulkValue in 0.5..2.5
    val depthValid = depthValue != null && depthValue in 1.0..200.0
    val canFinish = consent && bulkValid && depthValid

    Scaffold(modifier = modifier.fillMaxSize()) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(SoilDeep, SoilBrown, LeafGreen.copy(alpha = 0.92f))
                        )
                    )
                    .padding(horizontal = 24.dp, vertical = 34.dp)
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.Science,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(Modifier.height(18.dp))
                    Text(
                        text = "SoilCarbon Engine",
                        color = androidx.compose.ui.graphics.Color.White,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Offline-first soil organic carbon analysis",
                        color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.86f),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "${Constants.SRS_DOCUMENT_ID} · v${Constants.SRS_VERSION}",
                        color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.70f),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                SectionHeader(
                    title = "Choose your working role",
                    subtitle = "The workspace adapts to the task. You can change this later in Settings."
                )
                UserRole.entries.forEach { entry ->
                    RoleCard(
                        role = entry,
                        selected = role == entry,
                        onSelect = { role = entry }
                    )
                }

                SectionHeader(
                    title = "Default soil properties",
                    subtitle = "Used for carbon-stock conversion (Mg C/ha). Adjust per sampling protocol."
                )
                OutlinedTextField(
                    value = bulkDensity,
                    onValueChange = { bulkDensity = it },
                    label = { Text("Bulk density (g/cm³)") },
                    singleLine = true,
                    isError = showValidation && !bulkValid,
                    supportingText = { Text("Valid range 0.5 – 2.5") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = depth,
                    onValueChange = { depth = it },
                    label = { Text("Sampling depth (cm)") },
                    singleLine = true,
                    isError = showValidation && !depthValid,
                    supportingText = { Text("Valid range 1 – 200") },
                    modifier = Modifier.fillMaxWidth()
                )

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Checkbox(checked = consent, onCheckedChange = { consent = it })
                        Column(Modifier.padding(top = 10.dp)) {
                            Text(
                                "I consent to local processing of soil images and location data.",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "Images and measurements are encrypted on this device. With " +
                                    "network covariate lookup enabled, capture sends the sample " +
                                    "coordinates to SoilGrids, Open-Meteo and Sentinel; no image or " +
                                    "sample record is uploaded. Disable network lookup in Settings " +
                                    "for a fully offline workflow.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.Verified,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(Modifier.size(10.dp))
                        Text(
                            "Research-grade screening. Field results must be validated against " +
                                "dry combustion (ISO 10694) before regulatory or carbon-credit use.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                if (showValidation && !canFinish) {
                    Text(
                        text = when {
                            !bulkValid -> "Enter a valid bulk density."
                            !depthValid -> "Enter a valid sampling depth."
                            !consent -> "Consent is required to continue."
                            else -> ""
                        },
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Button(
                    onClick = {
                        showValidation = true
                        if (canFinish) {
                            onComplete(
                                role,
                                bulkValue ?: Constants.DEFAULT_BULK_DENSITY,
                                depthValue ?: Constants.DEFAULT_DEPTH_CM,
                                true
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Icon(Icons.Outlined.Agriculture, contentDescription = null)
                    Spacer(Modifier.size(10.dp))
                    Text("Enter the field workspace")
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun RoleCard(role: UserRole, selected: Boolean, onSelect: () -> Unit) {
    Surface(
        onClick = onSelect,
        shape = MaterialTheme.shapes.large,
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        tonalElevation = if (selected) 3.dp else 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = selected, onClick = onSelect)
            Column(Modifier.weight(1f)) {
                Text(
                    role.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    role.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
