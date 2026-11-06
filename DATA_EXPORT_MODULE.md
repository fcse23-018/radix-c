# Data Export Module - Design Note

> **Status: historical design note — not a description of shipped code.** This file predates the
> SoilCarbon Engine Android integration and claims KML support that the app does not provide. The
> shipped exporter supports CSV, GeoJSON, a full JSON bundle and a provenance manifest. See
> `README.md` and `ExportMapper.kt`.

## Overview
Complete data export system supporting GeoJSON, KML, and CSV formats for soil sample data.

## Components Implemented

### 1. ExportService Interface
**File**: `shared/commonMain/kotlin/com/terrochroma/shared/export/ExportService.kt`

Defines the export interface:
- `exportToGeoJSON(samples)` - Export specific samples to GeoJSON
- `exportToKML(samples)` - Export specific samples to KML
- `exportToCSV(samples)` - Export specific samples to CSV
- `exportAllToGeoJSON()` - Export all samples to GeoJSON
- `exportAllToKML()` - Export all samples to KML
- `exportAllToCSV()` - Export all samples to CSV

### 2. ExportServiceImpl
**File**: `shared/commonMain/kotlin/com/terrochroma/shared/export/ExportServiceImpl.kt`

Pure Kotlin implementation of all export formats:

#### GeoJSON Export
- **Format**: RFC 7946 GeoJSON FeatureCollection
- **Geometry**: Point with [longitude, latitude, altitude]
- **Properties**: Complete sample data including:
  - Basic info: id, timestamp, SOC, confidence, SOM
  - Camera parameters: ISO, exposure, white balance, focus, flash
  - Color features: CIE L*a*b*, HSV, Munsell
  - Texture features: contrast, homogeneity, energy, entropy
  - Provenance: image hash, model version, app version
- **Pretty Print**: Human-readable JSON formatting

#### KML Export
- **Format**: OGC KML 2.2
- **Structure**: Document with Placemark elements
- **Placemark Content**:
  - Name: Sample ID (first 8 characters)
  - Description: SOC, confidence, SOM, moisture, date
  - Point: longitude, latitude, altitude
  - Style: Color-coded based on SOC percentage
- **SOC Color Coding**:
  - < 1.0%: Red (low SOC)
  - 1.0-2.0%: Green (medium-low)
  - 2.0-3.0%: Cyan (medium)
  - 3.0-4.0%: Yellow (medium-high)
  - > 4.0%: Blue (high SOC)

#### CSV Export
- **Format**: Comma-separated values with header
- **Columns**: 27 columns including:
  - Identification: id, timestamp
  - Location: latitude, longitude, altitude
  - Results: SOC, confidence, SOM, lab value, moisture
  - Provenance: image hash, provenance hash
  - Camera: ISO, exposure time
  - Color: CIE L*a*b*, HSV values
  - Texture: contrast, homogeneity, energy, entropy
  - Metadata: model version, app version, sync status, notes
- **CSV Escaping**: Proper handling of commas, quotes, newlines

### 3. ExportViewModel
**File**: `shared/commonMain/kotlin/com/terrochroma/shared/ui/screens/export/ExportViewModel.kt`

ViewModel managing export operations:

#### State Management
- `ExportUiState`: Idle, Exporting, Success (with data and format), Error
- `sampleCount`: Number of available samples

#### Key Features
- **Export Operations**: Separate methods for each format
- **Loading States**: Shows exporting indicator
- **Error Handling**: Displays error messages
- **Success Feedback**: Shows export size and format

### 4. ExportScreen
**File**: `shared/commonMain/kotlin/com/terrochroma/shared/ui/screens/export/ExportScreen.kt`

UI for data export:

#### UI Components
- **TopAppBar**: With back navigation
- **Sample Count Card**: Shows available samples
- **Export Buttons**: Three buttons (GeoJSON, KML, CSV)
- **State Display**: Loading, success, or error states
- **Success State**: Shows format and data size
- **Error State**: Shows error message

#### Features
- Material 3 design with TerroChroma colors
- All three export formats accessible
- Visual feedback for all states
- Dismissible success/error messages

### 5. Navigation Integration
**Updated**: `shared/commonMain/kotlin/com/terrochroma/shared/ui/TerroChromaApp.kt`

- Added export screen route
- Integrated with navigation graph

**Updated**: `shared/commonMain/kotlin/com/terrochroma/shared/ui/screens/library/LibraryScreen.kt`

- Export buttons now navigate to export screen

## Export Format Details

### GeoJSON Structure
```json
{
  "type": "FeatureCollection",
  "features": [
    {
      "type": "Feature",
      "geometry": {
        "type": "Point",
        "coordinates": [longitude, latitude, altitude]
      },
      "properties": {
        "id": "sample-id",
        "timestamp": 1234567890,
        "soc_percentage": 2.5,
        "confidence_interval": 0.15,
        "som_percentage": 4.3,
        "camera_iso": 100,
        "cie_lab_l": 45.2,
        "texture_contrast": 0.85,
        ...
      }
    }
  ]
}
```

