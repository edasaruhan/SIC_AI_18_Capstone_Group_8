"""Decision selection tests: no real dataset needed."""

import hashlib

import numpy as np
import pandas as pd
import pytest

from src.models.threshold_exploration import (
    compare_thresholds, participant_splits, select_decision_threshold, verify_sources,
)


def toy_table():
    parts = []
    for user in range(6):
        active = np.array([0, 1000, 0, 1200, 0, 1500, 0, 1600], dtype=float)
        parts.append(pd.DataFrame({
            "participant_id": f"u{user}", "window_start_ms": np.arange(8) * 1_800_000,
            "window_end_ms": (np.arange(8) + 1) * 1_800_000,
            "screen_active_seconds": active, "unlock_count": (active > 0).astype(int) * 5,
            "app_switch_count": (active > 0).astype(int),
            "previous_session_duration_seconds": np.nan,
            "next_longest_session_seconds": active, "next_unlock_count": (active > 0).astype(int) * 5,
        }))
    return pd.concat(parts, ignore_index=True)


def test_threshold_ties_are_deterministic_and_no_test_arguments_exist():
    threshold, rows = select_decision_threshold([0, 0, 1, 1], [0.1, 0.2, 0.8, 0.9], [0.3, 0.5, 0.7])
    assert threshold == 0.7
    assert all(row["validation_f1"] == 1 for row in rows)


@pytest.mark.parametrize("labels,scores,candidates", [
    ([0, 0], [0.1, 0.2], [0.5]),
    ([0, 1], [0.1], [0.5]),
    ([0, 1], [0.1, np.nan], [0.5]),
    ([0, 1], [0.1, 1.1], [0.5]),
    ([0, 1], [0.1, 0.9], [0]),
    ([0, 1], [0.1, 0.9], []),
])
def test_invalid_validation_inputs_are_rejected(labels, scores, candidates):
    with pytest.raises(ValueError):
        select_decision_threshold(labels, scores, candidates)


def test_three_way_splits_are_group_disjoint_and_label_independent():
    table = toy_table()
    before = list(participant_splits(table))
    table["next_longest_session_seconds"] = 999
    after = list(participant_splits(table))
    seen_test = set()
    for left, right in zip(before, after):
        for idx, other in zip(left[1:], right[1:]):
            np.testing.assert_array_equal(idx, other)
        _, train_idx, val_idx, test_idx = left
        groups = [set(table.iloc[idx].participant_id) for idx in (train_idx, val_idx, test_idx)]
        assert not (groups[0] & groups[1] or groups[0] & groups[2] or groups[1] & groups[2])
        assert set.union(*groups) == set(table.participant_id)
        assert not seen_test & groups[2]
        seen_test |= groups[2]
    assert seen_test == set(table.participant_id)


def test_actual_models_use_same_test_scores_for_both_decision_policies():
    table = toy_table()
    report, predictions, local = compare_thresholds(table)
    assert len(report["fold_results"]) == 15
    assert len(predictions) == len(table) * 5
    assert len(local["validation_curves"]) == 6
    assert len(local["per_user_metrics"]) == 30
    assert predictions.groupby(["model", "policy", "participant_id"]).fold.nunique().eq(1).all()
    for name in ["Logistic Regression", "Random Forest"]:
        group = predictions.loc[predictions.model.eq(name)]
        default = group.loc[group.policy.eq("default_0.50")].sort_values(["participant_id", "window_start_ms"])
        selected = group.loc[group.policy.eq("validation_f1")].sort_values(["participant_id", "window_start_ms"])
        np.testing.assert_array_equal(default.score.to_numpy(), selected.score.to_numpy())
        np.testing.assert_array_equal(default.y.to_numpy(), selected.y.to_numpy())
        for fold in range(1, 4):
            row = next(r for r in report["fold_results"] if r["model"] == name and r["fold"] == fold and r["policy"] == "validation_f1")
            curve = next(r for r in local["validation_curves"] if r["model"] == name and r["fold"] == fold)
            best = max(curve["candidates"], key=lambda x: (x["validation_f1"], x["threshold"]))
            assert row["decision_threshold"] == best["threshold"]
            scored = selected.loc[selected.fold.eq(fold)]
            np.testing.assert_array_equal(scored.prediction, scored.score.ge(best["threshold"]).astype(int))


def test_source_manifest_detects_same_size_content_change(tmp_path):
    folder = tmp_path / "data"
    folder.mkdir()
    file = folder / "u1.csv"
    file.write_bytes(b"same")
    manifest = [{"file": "u1.csv", "bytes": 4, "sha256": hashlib.sha256(b"same").hexdigest()}]
    verify_sources(tmp_path, manifest)
    file.write_bytes(b"diff")
    with pytest.raises(ValueError, match="content changed"):
        verify_sources(tmp_path, manifest)
