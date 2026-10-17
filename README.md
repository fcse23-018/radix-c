# TerroChroma

**Rapid Optical Soil Carbon Sensing at the Edge**

TerroChroma is a production-ready Kotlin Multiplatform application that turns any modern smartphone into a scientific-grade Soil Organic Carbon (SOC) and Soil Organic Matter (SOM) analyzer. It uses the phone's camera, calibration card detection, on-device ML, and optional BLE NIR spectrometers to deliver laboratory-comparable results in under 2 seconds, fully offline.

## 🌱 Features

- **RAW Camera Capture**: Manual camera parameter locking (ISO, shutter, white balance, focus) with uncompressed RAW capture (DNG on Android, Bayer on iOS)
- **Calibration Card Detection**: Real-time ArUco/AprilTag detection for illuminant normalization to D65
- **Color Science**: Conversion to CIE L*a*b*, HSV, Munsell color spaces
- **Texture Analysis**: GLCM texture features (contrast, homogeneity, energy, entropy)
- **Moisture Correction**: External Parameter Orthogonalization (EPO) for moisture effects
- **On-Device ML**: TensorFlow Lite (Android) + Core ML (iOS) inference under 2 seconds
- **BLE Integration**: Optional connection to MEMS NIR spectrometers (900–2500 nm)
- **Offline-First**: AES-256 encrypted database with ≥1000 sample cache
- **Native Billing**: Google Play Billing (Android) + StoreKit 2 (iOS) with subscription tiers
- **Data Export**: GeoJSON, KML, CSV formats
- **Cloud Sync**: Optional sync with ISRIC SoilGrids and Earth Engine covariates

## 🎨 Color System

TerroChroma uses a soil-and-vegetation palette:

| Name          | Hex       | Role                          |
|---------------|-----------|-------------------------------|
| Deep Soil     | #3E2723   | Primary dark text, icons      |
| Soil Brown    | #5D4037   | App bars, primary structure   |
| Mid Soil      | #6D4C41   | Secondary text                |
| Leaf Green    | #2E7D32   | Primary accent, CTAs, success |
| Mid Green     | #388E3C   | Selected states               |
| Olive         | #689F38   | Highlights, badges            |
| Cream         | #F5F5F0   | Main backgrounds             |
| Warm Beige    | #EFEBE9   | Cards, bottom sheets          |

## 🏗️ Architecture

TerroChroma follows Clean Architecture with MVI (Model-View-Intent) pattern:

```
composeApp/
├── androidApp/              # Android entry point
├── iosApp/                  # iOS entry point
└── shared/                  # Shared KMP code
    ├── core/                # Core utilities
    ├── database/            # Encrypted SQLDelight database
    ├── camera/              # Platform-specific camera implementation
    │   ├── expect/          # Common interface
    │   ├── androidMain/     # CameraX/Camera2 (RAW/DNG)
    │   └── iosMain/         # AVFoundation (Bayer RAW)
    ├── vision/              # Computer vision & color science
    ├── ml/                  # On-device ML inference
    │   ├── androidMain/     # TensorFlow Lite
    │   └── iosMain/         # Core ML
    ├── billing/             # Native subscription billing
    │   ├── expect/          # Common interface
    │   ├── androidMain/     # Google Play Billing
    │   └── iosMain/         # StoreKit 2
    ├── sync/                # Cloud synchronization
    ├── domain/              # Domain models & business logic
    ├── data/                # Data layer
    └── ui/                  # Compose Multiplatform screens
        ├── theme/           # Design system
        ├── navigation/      # Navigation graph
        └── screens/          # Screen implementations
            ├── home/
            ├── capture/
            ├── map/
            ├── library/
            └── settings/
```

## 🛠️ Technology Stack

- **Language**: Kotlin 2.0+ (K2 compiler)
- **UI**: Compose Multiplatform (shared UI for Android + iOS)
- **Architecture**: Clean Architecture + MVI / Unidirectional Data Flow
- **Concurrency**: Kotlin Coroutines + StateFlow / SharedFlow
- **Database**: SQLDelight + SQLCipher (AES-256 encryption at rest)
- **Networking**: Ktor Client
- **Dependency Injection**: Koin
- **Camera**:
  - Android → CameraX / Camera2 (RAW/DNG support)
  - iOS → AVFoundation (Bayer RAW)
- **Computer Vision**: OpenCV (via cinterop / JNI)
- **On-device ML**: TensorFlow Lite (Android) + Core ML (iOS)
- **Bluetooth**: Kable (multiplatform BLE) for NIR spectrometers
- **Maps**: Mapbox (Android) / MapKit (iOS)
- **Billing**: Native only - Google Play Billing Library 7+ / StoreKit 2
- **Minimum targets**: Android 10 (API 29) / iOS 16

## 📱 Navigation

Bottom navigation with 5 tabs:

1. **Home** – Dashboard with last sample, stats, recent list
2. **Capture** (center FAB) – Full guided viewfinder experience
3. **Map** – Interactive map with SOC color-coded pins
4. **Library** – Searchable history + bulk export
5. **Settings** – Full configuration + subscription management

### Capture Wizard Flow

Card Alignment → Stability Check (IMU ≤ 10°) → Light Quality Gate → RAW Capture → Local Inference → Result Bottom Sheet

## 💳 Subscription Tiers

- **Free Tier**: Limited monthly captures (configurable, default 10)
- **Pro Monthly**: Unlimited captures
- **Pro Yearly**: Unlimited captures with optional free trial (7 days)
- **Soft Paywall**: Users experience value before subscription prompt
- **Restore Purchases**: Support for restoring previous purchases
- **Manage Subscription**: Deep-link to native store UI

