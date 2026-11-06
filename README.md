# SoilCarbon Engine

**Offline-first smartphone soil organic carbon (SOC) analysis**

SoilCarbon Engine is a production-oriented Android application that turns a modern smartphone into a
field screening instrument for soil organic carbon. It captures a RAW + JPEG image of a soil aperture
and a printed six-patch calibration card, rectifies the capture, fits a D65 radiometric transfer
function, extracts the exact feature vector mandated by **SRS-SOC-2026-V1 v2.4.0**, and runs on-device
TFLite inference.

> **Research screening only.** Results must be validated against dry combustion (ISO 10694) before
> regulatory reporting, carbon-credit issuance, or agronomic decisions. The shipped model is an
> explicitly labelled placeholder.

---

## Features

- **RAW + JPEG capture** — CameraX `OUTPUT_FORMAT_RAW_JPEG` with Camera2 interop for manual ISO,
  shutter, focus and white-balance locking.
- **Calibration card** — OpenCV ArUco IDs 20–23 at the corners plus ID 24 at the top centre, six
  neutral reflectance patches (99/80/60/40/20/5 %) and a 30 × 30 mm soil aperture.
- **Live quality gates** — IMU tilt ≤ 10°, minimum illuminance (default 50 lux), target alignment,
  patch coverage and GPS state are shown in the viewfinder before capture.
- **Radiometric calibration** — per-channel weighted affine fit over the six patches, with RMSE,
  worst R² and illumination-variation diagnostics saved per sample.
- **Colour science** — sRGB linearisation, CIE XYZ/L\*a\*b\*, approximate Munsell notation, HSV and
  reflectance ratios.
- **Texture** — GLCM contrast, energy, homogeneity, correlation, entropy and dissimilarity on the
  rectified aperture.
- **Moisture correction** — EPO projection from a gravimetric moisture value when supplied.
- **Exact SRS feature vector**
  `[L*, a*, b*, Munsell_Value, GLCM_Contrast, GLCM_Energy, NDVI_Sat, Clay_Content]`.
- **On-device inference** — TFLite with optional GPU delegate and NNAPI/CPU fallback. If the model
  asset is missing or invalid, analysis fails closed with a visible rejection; the app never
  invents a SOC value.
- **SOC/SOM + uncertainty** — SOM = SOC × 1.724, 95 % interval, confidence score and carbon stock
  (Mg C/ha) from bulk density and depth.
- **Geospatial fusion** — GPS fix, SoilGrids clay/SOC baseline, Sentinel-2 NDVI and Open-Meteo
  climate, all cached for offline use with regional defaults as the final fallback.
- **Encrypted local storage** — Room + SQLCipher (AES-256), Keystore-backed AES-GCM for image files.
- **Provenance** — SHA-256 over a deterministic scientific payload plus RAW/JPEG digests.
- **Role-based navigation** — Field Technicians get the capture workspace; Agronomist and Data
  Scientist roles get the dashboard and sample map. All roles share the same local analysis and
  export pipeline.
- **Exports** — CSV, GeoJSON, full JSON bundle and provenance manifest, shared read-only through the
  Android chooser or saved through the Storage Access Framework.
- **No account required** — network access is optional and can be disabled in Settings.

---

## Technology

| Layer | Implementation |
|---|---|
| Language | 100 % Kotlin |
| UI | Jetpack Compose + Material 3, single `MainActivity` |
| Architecture | Presentation → Domain → Data (Clean Architecture) |
| DI / async | Hilt, Coroutines, Flow, StateFlow |
| Persistence | Room 2.6 + SQLCipher 4.6, DataStore Preferences |
| Camera | CameraX 1.5 + Camera2 interop, RAW/DNG where supported |
| Vision | OpenCV 4.10 (ArUco, colour conversion, rectification) |
| ML | TensorFlow Lite 2.16 (GPU delegate optional) |
| Background | WorkManager + Hilt worker factory |
| Maps | osmdroid |
| Networking | Retrofit + OkHttp + Gson, rate-limited clients |
| Build | AGP 8.7.3, Kotlin 2.0, Gradle 8.9, minSdk 28, targetSdk 35 |

---

## Project structure

