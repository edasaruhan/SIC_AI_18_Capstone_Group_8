"""Small fabricated logs test full-file audit and cross-chunk bookkeeping."""

import csv
from pathlib import Path

import pandas as pd
import pytest

from src.models.dataset_audit import AUDIT_COLUMNS, audit_dataset, audit_file


def write_log(path: Path):
    rows = [
        ("2000", "Screen", "Unlocked", ""),
        ("1000", "Screen", "Off", ""),
        ("3000", "App", "", "+launcher"),
        ("2500", "App", "", "=keyboard"),
    ]
    with path.open("w", newline="") as stream:
        writer = csv.DictWriter(stream, fieldnames=AUDIT_COLUMNS)
        writer.writeheader()
        for raw, sensor, screen, app in rows:
            writer.writerow({"uuid": path.stem, "version": "302", "ts_raw": raw,
                             "ts": "1970-01-01 02:00:02", "sensor_id": sensor,
                             "Screen_Value": screen, "App_Value": app})


def test_audit_is_chunk_invariant_and_read_only(tmp_path):
    file = tmp_path / "u_test.csv"
    write_log(file)
    before = file.read_bytes()
    small = audit_file(file, chunksize=1)
    large = audit_file(file, chunksize=100)
    assert small == large
    assert file.read_bytes() == before
    assert small["rows"] == 4
    assert small["raw_order_descents"] == 2
    assert small["screen_raw_order_descents"] == 1
    assert small["app_raw_order_descents"] == 1
    assert small["screen_values"] == {"Off": 1, "Unlocked": 1}
    assert small["app_prefixes"]["+"] == small["app_prefixes"]["="] == 1
    assert small["non_esm_near_integer_offset_hours"] == {"2": 4}


def test_metadata_inventory_and_sparse_traits(tmp_path):
    folder = tmp_path / "data"
    folder.mkdir()
    write_log(folder / "u_test.csv")
    pd.DataFrame({"uuid": ["u_test"], "age": [30], "active_days": [21],
                  "PHQ8": [None]}).to_csv(tmp_path / "pinfo.csv", index=False)
    report = audit_dataset(tmp_path, chunksize=2)
    assert not report["file_errors"]
    summary = report["summary"]
    assert summary["rows"] == 4
    assert summary["files_with_raw_descents"] == 1
    assert summary["missing_csv_ids"] == summary["unknown_csv_ids"] == []
    assert summary["metadata_non_null_counts"]["PHQ8"] == 0
    assert summary["metadata_personalization_eligibility"] == {"1": 1, "3": 1, "7": 1, "14": 1}


def test_invalid_raw_time_and_uuid_mismatch_are_reported(tmp_path):
    file = tmp_path / "u_test.csv"
    write_log(file)
    frame = pd.read_csv(file, dtype=str, keep_default_na=False)
    frame.loc[0, "ts_raw"] = "not-a-time"
    frame.loc[1, "uuid"] = "wrong_id"
    frame.to_csv(file, index=False)
    result = audit_file(file, chunksize=1)
    assert result["invalid_raw_timestamps"] == 1
    assert result["uuid_mismatches"] == 1


def test_bad_header_is_not_silently_skipped(tmp_path):
    file = tmp_path / "u_test.csv"
    file.write_text("uuid,ts_raw\nu_test,1000\n")
    with pytest.raises(ValueError, match="missing columns"):
        audit_file(file)


def test_duplicate_metadata_ids_rejected(tmp_path):
    pd.DataFrame({"uuid": ["same", "same"]}).to_csv(tmp_path / "pinfo.csv", index=False)
    with pytest.raises(ValueError, match="unique"):
        audit_dataset(tmp_path)
