"""Read-only model-readiness audit of Mobile Phone Use CSVs.

Run: python -m src.models.dataset_audit
Outputs stay in ignored data/processed/. This does not reconstruct sessions,
create labels, repair source files or replace the team's preprocessing pipeline.
Only the listed model-relevant columns are value-profiled; all headers are read.
"""

import argparse
from collections import Counter
import csv
import json
from pathlib import Path

import numpy as np
import pandas as pd


AUDIT_COLUMNS = (
    "uuid", "version", "ts", "ts_raw", "sensor_id", "Screen_Value",
    "App_Value", "Notif_Action",
)


def audit_file(path: Path, chunksize: int = 50_000) -> dict:
    """Profile a complete participant file using bounded-size pandas chunks."""
    path = Path(path)
    if chunksize < 1:
        raise ValueError("chunksize must be positive")
    with path.open(encoding="utf-8-sig", newline="") as stream:
        columns = next(csv.reader(stream))
    missing = sorted(set(AUDIT_COLUMNS) - set(columns))
    if missing:
        raise ValueError(f"{path.name}: missing columns {missing}")
    if len(columns) != len(set(columns)):
        raise ValueError(f"{path.name}: duplicate columns")
    result = {
        "participant_id": path.stem, "bytes": path.stat().st_size,
        "columns": columns, "rows": 0, "uuid_mismatches": 0,
        "invalid_raw_timestamps": 0, "invalid_local_timestamps": 0,
        "raw_order_descents": 0, "screen_raw_order_descents": 0,
        "app_raw_order_descents": 0,
    }
    counters = {name: Counter() for name in (
        "sensors", "screen_values", "notification_actions", "versions",
        "app_prefixes", "non_esm_near_integer_offset_hours", "raw_year_counts",
        "screen_year_counts", "app_year_counts",
    )}
    nulls = Counter()
    previous = {"all": None, "Screen": None, "App": None}
    days = set()
    minimum = maximum = None
    non_hour_offsets = 0
    for frame in pd.read_csv(path, usecols=list(AUDIT_COLUMNS), dtype=str,
                             keep_default_na=False, chunksize=chunksize):
        result["rows"] += len(frame)
        result["uuid_mismatches"] += int(frame["uuid"].ne(path.stem).sum())
        nulls.update({name: int(frame[name].eq("").sum()) for name in AUDIT_COLUMNS})
        counters["sensors"].update(frame["sensor_id"].value_counts().to_dict())
        counters["versions"].update(frame["version"].value_counts().to_dict())
        for sensor, field, output in [
            ("Screen", "Screen_Value", "screen_values"),
            ("Notif", "Notif_Action", "notification_actions"),
        ]:
            values = frame.loc[frame["sensor_id"].eq(sensor), field]
            counters[output].update(values.replace("", "<missing>").value_counts().to_dict())
        apps = frame.loc[frame["sensor_id"].eq("App"), "App_Value"]
        for prefix in ("+", "=", "-"):
            counters["app_prefixes"][prefix] += int(apps.str.startswith(prefix).sum())
        counters["app_prefixes"]["unprefixed"] += int(
            (~apps.str.startswith(("+", "=", "-")) & apps.ne("")).sum())
        counters["app_prefixes"]["missing"] += int(apps.eq("").sum())

        numeric = pd.to_numeric(frame["ts_raw"], errors="coerce")
        valid = numeric.notna() & np.isfinite(numeric) & numeric.mod(1).eq(0)
        raw_dates = pd.to_datetime(numeric.where(valid), unit="ms", utc=True,
                                   errors="coerce")
        valid &= raw_dates.notna()
        result["invalid_raw_timestamps"] += int((~valid).sum())
        for sensor, output in [(None, "raw_year_counts"),
                               ("Screen", "screen_year_counts"), ("App", "app_year_counts")]:
            mask = valid if sensor is None else valid & frame["sensor_id"].eq(sensor)
            counters[output].update(raw_dates[mask].dt.year.astype(str).value_counts().to_dict())
        values = numeric[valid].astype("int64")
        if not values.empty:
            lo, hi = int(values.min()), int(values.max())
            minimum = lo if minimum is None else min(minimum, lo)
            maximum = hi if maximum is None else max(maximum, hi)
            days.update((values // 86_400_000).unique().tolist())
        for sensor, key, output in [
            (None, "all", "raw_order_descents"),
            ("Screen", "Screen", "screen_raw_order_descents"),
            ("App", "App", "app_raw_order_descents"),
        ]:
            selected = values if sensor is None else values[frame.loc[values.index, "sensor_id"].eq(sensor)]
            arr = selected.to_numpy()
            if arr.size:
                result[output] += int(np.count_nonzero(np.diff(arr) < 0))
                if previous[key] is not None and arr[0] < previous[key]:
                    result[output] += 1
                previous[key] = int(arr[-1])
        # This compares a naive wall-clock reading against epoch time. It is a
        # diagnostic offset, NOT a verified IANA time zone or DST conversion.
        local = pd.to_datetime(frame["ts"], format="%Y-%m-%d %H:%M:%S",
                               utc=True, errors="coerce")
        result["invalid_local_timestamps"] += int(local.isna().sum())
        mask = valid & local.notna() & frame["sensor_id"].ne("Esm")
        offsets = (local[mask] - raw_dates[mask]).dt.total_seconds()
        nearest = (offsets / 3600).round()
        near = (offsets - nearest * 3600).abs().le(2)
        counters["non_esm_near_integer_offset_hours"].update(
            nearest[near].astype(int).astype(str).value_counts().to_dict())
        non_hour_offsets += int((~near).sum())
    result.update({name: dict(sorted(count.items())) for name, count in counters.items()})
    result["missing_selected_values"] = dict(nulls)
    result["non_esm_non_hour_offset_rows"] = non_hour_offsets
    result["observed_utc_dates"] = len(days)
    for name, value in [("first_event_utc", minimum), ("last_event_utc", maximum)]:
        result[name] = None if value is None else pd.to_datetime(value, unit="ms", utc=True).isoformat()
    return result


def audit_dataset(root: Path, *, chunksize: int = 50_000) -> dict:
    root = Path(root)
    metadata = pd.read_csv(root / "pinfo.csv", dtype={"uuid": str})
    if "uuid" not in metadata:
        raise ValueError("pinfo.csv must contain uuid")
    if metadata["uuid"].isna().any() or metadata["uuid"].duplicated().any():
        raise ValueError("pinfo.csv must have unique, non-missing participant IDs")
    files = sorted((root / "data").glob("*.csv"))
    if not files:
        raise ValueError("No participant CSV files found")
    ids = set(metadata["uuid"])
    file_ids = {file.stem for file in files}
    per_file = []
    errors = []
    for i, file in enumerate(files, 1):
        try:
            per_file.append(audit_file(file, chunksize))
        except (ValueError, OSError, pd.errors.ParserError) as exc:
            errors.append({"file": file.name, "error": str(exc)})
        if i % 25 == 0 or i == len(files):
            print(f"Inspected {i}/{len(files)} participant CSVs", flush=True)
    totals = {name: sum(item[name] for item in per_file) for name in (
        "rows", "uuid_mismatches", "invalid_raw_timestamps", "invalid_local_timestamps",
        "raw_order_descents", "screen_raw_order_descents", "app_raw_order_descents",
    )}
    for name in ("sensors", "screen_values", "notification_actions", "app_prefixes",
                 "non_esm_near_integer_offset_hours", "versions", "raw_year_counts",
                 "screen_year_counts", "app_year_counts"):
        count = Counter()
        for item in per_file:
            count.update(item[name])
        totals[name] = dict(sorted(count.items()))
    totals.update({
        "participant_files": len(files), "successfully_read_files": len(per_file),
        "csv_bytes": sum(file.stat().st_size for file in files),
        "metadata_rows": len(metadata), "metadata_columns": list(metadata.columns),
        "missing_csv_ids": sorted(ids - file_ids), "unknown_csv_ids": sorted(file_ids - ids),
        "files_with_raw_descents": sum(item["raw_order_descents"] > 0 for item in per_file),
        "files_with_screen_descents": sum(item["screen_raw_order_descents"] > 0 for item in per_file),
        "files_with_app_descents": sum(item["app_raw_order_descents"] > 0 for item in per_file),
        "files_with_screen": sum(item["sensors"].get("Screen", 0) > 0 for item in per_file),
        "files_with_app": sum(item["sensors"].get("App", 0) > 0 for item in per_file),
        "screen_value_profiles": dict(Counter(
            ",".join(sorted(item["screen_values"])) for item in per_file)),
        "metadata_non_null_counts": {name: int(metadata[name].notna().sum()) for name in metadata},
    })
    totals["header_variants"] = [
        {"columns": list(columns), "file_count": count}
        for columns, count in Counter(tuple(item["columns"]) for item in per_file).items()
    ]
    for field in ("age", "active_days"):
        if field in metadata:
            values = pd.to_numeric(metadata[field], errors="coerce")
            totals[field] = {str(k): float(v) for k, v in values.describe().items()}
    if "active_days" in metadata:
        days = pd.to_numeric(metadata["active_days"], errors="coerce")
        totals["metadata_personalization_eligibility"] = {
            str(n): int(days.ge(n + 7).sum()) for n in (1, 3, 7, 14)
        }  # Metadata only, not verified valid-window availability.
    for field in ("locale", "timezone"):
        if field in metadata:
            totals[field] = {str(k): int(v) for k, v in metadata[field].value_counts().items()}
    starts = [item["first_event_utc"] for item in per_file if item["first_event_utc"]]
    ends = [item["last_event_utc"] for item in per_file if item["last_event_utc"]]
    totals["first_event_utc"] = min(starts) if starts else None
    totals["last_event_utc"] = max(ends) if ends else None
    return {
        "dataset_root": str(root), "summary": totals, "file_errors": errors,
        "per_file": per_file,
        "scope": "All CSV headers and all rows of selected model-relevant columns; sources unchanged.",
        "limitations": [
            "No sessions, proxy labels, feature table or model performance produced by this audit.",
            "UTC dates and pinfo active_days do not establish valid 30-minute window coverage.",
            "Offset diagnostics are not verified time-zone/DST assignments.",
            "Questionnaire and ESM summaries are not unwanted-use labels or prospective features.",
            "This audit does not establish dataset redistribution rights.",
        ],
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--dataset", type=Path, default=Path("src/data/mobile_phone_use"))
    parser.add_argument("--output", type=Path, default=Path("data/processed/dataset_audit.json"))
    parser.add_argument("--chunksize", type=int, default=50_000)
    args = parser.parse_args()
    report = audit_dataset(args.dataset, chunksize=args.chunksize)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(report, indent=2, ensure_ascii=False, allow_nan=False) + "\n")
    summary = report["summary"]
    print(json.dumps({name: summary[name] for name in (
        "participant_files", "successfully_read_files", "rows", "csv_bytes",
        "files_with_raw_descents", "files_with_screen", "files_with_app", "screen_value_profiles",
    )}, indent=2))
    print(f"Local audit saved to {args.output}")
    if report["file_errors"] or summary["missing_csv_ids"] or summary["unknown_csv_ids"]:
        raise SystemExit("Dataset audit found file errors or participant ID mismatches")


if __name__ == "__main__":
    main()
