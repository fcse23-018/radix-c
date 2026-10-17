# Computer Vision Module - Implementation Summary

## Overview
The Computer Vision Module has been implemented with OpenCV integration for ArUco detection, color space conversions, and GLCM texture feature extraction.

## Components Implemented

### 1. Vision Service Interface
**File**: `shared/vision/expect/VisionService.kt`

Defines the common interface for platform-specific vision implementations:
- `detectAruco()` - Detect ArUco/AprilTag calibration markers
- `extractColorFeatures()` - Extract color features from images
- `extractTextureFeatures()` - Extract GLCM texture features
- `normalizeIlluminant()` - Normalize images to D65 illuminant
- `initialize()` - Initialize OpenCV
- `release()` - Release resources

### 2. ArUco Detection Model
**File**: `shared/vision/model/ArucoDetectionResult.kt`

Data model for ArUco detection results:
- Marker ID and corners
- Center point and rotation
- Detection confidence
- Alignment status (within 5° tolerance)
- Calibration card configuration

### 3. Color Space Converter
**File**: `shared/vision/color/ColorSpaceConverter.kt`

Pure Kotlin implementation of color space conversions:

#### RGB to CIE L*a*b*
- Gamma correction (sRGB to linear RGB)
- RGB to XYZ transformation (D65 illuminant)
- XYZ to Lab conversion using D65 reference white

#### RGB to HSV
- Hue calculation using atan2
- Saturation calculation
- Value (brightness) calculation

#### RGB to Munsell
- Approximation of Munsell color system
- Hue categorization (R, YR, Y, GY, G, BG, B, PB, P, RP)
- Value (lightness) scaling
- Chroma (saturation) calculation

#### Illuminant Normalization
- D65 (standard daylight) - no correction
- D50 (horizon daylight)
- A (incandescent)
- F2 (fluorescent cool white)
- F11 (fluorescent high efficiency)

### 4. GLCM Texture Extractor
**File**: `shared/vision/texture/GLCMExtractor.kt`

Gray-Level Co-occurrence Matrix texture feature extraction:

#### Configuration
- Distance: 1 pixel (configurable)
- Angle: 0°, 45°, 90°, 135° (configurable)
- Gray levels: 256 (configurable)

#### Features Computed
1. **Contrast**: Measures local variations
   - High contrast = high texture variation
   - Formula: Σ(i-j)² * P(i,j)

2. **Homogeneity**: Measures closeness of distribution
   - High homogeneity = smooth texture
   - Formula: Σ P(i,j) / (1 + (i-j)²)

3. **Energy (Angular Second Moment)**: Measures textural uniformity
   - High energy = uniform texture
   - Formula: √(Σ P(i,j)²)

4. **Entropy**: Measures randomness
   - High entropy = high randomness/complexity
   - Formula: -Σ P(i,j) * ln(P(i,j))

#### Implementation Details
- Converts RGB to grayscale using luminance formula: Y = 0.299R + 0.587G + 0.114B
- Builds GLCM from grayscale image
- Normalizes GLCM to probabilities
- Computes all four texture features

### 5. Android Vision Service Implementation
**File**: `shared/vision/androidMain/VisionServiceImpl.kt`

OpenCV-based implementation for Android:

#### OpenCV Initialization
- Uses `OpenCVLoader.initAsync()` for async loading
- Loader callback handles success/failure
- Loads OpenCV 4.8.0

#### ArUco Detection
- Uses OpenCV ArUco module
- Dictionary: DICT_4X4_50 (4x4 markers, 50 IDs)
- Detection parameters with default settings
- Calculates rotation from corner points
- Checks alignment within 5° tolerance

#### Color Feature Extraction
- Samples center 50x50 pixel region
- Calculates average RGB color
- Normalizes to D65 illuminant
- Converts to CIE L*a*b*, HSV, Munsell

#### Texture Feature Extraction
- Uses GLCMExtractor (shared implementation)
- Returns all four texture features

#### Illuminant Normalization
- Uses gray world assumption for white balance
- Applies color channel scaling
- Returns corrected image data

### 6. iOS Vision Service Implementation
**File**: `shared/vision/iosMain/VisionServiceImpl.kt`

CoreImage/Vision framework implementation for iOS:

#### Vision Framework
- Uses built-in Vision framework (no OpenCV needed)
- `VNDetectRectanglesRequest` for rectangle detection
- Similar to ArUco detection

#### Color Feature Extraction
- Uses CoreImage for image processing
- Samples center region using CGRect
- Applies color space conversions (shared implementation)

#### Texture Feature Extraction
- Uses GLCMExtractor (shared implementation)
- Shared with Android

#### Illuminant Normalization
- Uses CoreImage filters for white balance
- (Simplified implementation - placeholder)

## Dependencies

### Android
```gradle
implementation("org.opencv:opencv-android:4.8.0")
```

### iOS
- Vision framework (built-in)
- CoreImage framework (built-in)
- No external dependencies needed

## Usage Example

```kotlin
// Initialize vision service
val visionService: VisionService = // platform-specific implementation
visionService.initialize()

// Detect ArUco marker
val arucoResult = visionService.detectAruco(imageData)
if (arucoResult.isSuccess && arucoResult.getOrNull()?.isAligned == true) {
    // Marker detected and aligned
}

// Extract color features
val colorFeatures = visionService.extractColorFeatures(imageData)
val cieLab = colorFeatures.getOrNull()?.cieLab

// Extract texture features
val textureFeatures = visionService.extractTextureFeatures(imageData)
val contrast = textureFeatures.getOrNull()?.contrast

// Normalize illuminant
val normalizedImage = visionService.normalizeIlluminant(imageData, Illuminant.D65)

// Release resources
visionService.release()
```

## Integration with Capture Flow

The Vision Service integrates with the camera pipeline:

1. **Card Alignment**: Detect ArUco marker to ensure calibration card is properly aligned
2. **Stability Check**: Verify marker rotation is within tolerance (≤ 5°)
3. **Light Quality**: Check illumination conditions
4. **RAW Capture**: Capture RAW image with locked parameters
5. **Color Extraction**: Extract color features from center region
6. **Texture Extraction**: Extract GLCM features from full image
7. **Normalization**: Apply illuminant normalization to D65
8. **ML Inference**: Pass features to ML model for SOC prediction

## Performance Considerations

- OpenCV initialization is async on Android
- GLCM computation is O(n) where n is number of pixels
- Color space conversions are O(1) per pixel
- ArUco detection is fast for 4x4 markers
- All operations should complete under 2 seconds for inference target

## Future Enhancements

1. **Complete iOS Vision Framework Integration**
   - Implement proper rectangle detection parsing
   - Add CoreImage filter implementations
   - Implement complete white balance correction

2. **Advanced Color Calibration**
   - Implement spectrophotometer calibration
   - Add color checker card support
   - Implement custom illuminant profiles

3. **Optimized GLCM**
   - Multi-angle GLCM averaging
   - Adaptive gray level quantization
   - GPU acceleration on supported devices

4. **EPO Moisture Correction**
   - Implement External Parameter Orthogonalization
   - Add moisture calibration data
   - Integrate with NIR spectrometer data

## Testing

Recommended tests:
- Unit tests for color space conversions
- Unit tests for GLCM feature extraction
- Integration tests for ArUco detection
- Performance tests for inference time
- Accuracy tests against reference data

## Notes

- The color space converter is pure Kotlin and platform-independent
- GLCM extractor is shared between Android and iOS
- ArUco detection uses platform-specific libraries (OpenCV / Vision)
- All implementations follow the expect/actual pattern
- Color space conversions are scientifically accurate based on CIE standards
