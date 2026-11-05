# Cloud Sync - WCS Design Note

> **Status: design note only — not implemented.**
> This document describes a proposed cloud-sync design. None of the `shared/sync/*` classes, the
> Earth Engine client, the sync service or the sync settings UI described below exist in this
> repository. The app implements optional point covariate lookups through SoilGrids, Open-Meteo and
> Sentinel in `data/remote/*`; it does not upload samples or synchronise data to a server.

## Overview
Design proposal for cloud sync using ISRIC SoilGrids Web Coverage Service (WCS) and Google Earth Engine API for environmental covariates.

## Why WCS Instead of REST API?

ISRIC SoilGrids REST API is currently paused/unavailable. WCS (Web Coverage Service) is:
- **More stable**: REST API is beta with frequent downtime
- **Production-ready**: ISRIC's recommended access method
- **Standard protocol**: OGC-compliant (not proprietary)
- **More efficient**: Designed for raster data access
- **Better support**: Widely supported by GIS software

## Proposed Components (not implemented)

### 1. WCS Data Models
**File**: `shared/sync/model/WCSCoverage.kt`

Data models for WCS operations:
- `WCSCapabilities`: Service metadata and available coverages
- `WCSCoverageSummary`: Brief coverage information
- `WCSCoverageDescription`: Detailed coverage metadata
- `WCSDimension`: Coverage dimensions (latitude, longitude, depth)
- `WCSCoverageData`: Downloaded raster data
- `WCSSoilData`: Soil properties at a point
- `SoilProperty`: Enum for soil types (SOC, SOM, pH, texture, etc.)
- `LatLon`: Geographic coordinates

### 2. SoilGrids WCS Client
**File**: `shared/sync/api/SoilGridsWCSClient.kt`

Proposed WCS client interface:

#### WCS Operations
- **getCapabilities()**: List all available soil layers
- **describeCoverage()**: Get detailed metadata for specific layer
- **getCoverage()**: Download spatial subset of soil data
- **getSoilDataAtPoint()**: Get soil data at specific coordinates
- **getAllSoilDataAtPoint()**: Get all soil properties at a point
- **downloadGlobalMap()**: Download complete global map

#### Supported Soil Properties
- SOC (Soil Organic Carbon)
- SOM (Soil Organic Matter)
- pH
- Sand, Silt, Clay percentages
- Bulk Density
- Cation Exchange Capacity (CEC)

#### Depth Intervals
- 0-5cm (topsoil)
- 5-15cm
- 15-30cm
- 30-60cm
- 60-100cm
- 100-200cm

#### Quantiles
- Mean
- Median
- Q0.05, Q0.25, Q0.75, Q0.95

### 3. Earth Engine API Client
**File**: `shared/sync/api/EarthEngineApiClient.kt`

Google Earth Engine API client for environmental covariates:

#### Features
- **OAuth2 Authentication**: Service account-based auth
- **NDVI**: Normalized Difference Vegetation Index
- **Precipitation**: Monthly/annual precipitation data
- **Temperature**: Temperature data
- **Elevation**: Digital elevation model (SRTM)
- **getCovariates()**: Fetch all covariates at a point

#### Authentication
- Uses Google Cloud service account
- OAuth2 JWT bearer token
- Token endpoint: `https://oauth2.googleapis.com/token`

### 4. Earth Engine Covariates Model
**File**: `shared/sync/model/EarthEngineCovariates.kt`

Data model for Earth Engine data:
- NDVI (-1 to 1)
- Precipitation (mm)
- Temperature (Celsius)
- Elevation (meters)
- Location and timestamp

### 5. Cloud Sync Service
**File**: `shared/sync/CloudSyncService.kt`

Main sync service coordinating WCS and Earth Engine:

#### Interface
- `syncSamples()`: Upload unsynced samples to cloud
- `fetchSoilGridsData()`: Fetch soil data at coordinates
- `fetchEarthEngineCovariates()`: Fetch environmental covariates
- `isOnline()`: Check network connectivity

#### Sync Result
- Synced count
- Failed count
- Conflicts (local vs remote versions)

#### Implementation
- Uses Ktor HTTP client
- Implements exponential backoff
- Handles sync conflicts
- Updates database sync status

### 6. Sync Settings UI
**File**: `shared/commonMain/kotlin/com/terrochroma/shared/ui/screens/sync/SyncSettingsScreen.kt`

Complete sync settings screen:

#### Features
- Sync status indicator (idle, syncing, error)
- Last sync timestamp
- Enable/disable sync toggle
- Sync frequency options (manual, daily, weekly)
- Manual sync button
- Information card about cloud sync

