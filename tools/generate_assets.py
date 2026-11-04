"""
Asset generator for SoilCarbon Engine (SRS-SOC-2026-V1).

Produces:
  app/src/main/assets/calibration/calibration_target.json
  app/src/main/assets/calibration/calibration_target.png
  app/src/main/assets/calibration/calibration_target.svg
  app/src/main/assets/models/soc_model.tflite
  app/src/main/assets/models/soc_model_metadata.json

The card geometry mirrors CalibrationTargetSpec.default() and the TFLite model is an explicitly
labelled PLACEHOLDER trained on the in-app heuristic. Replace both with validated artefacts before
regulatory or carbon-credit use.

Requirements: Python 3.10+, opencv-contrib-python, numpy, tensorflow >= 2.16.
Usage: python tools/generate_assets.py
"""

from __future__ import annotations

import hashlib
import json
import math
import os
from datetime import datetime, timezone

import cv2
import numpy as np

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
ASSETS = os.path.join(ROOT, "app", "src", "main", "assets")
CALIB_DIR = os.path.join(ASSETS, "calibration")
MODEL_DIR = os.path.join(ASSETS, "models")

CARD_VERSION = "SOC-CARD-2026.1"
CARD_WIDTH_MM = 180.0
CARD_HEIGHT_MM = 140.0
MARKER_SIZE_MM = 22.0

MARKERS = [
    {"id": 20, "role": "TOP_LEFT", "center": {"xMm": 16.0, "yMm": 16.0}, "sizeMm": MARKER_SIZE_MM},
    {"id": 21, "role": "TOP_RIGHT", "center": {"xMm": 164.0, "yMm": 16.0}, "sizeMm": MARKER_SIZE_MM},
    {"id": 22, "role": "BOTTOM_RIGHT", "center": {"xMm": 164.0, "yMm": 124.0}, "sizeMm": MARKER_SIZE_MM},
    {"id": 23, "role": "BOTTOM_LEFT", "center": {"xMm": 16.0, "yMm": 124.0}, "sizeMm": MARKER_SIZE_MM},
    {"id": 24, "role": "CENTER_ANCHOR", "center": {"xMm": 90.0, "yMm": 16.0}, "sizeMm": MARKER_SIZE_MM},
]

PATCHES = [
    {"id": "P99", "name": "99% white", "targetReflectance": 0.99, "rect": {"leftMm": 10.0, "topMm": 38.0, "widthMm": 22.0, "heightMm": 22.0}},
    {"id": "P80", "name": "80% grey", "targetReflectance": 0.80, "rect": {"leftMm": 37.0, "topMm": 38.0, "widthMm": 22.0, "heightMm": 22.0}},
    {"id": "P60", "name": "60% grey", "targetReflectance": 0.60, "rect": {"leftMm": 64.0, "topMm": 38.0, "widthMm": 22.0, "heightMm": 22.0}},
    {"id": "P40", "name": "40% grey", "targetReflectance": 0.40, "rect": {"leftMm": 91.0, "topMm": 38.0, "widthMm": 22.0, "heightMm": 22.0}},
    {"id": "P20", "name": "20% grey", "targetReflectance": 0.20, "rect": {"leftMm": 118.0, "topMm": 38.0, "widthMm": 22.0, "heightMm": 22.0}},
    {"id": "P05", "name": "5% black", "targetReflectance": 0.05, "rect": {"leftMm": 145.0, "topMm": 38.0, "widthMm": 22.0, "heightMm": 22.0}},
]

APERTURE = {"leftMm": 75.0, "topMm": 77.0, "widthMm": 30.0, "heightMm": 30.0}

SPEC = {
    "version": CARD_VERSION,
    "cardWidthMm": CARD_WIDTH_MM,
    "cardHeightMm": CARD_HEIGHT_MM,
    "markers": MARKERS,
    "patches": PATCHES,
    "aperture": APERTURE,
    "patchSampleInsetFraction": 0.20,
    "substrate": "Matte synthetic polymer (Yupo/polycarbonate)",
    "notes": (
        "Print at 100% scale on matte synthetic stock. Verify the 100 mm scale bar after printing. "
        "The aperture is a physical cut-out; place the card on a flat, level soil surface."
    ),
}

