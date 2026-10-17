package com.terrochroma.shared.ui.screens.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.terrochroma.shared.map.model.getMapPinColorForSOC
import com.terrochroma.shared.ui.theme.LeafGreen
import com.terrochroma.shared.ui.theme.SoilBrown

@Composable
fun MapScreen(
    viewModel: MapViewModel = viewModel(),
    onBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedSample by viewModel.selectedSample.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Map") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is MapUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is MapUiState.Ready -> {
                    MapContent(
                        selectedSample = selectedSample,
                        onSelectSample = { viewModel.selectSample(it) },
                        onClearSelection = { viewModel.clearSelection() },
                        onCenterOnSample = { viewModel.centerOnSample(it) }
                    )
                }
                is MapUiState.Error -> {
                    ErrorState(
                        message = state.message,
                        onDismiss = { viewModel.dismissError() }
                    )
                }
            }

            // Sample detail bottom sheet
            if (selectedSample != null) {
                SampleDetailBottomSheet(
                    sample = selectedSample,
                    onClose = { viewModel.clearSelection() }
                )
            }
        }
    }
}

@Composable
private fun MapContent(
    selectedSample: com.terrochroma.shared.domain.model.SoilSample?,
    onSelectSample: (com.terrochroma.shared.domain.model.SoilSample) -> Unit,
    onClearSelection: () -> Unit,
    onCenterOnSample: (com.terrochroma.shared.domain.model.SoilSample) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Map view placeholder
        Card(
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Interactive Map",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "SOC Color-coded Pins",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    // Legend
                    MapLegend()
                }
            }
        }
    }
}

@Composable
private fun MapLegend() {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "SOC Legend",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            LegendItem(color = com.terrochroma.shared.map.model.MapPinColor.RED, label = "< 1% SOC")
            LegendItem(color = com.terrochroma.shared.map.model.MapPinColor.GREEN, label = "1-2% SOC")
            LegendItem(color = com.terrochroma.shared.map.model.MapPinColor.CYAN, label = "2-3% SOC")
            LegendItem(color = com.terrochroma.shared.map.model.MapPinColor.YELLOW, label = "3-4% SOC")
            LegendItem(color = com.terrochroma.shared.map.model.MapPinColor.BLUE, label = "> 4% SOC")
        }
    }
}

@Composable
private fun LegendItem(color: com.terrochroma.shared.map.model.MapPinColor, label: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .background(getLegendColor(color))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

private fun getLegendColor(color: com.terrochroma.shared.map.model.MapPinColor): androidx.compose.ui.graphics.Color {
    return when (color) {
        com.terrochroma.shared.map.model.MapPinColor.RED -> androidx.compose.ui.graphics.Color.Red
        com.terrochroma.shared.map.model.MapPinColor.GREEN -> androidx.compose.ui.graphics.Color.Green
        com.terrochroma.shared.map.model.MapPinColor.CYAN -> androidx.compose.ui.graphics.Color.Cyan
        com.terrochroma.shared.map.model.MapPinColor.YELLOW -> androidx.compose.ui.graphics.Color.Yellow
        com.terrochroma.shared.map.model.MapPinColor.BLUE -> androidx.compose.ui.graphics.Color.Blue
    }
}

@Composable
private fun SampleDetailBottomSheet(
    sample: com.terrochroma.shared.domain.model.SoilSample,
    onClose: () -> Unit
) {
    BottomSheet(
        onDismissRequest = onClose,
        sheetContent = {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
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
                            text = "Sample Details",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = onClose) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "SOC: ${sample.socPercentage}%",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Confidence: ±${sample.confidenceInterval}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "SOM: ${sample.somPercentage}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (sample.moistureContent != null) {
                        Text(
                            text = "Moisture: ${sample.moistureContent}%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    Text(
                        text = "Date: ${java.util.Date(sample.timestamp)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorState(
    message: String,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.error
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onError
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text("Retry")
            }
        }
    }
}
