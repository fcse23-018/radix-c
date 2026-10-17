# BLE Spectrometer Integration - Implementation Summary

## Overview
Complete BLE spectrometer integration using Kable library for multiplatform Bluetooth Low Energy support, enabling connection to MEMS NIR spectrometers (900-2500 nm) and data fusion with camera measurements.

## Components Implemented

### 1. BLE Service Interface
**File**: `shared/ble/expect/BLEService.kt`

Common interface for platform-specific BLE implementations:
- `initialize()` - Initialize BLE and check permissions
- `scanDevices()` - Scan for nearby spectrometers
- `connect()` - Connect to specific device
- `disconnect()` - Disconnect from device
- `readSpectrumData()` - Read spectrum data
- `startContinuousScan()` - Start continuous spectrum scanning
- `stopContinuousScan()` - Stop continuous scanning
- `getConnectionState()` - Flow of connection state

### 2. BLE Data Models
**File**: `shared/ble/model/BLEDevice.kt`

Data models for BLE operations:
- `BLEDevice`: Device ID, name, RSSI, address
- `SpectrumData`: Wavelengths (900-2500 nm), intensities, timestamp
- `BLEConnectionState`: Enum (DISCONNECTED, SCANNING, CONNECTING, CONNECTED, ERROR)
- `NIRSpectrometerConfig`: Config (wavelength range, resolution, integration time)

### 3. Android BLE Service (Kable)
**File**: `shared/ble/androidMain/BLEServiceImpl.kt`

Kable-based implementation for Android:
- Permission checks (BLUETOOTH_SCAN, BLUETOOTH_CONNECT)
- BLE capability check
- Kable scanner with service UUID filter
- Device connection with timeout
- Characteristic reading
- Continuous scan with coroutine job
- Connection state management
- RSSI signal strength tracking

**Service UUID**: `0000ffe0-0000-1000-8000-00805f9b34fb`
**Characteristic UUID**: `0000ffe1-0000-1000-8000-00805f9b34fb`

### 4. iOS BLE Service (Kable)
**File**: `shared/ble/iosMain/BLEServiceImpl.kt`

Kable-based implementation for iOS:
- CoreBluetooth wrapper via Kable
- Service UUID filtering
- Device connection with disconnect handler
- Characteristic reading
- Continuous scan support
- Background scanning limitations noted
- Info.plist requires NSBluetoothAlwaysUsageDescription

### 5. Data Fusion Logic
**File**: `shared/commonMain/kotlin/com/terrochroma/shared/ml/fusion/DataFusion.kt`

Data fusion for combining camera and spectrometer data:
- **NIR Feature Extraction**: Red edge, water band, vegetation index
- **Moisture Estimation**: From water absorption bands
- **EPO Correction**: External Parameter Orthogonalization for moisture correction
- **Feature Fusion**: Combines color, texture, and NIR features
- **Quality Calculation**: Data quality metric

**Key Features:**
- Red edge (680-730 nm) - vegetation health
- Water absorption bands (1400-1500 nm, 1900-2000 nm)
- Vegetation index (NDVI-like)
- Moisture content estimation
- EPO removes moisture-related variation from color features

### 6. BLE Device Screen
**File**: `shared/commonMain/kotlin/com/terrochroma/shared/ui/screens/ble/BLEDeviceScreen.kt`

Complete UI for BLE spectrometer:
- Connection status card with icon
- Device list with RSSI indicators
- Scan button with loading state
- Connect button per device
- Spectrum data display
- Disconnect button
- Visual indicators for all states

### 7. BLE Device ViewModel
**File**: `shared/commonMain/kotlin/com/terrochroma/shared/ui/screens/ble/BLEDeviceViewModel.kt`

ViewModel managing BLE state:
- Connection state from BLE service
- Scanned devices list
- Current device reference
- Spectrum data
- Scanning state
- All BLE operations exposed as methods

## Dependencies Required

```gradle
// Common
implementation("com.juul.kable:kable:0.23.0")

// Android (already in AndroidManifest.xml)
// BLUETOOTH_SCAN, BLUETOOTH_CONNECT permissions

// iOS (in Info.plist)
// NSBluetoothAlwaysUsageDescription
```

## Permissions

### Android
```xml
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-feature android:name="android.hardware.bluetooth_le" android:required="false" />
```