FEATURE_ORDER = [
    "L_star",
    "a_star",
    "b_star",
    "munsell_value",
    "glcm_contrast",
    "glcm_energy",
    "ndvi_satellite",
    "clay_content_percent",
]
FEATURE_MEANS = [42.0, 6.0, 14.0, 5.0, 0.35, 0.25, 0.35, 25.0]
FEATURE_SCALES = [18.0, 11.0, 14.0, 3.0, 0.30, 0.20, 0.25, 12.0]


# --------------------------------------------------------------------------------------
# Calibration card
# --------------------------------------------------------------------------------------
def write_spec_json() -> None:
    os.makedirs(CALIB_DIR, exist_ok=True)
    path = os.path.join(CALIB_DIR, "calibration_target.json")
    with open(path, "w", encoding="utf-8") as handle:
        json.dump(SPEC, handle, indent=2)
        handle.write("\n")
    print(f"wrote {path}")


def draw_card_png(px_per_mm: int = 12) -> None:
    width = int(round(CARD_WIDTH_MM * px_per_mm))
    height = int(round(CARD_HEIGHT_MM * px_per_mm))
    canvas = np.full((height, width, 3), 255, dtype=np.uint8)

    def x(px: float) -> int:
        return int(round(px * px_per_mm))

    # Outer cutting guide.
    cv2.rectangle(canvas, (2, 2), (width - 3, height - 3), (40, 40, 40), 2)

    dictionary = cv2.aruco.getPredefinedDictionary(cv2.aruco.DICT_4X4_50)
    for marker in MARKERS:
        side = int(round(marker["sizeMm"] * px_per_mm))
        marker_img = cv2.aruco.generateImageMarker(dictionary, marker["id"], side)
        marker_bgr = cv2.cvtColor(marker_img, cv2.COLOR_GRAY2BGR)
        cx = x(marker["center"]["xMm"])
        cy = x(marker["center"]["yMm"])
        half = side // 2
        canvas[cy - half:cy - half + side, cx - half:cx - half + side] = marker_bgr

    for patch in PATCHES:
        rect = patch["rect"]
        left, top = x(rect["leftMm"]), x(rect["topMm"])
        right, bottom = x(rect["leftMm"] + rect["widthMm"]), x(rect["topMm"] + rect["heightMm"])
        reflectance = patch["targetReflectance"]
        # Encode the linear reflectance into an 8-bit print value.
        encoded = int(round(255.0 * (reflectance ** (1.0 / 2.2))))
        cv2.rectangle(canvas, (left, top), (right, bottom), (encoded, encoded, encoded), -1)
        cv2.rectangle(canvas, (left, top), (right, bottom), (20, 20, 20), 2)
        text_color = (10, 10, 10) if reflectance >= 0.45 else (245, 245, 245)
        label = patch["id"]
        font_scale = 0.55
        thickness = 1
        (tw, th), _ = cv2.getTextSize(label, cv2.FONT_HERSHEY_SIMPLEX, font_scale, thickness)
        cv2.putText(
            canvas,
            label,
            ((left + right - tw) // 2, (top + bottom + th) // 2),
            cv2.FONT_HERSHEY_SIMPLEX,
            font_scale,
            text_color,
            thickness,
            cv2.LINE_AA,
        )

    # Aperture cutting guide.
    ap = APERTURE
    left, top = x(ap["leftMm"]), x(ap["topMm"])
    right, bottom = x(ap["leftMm"] + ap["widthMm"]), x(ap["topMm"] + ap["heightMm"])
    for start, end in (
        ((left, top), (right, top)),
        ((right, top), (right, bottom)),
        ((right, bottom), (left, bottom)),
        ((left, bottom), (left, top)),
    ):
        # Dashed guide.
        cv2.line(canvas, start, end, (30, 30, 30), 2, cv2.LINE_AA)
    cv2.putText(
        canvas,
        "CUT OUT",
        (left + 8, top + int(ap["heightMm"] * px_per_mm / 2) - 2),
        cv2.FONT_HERSHEY_SIMPLEX,
        0.7,
        (30, 30, 30),
        2,
        cv2.LINE_AA,
    )
    cv2.putText(
        canvas,
        "30 x 30 mm",
        (left + 14, top + int(ap["heightMm"] * px_per_mm / 2) + 20),
        cv2.FONT_HERSHEY_SIMPLEX,
        0.5,
        (30, 30, 30),
        1,
        cv2.LINE_AA,
    )

    # 100 mm scale bar (verification after printing).
    bar_left = x(62.0)
    bar_top = x(68.0)
    bar_right = x(162.0)
    cv2.line(canvas, (bar_left, bar_top), (bar_right, bar_top), (20, 20, 20), 3)
    for mm in range(0, 101, 10):
        tick_x = x(62.0 + mm)
        cv2.line(canvas, (tick_x, bar_top - 6), (tick_x, bar_top + 6), (20, 20, 20), 2)
    cv2.putText(
        canvas,
        "100 mm",
        (x(100.0), bar_top - 14),
        cv2.FONT_HERSHEY_SIMPLEX,
        0.55,
        (20, 20, 20),
        1,
        cv2.LINE_AA,
    )

    cv2.putText(
        canvas,
        f"SoilCarbon Engine  |  Calibration Target  |  {CARD_VERSION}",
        (x(46.0), x(64.5)),
        cv2.FONT_HERSHEY_SIMPLEX,
        0.62,
        (20, 20, 20),
        2,
        cv2.LINE_AA,
    )
    cv2.putText(
        canvas,
        "Print at 100% scale on matte stock. Quiet zone is mandatory around every marker.",
        (x(39.0), x(111.5)),
        cv2.FONT_HERSHEY_SIMPLEX,
        0.46,
        (30, 30, 30),
        1,
        cv2.LINE_AA,
    )
    cv2.putText(
        canvas,
        "D65 diffuse daylight only. Patch order: 99 / 80 / 60 / 40 / 20 / 5 % reflectance.",
        (x(31.0), x(116.0)),
        cv2.FONT_HERSHEY_SIMPLEX,
        0.46,
        (30, 30, 30),
        1,
        cv2.LINE_AA,
    )

    path = os.path.join(CALIB_DIR, "calibration_target.png")
    cv2.imwrite(path, canvas)
    print(f"wrote {path} ({width}x{height}px @ {px_per_mm * 25.4:.1f} DPI)")


def draw_card_svg() -> None:
    def rect_svg(rect: dict, fill: str, stroke: str = "#222222", width: float = 0.4) -> str:
        return (
            f'<rect x="{rect["leftMm"]}" y="{rect["topMm"]}" width="{rect["widthMm"]}" '
            f'height="{rect["heightMm"]}" fill="{fill}" stroke="{stroke}" stroke-width="{width}" />'
        )

    parts = [
        '<?xml version="1.0" encoding="UTF-8"?>',
        f'<svg xmlns="http://www.w3.org/2000/svg" width="180mm" height="140mm" '
        f'viewBox="0 0 {CARD_WIDTH_MM} {CARD_HEIGHT_MM}">',
        '<rect width="180" height="140" fill="#ffffff" />',
        '<rect x="0.2" y="0.2" width="179.6" height="139.6" fill="none" stroke="#222" stroke-width="0.3" />',
    ]
    for marker in MARKERS:
        side = marker["sizeMm"]
        cx, cy = marker["center"]["xMm"], marker["center"]["yMm"]
        parts.append(
            f'<rect x="{cx - side / 2}" y="{cy - side / 2}" width="{side}" height="{side}" '
            f'fill="#000000" />'
        )
        parts.append(
            f'<text x="{cx}" y="{cy + 1.5}" font-family="monospace" font-size="4" '
            f'fill="#ffffff" text-anchor="middle">ID {marker["id"]}</text>'
        )
    for patch in PATCHES:
        encoded = int(round(255.0 * (patch["targetReflectance"] ** (1.0 / 2.2))))
        fill = f"#{encoded:02x}{encoded:02x}{encoded:02x}"
        parts.append(rect_svg(patch["rect"], fill))
        rect = patch["rect"]
        parts.append(
            f'<text x="{rect["leftMm"] + rect["widthMm"] / 2}" '
            f'y="{rect["topMm"] + rect["heightMm"] / 2 + 1.5}" '
            f'font-family="monospace" font-size="4" '
            f'fill="{"#111" if patch["targetReflectance"] >= 0.45 else "#f5f5f5"}" '
            f'text-anchor="middle">{patch["id"]}</text>'
        )
    parts.append(rect_svg(APERTURE, "none", "#222", 0.5))
    parts.append(
        f'<text x="{APERTURE["leftMm"] + APERTURE["widthMm"] / 2}" '
        f'y="{APERTURE["topMm"] + APERTURE["heightMm"] / 2}" font-family="sans-serif" '
        f'font-size="3.4" fill="#222" text-anchor="middle">CUT OUT 30 x 30 mm</text>'
    )
    parts.append(
        '<line x1="62" y1="68" x2="162" y2="68" stroke="#222" stroke-width="0.6" />'
    )
    for mm in range(0, 101, 10):
        parts.append(
            f'<line x1="{62 + mm}" y1="66.5" x2="{62 + mm}" y2="69.5" stroke="#222" stroke-width="0.4" />'
        )
    parts.append(
        '<text x="112" y="65" font-family="sans-serif" font-size="2.4" fill="#222" '
        'text-anchor="middle">100 mm</text>'
    )
    parts.append(
        f'<text x="90" y="63.5" font-family="sans-serif" font-size="3.2" font-weight="bold" '
        f'fill="#222" text-anchor="middle">SoilCarbon Engine | Calibration Target | {CARD_VERSION}</text>'
    )
    parts.append(
        '<text x="90" y="111" font-family="sans-serif" font-size="2.3" fill="#333" '
        'text-anchor="middle">Print at 100% scale on matte stock. D65 diffuse daylight only.</text>'
    )
    parts.append(
        '<text x="90" y="114.5" font-family="sans-serif" font-size="2.3" fill="#333" '
        'text-anchor="middle">Patch order: 99 / 80 / 60 / 40 / 20 / 5 % reflectance.</text>'
    )
    parts.append("</svg>")

    path = os.path.join(CALIB_DIR, "calibration_target.svg")
    with open(path, "w", encoding="utf-8") as handle:
        handle.write("\n".join(parts))
        handle.write("\n")
    print(f"wrote {path}")


# --------------------------------------------------------------------------------------
# Placeholder TFLite model
# --------------------------------------------------------------------------------------
def heuristic_targets(features: np.ndarray) -> np.ndarray:
    """Mirror HeuristicSocRegressor for synthetic bootstrap targets."""
    l_star, b_star, energy, ndvi, clay_pct = (
        features[:, 0],
        features[:, 2],
        features[:, 5],
        features[:, 6],
        features[:, 7],
    )
    darkness = (100.0 - l_star) / 100.0
    redness = np.maximum(b_star - 8.0, 0.0) / 30.0
    roughness = 1.0 - energy
    clay = clay_pct / 100.0
    vegetation = np.clip(ndvi, -1.0, 1.0)
    soc = np.clip(
        0.28 + darkness * 3.85 + redness * 0.55 + roughness * 0.95 + clay * 1.10
        + np.maximum(vegetation, 0.0) * 0.65,
        0.05,
        15.0,
    )
    uncertainty = np.clip(
        soc * (0.17 + np.abs(0.55 - darkness) * 0.10) + 0.12,
        0.08,
        4.5,
    )
    confidence = np.clip(
        0.88
        - np.where(energy < 0.08, 0.08, 0.0)
        - np.where((soc >= 14.5) | (soc <= 0.08), 0.20, 0.0),
        0.30,
        0.92,
    )
    return np.stack([soc, uncertainty, confidence], axis=1)


def build_model() -> None:
    os.environ.setdefault("TF_CPP_MIN_LOG_LEVEL", "2")
    import tensorflow as tf

    tf.keras.utils.set_random_seed(20260201)
    rng = np.random.default_rng(20260201)
    count = 6000
    raw = np.stack(
        [
            rng.uniform(8.0, 82.0, count),      # L*
            rng.uniform(-8.0, 26.0, count),     # a*
            rng.uniform(-2.0, 42.0, count),     # b*
            rng.uniform(1.5, 8.5, count),       # Munsell value
            rng.uniform(0.0, 1.0, count),       # GLCM contrast
            rng.uniform(0.01, 0.92, count),     # GLCM energy
            rng.uniform(-0.25, 0.92, count),    # NDVI
            rng.uniform(3.0, 65.0, count),      # clay %
        ],
        axis=1,
    )
    targets = heuristic_targets(raw)
    x = (raw - np.asarray(FEATURE_MEANS)) / np.asarray(FEATURE_SCALES)

    model = tf.keras.Sequential(
        [
            tf.keras.layers.Input(shape=(8,), name="feature_vector"),
            tf.keras.layers.Dense(64, activation="relu"),
            tf.keras.layers.Dense(48, activation="relu"),
            tf.keras.layers.Dense(24, activation="relu"),
            tf.keras.layers.Dense(3, name="soc_uncertainty_confidence"),
        ]
    )
    model.compile(optimizer=tf.keras.optimizers.Adam(0.004), loss="mse", metrics=["mae"])
    model.fit(x, targets, epochs=220, batch_size=128, validation_split=0.15, verbose=0)

    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    converter.optimizations = []
    tflite_bytes = converter.convert()
    if len(tflite_bytes) <= 1024:
        raise RuntimeError("Converted TFLite model is implausibly small")

    os.makedirs(MODEL_DIR, exist_ok=True)
    model_path = os.path.join(MODEL_DIR, "soc_model.tflite")
    with open(model_path, "wb") as handle:
        handle.write(tflite_bytes)
    print(f"wrote {model_path} ({len(tflite_bytes)} bytes)")

    # Report sanity metrics on a held-out split so the metadata is not invented.
    raw_eval = np.stack(
        [
            rng.uniform(8.0, 82.0, 1500),
            rng.uniform(-8.0, 26.0, 1500),
            rng.uniform(-2.0, 42.0, 1500),
            rng.uniform(1.5, 8.5, 1500),
            rng.uniform(0.0, 1.0, 1500),
            rng.uniform(0.01, 0.92, 1500),
            rng.uniform(-0.25, 0.92, 1500),
            rng.uniform(3.0, 65.0, 1500),
        ],
        axis=1,
    )
    expected = heuristic_targets(raw_eval)
    predicted = model.predict((raw_eval - np.asarray(FEATURE_MEANS)) / np.asarray(FEATURE_SCALES), verbose=0)
    rmse = float(np.sqrt(np.mean((predicted[:, 0] - expected[:, 0]) ** 2)))
    ss_res = float(np.sum((expected[:, 0] - predicted[:, 0]) ** 2))
    ss_tot = float(np.sum((expected[:, 0] - expected[:, 0].mean()) ** 2))
    r2 = 1.0 - ss_res / ss_tot if ss_tot > 0 else 0.0

    digest = hashlib.sha256(tflite_bytes).hexdigest()
    metadata = {
        "model_id": "soc-visible-color-v1",
        "model_version": "1.0.0-placeholder",
        "description": (
            "PLACEHOLDER visible-colour SOC regressor bootstrapped from the in-app heuristic. "
            "Replace with a dry-combustion-validated regional model before operational use."
        ),
        "feature_order": FEATURE_ORDER,
        "training_dataset": "synthetic bootstrap (HeuristicSocRegressor), 6000 samples",
        "validation_rmse_percent": round(max(rmse, 0.05), 4),
        "validation_r2": round(max(0.0, min(1.0, r2)), 4),
        "uncertainty_mode": "learned-sigma",
        "feature_means": FEATURE_MEANS,
        "feature_scales": FEATURE_SCALES,
        "output_scale": 1.0,
        "output_offset": 0.0,
        "is_placeholder": True,
        "created_at_utc": datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
        "sha256": digest,
    }
    metadata_path = os.path.join(MODEL_DIR, "soc_model_metadata.json")
    with open(metadata_path, "w", encoding="utf-8") as handle:
        json.dump(metadata, handle, indent=2)
        handle.write("\n")
    print(f"wrote {metadata_path} (RMSE {rmse:.4f} SOC%, R2 {r2:.4f})")


def main() -> None:
    write_spec_json()
    draw_card_png()
    draw_card_svg()
    build_model()


if __name__ == "__main__":
    main()