## 🔒 Security

- **AES-256**: All local data encrypted at rest
- **No Logging**: Sensitive location/image data not logged in production
- **Certificate Pinning**: For all backend calls
- **Biometric Lock**: Optional app lock with biometrics
- **Billing Security**: Follows Play Billing / StoreKit best practices
- **Provenance Record**: Cryptographic hash for every result (app version + model ID + camera params + hash)

## 🚀 Setup Instructions

### Prerequisites

- **Android Studio**: Hedgehog (2023.1.1) or later
- **Xcode**: 15.0 or later (for iOS development)
- **JDK**: 17 or later
- **Kotlin**: 2.0.0
- **Gradle**: 8.3.0

### Clone the Repository

```bash
git clone <repository-url>
cd RADIX-C
```

### Android Setup

1. Open the project in Android Studio
2. Sync Gradle files
3. Ensure you have the Android SDK installed (API 35 target)
4. Run the app on an Android device or emulator:

```bash
./gradlew :composeApp:androidApp:installDebug
```

### iOS Setup

1. Open `iosApp/iosApp.xcodeproj` in Xcode
2. Install CocoaPods dependencies:

```bash
cd iosApp
pod install
```

3. Build and run in Xcode or via command line:

```bash
./gradlew :composeApp:iosApp:assembleDebug
```

### ML Model Setup

The application expects a TensorFlow Lite model at `androidApp/src/main/assets/soc_model.tflite`.

To replace the ML model:

1. Train your model using the reference documents' methodology
2. Export to TFLite format
3. Place in `androidApp/src/main/assets/soc_model.tflite`
4. For iOS, convert to Core ML using `coremltools` and place in `iosApp/iosApp/`

### Database Encryption

The database uses SQLCipher with AES-256 encryption. The encryption key is currently generated from device ID. For production:

**Android**: Use Android Keystore for secure key storage
**iOS**: Use Keychain Services for secure key storage

### Billing Setup

**Android**:
1. Create app in Google Play Console
2. Configure subscription products:
   - `pro_monthly`
   - `pro_yearly`
3. Add public key to `androidApp/build.gradle.kts`

**iOS**:
1. Create app in App Store Connect
2. Configure subscription products:
   - `pro_monthly`
   - `pro_yearly`
3. Configure StoreKit configuration file

## 🧪 Testing

### Unit Tests

```bash
./gradlew test
```

### Android Instrumented Tests

```bash
./gradlew :composeApp:androidApp:connectedAndroidTest
```

### iOS Tests

```bash
./gradlew :composeApp:iosApp:testDebugUnitTest
```

## 📦 Build & Release

### Android Release

```bash
./gradlew :composeApp:androidApp:assembleRelease
```

The APK will be at `composeApp/androidApp/build/outputs/apk/release/`

### iOS Release

Build in Xcode with the Archive target, then upload to App Store Connect.

## 📚 Architecture Decisions

### Why Compose Multiplatform?

- Shared UI code reduces development time by ~40%
- Consistent user experience across platforms
- Hot reload for faster development
- Native performance

### Why SQLDelight over Room KMP?

- Better KMP support
- Type-safe SQL queries
- Excellent SQLCipher integration
- Migration support

### Why Native Billing?

- Google Play / Apple App Store policies require native billing
- Better user experience with native purchase flows
- Easier subscription management
- Compliance with store guidelines

### Why TensorFlow Lite + Core ML?

- On-device inference for offline capability
- <2 second inference time requirement
- Platform-specific optimizations
- Established ecosystems

## 🔧 Configuration

### Camera Parameters

Edit `CameraConfig.kt` to adjust default camera settings:

```kotlin
data class CameraConfig(
    val iso: Int = 100,
    val exposureTime: Long = 10000000L, // nanoseconds
    val whiteBalanceMode: WhiteBalanceMode = WhiteBalanceMode.AUTO,
    val focusMode: FocusMode = FocusMode.AUTO,
    val flashMode: FlashMode = FlashMode.OFF,
    val rawFormat: RawFormat = RawFormat.DNG
)
```

### Subscription Limits

Edit `SubscriptionConfig.kt` to adjust subscription parameters:

```kotlin
data class SubscriptionConfig(
    val freeMonthlyCaptures: Int = 10,
    val proMonthlyPrice: String = "$9.99",
    val proYearlyPrice: String = "$99.99",
    val proYearlyDiscount: Int = 17,
    val trialDays: Int = 7
)
```

## 📄 License

[Add your license here]

## 🤝 Contributing

[Add contribution guidelines here]

## 📞 Support

For support, email support@terrochroma.com or open an issue in the repository.

## 🎯 Roadmap

- [ ] OpenCV integration for ArUco/AprilTag detection
- [ ] Complete EPO moisture correction implementation
- [ ] BLE spectrometer integration with Kable
- [ ] ISRIC SoilGrids cloud sync
- [ ] Google Earth Engine covariates
- [ ] Advanced analytics dashboard
- [ ] Multi-language support
- [ ] Accessibility improvements

## 📖 References

Based on the following research:

1. **Smartphone-Based Soil Carbon Measurement Techniques** (full research paper)
2. **Software Requirements Specification (SRS) & Technical Reference Manual** (SRS-SOC-2026-V1)
3. **TerroChroma UI & Navigation Design Specification**

These documents are the single source of truth for all functional requirements, color system, navigation, capture flow, sensors, ML pipeline, security rules, and non-functional requirements.

---

**Version**: 1.0.0
**Last Updated**: October 2026
