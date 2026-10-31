"""Research-adapter tests, independent of the 4.49 GB local dataset."""

import numpy as np
import pandas as pd
import pytest

from src.models.features import prepare_features
from src.models.research_data import RESEARCH_FEATURES, WindowConfig, research_windows
from src.models.real_data_exploration import compare_models, fit_proxy_thresholds, proxy_labels


BASE = int(pd.Timestamp("2016-06-01", tz="UTC").timestamp() * 1000)


def event(minute, sensor, value=""):
    return {"ts_raw": BASE + minute * 60_000, "sensor_id": sensor,
            "Screen_Value": value if sensor == "Screen" else "",
            "App_Value": value if sensor == "App" else ""}


def example_rows():
    rows = [event(m, "Batt") for m in range(0, 181, 10)]
    rows += [event(0, "Screen", "Off"), event(5, "Screen", "Unlocked"),
             event(7, "Screen", "On"), event(15, "Screen", "Off"),
             event(25, "Screen", "Unlocked"), event(45, "Screen", "Off")]
    rows += [event(m, "App", value) for m, value in [
        (1, "a"), (2, "+launcher"), (3, "=keyboard"), (4, "-android"),
        (5, "a"), (10, "b"), (11, "b"), (31, "c"),
    ]]
    return rows


def test_windows_boundary_order_and_previous_session_are_causal(tmp_path):
    path = tmp_path / "u_test.csv"
    rows = example_rows()
    pd.DataFrame(rows).to_csv(path, index=False)
    ordered, info = research_windows(path)
    assert len(ordered) == 5
    assert ordered.iloc[0]["screen_active_seconds"] == 900
    assert ordered.iloc[1]["screen_active_seconds"] == 900
    assert ordered.iloc[0]["previous_session_duration_seconds"] == 600
    assert ordered.iloc[1]["previous_session_duration_seconds"] == 1200
    assert ordered.iloc[0]["next_longest_session_seconds"] == 900
    assert ordered.iloc[0]["unlock_count"] == 2
    assert ordered.iloc[0]["app_switch_count"] == 1
    assert ordered.iloc[1]["app_switch_count"] == 1
    assert info["excluded_app_events"] == 3
    prepare_features(ordered, RESEARCH_FEATURES)
    pd.DataFrame(rows[::-1]).to_csv(path, index=False)
    reversed_frame, _ = research_windows(path)
    pd.testing.assert_frame_equal(ordered, reversed_frame)


def test_gap_never_turns_next_available_row_into_next_window(tmp_path):
    rows = [event(m, "Batt") for m in [0, 10, 20, 80, 90, 100, 110, 120, 130, 140, 150, 160, 170, 180]]
    rows += [event(0, "Screen", "Off"), event(15, "Screen", "Unlocked"),
             event(100, "Screen", "Off")]
    path = tmp_path / "u_test.csv"
    pd.DataFrame(rows).to_csv(path, index=False)
    frame, diagnostics = research_windows(path)
    assert diagnostics["recording_gaps"] == 1
    assert len(frame) == 1
    assert frame.iloc[0]["window_start_ms"] == BASE + 120 * 60_000
    assert frame["previous_session_duration_seconds"].isna().all()
    assert (frame["target_end_ms"] - frame["window_end_ms"]).eq(30 * 60_000).all()


def test_missing_screen_start_and_long_session_are_flagged(tmp_path):
    rows = [event(m, "Batt") for m in range(0, 241, 10)]
    rows += [event(5, "Screen", "Unlocked"), event(190, "Screen", "Off")]
    path = tmp_path / "u_test.csv"
    pd.DataFrame(rows).to_csv(path, index=False)
    frame, detail = research_windows(path)
    assert detail["long_or_zero_sessions"] == 1
    assert len(frame) == 0  # Every usable adjacent pair touches the excluded span.


def test_invalid_year_and_unknown_screen_are_not_silent(tmp_path):
    rows = example_rows() + [{"ts_raw": 1000, "sensor_id": "Batt", "Screen_Value": "", "App_Value": ""}]
    path = tmp_path / "u_test.csv"
    pd.DataFrame(rows).to_csv(path, index=False)
    _, detail = research_windows(path)
    assert detail["excluded_timestamp_rows"] == 1
    rows += [event(60, "Screen", "unexpected")]
    pd.DataFrame(rows).to_csv(path, index=False)
    with pytest.raises(ValueError, match="unknown screen"):
        research_windows(path)


def test_proxy_thresholds_are_fitted_only_from_explicit_training_data():
    train = pd.DataFrame({"next_longest_session_seconds": [0, 0, 100, 200],
                          "next_unlock_count": [0, 0, 2, 4]})
    thresholds = fit_proxy_thresholds(train, 0.5)
    assert thresholds == {"longest_session_seconds": 50.0, "unlock_count": 1.0}
    held_out = pd.DataFrame({"next_longest_session_seconds": [0, 1800],
                             "next_unlock_count": [0, 30]})
    assert proxy_labels(held_out, thresholds).tolist() == [0, 1]
    assert thresholds == {"longest_session_seconds": 50.0, "unlock_count": 1.0}
    held_out.iloc[0, 0] = np.nan
    with pytest.raises(ValueError, match="Missing target"):
        proxy_labels(held_out, thresholds)
    empty_use = train * 0
    assert proxy_labels(empty_use, fit_proxy_thresholds(empty_use, 0.9)).sum() == 0


def test_research_comparison_keeps_each_user_in_one_holdout_fold():
    parts = []
    for user in range(3):
        active = np.array([0, 1000, 0, 1200, 0, 1500, 0, 1600], dtype=float)
        parts.append(pd.DataFrame({
            "participant_id": f"u{user}", "window_start_ms": BASE + np.arange(8) * 1_800_000,
            "window_end_ms": BASE + (np.arange(8) + 1) * 1_800_000,
            "screen_active_seconds": active, "unlock_count": (active > 0).astype(int) * 5,
            "app_switch_count": (active > 0).astype(int),
            "previous_session_duration_seconds": np.nan,
            "next_longest_session_seconds": active, "next_unlock_count": (active > 0).astype(int) * 5,
        }))
    table = pd.concat(parts, ignore_index=True)
    results, means, predictions, per_user = compare_models(table, folds=3, quantile=0.5)
    assert len(results) == 9 and len(per_user) == 9
    assert set(means) == {"Rule", "Logistic Regression", "Random Forest"}
    assert len(predictions) == len(table) * 3
    assert predictions.groupby(["model", "participant_id"])["fold"].nunique().eq(1).all()
    assert len(predictions.groupby("model").size()) == 3
    assert all(result["training_users"] == 2 and result["test_users"] == 1 for result in results)


@pytest.mark.parametrize("gap,session", [(0, 120), (30, 0), (np.nan, 120)])
def test_invalid_quality_settings(gap, session):
    with pytest.raises(ValueError):
        WindowConfig(gap_minutes=gap, max_session_minutes=session)
