# SoilCarbon Core Integration — Final Audit

**Branch:** `feat/soilcarbon-core`
**Baseline:** `a5e8c92`
**Latest verified code commit:** `f50fc3f`
**Date:** 6 November 2026

## Deliverable status

| Requirement | Status | Commit / evidence |
|---|---|---|
| Step 0 — scaffold/app comparison; real 8-feature contract | **FIXED** | `contracts/feature_spec.json` rewritten to `L_star`, `a_star`, `b_star`, `munsell_value`, `glcm_contrast`, `glcm_energy`, `ndvi_satellite`, `clay_content_percent`; `dce56d7` |
| Step 1 — contracts, training, architecture, CI, pure-JVM `domain` | **FIXED** | `domain` module via version catalog; app depends on it; `dce56d7` |
| Step 2 — single `AnalyzeSample` inference path, TFLite, metadata, delegates, placeholder policy | **FIXED** | `TfliteSocModel`, `FeatureInputFactory`, `AnalyzeSampleUseCase`; debug `ALLOW_DEMO`, release `BLOCK`; `e8fb65d`; device-load defect fixed in `dabba65` |
| Step 3 — encrypted blob delete/list, transactional lifecycle, orphan sweep, review evidence | **FIXED** | `@Transaction` sample + blob operations, `PartialFailure`, startup `sweepOrphans()`; `ff1c25a`; device defect fixed in `dabba65` |
| Step 3 device round-trip / fault injection | **FIXED (device-verified)** | 3 instrumented tests pass in CI run `42177119114` |
| Step 4 — RAW gate, GPU toggle, auto-refresh, patch gate wiring | **FIXED** | `CaptureGate`, `AutoSyncCoordinator`, `GpuPreference`; UI renamed **Refresh covariates**; `fc4482c` |
| Step 5 — FileProvider sharing, SAF copy, required export fields | **FIXED** | `ExportSharer`, `ExportMapper`, narrowed provider paths, 45-column CSV; `d2837d2` |
| Step 5 device URI / grant test | **FIXED (device-verified)** | 4 instrumented tests pass in CI run `42177119114` |
| Step 6 — remove unsupported claims | **FIXED** | False permissions/role/README claims removed; legacy docs marked historical; `2773d1c` plus documentation follow-up |
| Step 7 — CI, release build, signing, App Bundle, R8, NFR tests | **FIXED** | JDK 21 CI, env-var signing, `bundleRelease`, R8, KVM device-test job; `aedf360`, device/NFR results from `f50fc3f` |
| Validated production model | **NOT DONE** | Placeholder remains; requires field data and ISO 10694 validation |
| Cloud upload / account sync | **NOT DONE** | No backend; design note only |
| Agronomist overlays; data-scientist model management, spectral sync, batch reprocessing | **REMOVED-FROM-UI** | No implementation or feature claim remains |
| BLE / NIR spectrometer | **NOT DONE** | No code or permissions ship |

## Tests and builds