```
app/
  src/main/java/com/soilcarbon/engine/
    core/                 Constants, dispatchers, result wrappers
    domain/               Models, colour science, GLCM, calibration, repositories, use cases
    data/                 Room/SQLCipher, encrypted files, camera, sensors, vision, remote APIs, sync
    presentation/         Compose screens, ViewModels, navigation, theme
  src/main/assets/
    calibration/          calibration_target.json, printable PNG and SVG
    models/               soc_model.tflite + soc_model_metadata.json
  src/test/               JVM unit tests (colour, GLCM, calibration, feature vector, provenance)
tools/generate_assets.py  Regenerates the card assets and placeholder TFLite model
```

---

## Build and run

### Prerequisites

- Android Studio Ladybug (2024.2) or newer
- JDK 17 or newer (JDK 21 is used in CI)
- Android SDK Platform 35 and Build Tools 35
- A physical device is strongly recommended: RAW capture, IMU, GNSS and camera2 interop are device
  dependent and are not representative on an emulator

### Configure the SDK

Create `local.properties` in the repository root (already present in local checkouts):

```properties
sdk.dir=C\:\\Users\\<you>\\AppData\\Local\\Android\\Sdk
```

### Build

```bat
gradlew.bat :app:assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

### Install

```bat
gradlew.bat :app:installDebug
```

### Unit tests

```bat
gradlew.bat :app:testDebugUnitTest
```

The suite covers CIE colour conversion, GLCM normalisation, radiometric affine fitting, feature
vector ordering/clamping and provenance hashing.

### Device tests

```bat
gradlew.bat :app:connectedDebugAndroidTest
```

The nine instrumented tests cover the encrypted sample lifecycle, FileProvider URI/read grants,
NFR-1 inference latency and NFR-2 capacity for 1,000 local-only samples in SQLCipher. CI runs them
on an API 34 x86_64 KVM emulator (`/dev/kvm` is available on GitHub-hosted Linux runners). A
physical device is still required for camera/RAW/IMU/GNSS behaviour and for performance figures
representative of real field hardware.

### Release

```bat
gradlew.bat :app:assembleRelease
```

Release builds enable R8 and resource shrinking. Signing is supplied through the
`SOILCARBON_KEYSTORE_PATH`, `SOILCARBON_KEYSTORE_PASSWORD`, `SOILCARBON_KEY_ALIAS` and
`SOILCARBON_KEY_PASSWORD` environment variables; artifacts are unsigned when they are absent.
SQLCipher native libraries and the `tflite` asset are kept uncompressed by the packaging rules.

---

## Capture workflow

1. **Onboarding** — choose a role, set bulk density and sampling depth, record GDPR consent.
2. **Align** — place the card over level soil, fill the frame, avoid shadow across the patches.
3. **Lock** — open *Camera controls* to set ISO, shutter, focus and white balance; use `RAW + JPEG`.
4. **Gate** — wait for tilt ≤ 10°, sufficient lux, all five markers and six patches.
5. **Capture** — the images and sensor/location context are encrypted and written atomically.
6. **Analyse** — rectify aperture and patches, fit the six-patch transfer function, compute colour,
   GLCM and covariates, build the feature vector, run inference, hash provenance.
7. **Review** — inspect every scientific field, export CSV/GeoJSON/JSON or delete the record.

Captures are retained locally by default. When *Allow network covariate lookup* is enabled, analysis
sends the sample coordinates to SoilGrids, Open-Meteo and Sentinel to request covariate values;
sample images and records are never uploaded. Disable the setting for a fully offline workflow, or
use *Refresh covariates* to update the cached locations on demand.

### Role-based navigation

| Role | Tabs |
|---|---|
| Field Technician | Capture, Review, Settings |
| Agronomist | Dashboard, Review, Map, Settings |
| Data Scientist | Dashboard, Review, Map, Settings |

The role is stored in DataStore and can be changed at any time in Settings.

---

## Calibration card

The printable target is shipped at:

- `app/src/main/assets/calibration/calibration_target.json` — machine-readable geometry
- `app/src/main/assets/calibration/calibration_target.png` — 304.8 DPI raster (2160 × 1680 px)
- `app/src/main/assets/calibration/calibration_target.svg` — vector source

Physical specification:

| Item | Value |
|---|---|
| Card size | 180 × 140 mm |
| Corner ArUco markers | IDs 20, 21, 22, 23, 22 mm |
| Centre ArUco marker | ID 24, 22 mm |
| Calibration patches | 22 mm, 99 / 80 / 60 / 40 / 20 / 5 % nominal reflectance |
| Soil aperture | 30 × 30 mm cut-out |
| Scale check | 100 mm printed bar |

### Printing checklist

1. Print at **100 % scale**; disable “fit to page” and “shrink oversized pages”.
2. Use matte synthetic stock (Yupo or polycarbonate). Do not use glossy photo paper.
3. Measure the 100 mm scale bar with calipers; reject the print if it is off by more than 1 mm.
4. Cut the 30 × 30 mm aperture cleanly. Keep at least one white module of quiet zone around every
   ArUco marker.
5. Laminate only with a matte, non-UV-shifting film — gloss creates specular highlights.
6. Replace the card when patches fade, scratch or become soil-stained.
7. Capture in diffuse daylight, D65-like conditions. Avoid direct sun, flash, coloured canopies and
   mixed indoor lighting.

The app validates the card JSON at startup. A missing or malformed asset falls back to the compiled-in
`CalibrationTargetSpec.default()` geometry; capture is never blocked by an asset problem.

---

## Model asset and training notes

Shipped placeholder:

- `app/src/main/assets/models/soc_model.tflite`
- `app/src/main/assets/models/soc_model_metadata.json`

The shipped placeholder is marked `"is_placeholder": true`, and the UI/predictions label it
accordingly. It exists so the application can build, run and exercise the full inference path before
a validated regional model is available.

### Model contract

| Property | Requirement |
|---|---|
| Input | `float32[1, 8]` |
| Feature order | `[L*, a*, b*, Munsell_Value, GLCM_Contrast, GLCM_Energy, NDVI_Sat, Clay_Content]` |
| Input scaling | `(value - feature_means[i]) / feature_scales[i]` |
| Output | `float32[1, 3]` = `[SOC %, sigma, confidence]` |
| Output scaling | `SOC = raw[0] * output_scale + output_offset` |
| σ / confidence | Used when `> 0`; otherwise the metadata fallback is used |
| Uncertainty floor | The predictor combines learned σ with feature novelty and moisture penalty |
| Format | Uncompressed float TFLite flatbuffer, > 1 KiB |

The metadata arrays **must** match the training preprocessing. If the order or scaling changes, bump
`FeatureVector.FEATURE_VECTOR_VERSION`, retrain, and update both the app and the model metadata.

### Recommended training pipeline

1. Collect paired samples: calibrated phone capture + ISO 10694 dry-combustion SOC reference.
2. Stratify by soil colour, clay class, moisture and region. Keep an untouched hold-out site split.
3. Extract the eight features with the same code paths used on-device (`ColorScience`,
   `GlcmAnalyzer`, `FeatureVectorFactory`).
4. Fit imputation and scaling on the training split only; export the means and scales into
   `soc_model_metadata.json`.
5. Train a compact regressor with a physically plausible output range and a calibrated uncertainty
   head (quantile, heteroscedastic Gaussian, or ensemble σ).
6. Report RMSE, MAE, R², bias and 95 % interval coverage on the site-held-out test set.
7. Convert to TFLite and verify the input/output shapes on a device, not only in Python.
8. Replace `soc_model.tflite`, update the metadata, bump `model_version`, and record the model SHA-256.

### Conversion sketch

```python
import tensorflow as tf

