package com.terrochroma.shared.ui.screens.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.terrochroma.shared.ui.theme.LeafGreen
import com.terrochroma.shared.ui.theme.SoilBrown

@Composable
fun ConsentScreen(
    navController: NavController,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    var analyticsEnabled by remember { mutableStateOf(false) }
    var locationEnabled by remember { mutableStateOf(true) }
    var crashReportingEnabled by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // Header
        Text(
            text = "Privacy & Data Collection",
            style = MaterialTheme.typography.headlineMedium,
            color = SoilBrown,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "To provide soil carbon sensing, we collect certain data. Please review and choose your preferences.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Data Collection Summary
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Data We Collect",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                DataCollectionItem(
                    icon = "📍",
                    title = "Location Data",
                    description = "GPS coordinates for sample location (optional)"
                )
                DataCollectionItem(
                    icon = "📷",
                    title = "Camera Images",
                    description = "RAW images for soil analysis (encrypted)"
                )
                DataCollectionItem(
                    icon = "🔒",
                    title = "Device Information",
                    description = "Device model, camera parameters (for security)"
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Consent Options
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Your Preferences",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Location Toggle
                ConsentToggle(
                    title = "Enable Location",
                    description = "Allow GPS tagging of soil samples",
                    checked = locationEnabled,
                    onCheckedChange = { locationEnabled = it }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Analytics Toggle
                ConsentToggle(
                    title = "Anonymous Analytics",
                    description = "Help improve the app with anonymous usage data",
                    checked = analyticsEnabled,
                    onCheckedChange = { analyticsEnabled = it }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Crash Reporting Toggle
                ConsentToggle(
                    title = "Crash Reporting",
                    description = "Send crash reports to help us fix bugs",
                    checked = crashReportingEnabled,
                    onCheckedChange = { crashReportingEnabled = it }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Security Note
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = LeafGreen
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "🔐 Your Data is Protected",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• All data encrypted with AES-256\n• Stored only on your device\n• Never sold to third parties\n• You can delete your data anytime",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onDecline,
                modifier = Modifier.weight(1f)
            ) {
                Text("Decline")
            }

            Button(
                onClick = {
                    // Save preferences
                    onAccept()
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LeafGreen
                )
            ) {
                Text("Accept")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Privacy Policy Link
        TextButton(
            onClick = { /* Open privacy policy */ }
        ) {
            Text(
                "Read full Privacy Policy",
                style = MaterialTheme.typography.bodySmall,
                color = LeafGreen
            )
        }
    }
}

@Composable
private fun DataCollectionItem(
    icon: String,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = icon,
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ConsentToggle(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = LeafGreen,
                checkedTrackColor = LeafGreen.copy(alpha = 0.5f)
            )
        )
    }
}