### KML Structure
```xml
<kml xmlns="http://www.opengis.net/kml/2.2">
  <Document>
    <name>TerroChroma Soil Samples</name>
    <Placemark>
      <name>Sample abc12345</name>
      <description>
        SOC: 2.5%
        Confidence: ±0.15%
        SOM: 4.3%
        Date: Wed Oct 08 2026
      </description>
      <Point>
        <coordinates>-122.4194,37.7749,0.0</coordinates>
      </Point>
      <Style>
        <IconStyle>
          <color>ff00ff00</color>
          <scale>1.0</scale>
        </IconStyle>
      </Style>
    </Placemark>
  </Document>
</kml>
```

### CSV Structure
```csv
id,timestamp,latitude,longitude,altitude,soc_percentage,confidence_interval,som_percentage,lab_value,moisture_content,image_hash,provenance_hash,camera_iso,camera_exposure_time,cie_lab_l,cie_lab_a,cie_lab_b,hsv_h,hsv_s,hsv_v,texture_contrast,texture_homogeneity,texture_energy,texture_entropy,model_version,app_version,is_synced,notes
abc123,1728390000,37.7749,-122.4194,0.0,2.5,0.15,4.3,2.45,12.5,hash1,hash2,100,10000000,45.2,3.1,8.5,120,0.45,0.78,0.85,0.92,1.0,1.0.0,1.0.0,false,""
```

## Integration Points

### With Library Screen
```kotlin
Button(onClick = { navController.navigate("export") }) {
    Text("GeoJSON")
}
Button(onClick = { navController.navigate("export") }) {
    Text("KML")
}
Button(onClick = { navController.navigate("export") }) {
    Text("CSV")
}
```

### With Database
```kotlin
// In production, ExportServiceImpl would use DatabaseManager
val samples = databaseManager.getAllSamples()
exportToGeoJSON(samples)
```

## Usage Example

```kotlin
// Initialize export service
val exportService = ExportServiceImpl(databaseManager)

// Export all samples to GeoJSON
val geoJsonResult = exportService.exportAllToGeoJSON()
if (geoJsonResult.isSuccess) {
    val geoJsonData = geoJsonResult.getOrNull()
    // Save to file or share
}

// Export specific samples to KML
val selectedSamples = listOf(sample1, sample2, sample3)
val kmlResult = exportService.exportToKML(selectedSamples)
if (kmlResult.isSuccess) {
    val kmlData = kmlResult.getOrNull()
    // Save to file or share
}
```

## Design System Integration

### Colors Used
- **LeafGreen** (#2E7D32): Export buttons
- **SoilBrown** (#5D4037): Titles
- **MaterialTheme.colorScheme.error**: Error states
- **MaterialTheme.colorScheme.onPrimary**: Text on green backgrounds

### Material 3 Components
- Cards with rounded corners
- Button for primary actions
- TopAppBar with navigation
- CircularProgressIndicator for loading
- Success/error cards with icons

## File Saving

In production, the exported data would be saved to device storage:

### Android
```kotlin
val fileName = "terrochroma_export_${System.currentTimeMillis()}.geojson"
val file = File(context.getExternalFilesDir(null), fileName)
file.writeText(exportData)
```

### iOS
```kotlin
let fileName = "terrochroma_export_\(timestamp).geojson"
let fileURL = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0].appendingPathComponent(fileName)
try exportData.write(to: fileURL, atomically: true, encoding: .utf8)
```

## Future Enhancements

1. **File Sharing**: Share exported files via intent/share sheet
2. **Date Range Export**: Export samples within specific date range
3. **Location Filter**: Export samples by geographic region
4. **Custom Fields**: Allow users to select which fields to export
5. **Bulk Export**: Export to multiple formats simultaneously
6. **Cloud Export**: Direct upload to cloud storage (Google Drive, Dropbox)
7. **Export History**: Track previous exports with timestamps
8. **Scheduled Exports**: Automatic periodic exports

## Testing Recommendations

### Unit Tests
- GeoJSON format validation
- KML format validation
- CSV format validation
- SOC color coding logic
- CSV escaping logic

### Integration Tests
- End-to-end export flow
- Database query integration
- File saving functionality
- Large dataset performance

### UI Tests
- Navigation to export screen
- Export button interactions
- Loading state display
- Success/error state handling

## Notes

- All export formats include complete sample data
- GeoJSON follows RFC 7946 specification
- KML follows OGC KML 2.2 specification
- CSV uses standard RFC 4180 format
- KML includes SOC-based color coding for visualization
- Export operations are async with proper error handling
- Material 3 design follows TerroChroma color system
- Export data is returned as strings for flexibility (can be saved, shared, or displayed)