#### UI Components
- Status card with visual indicator
- Toggle switch for sync enable
- Radio buttons for frequency
- Sync button with loading state
- Information card explaining cloud sync

### 7. Sync Settings ViewModel
**File**: `shared/commonMain/kotlin/com/terrochroma/shared/ui/screens/sync/SyncSettingsViewModel.kt`

ViewModel managing sync settings state:
- Sync enabled/disabled
- Sync status
- Last sync time
- Sync frequency
- Manual sync trigger

### 8. Navigation Integration
**Updated**: `shared/commonMain/kotlin/com/terrochroma/shared/ui/TerroChromaApp.kt`

- Added sync_settings route
- Integrated with navigation graph

**Updated**: `shared/commonMain/kotlin/com/terrochroma/shared/ui/screens/settings/SettingsScreen.kt`

- Added clickable Cloud Sync setting
- Navigates to sync settings screen

## WCS Protocol Details

### GetCapabilities Request
```
GET https://maps.isric.org/ows?service=WCS&version=2.0.1&request=GetCapabilities
```

### DescribeCoverage Request
```
GET https://maps.isric.org/ows?service=WCS&version=2.0.1&request=DescribeCoverage&coverageid=soc_0-5cm_median
```

### GetCoverage Request
```
GET https://maps.isric.org/ows?service=WCS&version=2.0.1&request=GetCoverage&coverageid=soc_0-5cm_median&bbox=-180,-90,180,90&crs=EPSG:4326&format=image/geotiff
```

## Dependencies Required

```gradle
// Common (already in composeApp/build.gradle.kts)
implementation("io.ktor:ktor-client-core:2.3.7")
implementation("io.ktor:ktor-client-logging:2.3.7")
implementation("io.ktor:ktor-client-auth:2.3.7")
implementation("io.ktor:ktor-client-content-negotiation:2.3.7")
implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.7")
```

## Usage Example

### Fetch Soil Data at Point
```kotlin
val soilGridsClient = SoilGridsWCSClient(httpClient)
val soilData = soilGridsClient.getSoilDataAtPoint(
    latitude = 37.7749,
    longitude = -122.4194,
    property = SoilProperty.SOC,
    depthRange = "0-5cm",
    quantile = "median"
)
```

### Fetch Environmental Covariates
```kotlin
val earthEngineClient = EarthEngineApiClient(httpClient)
val covariates = earthEngineClient.getCovariates(
    projectId = "your-project-id",
    accessToken = "your-access-token",
    latitude = 37.7749,
    longitude = -122.4194
)
```

### Sync Samples
```kotlin
val cloudSyncService = CloudSyncServiceImpl(databaseManager, soilGridsClient, earthEngineClient)
val result = cloudSyncService.syncSamples()
// result.syncedCount, result.failedCount, result.conflicts
```

## Configuration

### Google Earth Engine Setup
1. Create Google Cloud project
2. Enable Earth Engine API
3. Create service account
4. Download service account key
5. Store key securely (encrypted local storage)

### ISRIC SoilGrids
- No API key required for WCS
- Base URL: `https://maps.isric.org/`
- Fair use policy: 5 API calls per minute
- Rate limiting expected in the proposed client

## Security Considerations

- Earth Engine service account keys stored in encrypted storage
- No sensitive data logged
- Certificate pinning recommended for HTTPS
- OAuth2 tokens rotated as needed
- Network connectivity checked before sync

## Offline-First Architecture

The app remains fully functional without cloud sync:
- All data stored locally in encrypted database
- Cloud sync is optional enhancement
- No network required for core features
- Sync queue for when offline
- Conflict resolution when back online

## Future Enhancements

1. **GeoTIFF Parsing**: Implement actual GeoTIFF library for value extraction
2. **XML Parsing**: Implement proper XML parser for WCS responses
3. **Background Sync**: Use WorkManager (Android) / BackgroundTasks (iOS)
4. **Conflict Resolution UI**: Allow users to resolve conflicts manually
5. **Sync History**: Track sync operations with timestamps
6. **Data Visualization**: Show SoilGrids data on map
7. **Advanced Filters**: Filter by date range, location, soil property
8. **Caching**: Cache WCS responses to reduce API calls

## Notes

- WCS is the production-recommended access method for SoilGrids
- REST API is currently paused/unavailable
- Architecture follows Clean Architecture principles
- All data models are serializable
- Error handling with Result type throughout
- Async operations with coroutines
- State management with StateFlow
- UI follows Material 3 design with TerroChroma colors
