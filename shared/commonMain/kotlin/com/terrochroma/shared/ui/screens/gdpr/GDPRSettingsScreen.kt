package com.terrochroma.shared.ui.screens.gdpr

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavController
import com.terrochroma.shared.ui.theme.LeafGreen
import com.terrochroma.shared.ui.theme.SoilBrown

@Composable
fun GDPRSettingsScreen(navController: NavController) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var autoDeleteEnabled by remember { mutableStateOf(false) }
    var autoDeleteMonths by remember { mutableStateOf(12) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Privacy & Data",
            style = MaterialTheme.typography.displaySmall,
            color = SoilBrown,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Privacy Policy
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Privacy Policy",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "View our complete privacy policy to understand how we collect, use, and protect your data.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { /* Open privacy policy URL */ },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LeafGreen
                    )
                ) {
                    Text("View Privacy Policy")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Data Management
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Data Management",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))

                GDPRActionItem(
                    title = "Export Your Data",
                    description = "Download all your data in CSV format",
                    onClick = { showExportDialog = true }
                )

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                GDPRActionItem(
                    title = "Delete All Data",
                    description = "Permanently delete all samples and local data",
                    onClick = { showDeleteDialog = true },
                    isDestructive = true
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Data Retention Settings
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Data Retention",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto-Delete Old Data",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Automatically delete samples older than",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = autoDeleteEnabled,
                        onCheckedChange = { autoDeleteEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = LeafGreen,
                            checkedTrackColor = LeafGreen.copy(alpha = 0.5f)
                        )
                    )
                }

                if (autoDeleteEnabled) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Delete after",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$autoDeleteMonths months",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(onClick = { 
                                if (autoDeleteMonths < 36) autoDeleteMonths += 6
                            }) {
                                Text("+")
                            }
                            IconButton(onClick = { 
                                if (autoDeleteMonths > 6) autoDeleteMonths -= 6
                            }) {
                                Text("-")
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Image Storage Settings
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Image Storage",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))

                var deleteRawImages by remember { mutableStateOf(false) }
                GDPRToggle(
                    title = "Delete Raw Images After Processing",
                    description = "Keep only processed features to save space",
                    checked = deleteRawImages,
                    onCheckedChange = { deleteRawImages = it }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Analytics Settings
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Analytics",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))

                var analyticsEnabled by remember { mutableStateOf(true) }
                GDPRToggle(
                    title = "Anonymous Analytics",
                    description = "Help improve the app with anonymous usage data",
                    checked = analyticsEnabled,
                    onCheckedChange = { analyticsEnabled = it }
                )

                Spacer(modifier = Modifier.height(12.dp))

                var crashReportingEnabled by remember { mutableStateOf(true) }
                GDPRToggle(
                    title = "Crash Reporting",
                    description = "Send crash reports to help us fix bugs",
                    checked = crashReportingEnabled,
                    onCheckedChange = { crashReportingEnabled = it }
                )
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete All Data?") },
            text = {
                Text(
                    "This will permanently delete all your soil samples, images, and local data. This action cannot be undone."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        // Trigger deletion
                        // dataDeletionService.deleteAllUserData()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete All Data")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Export Dialog
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Export Your Data") },
            text = {
                Text(
                    "Your data will be exported in CSV format. You can choose where to save the file."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExportDialog = false
                        // Trigger export
                        // dataDeletionService.exportUserData()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LeafGreen
                    )
                ) {
                    Text("Export")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun GDPRActionItem(
    title: String,
    description: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun GDPRToggle(
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
        Column(modifier = Modifier.weight(1f)) {
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
