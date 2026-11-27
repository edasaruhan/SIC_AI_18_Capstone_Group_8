import numpy as np
import pandas as pd

from src.models.pilot_holdout import WINDOW_MS, holdout_pairs, summarize


def test_pilot_holdout_requires_new_good_input_and_feedback_for_the_next_window():
    width = WINDOW_MS
    windows = pd.DataFrame({
        "participant_id": ["test"] * 7,
        "window_start_ms": np.arange(7) * width,
        "window_end_ms": np.arange(1, 8) * width,
        "quality_status": ["OBSERVATION_SUFFICIENT"] * 2 + ["UNCERTAIN"]
        + ["OBSERVATION_SUFFICIENT"] * 4,
    })
    feedback = windows.iloc[[1, 2, 3, 4, 5, 6]][
        ["participant_id", "window_start_ms", "window_end_ms"]
    ].copy()
    feedback["asked_at_ms"] = feedback.window_end_ms
    feedback["answered_at_ms"] = feedback.window_end_ms + 1
    feedback["answer"] = ["YES", "NO", "NO", "YES", "YES", "NO"]
    feedback["response_status"] = ["ANSWERED"] * 4 + ["SKIPPED", "ANSWERED"]
    feedback["question_version"] = "q-v1"
    feedback.loc[6, "answered_at_ms"] = 9 * width

    pairs = holdout_pairs(windows, feedback, width, exported_at=8 * width)

    # Old input, uncertain input, skipped answers and post-export answers are excluded.
    assert pairs.input_start_ms.tolist() == [width, 3 * width]
    assert pairs.answer.tolist() == ["NO", "YES"]


def test_threshold_comparison_counts_yes_and_no_and_keeps_undefined_f1():
    scores = np.array([0.10, 0.35, 0.65, 0.87])
    labels = np.array([False, False, True, True])
    baseline = summarize(scores, labels, 0.25)
    personal = summarize(scores, labels, 0.85)
    assert (baseline["yes_high"], baseline["no_high"]) == (2, 1)
    assert (personal["yes_high"], personal["no_high"], personal["yes_low"]) == (1, 0, 1)
    assert summarize(scores[:2], labels[:2], 0.25)["f1_yes"] is None
