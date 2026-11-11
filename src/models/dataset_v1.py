"""Validate and consume Ismail's prepared table without changing his pipeline."""

import hashlib
import json
from pathlib import Path

import numpy as np
import pandas as pd

from .features import FeatureSpec, prepare_features


# Source names/semantics stay intact: prev_session_s is measured at WINDOW START,
# hour/dow use the pipeline's shifted local clock, not the old draft's end time.
V1_FEATURES = (
    FeatureSpec("screen_s", 0, 1800),
    FeatureSpec("sessions_started", 0, integer=True),
    FeatureSpec("longest_run_s", 0, 1800, upper_bound_feature="screen_s"),
    FeatureSpec("app_switches", 0, integer=True),
    FeatureSpec("prev_session_s", 0, 10800),
    FeatureSpec("hour", 0, 23, integer=True),
    FeatureSpec("dow", 0, 6, integer=True),
)
LABEL_KINDS = ("long", "night", "rapid", "any")


def sha256_file(path):
    digest = hashlib.sha256()
    with Path(path).open("rb") as stream:
        for block in iter(lambda: stream.read(1 << 20), b""):
            digest.update(block)
    return digest.hexdigest()


def validate_v1(windows, inventory, summary):
    """Check EVERY row's target alignment before filtering out invalid windows."""
    required = {s.name for s in V1_FEATURES} | {
        "uuid", "w_start_s", "valid", "target_ok", "is_night", "clean_screen_s",
        *[prefix + k for k in LABEL_KINDS for prefix in ("lab_", "y_next_")],
    }
    if not windows.columns.is_unique or required - set(windows):
        raise ValueError("Missing or duplicate dataset_v1 columns")
    inv_cols = {"uuid", "usable_for_modeling", "has_screen_events", "valid_days",
                "active_days_screen", "mean_daily_unlocks", "windows_total",
                "windows_valid", "targets_ok"}
    if inv_cols - set(inventory) or inventory.uuid.isna().any() or inventory.uuid.duplicated().any():
        raise ValueError("Invalid participant inventory")
    if windows.empty or windows.uuid.isna().any() or set(windows.uuid) != set(inventory.uuid):
        raise ValueError("Window/inventory participant coverage mismatch")
    for frame, cols in [(windows, ("valid", "target_ok")),
                        (inventory, ("usable_for_modeling", "has_screen_events"))]:
        for col in cols:
            if frame[col].dtype != bool or frame[col].isna().any():
                raise ValueError(f"{col} must contain observed boolean flags")
    expected_usable = (inventory.has_screen_events & inventory.valid_days.ge(7)
                       & inventory.active_days_screen.ge(7) & inventory.mean_daily_unlocks.ge(1))
    if not expected_usable.equals(inventory.usable_for_modeling):
        raise ValueError("Eligibility differs from the delivered pipeline rule")
    stamps = windows.w_start_s.to_numpy()
    if (not np.issubdtype(stamps.dtype, np.number) or not np.isfinite(stamps).all()
            or (stamps % 1800 != 0).any() or windows.duplicated(["uuid", "w_start_s"]).any()):
        raise ValueError("Invalid or duplicate window timestamps")
    grouped = windows.groupby("uuid", sort=False)
    # Do not silently sort a corrupted table, skip gaps or shift after filtering.
    if not grouped.w_start_s.diff().dropna().eq(1800).all():
        raise ValueError("Each participant must have an ordered continuous 30-minute grid")
    expected_ok = windows.valid & grouped.valid.shift(-1).fillna(False).astype(bool)
    if not windows.target_ok.equals(expected_ok):
        raise ValueError("target_ok does not describe adjacent valid windows")
    for kind in LABEL_KINDS:
        lab, target = "lab_" + kind, "y_next_" + kind
        if not windows[lab].isin([0, 1]).all():
            raise ValueError(f"Nonbinary {lab}")
        expected = grouped[lab].shift(-1).where(expected_ok)
        if not np.allclose(windows[target], expected, rtol=0, atol=0, equal_nan=True):
            raise ValueError(f"Misaligned or missing {target}")
    if not windows.lab_any.eq(windows[["lab_long", "lab_night", "lab_rapid"]].max(axis=1)).all():
        raise ValueError("lab_any is not the union of its components")
    parameters = summary["parameters"]
    if parameters != {"max_session_h": 3.0, "long_min": 20, "night_start": 0,
                      "night_end": 5, "night_min": 5, "rapid_n": 6,
                      "rapid_source": "sessions", "split_on": 0}:
        raise ValueError("This research adapter supports the delivered v1 label version only")
    if not windows.lab_rapid.eq(windows.sessions_started.ge(parameters["rapid_n"])).all():
        raise ValueError("Rapid label disagrees with supplied session counts")
    night = windows.hour.ge(0) & windows.hour.lt(5)
    if (not windows.is_night.eq(night.astype(int)).all()
            or not windows.lab_night.eq(night & windows.clean_screen_s.ge(300)).all()):
        raise ValueError("Night label disagrees with supplied local time/clean duration")
    if (not windows.hour.eq((windows.w_start_s // 3600) % 24).all()
            or not windows.dow.eq((windows.w_start_s // 86400 + 3) % 7).all()):
        raise ValueError("Local-clock fields disagree with window starts")
    prepare_features(windows, V1_FEATURES)
    counts = grouped.agg(windows_total=("valid", "size"), windows_valid=("valid", "sum"),
                         targets_ok=("target_ok", "sum")).sort_index()
    if not counts.eq(inventory.set_index("uuid")[counts.columns].sort_index()).all().all():
        raise ValueError("Inventory window counts disagree with the table")
    checks = {"participants_processed": len(inventory),
              "participants_usable_for_modeling": int(inventory.usable_for_modeling.sum()),
              "windows_total": len(windows), "windows_valid": int(windows.valid.sum()),
              "windows_with_target": int(windows.target_ok.sum())}
    if any(summary.get(key) != val for key, val in checks.items()):
        raise ValueError("Summary coverage disagrees with delivered tables")
    eligible_ids = inventory.loc[inventory.usable_for_modeling, "uuid"]
    selected = windows.loc[windows.target_ok & windows.uuid.isin(eligible_ids)].copy()
    if selected.empty or selected.y_next_any.nunique() != 2:
        raise ValueError("Selected research data must contain both target classes")
    checks.update(selected_users=selected.uuid.nunique(), selected_rows=len(selected),
                  selected_positive_rows=int(selected.y_next_any.sum()),
                  excluded_users=len(inventory) - len(eligible_ids),
                  excluded_target_rows=int(windows.target_ok.sum()) - len(selected),
                  target_alignment_mismatches=0)
    # Generic split helper needs only participant_id; shifted local seconds remain
    # explicitly named as local seconds, never mislabeled as UTC milliseconds.
    selected["participant_id"] = selected.uuid
    return selected.reset_index(drop=True), checks


def load_v1(dataset, pipeline=None):
    dataset = Path(dataset)
    summary = json.loads((dataset / "summary.json").read_text())
    inventory = pd.read_csv(dataset / "participant_inventory.csv")
    table, coverage = validate_v1(pd.read_csv(dataset / "windows.csv.gz"), inventory, summary)
    manifest = {name: {"sha256": sha256_file(dataset / name),
                       "bytes": (dataset / name).stat().st_size}
                for name in ("windows.csv.gz", "participant_inventory.csv", "summary.json",
                             "windows_sutunlari.md", "sizinti_testi.txt", "label_sensitivity.csv",
                             "daily.csv.gz")}
    if pipeline is not None:
        digest = sha256_file(pipeline)
        if not digest.startswith(summary["script_sha256_12"]):
            raise ValueError("Pipeline hash does not match dataset_v1 provenance")
        manifest["mpu_pipeline.py"] = {"sha256": digest, "bytes": Path(pipeline).stat().st_size}
    return table, coverage, manifest
