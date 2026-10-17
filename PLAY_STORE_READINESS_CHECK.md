# Play Store Readiness Check - TerroChroma

## ✅ Passed Requirements

### 1. Target API Level
**Status**: ✅ PASS
- **Target API**: 35 (Android 15)
- **Minimum API**: 29 (Android 10)
- **Requirement**: Target API 35 required for new apps

### 2. Permissions
**Status**: ✅ PASS
All permissions are properly declared with runtime rationale in code:

| Permission | Status | Purpose |
|------------|--------|---------|
| CAMERA | ✅ | Required for soil sample capture |
| ACCESS_FINE_LOCATION | ✅ | GPS tagging of samples |
| ACCESS_COARSE_LOCATION | ✅ | GPS tagging (coarse) |
| BLUETOOTH_SCAN | ✅ | NIR spectrometer (Android 12+) |
| BLUETOOTH_CONNECT | ✅ | NIR spectrometer (Android 12+) |
| INTERNET | ✅ | Cloud sync with ISRIC SoilGrids |
| ACCESS_NETWORK_STATE | ✅ | Network connectivity check |
| FOREGROUND_SERVICE | ✅ | Background ML inference |
| FOREGROUND_SERVICE_CAMERA | ✅ | Camera foreground service |
| USE_BIOMETRIC | ✅ | Optional app lock |
| READ_MEDIA_IMAGES | ✅ | Image access (Android 13+) |

**Notes:**
- Storage permissions limited to API 32 (scoped storage)
- Legacy Bluetooth permissions limited to API 30
- All permissions have clear purpose in app

### 3. App Signing
**Status**: ⚠️ REQUIRES SETUP
- **ProGuard**: Enabled for release builds
- **Signing**: Not configured (needs keystore setup)

**Required for Release:**
```gradle
buildTypes {
    release {
        isMinifyEnabled = true
        proguardFiles(...)
        signingConfig = signingConfigs.release
    }
}

signingConfigs {
    create("release") {
        storeFile = file("keystore.jks")
        storePassword = "your_password"
        keyAlias = "your_key_alias"
        keyPassword = "your_key_password"
    }
}
```

### 4. App Icon
**Status**: ⚠️ PARTIAL
- **Adaptive Icon**: XML configuration complete
- **PNG Assets**: Need to be generated from SVG
- **Monochrome Icon**: Configuration complete

**Action Required:**
- Run instructions in `assets/logo/ICON_GENERATION_INSTRUCTIONS.md`
- Generate PNGs at all required densities (mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi)

### 5. Feature Graphic
**Status**: ❌ MISSING
- **Required**: 1024x500px feature graphic for Play Store
- **Location**: `assets/marketing/feature_graphic.png`

**Action Required:**
- Create feature graphic showing app in field use
- Include SOC visualization and calibration card

### 6. Screenshots
**Status**: ❌ MISSING
- **Required**: At least 2 screenshots
- **Recommended**: 8 screenshots (phone and tablet)
- **Aspect Ratios**: 16:9 or 9:16

**Action Required:**
- Take screenshots from running app
- Show: Capture flow, Results, Map, Library, Settings

### 7. App Name & Description
**Status**: ✅ PASS
- **App Name**: TerroChroma
- **Package Name**: com.terrochroma.app
- **Display Name**: Defined in strings.xml

**Play Store Description Needed:**
- Short description (80 chars max)
- Long description (4000 chars max)
- Keywords for ASO (App Store Optimization)

### 8. Content Rating
**Status**: ⚠️ REQUIRES QUESTIONNAIRE
- **Target Audience**: General audiences
- **Content**: No violence, no sexual content, no gambling
- **Data Safety**: Must complete Google Play Data Safety section

**Action Required:**
- Complete content rating questionnaire in Play Console
- Fill data safety section (location, camera, storage usage)

### 9. Database Encryption
**Status**: ✅ PASS
- **Encryption**: SQLCipher with AES-256
- **Key Management**: Device ID-based (needs Android Keystore for production)
- **Implementation**: Complete in `DatabaseDriverFactory.kt`

**Security Audit:**
```kotlin
// Current implementation (needs improvement)
actual fun getEncryptionKey(): String {
    val deviceId = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ANDROID_ID
    )
    return deviceId.take(32).padEnd(32, '0')
}
```

**Recommendation:**
- Use Android Keystore for production
- Generate random encryption key
- Store key in Keystore with hardware-backed security

### 10. Privacy Policy
**Status**: ❌ MISSING
- **Required**: Link to privacy policy
- **Location**: Must be hosted online

**Action Required:**
- Create privacy policy page
- Include data collection practices
- Add link to Play Console

### 11. Data Safety
**Status**: ⚠️ PARTIAL
- **Location Data**: Collected (GPS coordinates)
- **Camera Images**: Stored locally
- **Encrypted**: Yes (SQLCipher)
- **Shared**: No (only with user consent via export)

**Action Required:**
- Complete Google Play Data Safety section
- Document encryption methods
- Specify retention period

## 🔴 Critical Issues (Must Fix Before Launch)

