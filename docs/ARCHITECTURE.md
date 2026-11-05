# Architecture

## Principle
The model is the product; the app is delivery. Contracts come first, domain logic is pure Kotlin, and **nothing is user-visible unless it is wired end to end and tested**.

```
contracts/feature_spec.json  ──►  training/train_soc.py  ──►  soc_model.tflite + model_metadata.json
        │ (single source of truth)                                        │
        ▼                                                                 ▼
domain (pure Kotlin, JVM-tested)  ◄──────────────  ModelGate refuses mismatched / unvalidated models
  FeatureValidator → AnalyzeSample → Prediction(status, provenance, degraded)
  SampleService (row + blobs = one lifecycle unit)
        ▲  ports (interfaces)
        │
data (Android)  : TfliteSocModel, EncryptedBlobStore, RoomSampleRepository, CovariateRepository, ExportRepository
ui   (Compose)  : Capture / Review / Settings  (state in, events out; no logic)
```

## Layers and rules
| Layer | Contains | Rule |
|---|---|---|
| `domain` | spec, validation, gate, `AnalyzeSample`, `SampleService`, ports | No Android imports. Everything here is unit-tested on the JVM. |
| `data` | adapters implementing domain ports | Thin. Contract-tested against the same fakes' expectations. |
| `ui` | Compose screens + ViewModels | Calls use cases only. Never computes or validates. |

## Contracts
- **Feature spec** (`feature_spec.json`): names, order, units, ranges, required/optional, fallback. Order is positional input to the model, so it is versioned (`spec_version`).
- **Model metadata** (`model_metadata.schema.json`): feature order, `is_placeholder`, fallback features trained, target range, CV metrics, `validation.passed`, training-data hash.
- **Gate rules** (`ModelGate`): spec version equal; feature order equal; every fallback feature was simulated in training; non-placeholder models must have `validation.passed` and a data hash.
- **Provenance**: every feature is `MEASURED`, `USER_ENTERED` or `DEFAULT`. Any `DEFAULT` sets `Prediction.degraded`.

## How each audit finding is made structurally impossible
| Finding | Mechanism |
|---|---|
| Placeholder model gives numbers | `PlaceholderPolicy.BLOCK` by default; demo mode stamps `DEMO_NOT_VALID`; train script cannot emit `is_placeholder=false` for synthetic data or a failed gate |
| `validate()` never called | Only `AnalyzeSample` can call the model, and it validates first (tested: model call count is 0 on invalid input) |
| NDVI constant | Fallback is explicit, flagged `DEFAULT`, `degraded=true`, and the model must be trained with it (gate) |
| Orphaned encrypted files | `SampleService.delete` removes blobs then row; failure keeps the row (retryable); `sweepOrphans` cleans legacy leftovers |
| Dead toggles | See "Definition of done" below |

## Adapters to write in the Android module (not included: need the SDK to build)
1. `TfliteSocModel : SocModel` loads `soc_model.tflite` + `model_metadata.json` (use `parseModelMetadata`). Delegate chain GPU → NNAPI → CPU, chosen from a settings value **passed in the constructor** and rebuilt on change.
2. `EncryptedBlobStore : BlobStore` over `EncryptedFileStore`, with `delete` and `listAll` (the missing pieces).
3. `RoomSampleRepository : SampleRepository`. One `@Transaction` per insert/delete.
4. `CaptureViewModel`: builds `Map<String, FeatureInput>` from calibrated colour, GLCM, moisture, covariates; calls `AnalyzeSample`; on success calls `SampleService.create`.
5. Export: `FileProvider.getUriForFile` + `ACTION_SEND`; exports include `status`, `degraded`, `provenance`, `model_id/version`.

## Definition of done (per feature)
1. Wired end to end: the setting or flag changes behaviour.
2. A test fails if the wiring breaks (e.g. toggle off ⇒ `schedulePeriodic` not called).
3. Runs on a physical device.
4. Failure states are visible to the user; no silent best-effort.
5. UI text only promises what is shipped.

## Known limits of this scaffold
- Feature list is **assumed** (colour L*a*b*, 3 GLCM, moisture, NDVI); edit `feature_spec.json` to match the SRS.
- Gate thresholds (R² ≥ 0.5, RPD ≥ 1.5, ≥ 5 sites, |bias| ≤ 0.15) are **placeholders** to agree with soil scientists.
- `sweepOrphans` must run at startup only (not concurrent with `create`).
- Camera RAW, ArUco, GNSS and TFLite delegates still need real-device validation.
