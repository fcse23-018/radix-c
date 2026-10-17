package com.terrochroma.shared.ui.screens.ble

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.terrochroma.shared.ble.model.BLEConnectionState
import com.terrochroma.shared.ui.theme.LeafGreen
import com.terrochroma.shared.ui.theme.SoilBrown

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BLEDeviceScreen(
    viewModel: BLEDeviceViewModel = viewModel(),
    onBack: () -> Unit = {}
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val scannedDevices by viewModel.scannedDevices.collectAsState()
    val currentDevice by viewModel.currentDevice.collectAsState()
    val spectrumData by viewModel.spectrumData.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("NIR Spectrometer") },
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
            // Connection status card
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
                            text = "Connection Status",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = when (connectionState) {
                                BLEConnectionState.DISCONNECTED -> "Disconnected"
                                BLEConnectionState.SCANNING -> "Scanning..."
                                BLEConnectionState.CONNECTING -> "Connecting..."
                                BLEConnectionState.CONNECTED -> "Connected to ${currentDevice?.name ?: "Unknown"}"
                                BLEConnectionState.ERROR -> "Error"
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    ConnectionStatusIcon(state = connectionState)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Device list
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
                            text = "Available Devices",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Button(
                            onClick = { viewModel.scanDevices() },
                            enabled = !isScanning && connectionState == BLEConnectionState.DISCONNECTED
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isScanning) "Scanning..." else "Scan")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn {
                        items(scannedDevices) { device ->
                            DeviceListItem(
                                device = device,
                                onConnect = { viewModel.connect(device.id) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Spectrum data display
            if (connectionState == BLEConnectionState.CONNECTED) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Spectrum Data",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Button(
                                onClick = { viewModel.readSpectrum() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = LeafGreen
                                )
                            ) {
                                Text("Read Spectrum")
                            }
                            Button(
                                onClick = { viewModel.disconnect() }
                            ) {
                                Text("Disconnect")
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        spectrumData?.let { data ->
                            SpectrumDataDisplay(data)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectionStatusIcon(state: BLEConnectionState) {
    when (state) {
        BLEConnectionState.DISCONNECTED -> {
            Icon(
                Icons.Default.BluetoothDisabled,
                contentDescription = "Disconnected",
                tint = MaterialTheme.colorScheme.error
            )
        }
        BLEConnectionState.SCANNING, BLEConnectionState.CONNECTING -> {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp
            )
        }
        BLEConnectionState.CONNECTED -> {
            Icon(
                Icons.Default.Bluetooth,
                contentDescription = "Connected",
                tint = LeafGreen
            )
        }
        BLEConnectionState.ERROR -> {
            Icon(
                Icons.Default.BluetoothDisabled,
                contentDescription = "Error",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun DeviceListItem(
    device: com.terrochroma.shared.ble.model.BLEDevice,
    onConnect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "RSSI: ${device.rssi} dBm",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Button(
                onClick = onConnect,
                modifier = Modifier.height(36.dp)
            ) {
                Text("Connect")
            }
        }
    }
}

@Composable
private fun SpectrumDataDisplay(data: com.terrochroma.shared.ble.model.SpectrumData) {
    Column {
        Text(
            text = "Wavelengths: ${data.wavelengths.size} points",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = "Range: ${data.wavelengths.firstOrNull()?.toInt()} - ${data.wavelengths.lastOrNull()?.toInt()} nm",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = "Timestamp: ${java.util.Date(data.timestamp)}",
            style = MaterialTheme.typography.bodySmall
        )
    }
}