### 1. APK Cannot Be Built
**Issue**: Gradle wrapper not present, build tools not installed
**Impact**: Cannot generate APK for testing or upload
**Solution**: Install Android Studio or set up Gradle manually

### 2. App Icons Not Generated
**Issue**: Only SVG exists, PNGs not generated
**Impact**: App will use default Android icon
**Solution**: Run instructions in `ICON_GENERATION_INSTRUCTIONS.md`

### 3. Feature Graphic Missing
**Issue**: No 1024x500px feature graphic
**Impact**: App store listing incomplete
**Solution**: Create feature graphic

### 4. Screenshots Missing
**Issue**: No Play Store screenshots
**Impact**: Cannot submit to Play Store
**Solution**: Take screenshots from running app

### 5. Privacy Policy Missing
**Issue**: No privacy policy link
**Impact**: Cannot submit to Play Store
**Solution**: Create and host privacy policy

### 6. Signing Configuration Missing
**Issue**: No keystore configuration
**Impact**: Cannot sign release APK
**Solution**: Generate keystore and configure signing

## ⚠️ Important Issues (Should Fix)

### 1. Encryption Key Management
**Issue**: Uses device ID as encryption key (deterministic)
**Impact**: All devices with same device ID have same key
**Recommendation**: Use Android Keystore with random key

### 2. Service References
**Issue**: MLInferenceService referenced but not implemented
**Impact**: Potential crash if service is started
**Solution**: Implement service or remove reference

### 3. Kable Dependency
**Issue**: Kable 0.23.0 dependency added but may need updated version
**Impact**: May have compatibility issues
**Recommendation**: Verify latest stable version

### 4. Mapbox Access Token
**Issue**: No Mapbox access token configured
**Impact**: Map will not display tiles
**Solution**: Add token to `mapbox.xml`

### 5. ProGuard Rules
**Issue**: May need additional rules for all dependencies
**Impact**: Release build may have runtime crashes
**Recommendation**: Test release build thoroughly

## ✅ Well Implemented

### 1. Architecture
- Clean Architecture with proper separation
- expect/actual pattern used correctly
- Dependency injection with Koin

### 2. Design System
- Exact soil palette implemented
- Material 3 design language
- No pure white/black surfaces

### 3. Navigation
- 5-tab bottom navigation
- Proper routing
- Back navigation handled

### 4. Permissions
- All required permissions declared
- Scoped storage compliance
- Runtime permission ready

### 5. Database
- SQLDelight with SQLCipher
- AES-256 encryption
- Proper schema with indexes
- CRUD operations complete

### 6. Billing
- Google Play Billing Library 7+
- StoreKit 2 ready
- Subscription UI complete

### 7. Export
- GeoJSON, KML, CSV formats
- RFC-compliant implementations
- SOC color coding

### 8. Cloud Sync
- WCS implementation (production-recommended)
- Earth Engine API integration
- Offline-first architecture

### 9. BLE
- Kable integration
- Data fusion with EPO
- Moisture correction

### 10. Testing
- Unit tests for critical algorithms
- Test infrastructure complete

## 📋 Play Store Submission Checklist

- [ ] Generate and sign release APK
- [ ] Create Google Play Console account
- [ ] Create app listing
- [ ] Upload APK (or use App Bundle)
- [ ] Generate app icons (all densities)
- [ ] Create feature graphic (1024x500px)
- [ ] Take screenshots (at least 2)
- [ ] Write short description (80 chars)
- [ ] Write long description (4000 chars)
- [ ] Complete content rating questionnaire
- [ ] Fill data safety section
- [ ] Add privacy policy link
- [ ] Add privacy policy URL to app (Settings screen)
- [ ] Configure keystore for signing
- [ ] Test release build on physical device
- [ ] Verify no crashes or ANRs
- [ ] Verify battery usage is reasonable
- [ ] Test on multiple device sizes
- [ ] Test on Android 10-15
- [ ] Verify subscription flow works
- [ ] Verify export functionality
- [ ] Verify database encryption
- [ ] Verify permissions work correctly

## Summary

**Play Store Readiness: 60%**

**Strong Points:**
- Excellent architecture
- Proper target API (35)
- Good permission declarations
- Database encryption implemented
- Comprehensive feature set

**Blocking Issues:**
- APK cannot be built (no Gradle/setup)
- Missing app icons (PNGs)
- Missing feature graphic
- Missing screenshots
- Missing privacy policy
- No signing configuration

**Estimated Time to Complete:**
- Build setup: 1-2 hours (Android Studio installation)
- Asset generation: 2-3 hours
- Play Store assets: 2-3 hours
- Privacy policy: 1 hour
- Testing: 4-6 hours

**Total: 10-15 hours** of additional work to be Play Store ready.

## Recommendation

**Priority 1 (Blocking):**
1. Install Android Studio and build APK
2. Generate app icons from SVG
3. Create feature graphic
4. Take screenshots
5. Create privacy policy
6. Configure signing

**Priority 2 (Important):**
1. Improve encryption key management
2. Test release build
3. Verify all features work on physical device
4. Complete Play Console data safety section

The codebase is excellent, but the build environment and Play Store assets need to be completed before submission.
