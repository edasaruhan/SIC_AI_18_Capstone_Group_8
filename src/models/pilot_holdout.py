"""Compare frozen thresholds on a subsequent Android pilot export."""

import argparse
from datetime import datetime
from hashlib import sha256
from io import BytesIO
import json
from pathlib import Path
from zipfile import ZipFile

import numpy as np
import pandas as pd

from .dataset_v1 import V1_FEATURES
from .features import prepare_features
from .mobile_export import INPUT, OUTPUT, runtime_session


WINDOW_MS = 1_800_000
KEY = ["participant_id", "window_start_ms", "window_end_ms"]
FILES = ("manifest.json", "feature_windows.csv", "feedbacks.csv")


def read_export(path):
    if path.is_dir():
        return {name: (path / name).read_bytes() for name in FILES}
    with ZipFile(path) as bundle:
        return {name: bundle.read(name) for name in FILES}


def read_table(blob, manifest, count_key):
    table = pd.read_csv(BytesIO(blob), dtype={"participant_id": str})
    if (len(table) != manifest[count_key] or table[KEY].isna().any().any()
            or table.duplicated(KEY).any() or not table.participant_id.eq(manifest["participant_id"]).all()
            or not table.window_end_ms.sub(table.window_start_ms).eq(WINDOW_MS).all()
            or not table.window_start_ms.mod(WINDOW_MS).eq(0).all()
            or not table.window_end_ms.le(manifest["created_at_ms"]).all()):
        raise ValueError(f"Invalid or inconsistent pilot table: {count_key}")
    return table


def holdout_pairs(windows, feedback, cutoff, exported_at):
    valid = feedback[
        feedback.response_status.eq("ANSWERED") & feedback.answer.isin(["YES", "NO"])
        & feedback.question_version.eq("q-v1")
        & feedback.answered_at_ms.ge(feedback.window_end_ms)
        & feedback.answered_at_ms.ge(feedback.asked_at_ms)
        & feedback.answered_at_ms.le(exported_at)
    ]
    valid = valid.merge(windows[KEY], on=KEY, validate="one_to_one")
    inputs = windows[windows.window_start_ms.ge(cutoff)
                     & windows.quality_status.eq("OBSERVATION_SUFFICIENT")].copy()
    inputs["input_start_ms"] = inputs.window_start_ms
    inputs["window_start_ms"] = inputs.window_end_ms
    inputs["window_end_ms"] = inputs.window_start_ms + WINDOW_MS
    return inputs.merge(valid[KEY + ["answer"]], on=KEY, validate="one_to_one")


def summarize(scores, labels, threshold):
    high = scores >= threshold
    tp, fn = int((labels & high).sum()), int((labels & ~high).sum())
    fp, tn = int((~labels & high).sum()), int((~labels & ~high).sum())
    return {"threshold": threshold, "yes_high": tp, "yes_low": fn, "no_high": fp, "no_low": tn,
            "f1_yes": 2 * tp / (2 * tp + fp + fn) if labels.any() else None}


def evaluate(frozen, original, new, model_dir, consent_start_ms=None):
    if {name: sha256(blob).hexdigest() for name, blob in original.items()} != frozen["calibration_source_sha256"]:
        raise ValueError("Calibration ZIP does not match the frozen record")
    baseline, manifest = json.loads(original["manifest.json"]), json.loads(new["manifest.json"])
    if baseline["participant_id"] != manifest["participant_id"]:
        raise ValueError("Participant IDs differ. Verify continuity before combining exports.")
    if (manifest["schema_version"], manifest["feature_version"], manifest["time_unit"]) != (1, "v1", "utc_epoch_ms"):
        raise ValueError("Unsupported pilot export schema")
    cutoff = frozen["calibration_export_cutoff_ms"]
    if cutoff != baseline["created_at_ms"] or manifest["created_at_ms"] <= cutoff:
        raise ValueError("A subsequent export is required")
    if consent_start_ms is not None:
        if consent_start_ms > baseline["included_start_ms"]:
            raise ValueError("Confirmed consent begins after the calibration data starts. Review calibration eligibility.")
        cutoff = max(cutoff, consent_start_ms)
    windows = read_table(new["feature_windows.csv"], manifest, "window_count")
    feedback = read_table(new["feedbacks.csv"], manifest, "feedback_count")
    if not windows.feature_version.eq("v1").all():
        raise ValueError("Unexpected feature version")
    pairs = holdout_pairs(windows, feedback, cutoff, manifest["created_at_ms"])
    frozen_at_ms = datetime.fromisoformat(frozen["frozen_at"]).timestamp() * 1000
    retrospective_pairs = int(pairs.input_start_ms.lt(frozen_at_ms).sum())
    result = {
        "app_version": manifest["app_version"],
        "matched": len(pairs),
        "yes": int(pairs.answer.eq("YES").sum()),
        "no": int(pairs.answer.eq("NO").sum()),
        "inputs_before_threshold_freeze": retrospective_pairs,
        "consent_start_confirmed": consent_start_ms is not None,
    }
    if pairs.empty:
        return {**result, "status": "no_eligible_matches"}
    frame = prepare_features(pairs, V1_FEATURES)
    values = frame.to_numpy(dtype=np.float32)
    if not np.isfinite(values).all():
        raise ValueError("Live-compatible pilot features must be finite")
    model = (model_dir / "model.onnx").read_bytes()
    if sha256(model).hexdigest() != frozen["model_sha256"]:
        raise ValueError("ONNX model differs from the calibration model")
    scores, = runtime_session(model).run([OUTPUT], {INPUT: values})
    labels = pairs.answer.eq("YES").to_numpy()
    return {
        **result,
        "status": "both_classes" if labels.any() and not labels.all() else "single_class",
        "baseline": summarize(scores.astype(float), labels, frozen["baseline"]["threshold"]),
        "candidate": summarize(scores.astype(float), labels, frozen["candidate"]["threshold"]),
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("new_export", type=Path)
    parser.add_argument("--calibration-export", type=Path, required=True)
    parser.add_argument("--frozen", type=Path, default=Path("data/processed/pilot_threshold/calibration.json"))
    parser.add_argument("--model-dir", type=Path, default=Path("models/mobile/dataset_v1-xgboost-fold-1-v1"))
    parser.add_argument("--consent-start-ms", type=int)
    args = parser.parse_args()
    result = evaluate(json.loads(args.frozen.read_text()), read_export(args.calibration_export),
                      read_export(args.new_export), args.model_dir, args.consent_start_ms)
    print(json.dumps(result, indent=2, allow_nan=False))


if __name__ == "__main__":
    main()
