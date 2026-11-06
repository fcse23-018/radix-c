# Map Integration - Design Note

> **Status: historical design note — not a description of shipped code.** This file predates the
> SoilCarbon Engine Android integration and refers to Mapbox/MapKit. The app uses osmdroid with
> OpenStreetMap tiles in `presentation/map/MapScreen.kt`.

## Overview
Map integration with Mapbox (Android) and MapKit (iOS) for displaying soil samples with SOC color-coded pins.

## Components Implemented

### 1. Map Service Interface
**File**: `shared/map/expect/MapService.kt`

Common interface for platform-specific map implementations:
- `initialize()` - Initialize map with access token
- `addSamplePin()` - Add pin for a sample
- `removeSamplePin()` - Remove pin
- `clearAllPins()` - Clear all pins
- `centerOnLocation()` - Center camera on location
- `getMapController()` - Get map controller for advanced operations

### 2. Map Pin Model
**File**: `shared/commonMain/kotlin/com/terrochroma/shared/map/model/MapPin.kt`

Data model for map pins:
- `MapPin`: Sample ID, coordinates, SOC percentage, color
- `MapPinColor`: Enum for SOC-based colors
- `getMapPinColorForSOC()`: Helper function to determine color from SOC

**SOC Color Coding:**
- < 1%: Red (low SOC)
- 1-2%: Green (medium-low)
- 2-3%: Cyan (medium)
- 3-4%: Yellow (medium-high)
- > 4%: Blue (high SOC)

### 3. Android Map Service (Mapbox)
**File**: `shared/map/androidMain/MapServiceImpl.kt`

Placeholder implementation for Mapbox integration:
- Mapbox initialization with access token
- Marker addition with SOC-based colors
- Camera centering
- Marker removal

**Note**: Actual Mapbox implementation requires:
- Mapbox access token in `mapbox.xml`
- MapView in Compose UI
- Mapbox SDK integration

### 4. iOS Map Service (MapKit)
**File**: `shared/map/iosMain/MapServiceImpl.kt`

Placeholder implementation for MapKit integration:
- MKMapView initialization
- MKPointAnnotation for markers
- Custom marker views with SOC colors
- Camera centering

**Note**: Actual MapKit implementation requires:
- MKMapView in Compose (via UIKit interop)
- Custom annotation views
- MapKit framework

### 5. Map ViewModel
**File**: `shared/commonMain/kotlin/com/terrochroma/shared/ui/screens/map/MapViewModel.kt`

ViewModel managing map state:
- Initialize map service
- Load samples from database
- Add pins to map
- Handle sample selection
- Center on sample
- Error handling

### 6. Updated Map Screen
**File**: `shared/commonMain/kotlin/com/terrochroma/shared/ui/screens/map/MapScreen.kt`

Enhanced map screen with:
- Loading state
- Ready state with map content
- Error state with retry
- Map legend showing SOC colors
- Sample detail display
- Integration with MapViewModel

## Dependencies Required

### Android
```gradle
implementation("com.mapbox.mapboxsdk:mapbox-android-sdk:10.0.1")
implementation("com.mapbox.mapboxsdk:mapbox-android-plugin-annotation-v9:0.9.0")
```

### iOS
MapKit is built-in, no additional dependencies needed.

## Configuration

### Android Mapbox Token
Create `composeApp/androidApp/src/main/res/values/mapbox.xml`:
```xml
<resources>
    <string name="mapbox_access_token">YOUR_ACCESS_TOKEN</string>
</resources>
```

### iOS MapKit
No additional configuration needed beyond Info.plist permissions.

## Integration Points

### With Database
```kotlin
val samples = databaseManager.getAllSamples()
samples.forEach { sample ->
    mapService.addSamplePin(sample)
}
```

### With Navigation
Map screen is already integrated in main navigation graph.

## Future Enhancements

1. **Real MapView Integration**: Add actual Mapbox/MapKit views to Compose
2. **Filter Controls**: Filter by date range, SOC range, sync status
3. **Clustering**: Cluster nearby pins for better performance
4. **Heat Map**: Show SOC density heatmap
5. **Offline Maps**: Download map tiles for offline use
6. **Custom Markers**: Generate custom marker icons from SVG
7. **Route Planning**: Plan routes between sample locations
8. **Share Location**: Share sample location with other apps

## Notes

- Service interfaces are complete and follow expect/actual pattern
- SOC color coding is consistent with export format
- Map legend provides visual reference for colors
- Placeholder implementations allow architecture to be tested
- Full Mapbox/MapKit integration requires UI component work
- All features are architected correctly for platform-specific implementations
