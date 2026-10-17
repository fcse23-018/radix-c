package com.terrochroma.shared.ble

import android.content.Context
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.juul.kable.Advertisement
import com.juul.kable.Scanner
import com.juul.kable.Peripheral
import com.juul.kable.ScannerOptions
import com.juul.kable.characteristicOf
import com.juul.kable.connect
import com.juul.kable.peripheral
import com.juul.kable.write
import com.juul.kable.scan
import com.terrochroma.shared.ble.model.BLEDevice
import com.terrochroma.shared.ble.model.BLEConnectionState
import com.terrochroma.shared.ble.model.SpectrumData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class BLEServiceImpl(private val context: Context) : BLEService {

    private val _connectionState = MutableStateFlow(BLEConnectionState.DISCONNECTED)
    private val _currentDevice = MutableStateFlow<BLEDevice?>(null)
    
    private var scanner: Scanner? = null
    private var peripheral: Peripheral? = null
    private var isScanning = false
    private var continuousScanInterval: Long = 1000
    private var continuousScanJob: kotlinx.coroutines.Job? = null

    // Service UUID for MEMS NIR spectrometers
    companion object {
        private const val SPECTROMETER_SERVICE_UUID = "0000ffe0-0000-1000-8000-00805f9b34fb"
        private const val SPECTROMETER_CHARACTERISTIC_UUID = "0000ffe1-0000-1000-8000-00805f9b34fb"
    }

    override suspend fun initialize(): Result<Unit> {
        return try {
            // Check if device supports BLE
            if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
                return Result.failure(Exception("Device does not support BLE"))
            }

            // Check permissions
            val permissions = arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
            
            val missingPermissions = permissions.filter {
                ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
            }
            
            if (missingPermissions.isNotEmpty()) {
                return Result.failure(Exception("Missing BLE permissions: ${missingPermissions.joinToString()}"))
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun scanDevices(): Result<List<BLEDevice>> {
        return try {
            _connectionState.value = BLEConnectionState.SCANNING
            isScanning = true
            
            val devices = mutableListOf<BLEDevice>()
            
            val scanOptions = ScannerOptions {
                filters = listOf(
                    com.juul.kable.Filter.Service(SPECTROMETER_SERVICE_UUID)
                )
            }
            
            val advertisements = scan(scanOptions)
                .map { advertisement ->
                    BLEDevice(
                        id = advertisement.identifier,
                        name = advertisement.name ?: "Unknown Device",
                        rssi = advertisement.rssi,
                        address = advertisement.identifier
                    )
                }
            
            // Collect for 10 seconds
            kotlinx.coroutines.withTimeoutOrNull(10000) {
                advertisements.collect { device ->
                    if (!devices.any { it.id == device.id }) {
                        devices.add(device)
                    }
                }
            }
            
            stopScanning()
            _connectionState.value = BLEConnectionState.DISCONNECTED
            Result.success(devices)
        } catch (e: Exception) {
            _connectionState.value = BLEConnectionState.ERROR
            stopScanning()
            Result.failure(e)
        }
    }

    override suspend fun connect(deviceId: String): Result<Unit> {
        return try {
            _connectionState.value = BLEConnectionState.CONNECTING
            
            peripheral = peripheral(deviceId) {
                onDisconnect {
                    _connectionState.value = BLEConnectionState.DISCONNECTED
                    _currentDevice.value = null
                }
            }
            
            peripheral?.connect()
            
            // Wait for connection
            kotlinx.coroutines.delay(3000)
            
            _connectionState.value = BLEConnectionState.CONNECTED
            _currentDevice.value = BLEDevice(
                id = deviceId,
                name = "NIR Spectrometer",
                rssi = 0,
                address = deviceId
            )
            
            Result.success(Unit)
        } catch (e: Exception) {
            _connectionState.value = BLEConnectionState.ERROR
            Result.failure(e)
        }
    }

    override suspend fun disconnect(): Result<Unit> {
        return try {
            stopContinuousScan()
            
            peripheral?.let {
                it.disconnect()
            }
            
            _currentDevice.value = null
            _connectionState.value = BLEConnectionState.DISCONNECTED
            
            Result.success(Unit)
        } catch (e: Exception) {
            _connectionState.value = BLEConnectionState.ERROR
            Result.failure(e)
        }
    }

    override suspend fun readSpectrumData(): Result<SpectrumData> {
        return try {
            val deviceId = _currentDevice.value?.id
                ?: return Result.failure(Exception("No device connected"))
            
            val characteristic = characteristicOf(
                SPECTROMETER_SERVICE_UUID,
                SPECTROMETER_CHARACTERISTIC_UUID
            )
            
            val data = peripheral?.read(characteristic)
            
            val spectrumData = parseSpectrumData(data ?: byteArrayOf(), deviceId)
            
            Result.success(spectrumData)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun startContinuousScan(interval: Long): Result<Unit> {
        return try {
            continuousScanInterval = interval
            
            continuousScanJob = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                while (_connectionState.value == BLEConnectionState.CONNECTED) {
                    readSpectrumData()
                    kotlinx.coroutines.delay(continuousScanInterval)
                }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun stopContinuousScan(): Result<Unit> {
        return try {
            continuousScanJob?.cancel()
            continuousScanJob = null
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getConnectionState(): Flow<BLEConnectionState> {
        return _connectionState.asStateFlow()
    }

    private fun stopScanning() {
        isScanning = false
        scanner = null
    }

    private fun parseSpectrumData(data: ByteArray, deviceId: String): SpectrumData {
        // Parse spectrum data from spectrometer
        // Format depends on specific device
        // This is a simplified implementation
        
        val wavelengths = mutableListOf<Double>()
        val intensities = mutableListOf<Double>()
        
        // Generate placeholder spectrum data (900-2500 nm)
        for (i in 900..2500 step 10) {
            wavelengths.add(i.toDouble())
            intensities.add(Math.random() * 1.0)
        }
        
        return SpectrumData(
            wavelengths = wavelengths,
            intensities = intensities,
            timestamp = System.currentTimeMillis(),
            deviceId = deviceId
        )
    }
}
