package com.terrochroma.shared.ui.screens.ble

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.terrochroma.shared.ble.BLEService
import com.terrochroma.shared.ble.model.BLEConnectionState
import com.terrochroma.shared.ble.model.BLEDevice
import com.terrochroma.shared.ble.model.SpectrumData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BLEDeviceViewModel(
    private val bleService: BLEService
) : ViewModel() {

    private val _connectionState = MutableStateFlow(BLEConnectionState.DISCONNECTED)
    val connectionState: StateFlow<BLEConnectionState> = _connectionState.asStateFlow()

    private val _scannedDevices = MutableStateFlow<List<BLEDevice>>(emptyList())
    val scannedDevices: StateFlow<List<BLEDevice>> = _scannedDevices.asStateFlow()

    private val _currentDevice = MutableStateFlow<BLEDevice?>(null)
    val currentDevice: StateFlow<BLEDevice?> = _currentDevice.asStateFlow()

    private val _spectrumData = MutableStateFlow<SpectrumData?>(null)
    val spectrumData: StateFlow<SpectrumData?> = _spectrumData.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    init {
        viewModelScope.launch {
            bleService.getConnectionState().collect { state ->
                _connectionState.value = state
            }
        }
    }

    fun scanDevices() {
        viewModelScope.launch {
            _isScanning.value = true
            try {
                val result = bleService.scanDevices()
                if (result.isSuccess) {
                    _scannedDevices.value = result.getOrNull() ?: emptyList()
                }
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun connect(deviceId: String) {
        viewModelScope.launch {
            try {
                val result = bleService.connect(deviceId)
                if (result.isSuccess) {
                    _currentDevice.value = _scannedDevices.value.find { it.id == deviceId }
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            try {
                bleService.disconnect()
                _currentDevice.value = null
                _spectrumData.value = null
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun readSpectrum() {
        viewModelScope.launch {
            try {
                val result = bleService.readSpectrumData()
                if (result.isSuccess) {
                    _spectrumData.value = result.getOrNull()
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}