| Check | Result |
|---|---|
| `./gradlew test` with JDK 21 | **Passed** — 94 JVM tests, 0 failures (35 domain, 59 app; release unit tests also pass) |
| `python -m pytest training/tests -q` | **Passed** — 8 tests |
| `:app:compileDebugAndroidTestKotlin` | **Passed** — 9 instrumented tests compile |
| `assembleRelease` + `bundleRelease` | **Passed** with R8 and resource shrinking |
| Env-var signing | **Verified locally** with an ephemeral key: `apksigner` and `jarsigner` both exit 0; test key deleted afterward. CI now verifies the signature when secrets exist |
| Instrumented tests | **Passed — 9/9** on an API 34 `google_apis` x86_64 emulator with KVM (CI run `42177119114`) |
| NFR-1 inference latency | **Passed** — median **0.208 ms**, p95 **0.313 ms** over 50 runs; limit 2,000 ms. CPU/NNAPI path, model inference only |
| NFR-2 offline capacity | **Passed** — 1,000 local-only samples inserted, closed, reopened and counted; plaintext probe absent; 1 database file; 2,787 ms total |
| GitHub Actions workflow | **Passed** — [run 42177119114](https://github.com/fcse23-018/radix-c/actions/runs/42177119114) at `f50fc3f`: `kotlin`, `python` and `instrumented` jobs all succeeded; release artifact uploaded |
| Local emulator | **Still unavailable** — VT-x is disabled in firmware and the session has no administrator rights; the AVD crashes under software emulation (`0xC0000005`). Device tests therefore run on GitHub-hosted Linux runners |

The device suite covers all nine tests:

- `SampleLifecycleInstrumentedTest` — 3 tests: encrypted create/delete round-trip, delete-failure recovery, and orphan sweep.
- `ExportShareInstrumentedTest` — 4 tests: FileProvider URI resolution, read grant, chooser, path rejection.
- `InferenceLatencyInstrumentedTest` — NFR-1, 5 warm-up and 50 measured CPU/NNAPI inferences.
- `OfflineCapacityInstrumentedTest` — NFR-2, 1,000 local-only samples in a SQLCipher Room database, close/reopen, plaintext-absence check.

The NFR numbers above are functional evidence on an emulated device. They are not a substitute for a
benchmark on the target mid-tier physical phone.

## Defects found by the device suite and fixed

Running the suite in CI exposed two defects that the JVM suite could not see:

1. **Every encrypted file write failed on Android.** `EncryptedFileStore` supplied a caller-generated
   IV to an Android Keystore key created with randomized encryption required, which throws
   `InvalidAlgorithmParameterException`. Fixed in `dabba65` by letting the Keystore cipher generate
   the IV and storing it in the envelope; decryption still uses the stored IV.
2. **Every model load failed on Android.** `TfliteSocModel` passed a heap `ByteBuffer` to
   `Interpreter`, which requires a direct buffer, so GPU, NNAPI and CPU delegates all failed. Fixed
   in `dabba65` with a direct buffer (duplicated per delegate attempt) and a surfaced root cause.
3. **Orphan-sweep test fixture was invalid.** The test created a live sample with no blobs, then
   expected a blob to remain. Fixed in `d308f52` to create the live sample with a blob.

## Release size

| Artifact | Unminified release | R8 + resource shrinking | Reduction |
|---|---:|---:|---:|
| Universal APK | 232,292,668 B (221.53 MiB) | 185,851,652 B (177.24 MiB) | 46.4 MiB (20.0%) |
| AAB | 90,193,388 B (86.02 MiB) | 81,593,179 B (77.81 MiB) | 8.6 MiB (9.5%) |

The AAB is configured for ABI/density/language splits. Local release output is unsigned unless the
`SOILCARBON_*` signing variables are set.

## Step 6 removals and corrections

- Removed `POST_NOTIFICATIONS` and all Bluetooth/NIR permissions.
- Removed the “heuristic fallback”; missing models now report **Model unavailable** and fail closed.
- Removed fake fallback validation metrics.
- Corrected role descriptions, settings copy and onboarding consent.
- README now lists cloud upload, agronomist overlays, data-scientist tooling and BLE/NIR as **not implemented**.
- `CLOUD_SYNC_WCS.md` marked design-only.
- Legacy TerroChroma KMP status, privacy, GDPR, subscription, map, vision, export and Play-readiness documents are marked historical/superseded.
- Play-readiness permission, billing, export, cloud-sync, BLE and signing sections corrected to match the shipped app.

## Assumptions and scaffold-vs-project differences

- The app’s real eight features replace the scaffold feature list and order.
- The project’s Kotlin 2.0.21/AGP/version catalog and JUnit 4 stack were kept.
- `domain` is pure Kotlin/JVM with no own `repositories {}` block.
- The app uses a suspend, rich-model `SampleService`; the pure-JVM scaffold service remains for contract tests.
- The shipped sidecar combines the scaffold schema with placeholder TFLite normalization constants; it remains `is_placeholder: true` with `validation.passed: false`.
- Model gate thresholds remain scaffold defaults: R² ≥ 0.5, RPD ≥ 1.5, ≥ 5 sites, |bias| ≤ 0.15.
- Exports contain 45 CSV columns; a JVM test keeps header and row in lockstep.

## Remaining user / hardware needs

1. A validated field dataset and trained model, validated against ISO 10694.
2. A physical mid-tier Android device to confirm NFR-1 latency under real thermal/CPU conditions and
   to exercise camera/RAW/IMU/GNSS paths that an emulator cannot represent.
3. A production release keystore plus `SOILCARBON_KEYSTORE_BASE64`,
   `SOILCARBON_KEYSTORE_PASSWORD`, `SOILCARBON_KEY_ALIAS` and `SOILCARBON_KEY_PASSWORD` repository
   secrets. Without them CI uploads unsigned artifacts; the workflow now verifies signatures when
   they are present.
4. A Sentinel Hub token if satellite NDVI is required; otherwise NDVI remains at the documented default.
5. A decision on whether the current model-gate thresholds suit the deployment region.
