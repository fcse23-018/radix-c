# TerroChroma - Project Status

> **Status: historical document — superseded.** This describes the older TerroChroma Kotlin
> Multiplatform prototype. Cloud sync, BLE/NIR and the listed `shared/*` files are not implemented
> in this app. See `README.md` for the current status.

## ✅ Completed Components

### 1. Project Structure
- Complete Kotlin Multiplatform project setup
- Compose Multiplatform for shared UI
- Proper directory structure following Clean Architecture
- Android and iOS targets configured

### 2. Design System
- Exact soil-and-vegetation color palette implemented
- Material 3 theme with earthy colors
- Typography system defined
- Dark theme support

### 3. Navigation
- 5-tab bottom navigation structure
- Navigation graph implemented
- Screen routing set up
- Center FAB for Capture

### 4. Camera Pipeline
- expect/actual pattern for platform-specific implementation
- Android: CameraX/Camera2 with RAW/DNG support
- iOS: AVFoundation with Bayer RAW support
- Manual camera parameter locking (ISO, shutter, white balance, focus)
- Capture result with metadata

### 5. ML Inference
- expect/actual pattern for platform-specific ML
- Android: TensorFlow Lite implementation
- iOS: Core ML implementation
- Input/output models defined
- <2 second inference target architecture

### 6. Database
- SQLDelight with SQLCipher encryption
- AES-256 encryption at rest
- expect/actual for platform-specific drivers
- SoilSample schema with all required fields
- CRUD operations implemented
- Cryptographic provenance hashing

### 7. Native Billing
- expect/actual pattern for platform-specific billing
- Android: Google Play Billing Library 7+
- iOS: StoreKit 2
- Subscription offer models
- Purchase flow implementation
- Restore purchases support

### 8. UI Screens
- Home: Dashboard with last sample display
- Capture: Camera viewfinder placeholder
- Map: Interactive map placeholder
- Library: Sample history with export buttons
- Settings: Subscription and configuration options

### 9. Domain Models
- SoilSample with camera params, color features, texture features
- Subscription tiers and configuration
- Camera configuration models
- ML input/output models

### 10. Documentation
- Comprehensive README with setup instructions
- Architecture documentation
- Technology stack decisions explained
- Configuration guidelines

## 🔨 In Progress / To Be Completed

### High Priority
1. **Computer Vision Module**
   - OpenCV integration for ArUco/AprilTag detection
   - Color space conversions (RGB → CIE L*a*b*, HSV, Munsell)
   - GLCM texture feature extraction
   - EPO moisture correction
   - Illuminant normalization to D65

2. **Subscription Management UI**
   - Subscription selection screen
   - Paywall implementation
   - Trial flow
   - Restore purchases UI

3. **Data Export**
   - GeoJSON export implementation
   - KML export implementation
   - CSV export implementation
   - Bulk export functionality

### Medium Priority
4. **Professional Logo & App Icon**
   - Logo design combining soil/leaf/chromatic motif
   - Adaptive icon generation
   - Monochrome icon
   - Feature graphic

5. **Onboarding Flow**
   - Splash screen
   - First-time user onboarding
   - Calibration card introduction
   - App tutorial

6. **Capture Wizard Implementation**
   - Card alignment detection UI
   - Stability check (IMU integration)
   - Light quality gate
   - Guided capture flow
   - Result bottom sheet

### Lower Priority
7. **Unit Tests**
   - Database tests
   - ML service tests
   - Domain model tests
   - ViewModel tests

8. **UI Tests**
   - Navigation tests
   - Screen interaction tests
   - Capture flow tests

9. **Map Integration**
   - Mapbox for Android
   - MapKit for iOS
   - SOC color-coded pins
   - Map interactions

10. **Cloud Sync**
    - ISRIC SoilGrids integration
    - Earth Engine covariates
    - Sync conflict resolution

11. **BLE Spectrometer**
    - Kable integration
    - MEMS NIR spectrometer support
    - Data fusion with camera

## 📁 Key Files Created

### Configuration
- `settings.gradle.kts` - Project configuration
- `build.gradle.kts` - Root build configuration
- `composeApp/build.gradle.kts` - Compose app build
- `composeApp/androidApp/build.gradle.kts` - Android app build
- `shared/build.gradle.kts` - Shared module build

