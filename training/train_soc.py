# %% [markdown]
# # SOC model training, validation gate and export
# Run as a script (`python train_soc.py --synthetic`) or open as a notebook
# (cells are `# %%` delimited; works in VS Code / Jupytext / `jupytext --to ipynb`).
#
# Pipeline: load spec -> load data -> leave-site-out CV -> go/no-go gate ->
# fit final model -> write model + metadata sidecar (+ optional TFLite).
#
# Hard rules enforced here:
#   * feature order comes ONLY from contracts/feature_spec.json
#   * synthetic data can NEVER produce is_placeholder=false
#   * validation is grouped by SITE (random splits leak and overstate accuracy)
#   * fallback features (e.g. ndvi_satellite) are simulated during training so the
#     model is valid when the app has to use the default

# %%
from __future__ import annotations
import argparse, hashlib, json, datetime as dt
from pathlib import Path
import numpy as np
import pandas as pd
from sklearn.ensemble import GradientBoostingRegressor
from sklearn.model_selection import GroupKFold

ROOT = Path(__file__).resolve().parent.parent
SPEC_PATH = ROOT / "contracts" / "feature_spec.json"
TARGET = "soc_pct"          # dry combustion, ISO 10694
GATE = {"min_r2": 0.50, "min_rpd": 1.5, "min_sites": 5, "max_abs_bias": 0.15}  # ASSUMED: agree with agronomists

def load_spec(path: Path = SPEC_PATH) -> dict:
    spec = json.loads(path.read_text())
    names = [f["name"] for f in spec["features"]]
    assert len(names) == len(set(names)), "duplicate feature names"
    for f in spec["features"]:
        assert f["min"] < f["max"], f"bad range for {f['name']}"
        if f["fallback"] is not None:
            assert f["min"] <= f["fallback"] <= f["max"], f"fallback out of range: {f['name']}"
        assert f["required"] or f["fallback"] is not None, f"optional feature needs fallback: {f['name']}"
    return spec

# %% [markdown]
# ## Data
# Real CSV columns: every feature name, `soc_pct`, `site_id`. Missing optional
# features (blank) are filled with the spec fallback, exactly as the app does.

# %%
def _synthetic_value(name: str, spec: dict, rng: np.random.Generator) -> float:
    """Plausible soil-range value for a known feature; uniform in spec range otherwise."""
    known = {
        "L_star": (15.0, 65.0),
        "a_star": (2.0, 18.0),
        "b_star": (5.0, 32.0),
        "munsell_value": (2.0, 8.0),
        "glcm_contrast": (0.0, 1.0),
        "glcm_energy": (0.0, 1.0),
        "ndvi_satellite": (0.1, 0.8),
        "clay_content_percent": (5.0, 60.0),
    }
    if name in known:
        return rng.uniform(*known[name])
    f = next(x for x in spec["features"] if x["name"] == name)
    return rng.uniform(f["min"], f["max"])


def make_synthetic(spec: dict, n_sites: int = 12, per_site: int = 40, seed: int = 0) -> pd.DataFrame:
    """Plausible-shaped fake data to exercise the pipeline. NOT for any real use."""
    rng = np.random.default_rng(seed)
    rows = []
    for s in range(n_sites):
        site_shift = rng.normal(0, 0.25)
        for _ in range(per_site):
            row = {f["name"]: _synthetic_value(f["name"], spec, rng) for f in spec["features"]}
            soc = (4.5 - 0.06 * row["L_star"] + 0.02 * row["b_star"]
                   + 0.03 * row["clay_content_percent"] + 0.8 * row["ndvi_satellite"]
                   - 1.0 * row["glcm_energy"] + site_shift + rng.normal(0, 0.3))
            row["soc_pct"] = max(soc, 0.05)
            row["site_id"] = f"site{s}"
            rows.append(row)
    return pd.DataFrame(rows)

