import json, sys
from pathlib import Path
import jsonschema, pandas as pd, pytest
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import train_soc as t

ROOT = Path(__file__).resolve().parents[2]
SCHEMA = json.loads((ROOT / "contracts" / "model_metadata.schema.json").read_text())

@pytest.fixture(scope="module")
def spec(): return t.load_spec()

@pytest.fixture(scope="module")
def synthetic_run(spec, tmp_path_factory):
    return t.run(t.make_synthetic(spec, n_sites=8, per_site=25), spec, tmp_path_factory.mktemp("o"), True, "0.0.1")

def test_spec_has_eight_unique_features(spec):
    assert len(spec["features"]) == 8

def test_metadata_matches_schema_and_spec_order(spec, synthetic_run):
    jsonschema.validate(synthetic_run["meta"], SCHEMA)
    assert synthetic_run["meta"]["feature_order"] == [f["name"] for f in spec["features"]]

def test_synthetic_data_can_never_be_reportable(synthetic_run):
    assert synthetic_run["meta"]["is_placeholder"] is True
    assert synthetic_run["meta"]["validation"]["passed"] is False

def test_validation_is_grouped_by_site(spec):
    df = t.make_synthetic(spec, n_sites=6, per_site=10)
    from sklearn.model_selection import GroupKFold
    for tr, te in GroupKFold(3).split(df, groups=df["site_id"]):
        assert not set(df.iloc[tr].site_id) & set(df.iloc[te].site_id)

def test_gate_rejects_weak_or_few_sites():
    good = {"r2": .7, "rpd": 2.0, "bias": 0.0}
    assert t.gate(good, 10)
    assert not t.gate({**good, "r2": .2}, 10)
    assert not t.gate(good, 2)
    assert not t.gate({**good, "bias": .5}, 10)

def test_csv_rejects_out_of_range(spec, tmp_path):
    df = t.make_synthetic(spec, 2, 5); df.loc[0, "L_star"] = 999
    p = tmp_path / "d.csv"; df.to_csv(p, index=False)
    with pytest.raises(ValueError, match="out of spec range"):
        t.load_csv(p, spec)

def test_csv_blank_optional_gets_fallback_but_blank_required_fails(spec, tmp_path):
    df = t.make_synthetic(spec, 2, 5); df.loc[0, "ndvi_satellite"] = None
    p = tmp_path / "d.csv"; df.to_csv(p, index=False)
    assert t.load_csv(p, spec).loc[0, "ndvi_satellite"] == 0.35
    df.loc[1, "L_star"] = None; df.to_csv(p, index=False)
    with pytest.raises(ValueError):
        t.load_csv(p, spec)

def test_fallback_augmentation_applies_default(spec):
    df = t.make_synthetic(spec, 2, 50)
    aug = t.augment_fallback(df, spec, 0.5)
    assert 0.2 < (aug.ndvi_satellite == 0.35).mean() < 0.8
