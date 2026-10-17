package com.terrochroma.shared.ui.screens.sync

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Sync
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
fun SyncSettingsScreen(
    viewModel: SyncSettingsViewModel = viewModel(),
    onBack: () -> Unit = {}
) {
    val syncEnabled by viewModel.syncEnabled.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val lastSyncTime by viewModel.lastSyncTime.collectAsState()
    val syncFrequency by viewModel.syncFrequency.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cloud Sync") },
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
        ) {
            // Sync status card
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Sync Status",
                            style = MaterialTheme.typography.titleLarge
                        )
                        SyncStatusIndicator(status = syncStatus)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (lastSyncTime > 0) {
                            "Last synced: ${java.util.Date(lastSyncTime)}"
                        } else {
                            "Never synced"
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sync toggle
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Enable Cloud Sync",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Sync samples with ISRIC SoilGrids",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Switch(
                        checked = syncEnabled,
                        onCheckedChange = { viewModel.toggleSyncEnabled() }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sync frequency
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Sync Frequency",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SyncFrequencyOptions(
                        selectedFrequency = syncFrequency,
                        onFrequencySelected = { viewModel.setSyncFrequency(it) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Manual sync button
            Button(
                onClick = { viewModel.manualSync() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LeafGreen
                ),
                enabled = syncEnabled && syncStatus != SyncStatus.Syncing
            ) {
                Icon(Icons.Default.Sync, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (syncStatus == SyncStatus.Syncing) "Syncing..." else "Sync Now"
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Information
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
                        text = "About Cloud Sync",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Cloud sync uses ISRIC SoilGrids WCS to fetch global soil data for comparison with your measurements. This feature requires an internet connection.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun SyncStatusIndicator(status: SyncStatus) {
    when (status) {
        SyncStatus.Idle -> {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = "Sync idle",
                tint = LeafGreen
            )
        }
        SyncStatus.Syncing -> {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp
            )
        }
        SyncStatus.Error -> {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = "Sync error",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun SyncFrequencyOptions(
    selectedFrequency: SyncFrequency,
    onFrequencySelected: (SyncFrequency) -> Unit
) {
    Column {
        SyncFrequencyItem(
            frequency = SyncFrequency.Manual,
            selected = selectedFrequency == SyncFrequency.Manual,
            onClick = { onFrequencySelected(SyncFrequency.Manual) }
        )
        SyncFrequencyItem(
            frequency = SyncFrequency.Daily,
            selected = selectedFrequency == SyncFrequency.Daily,
            onClick = { onFrequencySelected(SyncFrequency.Daily) }
        )
        SyncFrequencyItem(
            frequency = SyncFrequency.Weekly,
            selected = selectedFrequency == SyncFrequency.Weekly,
            onClick = { onFrequencySelected(SyncFrequency.Weekly) }
        )
    }
}

@Composable
private fun SyncFrequencyItem(
    frequency: SyncFrequency,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = frequency.displayName,
            style = MaterialTheme.typography.bodyMedium
        )
        RadioButton(
            selected = selected,
            onClick = onClick
        )
    }
}

enum class SyncStatus {
    Idle,
    Syncing,
    Error
}

enum class SyncFrequency(val displayName: String) {
    Manual("Manual only"),
    Daily("Daily"),
    Weekly("Weekly")
}