def load_csv(path: Path, spec: dict) -> pd.DataFrame:
    df = pd.read_csv(path)
    for f in spec["features"]:
        if f["name"] not in df:
            raise ValueError(f"missing column {f['name']}")
        if f["fallback"] is not None:
            df[f["name"]] = df[f["name"]].fillna(f["fallback"])
        elif df[f["name"]].isna().any():
            raise ValueError(f"required feature {f['name']} has blanks")
        bad = ((df[f["name"]] < f["min"]) | (df[f["name"]] > f["max"])).sum()
        if bad:
            raise ValueError(f"{bad} rows out of spec range for {f['name']}")
    for col in (TARGET, "site_id"):
        if col not in df:
            raise ValueError(f"missing column {col}")
    return df

def augment_fallback(df: pd.DataFrame, spec: dict, frac: float, seed: int = 1) -> pd.DataFrame:
    """Replace optional features with their fallback in `frac` of rows so the model learns the degraded case."""
    rng = np.random.default_rng(seed)
    out = df.copy()
    for f in spec["features"]:
        if f["fallback"] is not None and frac > 0:
            mask = rng.random(len(out)) < frac
            out.loc[mask, f["name"]] = f["fallback"]
    return out

def data_hash(df: pd.DataFrame) -> str:
    return hashlib.sha256(pd.util.hash_pandas_object(df, index=False).values.tobytes()).hexdigest()

# %% [markdown]
# ## Validation: leave-site-out, with fallback-degraded scoring

# %%
def make_model(seed: int = 0) -> GradientBoostingRegressor:
    return GradientBoostingRegressor(n_estimators=300, max_depth=3, learning_rate=0.05,
                                     subsample=0.8, random_state=seed)

def metrics(y, p) -> dict:
    y, p = np.asarray(y), np.asarray(p)
    rmse = float(np.sqrt(np.mean((y - p) ** 2)))
    ss_res, ss_tot = np.sum((y - p) ** 2), np.sum((y - y.mean()) ** 2)
    return {"r2": float(1 - ss_res / ss_tot), "rmse": rmse,
            "rpd": float(np.std(y, ddof=1) / rmse), "bias": float(np.mean(p - y))}

def cross_site_cv(df: pd.DataFrame, spec: dict, fallback_frac: float) -> dict:
    names = [f["name"] for f in spec["features"]]
    n_sites = df["site_id"].nunique()
    folds = GroupKFold(n_splits=min(5, n_sites))
    oof = np.zeros(len(df))
    for tr, te in folds.split(df, groups=df["site_id"]):
        train = augment_fallback(df.iloc[tr], spec, fallback_frac)
        model = make_model().fit(train[names], train[TARGET])
        oof[te] = model.predict(df.iloc[te][names])      # held-out sites, real features
    result = metrics(df[TARGET], oof)
    # degraded case: every optional feature forced to its fallback at test time
    degraded = df.copy()
    for f in spec["features"]:
        if f["fallback"] is not None:
            degraded[f["name"]] = f["fallback"]
    oof_d = np.zeros(len(df))
    for tr, te in folds.split(df, groups=df["site_id"]):
        train = augment_fallback(df.iloc[tr], spec, fallback_frac)
        model = make_model().fit(train[names], train[TARGET])
        oof_d[te] = model.predict(degraded.iloc[te][names])
    result["degraded"] = metrics(df[TARGET], oof_d)
    result["baseline_rmse"] = float(np.std(df[TARGET], ddof=0))   # predict-the-mean reference
    return result

def gate(result: dict, n_sites: int) -> bool:
    return (result["r2"] >= GATE["min_r2"] and result["rpd"] >= GATE["min_rpd"]
            and n_sites >= GATE["min_sites"] and abs(result["bias"]) <= GATE["max_abs_bias"])

# %% [markdown]
# ## Fit, export, metadata