### iOS
```xml
<key>NSBluetoothAlwaysUsageDescription</key>
<string>TerroChroma needs Bluetooth to connect to NIR spectrometers for enhanced soil carbon measurements.</string>
```

## Wavelength Range

**MEMS NIR Spectrometer**: 900-2500 nm

Key spectral regions:
- **Visible-NIR boundary**: 700-800 nm
- **Red edge**: 680-730 nm (vegetation health)
- **Water absorption 1**: 1400-1500 nm
- **Water absorption 2**: 1900-2000 nm
- **Organic matter**: 1600-1800 nm

## Data Fusion Process

1. **Capture**: Camera captures color features, spectrometer captures NIR spectrum
2. **Extract**: Extract NIR features (red edge, water band, vegetation index)
3. **Estimate**: Estimate moisture content from water absorption bands
4. **Correct**: Apply EPO correction to color features based on moisture
5. **Fuse**: Combine corrected color, texture, and NIR features
6. **Quality**: Calculate data quality metric
7. **Inference**: Feed fused features to ML model

## Integration Points

### With Capture Screen
```kotlin
// Add BLE status indicator
if (bleService.isConnected()) {
    FusionModeIndicator(enabled = true)
}

// Use fused data when spectrometer connected
val fusedFeatures = dataFusion.fuseFeatures(
    colorFeatures,
    textureFeatures,
    spectrumData
)
mlService.predict(fusedFeatures)
```

### With ML Service
```kotlin
// Fused feature vector includes:
// - Color features (corrected for moisture)
// - Texture features
// - NIR features (red edge, water band, vegetation index)
// - Moisture content
// - Data quality metric
```

## Usage Example

### Scan and Connect
```kotlin
val bleService = BLEServiceImpl(context)
bleService.initialize()

// Scan for devices
val devices = bleService.scanDevices()
// Returns list of BLEDevice with name, RSSI

// Connect to device
bleService.connect(deviceId)
// State changes: CONNECTING -> CONNECTED
```

### Read Spectrum
```kotlin
val spectrumData = bleService.readSpectrumData()
// Returns SpectrumData with wavelengths (900-2500 nm) and intensities
```

### Data Fusion
```kotlin
val dataFusion = DataFusion()
val fusedFeatures = dataFusion.fuseFeatures(
    colorFeatures = cameraColorFeatures,
    textureFeatures = glcmTextureFeatures,
    spectrumData = spectrometerData
)
// Returns FusedFeatures with all combined features
```

## Spectrometer Configuration

Default configuration:
- Wavelength range: 900-2500 nm
- Resolution: 1 nm
- Integration time: 100 ms
- Scan count: 1

Configuration can be adjusted per device requirements.

## Error Handling

- Permission errors: Checked at initialization
- Connection failures: State changes to ERROR
- Scan timeout: 10 second limit
- Characteristic read failures: Returns error result
- Device not found: Returns error in scan result

## Battery Optimization

- Bluetooth scanning limited to 10 seconds
- Continuous scan can be stopped manually
- Connection disconnected when not in use
- Service UUID filtering reduces unnecessary discoveries

## Testing Notes

**Hardware Required:**
- MEMS NIR spectrometer (900-2500 nm)
- Compatible service UUIDs
- Android device with BLE support
- iOS device with BLE support

**Mock Testing:**
- Mock BLE service available for testing without hardware
- Placeholder spectrum data generated for testing
- Connection states can be simulated

## Future Enhancements

1. **Auto-reconnect**: Automatically reconnect to last device
2. **Device Profiles**: Save spectrometer configurations
3. **Spectrum Visualization**: Graph of wavelength vs intensity
4. **Calibration**: Spectrometer calibration curves
5. **Multiple Devices**: Support multiple connected spectrometers
6. **Background Scanning**: Support for background operation
7. **Data Caching**: Cache spectrum data locally
8. **Quality Alerts**: Warn when data quality is low

## Notes

- Kable provides multiplatform BLE support
- Service UUID filtering reduces scan time
- Data fusion improves SOC prediction accuracy
- EPO correction removes moisture-related variation
- Moisture estimation from NIR water absorption bands
- All BLE operations are async with proper error handling
- State management with StateFlow for reactive UI
- Camera-only mode still works without spectrometer