model = tf.keras.models.load_model("soc_model.keras")
converter = tf.lite.TFLiteConverter.from_keras_model(model)
converter.optimizations = []          # keep float32 for the reference path
tflite_model = converter.convert()
open("app/src/main/assets/models/soc_model.tflite", "wb").write(tflite_model)
```

Validate the flatbuffer before shipping:

```python
import tensorflow as tf
interpreter = tf.lite.Interpreter(
    model_path="app/src/main/assets/models/soc_model.tflite"
)
interpreter.allocate_tensors()
print(interpreter.get_input_details()[0]["shape"])   # [1, 8]
print(interpreter.get_output_details()[0]["shape"])  # [1, 3]
```

The app never invents a SOC value when the model is missing: `TfliteSocModel` surfaces the load
failure, `AnalyzeSampleUseCase` converts it into a visible rejection, and no sample is stored as
analysed. Release builds also block the placeholder model; debug builds label its output
**DEMO – NOT VALID**.

### Regenerating the placeholder and card assets

```bat
python tools\generate_assets.py
```

Requirements: Python 3.10+, `opencv-contrib-python`, `numpy`, `tensorflow >= 2.16`. The script rewrites
the JSON/PNG/SVG card and the placeholder TFLite model with a fresh SHA-256. Do not use the
regenerated placeholder for operational decisions.

---

## Data, privacy and security

- **Database** — Room over SQLCipher with a 256-bit passphrase held by `KeystoreSecretStore`.
- **Images** — AES-256-GCM streaming encryption under `filesDir/secure`, keyed from Android Keystore.
- **Provenance** — SHA-256 over a canonical payload; RAW and JPEG digests are stored separately.
- **Backups** — disabled (`allowBackup=false`); data-extraction rules exclude sensitive files.
- **Network** — covariate requests are rate-limited and optional. Disable *Allow network covariate
  lookup* for a fully offline workflow. Cached results are used when offline; regional defaults are
  the final fallback and are flagged in the UI.
- **Consent** — onboarding records GDPR consent locally; no account or cloud identity is created.
- **Deletion** — deleting a sample removes its database row and encrypted image files in one
  lifecycle unit. If an encrypted file cannot be removed, the sample is kept so the delete can be
  retried and no orphaned file is left behind. Use Android’s app-storage controls for a full wipe.
- **Covariate network requests** — when network lookup is enabled, sample coordinates are sent to
  SoilGrids, Open-Meteo and Sentinel. No image or sample record is uploaded.

Exports are written to `cacheDir/exports` and shared read-only through `FileProvider`; the provider
exposes only that directory. Treat exported CSV/GeoJSON/JSON as sensitive: they contain location,
image hashes, model identity and soil properties.

---

## Offline behaviour

| Capability | Online | Offline |
|---|---|---|
| Capture, calibration, colour, GLCM, inference | ✓ | ✓ |
| Covariates | SoilGrids / Sentinel / Open-Meteo | Last cache, then regional defaults |
| Maps | osmdroid tile download | Cached tiles only |
| Exports | Local files + share sheet | Local files + share sheet |
| Model update | Manual asset replacement | Existing asset |

The application is offline-first by design: every capture and analysis path must complete without a
network connection.

---

## Validation and limitations

- The **shipped model is a placeholder** with sidecar metrics from a synthetic training fixture. Its
  reported fit is not evidence of field accuracy.
- Visible-colour SOC prediction is sensitive to moisture, soil roughness, iron oxides, organic
  matter composition, camera spectral response and illumination. The calibration card corrects
  intensity and tone response, not the fundamental spectral ambiguity.
- RAW support, manual sensor controls and Camera2 interop vary by device and OEM. The app records the
  actual exposure parameters and whether RAW was available; it does not claim RAW on unsupported
  hardware.
- Munsell notation is approximate and intended for QC display, not as a replacement for a soil colour
  chart.
- GLCM features depend on aperture rectification, focus and texture scale; saturated contrast is
  flagged by the feature validator.
- GPS accuracy is recorded, but the app does not guarantee a survey-grade location.
- Satellite NDVI and SoilGrids values are coarse covariates, not local measurements.
- Carbon stock uses `SOC% × bulk density × depth` with user-supplied defaults; it is not a
  volumetric-equivalent stock calculation.
- Do not use this application for carbon-credit trading, regulatory reporting or laboratory
  certification without independent validation and a documented quality-management system.

---

## Not implemented

The following capabilities appear in older project documents but are **not present in this app**:

- **Cloud upload / account sync** — there is no server backend, account system or sample upload.
  `CLOUD_SYNC_WCS.md` is a design note only. Network access is limited to optional point covariate
  lookups (SoilGrids, Open-Meteo, Sentinel).
- **Agronomist regional calibration overlays** — the Agronomist role gets the dashboard and map, not
  regional overlay editing.
- **Data-scientist model management, spectral database sync and batch reprocessing** — the Data
  Scientist role shares the dashboard/map/review/export workspace. Models are shipped as app assets;
  there is no in-app download, replacement or batch reprocessing.
- **Bluetooth / NIR spectrometer input** — no BLE code or permissions ship in the manifest.

---

## References

1. **Smartphone Soil Carbon SRS & Reference Manual** — `SRS-SOC-2026-V1` v2.4.0 (authoritative
   requirements).
2. **Smartphone-Based Soil Carbon Measurement Techniques** — method review.
3. **TerroChroma UI & Navigation Specification** — capture and navigation guidance.

Supporting repository documents: `COMPUTER_VISION_MODULE.md`, `DATA_EXPORT_MODULE.md`,
`MAP_INTEGRATION.md`, `GDPR_COMPLIANCE.md`, `PRIVACY_POLICY.md`.

Design note only (not implemented in this app): `CLOUD_SYNC_WCS.md`.

---

**Version:** 2.4.0  
**Application ID:** `com.soilcarbon.engine`  
**Last updated:** November 2026