# %%
def export_tflite(df, spec, out_dir: Path, fallback_frac: float) -> bool:
    """Keras MLP -> TFLite (normalisation baked in). Skipped if tensorflow is absent."""
    try:
        import tensorflow as tf
    except ImportError:
        print("tensorflow not installed: skipping TFLite export (sklearn model still written)")
        return False
    names = [f["name"] for f in spec["features"]]
    train = augment_fallback(df, spec, fallback_frac)
    X, y = train[names].to_numpy("float32"), train[TARGET].to_numpy("float32")
    norm = tf.keras.layers.Normalization(); norm.adapt(X)
    m = tf.keras.Sequential([tf.keras.Input((len(names),)), norm,
                             tf.keras.layers.Dense(32, activation="relu"),
                             tf.keras.layers.Dense(16, activation="relu"), tf.keras.layers.Dense(1)])
    m.compile("adam", "mse"); m.fit(X, y, epochs=200, batch_size=32, verbose=0)
    conv = tf.lite.TFLiteConverter.from_keras_model(m)
    (out_dir / "soc_model.tflite").write_bytes(conv.convert())
    return True

def run(df: pd.DataFrame, spec: dict, out_dir: Path, synthetic: bool, version: str,
        fallback_frac: float = 0.3, tflite: bool = False) -> dict:
    out_dir.mkdir(parents=True, exist_ok=True)
    names = [f["name"] for f in spec["features"]]
    n_sites = int(df["site_id"].nunique())
    cv = cross_site_cv(df, spec, fallback_frac)
    passed = gate(cv, n_sites)
    final = make_model().fit(augment_fallback(df, spec, fallback_frac)[names], df[TARGET])
    import pickle; (out_dir / "sklearn_model.pkl").write_bytes(pickle.dumps(final))
    if tflite:
        export_tflite(df, spec, out_dir, fallback_frac)
    meta = {
        "model_id": "soc-gbr" if not synthetic else "soc-synthetic-demo",
        "model_version": version,
        "spec_version": spec["spec_version"],
        "feature_order": names,
        "is_placeholder": bool(synthetic or not passed),   # synthetic or failed gate => never reportable
        "fallback_features_trained": [f["name"] for f in spec["features"] if f["fallback"] is not None],
        "target": {"name": "soc", "unit": "% dry mass", "min": 0.0, "max": 20.0},
        "training": {"data_sha256": data_hash(df), "n_samples": int(len(df)), "n_sites": n_sites,
                     "created_utc": dt.datetime.now(dt.timezone.utc).isoformat(timespec="seconds"),
                     "lab_method": "ISO 10694 dry combustion" if not synthetic else "SYNTHETIC"},
        "validation": {"scheme": "group-k-fold by site", "r2": cv["r2"], "rmse": cv["rmse"],
                       "rpd": cv["rpd"], "bias": cv["bias"], "passed": bool(passed and not synthetic),
                       "thresholds": GATE},
    }
    (out_dir / "model_metadata.json").write_text(json.dumps(meta, indent=2))
    (out_dir / "validation_report.json").write_text(json.dumps(cv, indent=2))
    return {"meta": meta, "cv": cv}

# %%
if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("--csv", type=Path); ap.add_argument("--synthetic", action="store_true")
    ap.add_argument("--out", type=Path, default=Path("build")); ap.add_argument("--version", default="0.1.0")
    ap.add_argument("--fallback-frac", type=float, default=0.3); ap.add_argument("--tflite", action="store_true")
    a = ap.parse_args()
    spec = load_spec()
    if a.synthetic:
        df = make_synthetic(spec)
    elif a.csv:
        df = load_csv(a.csv, spec)
    else:
        ap.error("provide --csv or --synthetic")
    r = run(df, spec, a.out, a.synthetic, a.version, a.fallback_frac, a.tflite)
    print(json.dumps({k: r["cv"][k] for k in ("r2", "rmse", "rpd", "bias")}, indent=2))
    print("is_placeholder:", r["meta"]["is_placeholder"], "| gate passed:", r["meta"]["validation"]["passed"])