### Android
- `composeApp/androidApp/src/main/AndroidManifest.xml` - Permissions and services
- `composeApp/androidApp/src/main/java/com/terrochroma/app/MainActivity.kt` - Entry point
- `composeApp/androidApp/src/main/java/com/terrochroma/app/TerroChromaApplication.kt` - Application class
- `composeApp/androidApp/src/main/res/values/` - Resources (strings, colors, themes)
- `composeApp/androidApp/proguard-rules.pro` - ProGuard rules

### iOS
- `composeApp/iosApp/Podfile` - CocoaPods configuration

### Shared - Core
- `shared/commonMain/kotlin/com/terrochroma/shared/ui/theme/Color.kt` - Color palette
- `shared/commonMain/kotlin/com/terrochroma/shared/ui/theme/Theme.kt` - Material theme
- `shared/commonMain/kotlin/com/terrochroma/shared/ui/theme/Type.kt` - Typography

### Shared - UI
- `shared/commonMain/kotlin/com/terrochroma/shared/ui/TerroChromaApp.kt` - Main app
- `shared/commonMain/kotlin/com/terrochroma/shared/ui/navigation/Screen.kt` - Navigation
- `shared/commonMain/kotlin/com/terrochroma/shared/ui/screens/home/HomeScreen.kt`
- `shared/commonMain/kotlin/com/terrochroma/shared/ui/screens/capture/CaptureScreen.kt`
- `shared/commonMain/kotlin/com/terrochroma/shared/ui/screens/map/MapScreen.kt`
- `shared/commonMain/kotlin/com/terrochroma/shared/ui/screens/library/LibraryScreen.kt`
- `shared/commonMain/kotlin/com/terrochroma/shared/ui/screens/settings/SettingsScreen.kt`

### Shared - Domain
- `shared/commonMain/kotlin/com/terrochroma/shared/domain/model/SoilSample.kt`
- `shared/commonMain/kotlin/com/terrochroma/shared/domain/model/Subscription.kt`

### Shared - Camera
- `shared/camera/expect/CameraService.kt` - Common interface
- `shared/camera/model/CameraConfig.kt` - Configuration
- `shared/camera/model/CaptureResult.kt` - Result model
- `shared/camera/androidMain/CameraServiceImpl.kt` - Android implementation
- `shared/camera/iosMain/CameraServiceImpl.kt` - iOS implementation

### Shared - Billing
- `shared/billing/expect/BillingService.kt` - Common interface
- `shared/billing/model/SubscriptionOffer.kt` - Offer model
- `shared/billing/androidMain/BillingServiceImpl.kt` - Google Play Billing
- `shared/billing/iosMain/BillingServiceImpl.kt` - StoreKit 2

### Shared - ML
- `shared/ml/expect/MLService.kt` - Common interface
- `shared/ml/model/MLInput.kt` - Input model
- `shared/ml/model/MLOutput.kt` - Output model
- `shared/ml/androidMain/MLServiceImpl.kt` - TensorFlow Lite
- `shared/ml/iosMain/MLServiceImpl.kt` - Core ML

### Shared - Database
- `shared/database/SoilSample.sq` - SQL schema
- `shared/database/DatabaseManager.kt` - Database manager
- `shared/database/androidMain/DatabaseDriverFactory.kt` - Android driver
- `shared/database/iosMain/DatabaseDriverFactory.kt` - iOS driver

### Shared - Vision
- `shared/vision/model/ColorFeatures.kt` - Color feature models
- `shared/vision/model/TextureFeatures.kt` - Texture feature models

## 🚀 Next Steps

To complete the application:

1. **Add OpenCV integration** for computer vision features
2. **Implement actual camera preview** in Compose
3. **Add location services** for GPS tagging
4. **Create subscription UI** with paywall logic
5. **Implement data export** functionality
6. **Design and add logo** and app icons
7. **Create onboarding flow** and splash screen
8. **Add unit tests** for critical components
9. **Configure build signing** for release builds
10. **Test on real devices** for camera and ML performance

## 📝 Notes

- The project structure follows the master prompt's architecture requirements exactly
- All mandatory color values from the UI spec are implemented
- The expect/actual pattern is used correctly for platform-specific code
- Database encryption is implemented with SQLCipher
- Native billing follows Google Play and App Store requirements
- The README provides comprehensive setup and configuration instructions

The foundation is solid and ready for feature completion.
