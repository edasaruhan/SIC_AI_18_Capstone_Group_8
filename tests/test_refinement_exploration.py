"""Prepared-data contract, causal baseline and bounded refinement checks."""

import numpy as np
import pandas as pd
import pytest

from src.models.dataset_v1 import V1_FEATURES, validate_v1
from src.models.refinement_exploration import (
    causal_rule_scores, compare_refinement, select_candidate, v1_metrics,
)


def delivered_table(users=6):
    parts = []
    for user in range(users):
        start = np.arange(384) * 1800 + 1464739200
        usage = np.where(np.arange(384) % 8 < 3, 1300., 0.)
        f = pd.DataFrame({"uuid": f"u{user}", "w_start_s": start,
                          "valid": True, "screen_s": usage,
                          "sessions_started": (usage > 0).astype(float) * 7,
                          "longest_run_s": usage, "app_switches": 1.,
                          "prev_session_s": 0., "hour": start // 3600 % 24,
                          "dow": (start // 86400 + 3) % 7,
                          "clean_screen_s": usage})
        f["is_night"] = f.hour.lt(5).astype(int)
        f["lab_long"] = f.screen_s.ge(1200).astype(int)
        f["lab_rapid"] = f.sessions_started.ge(6).astype(int)
        f["lab_night"] = (f.is_night.eq(1) & f.clean_screen_s.ge(300)).astype(int)
        f["lab_any"] = f[["lab_long", "lab_rapid", "lab_night"]].max(axis=1)
        f["target_ok"] = True
        f.loc[len(f)-1, "target_ok"] = False
        for k in ("long", "night", "rapid", "any"):
            f["y_next_"+k] = f["lab_"+k].shift(-1)
        parts.append(f)
    W = pd.concat(parts, ignore_index=True)
    inv = W.groupby("uuid").agg(windows_total=("valid", "size"),
        windows_valid=("valid", "sum"), targets_ok=("target_ok", "sum")).reset_index()
    inv = inv.assign(usable_for_modeling=True, has_screen_events=True,
                     valid_days=8., active_days_screen=8, mean_daily_unlocks=100.)
    summary = {"participants_processed": users, "participants_usable_for_modeling": users,
               "windows_total": len(W), "windows_valid": len(W), "windows_with_target": users*383,
               "parameters": {"max_session_h": 3., "long_min": 20, "night_start": 0,
                              "night_end": 5, "night_min": 5, "rapid_n": 6,
                              "rapid_source": "sessions", "split_on": 0}}
    return W, inv, summary


def test_adapter_filters_source_eligibility_and_never_adds_labels_as_features():
    W, inv, summary = delivered_table()
    inv.loc[0, ["usable_for_modeling", "active_days_screen"]] = [False, 1]
    summary["participants_usable_for_modeling"] -= 1
    table, coverage = validate_v1(W, inv, summary)
    assert table.uuid.nunique() == 5 and len(table) == 5*383
    assert coverage["excluded_target_rows"] == 383
    assert coverage["target_alignment_mismatches"] == 0
    assert not set(s.name for s in V1_FEATURES) & {"lab_any", "y_next_any", "clean_screen_s", "uuid"}


@pytest.mark.parametrize("mutation", ["target", "target_null", "duplicate", "grid",
                                      "eligibility", "flags", "summary"])
def test_corrupt_prepared_data_is_rejected(mutation):
    W, inv, summary = delivered_table()
    if mutation == "target": W.loc[0, "y_next_any"] = 1 - W.loc[0, "y_next_any"]
    if mutation == "target_null": W.loc[0, "y_next_any"] = np.nan
    if mutation == "duplicate": W.loc[1, "w_start_s"] = W.loc[0, "w_start_s"]
    if mutation == "grid": W.loc[1, "w_start_s"] += 1800
    if mutation == "eligibility": inv.loc[0, "usable_for_modeling"] = False
    if mutation == "flags": W["valid"] = W.valid.astype(int)
    if mutation == "summary": summary["windows_total"] += 1
    with pytest.raises(ValueError): validate_v1(W, inv, summary)


def test_rule_ignores_retrospective_labels_and_target_columns():
    W, _, _ = delivered_table()
    score = causal_rule_scores(W)
    W[["lab_any", "y_next_any", "clean_screen_s"]] = 999
    np.testing.assert_array_equal(causal_rule_scores(W), score)
    W.loc[0, ["longest_run_s", "screen_s", "sessions_started", "hour"]] = [0., 300., 0., 2]
    assert causal_rule_scores(W.iloc[:1])[0] == 1/3


def test_metric_day_denominator_uses_local_prediction_date_and_partial_days():
    table = pd.DataFrame({"participant_id": ["a", "a"], "w_start_s": [84600, 86400]})
    result = v1_metrics([0, 0], [1, 1], [0.8, 0.9], table)
    assert result["evaluated_participant_local_days"] == 1
    assert result["false_alerts_per_evaluated_participant_local_day"] == 2
    assert not any("utc" in k for k in result)


def test_refinement_selection_splits_artifacts_and_metrics(tmp_path, monkeypatch):
    from src.models import refinement_exploration as module
    W, inv, summary = delivered_table()
    table, _ = validate_v1(W, inv, summary)
    monkeypatch.setattr(module, "CANDIDATES", {
        "Logistic Regression": [{"C": 1.0}, {"C": .1}],
        "Random Forest": [{"n_estimators": 3}],
        "XGBoost": [{"n_estimators": 3, "min_child_weight": 1}],
    })
    report, predictions, local = compare_refinement(table, model_dir=tmp_path)
    assert len(report["fold_results"]) == 21
    assert len(report["saved_model_checks"]) == 9
    assert len(predictions) == 4*len(table)
    for split in local["splits"]:
        a, b, c = (set(split[k]) for k in ("training", "validation", "test"))
        assert not (a&b or a&c or b&c)
    assert predictions.groupby(["model", "participant_id"]).fold.nunique().eq(1).all()
    for row in report["fold_results"]:
        assert set(row["metrics"]) == {"training", "validation", "test"}
        assert all(row["metrics"][k]["rows"] == row["split_counts"][k]["rows"] for k in row["metrics"])
        if row["policy"] != "validation_selected": continue
        search = next(s for s in local["searches"] if (s["fold"], s["model"]) == (row["fold"], row["model"]))
        best = max(search["candidates"], key=lambda s: (s["validation_metrics"]["f1"], -s["candidate"]))
        assert row["parameters"] == best["parameters"]
        assert row["decision_threshold"] == best["threshold"]
        assert not any("test" in k for k in best)
    assert select_candidate.__code__.co_varnames[:4] == ("training", "validation", "name", "candidates")
